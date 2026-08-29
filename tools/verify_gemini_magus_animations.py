#!/usr/bin/env python3
"""Verify the animation wiring for the Gemini pair and Magus Prime.

Read-only. Checks, in order:

1. Every animation name named in Java resolves to a clip in the resource that boss's animator
   points at, and every controller a command targets is actually registered.
2. Every bone a v2 clip animates exists on the model that boss renders with.
3. Keyframes are in ascending time order and no keyframe sits past the clip's declared length.
4. The legacy resources are still present and still contain every clip they originally held, and
   every one of those clips was carried forward into the v2 resource.
5. The release ticks the entity code schedules line up with the key poses in the clip it plays.

Run from anywhere: ``python tools/verify_gemini_magus_animations.py``. Exits non-zero on failure.
"""

from __future__ import annotations

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "rpg-minibosses")
JAVA = os.path.join(ROOT, "common", "src", "main", "java", "com", "cleannrooster", "rpg_minibosses")

BOSSES = {
    "gemini": {
        "active": os.path.join(ASSETS, "animations", "gemini_v2.animations.json"),
        "legacy": [os.path.join(ASSETS, "animations", "gemini.animations.json"),
                   os.path.join(ASSETS, "animations", "gemini.animation.json")],
        "model": os.path.join(ASSETS, "geo", "gemini.geo.json"),
        "sources": [os.path.join(JAVA, "entity", "GeminiEntity.java"),
                    os.path.join(JAVA, "client", "entity", "renderer", "GeminiAnimationProvider.java")],
        "provider": os.path.join(JAVA, "client", "entity", "renderer", "GeminiAnimationProvider.java"),
        "prefix": "animation.awakener.",
    },
    "magus": {
        "active": os.path.join(ASSETS, "animations", "magus_v2.animations.json"),
        "legacy": [os.path.join(ASSETS, "animations", "magus.animations.json")],
        "model": os.path.join(ASSETS, "geo", "magus_prime.geo.json"),
        "sources": [os.path.join(JAVA, "entity", "MagusPrimeEntity.java"),
                    os.path.join(JAVA, "client", "entity", "renderer", "MagusPrimeAnimationProvider.java")],
        "provider": os.path.join(JAVA, "client", "entity", "renderer", "MagusPrimeAnimationProvider.java"),
        "prefix": "animation.magus.",
    },
}

# Clip -> {tick: what the entity code does on that tick}. Ticks are relative to the dispatch of the
# clip, at 20 ticks per second. A clip only needs a keyframe AT that tick on the bone doing the
# work; this checks the clip has a key pose there at all, which is what catches a release drifting
# away from the pose that is supposed to throw it.
RELEASE_BEATS = {
    "animation.awakener.v2.beam_snap": [10],
    "animation.awakener.v2.beam_snap_frost": [10],
    "animation.awakener.v2.beam_channel": [20, 28, 36],
    "animation.awakener.v2.beam_channel_frost": [20, 28, 36],
    "animation.awakener.v2.comet_call": [40, 60, 80, 100],
    "animation.awakener.v2.comet_call_frost": [40, 60, 80, 100],
    "animation.awakener.v2.cloud_spread": [40, 60, 80, 100],
    "animation.awakener.v2.cloud_spread_frost": [40, 60, 80, 100],
    "animation.magus.v2.cast.quick": [5],
    "animation.magus.v2.cast.quick_nova": [5],
    "animation.magus.v2.cast.heavy": [40],
    "animation.magus.v2.cast.nova": [40],
    "animation.magus.v2.cast.channel": [40],
}

# Clip -> the action-lock length, in ticks, the entity holds while it plays. The lock must not end
# before the clip's release, or the boss would act again mid-telegraph.
LOCK_TICKS = {
    "animation.magus.v2.cast.quick": 10,   # phase 3 is the shortest lock of the three
    "animation.magus.v2.cast.quick_nova": 10,
    "animation.magus.v2.cast.heavy": 48,   # beginCast(58) minus the 10-tick lead-in
    "animation.magus.v2.cast.nova": 48,
    "animation.magus.v2.cast.channel": 48,
    "animation.magus.v2.phase_transition": 60,
}

failures = []


def fail(message):
    failures.append(message)
    print("FAIL: " + message)


def load(path):
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def read(path):
    with open(path, "r", encoding="utf-8") as handle:
        return handle.read()


def model_bones(path):
    document = load(path)
    names = set()
    for geometry in document["minecraft:geometry"]:
        for bone in geometry["bones"]:
            names.add(bone["name"])
    return names


