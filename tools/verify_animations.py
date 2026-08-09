#!/usr/bin/env python3
"""Verify the animation wiring for the miniboss combat overhaul.

Read-only. Checks, in order:

1. Every animation name referenced from Java resolves to a clip in the resource the animator points at.
2. Every bone a v2 clip animates exists on the model each of the five minibosses renders with.
3. Looping clips close their seam, and no clip has out-of-order keyframes.
4. The legacy resource is still present and still contains every clip it originally did.

Run from anywhere: ``python tools/verify_animations.py``. Exits non-zero on any failure.
"""

from __future__ import annotations

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "rpg-minibosses")
JAVA = os.path.join(ROOT, "common", "src", "main", "java")

ACTIVE = os.path.join(ASSETS, "animations", "mobs_v2.animations.json")
LEGACY = os.path.join(ASSETS, "animations", "mobs.animations.json")

# Which geometry each of the five renders with — see MinibossRenderer.
MODELS = {
    "jugg": "juggmob.json",
    "templar": "templarmob.geo.json",
    "trickster": "thiefmob.json",
    "merc": "artmob.json",
    "mage": "archmagefire.json",
}

failures = []


def fail(message):
    failures.append(message)
    print("FAIL: " + message)


def load(path):
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


# Actions whose clip length deliberately does not equal their phase total, and why.
TIMING_EXEMPT = {
    "rogue_throw": "the clip is re-dispatched mid-action for the second knife, so it plays twice",
    "mage_fireball": "the clip is re-dispatched mid-action for the second bolt, so it plays twice",
    "jugg_spin": "the clip loops for the duration of the spin rather than spanning it once",
}

BRAIN_DIR = os.path.join(JAVA, "com", "cleannrooster", "rpg_minibosses", "entity", "brain", "impl")

FAMILY_PREFIX = {
    "JuggernautBrain.java": "animation.v2.jugg.",
    "TemplarBrain.java": "animation.v2.templar.",
    "RogueBrain.java": "animation.v2.trickster.",
    "MercenaryBrain.java": "animation.v2.merc.",
    "FireMageBrain.java": "animation.v2.mage.",
}


def swing_directions(text, prefix):
    """Work out which way each attack's SwingPath declares its blade travels.

    Read from the Java rather than kept in a table here. A hand-maintained expectation list is a third
    source of truth, and it went stale the moment a clip was re-authored — the check then reported a
    correct pairing as broken. Now there are only two things to compare: what the attack declares and
    what the clip does.

    Returns clip name -> +1 for left-to-right, -1 for right-to-left. Attacks whose swing has no
    horizontal travel (rising cuts, lanes, cones) are omitted; there is nothing to disagree about.
    """
    expected = {}
    factory_sign = {"rightToLeft": -1, "leftToRight": +1}

    def record(clip, factory):
        if factory in factory_sign:
            expected[clip] = factory_sign[factory]

    # Inline builders: the factory sits directly in the chain.
    for match in re.finditer(r'CombatAction\.builder\("([^"]+)"\)(.*?)\.build\(\);', text, re.S):
        chain = match.group(2)
        animation = re.search(r'\.animation\(\s*(?:family \+ )?"([^"]+)"\s*\)', chain)
        if not animation:
            continue
        name = animation.group(1)
        if not name.startswith("animation."):
            name = prefix + name
        factories = re.findall(r'SwingPath\.(\w+)\(', chain)
        if len(factories) == 1:
            record(name, factories[0])

    # Helper builders: a ternary picks the factory, and the call site supplies the deciding argument.
    helper = re.compile(
        r'private CombatAction (\w+)\([^)]*\)\s*\{\s*return CombatAction\.builder\(\w+\)(.*?)\.build\(\);',
        re.S)
    for match in helper.finditer(text):
        method, chain = match.group(1), match.group(2)
        ternary = re.search(r'\(\s*(\w+)\s*(?:>\s*0\s*)?\?\s*SwingPath\.(\w+)\(.*?:\s*SwingPath\.(\w+)\(',
                            chain, re.S)
        if not ternary:
            continue
        _, when_true, when_false = ternary.groups()
        sites = re.findall(r'%s\(\s*"[^"]+",\s*family \+ "([^"]+)",\s*([^)]+)\)' % re.escape(method), text)
        for suffix, argument in sites:
            argument = argument.strip()
            truthy = argument == "true" or (argument.lstrip("+-").isdigit() and not argument.startswith("-")
                                            and argument != "0")
            record(prefix + suffix, when_true if truthy else when_false)
    return expected


