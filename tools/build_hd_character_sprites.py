#!/usr/bin/env python3
"""Build game-ready 96x96 high-detail class sprite sheets.

The animation geometry comes from the proven 48x48 sheets, while the colour,
lighting and one-pixel material detail are upgraded for the richer painted
pixel-art direction used by the new class concepts. This preserves every
movement/attack/cast/hurt/death frame and keeps the runtime grid deterministic.

Output layout per file: 6 columns x 24 rows, 96x96 per frame (576x2304).
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import argparse
import hashlib

import numpy as np
from PIL import Image
from scipy import ndimage as ndi

FRAME_IN = 48
FRAME_OUT = 96
COLS = 6
ROWS = 24


@dataclass(frozen=True)
class Style:
    shadow: tuple[int, int, int]
    outline: tuple[int, int, int]
    cool: tuple[int, int, int]
    cool_light: tuple[int, int, int]
    warm: tuple[int, int, int]
    warm_light: tuple[int, int, int]
    accent: tuple[int, int, int]
    accent_light: tuple[int, int, int]
    cloth: tuple[int, int, int]
    cloth_light: tuple[int, int, int]


STYLES: dict[str, Style] = {
    "Warrior": Style(
        shadow=(15, 17, 24), outline=(9, 10, 15),
        cool=(71, 83, 100), cool_light=(205, 218, 224),
        warm=(137, 67, 35), warm_light=(235, 157, 61),
        accent=(112, 37, 31), accent_light=(194, 67, 43),
        cloth=(77, 35, 36), cloth_light=(145, 57, 48),
    ),
    "Mage": Style(
        shadow=(20, 14, 31), outline=(10, 8, 17),
        cool=(37, 38, 92), cool_light=(116, 108, 204),
        warm=(103, 65, 31), warm_light=(210, 154, 67),
        accent=(22, 146, 185), accent_light=(100, 238, 255),
        cloth=(52, 28, 107), cloth_light=(113, 64, 178),
    ),
    "Archer": Style(
        shadow=(18, 22, 14), outline=(9, 12, 8),
        cool=(45, 67, 35), cool_light=(116, 148, 72),
        warm=(89, 57, 29), warm_light=(190, 135, 62),
        accent=(40, 106, 35), accent_light=(105, 190, 68),
        cloth=(27, 74, 31), cloth_light=(74, 137, 50),
    ),
    "Paladin": Style(
        shadow=(26, 31, 39), outline=(12, 15, 20),
        cool=(64, 90, 133), cool_light=(101, 160, 226),
        warm=(146, 93, 27), warm_light=(246, 191, 68),
        accent=(22, 76, 148), accent_light=(67, 142, 230),
        cloth=(174, 164, 133), cloth_light=(242, 233, 197),
    ),
    "Rogue": Style(
        shadow=(11, 10, 16), outline=(5, 5, 9),
        cool=(42, 42, 51), cool_light=(113, 112, 128),
        warm=(72, 51, 38), warm_light=(143, 103, 66),
        accent=(67, 31, 105), accent_light=(167, 84, 219),
        cloth=(25, 23, 31), cloth_light=(67, 61, 77),
    ),
}


def rgb_to_hsv(rgb: np.ndarray) -> np.ndarray:
    rgb = rgb.astype(np.float32) / 255.0
    mx = rgb.max(axis=-1)
    mn = rgb.min(axis=-1)
    diff = mx - mn
    h = np.zeros_like(mx)
    mask = diff > 1e-6
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    sel = mask & (mx == r)
    h[sel] = ((g[sel] - b[sel]) / diff[sel]) % 6
    sel = mask & (mx == g)
    h[sel] = (b[sel] - r[sel]) / diff[sel] + 2
    sel = mask & (mx == b)
    h[sel] = (r[sel] - g[sel]) / diff[sel] + 4
    h /= 6.0
    s = np.where(mx <= 1e-6, 0.0, diff / np.maximum(mx, 1e-6))
    return np.stack([h, s, mx], axis=-1)


def lerp(a: np.ndarray, b: np.ndarray, t: np.ndarray) -> np.ndarray:
    return a * (1.0 - t[..., None]) + b * t[..., None]


def ramp(dark: tuple[int, int, int], light: tuple[int, int, int], t: np.ndarray) -> np.ndarray:
    a = np.array(dark, dtype=np.float32)
    b = np.array(light, dtype=np.float32)
    curved = np.clip(t, 0, 1) ** 0.82
    return lerp(np.broadcast_to(a, t.shape + (3,)), np.broadcast_to(b, t.shape + (3,)), curved)


def recolour(base_rgb: np.ndarray, mask: np.ndarray, style: Style) -> tuple[np.ndarray, np.ndarray]:
    hsv = rgb_to_hsv(base_rgb)
    h, s, v = hsv[..., 0], hsv[..., 1], hsv[..., 2]

    # Material estimates from the original sprites. They deliberately overlap;
    # the priority order below preserves bright magic/gold/metal accents.
    magic = mask & (s > 0.48) & ((h > 0.47) & (h < 0.76)) & (v > 0.45)
    warm = mask & (s > 0.42) & ((h < 0.16) | (h > 0.94))
    green = mask & (s > 0.34) & (h >= 0.16) & (h < 0.48)
    metal = mask & (s < 0.30) & (v > 0.32)
    dark = mask & (v < 0.22)
    cloth = mask & ~(magic | warm | green | metal | dark)

    out = np.zeros(base_rgb.shape, dtype=np.float32)
    lum = np.clip(v * 1.10, 0, 1)
    out[dark] = ramp(style.shadow, style.cloth, lum)[dark]
    out[cloth] = ramp(style.cloth, style.cloth_light, lum)[cloth]
    out[metal] = ramp(style.cool, style.cool_light, lum)[metal]
    out[warm] = ramp(style.warm, style.warm_light, lum)[warm]
    out[green] = ramp(style.cloth, style.accent_light, lum)[green]
    out[magic] = ramp(style.accent, style.accent_light, lum)[magic]

    # Keep a little of the source hue so class identity and small details survive.
    source = base_rgb.astype(np.float32)
    out = out * 0.82 + source * 0.18
    material = np.zeros(mask.shape, dtype=np.uint8)
    material[cloth] = 1
    material[metal] = 2
    material[warm] = 3
    material[magic | green] = 4
    return out, material


def detail_frame(frame: Image.Image, class_name: str, frame_index: int) -> Image.Image:
    style = STYLES[class_name]
    src = np.array(frame.convert("RGBA"), dtype=np.uint8)
    src = np.array(Image.fromarray(src).resize((FRAME_OUT, FRAME_OUT), Image.Resampling.NEAREST))
    rgb = src[..., :3]
    alpha = src[..., 3]
    mask = alpha > 12

    coloured, material = recolour(rgb, mask, style)

    # Directional top-left light and stronger lower-right occlusion.
    distance = ndi.distance_transform_edt(mask)
    gy, gx = np.gradient(distance.astype(np.float32))
    directional = np.clip((-gx - gy) * 0.035, -0.12, 0.14)
    edge = np.exp(-distance / 2.4)
    y_norm = np.linspace(0, 1, FRAME_OUT, dtype=np.float32)[:, None]
    light = 1.03 + directional - edge * (0.10 + 0.08 * y_norm)
    coloured *= light[..., None]

    # One-pixel texture. Seeded per class/frame, so builds are deterministic.
    seed_bytes = hashlib.sha256(f"{class_name}:{frame_index}".encode()).digest()[:8]
    rng = np.random.default_rng(int.from_bytes(seed_bytes, "big"))
    noise = rng.normal(0.0, 1.0, mask.shape).astype(np.float32)
    noise = ndi.gaussian_filter(noise, sigma=0.45)
    checker = ((np.indices(mask.shape).sum(axis=0) + frame_index) & 1).astype(np.float32) * 2 - 1

    amplitude = np.zeros(mask.shape, dtype=np.float32)
    amplitude[material == 1] = 5.5   # cloth/leather grain
    amplitude[material == 2] = 7.0   # hammered metal
    amplitude[material == 3] = 6.5   # gold/warm trim
    amplitude[material == 4] = 8.0   # magic/specular accents
    texture = noise * amplitude + checker * amplitude * 0.20
    coloured += texture[..., None]

    # Micro-specular pixels on metal/gold/magic, mostly on upper-left surfaces.
    yy, xx = np.indices(mask.shape)
    sparkle_pattern = ((xx * 7 + yy * 11 + frame_index * 13) % 29 == 0)
    upper = yy < FRAME_OUT * 0.72
    spark = mask & upper & sparkle_pattern & np.isin(material, [2, 3, 4])
    coloured[spark] = np.minimum(255, coloured[spark] * 1.22 + 18)

    # Reinforce internal pixel-cluster seams without blurring the silhouette.
    original_small = np.array(frame.convert("RGBA"), dtype=np.uint8)
    small_rgb = original_small[..., :3].astype(np.int16)
    discontinuity = np.zeros((FRAME_IN, FRAME_IN), dtype=bool)
    discontinuity[:, 1:] |= np.max(np.abs(small_rgb[:, 1:] - small_rgb[:, :-1]), axis=2) > 48
    discontinuity[1:, :] |= np.max(np.abs(small_rgb[1:, :] - small_rgb[:-1, :]), axis=2) > 48
    seam = np.array(Image.fromarray((discontinuity * 255).astype(np.uint8)).resize(
        (FRAME_OUT, FRAME_OUT), Image.Resampling.NEAREST)) > 0
    seam &= mask & (distance > 1.0)
    coloured[seam] *= 0.88

    # Crisp outside outline plus a softer inner contact shadow.
    outer = ndi.binary_dilation(mask, iterations=1) & ~mask
    inner_edge = mask & ~ndi.binary_erosion(mask, iterations=1)

    out = np.zeros((FRAME_OUT, FRAME_OUT, 4), dtype=np.uint8)
    out[..., :3] = np.clip(coloured, 0, 255).astype(np.uint8)
    out[..., 3] = alpha
    out[inner_edge, :3] = (out[inner_edge, :3].astype(np.float32) * 0.78).astype(np.uint8)
    out[outer, :3] = np.array(style.outline, dtype=np.uint8)
    out[outer, 3] = 255

    # Preserve smooth spell fringes while ensuring the body stays fully opaque.
    solid = mask & (alpha > 160)
    out[solid, 3] = 255
    return Image.fromarray(out, mode="RGBA")


def build_sheet(source: Path, target: Path, class_name: str) -> None:
    sheet = Image.open(source).convert("RGBA")
    expected = (COLS * FRAME_IN, ROWS * FRAME_IN)
    if sheet.size != expected:
        raise ValueError(f"{source} must be {expected[0]}x{expected[1]}, got {sheet.size}")

    output = Image.new("RGBA", (COLS * FRAME_OUT, ROWS * FRAME_OUT), (0, 0, 0, 0))
    index = 0
    for row in range(ROWS):
        for col in range(COLS):
            box = (col * FRAME_IN, row * FRAME_IN, (col + 1) * FRAME_IN, (row + 1) * FRAME_IN)
            frame = sheet.crop(box)
            detailed = detail_frame(frame, class_name, index)
            output.alpha_composite(detailed, (col * FRAME_OUT, row * FRAME_OUT))
            index += 1

    target.parent.mkdir(parents=True, exist_ok=True)
    output.save(target, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--assets", type=Path, default=Path(__file__).resolve().parents[1] / "assets" / "characters")
    args = parser.parse_args()

    for class_name in STYLES:
        source = args.assets / f"{class_name}.png"
        target = args.assets / f"{class_name}_HD.png"
        build_sheet(source, target, class_name)
        print(f"built {target.name}: {COLS * FRAME_OUT}x{ROWS * FRAME_OUT}")


if __name__ == "__main__":
    main()
