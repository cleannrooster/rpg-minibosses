#!/usr/bin/env python3
"""Author the v2 animation resources for the Gemini pair and Magus Prime.

Both bosses already had strong, bespoke clips. This does NOT replace them: every clip in
``gemini.animations.json`` and ``magus.animations.json`` is copied forward verbatim into the
matching ``*_v2.animations.json``, and the legacy files stay on disk untouched as reference
material. The v2 files then add the authored clips that give each fight a readable visual
grammar — one distinct body shape per class of threat, with the key poses landing on the exact
ticks the entity code resolves the spell.

Why a script rather than hand-edited JSON: the frost twin's clips are exact mirrors of the fire
twin's, so authoring them by hand would mean maintaining the same motion twice.

**It will not overwrite hand edits.** After a successful write it records a checksum of what it
produced in ``tools/.<name>.stamp``. On the next run it re-checks that stamp: if the file on disk
is not byte-for-byte what this script last emitted, somebody has edited it by hand — in Blockbench,
most likely — and the script aborts rather than destroying that work. ``--force`` overrides, and
always writes a timestamped backup alongside the output first. A missing stamp counts as edited,
so a file this script has never stamped is never silently replaced.

This is the same guard ``gen_v2_animations.py`` carries, and it exists because it has happened:
regenerating on top of hand-tuned keyframes has destroyed work in this repo before.

    python tools/gen_gemini_magus_v2_animations.py            # dry run, prints a summary
    python tools/gen_gemini_magus_v2_animations.py --write    # create the v2 files
    python tools/gen_gemini_magus_v2_animations.py --write --force   # overwrite, after backing up

Rotation sign convention: see the header of ``tools/gen_v2_animations.py`` before changing any
number here. AzureLib parses Bedrock JSON with X and Y rotations negated and Z taken as-is, so the
poses below are anchored on values lifted from clips that already read correctly in game rather
than derived from first principles.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
ANIM = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "rpg-minibosses", "animations")

GEMINI_LEGACY = os.path.join(ANIM, "gemini.animations.json")
GEMINI_V2 = os.path.join(ANIM, "gemini_v2.animations.json")
MAGUS_LEGACY = os.path.join(ANIM, "magus.animations.json")
MAGUS_V2 = os.path.join(ANIM, "magus_v2.animations.json")


# ── Authoring helpers ────────────────────────────────────────────────────────
#
# A clip is authored as {bone: {channel: [(time, vector) | (time, vector, easing), ...]}}.
# Times are seconds, matching the Bedrock format the rest of the repo uses.


def track(keys):
    out = {}
    for key in keys:
        time, vector = key[0], key[1]
        frame = {"vector": [round(float(v), 4) for v in vector]}
        if len(key) > 2 and key[2]:
            frame["easing"] = key[2]
        out[format(float(time), "g")] = frame
    return out


def clip(length, bones, loop=False):
    out = {"animation_length": length,
           "bones": {bone: {channel: track(keys) for channel, keys in channels.items()}
                     for bone, channels in bones.items()}}
    if loop:
        out["loop"] = True
    return out


MIRROR_PAIRS = {
    "rightArm": "leftArm", "leftArm": "rightArm",
    "rightLeg": "leftLeg", "leftLeg": "rightLeg",
    "armorRightArm": "armorLeftArm", "armorLeftArm": "armorRightArm",
    "armorRightLeg": "armorLeftLeg", "armorLeftLeg": "armorRightLeg",
}


def mirror(source):
    """Mirror a clip across the model's X axis, so the frost twin leads with the other hand."""
    bones = {}
    for bone, channels in source["bones"].items():
        mirrored = {}
        for channel, frames in channels.items():
            out = {}
            for time, frame in frames.items():
                vector = list(frame["vector"])
                if channel == "rotation":
                    vector = [vector[0], -vector[1], -vector[2]]
                elif channel == "position":
                    vector = [-vector[0], vector[1], vector[2]]
                new_frame = {"vector": [round(v, 4) for v in vector]}
                if "easing" in frame:
                    new_frame["easing"] = frame["easing"]
                out[time] = new_frame
            mirrored[channel] = out
        bones[MIRROR_PAIRS.get(bone, bone)] = mirrored
    out = {"animation_length": source["animation_length"], "bones": bones}
    if source.get("loop"):
        out["loop"] = True
    return out


# ── Gemini ───────────────────────────────────────────────────────────────────
#
# Rest pose taken from animation.awakener.idle so every clip starts and ends on the silhouette
# the player already reads as "a suspended djinn", and only the middle of the clip carries the
# telegraph. Bones are those on gemini.geo.json.

G_BODY_POS = [0, 6, 0]
G_RIGHT_ARM = [0, -17.5, 35]
G_LEFT_ARM = [0, 17.5, -32.5]
G_RIGHT_ARM_POS = [-1, 0, 0]
G_LEFT_ARM_POS = [2, 0, 0]
G_LEFT_LEG = [15.2, -2.5, 9.7]
G_RIGHT_LEG = [10, 0.9, -4.9]
G_HEAD = [7.5, 0, 0]

SETTLE = "easeInOutSine"