def check_swing_directions(active, timings, text, prefix):
    """Check each melee clip's body-yaw travel against the direction its attack's SwingPath declares.

    This is the invariant the whole arc system exists to hold: the damaging sweep travels the way the
    weapon visibly does. Nothing else catches a clip and its arc pointing opposite ways, because both look
    individually reasonable — it only shows up as a swing that hits behind where it looked.

    Positive JSON body yaw turns the model toward its world right (see gen_v2_animations.py), so a
    right-to-left swing must show yaw DECREASING across the contact frame.
    """
    for name, expected in swing_directions(text, prefix).items():
        clip = active.get(name)
        if clip is None:
            fail("swing-direction check: '%s' is not in the active resource" % name)
            continue
        body = clip.get("bones", {}).get("body", {}).get("rotation")
        if not body or isinstance(body, list) or "vector" in body:
            continue  # a clip that never turns its body has no travel to check
        # A keyframe is either {"vector": [...]} or a bare [x, y, z].
        keys = sorted(((float(t), (v["vector"] if isinstance(v, dict) else v)[1])
                       for t, v in body.items()), key=lambda kv: kv[0])
        windup = timings.get(name)
        if windup is None:
            continue
        contact = windup / 20.0
        # Measure the strike itself: from the last key before contact to the contact key. Spanning past
        # the contact instead would measure the follow-through's return to neutral, which runs the other
        # way and would report every correct clip as backwards.
        before = [y for t, y in keys if t < contact - 1e-6]
        after = [y for t, y in keys if t >= contact - 1e-6]
        if not before or not after:
            continue
        travel = after[0] - before[-1]
        if travel == 0 or (travel > 0) != (expected > 0):
            fail("'%s' swings %s through contact but its attack declares the opposite"
                 % (name, "left to right" if travel > 0 else "right to left"))


def check_helper_timings(active, text, prefix, report):
    """Check actions built through a shared helper method rather than inline.

    ``CombatAction.builder(id)`` with a parameter is invisible to the literal-id scan, which hid the
    Templar's sweeps and the Trickster's crossing slashes completely. Here the helper's own timing is read
    once and then checked against every call site that names a clip.
    """
    helper = re.compile(
        r'private CombatAction (\w+)\([^)]*\)\s*\{\s*return CombatAction\.builder\(\w+\)(.*?)\.build\(\);',
        re.S)
    for match in helper.finditer(text):
        method, chain = match.group(1), match.group(2)
        timing = re.search(r'\.timing\((\d+),\s*(\d+),\s*(\d+)\)', chain)
        if not timing:
            continue
        ticks = sum(int(timing.group(i)) for i in (1, 2, 3))
        sites = re.findall(r'%s\(\s*"([^"]+)",\s*family \+ "([^"]+)"' % re.escape(method), text)
        if not sites:
            fail("helper %s builds an action but no call site names its clip" % method)
        for action_id, suffix in sites:
            name = prefix + suffix
            if name not in active:
                fail("%s plays '%s', which is not in the active resource" % (action_id, name))
                continue
            clip_ticks = active[name].get("animation_length", 0.0) * 20.0
            report(action_id, name, int(timing.group(1)), clip_ticks)
            if abs(clip_ticks - ticks) > 1.0:
                fail("%s runs %d ticks but '%s' is %.1f ticks long"
                     % (action_id, ticks, name, clip_ticks))


