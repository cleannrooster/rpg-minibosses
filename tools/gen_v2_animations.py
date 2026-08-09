#!/usr/bin/env python3
"""Author the v2 combat animation set for the five land minibosses.

This is an *authoring* tool, not a procedural generator: every pose below is hand-picked. The script
exists only to serialise those poses into Bedrock animation JSON without hand-writing tens of thousands
of braces, and to guarantee clean keyframe channels (every animated bone gets a value at every keyframe,
so nothing interpolates from a stale channel and loop seams close exactly).

SAFETY
------
It writes exactly one file, ``animations/mobs_v2.animations.json``, and refuses to touch anything else.
The legacy ``mobs.animations.json`` is read only to copy its clips forward, never modified.

**It will not overwrite hand edits.** After a successful write it records a checksum of what it produced
in ``tools/.mobs_v2.stamp``. On the next run it re-checks that stamp: if the file on disk is not byte-for
-byte what this script last emitted, somebody has edited it by hand — in Blockbench, most likely — and the
script aborts rather than destroying that work. ``--force`` overrides, and always writes a timestamped
backup alongside the output first.

This guard exists because it happened: a run of this script overwrote hand-authored animation edits, and
they were only recoverable because Gradle happened to have copied the file into ``common/build`` first.

Rotation sign convention — read this before touching a number
-------------------------------------------------------------
AzureLib parses Bedrock animation JSON with **X and Y rotations negated** and Z taken as-is
(``AzBakedAnimationsAdapter``: ``toRadians(-x)``, ``toRadians(-y)``, ``toRadians(z)``). Combined with the
model facing -Z and the entity's own right being -X (``rightArm`` sits at x=-5 on every one of these rigs),
that gives:

* **X** — negative swings a limb toward the model's front, for arms *and* legs alike. ``-90`` on an arm is
  straight ahead, ``-180`` straight overhead. Positive swings it backward. On the ``body`` bone, whose
  geometry sits above the pivot rather than below it, the same rotation reads as a forward lean, so
  positive body X leans forward.
* **Y** — positive turns the bone toward the entity's **right**, negative toward its left.
* **Z** — for a limb hanging below its pivot, positive swings the foot/hand toward model +X, which is the
  entity's **right**. On the ``body`` bone, whose geometry is above the pivot, positive tilts the torso's
  top the other way, toward its left.

The X/Z senses were established from the geometry (``waistFront`` sits at z=-5 and ``waistBack`` at z=+4,
so the model faces -Z) and then **corrected against what the mobs actually do in game**: a clip that moves
a limb toward model +X reads on screen as movement to the entity's world right. Two earlier passes derived
the opposite from bone names and compass reasoning and were wrong both times. If a direction ever looks
mirrored again, trust the screen over this comment — but then fix this comment.

The X rule is confirmed empirically by the legacy walk cycle, using its *position* channel — AzureLib does
not negate positions, so they are unambiguous. ``animation.unknown.walk`` lifts and translates each leg
forward (``y=+3, z=-4`` for the left; ``y=+4, z=-2`` for the right) on exactly the frame that leg's
rotation X reaches its **minimum**. Negative is forward. The arms agree: the left arm is at its most
negative on the frame the right leg is forward, which is what a walking body does.

Everything in this file is therefore authored directly in Bedrock's frame, with one exception: leg **Z**
was originally written with the opposite sense, so :func:`P` mirrors that single axis about the neutral on
the way out. Leg X is *not* mirrored — doing so is what made the legs swing lopsidedly backwards, because
the authored values are absolute poses rather than offsets from the neutral.

Rig notes (from geo/juggmob.json, templarmob.geo.json, thiefmob.json, archmagefire.json, artmob.json)
----------------------------------------------------------------------------------------------------
* Shared biped bones: body, torso, head, leftArm, rightArm, leftLeg, rightLeg, plus itemBone parented to
  rightArm. Armour bones (armorLeftArm, ...) are children of the limbs and are deliberately never
  animated — rotating them independently detaches plate from the limb it is strapped to.
* ``head`` additionally receives the look yaw/pitch from MinibossModelRenderer, so head keys here are a
  small offset on top of the look direction, never an absolute aim.
* The rig's authored neutral is a quarter-turned stance (body yaw -27.5 with the head counter-rotated
  +24.6). Every v2 clip is built on that same neutral, which is why the mobs still read as themselves.
* Templar's ``body`` pivots at y=0 while the others pivot at y=12, so its body X-rotations are authored
  smaller — the same angle swings the whole model much further on that rig.
* Artillerist's crossbow hangs off itemBone -> repeater_crossbow_arrow; itemBone is keyed for aim, and the
  crossbow child follows it, so the weapon never separates from the hand.

Timing
------
Attack clips are registered through :func:`phased`, which is given the same windup/active/recovery tick
counts as the matching ``CombatAction`` in Java plus the authored time of the clip's contact frame. It
remaps the keyframes so the contact lands exactly on the windup->active boundary and the clip's length
equals the action's total duration. ``tools/verify_animations.py`` reads the Java timings back and fails
if the two ever drift apart.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
import time
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
ANIM_DIR = os.path.join(HERE, "..", "common", "src", "main", "resources",
                        "assets", "rpg-minibosses", "animations")
LEGACY = os.path.join(ANIM_DIR, "mobs.animations.json")
OUTPUT = os.path.join(ANIM_DIR, "mobs_v2.animations.json")
#: Checksum of what this script last emitted, so hand edits to the output can be detected.
STAMP = os.path.join(HERE, ".mobs_v2.stamp")


def guard_hand_edits(force, payload):
    """Abort rather than clobber an output that has been edited outside this script.

    The output is a file a human can reasonably want to open in Blockbench and tweak. Regenerating on top
    of that silently destroys the tweak, so the check is: does the file on disk still match the checksum
    recorded on the last run? If not, stop — unless told otherwise, in which case take a backup first.
    """
    if not os.path.exists(OUTPUT):
        return
    current = open(OUTPUT, "rb").read()
    if hashlib.sha256(current).hexdigest() == hashlib.sha256(payload.encode("utf-8")).hexdigest():
        return  # identical output; nothing to lose either way
    recorded = open(STAMP, encoding="utf-8").read().strip() if os.path.exists(STAMP) else None
    edited = recorded is None or hashlib.sha256(current).hexdigest() != recorded
    if not edited:
        return
    backup = "%s.%s.bak" % (OUTPUT, time.strftime("%Y%m%d-%H%M%S"))
    shutil.copy2(OUTPUT, backup)
    if not force:
        raise SystemExit(
            "refusing to overwrite: %s has been modified since this script last wrote it.\n"
            "  A copy of the current file is at %s\n"
            "  Re-run with --force to overwrite anyway, or fold the hand edits back into this script."
            % (os.path.normpath(OUTPUT), os.path.normpath(backup)))
    print("forced overwrite; previous file backed up to %s" % os.path.normpath(backup))

# --- pose vocabulary ------------------------------------------------------------------------------

BODY = [5.0, -27.5, 0.0]
BODY_POS = [0.0, -1.0, 0.0]
HEAD = [-11.0, 24.6, -4.6]
LLEG = [19.1, 5.9, -16.5]
RLEG = [-22.3, 33.8, -2.7]
RLEG_POS = [-1.0, 1.0, 0.0]


def base_pose(arms):
    """The rig's neutral stance, with a weapon-specific arm set layered on."""
    pose = {
        "body": {"r": list(BODY), "p": list(BODY_POS)},
        "head": {"r": list(HEAD)},
        "leftLeg": {"r": list(LLEG), "p": [0.0, 0.0, 0.0]},
        "rightLeg": {"r": list(RLEG), "p": list(RLEG_POS)},
    }
    pose.update({k: {kk: list(vv) for kk, vv in v.items()} for k, v in arms.items()})
    return pose


# Carried tilt of `itemBone` in each rig, in degrees of X — the angle the held weapon sits at relative to
# the forearm at rest. Animation is additive on the model's own bone rotation (AzureLib does
# `setRotX(animated + initialSnapshot)`, and both the geo bake and the keyframe parser negate X the same
# way), and full extension is reached by continuing that tilt in the same direction rather than cancelling
# it. That direction is stated from what the models actually do on screen: the arithmetic suggested the
# opposite and was wrong, exactly as it was for the lateral clips. Trust the screen.
ITEM_TILT_2H = 45.0        # juggmob.json, templarmob.geo.json
ITEM_TILT_DAGGER = 40.0    # thiefmob.json


def EXT(tilt, amount=1.0):
    """`itemBone` rotation that extends the weapon into line with the arm.

    ``amount`` 0 leaves the weapon at its carried angle, 1 makes it a straight continuation of the
    forearm — so a swing can start with the weapon cocked and reach full extension exactly on contact,
    which is what makes the arc read as the weapon travelling rather than the arm alone.
    """
    return {"r": [tilt * amount, 0.0, 0.0]}


# Two-handed haft grip: left hand forward on the shaft, right hand back at the butt.
# itemBone is keyed at zero in the neutral so every clip carries the channel; a clip that never touches
# it emits one constant key and the weapon simply keeps its carried angle.
ARMS_2H = {
    "leftArm": {"r": [-67.4, 37.5, 17.6], "p": [0.0, -3.0, 2.0]},
    "rightArm": {"r": [-110.3, -48.8, 58.3], "p": [0.0, -2.0, 0.0]},
    "itemBone": {"r": [0.0, 0.0, 0.0]},
}
# One-handed blade: weapon arm relaxed and low, off hand out for balance.
ARMS_1H = {
    "leftArm": {"r": [6.4, -9.1, -50.2], "p": [0.0, 0.0, 0.0]},
    "rightArm": {"r": [-12.0, 46.0, 22.6], "p": [0.0, 0.0, 0.0]},
    "itemBone": {"r": [0.0, 0.0, 0.0]},
}
# Staff carried across the body, casting hand free.
ARMS_STAFF = {
    "leftArm": {"r": [-24.0, -14.0, -46.0], "p": [0.0, 0.0, 0.0]},
    "rightArm": {"r": [-38.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]},
}
# Crossbow shouldered: both hands on the stock, itemBone carrying the weapon's own tilt.
ARMS_XBOW = {
    "leftArm": {"r": [-48.1, 31.4, -38.3], "p": [0.0, 0.0, 2.0]},
    "rightArm": {"r": [-62.6, -36.2, 9.3], "p": [0.0, -2.0, -3.0]},
    "itemBone": {"r": [-2.2, -1.9, 74.2], "p": [2.0, -1.0, -0.3]},
}


LEG_NEUTRAL = {"leftLeg": LLEG, "rightLeg": RLEG}
#: Only Z. Leg X is authored directly in Bedrock's frame (negative = forward, same as the arms), which is
#: what the legacy clips use and what the walk cycle's position channel confirms. Leg Z was authored the
#: other way round — values below the neutral were written meaning "foot out to the entity's left" — so
#: that one axis is mirrored about the neutral on the way out. Reflecting X as well is what made the legs
#: swing lopsidedly backwards; do not add it back.
LEG_REFLECTED_AXES = (2,)


def P(base, **overrides):
    """A pose: the base stance with named bones replaced.

    Each override is ``bone=dict(r=[x,y,z])`` and optionally ``p=[x,y,z]``. Bones not mentioned keep the
    base value, which is what keeps a swing from silently dropping the legs back to rest.

    Leg rotations are reflected about their neutral on the Z axis only — see ``LEG_REFLECTED_AXES``
    and the module docstring. Every other channel, including leg X, passes through exactly as authored.
    """
    pose = {k: {kk: list(vv) for kk, vv in v.items()} for k, v in base.items()}
    for bone, value in overrides.items():
        entry = pose.setdefault(bone, {})
        if "r" in value:
            rotation = list(value["r"])
            neutral = LEG_NEUTRAL.get(bone)
            if neutral is not None:
                for axis in LEG_REFLECTED_AXES:
                    rotation[axis] = 2.0 * neutral[axis] - rotation[axis]
            entry["r"] = rotation
        if "p" in value:
            entry["p"] = list(value["p"])
    return pose