def gemini_beam_snap():
    """Quick projectile pressure. One compact arm, release lands on tick 10 (0.5 s)."""
    return clip(1.5, {
        "body": {
            "rotation": [(0.0, [0, 0, 0]), (0.25, [-4, -14, 0]), (0.5, [-11, -16, 0]),
                         (0.75, [-4, -8, 0]), (1.5, [0, 0, 0], SETTLE)],
            "position": [(0.0, G_BODY_POS), (0.3, [0, 6.5, -3]), (0.5, [0, 6.5, 6]),
                         (0.75, [0, 6, 2]), (1.5, G_BODY_POS, SETTLE)],
        },
        # Right arm cocks back over 6 ticks, then snaps through on the release tick.
        "rightArm": {
            "rotation": [(0.0, G_RIGHT_ARM), (0.3, [-104, -12, 18]), (0.45, [-120, -6, 8]),
                         (0.5, [-74, -22, 14]), (0.75, [-52, -18, 24]),
                         (1.5, G_RIGHT_ARM, SETTLE)],
            "position": [(0.0, G_RIGHT_ARM_POS), (0.5, [-2, 1, -3]), (1.5, G_RIGHT_ARM_POS)],
        },
        "armorRightArm": {
            "rotation": [(0.0, [0, 0, 0]), (0.3, [24, 6, 42]), (0.5, [-38, 4, 58]),
                         (1.5, [0, 0, 0], SETTLE)],
        },
        "leftArm": {
            "rotation": [(0.0, G_LEFT_ARM), (0.3, [16, 24, -44]), (0.5, [22, 30, -50]),
                         (1.5, G_LEFT_ARM, SETTLE)],
            "position": [(0.0, G_LEFT_ARM_POS), (1.5, G_LEFT_ARM_POS)],
        },
        "leftLeg": {
            "rotation": [(0.0, G_LEFT_LEG), (0.5, [-14, -2.5, 9.7]), (1.5, G_LEFT_LEG, SETTLE)],
            "position": [(0.0, [1, 1, -1]), (1.5, [1, 1, -1])],
        },
        "rightLeg": {
            "rotation": [(0.0, G_RIGHT_LEG), (0.5, [-20, 0.9, -4.9]), (1.5, G_RIGHT_LEG, SETTLE)],
            "position": [(0.0, [-1, 1, -3]), (1.5, [-1, 1, -3])],
        },
        "head": {"rotation": [(0.0, G_HEAD), (0.3, [2, 12, 0]), (0.5, [-2, 14, 0]),
                              (1.5, G_HEAD, SETTLE)]},
    })


def gemini_beam_channel():
    """Sustained beam. Squares up by 0.5 s, then three releases on ticks 20 / 28 / 36."""
    align_right = [-92, -10, 14]
    align_left = [-92, 10, -14]
    return clip(3.0, {
        "body": {
            # Turned squarely onto the target for the whole channel — no yaw wander mid-beam.
            "rotation": [(0.0, [0, 0, 0]), (0.5, [-6, 0, 0]), (0.9, [-12, 0, 0]),
                         (1.0, [-4, 0, 0]), (1.4, [-4, 0, 0]), (1.8, [-4, 0, 0]),
                         (2.2, [-8, 0, 0]), (3.0, [0, 0, 0], SETTLE)],
            "position": [(0.0, G_BODY_POS), (0.5, [0, 7, -2]), (0.9, [0, 7, -4]),
                         (1.0, [0, 7, 5]), (1.2, [0, 7, 3]),
                         (1.4, [0, 7, 7]), (1.6, [0, 7, 4]),
                         (1.8, [0, 7, 9]), (2.2, [0, 7, 4]),
                         (3.0, G_BODY_POS, SETTLE)],
        },
        "rightArm": {
            "rotation": [(0.0, G_RIGHT_ARM), (0.5, align_right), (0.9, [-106, -6, 8]),
                         (1.0, [-80, -14, 16]), (1.2, align_right),
                         (1.4, [-80, -14, 16]), (1.6, align_right),
                         (1.8, [-80, -14, 16]), (2.2, align_right),
                         (3.0, G_RIGHT_ARM, SETTLE)],
            "position": [(0.0, G_RIGHT_ARM_POS), (0.5, [-3, 1, -4]), (2.2, [-3, 1, -4]),
                         (3.0, G_RIGHT_ARM_POS, SETTLE)],
        },
        "leftArm": {
            "rotation": [(0.0, G_LEFT_ARM), (0.5, align_left), (0.9, [-106, 6, -8]),
                         (1.0, [-80, 14, -16]), (1.2, align_left),
                         (1.4, [-80, 14, -16]), (1.6, align_left),
                         (1.8, [-80, 14, -16]), (2.2, align_left),
                         (3.0, G_LEFT_ARM, SETTLE)],
            "position": [(0.0, G_LEFT_ARM_POS), (0.5, [3, 1, -4]), (2.2, [3, 1, -4]),
                         (3.0, G_LEFT_ARM_POS, SETTLE)],
        },
        "armorRightArm": {
            "rotation": [(0.0, [0, 0, 0]), (0.5, [-30, 4, 46]), (1.0, [-48, 4, 60]),
                         (1.4, [-48, 4, 60]), (1.8, [-48, 4, 60]), (2.2, [-30, 4, 46]),
                         (3.0, [0, 0, 0], SETTLE)],
        },
        "armorLeftArm": {
            "rotation": [(0.0, [0, 0, 0]), (0.5, [-30, -4, -46]), (1.0, [-48, -4, -60]),
                         (1.4, [-48, -4, -60]), (1.8, [-48, -4, -60]), (2.2, [-30, -4, -46]),
                         (3.0, [0, 0, 0], SETTLE)],
        },
        "leftLeg": {
            "rotation": [(0.0, G_LEFT_LEG), (0.5, [-8, -2.5, 9.7]), (2.2, [-8, -2.5, 9.7]),
                         (3.0, G_LEFT_LEG, SETTLE)],
            "position": [(0.0, [1, 1, -1]), (3.0, [1, 1, -1])],
        },
        "rightLeg": {
            "rotation": [(0.0, G_RIGHT_LEG), (0.5, [-12, 0.9, -4.9]), (2.2, [-12, 0.9, -4.9]),
                         (3.0, G_RIGHT_LEG, SETTLE)],
            "position": [(0.0, [-1, 1, -3]), (3.0, [-1, 1, -3])],
        },
        "head": {"rotation": [(0.0, G_HEAD), (0.5, [0, 0, 0]), (2.2, [0, 0, 0]),
                              (3.0, G_HEAD, SETTLE)]},
    })


