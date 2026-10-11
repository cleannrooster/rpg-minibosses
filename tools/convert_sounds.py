"""Convert the boss sound effects into the mod's assets.

The table below is the single source of truth for which source file backs which sound event. Running
the script:

  * converts each listed source to mono Ogg Vorbis under
    common/src/main/resources/assets/rpg-minibosses/sounds/boss/<key path>_<n>.ogg
    (mono so the game fades it with distance; leading/trailing silence trimmed, except loops, whose
    length is the loop),
  * adds a sounds.json entry for each key that does not have one yet.

It never overwrites: an existing .ogg is skipped, and an existing sounds.json key is left exactly as it
is (so hand edits to sounds.json survive). Pass --force to re-convert existing files; each overwritten
file is first copied to <name>.ogg.bak.

Sources (see THIRD_PARTY_NOTICES.md for licences):
  * "Fantasy SFX Pack Vol 1" by JC Sounds  -> <Mods>/Fantasy SFX Pack Vol 1/*.wav     (CC-BY 4.0)
  * "Spell Sounds Starter Pack"            -> <Mods>/*.ogg                            (CC-BY-SA 3.0)

Usage:  python tools/convert_sounds.py [--source-root ../..] [--force] [--dry-run]
Requires ffmpeg on PATH.
"""

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
ASSETS = PROJECT / "common/src/main/resources/assets/rpg-minibosses"
SOUNDS_DIR = ASSETS / "sounds/boss"
SOUNDS_JSON = ASSETS / "sounds.json"
NAMESPACE = "rpg-minibosses"

FANTASY = "Fantasy SFX Pack Vol 1/"
F = "Fantasy SFX Pack Vol 1_"


def fantasy(name):
    return FANTASY + ("Loop_Fantasy Pack Vol 1_" + name if name.startswith("Dark Necromancy")
                      else name.split("|")[0] + "_" + F + name.split("|")[1])


# key (sound event path under rpg-minibosses:boss.) -> (subtitle, [source files relative to the Mods folder])
# Fantasy pack files are written as "Prefix|Name" for "Prefix_Fantasy SFX Pack Vol 1_Name.wav".
SOUNDS = {
    # --- fire ---
    "fire.windup": ("Fire gathers", [fantasy("Single|Fireball_Buildup.wav")]),
    "fire.release": ("Fire is hurled", [fantasy(f"Single|Fireball Launch_0{i}.wav") for i in (1, 2, 3)]),
    "fire.impact": ("Fire bursts", [fantasy(f"Single|Fireball Impact_0{i}.wav") for i in (1, 2, 3)]),
    "fire.ignite": ("Flames roar", ["flamethrower.ogg"]),
    "fire.loop": ("Fire crackles", [fantasy("Loop|Fireball_Hold.wav")]),
    # --- frost ---
    "frost.windup": ("Frost gathers", [fantasy("Single|Ice Spell_Buildup.wav")]),
    "frost.release": ("Ice is hurled", [fantasy(f"Single|Ice Shard Projectile_Launch_0{i}.wav") for i in (1, 2, 3)]),
    "frost.impact": ("Ice shatters", [fantasy(f"Single|Ice Shard  Projectile_Impact_0{i}.wav") for i in (1, 2, 3)]),
    "frost.freeze": ("Something freezes", ["freeze2.ogg"]),
    "frost.loop": ("Frost hums", [fantasy("Loop|Ice Spell_Hold.wav")]),
    # --- lightning ---
    "lightning.windup": ("Lightning gathers", [fantasy("Single|Electric Spell_Buildup.wav")]),
    "lightning.release": ("Lightning is hurled", [fantasy(f"Single|Electric Spell_Launch_0{i}.wav") for i in (1, 2, 3)]),
    "lightning.impact": ("Lightning strikes", [fantasy(f"Single|Electric Spell_Hit_0{i}.wav") for i in (1, 2, 3)]),
    "lightning.loop": ("Lightning crackles", [fantasy("Loop|Electric Spell_Hold.wav")]),
    # --- arcane ---
    "arcane.windup": ("Arcane power gathers", ["enchant.ogg", "enchant2.ogg"]),
    "arcane.release": ("Arcane power is hurled", ["zap2.ogg", "zap2a.ogg", "zap2b.ogg", "zap2c.ogg"]),
    "arcane.impact": ("Arcane power bursts", ["magicdrop.ogg"]),
    # --- soul ---
    "soul.windup": ("Dark power gathers", ["curse.ogg", "curse3.ogg", "curse4.ogg"]),
    "soul.release": ("Dark power is unleashed", ["curse5.ogg"]),
    "soul.impact": ("Dark power strikes", ["magicerase.ogg"]),
    "soul.loop": ("Chanting", [fantasy("Dark Necromancy Chant_01.wav")]),
    # --- healing ---
    "healing.windup": ("Holy power gathers", ["blessing2.ogg", "blessing3.ogg"]),
    "healing.release": ("Holy power is released", ["heal.ogg"]),
    "healing.impact": ("Holy chime", [fantasy(f"Single|Healing Chime_0{i}.wav") for i in (1, 2)]),
    # --- physical ---
    "physical.swing": ("Blade swings", [fantasy(f"Single|Sword_Swing_0{i}.wav") for i in (1, 2, 3)]),
    "physical.heavy_swing": ("Heavy blade swings", [fantasy(f"Single|Heavy Sword_Swing_0{i}.wav") for i in (1, 2, 3)]),
    "physical.hit": ("Blade strikes", [fantasy(f"Single|Sword_Hit_Metal_0{i}.wav") for i in (1, 2, 3)]),
    "physical.heavy_hit": ("Heavy blade strikes", [fantasy(f"Single|Heavy Sword_Hit_Metal_0{i}.wav") for i in (1, 2, 3)]),
    "physical.dagger_throw": ("Dagger flies", [fantasy(f"Single|Dagger_Swing_0{i}.wav") for i in (1, 2, 3)]),
    "physical.dagger_hit": ("Dagger strikes", [fantasy(f"Single|Dagger_Hit_Metal_0{i}.wav") for i in (1, 2, 3)]),
    "physical.bow_draw": ("Bow draws", [fantasy(f"Single|Bow_Arrow_Draw_0{i}.wav") for i in (1, 2)]),
    "physical.bow_shoot": ("Bow fires", [fantasy(f"Single|Bow_Arrow_Shoot_0{i}.wav") for i in (1, 2)]),
    "physical.bow_hit": ("Arrow strikes", [fantasy(f"Single|Bow_Arrow_Hit_0{i}.wav") for i in (1, 2)]),
    # --- utility ---
    "teleport.out": ("Boss vanishes", [fantasy("Single|Teleport_Out.wav")]),
    "teleport.in": ("Boss appears", [fantasy("Single|Teleport_In.wav")]),
    "shield": ("Magic shield rises", [fantasy(f"Single|Magic Shield_Activation_0{i}.wav") for i in (1, 2)]),
    "explosion": ("Large explosion", [f"explode{i}.ogg" for i in (1, 2, 3, 4, 5)]),
}