def keyframe_times(channel):
    times = []
    for key in channel:
        try:
            times.append(float(key))
        except ValueError:
            times.append(0.0)  # a bare {"vector": ...} channel is a single static value
    return times


ANIMATION_NAME = re.compile(r'"(animation\.[A-Za-z0-9_.]+)"')
CONTROLLER_DECL = re.compile(r'builder\(this,\s*"([A-Za-z0-9_]+)"\s*\)')
COMMAND_CREATE = re.compile(r'AzCommand\.create\(\s*"([A-Za-z0-9_]+)"\s*,\s*"(animation\.[A-Za-z0-9_.]+)"')


def check(name, spec):
    print("\n=== {} ===".format(name))
    active = load(spec["active"])["animations"]
    print("  active resource: {} ({} clips)".format(os.path.basename(spec["active"]), len(active)))

    # 1. Names referenced from Java resolve, and their controllers exist.
    referenced = set()
    for source in spec["sources"]:
        for match in ANIMATION_NAME.findall(read(source)):
            if match.startswith(spec["prefix"]):
                referenced.add(match)
    for clip in sorted(referenced):
        if clip not in active:
            fail("{}: Java references {} but it is not in {}".format(
                name, clip, os.path.basename(spec["active"])))
    print("  {} clip names referenced from Java, all resolve".format(len(referenced)))

    provider = read(spec["provider"])
    controllers = set(CONTROLLER_DECL.findall(provider))
    for controller, clip in COMMAND_CREATE.findall(provider):
        if controller not in controllers:
            fail("{}: command for {} targets controller '{}', which is not registered".format(
                name, clip, controller))
    print("  controllers registered: {}".format(", ".join(sorted(controllers))))

    # 2. Bones exist on the model.
    bones = model_bones(spec["model"])
    for clip, body in active.items():
        for bone in body.get("bones", {}):
            if bone not in bones:
                fail("{}: clip {} animates bone '{}', absent from {}".format(
                    name, clip, bone, os.path.basename(spec["model"])))
    print("  every animated bone exists on {}".format(os.path.basename(spec["model"])))

    # 3. Ordering and length.
    for clip, body in active.items():
        length = float(body.get("animation_length", 0))
        for bone, channels in body.get("bones", {}).items():
            for channel, frames in channels.items():
                if not isinstance(frames, dict):
                    continue
                times = keyframe_times(frames)
                if times != sorted(times):
                    fail("{}: clip {} bone {} channel {} has out-of-order keyframes".format(
                        name, clip, bone, channel))
                if times and times[-1] > length + 1e-6:
                    fail("{}: clip {} bone {} channel {} keys at {}s, past its {}s length".format(
                        name, clip, bone, channel, times[-1], length))
    print("  keyframes ordered and inside every clip's declared length")

    # 4. Legacy preserved and carried forward.
    for legacy_path in spec["legacy"]:
        if not os.path.exists(legacy_path):
            fail("{}: legacy resource {} is missing — it is meant to be kept".format(
                name, os.path.basename(legacy_path)))
            continue
        legacy = load(legacy_path)["animations"]
        for clip in legacy:
            if clip not in active:
                fail("{}: legacy clip {} was not carried into the v2 resource".format(name, clip))
        print("  legacy {} present, all {} clips carried forward".format(
            os.path.basename(legacy_path), len(legacy)))

    # 5. Releases land on key poses, and locks cover them.
    for clip, beats in RELEASE_BEATS.items():
        if clip not in active:
            continue
        keyed = set()
        for channels in active[clip]["bones"].values():
            for frames in channels.values():
                if isinstance(frames, dict):
                    for time in keyframe_times(frames):
                        keyed.add(round(time * 20))
        for beat in beats:
            if beat not in keyed:
                fail("{}: clip {} resolves on tick {} but has no key pose there".format(
                    name, clip, beat))
    for clip, lock in LOCK_TICKS.items():
        if clip not in active:
            continue
        for beat in RELEASE_BEATS.get(clip, []):
            if beat > lock:
                fail("{}: clip {} releases on tick {} but the action lock is only {}".format(
                    name, clip, beat, lock))
    print("  release beats land on key poses and sit inside their action locks")


def main():
    for name, spec in BOSSES.items():
        check(name, spec)
    print("")
    if failures:
        print("{} failure(s).".format(len(failures)))
        return 1
    print("All checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