def gemini_comet_call():
    """Meteor / comet. Overhead gather, then four throws on ticks 40 / 60 / 80 / 100."""
    gather_right = [-178, 26, -26]
    gather_left = [-178, -26, 26]
    high_right = [-196, 20, -18]
    high_left = [-196, -20, 18]
    throw_right = [-66, 12, 12]
    throw_left = [-66, -12, -12]

    body_rot = [(0.0, [0, 0, 0]), (0.6, [-18, 0, 0]), (1.4, [-24, 0, 0])]
    body_pos = [(0.0, G_BODY_POS), (0.6, [0, 9, 0]), (1.4, [0, 11, 0])]
    right = [(0.0, G_RIGHT_ARM), (0.6, gather_right), (1.4, high_right)]
    left = [(0.0, G_LEFT_ARM), (0.6, gather_left), (1.4, high_left)]
    head = [(0.0, G_HEAD), (0.6, [-24, 0, 0]), (1.4, [-30, 0, 0])]

    for beat in (2.0, 3.0, 4.0, 5.0):
        body_rot += [(beat, [14, 0, 0]), (beat + 0.4, [-22, 0, 0])]
        body_pos += [(beat, [0, 9, 3]), (beat + 0.4, [0, 11, 0])]
        right += [(beat, throw_right), (beat + 0.4, high_right)]
        left += [(beat, throw_left), (beat + 0.4, high_left)]
        head += [(beat, [16, 0, 0]), (beat + 0.4, [-28, 0, 0])]

    body_rot += [(6.0, [0, 0, 0], "easeInBack")]
    body_pos += [(6.0, G_BODY_POS, "easeInBack")]
    right += [(6.0, G_RIGHT_ARM, "easeInBack")]
    left += [(6.0, G_LEFT_ARM, "easeInBack")]
    head += [(6.0, G_HEAD, "easeInBack")]

    return clip(6.0, {
        "body": {"rotation": body_rot, "position": body_pos},
        "rightArm": {"rotation": right,
                     "position": [(0.0, G_RIGHT_ARM_POS), (0.6, [-3, 3, 0]), (5.4, [-3, 3, 0]),
                                  (6.0, G_RIGHT_ARM_POS, "easeInBack")]},
        "leftArm": {"rotation": left,
                    "position": [(0.0, G_LEFT_ARM_POS), (0.6, [4, 3, 0]), (5.4, [4, 3, 0]),
                                 (6.0, G_LEFT_ARM_POS, "easeInBack")]},
        "leftLeg": {"rotation": [(0.0, G_LEFT_LEG), (0.6, [26, -2.5, 9.7]), (5.4, [26, -2.5, 9.7]),
                                 (6.0, G_LEFT_LEG, "easeInBack")],
                    "position": [(0.0, [1, 1, -1]), (6.0, [1, 1, -1])]},
        "rightLeg": {"rotation": [(0.0, G_RIGHT_LEG), (0.6, [22, 0.9, -4.9]), (5.4, [22, 0.9, -4.9]),
                                  (6.0, G_RIGHT_LEG, "easeInBack")],
                     "position": [(0.0, [-1, 1, -3]), (6.0, [-1, 1, -3])]},
        "head": {"rotation": head},
    })