def by(delta):
    """Body yaw relative to the rig's authored neutral."""
    return BODY[1] + delta


# --- serialisation --------------------------------------------------------------------------------

#: Bones dropped from an upper-body-only clip. A MOBILE action plays over a running or orbiting mob, so
#: anything it keys wins over the locomotion controller underneath it. Keying the legs there made a
#: sprinting Mercenary stand still from the waist down for the length of a trap throw.
LOWER_BODY = ("leftLeg", "rightLeg")


def build(length, loop, frames, default_easing=None, upper_body_only=False):
    """Turn ``[(time, pose, easing), ...]`` into a Bedrock animation body.

    Every bone that moves gets a key at every frame time. Constant channels collapse to a single vector,
    so the output stays readable and the file stays small.
    """
    times = [f[0] for f in frames]
    bones_seen = OrderedDict()
    for _, pose, _ in frames:
        for bone in pose:
            bones_seen.setdefault(bone, None)

    out = OrderedDict()
    for bone in bones_seen:
        if upper_body_only and bone in LOWER_BODY:
            continue
        channels = OrderedDict()
        for chan_key, chan_name in (("r", "rotation"), ("p", "position")):
            if upper_body_only and bone == "body" and chan_key == "p":
                continue  # the locomotion cycle owns the body's bob while the mob is moving
            series = []
            for time, pose, easing in frames:
                value = pose.get(bone, {}).get(chan_key)
                if value is None:
                    value = [0.0, 0.0, 0.0] if chan_key == "p" else None
                series.append((time, value, easing))
            if any(v is None for _, v, _ in series):
                continue
            if chan_key == "p" and all(v == [0.0, 0.0, 0.0] for _, v, _ in series):
                continue
            if all(v == series[0][1] for _, v, _ in series):
                channels[chan_name] = {"vector": [round(x, 4) for x in series[0][1]]}
                continue
            keys = OrderedDict()
            for time, value, easing in series:
                entry = {"vector": [round(x, 4) for x in value]}
                ease = easing or default_easing
                if ease:
                    entry["easing"] = ease
                keys[fmt_time(time)] = entry
            channels[chan_name] = keys
        if channels:
            out[bone] = channels

    animation = OrderedDict()
    if loop:
        animation["loop"] = True
    animation["animation_length"] = round(length, 4)
    animation["bones"] = out
    assert times == sorted(times), "keyframe times must be authored in order"
    return animation


def fmt_time(value):
    text = ("%.4f" % value).rstrip("0")
    return text + "0" if text.endswith(".") else text


CLIPS = OrderedDict()
#: clip name -> (windup, active, recovery) in ticks, mirrored from the Java action definitions.
TIMINGS = OrderedDict()
#: clip name -> (windup scale, recovery scale). Anything below 1.0 is playing faster than authored.
COMPRESSION = OrderedDict()


def clip(name, length, frames, loop=False, easing="easeInOutSine", upper_body_only=False):
    assert name not in CLIPS, "duplicate clip " + name
    CLIPS[name] = build(length, loop, frames, easing, upper_body_only)


def phased(name, frames, windup, active, recovery, contact, easing="easeInOutSine",
           upper_body_only=False):
    """Register an attack clip placed onto its action's real phase boundaries.

    ``windup``/``active``/``recovery`` are the tick counts of the matching ``CombatAction``; ``contact`` is
    the authored time of the frame that coincides with the moment the attack becomes damaging.

    The placement **holds rather than stretches**. An earlier version scaled the authored times uniformly
    into each phase, which meant a slam authored to feel like a second and a half played out over two and a
    fifth — every beat slowed by the same factor, so the whole move read as sluggish and disconnected from
    the hit. Here the authored pacing is kept at its own speed and the slack is spent on two holds:

    * in the windup, on the loaded pose — the hammer goes up quickly and *waits* there, which is what a
      heavy telegraph should look like;
    * in the recovery, on the follow-through pose, with the return to neutral kept at its authored speed at
      the very end — the mob stays visibly committed through its punish window instead of drifting back.

    Only when the authored motion is longer than its budget is anything compressed.
    """
    authored_total = frames[-1][0]
    index = next((i for i, f in enumerate(frames) if abs(f[0] - contact) < 1e-6), None)
    assert index is not None, name + ": no keyframe at the stated contact time"
    assert 0 < index < len(frames) - 1, name + ": contact must fall inside the clip"

    pre, contact_frame, post = frames[:index], frames[index], frames[index + 1:]
    windup_seconds = windup / 20.0
    total_seconds = (windup + active + recovery) / 20.0

    placed = []
    # --- windup: approach at authored speed, then hold the loaded pose until the strike ---
    load_time = pre[-1][0]
    strike_gap = contact - load_time
    load_at = windup_seconds - strike_gap
    if load_at <= 0.0:
        # The action's whole windup is shorter than the authored strike; compress everything to fit.
        scale_pre = windup_seconds / contact
        placed = [(t * scale_pre, p, e) for t, p, e in pre]
    else:
        scale_pre = min(1.0, load_at / load_time) if load_time > 0 else 1.0
        placed = [(t * scale_pre, p, e) for t, p, e in pre]
        if placed[-1][0] < load_at:
            # Not a dead freeze: the loaded pose keeps creeping a little further along the direction it
            # arrived from, so a long telegraph reads as the mob straining against the weight rather than
            # as the animation having stalled.
            held = strained(pre[-2][1] if len(pre) >= 2 else None, pre[-1][1])
            placed.append((load_at, held, "easeInOutSine"))
    placed.append((windup_seconds, contact_frame[1], contact_frame[2]))

    # --- release and recovery: authored speed, hold the follow-through, settle at the end ---
    budget = total_seconds - windup_seconds
    authored_after = authored_total - contact
    scale = min(1.0, budget / authored_after) if authored_after > 0 else 1.0
    after = [(windup_seconds + (t - contact) * scale, p, e) for t, p, e in post]
    if after[-1][0] < total_seconds:
        if len(after) >= 2:
            settle = after[-1][0] - after[-2][0]
            hold_at = max(after[-2][0], total_seconds - settle)
            held = strained(after[-3][1] if len(after) >= 3 else None, after[-2][1])
            after = after[:-1] + [(hold_at, held, "easeInOutSine"),
                                  (total_seconds, after[-1][1], after[-1][2])]
        else:
            after.append((total_seconds, after[-1][1], after[-1][2]))
    placed.extend(after)

    # Keep times strictly increasing after rounding.
    remapped = []
    for time, pose, frame_easing in placed:
        time = round(time, 4)
        if remapped and time <= remapped[-1][0]:
            time = round(remapped[-1][0] + 0.05, 4)
        remapped.append((time, pose, frame_easing))

    TIMINGS[name] = (windup, active, recovery)
    # Compression is the one thing this function does that speeds an animation up rather than holding it.
    # It is recorded so a clip can never quietly end up playing faster than it was authored to.
    COMPRESSION[name] = (round(min(1.0, scale_pre), 3), round(min(1.0, scale), 3))
    clip(name, total_seconds, remapped, easing=easing, upper_body_only=upper_body_only)


def strained(previous, pose, factor=0.12):
    """The pose carried a little further along the direction it was already travelling.

    Used for the holds :func:`phased` inserts. Duplicating a keyframe exactly makes the model stop dead
    for the length of the hold, which reads as a hitch; continuing the motion by a fraction keeps it alive
    without changing the pose the player is reading.
    """
    if previous is None:
        return pose
    out = {}
    for bone, channels in pose.items():
        entry = {}
        for key, value in channels.items():
            before = previous.get(bone, {}).get(key)
            if before is None or len(before) != len(value):
                entry[key] = list(value)
            else:
                entry[key] = [v + (v - b) * factor for v, b in zip(value, before)]
        out[bone] = entry
    return out


def loop_clip(name, length, frames, easing="easeInOutSine", spin_seam=False):
    """A looping clip. The last frame must equal the first, or the seam pops.

    ``spin_seam`` exempts a clip whose body yaw deliberately ends a full turn ahead of where it started —
    a continuous rotation loops correctly at 360 degrees even though the numbers differ.
    """
    first, last = frames[0][1], frames[-1][1]
    if spin_seam:
        last = {bone: dict(value) for bone, value in last.items()}
        rotation = list(last["body"]["r"])
        rotation[1] -= 360.0
        last["body"] = dict(last["body"], r=rotation)
    assert first == last, name + ": loop seam does not close"
    clip(name, length, frames, loop=True, easing=easing)


# ==================================================================================================
# JUGGERNAUT — mass and commitment. Two-handed hammer, wide planted base, poor redirection.
# ==================================================================================================

J = base_pose(ARMS_2H)