SILENCE = ("silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.02,"
           "areverse,silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.05,areverse")


def is_loop(key):
    return key.endswith(".loop")


def destination(key, index):
    return SOUNDS_DIR / (key.replace(".", "/") + f"_{index + 1}.ogg")


def resource_id(path):
    rel = path.relative_to(ASSETS / "sounds").with_suffix("")
    return f"{NAMESPACE}:{rel.as_posix()}"


def convert(source, target, loop, dry_run):
    filters = [] if loop else ["-af", SILENCE]
    cmd = ["ffmpeg", "-v", "error", "-y", "-i", str(source), "-ac", "1", *filters,
           "-c:a", "libvorbis", "-q:a", "5", str(target)]
    if dry_run:
        print("  would run:", " ".join(cmd))
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(cmd, check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", default=str(PROJECT.parent.parent),
                        help="folder holding the loose .ogg files and the Fantasy SFX pack (default: the Mods folder)")
    parser.add_argument("--force", action="store_true", help="re-convert existing files (backing each up first)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    root = Path(args.source_root)

    missing = [s for _, (_, files) in SOUNDS.items() for s in files if not (root / s).is_file()]
    if missing:
        print("Missing sources under", root, file=sys.stderr)
        for s in missing:
            print("  ", s, file=sys.stderr)
        return 1

    converted = skipped = 0
    for key, (_, files) in SOUNDS.items():
        for i, source in enumerate(files):
            target = destination(key, i)
            if target.exists() and not args.force:
                skipped += 1
                continue
            if target.exists() and not args.dry_run:
                shutil.copy2(target, target.with_suffix(".ogg.bak"))
            convert(root / source, target, is_loop(key), args.dry_run)
            converted += 1

    sounds = json.loads(SOUNDS_JSON.read_text(encoding="utf-8")) if SOUNDS_JSON.exists() else {}
    added = []
    for key, (subtitle, files) in SOUNDS.items():
        event = "boss." + key
        if event in sounds:
            continue
        sounds[event] = {
            "subtitle": f"subtitles.{NAMESPACE}.boss.{key}",
            "sounds": [{"name": resource_id(destination(key, i)), "stream": False}
                       for i in range(len(files))],
        }
        added.append(event)
    if added and not args.dry_run:
        SOUNDS_JSON.write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")

    print(f"converted {converted}, skipped {skipped} existing, added {len(added)} sounds.json keys")
    return 0


if __name__ == "__main__":
    sys.exit(main())