def gemini_cloud_spread():
    """Cloud / area denial. Arms held wide and low, four downward pushes on the drop ticks."""
    open_right = [-18, -46, 96]
    open_left = [-18, 46, -96]
    push_right = [26, -56, 116]
    push_left = [26, 56, -116]

    body_rot = [(0.0, [0, 0, 0]), (0.7, [10, 0, 0]), (1.5, [8, 0, 0])]
    body_pos = [(0.0, G_BODY_POS), (0.7, [0, 8, 0]), (1.5, [0, 8, 0])]
    right = [(0.0, G_RIGHT_ARM), (0.7, open_right), (1.5, [-30, -46, 96])]
    left = [(0.0, G_LEFT_ARM), (0.7, open_left), (1.5, [-30, 46, -96])]
    head = [(0.0, G_HEAD), (0.7, [22, 0, 0]), (1.5, [22, 0, 0])]

    for beat in (2.0, 3.0, 4.0, 5.0):
        body_rot += [(beat, [19, 0, 0]), (beat + 0.5, [8, 0, 0])]
        body_pos += [(beat, [0, 5, 0]), (beat + 0.5, [0, 8, 0])]
        right += [(beat, push_right), (beat + 0.5, [-30, -46, 96])]
        left += [(beat, push_left), (beat + 0.5, [-30, 46, -96])]
        head += [(beat, [30, 0, 0]), (beat + 0.5, [22, 0, 0])]

    body_rot += [(6.0, [0, 0, 0], SETTLE)]
    body_pos += [(6.0, G_BODY_POS, SETTLE)]
    right += [(6.0, G_RIGHT_ARM, SETTLE)]
    left += [(6.0, G_LEFT_ARM, SETTLE)]
    head += [(6.0, G_HEAD, SETTLE)]

    return clip(6.0, {
        "body": {"rotation": body_rot, "position": body_pos},
        "rightArm": {"rotation": right,
                     "position": [(0.0, G_RIGHT_ARM_POS), (0.7, [-4, -1, 0]), (5.5, [-4, -1, 0]),
                                  (6.0, G_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": left,
                    "position": [(0.0, G_LEFT_ARM_POS), (0.7, [5, -1, 0]), (5.5, [5, -1, 0]),
                                 (6.0, G_LEFT_ARM_POS, SETTLE)]},
        "armorRightArm": {"rotation": [(0.0, [0, 0, 0]), (0.7, [0, 0, 28]), (5.5, [0, 0, 28]),
                                       (6.0, [0, 0, 0], SETTLE)]},
        "armorLeftArm": {"rotation": [(0.0, [0, 0, 0]), (0.7, [0, 0, -28]), (5.5, [0, 0, -28]),
                                      (6.0, [0, 0, 0], SETTLE)]},
        "leftLeg": {"rotation": [(0.0, G_LEFT_LEG), (0.7, [32, -2.5, 22]), (5.5, [32, -2.5, 22]),
                                 (6.0, G_LEFT_LEG, SETTLE)],
                    "position": [(0.0, [1, 1, -1]), (6.0, [1, 1, -1])]},
        "rightLeg": {"rotation": [(0.0, G_RIGHT_LEG), (0.7, [28, 0.9, -18]), (5.5, [28, 0.9, -18]),
                                  (6.0, G_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, [-1, 1, -3]), (6.0, [-1, 1, -3])]},
        "head": {"rotation": head},
    })


# Both handoff clips start AND end on the rest pose, with the readable shape in the middle. That
# is deliberate: they play on a controller that layers over the attack clips, so a clip that ended
# on its extreme would leave the twin holding that extreme over everything that followed.


def gemini_role_ascend():
    """15-tick handoff: the incoming PRIMARY dips, throws itself open, then holds station."""
    return clip(0.75, {
        "body": {
            "rotation": [(0.0, [0, 0, 0]), (0.2, [11, 0, 0]), (0.5, [-12, 0, 0]),
                         (0.75, [0, 0, 0], SETTLE)],
            "position": [(0.0, G_BODY_POS), (0.2, [0, 3, 0]), (0.5, [0, 11, 0]),
                         (0.75, G_BODY_POS, SETTLE)],
        },
        "rightArm": {
            "rotation": [(0.0, G_RIGHT_ARM), (0.2, [16, -26, 22]), (0.5, [-28, -32, 62]),
                         (0.75, G_RIGHT_ARM, SETTLE)],
            "position": [(0.0, G_RIGHT_ARM_POS), (0.5, [-3, 0, 0]), (0.75, G_RIGHT_ARM_POS)],
        },
        "leftArm": {
            "rotation": [(0.0, G_LEFT_ARM), (0.2, [16, 26, -20]), (0.5, [-28, 32, -60]),
                         (0.75, G_LEFT_ARM, SETTLE)],
            "position": [(0.0, G_LEFT_ARM_POS), (0.5, [4, 0, 0]), (0.75, G_LEFT_ARM_POS)],
        },
        "leftLeg": {"rotation": [(0.0, G_LEFT_LEG), (0.2, [36, -2.5, 9.7]), (0.5, [2, -2.5, 20]),
                                 (0.75, G_LEFT_LEG, SETTLE)],
                    "position": [(0.0, [1, 1, -1]), (0.75, [1, 1, -1])]},
        "rightLeg": {"rotation": [(0.0, G_RIGHT_LEG), (0.2, [32, 0.9, -4.9]), (0.5, [-2, 0.9, -16]),
                                  (0.75, G_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, [-1, 1, -3]), (0.75, [-1, 1, -3])]},
        "head": {"rotation": [(0.0, G_HEAD), (0.2, [20, 0, 0]), (0.5, [-16, 0, 0]),
                              (0.75, G_HEAD, SETTLE)]},
    })


def gemini_role_settle():
    """15-tick handoff: the outgoing PRIMARY sinks, closes up, then eases back to station."""
    return clip(0.75, {
        "body": {
            "rotation": [(0.0, [0, 0, 0]), (0.15, [-5, 0, 0]), (0.5, [12, 0, 0]),
                         (0.75, [0, 0, 0], SETTLE)],
            "position": [(0.0, G_BODY_POS), (0.15, [0, 7.5, 0]), (0.5, [0, 2, 0]),
                         (0.75, G_BODY_POS, SETTLE)],
        },
        "rightArm": {
            "rotation": [(0.0, G_RIGHT_ARM), (0.5, [20, -8, 14]), (0.75, G_RIGHT_ARM, SETTLE)],
            "position": [(0.0, G_RIGHT_ARM_POS), (0.5, [0, 0, 0]), (0.75, G_RIGHT_ARM_POS)],
        },
        "leftArm": {
            "rotation": [(0.0, G_LEFT_ARM), (0.5, [20, 8, -12]), (0.75, G_LEFT_ARM, SETTLE)],
            "position": [(0.0, G_LEFT_ARM_POS), (0.5, [1, 0, 0]), (0.75, G_LEFT_ARM_POS)],
        },
        "leftLeg": {"rotation": [(0.0, G_LEFT_LEG), (0.5, [30, -2.5, 9.7]), (0.75, G_LEFT_LEG, SETTLE)],
                    "position": [(0.0, [1, 1, -1]), (0.75, [1, 1, -1])]},
        "rightLeg": {"rotation": [(0.0, G_RIGHT_LEG), (0.5, [26, 0.9, -4.9]), (0.75, G_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, [-1, 1, -3]), (0.75, [-1, 1, -3])]},
        "head": {"rotation": [(0.0, G_HEAD), (0.5, [24, 0, 0]), (0.75, G_HEAD, SETTLE)]},
    })


def build_gemini():
    fire = {
        "animation.awakener.v2.beam_snap": gemini_beam_snap(),
        "animation.awakener.v2.beam_channel": gemini_beam_channel(),
        "animation.awakener.v2.comet_call": gemini_comet_call(),
        "animation.awakener.v2.cloud_spread": gemini_cloud_spread(),
    }
    out = dict(fire)
    # The frost twin runs the same motion off the other hand, so the pair reads as related
    # without either of them looking like a copy of the other.
    for name, source in fire.items():
        out[name + "_frost"] = mirror(source)
    out["animation.awakener.v2.role_ascend"] = gemini_role_ascend()
    out["animation.awakener.v2.role_settle"] = gemini_role_settle()
    return out


# ── Magus ────────────────────────────────────────────────────────────────────
#
# Rest pose taken from animation.magus.idle. Magus carries the staff in the LEFT hand
# (leftItem hangs off leftArm), so a heavy cast is read by the right hand coming across to
# join it, and a quick cast by the right hand acting alone. bone / bone2 / bone3 are the
# floating aura pieces; scaling them is how the existing clips show gathered power.

M_BODY = [-5, 0, 0]
M_HEAD = [7.5, 0, 0]
M_BONE = [14.9, -1.3, 4.8]
M_BONE2 = [0, 0, 15]
M_BONE2_POS = [0, 1, 0]
M_BIPED = [0, 0, 0]
M_RIGHT_ARM = [-18.6, 7.5, 21.3]
M_RIGHT_ARM_POS = [-1, 0, 0]
M_LEFT_ARM = [22.5, 75, 10]
M_LEFT_ARM_POS = [2, -1, 3]
M_RIGHT_LEG = [11.3, -2.5, 13.8]
M_RIGHT_LEG_POS = [0, 0, -2]
M_LEFT_LEG = [2.3, -2.7, -5.1]
M_LEFT_LEG_POS = [1, 0, 1]
M_ARMOR_LEFT_LEG = [7.5, 0, 0]
M_BONE3 = [19.7, -11, 28.1]
ONE = [1, 1, 1]


def m_rest(*, exclude=()):
    """Static rest tracks for the bones a clip does not otherwise move."""
    rest = {
        "body": {"rotation": [(0.0, M_BODY)]},
        "head": {"rotation": [(0.0, M_HEAD)]},
        "bone": {"rotation": [(0.0, M_BONE)]},
        "bone2": {"rotation": [(0.0, M_BONE2)], "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM)], "position": [(0.0, M_RIGHT_ARM_POS)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM)], "position": [(0.0, M_LEFT_ARM_POS)]},
        "rightLeg": {"rotation": [(0.0, M_RIGHT_LEG)], "position": [(0.0, M_RIGHT_LEG_POS)]},
        "leftLeg": {"rotation": [(0.0, M_LEFT_LEG)], "position": [(0.0, M_LEFT_LEG_POS)]},
        "armorLeftLeg": {"rotation": [(0.0, M_ARMOR_LEFT_LEG)]},
        "bone3": {"rotation": [(0.0, M_BONE3)], "scale": [(0.0, ONE)]},
        "bone4": {"scale": [(0.0, ONE)]},
    }
    return {bone: channels for bone, channels in rest.items() if bone not in exclude}


def magus_clip(length, bones):
    merged = m_rest(exclude=tuple(bones))
    merged.update(bones)
    return clip(length, merged)


def magus_cast_quick():
    """QUICK PROJECTILE — right hand alone, release on tick 5, fully recovered by tick 14."""
    return magus_clip(0.7, {
        "body": {"rotation": [(0.0, M_BODY), (0.15, [-6, -30, 3]), (0.25, [-6, -34, 3]),
                              (0.7, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.25, [7.6, 34.8, 4.4]), (0.7, M_HEAD, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.15, [-118, 2, -18]), (0.25, [-88, -8, -26]),
                                  (0.45, [-52, 2, 2]), (0.7, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.25, [1, 0, -2]),
                                  (0.7, M_RIGHT_ARM_POS, SETTLE)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (0.25, [0, 0, 24])],
                  "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (0.2, [1, 1.25, 1]), (0.35, ONE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (0.25, [17.6, -14.2, 37.8]), (0.7, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (0.2, [1, 1.25, 1]), (0.35, ONE)]},
    })


def magus_cast_quick_nova():
    """QUICK NOVA — same 5-tick release, but both hands break outward from the chest."""
    return magus_clip(0.7, {
        "body": {"rotation": [(0.0, M_BODY), (0.15, [4, 0, 0]), (0.25, [-14, 0, 0]),
                              (0.7, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.15, [18, 0, 0]), (0.25, [-8, 0, 0]),
                              (0.7, M_HEAD, SETTLE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.15, [8, 0, 0]), (0.25, [-6, 0, 0]),
                                   (0.7, M_BIPED, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.15, [-46, 34, 58]), (0.25, [-24, -18, 96]),
                                  (0.7, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.15, [1, 0, -2]), (0.25, [-3, 0, 0]),
                                  (0.7, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.15, [-40, 46, -34]), (0.25, [-20, 62, -78]),
                                 (0.7, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.15, [0, 0, -1]), (0.25, [4, -1, 2]),
                                 (0.7, M_LEFT_ARM_POS, SETTLE)]},
        "bone2": {"rotation": [(0.0, M_BONE2)], "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (0.15, [1.25, 0.8, 1.25]), (0.3, [0.9, 1.3, 0.9]),
                            (0.7, ONE, SETTLE)]},
        "bone4": {"scale": [(0.0, ONE), (0.15, [0.85, 1, 0.85]), (0.28, [1.35, 1, 1.35]),
                            (0.7, ONE, SETTLE)]},
    })


def magus_cast_heavy():
    """HEAVY PROJECTILE — both hands committed to the staff, release on tick 40, settle by 48."""
    return magus_clip(2.4, {
        "body": {"rotation": [(0.0, M_BODY), (0.6, [4, 26, 0]), (1.6, [8, 34, 0]),
                              (2.0, [-16, -8, 0]), (2.4, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.6, [10, -22, 0]), (1.6, [12, -28, 0]),
                              (2.0, [-4, 6, 0]), (2.4, M_HEAD, SETTLE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.6, [6, 0, 0]), (1.6, [10, 0, 0]),
                                   (2.0, [-12, 0, 0]), (2.4, M_BIPED, SETTLE)]},
        # Right hand travels across to meet the staff, both arms drive forward on the release.
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.6, [-64, 46, -22]), (1.6, [-84, 52, -30]),
                                  (2.0, [-116, 10, -6]), (2.4, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.6, [2, 0, 1]), (1.6, [3, 1, 1]),
                                  (2.0, [1, 0, -4]), (2.4, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.6, [-30, 62, 6]), (1.6, [-52, 58, 4]),
                                 (2.0, [-104, 30, 0]), (2.4, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (1.6, [2, 1, 2]), (2.0, [1, 0, -3]),
                                 (2.4, M_LEFT_ARM_POS, SETTLE)]},
        "rightLeg": {"rotation": [(0.0, M_RIGHT_LEG), (1.6, [22, -2.5, 13.8]), (2.0, [-8, -2.5, 13.8]),
                                  (2.4, M_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, M_RIGHT_LEG_POS)]},
        "leftLeg": {"rotation": [(0.0, M_LEFT_LEG), (1.6, [-12, -2.7, -5.1]), (2.0, [16, -2.7, -5.1]),
                                 (2.4, M_LEFT_LEG, SETTLE)],
                    "position": [(0.0, M_LEFT_LEG_POS)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (1.6, [0, 0, 34]), (2.4, M_BONE2, SETTLE)],
                  "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (1.6, [1, 1.6, 1]), (2.05, [1, 0.8, 1]), (2.4, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (1.6, [14, -18, 46]), (2.4, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (1.6, [1, 1.6, 1]), (2.05, [1, 0.8, 1]), (2.4, ONE, SETTLE)]},
    })


def magus_cast_nova():
    """NOVA / RADIAL — gathers to centre of mass, breaks outward on tick 40, settles by 48."""
    return magus_clip(2.4, {
        "body": {"rotation": [(0.0, M_BODY), (0.8, [14, 0, 0]), (1.7, [20, 0, 0]),
                              (2.0, [-20, 0, 0]), (2.4, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.8, [26, 0, 0]), (1.7, [30, 0, 0]),
                              (2.0, [-22, 0, 0]), (2.4, M_HEAD, SETTLE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.8, [16, 0, 0]), (1.7, [20, 0, 0]),
                                   (2.0, [-14, 0, 0]), (2.4, M_BIPED, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.8, [-58, 38, 40]), (1.7, [-70, 44, 30]),
                                  (2.0, [-16, -24, 104]), (2.4, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.8, [2, 0, -2]), (1.7, [2, 0, -3]),
                                  (2.0, [-4, 0, 0]), (2.4, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.8, [-52, 50, -22]), (1.7, [-64, 46, -14]),
                                 (2.0, [-12, 66, -92]), (2.4, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.8, [-1, 0, -2]), (1.7, [-1, 0, -3]),
                                 (2.0, [5, -1, 1]), (2.4, M_LEFT_ARM_POS, SETTLE)]},
        "rightLeg": {"rotation": [(0.0, M_RIGHT_LEG), (1.7, [24, -2.5, 13.8]), (2.0, [-6, -2.5, 24]),
                                  (2.4, M_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, M_RIGHT_LEG_POS)]},
        "leftLeg": {"rotation": [(0.0, M_LEFT_LEG), (1.7, [20, -2.7, -5.1]), (2.0, [-6, -2.7, -18]),
                                 (2.4, M_LEFT_LEG, SETTLE)],
                    "position": [(0.0, M_LEFT_LEG_POS)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (1.7, [0, 0, 6]), (2.4, M_BONE2, SETTLE)],
                  "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (1.7, [0.75, 0.75, 0.75]), (2.05, [1.5, 1.5, 1.5]),
                            (2.4, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (1.7, [26, -6, 16]), (2.4, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (1.7, [0.75, 0.75, 0.75]), (2.05, [1.5, 1.5, 1.5]),
                            (2.4, ONE, SETTLE)]},
        "bone4": {"scale": [(0.0, ONE), (1.7, [0.8, 1, 0.8]), (2.05, [1.5, 1, 1.5]),
                            (2.4, ONE, SETTLE)]},
    })


def magus_cast_shockwave():
    """SHOCKWAVE / GROUND-DIRECTED — staff raised then driven down and forward on tick 12."""
    return magus_clip(1.2, {
        "body": {"rotation": [(0.0, M_BODY), (0.35, [-22, 0, 0]), (0.6, [26, 0, 0]),
                              (0.85, [16, 0, 0]), (1.2, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.35, [-18, 0, 0]), (0.6, [34, 0, 0]),
                              (1.2, M_HEAD, SETTLE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.35, [-14, 0, 0]), (0.6, [22, 0, 0]),
                                   (1.2, M_BIPED, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.35, [-158, 16, -10]), (0.6, [-26, 4, 6]),
                                  (0.85, [-14, 6, 12]), (1.2, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.35, [1, 1, 0]), (0.6, [1, -1, -3]),
                                  (1.2, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.35, [-150, 40, 4]), (0.6, [-18, 24, 2]),
                                 (0.85, [-8, 40, 6]), (1.2, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.35, [1, 2, 0]), (0.6, [1, -1, -3]),
                                 (1.2, M_LEFT_ARM_POS, SETTLE)]},
        "rightLeg": {"rotation": [(0.0, M_RIGHT_LEG), (0.6, [-14, -2.5, 13.8]),
                                  (1.2, M_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, M_RIGHT_LEG_POS)]},
        "leftLeg": {"rotation": [(0.0, M_LEFT_LEG), (0.6, [22, -2.7, -5.1]),
                                 (1.2, M_LEFT_LEG, SETTLE)],
                    "position": [(0.0, M_LEFT_LEG_POS)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (0.35, [0, 0, 40]), (0.6, [0, 0, -8]),
                               (1.2, M_BONE2, SETTLE)],
                  "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (0.35, [1, 1.5, 1]), (0.65, [1.3, 0.7, 1.3]),
                            (1.2, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (0.35, [8, -18, 48]), (1.2, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (0.35, [1, 1.5, 1]), (0.65, [1.3, 0.7, 1.3]),
                            (1.2, ONE, SETTLE)]},
    })


def magus_cast_channel():
    """LONG CHANNEL / CATASTROPHIC — held two-handed above the head, release on tick 40."""
    hold_right = [-166, 20, -14]
    hold_left = [-160, 44, 8]
    return magus_clip(2.4, {
        "body": {"rotation": [(0.0, M_BODY), (0.5, [-16, 0, 0]), (1.0, [-22, 0, 0]),
                              (1.5, [-26, 0, 0]), (1.9, [-30, 0, 0]), (2.0, [22, 0, 0]),
                              (2.4, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.5, [-24, 0, 0]), (1.9, [-32, 0, 0]),
                              (2.0, [26, 0, 0]), (2.4, M_HEAD, SETTLE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.5, [-12, 0, 0]), (1.9, [-18, 0, 0]),
                                   (2.0, [18, 0, 0]), (2.4, M_BIPED, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.5, hold_right), (1.0, [-172, 20, -14]),
                                  (1.5, hold_right), (1.9, [-176, 20, -14]), (2.0, [-40, 10, 30]),
                                  (2.4, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.5, [0, 1, 0]), (1.9, [0, 2, 0]),
                                  (2.4, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.5, hold_left), (1.0, [-166, 44, 8]),
                                 (1.5, hold_left), (1.9, [-170, 44, 8]), (2.0, [-34, 60, 12]),
                                 (2.4, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.5, [1, 1, 1]), (1.9, [1, 2, 1]),
                                 (2.4, M_LEFT_ARM_POS, SETTLE)]},
        "rightLeg": {"rotation": [(0.0, M_RIGHT_LEG), (0.5, [-8, -2.5, 13.8]), (1.9, [-12, -2.5, 13.8]),
                                  (2.0, [18, -2.5, 13.8]), (2.4, M_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, M_RIGHT_LEG_POS)]},
        "leftLeg": {"rotation": [(0.0, M_LEFT_LEG), (0.5, [-14, -2.7, -5.1]), (1.9, [-18, -2.7, -5.1]),
                                 (2.0, [14, -2.7, -5.1]), (2.4, M_LEFT_LEG, SETTLE)],
                    "position": [(0.0, M_LEFT_LEG_POS)]},
        # The aura swells the whole way in, then dumps on the release — the anticipation IS the tell.
        "bone2": {"rotation": [(0.0, M_BONE2), (1.0, [0, 0, 30]), (1.9, [0, 0, 44]),
                               (2.4, M_BONE2, SETTLE)],
                  "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (0.5, [1.1, 1.3, 1.1]), (1.9, [1.4, 2.1, 1.4]),
                            (2.05, [0.7, 0.6, 0.7]), (2.4, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (1.0, [10, -20, 44]), (1.9, [6, -24, 56]),
                               (2.4, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (0.5, [1.1, 1.3, 1.1]), (1.9, [1.4, 2.1, 1.4]),
                            (2.05, [0.7, 0.6, 0.7]), (2.4, ONE, SETTLE)]},
        "bone4": {"scale": [(0.0, ONE), (1.9, [1.3, 1, 1.3]), (2.05, [0.8, 1, 0.8]),
                            (2.4, ONE, SETTLE)]},
    })


def magus_phase_transition():
    """PHASE TRANSITION — 60 ticks: contract, hold, then throw the guard wide open."""
    return magus_clip(3.0, {
        "body": {"rotation": [(0.0, M_BODY), (0.5, [24, 0, 0]), (1.4, [30, 0, 0]),
                              (1.8, [-26, 0, 0]), (2.3, [-12, 0, 0]), (3.0, M_BODY, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.5, [34, 0, 0]), (1.4, [38, 0, 0]),
                              (1.8, [-34, 0, 0]), (3.0, M_HEAD, SETTLE)]},
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.5, [22, 0, 0]), (1.4, [26, 0, 0]),
                                   (1.8, [-20, 0, 0]), (3.0, M_BIPED, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.5, [-72, 40, 34]), (1.4, [-80, 46, 26]),
                                  (1.8, [-32, -30, 118]), (2.3, [-24, -18, 76]),
                                  (3.0, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.5, [2, 0, -2]), (1.8, [-4, 1, 0]),
                                  (3.0, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.5, [-66, 52, -18]), (1.4, [-74, 48, -10]),
                                 (1.8, [-28, 70, -104]), (2.3, [-20, 74, -62]),
                                 (3.0, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.5, [-1, 0, -2]), (1.8, [5, -1, 1]),
                                 (3.0, M_LEFT_ARM_POS, SETTLE)]},
        "rightLeg": {"rotation": [(0.0, M_RIGHT_LEG), (1.4, [26, -2.5, 13.8]), (1.8, [-10, -2.5, 26]),
                                  (3.0, M_RIGHT_LEG, SETTLE)],
                     "position": [(0.0, M_RIGHT_LEG_POS)]},
        "leftLeg": {"rotation": [(0.0, M_LEFT_LEG), (1.4, [22, -2.7, -5.1]), (1.8, [-10, -2.7, -20]),
                                 (3.0, M_LEFT_LEG, SETTLE)],
                    "position": [(0.0, M_LEFT_LEG_POS)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (1.4, [0, 0, 46]), (1.8, [0, 0, -14]),
                               (3.0, M_BONE2, SETTLE)],
                  "position": [(0.0, M_BONE2_POS)],
                  "scale": [(0.0, ONE), (1.4, [0.7, 0.7, 0.7]), (1.85, [1.8, 1.8, 1.8]),
                            (3.0, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (1.4, [28, -4, 12]), (3.0, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (1.4, [0.7, 0.7, 0.7]), (1.85, [1.8, 1.8, 1.8]),
                            (3.0, ONE, SETTLE)]},
        "bone4": {"scale": [(0.0, ONE), (1.4, [0.75, 1, 0.75]), (1.85, [1.7, 1, 1.7]),
                            (3.0, ONE, SETTLE)]},
    })


# The three barrier reactions animate the upper body only. They run on their own controller so a
# barrier event reads over whatever cast is in flight without wiping the cast's lower body.
def magus_barrier_shift():
    """BARRIER — a short ritual guard: staff swept across, free hand sealing it."""
    return clip(0.75, {
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.2, [0, -18, 0]), (0.4, [0, 12, 0]),
                                   (0.75, M_BIPED, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.25, [16, -12, 0]), (0.75, M_HEAD, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.25, [-96, 52, -34]), (0.45, [-70, 30, -10]),
                                  (0.75, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.25, [2, 1, -1]),
                                  (0.75, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.25, [-44, 66, 22]), (0.45, [-20, 72, 16]),
                                 (0.75, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.75, M_LEFT_ARM_POS)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (0.3, [0, 0, 36]), (0.75, M_BONE2, SETTLE)],
                  "scale": [(0.0, ONE), (0.3, [1.35, 1.35, 1.35]), (0.75, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (0.3, [12, -20, 48]), (0.75, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (0.3, [1.35, 1.35, 1.35]), (0.75, ONE, SETTLE)]},
    })


def magus_barrier_absorb():
    """WRONG SCHOOL — the hit is drawn inward and Magus draws himself up on it."""
    return clip(0.6, {
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.15, [14, 0, 0]), (0.35, [-12, 0, 0]),
                                   (0.6, M_BIPED, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.15, [24, 0, 0]), (0.35, [-18, 0, 0]),
                              (0.6, M_HEAD, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.15, [-58, 54, -40]), (0.35, [-34, 20, 34]),
                                  (0.6, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.15, [3, 0, -2]),
                                  (0.6, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.15, [-46, 62, -8]), (0.35, [-16, 80, 18]),
                                 (0.6, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.15, [0, 0, -1]),
                                 (0.6, M_LEFT_ARM_POS, SETTLE)]},
        # Aura is pulled in tight, then swells past rest — the "it fed him" beat.
        "bone2": {"rotation": [(0.0, M_BONE2), (0.6, M_BONE2)],
                  "scale": [(0.0, ONE), (0.15, [0.55, 0.55, 0.55]), (0.35, [1.6, 1.6, 1.6]),
                            (0.6, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (0.6, M_BONE3)],
                  "scale": [(0.0, ONE), (0.15, [0.55, 0.55, 0.55]), (0.35, [1.6, 1.6, 1.6]),
                            (0.6, ONE, SETTLE)]},
    })


def magus_barrier_break():
    """BARRIER BREAK — the guard collapses: shoulders snap open, head thrown back."""
    return clip(0.6, {
        "bipedBody": {"rotation": [(0.0, M_BIPED), (0.1, [-22, 0, 0]), (0.3, [10, 0, 0]),
                                   (0.6, M_BIPED, SETTLE)]},
        "head": {"rotation": [(0.0, M_HEAD), (0.1, [-36, 0, 0]), (0.3, [20, 0, 0]),
                              (0.6, M_HEAD, SETTLE)]},
        "rightArm": {"rotation": [(0.0, M_RIGHT_ARM), (0.1, [-40, -34, 96]), (0.3, [-6, 12, 44]),
                                  (0.6, M_RIGHT_ARM, SETTLE)],
                     "position": [(0.0, M_RIGHT_ARM_POS), (0.1, [-4, 0, 1]),
                                  (0.6, M_RIGHT_ARM_POS, SETTLE)]},
        "leftArm": {"rotation": [(0.0, M_LEFT_ARM), (0.1, [-36, 84, -70]), (0.3, [-2, 80, -12]),
                                 (0.6, M_LEFT_ARM, SETTLE)],
                    "position": [(0.0, M_LEFT_ARM_POS), (0.1, [5, -1, 1]),
                                 (0.6, M_LEFT_ARM_POS, SETTLE)]},
        "bone2": {"rotation": [(0.0, M_BONE2), (0.1, [0, 0, -20]), (0.6, M_BONE2, SETTLE)],
                  "scale": [(0.0, ONE), (0.1, [1.9, 1.9, 1.9]), (0.3, [0.4, 0.4, 0.4]),
                            (0.6, ONE, SETTLE)]},
        "bone3": {"rotation": [(0.0, M_BONE3), (0.1, [30, 0, 6]), (0.6, M_BONE3, SETTLE)],
                  "scale": [(0.0, ONE), (0.1, [1.9, 1.9, 1.9]), (0.3, [0.4, 0.4, 0.4]),
                            (0.6, ONE, SETTLE)]},
    })


def build_magus():
    return {
        "animation.magus.v2.cast.quick": magus_cast_quick(),
        "animation.magus.v2.cast.quick_nova": magus_cast_quick_nova(),
        "animation.magus.v2.cast.heavy": magus_cast_heavy(),
        "animation.magus.v2.cast.nova": magus_cast_nova(),
        "animation.magus.v2.cast.shockwave": magus_cast_shockwave(),
        "animation.magus.v2.cast.channel": magus_cast_channel(),
        "animation.magus.v2.phase_transition": magus_phase_transition(),
        "animation.magus.v2.barrier_shift": magus_barrier_shift(),
        "animation.magus.v2.barrier_absorb": magus_barrier_absorb(),
        "animation.magus.v2.barrier_break": magus_barrier_break(),
    }


# ── Emit ─────────────────────────────────────────────────────────────────────


def stamp_path(out_path):
    """Where this script records the checksum of what it last emitted for a given output."""
    return os.path.join(HERE, "." + os.path.basename(out_path) + ".stamp")


def guard_hand_edits(out_path, payload, force):
    """Abort rather than clobber an output that has been edited outside this script.

    These are files a human can reasonably want to open in Blockbench and tweak. Regenerating on
    top of that silently destroys the tweak, so the check is: does the file on disk still match the
    checksum recorded on the last run? If not — including the case where no stamp exists at all —
    stop, unless told otherwise, and take a backup either way.
    """
    if not os.path.exists(out_path):
        return True
    current = open(out_path, "rb").read()
    digest = hashlib.sha256(current).hexdigest()
    if digest == hashlib.sha256(payload.encode("utf-8")).hexdigest():
        return True  # identical output; nothing to lose either way
    stamp = stamp_path(out_path)
    recorded = open(stamp, encoding="utf-8").read().strip() if os.path.exists(stamp) else None
    if recorded is not None and digest == recorded:
        return True  # still exactly what this script last wrote
    backup = "%s.%s.bak" % (out_path, time.strftime("%Y%m%d-%H%M%S"))
    shutil.copy2(out_path, backup)
    if not force:
        print("    REFUSING to overwrite {}: it has been modified since this script last wrote it."
              .format(os.path.basename(out_path)))
        print("    A copy of the current file is at {}".format(os.path.normpath(backup)))
        print("    Fold the hand edits back into this script, or re-run with --force.")
        return False
    print("    forced overwrite; previous file backed up to {}".format(os.path.normpath(backup)))
    return True


def emit(legacy_path, out_path, authored, write, force):
    with open(legacy_path, "r", encoding="utf-8") as handle:
        legacy = json.load(handle)

    combined = dict(legacy["animations"])  # every legacy clip carried forward, verbatim
    clashes = sorted(set(combined) & set(authored))
    if clashes:
        print("ERROR: authored clip names collide with legacy names: " + ", ".join(clashes))
        return False
    combined.update(authored)

    document = {"format_version": legacy.get("format_version", "1.8.0"), "animations": combined}

    print("{}: {} legacy + {} authored = {} clips".format(
        os.path.basename(out_path), len(legacy["animations"]), len(authored), len(combined)))
    for name in authored:
        print("    + {} ({} s)".format(name, authored[name]["animation_length"]))

    payload = json.dumps(document, indent=2) + "\n"

    if not write:
        return True
    if not guard_hand_edits(out_path, payload, force):
        return False
    with open(out_path, "w", encoding="utf-8") as handle:
        handle.write(payload)
    with open(stamp_path(out_path), "w", encoding="utf-8") as handle:
        handle.write(hashlib.sha256(payload.encode("utf-8")).hexdigest() + "\n")
    print("    wrote " + out_path)
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true", help="actually write the v2 files")
    parser.add_argument("--force", action="store_true", help="overwrite existing v2 files")
    args = parser.parse_args()

    ok = emit(GEMINI_LEGACY, GEMINI_V2, build_gemini(), args.write, args.force)
    ok = emit(MAGUS_LEGACY, MAGUS_V2, build_magus(), args.write, args.force) and ok
    if not args.write:
        print("\ndry run — nothing written. Re-run with --write.")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