# Weight settled back over the hips, hammer head resting low and to the right. Almost no motion: the
# Juggernaut idles like something that does not need to shift its feet.
loop_clip("animation.v2.jugg.stance", 3.0, [
    (0.0, P(J, body={"r": [4.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (1.5, P(J, body={"r": [6.5, by(1.5), 0.0], "p": [0.0, -0.4, 0.0]},
            rightArm={"r": [-106.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]},
            head={"r": [-9.0, 24.6, -4.6]}), None),
    (3.0, P(J, body={"r": [4.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]}), None),
])

# Forward advance: long strides, body pitched into the walk, hammer carried across. Deliberately heavy —
# a full stride cycle is a whole second, which is what makes the acceleration curve legible.
loop_clip("animation.v2.jugg.advance", 1.0, [
    (0.0, P(J, body={"r": [13.0, by(2), -3.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [38.0, 5.9, -16.5]},
            rightLeg={"r": [-34.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
            rightArm={"r": [-100.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]},
            leftArm={"r": [-60.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
    (0.25, P(J, body={"r": [16.0, by(0), 0.0], "p": [0.0, 0.5, 0.0]},
             leftLeg={"r": [10.0, 5.9, -16.5]},
             rightLeg={"r": [-6.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.5, P(J, body={"r": [13.0, by(-2), 3.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-30.0, 5.9, -16.5]},
            rightLeg={"r": [40.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
            rightArm={"r": [-118.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]},
            leftArm={"r": [-74.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
    (0.75, P(J, body={"r": [16.0, by(0), 0.0], "p": [0.0, 0.5, 0.0]},
             leftLeg={"r": [8.0, 5.9, -16.5]},
             rightLeg={"r": [-4.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (1.0, P(J, body={"r": [13.0, by(2), -3.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [38.0, 5.9, -16.5]},
            rightLeg={"r": [-34.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
            rightArm={"r": [-100.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]},
            leftArm={"r": [-60.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
])


def heavy_lateral(name, sign):
    """Juggernaut sidestep: a shuffle, not a stride. It never crosses its feet."""
    lead, trail = ("leftLeg", "rightLeg") if sign > 0 else ("rightLeg", "leftLeg")
    lead_base = LLEG if sign > 0 else RLEG
    trail_base = RLEG if sign > 0 else LLEG
    loop_clip(name, 0.7, [
        (0.0, P(J, body={"r": [8.0, by(0), -6.0 * sign], "p": [0.0, -1.0, 0.0]},
                **{lead: {"r": [lead_base[0], lead_base[1], lead_base[2] - 10.0 * sign]},
                   trail: {"r": [trail_base[0], trail_base[1], trail_base[2] - 2.0 * sign]}}), None),
        (0.35, P(J, body={"r": [9.0, by(0), -12.0 * sign], "p": [0.0, 0.2, 0.0]},
                 **{lead: {"r": [lead_base[0] - 4.0, lead_base[1], lead_base[2] - 26.0 * sign]},
                    trail: {"r": [trail_base[0] + 4.0, trail_base[1], trail_base[2] + 6.0 * sign]}}), None),
        (0.7, P(J, body={"r": [8.0, by(0), -6.0 * sign], "p": [0.0, -1.0, 0.0]},
                **{lead: {"r": [lead_base[0], lead_base[1], lead_base[2] - 10.0 * sign]},
                   trail: {"r": [trail_base[0], trail_base[1], trail_base[2] - 2.0 * sign]}}), None),
    ])


heavy_lateral("animation.v2.jugg.lateral_left", -1)
heavy_lateral("animation.v2.jugg.lateral_right", 1)

# Backstep: it does not turn around. Shoulders stay square to the threat, feet give ground grudgingly.
loop_clip("animation.v2.jugg.backstep", 0.9, [
    (0.0, P(J, body={"r": [-2.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-14.0, 5.9, -16.5]},
            rightLeg={"r": [16.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
            rightArm={"r": [-116.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]}), None),
    (0.45, P(J, body={"r": [-4.0, by(0), 0.0], "p": [0.0, -0.5, 0.0]},
             leftLeg={"r": [18.0, 5.9, -16.5]},
             rightLeg={"r": [-12.0, 33.8, -2.7], "p": [-1.0, 1.5, 1.0]},
             rightArm={"r": [-112.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]}), None),
    (0.9, P(J, body={"r": [-2.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-14.0, 5.9, -16.5]},
            rightLeg={"r": [16.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
            rightArm={"r": [-116.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]}), None),
])

# Brace: hard plant. Weight drops, feet widen, the hammer comes across as a barrier. Held on last frame.
clip("animation.v2.jugg.brace", 0.6, [
    (0.0, P(J), None),
    (0.2, P(J, body={"r": [-6.0, by(14), 0.0], "p": [0.0, 0.5, 0.0]},
            leftArm={"r": [-96.0, 20.0, 30.0], "p": [0.0, -2.0, 3.0]},
            rightArm={"r": [-126.0, -30.0, 46.0], "p": [0.0, -1.0, 1.0]}), "easeOutQuad"),
    (0.6, P(J, body={"r": [14.0, by(24), 0.0], "p": [0.0, -3.0, 0.0]},
            leftLeg={"r": [16.0, 5.9, -30.0], "p": [1.0, 0.0, 0.0]},
            rightLeg={"r": [-14.0, 33.8, 12.0], "p": [-2.0, 1.0, 0.0]},
            leftArm={"r": [-88.0, 26.0, 22.0], "p": [0.0, -2.0, 3.0]},
            rightArm={"r": [-118.0, -34.0, 50.0], "p": [0.0, -1.0, 1.0]},
            head={"r": [4.0, 24.6, -4.6]}), "easeOutQuad"),
])

# Heavy swing: the whole body winds up over the right shoulder, then drives through with a forward step.
# The arc is broad and the recovery is long — the punish window is the point.
phased("animation.v2.jugg.heavy_swing", [
    (0.0, P(J), None),
    (0.35, P(J, body={"r": [-4.0, by(46), -8.0], "p": [0.0, -0.5, 0.0]},
             rightArm={"r": [-172.0, -34.0, 40.0], "p": [0.0, -1.0, -2.0]},
             leftArm={"r": [-138.0, 44.0, 6.0], "p": [-1.0, -2.0, 1.0]},
             rightLeg={"r": [-30.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
             head={"r": [-4.0, 14.0, -4.6]}), "easeOutQuad"),
    (0.5, P(J, body={"r": [2.0, by(52), -10.0], "p": [0.0, -0.5, 0.0]},
            rightArm={"r": [-180.0, -30.0, 38.0], "p": [0.0, -1.0, -2.0]},
            leftArm={"r": [-144.0, 46.0, 4.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H, 0.4)), "easeInQuad"),
    (0.68, P(J, body={"r": [26.0, by(-64), 12.0], "p": [0.0, -2.0, 0.0]},
             rightArm={"r": [-80.0, 24.0, 62.0], "p": [0.0, -2.0, 1.0]},
             leftArm={"r": [-70.0, -14.0, 22.0], "p": [1.0, -3.0, 2.0]},
             itemBone=EXT(ITEM_TILT_2H),
             leftLeg={"r": [40.0, 5.9, -16.5], "p": [0.0, 0.0, -1.0]},
             rightLeg={"r": [-20.0, 33.8, -2.7], "p": [-1.0, 1.0, 2.0]},
             head={"r": [8.0, -6.0, -4.6]}), "easeInQuad"),
    (0.85, P(J, body={"r": [30.0, by(-72), 14.0], "p": [0.0, -3.0, 0.0]},
             rightArm={"r": [-62.0, 30.0, 66.0], "p": [0.0, -2.0, 1.0]},
             leftArm={"r": [-54.0, -18.0, 26.0], "p": [1.0, -3.0, 2.0]},
             itemBone=EXT(ITEM_TILT_2H, 0.8)), "easeOutQuad"),
    (1.15, P(J), "easeInOutSine"),
], 14, 5, 9, 0.68)

# Shoulder check: the point-blank answer to a player standing inside the hammer's arc. Short, blunt,
# almost no telegraph — it exists to make hugging the hitbox unsafe, not to be a real threat.
phased("animation.v2.jugg.shoulder_check", [
    (0.0, P(J), None),
    (0.15, P(J, body={"r": [-2.0, by(24), -6.0], "p": [0.0, 0.0, 0.0]},
             leftArm={"r": [-56.0, 44.0, 24.0], "p": [0.0, -3.0, 1.0]}), "easeOutQuad"),
    (0.3, P(J, body={"r": [18.0, by(-30), 10.0], "p": [0.0, -1.0, 0.0]},
            leftArm={"r": [-80.0, 10.0, 6.0], "p": [1.0, -2.0, 3.0]},
            leftLeg={"r": [34.0, 5.9, -16.5]},
            head={"r": [2.0, 6.0, -4.6]}), "easeInQuad"),
    (0.55, P(J), "easeOutSine"),
], 6, 3, 8, 0.3)

# Slam: rise, hang, drive the hammer into the floor. The contrast between the lift and the impact is what
# sells the weight; the body position channel does the heavy lifting so the model visibly leaves the ground.
phased("animation.v2.jugg.slam", [
    (0.0, P(J), None),
    (0.45, P(J, body={"r": [-16.0, by(10), 0.0], "p": [0.0, 3.0, -1.0]},
             rightArm={"r": [-196.0, -20.0, 30.0], "p": [0.0, 0.0, -2.0]},
             itemBone=EXT(ITEM_TILT_2H),
             leftArm={"r": [-186.0, 20.0, -10.0], "p": [0.0, -1.0, -2.0]},
             leftLeg={"r": [26.0, 5.9, -16.5]},
             rightLeg={"r": [-26.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
             head={"r": [-24.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.6, P(J, body={"r": [-20.0, by(12), 0.0], "p": [0.0, 4.5, -1.0]},
            rightArm={"r": [-204.0, -18.0, 28.0], "p": [0.0, 0.0, -2.0]},
             itemBone=EXT(ITEM_TILT_2H),
            leftArm={"r": [-194.0, 18.0, -12.0], "p": [0.0, -1.0, -2.0]},
            head={"r": [-28.0, 24.6, -4.6]}), "easeInOutSine"),
    (0.75, P(J, body={"r": [46.0, by(4), 0.0], "p": [0.0, -3.0, 1.0]},
             rightArm={"r": [-24.0, -6.0, 14.0], "p": [0.0, -1.0, 2.0]},
             itemBone=EXT(ITEM_TILT_2H),
             leftArm={"r": [-20.0, 6.0, -8.0], "p": [0.0, -2.0, 2.0]},
             leftLeg={"r": [54.0, 5.9, -20.0], "p": [0.0, -1.0, -2.0]},
             rightLeg={"r": [-46.0, 33.8, 4.0], "p": [-1.0, 0.0, 2.0]},
             head={"r": [22.0, 24.6, -4.6]}), "easeInQuad"),
    (0.95, P(J, body={"r": [40.0, by(4), 0.0], "p": [0.0, -2.5, 1.0]},
             rightArm={"r": [-30.0, -6.0, 14.0], "p": [0.0, -1.0, 2.0]},
             itemBone=EXT(ITEM_TILT_2H),
             leftArm={"r": [-26.0, 6.0, -8.0], "p": [0.0, -2.0, 2.0]}), "easeOutQuad"),
    (1.5, P(J), "easeInOutSine"),
], 22, 4, 14, 0.75)

# Leap: compress, launch, hang with the hammer overhead, land into the impact pose. The compression is
# long enough to be read and reacted to before the destination is locked.
phased("animation.v2.jugg.leap", [
    (0.0, P(J), None),
    (0.4, P(J, body={"r": [26.0, by(0), 0.0], "p": [0.0, -4.0, 0.0]},
            leftLeg={"r": [48.0, 5.9, -22.0], "p": [0.0, -1.0, -1.0]},
            rightLeg={"r": [-44.0, 33.8, 4.0], "p": [-1.0, 0.0, 1.0]},
            rightArm={"r": [-84.0, -48.8, 58.3], "p": [0.0, -1.0, 1.0]},
            leftArm={"r": [-44.0, 37.5, 17.6], "p": [0.0, -2.0, 3.0]},
            head={"r": [10.0, 24.6, -4.6]}), "easeInQuad"),
    (0.6, P(J, body={"r": [-14.0, by(0), 0.0], "p": [0.0, 2.0, 0.0]},
            leftLeg={"r": [-24.0, 5.9, -16.5]},
            rightLeg={"r": [10.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
            rightArm={"r": [-186.0, -24.0, 34.0], "p": [0.0, 0.0, -2.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftArm={"r": [-178.0, 22.0, -6.0], "p": [0.0, -1.0, -2.0]},
            head={"r": [-20.0, 24.6, -4.6]}), "easeOutQuad"),
    (1.0, P(J, body={"r": [-18.0, by(0), 0.0], "p": [0.0, 2.5, 0.0]},
            leftLeg={"r": [-30.0, 5.9, -16.5]},
            rightLeg={"r": [22.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
            rightArm={"r": [-200.0, -22.0, 32.0], "p": [0.0, 0.0, -2.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftArm={"r": [-190.0, 20.0, -8.0], "p": [0.0, -1.0, -2.0]}), "easeInOutSine"),
    (1.2, P(J, body={"r": [48.0, by(2), 0.0], "p": [0.0, -3.5, 1.0]},
            leftLeg={"r": [56.0, 5.9, -22.0], "p": [0.0, -1.0, -2.0]},
            rightLeg={"r": [-48.0, 33.8, 6.0], "p": [-1.0, 0.0, 2.0]},
            rightArm={"r": [-18.0, -4.0, 12.0], "p": [0.0, -1.0, 2.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftArm={"r": [-14.0, 4.0, -6.0], "p": [0.0, -2.0, 2.0]},
            head={"r": [24.0, 24.6, -4.6]}), "easeInQuad"),
    (1.6, P(J), "easeOutSine"),
], 16, 14, 12, 0.6)

# Spin: one revolution of a body that is genuinely carrying rotational momentum. Looped by the runner for
# the duration of the ability, so the seam has to close exactly — it does, at 360 degrees of body yaw.
loop_clip("animation.v2.jugg.spin", 0.8, [
    (0.0, P(J, body={"r": [16.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-92.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-84.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftLeg={"r": [20.0, 5.9, -16.5]},
            rightLeg={"r": [-20.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), "linear"),
    (0.2, P(J, body={"r": [18.0, by(90), 0.0], "p": [0.0, -0.5, 0.0]},
            rightArm={"r": [-92.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-84.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftLeg={"r": [-14.0, 5.9, -16.5]},
            rightLeg={"r": [18.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), "linear"),
    (0.4, P(J, body={"r": [16.0, by(180), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-92.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-84.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftLeg={"r": [20.0, 5.9, -16.5]},
            rightLeg={"r": [-20.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), "linear"),
    (0.6, P(J, body={"r": [18.0, by(270), 0.0], "p": [0.0, -0.5, 0.0]},
            rightArm={"r": [-92.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-84.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftLeg={"r": [-14.0, 5.9, -16.5]},
            rightLeg={"r": [18.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), "linear"),
    (0.8, P(J, body={"r": [16.0, by(360), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-92.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-84.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H),
            leftLeg={"r": [20.0, 5.9, -16.5]},
            rightLeg={"r": [-20.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), "linear"),
], spin_seam=True)

# Spin winddown: the momentum runs out against the ground rather than fading. Half a turn, dragged foot,
# hard plant.
clip("animation.v2.jugg.spin_end", 0.8, [
    (0.0, P(J, body={"r": [16.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-92.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-84.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H)), "linear"),
    (0.3, P(J, body={"r": [22.0, by(130), 0.0], "p": [0.0, -1.5, 0.0]},
            rightArm={"r": [-86.0, -30.0, 44.0], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-78.0, 30.0, 8.0], "p": [-1.0, -2.0, 1.0]},
            itemBone=EXT(ITEM_TILT_2H, 0.85),
            leftLeg={"r": [34.0, 5.9, -24.0], "p": [0.0, 0.0, -1.0]}), "easeOutQuad"),
    (0.5, P(J, body={"r": [30.0, by(178), 0.0], "p": [0.0, -3.0, 0.0]},
            rightArm={"r": [-62.0, -20.0, 40.0], "p": [0.0, -2.0, -1.0]},
            leftArm={"r": [-54.0, 22.0, 12.0], "p": [-1.0, -2.0, 1.0]},
            leftLeg={"r": [44.0, 5.9, -28.0], "p": [0.0, -1.0, -2.0]},
            rightLeg={"r": [-38.0, 33.8, 6.0], "p": [-1.0, 0.0, 2.0]},
            head={"r": [16.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.8, P(J, body={"r": [5.0, by(180), 0.0], "p": [0.0, -1.0, 0.0]}), "easeOutSine"),
])

# ==================================================================================================
# TEMPLAR — relentless forward pressure. Two-handed, but everything moves through the target rather
# than stopping at it. Body X-rotations stay smaller here: its body bone pivots at the feet.
# ==================================================================================================

T = base_pose(ARMS_2H)

loop_clip("animation.v2.templar.stance", 2.4, [
    (0.0, P(T, body={"r": [3.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (1.2, P(T, body={"r": [5.0, by(3), 0.0], "p": [0.0, -0.6, 0.0]},
            rightArm={"r": [-114.0, -48.8, 58.3], "p": [0.0, -2.0, 0.0]},
            leftArm={"r": [-70.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
    (2.4, P(T, body={"r": [3.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]}), None),
])

# Advance: a march. Shorter cycle than the Juggernaut, shoulders driving forward, weapon already levelled
# so the transition into a sweep costs nothing.
loop_clip("animation.v2.templar.advance", 0.7, [
    (0.0, P(T, body={"r": [8.0, by(2), -2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [40.0, 5.9, -16.5]},
            rightLeg={"r": [-36.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
            rightArm={"r": [-104.0, -48.8, 58.3], "p": [0.0, -2.0, -1.0]},
            leftArm={"r": [-62.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
    (0.175, P(T, body={"r": [10.0, by(0), 0.0], "p": [0.0, -0.2, 0.0]},
              leftLeg={"r": [8.0, 5.9, -16.5]},
              rightLeg={"r": [-6.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.35, P(T, body={"r": [8.0, by(-2), 2.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-32.0, 5.9, -16.5]},
             rightLeg={"r": [42.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
             rightArm={"r": [-116.0, -48.8, 58.3], "p": [0.0, -2.0, 1.0]},
             leftArm={"r": [-72.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
    (0.525, P(T, body={"r": [10.0, by(0), 0.0], "p": [0.0, -0.2, 0.0]},
              leftLeg={"r": [6.0, 5.9, -16.5]},
              rightLeg={"r": [-4.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.7, P(T, body={"r": [8.0, by(2), -2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [40.0, 5.9, -16.5]},
            rightLeg={"r": [-36.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
            rightArm={"r": [-104.0, -48.8, 58.3], "p": [0.0, -2.0, -1.0]},
            leftArm={"r": [-62.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), None),
])


def templar_lateral(name, sign):
    """Templar sidestep: it keeps the weapon pointed at the target and crabs sideways."""
    lead, trail = ("leftLeg", "rightLeg") if sign > 0 else ("rightLeg", "leftLeg")
    lead_base = LLEG if sign > 0 else RLEG
    trail_base = RLEG if sign > 0 else LLEG
    loop_clip(name, 0.55, [
        (0.0, P(T, body={"r": [4.0, by(0), -5.0 * sign], "p": [0.0, -1.0, 0.0]},
                **{lead: {"r": [lead_base[0] - 2.0, lead_base[1], lead_base[2] - 12.0 * sign]},
                   trail: {"r": [trail_base[0] + 2.0, trail_base[1], trail_base[2]]}}), None),
        (0.275, P(T, body={"r": [5.0, by(0), -11.0 * sign], "p": [0.0, 0.0, 0.0]},
                  **{lead: {"r": [lead_base[0] - 8.0, lead_base[1], lead_base[2] - 30.0 * sign]},
                     trail: {"r": [trail_base[0] + 8.0, trail_base[1], trail_base[2] + 8.0 * sign]}}), None),
        (0.55, P(T, body={"r": [4.0, by(0), -5.0 * sign], "p": [0.0, -1.0, 0.0]},
                 **{lead: {"r": [lead_base[0] - 2.0, lead_base[1], lead_base[2] - 12.0 * sign]},
                    trail: {"r": [trail_base[0] + 2.0, trail_base[1], trail_base[2]]}}), None),
    ])


templar_lateral("animation.v2.templar.lateral_left", -1)
templar_lateral("animation.v2.templar.lateral_right", 1)

# Backstep exists mostly so Divine Fall's exit and the parry reset have somewhere to go. The Templar
# gives ground reluctantly and never turns its back.
loop_clip("animation.v2.templar.backstep", 0.75, [
    (0.0, P(T, body={"r": [-3.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-16.0, 5.9, -16.5]},
            rightLeg={"r": [14.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]}), None),
    (0.375, P(T, body={"r": [-4.0, by(0), 0.0], "p": [0.0, -0.4, 0.0]},
              leftLeg={"r": [16.0, 5.9, -16.5]},
              rightLeg={"r": [-14.0, 33.8, -2.7], "p": [-1.0, 1.5, 1.0]}), None),
    (0.75, P(T, body={"r": [-3.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-16.0, 5.9, -16.5]},
             rightLeg={"r": [14.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]}), None),
])


def templar_sweep(name, direction):
    """An advancing sweep. ``direction`` +1 travels right-to-left, -1 left-to-right.

    The recovery deliberately ends leaning *forward* rather than back at neutral: the clip hands over to
    the advance cycle already biased into the next step, which is what removes the attack-then-stop beat.
    """
    d = direction
    phased(name, [
        (0.0, P(T), None),
        (0.25, P(T, body={"r": [-2.0, by(42 * d), -6.0 * d], "p": [0.0, -0.5, 0.0]},
                 rightArm={"r": [-158.0, -40.0 * d, 44.0], "p": [0.0, -1.0, -2.0]},
                 leftArm={"r": [-126.0, 40.0 * d, 10.0], "p": [-1.0, -2.0, 0.0]},
                 head={"r": [-8.0, 24.6 - 12.0 * d, -4.6]}), "easeOutQuad"),
        (0.34, P(T, body={"r": [0.0, by(48 * d), -8.0 * d], "p": [0.0, -0.5, 0.0]},
                 rightArm={"r": [-164.0, -36.0 * d, 42.0], "p": [0.0, -1.0, -2.0]},
                 leftArm={"r": [-132.0, 42.0 * d, 8.0], "p": [-1.0, -2.0, 0.0]},
                 itemBone=EXT(ITEM_TILT_2H, 0.4)), "easeInQuad"),
        (0.5, P(T, body={"r": [12.0, by(-58 * d), 10.0 * d], "p": [0.0, -1.0, 0.0]},
                rightArm={"r": [-84.0, 26.0 * d, 58.0], "p": [0.0, -2.0, 2.0]},
                leftArm={"r": [-76.0, -20.0 * d, 20.0], "p": [1.0, -3.0, 2.0]},
                itemBone=EXT(ITEM_TILT_2H),
                leftLeg={"r": [34.0, 5.9, -16.5], "p": [0.0, 0.0, -1.0]},
                rightLeg={"r": [-18.0, 33.8, -2.7], "p": [-1.0, 1.0, 2.0]},
                head={"r": [2.0, 24.6 + 14.0 * d, -4.6]}), "easeInQuad"),
        (0.62, P(T, body={"r": [14.0, by(-64 * d), 12.0 * d], "p": [0.0, -1.5, 0.0]},
                 rightArm={"r": [-72.0, 30.0 * d, 60.0], "p": [0.0, -2.0, 2.0]},
                 leftArm={"r": [-66.0, -24.0 * d, 24.0], "p": [1.0, -3.0, 2.0]},
                 itemBone=EXT(ITEM_TILT_2H, 0.8)), "easeOutQuad"),
        (0.85, P(T, body={"r": [9.0, by(2), -2.0], "p": [0.0, -1.0, 0.0]},
                 leftLeg={"r": [38.0, 5.9, -16.5]},
                 rightLeg={"r": [-34.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
                 rightArm={"r": [-104.0, -48.8, 58.3], "p": [0.0, -2.0, -1.0]},
                 leftArm={"r": [-62.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), "easeInOutSine"),
    ], 10, 5, 7, 0.5)


templar_sweep("animation.v2.templar.sweep_rl", 1)
templar_sweep("animation.v2.templar.sweep_lr", -1)

# Dash: a short plant, a visible forward load, then a low committed drive with the weapon trailing.
phased("animation.v2.templar.dash", [
    (0.0, P(T), None),
    (0.3, P(T, body={"r": [-6.0, by(0), 0.0], "p": [0.0, -0.5, 0.0]},
            leftLeg={"r": [-8.0, 5.9, -16.5]},
            rightLeg={"r": [26.0, 33.8, -2.7], "p": [-1.0, 1.0, 2.0]},
            rightArm={"r": [-128.0, -48.8, 58.3], "p": [0.0, -1.0, -3.0]},
            leftArm={"r": [-84.0, 37.5, 17.6], "p": [0.0, -2.0, 0.0]},
            head={"r": [-16.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.42, P(T, body={"r": [24.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [52.0, 5.9, -16.5], "p": [0.0, 0.0, -2.0]},
             rightLeg={"r": [-46.0, 33.8, -2.7], "p": [-1.0, 1.0, 3.0]},
             rightArm={"r": [-88.0, -48.8, 58.3], "p": [0.0, -2.0, 2.0]},
             leftArm={"r": [-48.0, 37.5, 17.6], "p": [0.0, -3.0, 3.0]},
             itemBone=EXT(ITEM_TILT_2H),
             head={"r": [6.0, 24.6, -4.6]}), "easeInQuad"),
    (0.68, P(T, body={"r": [22.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-30.0, 5.9, -16.5], "p": [0.0, 0.0, 2.0]},
             rightLeg={"r": [46.0, 33.8, -2.7], "p": [-1.0, 1.0, -2.0]},
             rightArm={"r": [-92.0, -48.8, 58.3], "p": [0.0, -2.0, 2.0]},
             leftArm={"r": [-52.0, 37.5, 17.6], "p": [0.0, -3.0, 3.0]},
             itemBone=EXT(ITEM_TILT_2H)), "linear"),
    (0.9, P(T, body={"r": [9.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]}), "easeOutQuad"),
], 8, 10, 5, 0.42)

# Dash-cut: the follow-up a dash flows straight into. A rising cross-cut from low right to high left,
# short enough that it reads as one action with the dash rather than as a second decision.
phased("animation.v2.templar.dash_cut", [
    (0.0, P(T, body={"r": [20.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-92.0, -48.8, 58.3], "p": [0.0, -2.0, 2.0]},
            leftArm={"r": [-52.0, 37.5, 17.6], "p": [0.0, -3.0, 3.0]}), None),
    (0.12, P(T, body={"r": [22.0, by(-28), 8.0], "p": [0.0, -1.5, 0.0]},
             rightArm={"r": [-48.0, 24.0, 52.0], "p": [0.0, -2.0, 2.0]},
             leftArm={"r": [-34.0, -12.0, 18.0], "p": [1.0, -3.0, 3.0]}), "easeOutQuad"),
    (0.28, P(T, body={"r": [-4.0, by(38), -10.0], "p": [0.0, 0.0, 0.0]},
             rightArm={"r": [-176.0, -34.0, 36.0], "p": [0.0, -1.0, -2.0]},
             leftArm={"r": [-150.0, 40.0, 4.0], "p": [-1.0, -2.0, 0.0]},
             itemBone=EXT(ITEM_TILT_2H),
             leftLeg={"r": [30.0, 5.9, -16.5]},
             head={"r": [-14.0, 24.6, -4.6]}), "easeInQuad"),
    (0.6, P(T, body={"r": [9.0, by(2), -2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [36.0, 5.9, -16.5]},
            rightLeg={"r": [-32.0, 33.8, -2.7], "p": [-1.0, 2.0, -2.0]},
            rightArm={"r": [-104.0, -48.8, 58.3], "p": [0.0, -2.0, -1.0]},
            leftArm={"r": [-62.0, 37.5, 17.6], "p": [0.0, -3.0, 2.0]}), "easeOutSine"),
], 6, 4, 8, 0.28)

# Divine Fall: the one moment the Templar voluntarily becomes still. Weapon planted, both hands on the
# pommel, head raised. Held through the whole channel by the runner, then released back into the advance.
phased("animation.v2.templar.divine_fall", [
    (0.0, P(T), None),
    (0.5, P(T, body={"r": [-2.0, by(16), 0.0], "p": [0.0, -2.0, 0.0]},
            rightArm={"r": [-58.0, -22.0, 30.0], "p": [0.0, -1.0, 2.0]},
            leftArm={"r": [-52.0, 20.0, -14.0], "p": [0.0, -1.0, 3.0]},
            leftLeg={"r": [22.0, 5.9, -24.0], "p": [0.0, -1.0, 0.0]},
            rightLeg={"r": [-18.0, 33.8, 8.0], "p": [-2.0, 0.0, 0.0]},
            head={"r": [-30.0, 24.6, -4.6]}), "easeOutQuad"),
    (1.0, P(T, body={"r": [-4.0, by(16), 0.0], "p": [0.0, -2.0, 0.0]},
            rightArm={"r": [-62.0, -22.0, 30.0], "p": [0.0, -1.0, 2.0]},
            leftArm={"r": [-56.0, 20.0, -14.0], "p": [0.0, -1.0, 3.0]},
            leftLeg={"r": [22.0, 5.9, -24.0], "p": [0.0, -1.0, 0.0]},
            rightLeg={"r": [-18.0, 33.8, 8.0], "p": [-2.0, 0.0, 0.0]},
            head={"r": [-34.0, 24.6, -4.6]}), "easeInOutSine"),
    (2.4, P(T, body={"r": [-3.0, by(16), 0.0], "p": [0.0, -2.0, 0.0]},
            rightArm={"r": [-60.0, -22.0, 30.0], "p": [0.0, -1.0, 2.0]},
            leftArm={"r": [-54.0, 20.0, -14.0], "p": [0.0, -1.0, 3.0]},
            leftLeg={"r": [22.0, 5.9, -24.0], "p": [0.0, -1.0, 0.0]},
            rightLeg={"r": [-18.0, 33.8, 8.0], "p": [-2.0, 0.0, 0.0]},
            head={"r": [-32.0, 24.6, -4.6]}), "easeInOutSine"),
    (3.0, P(T, body={"r": [9.0, by(0), 0.0], "p": [0.0, -1.0, 0.0]}), "easeInQuad"),
], 16, 60, 14, 0.5)

# Parry: a short, sharp catch on the haft. Fast enough that it never freezes the AI.
clip("animation.v2.templar.parry", 0.42, [
    (0.0, P(T), None),
    (0.1, P(T, body={"r": [-4.0, by(30), 0.0], "p": [0.0, -0.5, 0.0]},
            rightArm={"r": [-142.0, -18.0, 34.0], "p": [0.0, -1.0, -1.0]},
            leftArm={"r": [-118.0, 14.0, -4.0], "p": [0.0, -2.0, 1.0]},
            head={"r": [-6.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.2, P(T, body={"r": [2.0, by(-12), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-124.0, -34.0, 48.0], "p": [0.0, -2.0, 0.0]},
            leftArm={"r": [-96.0, 30.0, 8.0], "p": [0.0, -2.0, 2.0]}), "easeInQuad"),
    (0.42, P(T), "easeOutSine"),
])

# ==================================================================================================
# TRICKSTER — directional agility. One-handed blade, low centre of gravity, attacks that pass through.
# ==================================================================================================

R = base_pose(ARMS_1H)

loop_clip("animation.v2.trickster.stance", 1.6, [
    (0.0, P(R, body={"r": [6.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-18.0, 44.0, 22.6]}), None),
    (0.8, P(R, body={"r": [8.0, by(9), 0.0], "p": [0.0, -0.4, 0.0]},
            rightArm={"r": [-24.0, 46.0, 22.6]},
            leftArm={"r": [2.0, -9.1, -54.0]}), None),
    (1.6, P(R, body={"r": [6.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-18.0, 44.0, 22.6]}), None),
])

# Cautious forward movement: short steps, weight kept back, blade held ready. Not a run.
loop_clip("animation.v2.trickster.advance", 0.55, [
    (0.0, P(R, body={"r": [10.0, by(6), -2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [34.0, 5.9, -16.5]},
            rightLeg={"r": [-30.0, 33.8, -2.7], "p": [-1.0, 1.5, -1.0]},
            rightArm={"r": [-26.0, 44.0, 22.6]},
            leftArm={"r": [-12.0, -9.1, -50.2]}), None),
    (0.1375, P(R, body={"r": [11.0, by(6), 0.0], "p": [0.0, -0.3, 0.0]},
               leftLeg={"r": [6.0, 5.9, -16.5]},
               rightLeg={"r": [-4.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.275, P(R, body={"r": [10.0, by(6), 2.0], "p": [0.0, -1.0, 0.0]},
              leftLeg={"r": [-28.0, 5.9, -16.5]},
              rightLeg={"r": [36.0, 33.8, -2.7], "p": [-1.0, 1.5, -1.0]},
              rightArm={"r": [-20.0, 44.0, 22.6]},
              leftArm={"r": [4.0, -9.1, -50.2]}), None),
    (0.4125, P(R, body={"r": [11.0, by(6), 0.0], "p": [0.0, -0.3, 0.0]},
               leftLeg={"r": [4.0, 5.9, -16.5]},
               rightLeg={"r": [-2.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.55, P(R, body={"r": [10.0, by(6), -2.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [34.0, 5.9, -16.5]},
             rightLeg={"r": [-30.0, 33.8, -2.7], "p": [-1.0, 1.5, -1.0]},
             rightArm={"r": [-26.0, 44.0, 22.6]},
             leftArm={"r": [-12.0, -9.1, -50.2]}), None),
])


def trickster_orbit(name, sign):
    """A genuine sidestep cycle: the trailing foot crosses behind, the torso stays open to the target.

    This clip is the whole reason the orbit no longer reads as "running forward while sliding sideways".
    """
    lead, trail = ("leftLeg", "rightLeg") if sign > 0 else ("rightLeg", "leftLeg")
    lead_base = LLEG if sign > 0 else RLEG
    trail_base = RLEG if sign > 0 else LLEG
    loop_clip(name, 0.5, [
        (0.0, P(R, body={"r": [6.0, by(6), -8.0 * sign], "p": [0.0, -1.0, 0.0]},
                rightArm={"r": [-22.0, 44.0, 22.6]},
                **{lead: {"r": [lead_base[0] - 6.0, lead_base[1], lead_base[2] - 18.0 * sign]},
                   trail: {"r": [trail_base[0] + 6.0, trail_base[1], trail_base[2] + 4.0 * sign]}}), None),
        (0.25, P(R, body={"r": [7.0, by(6), -16.0 * sign], "p": [0.0, 0.2, 0.0]},
                 rightArm={"r": [-28.0, 44.0, 22.6]},
                 leftArm={"r": [-4.0, -9.1, -50.2 - 8.0 * sign]},
                 **{lead: {"r": [lead_base[0] - 16.0, lead_base[1], lead_base[2] - 38.0 * sign]},
                    trail: {"r": [trail_base[0] + 16.0, trail_base[1], trail_base[2] + 16.0 * sign]}}), None),
        (0.5, P(R, body={"r": [6.0, by(6), -8.0 * sign], "p": [0.0, -1.0, 0.0]},
                rightArm={"r": [-22.0, 44.0, 22.6]},
                **{lead: {"r": [lead_base[0] - 6.0, lead_base[1], lead_base[2] - 18.0 * sign]},
                   trail: {"r": [trail_base[0] + 6.0, trail_base[1], trail_base[2] + 4.0 * sign]}}), None),
    ])


trickster_orbit("animation.v2.trickster.lateral_left", -1)
trickster_orbit("animation.v2.trickster.lateral_right", 1)

# Withdrawal: an angled backwards skip, torso still turned toward the threat.
loop_clip("animation.v2.trickster.backstep", 0.5, [
    (0.0, P(R, body={"r": [-4.0, by(16), -6.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-20.0, 5.9, -16.5]},
            rightLeg={"r": [22.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
            rightArm={"r": [-14.0, 44.0, 22.6]}), None),
    (0.25, P(R, body={"r": [-6.0, by(16), -8.0], "p": [0.0, 0.6, 0.0]},
             leftLeg={"r": [20.0, 5.9, -16.5]},
             rightLeg={"r": [-18.0, 33.8, -2.7], "p": [-1.0, 1.5, 2.0]},
             rightArm={"r": [-8.0, 44.0, 22.6]}), None),
    (0.5, P(R, body={"r": [-4.0, by(16), -6.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-20.0, 5.9, -16.5]},
            rightLeg={"r": [22.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
            rightArm={"r": [-14.0, 44.0, 22.6]}), None),
])


def trickster_cross(name, sign):
    """A crossing slash: the body turns *through* the target line and exits past its shoulder.

    ``sign`` +1 exits to the Trickster's left. The final pose is deliberately not neutral — the torso is
    already carried past the swing, which is what makes the exit read as continuing rather than stopping.
    """
    s = sign
    phased(name, [
        (0.0, P(R, body={"r": [8.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]}), None),
        (0.14, P(R, body={"r": [4.0, by(34 * s), -8.0 * s], "p": [0.0, -0.5, 0.0]},
                 rightArm={"r": [-96.0, 20.0 * s, 46.0], "p": [0.0, -1.0, -2.0]},
                 itemBone=EXT(ITEM_TILT_DAGGER, 0.35),
                 leftArm={"r": [-24.0, -20.0 * s, -58.0], "p": [0.0, 0.0, -1.0]},
                 head={"r": [-14.0, 24.6 - 10.0 * s, -4.6]}), "easeOutQuad"),
        (0.26, P(R, body={"r": [14.0, by(-30 * s), 12.0 * s], "p": [0.0, -1.5, 0.0]},
                 rightArm={"r": [-76.0, 44.0 * s, 8.0], "p": [1.0, -1.0, 2.0]},
                 itemBone=EXT(ITEM_TILT_DAGGER),
                 leftArm={"r": [10.0, 10.0 * s, -40.0], "p": [0.0, 0.0, 1.0]},
                 leftLeg={"r": [40.0, 5.9, -16.5 - 10.0 * s], "p": [0.0, 0.0, -1.0]},
                 rightLeg={"r": [-24.0, 33.8, -2.7], "p": [-1.0, 1.0, 2.0]},
                 head={"r": [4.0, 24.6 + 16.0 * s, -4.6]}), "easeInQuad"),
        (0.4, P(R, body={"r": [10.0, by(-58 * s), 8.0 * s], "p": [0.0, -1.0, 0.0]},
                rightArm={"r": [-52.0, 50.0 * s, -6.0], "p": [1.0, 0.0, 1.0]},
                itemBone=EXT(ITEM_TILT_DAGGER, 0.75),
                leftArm={"r": [16.0, 4.0 * s, -34.0]},
                leftLeg={"r": [-22.0, 5.9, -16.5], "p": [0.0, 0.0, 2.0]},
                rightLeg={"r": [34.0, 33.8, -2.7], "p": [-1.0, 1.0, -1.0]}), "easeOutQuad"),
        (0.62, P(R, body={"r": [8.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]},
                 rightArm={"r": [-20.0, 44.0, 22.6]}), "easeInOutSine"),
    ], 5, 8, 7, 0.26)


trickster_cross("animation.v2.trickster.cross_slash_l", 1)
trickster_cross("animation.v2.trickster.cross_slash_r", -1)

# Mobile knife throw: the upper body squares to the target and snaps, the legs stay in a neutral pose the
# locomotion clip can still show through. Deliberately short — this is harassment, not a commitment.
clip("animation.v2.trickster.throw_mobile", 0.45, [
    (0.0, P(R, body={"r": [6.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.14, P(R, body={"r": [2.0, by(26), -4.0], "p": [0.0, -1.0, 0.0]},
             rightArm={"r": [-142.0, -18.0, 26.0], "p": [0.0, -1.0, -2.0]},
             leftArm={"r": [-46.0, -14.0, -48.0], "p": [0.0, 0.0, -1.0]},
             head={"r": [-8.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.24, P(R, body={"r": [10.0, by(-16), 6.0], "p": [0.0, -1.0, 0.0]},
             rightArm={"r": [-58.0, 22.0, 14.0], "p": [0.0, -1.0, 2.0]},
             leftArm={"r": [-20.0, 6.0, -44.0]},
             head={"r": [2.0, 24.6, -4.6]}), "easeInQuad"),
    (0.45, P(R, body={"r": [6.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]},
             rightArm={"r": [-20.0, 44.0, 22.6]}), "easeOutSine"),
], upper_body_only=True)

# Roll: a real forward roll with a full body revolution. Length matches the committed motion duration the
# brain uses, so the clip and the translation start and stop together.
phased("animation.v2.trickster.roll", [
    (0.0, P(R, body={"r": [8.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.12, P(R, body={"r": [46.0, by(0), 0.0], "p": [0.0, -3.0, 0.0]},
             rightArm={"r": [-60.0, 20.0, 14.0]},
             leftArm={"r": [-52.0, -18.0, -20.0]},
             leftLeg={"r": [52.0, 5.9, -16.5]},
             rightLeg={"r": [30.0, 33.8, -2.7], "p": [-1.0, 0.0, 0.0]},
             head={"r": [20.0, 24.6, -4.6]}), "easeInQuad"),
    (0.36, P(R, body={"r": [200.0, by(0), 0.0], "p": [0.0, 2.0, -1.0]},
             rightArm={"r": [-120.0, 10.0, 10.0]},
             leftArm={"r": [-114.0, -10.0, -10.0]},
             leftLeg={"r": [96.0, 5.9, -16.5]},
             rightLeg={"r": [88.0, 33.8, -2.7], "p": [-1.0, 0.0, 0.0]}), "linear"),
    (0.54, P(R, body={"r": [352.0, by(0), 0.0], "p": [0.0, -2.0, 1.0]},
             rightArm={"r": [-40.0, 24.0, 16.0]},
             leftArm={"r": [-34.0, -20.0, -24.0]},
             leftLeg={"r": [44.0, 5.9, -16.5]},
             rightLeg={"r": [26.0, 33.8, -2.7], "p": [-1.0, 0.0, 0.0]},
             head={"r": [16.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.75, P(R, body={"r": [368.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]},
             rightArm={"r": [-20.0, 44.0, 22.6]}), "easeOutSine"),
], 3, 13, 3, 0.12)


def trickster_dodge(name, sign):
    """A lateral evade — a hop and a twist, not a forward roll. ``sign`` +1 goes to its left."""
    s = sign
    clip(name, 0.5, [
        (0.0, P(R, body={"r": [6.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]}), None),
        (0.1, P(R, body={"r": [4.0, by(6 + 18 * s), -26.0 * s], "p": [0.0, 1.5, 0.0]},
                leftLeg={"r": [-14.0, 5.9, -16.5 - 24.0 * s]},
                rightLeg={"r": [-10.0, 33.8, -2.7 - 24.0 * s], "p": [-1.0, 1.0, 0.0]},
                rightArm={"r": [-44.0, 44.0, 22.6 + 16.0 * s]},
                leftArm={"r": [-30.0, -9.1, -50.2 - 10.0 * s]},
                head={"r": [-8.0, 24.6, -4.6 - 10.0 * s]}), "easeOutQuad"),
        (0.26, P(R, body={"r": [8.0, by(6 + 26 * s), -34.0 * s], "p": [0.0, 0.0, 0.0]},
                 leftLeg={"r": [12.0, 5.9, -16.5 - 34.0 * s]},
                 rightLeg={"r": [8.0, 33.8, -2.7 - 34.0 * s], "p": [-1.0, 1.0, 0.0]},
                 rightArm={"r": [-30.0, 44.0, 22.6 + 10.0 * s]},
                 leftArm={"r": [-16.0, -9.1, -50.2 - 6.0 * s]}), "easeInQuad"),
        (0.5, P(R, body={"r": [6.0, by(6), 0.0], "p": [0.0, -1.0, 0.0]},
                rightArm={"r": [-20.0, 44.0, 22.6]}), "easeOutSine"),
    ])


trickster_dodge("animation.v2.trickster.dodge_left", -1)
trickster_dodge("animation.v2.trickster.dodge_right", 1)

# ==================================================================================================
# MERCENARY / ARTILLERIST — disciplined relocation and firing positions. Crossbow on itemBone.
# ==================================================================================================

M = base_pose(ARMS_XBOW)

# Firing stance: bladed to the target, crossbow up, almost motionless. Only breathing.
loop_clip("animation.v2.merc.stance", 2.6, [
    (0.0, P(M, body={"r": [4.0, by(12), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (1.3, P(M, body={"r": [5.0, by(12), 0.0], "p": [0.0, -0.5, 0.0]},
            rightArm={"r": [-64.0, -36.2, 9.3], "p": [0.0, -2.0, -3.0]},
            leftArm={"r": [-50.0, 31.4, -38.3], "p": [0.0, 0.0, 2.0]}), None),
    (2.6, P(M, body={"r": [4.0, by(12), 0.0], "p": [0.0, -1.0, 0.0]}), None),
])

# Tactical sprint: crossbow carried down and across so it does not obscure the run. Fast cycle, long
# strides — this is the movement the relocation reads on.
loop_clip("animation.v2.merc.advance", 0.5, [
    (0.0, P(M, body={"r": [16.0, by(4), -3.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [48.0, 5.9, -16.5]},
            rightLeg={"r": [-42.0, 33.8, -2.7], "p": [-1.0, 2.0, -3.0]},
            rightArm={"r": [-40.0, -30.0, 12.0], "p": [0.0, -2.0, -2.0]},
            leftArm={"r": [-30.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]},
            itemBone={"r": [-16.0, -1.9, 62.0], "p": [2.0, -1.0, -0.3]}), None),
    (0.125, P(M, body={"r": [18.0, by(4), 0.0], "p": [0.0, 0.0, 0.0]},
              leftLeg={"r": [8.0, 5.9, -16.5]},
              rightLeg={"r": [-6.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
              rightArm={"r": [-46.0, -30.0, 12.0], "p": [0.0, -2.0, -2.0]},
              leftArm={"r": [-36.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]},
              itemBone={"r": [-16.0, -1.9, 62.0], "p": [2.0, -1.0, -0.3]}), None),
    (0.25, P(M, body={"r": [16.0, by(4), 3.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-38.0, 5.9, -16.5]},
             rightLeg={"r": [50.0, 33.8, -2.7], "p": [-1.0, 2.0, -3.0]},
             rightArm={"r": [-52.0, -30.0, 12.0], "p": [0.0, -2.0, -2.0]},
             leftArm={"r": [-42.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]},
             itemBone={"r": [-16.0, -1.9, 62.0], "p": [2.0, -1.0, -0.3]}), None),
    (0.375, P(M, body={"r": [18.0, by(4), 0.0], "p": [0.0, 0.0, 0.0]},
              leftLeg={"r": [6.0, 5.9, -16.5]},
              rightLeg={"r": [-4.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
              rightArm={"r": [-46.0, -30.0, 12.0], "p": [0.0, -2.0, -2.0]},
              leftArm={"r": [-36.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]},
              itemBone={"r": [-16.0, -1.9, 62.0], "p": [2.0, -1.0, -0.3]}), None),
    (0.5, P(M, body={"r": [16.0, by(4), -3.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [48.0, 5.9, -16.5]},
            rightLeg={"r": [-42.0, 33.8, -2.7], "p": [-1.0, 2.0, -3.0]},
            rightArm={"r": [-40.0, -30.0, 12.0], "p": [0.0, -2.0, -2.0]},
            leftArm={"r": [-30.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]},
            itemBone={"r": [-16.0, -1.9, 62.0], "p": [2.0, -1.0, -0.3]}), None),
])


def merc_lateral(name, sign):
    """Sidestep with the crossbow kept on target — the shooter never breaks its aim to reposition."""
    lead, trail = ("leftLeg", "rightLeg") if sign > 0 else ("rightLeg", "leftLeg")
    lead_base = LLEG if sign > 0 else RLEG
    trail_base = RLEG if sign > 0 else LLEG
    loop_clip(name, 0.5, [
        (0.0, P(M, body={"r": [4.0, by(12), -5.0 * sign], "p": [0.0, -1.0, 0.0]},
                **{lead: {"r": [lead_base[0] - 4.0, lead_base[1], lead_base[2] - 14.0 * sign]},
                   trail: {"r": [trail_base[0] + 4.0, trail_base[1], trail_base[2] + 2.0 * sign]}}), None),
        (0.25, P(M, body={"r": [5.0, by(12), -11.0 * sign], "p": [0.0, 0.0, 0.0]},
                 **{lead: {"r": [lead_base[0] - 12.0, lead_base[1], lead_base[2] - 32.0 * sign]},
                    trail: {"r": [trail_base[0] + 12.0, trail_base[1], trail_base[2] + 12.0 * sign]}}), None),
        (0.5, P(M, body={"r": [4.0, by(12), -5.0 * sign], "p": [0.0, -1.0, 0.0]},
                **{lead: {"r": [lead_base[0] - 4.0, lead_base[1], lead_base[2] - 14.0 * sign]},
                   trail: {"r": [trail_base[0] + 4.0, trail_base[1], trail_base[2] + 2.0 * sign]}}), None),
    ])


merc_lateral("animation.v2.merc.lateral_left", -1)
merc_lateral("animation.v2.merc.lateral_right", 1)

loop_clip("animation.v2.merc.backstep", 0.55, [
    (0.0, P(M, body={"r": [-2.0, by(12), 0.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-18.0, 5.9, -16.5]},
            rightLeg={"r": [18.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]}), None),
    (0.275, P(M, body={"r": [-3.0, by(12), 0.0], "p": [0.0, -0.3, 0.0]},
              leftLeg={"r": [18.0, 5.9, -16.5]},
              rightLeg={"r": [-16.0, 33.8, -2.7], "p": [-1.0, 1.5, 1.0]}), None),
    (0.55, P(M, body={"r": [-2.0, by(12), 0.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-18.0, 5.9, -16.5]},
             rightLeg={"r": [18.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]}), None),
])

# Plant: the transition from sprint to firing position. Skid, drop the weight, square up, weapon comes up.
# This is the visual answer to "arrive, hard arrest, begin firing".
clip("animation.v2.merc.plant", 0.5, [
    (0.0, P(M, body={"r": [17.0, by(4), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-46.0, -30.0, 12.0], "p": [0.0, -2.0, -2.0]},
            leftArm={"r": [-36.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]},
            itemBone={"r": [-16.0, -1.9, 62.0], "p": [2.0, -1.0, -0.3]}), None),
    (0.18, P(M, body={"r": [-6.0, by(18), 0.0], "p": [0.0, -3.0, 0.0]},
             leftLeg={"r": [42.0, 5.9, -24.0], "p": [0.0, -1.0, -2.0]},
             rightLeg={"r": [-34.0, 33.8, 6.0], "p": [-1.0, 0.0, 2.0]},
             rightArm={"r": [-54.0, -36.2, 9.3], "p": [0.0, -2.0, -3.0]},
             leftArm={"r": [-42.0, 31.4, -38.3], "p": [0.0, 0.0, 2.0]},
             head={"r": [-16.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.5, P(M, body={"r": [4.0, by(12), 0.0], "p": [0.0, -1.0, 0.0]}), "easeOutSine"),
])

# Heavy aimed shot: settle, hold, release, absorb the recoil through the shoulder. The hold is the
# telegraph and it is not shortened anywhere.
phased("animation.v2.merc.aim_heavy", [
    (0.0, P(M), None),
    (0.35, P(M, body={"r": [2.0, by(20), 0.0], "p": [0.0, -1.5, 0.0]},
             rightArm={"r": [-78.0, -30.0, 6.0], "p": [0.0, -2.0, -2.0]},
             leftArm={"r": [-72.0, 26.0, -30.0], "p": [0.0, 0.0, 2.0]},
             leftLeg={"r": [16.0, 5.9, -22.0], "p": [0.0, 0.0, 0.0]},
             rightLeg={"r": [-16.0, 33.8, 4.0], "p": [-2.0, 1.0, 0.0]},
             itemBone={"r": [-2.2, -1.9, 78.0], "p": [2.0, -1.0, -0.3]},
             head={"r": [-6.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.75, P(M, body={"r": [2.0, by(20), 0.0], "p": [0.0, -1.5, 0.0]},
             rightArm={"r": [-80.0, -30.0, 6.0], "p": [0.0, -2.0, -2.0]},
             leftArm={"r": [-74.0, 26.0, -30.0], "p": [0.0, 0.0, 2.0]},
             leftLeg={"r": [16.0, 5.9, -22.0], "p": [0.0, 0.0, 0.0]},
             rightLeg={"r": [-16.0, 33.8, 4.0], "p": [-2.0, 1.0, 0.0]},
             itemBone={"r": [-2.2, -1.9, 78.0], "p": [2.0, -1.0, -0.3]},
             head={"r": [-6.0, 24.6, -4.6]}), "easeInOutSine"),
    (0.85, P(M, body={"r": [-6.0, by(24), 0.0], "p": [0.0, -1.0, 0.0]},
             rightArm={"r": [-92.0, -30.0, 6.0], "p": [0.0, -1.0, -4.0]},
             leftArm={"r": [-86.0, 26.0, -30.0], "p": [0.0, 1.0, 0.0]},
             itemBone={"r": [-14.0, -1.9, 78.0], "p": [2.0, -1.0, -0.3]},
             head={"r": [-14.0, 24.6, -4.6]}), "easeOutQuad"),
    (1.4, P(M), "easeInOutSine"),
], 17, 3, 8, 0.85)

# Four-shot burst: four distinct recoil beats on one hold, so the sequence is visible rather than implied.
BURST_HOLD = dict(
    body={"r": [2.0, by(18), 0.0], "p": [0.0, -1.2, 0.0]},
    rightArm={"r": [-76.0, -32.0, 8.0], "p": [0.0, -2.0, -2.0]},
    leftArm={"r": [-70.0, 28.0, -32.0], "p": [0.0, 0.0, 2.0]},
    leftLeg={"r": [16.0, 5.9, -22.0]},
    rightLeg={"r": [-16.0, 33.8, 4.0], "p": [-2.0, 1.0, 0.0]},
    itemBone={"r": [-2.2, -1.9, 76.0], "p": [2.0, -1.0, -0.3]},
)
BURST_KICK = dict(
    body={"r": [-4.0, by(22), 0.0], "p": [0.0, -1.0, 0.0]},
    rightArm={"r": [-88.0, -32.0, 8.0], "p": [0.0, -1.0, -4.0]},
    leftArm={"r": [-82.0, 28.0, -32.0], "p": [0.0, 1.0, 0.0]},
    leftLeg={"r": [16.0, 5.9, -22.0]},
    rightLeg={"r": [-16.0, 33.8, 4.0], "p": [-2.0, 1.0, 0.0]},
    itemBone={"r": [-16.0, -1.9, 76.0], "p": [2.0, -1.0, -0.3]},
)
burst_frames = [(0.0, P(M), None), (0.4, P(M, **BURST_HOLD), "easeOutQuad")]
for index in range(4):
    shot = 0.55 + index * 0.3
    burst_frames.append((shot, P(M, **BURST_KICK), "easeOutQuad"))
    burst_frames.append((shot + 0.16, P(M, **BURST_HOLD), "easeInOutSine"))
burst_frames.append((2.1, P(M), "easeInOutSine"))
# Contact is the first shot; the four recoil beats above are already spaced to land on the action's shot
# ticks (3, 9, 15, 21 of the active window) once phased() has scaled them.
phased("animation.v2.merc.burst", burst_frames, 8, 24, 12, 0.4)

# Trap throw, authored to be legible mid-sprint: only the off hand and torso move, so the running legs
# from the locomotion controller still read underneath it.
phased("animation.v2.merc.trap_throw", [
    (0.0, P(M, body={"r": [16.0, by(4), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.14, P(M, body={"r": [14.0, by(22), -6.0], "p": [0.0, -1.0, 0.0]},
             leftArm={"r": [-118.0, 10.0, -20.0], "p": [0.0, 0.0, -2.0]},
             head={"r": [-4.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.26, P(M, body={"r": [18.0, by(-8), 6.0], "p": [0.0, -1.0, 0.0]},
             leftArm={"r": [-30.0, 40.0, -46.0], "p": [0.0, 0.0, 3.0]}), "easeInQuad"),
    (0.5, P(M, body={"r": [16.0, by(4), 0.0], "p": [0.0, -1.0, 0.0]},
            leftArm={"r": [-30.0, 26.0, -30.0], "p": [0.0, 0.0, 1.0]}), "easeOutSine"),
], 3, 3, 5, 0.14, upper_body_only=True)

# Suppressing snap shot: fired from the hip while still moving into position. Short, cheap, low commitment.
phased("animation.v2.merc.snap_shot", [
    (0.0, P(M, body={"r": [10.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.12, P(M, body={"r": [6.0, by(22), 0.0], "p": [0.0, -1.0, 0.0]},
             rightArm={"r": [-58.0, -34.0, 10.0], "p": [0.0, -2.0, -2.0]},
             itemBone={"r": [-8.0, -1.9, 72.0], "p": [2.0, -1.0, -0.3]}), "easeOutQuad"),
    (0.2, P(M, body={"r": [2.0, by(26), 0.0], "p": [0.0, -1.0, 0.0]},
            rightArm={"r": [-70.0, -34.0, 10.0], "p": [0.0, -1.0, -4.0]},
            itemBone={"r": [-20.0, -1.9, 72.0], "p": [2.0, -1.0, -0.3]}), "easeOutQuad"),
    (0.4, P(M, body={"r": [10.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]}), "easeInOutSine"),
], 4, 2, 4, 0.2, upper_body_only=True)

# ==================================================================================================
# FIRE MAGE — elastic spacing and mobile casting. Staff in the right hand, casting hand free.
# ==================================================================================================

F = base_pose(ARMS_STAFF)

loop_clip("animation.v2.mage.stance", 2.2, [
    (0.0, P(F, body={"r": [3.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (1.1, P(F, body={"r": [5.0, by(10), 0.0], "p": [0.0, -0.4, 0.0]},
            leftArm={"r": [-30.0, -14.0, -44.0]},
            rightArm={"r": [-42.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
    (2.2, P(F, body={"r": [3.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]}), None),
])

loop_clip("animation.v2.mage.advance", 0.6, [
    (0.0, P(F, body={"r": [9.0, by(8), -2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [32.0, 5.9, -16.5]},
            rightLeg={"r": [-28.0, 33.8, -2.7], "p": [-1.0, 1.5, -1.0]},
            rightArm={"r": [-44.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.15, P(F, body={"r": [10.0, by(8), 0.0], "p": [0.0, -0.3, 0.0]},
             leftLeg={"r": [6.0, 5.9, -16.5]},
             rightLeg={"r": [-4.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.3, P(F, body={"r": [9.0, by(8), 2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-26.0, 5.9, -16.5]},
            rightLeg={"r": [34.0, 33.8, -2.7], "p": [-1.0, 1.5, -1.0]},
            rightArm={"r": [-34.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.45, P(F, body={"r": [10.0, by(8), 0.0], "p": [0.0, -0.3, 0.0]},
             leftLeg={"r": [4.0, 5.9, -16.5]},
             rightLeg={"r": [-2.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]}), None),
    (0.6, P(F, body={"r": [9.0, by(8), -2.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [32.0, 5.9, -16.5]},
            rightLeg={"r": [-28.0, 33.8, -2.7], "p": [-1.0, 1.5, -1.0]},
            rightArm={"r": [-44.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
])


def mage_lateral(name, sign):
    """Tangential drift. Upper body stays open to the target so a cast can start on any frame."""
    lead, trail = ("leftLeg", "rightLeg") if sign > 0 else ("rightLeg", "leftLeg")
    lead_base = LLEG if sign > 0 else RLEG
    trail_base = RLEG if sign > 0 else LLEG
    loop_clip(name, 0.55, [
        (0.0, P(F, body={"r": [4.0, by(8), -7.0 * sign], "p": [0.0, -1.0, 0.0]},
                **{lead: {"r": [lead_base[0] - 4.0, lead_base[1], lead_base[2] - 16.0 * sign]},
                   trail: {"r": [trail_base[0] + 4.0, trail_base[1], trail_base[2] + 4.0 * sign]}}), None),
        (0.275, P(F, body={"r": [5.0, by(8), -14.0 * sign], "p": [0.0, 0.2, 0.0]},
                  leftArm={"r": [-28.0, -14.0, -46.0 - 8.0 * sign]},
                  **{lead: {"r": [lead_base[0] - 14.0, lead_base[1], lead_base[2] - 34.0 * sign]},
                     trail: {"r": [trail_base[0] + 14.0, trail_base[1], trail_base[2] + 14.0 * sign]}}), None),
        (0.55, P(F, body={"r": [4.0, by(8), -7.0 * sign], "p": [0.0, -1.0, 0.0]},
                 **{lead: {"r": [lead_base[0] - 4.0, lead_base[1], lead_base[2] - 16.0 * sign]},
                    trail: {"r": [trail_base[0] + 4.0, trail_base[1], trail_base[2] + 4.0 * sign]}}), None),
    ])


mage_lateral("animation.v2.mage.lateral_left", -1)
mage_lateral("animation.v2.mage.lateral_right", 1)

loop_clip("animation.v2.mage.backstep", 0.55, [
    (0.0, P(F, body={"r": [-4.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]},
            leftLeg={"r": [-18.0, 5.9, -16.5]},
            rightLeg={"r": [18.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
            rightArm={"r": [-30.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.275, P(F, body={"r": [-5.0, by(8), 0.0], "p": [0.0, -0.2, 0.0]},
              leftLeg={"r": [18.0, 5.9, -16.5]},
              rightLeg={"r": [-16.0, 33.8, -2.7], "p": [-1.0, 1.5, 1.0]},
              rightArm={"r": [-26.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.55, P(F, body={"r": [-4.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-18.0, 5.9, -16.5]},
             rightLeg={"r": [18.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
             rightArm={"r": [-30.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]}), None),
])

# Quick cast: a flick of the free hand. Legs are left near neutral so the lateral clip still reads through
# it and the mage never has to stop moving to throw a fireball.
clip("animation.v2.mage.cast_quick", 0.4, [
    (0.0, P(F, body={"r": [4.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]}), None),
    (0.12, P(F, body={"r": [1.0, by(20), 0.0], "p": [0.0, -1.0, 0.0]},
             leftArm={"r": [-116.0, -26.0, -30.0], "p": [0.0, 0.0, -1.0]},
             head={"r": [-8.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.22, P(F, body={"r": [8.0, by(-6), 0.0], "p": [0.0, -1.0, 0.0]},
             leftArm={"r": [-72.0, 6.0, -22.0], "p": [0.0, 0.0, 2.0]},
             head={"r": [0.0, 24.6, -4.6]}), "easeInQuad"),
    (0.4, P(F, body={"r": [4.0, by(8), 0.0], "p": [0.0, -1.0, 0.0]},
            leftArm={"r": [-24.0, -14.0, -46.0]}), "easeOutSine"),
], upper_body_only=True)

# Long cast: staff raised, free hand gathering, held while the legs keep drifting. The release is a
# forward drive of the staff. Deliberately long: this is the readable, punishable one.
phased("animation.v2.mage.cast_long", [
    (0.0, P(F), None),
    (0.6, P(F, body={"r": [-4.0, by(14), 0.0], "p": [0.0, -0.5, 0.0]},
            rightArm={"r": [-138.0, 26.0, 14.0], "p": [0.0, -1.0, -2.0]},
            leftArm={"r": [-96.0, -20.0, -24.0], "p": [0.0, 0.0, 0.0]},
            head={"r": [-24.0, 24.6, -4.6]}), "easeOutQuad"),
    (1.4, P(F, body={"r": [-6.0, by(14), 0.0], "p": [0.0, -0.2, 0.0]},
            rightArm={"r": [-150.0, 24.0, 12.0], "p": [0.0, -1.0, -2.0]},
            leftArm={"r": [-104.0, -22.0, -22.0], "p": [0.0, 0.0, 0.0]},
            head={"r": [-28.0, 24.6, -4.6]}), "easeInOutSine"),
    (2.4, P(F, body={"r": [-6.0, by(14), 0.0], "p": [0.0, -0.2, 0.0]},
            rightArm={"r": [-156.0, 24.0, 12.0], "p": [0.0, -1.0, -2.0]},
            leftArm={"r": [-110.0, -22.0, -22.0], "p": [0.0, 0.0, 0.0]},
            head={"r": [-30.0, 24.6, -4.6]}), "easeInOutSine"),
    (2.7, P(F, body={"r": [14.0, by(4), 0.0], "p": [0.0, -1.5, 0.0]},
            rightArm={"r": [-84.0, 34.0, 20.0], "p": [0.0, -1.0, 3.0]},
            leftArm={"r": [-70.0, -8.0, -30.0], "p": [0.0, 0.0, 3.0]},
            head={"r": [8.0, 24.6, -4.6]}), "easeInQuad"),
    (3.2, P(F), "easeOutSine"),
], 54, 6, 14, 2.7)

# Nova: gather low, then throw both arms out. Fully planted — the mage buys the ring with its mobility.
phased("animation.v2.mage.nova", [
    (0.0, P(F), None),
    (0.5, P(F, body={"r": [22.0, by(0), 0.0], "p": [0.0, -3.0, 0.0]},
            rightArm={"r": [-24.0, 20.0, 34.0], "p": [0.0, -1.0, 3.0]},
            leftArm={"r": [-20.0, -20.0, -34.0], "p": [0.0, -1.0, 3.0]},
            leftLeg={"r": [24.0, 5.9, -26.0], "p": [0.0, -1.0, 0.0]},
            rightLeg={"r": [-20.0, 33.8, 10.0], "p": [-2.0, 0.0, 0.0]},
            head={"r": [14.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.72, P(F, body={"r": [26.0, by(0), 0.0], "p": [0.0, -3.5, 0.0]},
             rightArm={"r": [-16.0, 18.0, 38.0], "p": [0.0, -1.0, 4.0]},
             leftArm={"r": [-12.0, -18.0, -38.0], "p": [0.0, -1.0, 4.0]},
             leftLeg={"r": [26.0, 5.9, -28.0], "p": [0.0, -1.0, 0.0]},
             rightLeg={"r": [-22.0, 33.8, 12.0], "p": [-2.0, 0.0, 0.0]},
             head={"r": [18.0, 24.6, -4.6]}), "easeInOutSine"),
    (0.85, P(F, body={"r": [-18.0, by(0), 0.0], "p": [0.0, 1.5, 0.0]},
             rightArm={"r": [-132.0, 4.0, 74.0], "p": [0.0, 0.0, 0.0]},
             leftArm={"r": [-128.0, -4.0, -74.0], "p": [0.0, 0.0, 0.0]},
             leftLeg={"r": [-6.0, 5.9, -22.0]},
             rightLeg={"r": [4.0, 33.8, 8.0], "p": [-1.0, 1.0, 0.0]},
             head={"r": [-26.0, 24.6, -4.6]}), "easeOutQuad"),
    (1.5, P(F), "easeInOutSine"),
], 16, 3, 11, 0.85)

# Jump back: a short crouch, a diagonal launch, an airborne tuck, and a landing that ends already facing
# the target. Timed to match the committed motion the brain drives.
phased("animation.v2.mage.jump_back", [
    (0.0, P(F), None),
    (0.2, P(F, body={"r": [20.0, by(8), 0.0], "p": [0.0, -3.0, 0.0]},
            leftLeg={"r": [40.0, 5.9, -24.0], "p": [0.0, -1.0, 0.0]},
            rightLeg={"r": [-34.0, 33.8, 6.0], "p": [-1.0, 0.0, 0.0]},
            leftArm={"r": [-8.0, -14.0, -40.0]},
            rightArm={"r": [-18.0, 34.0, 20.0], "p": [0.0, -1.0, 0.0]},
            head={"r": [4.0, 24.6, -4.6]}), "easeInQuad"),
    (0.36, P(F, body={"r": [-16.0, by(8), 0.0], "p": [0.0, 2.0, 0.0]},
             leftLeg={"r": [-24.0, 5.9, -16.5]},
             rightLeg={"r": [-30.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
             leftArm={"r": [-92.0, -22.0, -28.0]},
             rightArm={"r": [-76.0, 30.0, 18.0], "p": [0.0, -1.0, 0.0]},
             head={"r": [-18.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.6, P(F, body={"r": [-10.0, by(8), 0.0], "p": [0.0, 1.5, 0.0]},
            leftLeg={"r": [26.0, 5.9, -16.5]},
            rightLeg={"r": [10.0, 33.8, -2.7], "p": [-1.0, 1.0, 0.0]},
            leftArm={"r": [-60.0, -18.0, -34.0]},
            rightArm={"r": [-52.0, 32.0, 20.0], "p": [0.0, -1.0, 0.0]}), "easeInOutSine"),
    (0.74, P(F, body={"r": [18.0, by(8), 0.0], "p": [0.0, -2.5, 0.0]},
             leftLeg={"r": [38.0, 5.9, -22.0], "p": [0.0, -1.0, 0.0]},
             rightLeg={"r": [-30.0, 33.8, 4.0], "p": [-1.0, 0.0, 0.0]},
             head={"r": [8.0, 24.6, -4.6]}), "easeInQuad"),
    (0.9, P(F), "easeOutSine"),
], 4, 12, 3, 0.2)

# Flame sweep: a short, wide arc of the free hand across the mage's front. Exists purely to make standing
# in contact range unprofitable, so it is fast, cheap and carries a small backward step.
phased("animation.v2.mage.flame_sweep", [
    (0.0, P(F), None),
    (0.2, P(F, body={"r": [2.0, by(34), -6.0], "p": [0.0, -1.0, 0.0]},
            leftArm={"r": [-104.0, -40.0, -18.0], "p": [0.0, 0.0, -2.0]},
            rightArm={"r": [-30.0, 40.0, 24.0], "p": [0.0, -1.0, 0.0]},
            head={"r": [-6.0, 24.6, -4.6]}), "easeOutQuad"),
    (0.36, P(F, body={"r": [6.0, by(-36), 8.0], "p": [0.0, -1.0, 0.0]},
             leftArm={"r": [-92.0, 44.0, -62.0], "p": [0.0, 0.0, 2.0]},
             rightArm={"r": [-24.0, 28.0, 18.0], "p": [0.0, -1.0, 0.0]},
             leftLeg={"r": [-14.0, 5.9, -16.5]},
             rightLeg={"r": [16.0, 33.8, -2.7], "p": [-1.0, 1.0, 1.0]},
             head={"r": [2.0, 24.6, -4.6]}), "easeInQuad"),
    (0.7, P(F), "easeOutSine"),
], 8, 4, 8, 0.36)


# --- output ---------------------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true",
                        help="actually write mobs_v2.animations.json (otherwise dry run)")
    parser.add_argument("--force", action="store_true",
                        help="overwrite even if the output has been hand-edited since the last run "
                             "(a timestamped backup is written first)")
    args = parser.parse_args()

    with open(LEGACY, "r", encoding="utf-8") as handle:
        legacy = json.load(handle)

    merged = OrderedDict()
    # Legacy clips are copied forward verbatim so the v2 resource is self-contained and every existing
    # animation name still resolves. The legacy FILE is never written to; see the module docstring.
    for name, body in legacy.get("animations", {}).items():
        merged[name] = body
    overlaps = [name for name in CLIPS if name in merged]
    if overlaps:
        raise SystemExit("v2 clip names collide with legacy clips: " + ", ".join(overlaps))
    merged.update(CLIPS)

    document = OrderedDict()
    document["format_version"] = legacy.get("format_version", "1.8.0")
    document["animations"] = merged

    print("legacy clips carried forward: %d" % len(legacy.get("animations", {})))
    print("new v2 clips authored:        %d" % len(CLIPS))
    squeezed = {n: c for n, c in COMPRESSION.items() if min(c) < 0.9}
    if squeezed:
        print("\nclips played faster than authored (windup, recovery):")
        for name, factors in sorted(squeezed.items(), key=lambda kv: min(kv[1])):
            print("  %-40s %s" % (name, factors))
    for name in CLIPS:
        print("  " + name)

    if not args.write:
        print("\ndry run — pass --write to emit %s" % os.path.normpath(OUTPUT))
        return

    if os.path.abspath(OUTPUT) == os.path.abspath(LEGACY):
        raise SystemExit("refusing to overwrite the legacy animation file")

    payload = json.dumps(document, indent=1) + "\n"
    # Never clobber hand edits. See guard_hand_edits and the SAFETY note in the module docstring.
    guard_hand_edits(args.force, payload)

    with open(OUTPUT, "w", encoding="utf-8") as handle:
        handle.write(payload)
    with open(STAMP, "w", encoding="utf-8") as handle:
        handle.write(hashlib.sha256(payload.encode("utf-8")).hexdigest())
    print("\nwrote %s" % os.path.normpath(OUTPUT))


if __name__ == "__main__":
    sys.exit(main())
