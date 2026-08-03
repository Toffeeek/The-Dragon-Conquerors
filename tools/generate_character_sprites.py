#!/usr/bin/env python3
"""Generate deterministic 48x48 pixel-art character sprite sheets.

Layout: 6 frames per row. Rows are grouped by state in this order:
IDLE, WALK, ATTACK, CAST, HURT, DEATH. Each state contains directions:
DOWN, LEFT, RIGHT, UP.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Tuple

from PIL import Image, ImageDraw

FRAME = 48
FRAMES = 6
DIRECTIONS = ("down", "left", "right", "up")
STATES = ("idle", "walk", "attack", "cast", "hurt", "death")
OUT_DIR = Path(__file__).resolve().parents[1] / "assets" / "characters"

Color = Tuple[int, int, int, int]


def rgba(hex_color: str, alpha: int = 255) -> Color:
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


@dataclass(frozen=True)
class ClassStyle:
    name: str
    body: Color
    body_dark: Color
    trim: Color
    skin: Color
    hair: Color
    metal: Color
    accent: Color
    effect: Color
    equipment: str


STYLES: Dict[str, ClassStyle] = {
    "Warrior": ClassStyle("Warrior", rgba("8E3030"), rgba("4B1820"), rgba("E2B449"), rgba("D9A06C"), rgba("402620"), rgba("C8D0D8"), rgba("6E7D8B"), rgba("FFB43C", 210), "sword_shield"),
    "Mage": ClassStyle("Mage", rgba("49328A"), rgba("241A55"), rgba("72D1E8"), rgba("E1B58C"), rgba("D6DCE8"), rgba("A98755"), rgba("9B5DE5"), rgba("58E6FF", 220), "staff"),
    "Archer": ClassStyle("Archer", rgba("2E7048"), rgba("183D2A"), rgba("D8B858"), rgba("D8A06F"), rgba("6B3E25"), rgba("9E7042"), rgba("79A95C"), rgba("D9F06B", 220), "bow"),
    "Paladin": ClassStyle("Paladin", rgba("D8D2B8"), rgba("70798A"), rgba("E8C84F"), rgba("D7A071"), rgba("C7B07A"), rgba("E8EDF0"), rgba("3D74B8"), rgba("FFF09A", 225), "hammer_shield"),
    "Rogue": ClassStyle("Rogue", rgba("30343C"), rgba("15181E"), rgba("7B4FB3"), rgba("C98962"), rgba("1E1A22"), rgba("BFC6CE"), rgba("8E52C7"), rgba("C85CFF", 215), "daggers"),
}


def px(draw: ImageDraw.ImageDraw, box, fill, outline=None):
    draw.rectangle(box, fill=fill, outline=outline)


def poly(draw: ImageDraw.ImageDraw, pts, fill, outline=None):
    draw.polygon(pts, fill=fill, outline=outline)


def ellipse(draw: ImageDraw.ImageDraw, box, fill, outline=None):
    draw.ellipse(box, fill=fill, outline=outline)


def mirror_x(x: int) -> int:
    return FRAME - 1 - x


def draw_shadow(draw: ImageDraw.ImageDraw, alpha: int = 90, stretch: int = 0):
    ellipse(draw, (13 - stretch, 38, 35 + stretch, 43), (8, 10, 14, alpha))


def direction_sign(direction: str) -> int:
    return -1 if direction == "left" else 1


def draw_body(draw: ImageDraw.ImageDraw, style: ClassStyle, direction: str, yoff: int, leg_phase: int, hurt_flash: bool = False, fallen: float = 0.0):
    outline = rgba("161922")
    flash = rgba("FFF0F0") if hurt_flash else None

    if fallen > 0:
        # Compact side-on fallen silhouette used by death frames.
        y = 29 + int(fallen * 7)
        x = 14 + int(fallen * 5)
        px(draw, (x, y, x + 20, y + 8), flash or style.body, outline)
        ellipse(draw, (x + 17, y - 2, x + 25, y + 6), style.skin, outline)
        return

    cx = 24
    head_y = 10 + yoff
    torso_y = 20 + yoff
    side = direction_sign(direction)

    # Legs and boots.
    lstep = leg_phase
    rstep = -leg_phase
    px(draw, (18 + lstep, 31 + yoff, 22 + lstep, 39 + yoff), style.body_dark, outline)
    px(draw, (26 + rstep, 31 + yoff, 30 + rstep, 39 + yoff), style.body_dark, outline)
    px(draw, (17 + lstep, 38 + yoff, 23 + lstep, 41 + yoff), rgba("27242A"), outline)
    px(draw, (25 + rstep, 38 + yoff, 31 + rstep, 41 + yoff), rgba("27242A"), outline)

    # Cloak/back layer.
    if style.name in ("Mage", "Rogue", "Archer"):
        poly(draw, [(16, torso_y + 2), (32, torso_y + 2), (35, 37 + yoff), (13, 37 + yoff)], style.body_dark, outline)

    # Torso.
    torso_fill = flash or style.body
    px(draw, (16, torso_y, 32, 33 + yoff), torso_fill, outline)
    px(draw, (18, torso_y + 2, 30, torso_y + 5), style.trim, None)
    px(draw, (22, torso_y, 26, 33 + yoff), style.body_dark, None)

    # Arms.
    if direction in ("left", "right"):
        front_x = 31 if side > 0 else 13
        back_x = 13 if side > 0 else 31
        px(draw, (front_x, torso_y + 2, front_x + 4, torso_y + 12), style.body, outline)
        px(draw, (back_x, torso_y + 3, back_x + 3, torso_y + 11), style.body_dark, outline)
    else:
        px(draw, (12, torso_y + 2, 17, torso_y + 12), style.body_dark if direction == "up" else style.body, outline)
        px(draw, (31, torso_y + 2, 36, torso_y + 12), style.body_dark if direction == "up" else style.body, outline)

    # Head/hood/helmet.
    ellipse(draw, (18, head_y, 30, head_y + 12), style.skin, outline)
    if style.name == "Warrior":
        px(draw, (17, head_y - 2, 31, head_y + 5), style.metal, outline)
        px(draw, (19, head_y + 4, 29, head_y + 7), style.accent, outline)
        px(draw, (22, head_y - 5, 26, head_y - 1), style.trim, outline)
    elif style.name == "Paladin":
        px(draw, (17, head_y - 2, 31, head_y + 6), style.metal, outline)
        px(draw, (20, head_y + 4, 28, head_y + 7), style.accent, outline)
        px(draw, (23, head_y - 6, 25, head_y - 1), style.trim, outline)
    elif style.name == "Mage":
        poly(draw, [(16, head_y + 3), (24, head_y - 8), (33, head_y + 5)], style.body_dark, outline)
        px(draw, (17, head_y + 3, 31, head_y + 7), style.trim, outline)
    elif style.name == "Archer":
        poly(draw, [(17, head_y + 3), (24, head_y - 5), (31, head_y + 3)], style.body_dark, outline)
        px(draw, (18, head_y + 1, 30, head_y + 5), style.body, outline)
    elif style.name == "Rogue":
        poly(draw, [(16, head_y + 5), (20, head_y - 4), (28, head_y - 4), (33, head_y + 5)], style.body_dark, outline)
        px(draw, (18, head_y + 4, 30, head_y + 8), style.body, outline)

    # Eyes / direction cue.
    eye = rgba("EDF8FF")
    if direction == "down":
        px(draw, (20, head_y + 6, 21, head_y + 7), eye)
        px(draw, (27, head_y + 6, 28, head_y + 7), eye)
    elif direction == "left":
        px(draw, (19, head_y + 6, 20, head_y + 7), eye)
    elif direction == "right":
        px(draw, (28, head_y + 6, 29, head_y + 7), eye)


def draw_equipment(draw: ImageDraw.ImageDraw, style: ClassStyle, direction: str, state: str, frame: int, yoff: int):
    outline = rgba("151820")
    side = direction_sign(direction)
    attack_t = frame / (FRAMES - 1)
    swing = int(round((1.0 - abs(attack_t * 2 - 1)) * 10))

    if style.equipment == "sword_shield":
        # Shield stays visible on the off-hand.
        shield_x = 10 if direction != "left" else 31
        ellipse(draw, (shield_x, 23 + yoff, shield_x + 8, 35 + yoff), style.accent, outline)
        px(draw, (shield_x + 2, 26 + yoff, shield_x + 6, 31 + yoff), style.trim, None)
        if state == "attack":
            if direction in ("left", "right"):
                base_x = 31 if side > 0 else 16
                tip_x = base_x + side * (7 + swing)
                draw.line((base_x, 25 + yoff, tip_x, 18 + yoff - swing // 3), fill=style.metal, width=3)
                draw.line((tip_x, 18 + yoff - swing // 3, tip_x + side * 4, 14 + yoff - swing // 3), fill=style.metal, width=2)
            else:
                draw.line((29, 26 + yoff, 34 + swing // 2, 12 + yoff), fill=style.metal, width=3)
        else:
            draw.line((34, 20 + yoff, 36, 36 + yoff), fill=style.metal, width=3)
            px(draw, (31, 21 + yoff, 38, 23 + yoff), style.trim, outline)

    elif style.equipment == "hammer_shield":
        shield_x = 9 if direction != "left" else 31
        ellipse(draw, (shield_x, 22 + yoff, shield_x + 9, 36 + yoff), style.accent, outline)
        px(draw, (shield_x + 3, 25 + yoff, shield_x + 6, 33 + yoff), style.trim)
        if state == "attack":
            bx = 31 if side > 0 else 16
            tx = bx + side * (6 + swing)
            draw.line((bx, 28 + yoff, tx, 15 + yoff), fill=rgba("8A6B3A"), width=3)
            px(draw, (tx - 4, 11 + yoff, tx + 4, 17 + yoff), style.metal, outline)
        else:
            draw.line((34, 22 + yoff, 36, 36 + yoff), fill=rgba("8A6B3A"), width=3)
            px(draw, (31, 18 + yoff, 39, 24 + yoff), style.metal, outline)

    elif style.equipment == "staff":
        sx = 34 if direction != "left" else 13
        if state == "attack":
            sx += side * min(5, swing // 2)
        draw.line((sx, 16 + yoff, sx, 39 + yoff), fill=rgba("8B633A"), width=3)
        ellipse(draw, (sx - 4, 10 + yoff, sx + 4, 18 + yoff), style.effect, outline)
        px(draw, (sx - 1, 12 + yoff, sx + 1, 16 + yoff), rgba("E9FCFF"), None)

    elif style.equipment == "bow":
        bx = 35 if direction != "left" else 13
        if state == "attack":
            bx += side * min(4, swing // 2)
        # Curved bow approximation with segmented pixels.
        draw.arc((bx - 5, 16 + yoff, bx + 5, 36 + yoff), 70 if side > 0 else 110, 290 if side > 0 else 250, fill=style.trim, width=2)
        draw.line((bx, 17 + yoff, bx, 35 + yoff), fill=rgba("D7D1C5"), width=1)
        if state == "attack" and frame >= 2:
            arrow_len = 6 + frame * 3
            ax = bx + side * arrow_len
            draw.line((bx, 26 + yoff, ax, 26 + yoff), fill=style.metal, width=2)
            poly(draw, [(ax, 24 + yoff), (ax + side * 4, 26 + yoff), (ax, 28 + yoff)], style.trim, outline)
        # quiver
        px(draw, (14, 18 + yoff, 18, 33 + yoff), rgba("76502F"), outline)
        draw.line((15, 18 + yoff, 13, 12 + yoff), fill=style.metal, width=1)
        draw.line((17, 18 + yoff, 18, 11 + yoff), fill=style.metal, width=1)

    elif style.equipment == "daggers":
        if state == "attack":
            reach = 4 + swing
            lx = 16 - reach if direction == "left" else 15
            rx = 32 + reach if direction == "right" else 33
            draw.line((17, 27 + yoff, lx, 20 + yoff), fill=style.metal, width=2)
            draw.line((31, 27 + yoff, rx, 20 + yoff), fill=style.metal, width=2)
        else:
            draw.line((15, 26 + yoff, 10, 34 + yoff), fill=style.metal, width=2)
            draw.line((33, 26 + yoff, 38, 34 + yoff), fill=style.metal, width=2)


def draw_effect(draw: ImageDraw.ImageDraw, style: ClassStyle, direction: str, state: str, frame: int, yoff: int):
    side = direction_sign(direction)
    if state == "attack" and frame in (2, 3, 4):
        alpha = 190 - abs(3 - frame) * 45
        if style.name in ("Mage",):
            x = 34 + side * (frame * 3)
            ellipse(draw, (x - 5, 17 + yoff, x + 5, 27 + yoff), style.effect[:-1] + (alpha,))
        elif style.name == "Archer":
            pass
        else:
            if direction in ("left", "right"):
                x0 = 24 + side * 8
                x1 = 24 + side * (14 + frame * 2)
                draw.arc((min(x0, x1) - 4, 10 + yoff, max(x0, x1) + 4, 35 + yoff), 260 if side > 0 else 80, 100 if side > 0 else 280, fill=style.effect[:-1] + (alpha,), width=2)
            else:
                draw.arc((15, 7 + yoff, 42, 34 + yoff), 230, 330, fill=style.effect[:-1] + (alpha,), width=2)

    if state == "cast":
        pulse = 3 + (frame % 3) * 2
        ellipse(draw, (24 - pulse, 22 + yoff - pulse, 24 + pulse, 22 + yoff + pulse), style.effect[:-1] + (110,))
        for i in range(4):
            off = (frame * 3 + i * 9) % 28
            x = 10 + off
            yy = 38 - ((frame * 4 + i * 7) % 22)
            px(draw, (x, yy, x + 1, yy + 1), style.effect)
        if frame >= 3:
            distance = 5 + (frame - 2) * 5
            x = 24 + side * distance if direction in ("left", "right") else 24
            y = 22 - distance if direction == "up" else 22 + distance if direction == "down" else 20
            ellipse(draw, (x - 3, y - 3, x + 3, y + 3), style.effect)

    if state == "hurt":
        for i in range(frame + 1):
            x = 10 + ((i * 11 + frame * 5) % 28)
            y = 11 + ((i * 7 + frame * 3) % 20)
            px(draw, (x, y, x + 1, y + 1), rgba("FF6B6B", 210))


def render_frame(style: ClassStyle, state: str, direction: str, frame: int) -> Image.Image:
    img = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")

    if state == "idle":
        yoff = 1 if frame in (2, 3) else 0
        leg = 0
    elif state == "walk":
        cycle = (0, 1, 2, 0, -2, -1)
        leg = cycle[frame]
        yoff = 0 if frame % 2 == 0 else -1
    else:
        leg = 0
        yoff = 0

    if state == "death":
        progress = frame / (FRAMES - 1)
        draw_shadow(draw, alpha=max(25, 90 - frame * 10), stretch=frame)
        draw_body(draw, style, direction, 0, 0, fallen=progress)
        if frame < 3:
            draw_equipment(draw, style, direction, "idle", frame, 0)
        return img

    draw_shadow(draw, alpha=100 if state != "cast" else 70, stretch=1 if state == "attack" else 0)

    recoil = 0
    if state == "hurt":
        recoil = (frame % 3) - 1
        if direction == "right": recoil = -recoil
        elif direction == "left": recoil = recoil
        yoff = abs(recoil)

    draw_body(draw, style, direction, yoff, leg, hurt_flash=(state == "hurt" and frame % 2 == 1))
    draw_equipment(draw, style, direction, state, frame, yoff)
    draw_effect(draw, style, direction, state, frame, yoff)

    return img


def generate_sheet(style: ClassStyle) -> Image.Image:
    sheet = Image.new("RGBA", (FRAME * FRAMES, FRAME * len(STATES) * len(DIRECTIONS)), (0, 0, 0, 0))
    row = 0
    for state in STATES:
        for direction in DIRECTIONS:
            for frame in range(FRAMES):
                sheet.alpha_composite(render_frame(style, state, direction, frame), (frame * FRAME, row * FRAME))
            row += 1
    return sheet


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for style in STYLES.values():
        output = OUT_DIR / f"{style.name}.png"
        generate_sheet(style).save(output, optimize=True)
        print(f"generated {output.relative_to(OUT_DIR.parents[1])}")


if __name__ == "__main__":
    main()
