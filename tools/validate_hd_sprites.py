#!/usr/bin/env python3
"""Validate the runtime HD character sheets and their exact animation grid."""
from pathlib import Path
import sys
from PIL import Image

FRAME = 96
COLS = 6
ROWS = 24
CLASSES = ("Warrior", "Mage", "Archer", "Paladin", "Rogue")


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    asset_dir = root / "assets" / "characters"
    failed = False

    for class_name in CLASSES:
        path = asset_dir / f"{class_name}_HD.png"
        if not path.exists():
            print(f"[FAIL] missing {path.relative_to(root)}")
            failed = True
            continue

        image = Image.open(path).convert("RGBA")
        expected = (COLS * FRAME, ROWS * FRAME)
        if image.size != expected:
            print(f"[FAIL] {path.name}: expected {expected}, got {image.size}")
            failed = True
            continue

        empty = []
        for row in range(ROWS):
            for col in range(COLS):
                frame = image.crop((
                    col * FRAME, row * FRAME,
                    (col + 1) * FRAME, (row + 1) * FRAME,
                ))
                if frame.getchannel("A").getbbox() is None:
                    empty.append((row, col))

        if empty:
            print(f"[FAIL] {path.name}: empty cells {empty}")
            failed = True
        else:
            print(f"[PASS] {path.name}: {COLS}x{ROWS} cells, all 144 frames populated")

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