def check_timings(active, windups=None):
    """Cross-check every CombatAction's phase total against the length of the clip it plays."""
    for filename, prefix in FAMILY_PREFIX.items():
        path = os.path.join(BRAIN_DIR, filename)
        if not os.path.exists(path):
            fail("brain source %s is missing" % filename)
            continue
        text = open(path, encoding="utf-8").read()
        check_helper_timings(active, text, prefix,
                             lambda action_id, clip, windup, ignored:
                                 windups.__setitem__(clip, windup) if windups is not None else None)
        # Each builder chain runs from `CombatAction.builder("id")` to its `.build();`.
        for match in re.finditer(r'CombatAction\.builder\("([^"]+)"\)(.*?)\.build\(\);', text, re.S):
            action_id, chain = match.group(1), match.group(2)
            timing = re.search(r'\.timing\((\d+),\s*(\d+),\s*(\d+)\)', chain)
            if not timing:
                continue
            animation = re.search(r'\.animation\(\s*(?:family \+ )?"([^"]+)"\s*\)', chain)
            if animation:
                name = animation.group(1)
            else:
                # The builder takes its clip as a parameter (the shared sweep/cross-slash helpers). Resolve
                # it from the helper's call site instead of skipping, which is how the Trickster's crossing
                # slashes drifted out of step unnoticed.
                call = re.search(r'\("%s",\s*family \+ "([^"]+)"' % re.escape(action_id), text)
                if not call:
                    fail("cannot resolve which clip %s plays; timing is unchecked" % action_id)
                    continue
                name = prefix + call.group(1)
            if not name.startswith("animation."):
                name = prefix + name
            if name not in active:
                fail("%s plays '%s', which is not in the active resource" % (action_id, name))
                continue
            if action_id in TIMING_EXEMPT:
                continue
            speed_match = re.search(r'\.animationSpeed\(([0-9.]+)f?\)', chain)
            speed = float(speed_match.group(1)) if speed_match else 1.0
            ticks = sum(int(timing.group(i)) for i in (1, 2, 3))
            if windups is not None:
                windups[name] = int(timing.group(1))
            clip_ticks = active[name].get("animation_length", 0.0) * 20.0 / speed
            if abs(clip_ticks - ticks) > 1.0:
                fail("%s runs %d ticks but '%s' is %.1f ticks long"
                     % (action_id, ticks, name, clip_ticks))


def main():
    if not os.path.exists(LEGACY):
        fail("the legacy animation resource has been deleted; it must be retained for posterity")
        return 1

    active = load(ACTIVE)["animations"]
    legacy = load(LEGACY)["animations"]

    # 1. Java references -----------------------------------------------------------------------
    #
    # Only clips dispatched at the *miniboss* animator are checked. Gemini, the Magus and the orb each
    # point at their own resource, so their names legitimately do not appear here. The scan is therefore
    # narrowed to the miniboss dispatcher plus every call that goes through it.
    referenced = set()
    families = ("animation.v2.jugg.", "animation.v2.templar.", "animation.v2.trickster.",
                "animation.v2.merc.", "animation.v2.mage.")

    dispatcher = os.path.join(JAVA, "com", "cleannrooster", "rpg_minibosses", "client", "entity",
                              "renderer", "MinibossAnimationProvider.java")
    text = open(dispatcher, encoding="utf-8").read()
    # AzCommand.create("controller", "clip", ...) — the legacy one-shot commands.
    for match in re.finditer(r'AzCommand\.create\(\s*"[^"]+",\s*"([^"]+)"', text):
        referenced.add(match.group(1))
    # The bare locomotion names the legacy helpers build inline.
    for match in re.finditer(r'"(base_controller|attacks|dash)",\s*"([^"]+)"', text):
        referenced.add(match.group(2))

    for dirpath, _, filenames in os.walk(JAVA):
        for name in filenames:
            if not name.endswith(".java"):
                continue
            body = open(os.path.join(dirpath, name), encoding="utf-8").read()
            # dispatcher.play("controller", family + "suffix", ...) and the literal form.
            for match in re.finditer(r'family \+ "([a-z_0-9]+)"', body):
                suffix = match.group(1)
                matches = [prefix + suffix for prefix in families if prefix + suffix in active]
                if matches:
                    referenced.update(matches)
                else:
                    fail("no v2 clip matches suffix '%s' referenced in %s" % (suffix, name))
            for match in re.finditer(r'\.play\(\s*"[^"]+",\s*"(animation\.[A-Za-z0-9_.]+)"', body):
                referenced.add(match.group(1))
            # Explicit family constant concatenations, e.g. Dispatcher.JUGG + "brace".
            for match in re.finditer(r'Dispatcher\.[A-Z]+\s*\+\s*"([a-z_0-9]+)"', body):
                suffix = match.group(1)
                if not any(prefix + suffix in active for prefix in families):
                    fail("no v2 clip matches suffix '%s' referenced in %s" % (suffix, name))
                else:
                    referenced.update(p + suffix for p in families if p + suffix in active)

    missing = sorted(n for n in referenced if n not in active)
    for name in missing:
        fail("miniboss dispatch references animation '%s' which is not in the active resource" % name)

    # 2. Bones ----------------------------------------------------------------------------------
    for key, geo_file in MODELS.items():
        geometry = load(os.path.join(ASSETS, "geo", geo_file))
        bones = {b["name"] for b in geometry["minecraft:geometry"][0]["bones"]}
        prefix = "animation.v2.%s." % key
        for name, clip in active.items():
            if not name.startswith(prefix):
                continue
            for bone in clip.get("bones", {}):
                if bone not in bones:
                    fail("%s animates bone '%s' which %s does not have" % (name, bone, geo_file))

    # 3. Clip structure -------------------------------------------------------------------------
    for name, clip in active.items():
        if not name.startswith("animation.v2."):
            continue
        length = clip.get("animation_length")
        looping = clip.get("loop") is True
        for bone, channels in clip.get("bones", {}).items():
            for channel, keys in channels.items():
                # A channel is either a keyframe map, {"vector": [...]}, or — as Blockbench exports a
                # static value — a bare [x, y, z]. The last two are constant and have no timing to check.
                if isinstance(keys, list) or "vector" in keys:
                    continue
                times = [float(t) for t in keys]
                if times != sorted(times):
                    fail("%s/%s/%s has out-of-order keyframes" % (name, bone, channel))
                if length is not None and times and times[-1] > length + 1e-6:
                    fail("%s/%s/%s has a keyframe past the clip length" % (name, bone, channel))
                if looping and times and abs(times[-1] - length) > 1e-6:
                    fail("%s/%s/%s is looping but does not key its final frame at the clip length"
                         % (name, bone, channel))

    # 4. Clip length vs action timing -----------------------------------------------------------
    #
    # An attack whose clip is shorter than its phases pops back to the locomotion stance mid-swing; one
    # that is longer gets cut off before its follow-through. Both read as the animation being unrelated to
    # what the mob is doing, so the two are checked against each other rather than trusted to stay in step.
    windups = {}
    check_timings(active, windups)

    # 5. Swing direction vs animation --------------------------------------------------------------
    for filename, prefix in FAMILY_PREFIX.items():
        path = os.path.join(BRAIN_DIR, filename)
        if os.path.exists(path):
            check_swing_directions(active, windups, open(path, encoding="utf-8").read(), prefix)

    # 6. Legacy integrity -----------------------------------------------------------------------
    dropped = sorted(n for n in legacy if n not in active)
    for name in dropped:
        fail("legacy clip '%s' was not carried forward into the v2 resource" % name)

    print()
    print("active clips:  %d" % len(active))
    print("legacy clips:  %d (all present in v2: %s)" % (len(legacy), not dropped))
    print("java refs:     %d" % len(referenced))
    print("failures:      %d" % len(failures))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
