#!/usr/bin/env python3
"""Render premium RuStore marketing cards from reviewed WorkTime UI screenshots."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
REFERENCE_ROOT = ROOT / "app/src/screenshotTestDebug/reference"
OUTPUT = ROOT / "docs/rustore/assets"
OUTPUT.mkdir(parents=True, exist_ok=True)

WIDTH = 1080
HEIGHT = 1920
SAFE_X = 86
FONT = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
BOLD_FONT = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")

LIGHT_BG = (244, 247, 252, 255)
LIGHT_FG = (24, 28, 35, 255)
LIGHT_MUTED = (86, 96, 112, 255)
DARK_BG = (14, 17, 23, 255)
DARK_FG = (245, 247, 251, 255)
DARK_MUTED = (184, 193, 208, 255)
BLUE = (59, 104, 178, 255)
BLUE_LIGHT = (202, 219, 247, 255)
BLUE_DARK = (33, 56, 93, 255)
WHITE = (255, 255, 255, 255)


def find_reference(prefix: str) -> Path:
    matches = sorted(REFERENCE_ROOT.rglob(f"{prefix}_*.png"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected exactly one screenshot for {prefix!r}, found {len(matches)}: {matches}")
    return matches[0]


SCREENSHOTS = {
    "calendar_light": lambda: find_reference("CalendarPopulatedLightScreenshot"),
    "calendar_dark": lambda: find_reference("CalendarPopulatedDarkScreenshot"),
    "year_light": lambda: find_reference("YearSummaryPopulatedScreenshot"),
    "day_editor": lambda: find_reference("StoreDayEditorPopulatedScreenshot"),
    "privacy_dark": lambda: find_reference("StorePrivacyDarkScreenshot"),
    "export": lambda: find_reference("StoreExportFormatScreenshot"),
}


def font(size: int, *, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(BOLD_FONT if bold else FONT), size)


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


def alpha_blur_blob(
    canvas: Image.Image,
    box: tuple[int, int, int, int],
    fill: tuple[int, int, int, int],
    blur: int,
) -> None:
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.ellipse(box, fill=fill)
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def background(index: int, *, dark: bool = False) -> Image.Image:
    base = DARK_BG if dark else LIGHT_BG
    canvas = Image.new("RGBA", (WIDTH, HEIGHT), base)
    # Shared carousel motif: a soft blue mass progresses across the six cards.
    x = -260 + index * 180
    if dark:
        alpha_blur_blob(canvas, (x, -180, x + 820, 640), (48, 83, 142, 88), 110)
        alpha_blur_blob(canvas, (WIDTH - 520, 1180, WIDTH + 250, 2050), (30, 47, 77, 105), 125)
    else:
        alpha_blur_blob(canvas, (x, -210, x + 840, 650), (164, 196, 244, 118), 115)
        alpha_blur_blob(canvas, (WIDTH - 420, 1280, WIDTH + 250, 2040), (205, 220, 244, 90), 120)
    return canvas


def wrap_lines(
    draw: ImageDraw.ImageDraw,
    text: str,
    text_font: ImageFont.FreeTypeFont,
    max_width: int,
) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        trial = word if not current else f"{current} {word}"
        if draw.textbbox((0, 0), trial, font=text_font)[2] <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_heading(
    canvas: Image.Image,
    title: str,
    subtitle: str,
    *,
    dark: bool = False,
    title_size: int = 70,
) -> None:
    draw = ImageDraw.Draw(canvas)
    foreground = DARK_FG if dark else LIGHT_FG
    muted = DARK_MUTED if dark else LIGHT_MUTED

    draw.text(
        (SAFE_X, 82),
        "WorkTime",
        font=font(29, bold=True),
        fill=BLUE_LIGHT if dark else BLUE,
    )

    title_font = font(title_size, bold=True)
    lines = wrap_lines(draw, title, title_font, WIDTH - SAFE_X * 2)
    if len(lines) > 2:
        title_font = font(title_size - 6, bold=True)
        lines = wrap_lines(draw, title, title_font, WIDTH - SAFE_X * 2)

    y = 160
    line_height = int(title_font.size * 1.12)
    for line in lines[:2]:
        draw.text((SAFE_X, y), line, font=title_font, fill=foreground)
        y += line_height

    subtitle_font = font(34)
    subtitle_lines = wrap_lines(draw, subtitle, subtitle_font, WIDTH - SAFE_X * 2)
    y += 18
    for line in subtitle_lines[:2]:
        draw.text((SAFE_X, y), line, font=subtitle_font, fill=muted)
        y += 45


def shadowed_panel(
    canvas: Image.Image,
    image: Image.Image,
    *,
    center: tuple[int, int],
    width: int,
    radius: int = 42,
    angle: float = 0.0,
    shadow_alpha: int = 52,
    shadow_blur: int = 26,
    border: tuple[int, int, int, int] | None = None,
) -> tuple[int, int, int, int]:
    image = image.convert("RGBA")
    scale = width / image.width
    image = image.resize((width, max(1, int(image.height * scale))), Image.Resampling.LANCZOS)
    image.putalpha(rounded_mask(image.size, radius))

    if border:
        framed = Image.new("RGBA", (image.width + 4, image.height + 4), (0, 0, 0, 0))
        frame_draw = ImageDraw.Draw(framed)
        frame_draw.rounded_rectangle(
            (0, 0, framed.width - 1, framed.height - 1),
            radius=radius + 2,
            fill=border,
        )
        framed.alpha_composite(image, (2, 2))
        image = framed

    if angle:
        image = image.rotate(angle, expand=True, resample=Image.Resampling.BICUBIC)

    x = int(center[0] - image.width / 2)
    y = int(center[1] - image.height / 2)

    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle(
        (x + 12, y + 18, x + image.width + 12, y + image.height + 18),
        radius=radius + 8,
        fill=(0, 0, 0, shadow_alpha),
    )
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(shadow_blur)))
    canvas.alpha_composite(image, (x, y))
    return x, y, image.width, image.height


def crop_norm(
    image: Image.Image,
    left: float,
    top: float,
    right: float,
    bottom: float,
) -> Image.Image:
    width, height = image.size
    return image.crop(
        (
            int(width * left),
            int(height * top),
            int(width * right),
            int(height * bottom),
        )
    )


def pill(
    canvas: Image.Image,
    text: str,
    xy: tuple[int, int],
    *,
    dark: bool = False,
    accent: bool = False,
) -> None:
    draw = ImageDraw.Draw(canvas)
    text_font = font(28, bold=True)
    bbox = draw.textbbox((0, 0), text, font=text_font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x, y = xy
    pad_x, pad_y = 24, 15

    if accent:
        fill = (64, 109, 181, 235) if dark else (221, 232, 250, 245)
        foreground = WHITE if dark else BLUE_DARK
    else:
        fill = (31, 36, 46, 230) if dark else (255, 255, 255, 238)
        foreground = DARK_FG if dark else LIGHT_FG

    draw.rounded_rectangle(
        (x, y, x + text_width + pad_x * 2, y + text_height + pad_y * 2),
        radius=28,
        fill=fill,
    )
    draw.text((x + pad_x, y + pad_y - 2), text, font=text_font, fill=foreground)


def save(canvas: Image.Image, filename: str) -> None:
    path = OUTPUT / filename
    canvas.convert("RGB").save(path, "PNG", optimize=True, compress_level=9)
    if path.stat().st_size > 3 * 1024 * 1024:
        quantized = canvas.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT)
        quantized.save(path, "PNG", optimize=True, compress_level=9)
    assert Image.open(path).size == (WIDTH, HEIGHT)
    assert path.stat().st_size <= 3 * 1024 * 1024


def hero_card() -> None:
    canvas = background(0)
    draw_heading(canvas, "Рабочий месяц — одним взглядом", "Смены · часы · доход")
    screen = Image.open(SCREENSHOTS["calendar_light"]()).convert("RGBA")

    shadowed_panel(
        canvas,
        screen,
        center=(555, 1300),
        width=780,
        angle=-2.0,
        shadow_alpha=58,
    )

    summary = crop_norm(screen, 0.03, 0.72, 0.97, 0.95)
    shadowed_panel(
        canvas,
        summary,
        center=(790, 745),
        width=540,
        radius=36,
        angle=2.4,
        shadow_alpha=40,
        shadow_blur=20,
    )
    save(canvas, "01-hero-calendar.png")


def shift_card() -> None:
    canvas = background(1)
    draw_heading(
        canvas,
        "Добавил смену — всё посчитано",
        "Ставка, премия и штраф — в одной записи",
        title_size=66,
    )
    editor = Image.open(SCREENSHOTS["day_editor"]()).convert("RGBA")
    calendar = Image.open(SCREENSHOTS["calendar_light"]()).convert("RGBA")

    calendar_focus = crop_norm(calendar, 0.02, 0.19, 0.98, 0.70)
    shadowed_panel(
        canvas,
        calendar_focus,
        center=(315, 1160),
        width=610,
        radius=42,
        angle=-5.0,
        shadow_alpha=36,
    )

    shadowed_panel(
        canvas,
        editor,
        center=(700, 1320),
        width=690,
        radius=44,
        angle=2.1,
        shadow_alpha=62,
    )
    pill(canvas, "8 ч · 500 ₽/ч", (92, 665), accent=True)
    save(canvas, "02-shift-entry.png")


def summary_card() -> None:
    canvas = background(2)
    draw_heading(canvas, "Итоги за месяц и год", "Без таблиц и ручных пересчётов")
    year = Image.open(SCREENSHOTS["year_light"]()).convert("RGBA")
    calendar = Image.open(SCREENSHOTS["calendar_light"]()).convert("RGBA")

    back = crop_norm(calendar, 0.05, 0.63, 0.95, 0.92)
    shadowed_panel(
        canvas,
        back,
        center=(300, 930),
        width=540,
        radius=40,
        angle=-6.0,
        shadow_alpha=28,
        shadow_blur=22,
    )

    shadowed_panel(
        canvas,
        year,
        center=(590, 1280),
        width=720,
        radius=44,
        angle=1.4,
        shadow_alpha=56,
    )

    totals = crop_norm(year, 0.06, 0.12, 0.94, 0.42)
    shadowed_panel(
        canvas,
        totals,
        center=(770, 750),
        width=560,
        radius=36,
        angle=-1.8,
        shadow_alpha=42,
        shadow_blur=20,
    )
    save(canvas, "03-year-summary.png")


def themes_card() -> None:
    canvas = background(3)
    draw_heading(
        canvas,
        "Комфортно и днём, и вечером",
        "Светлая, тёмная и системная темы",
        title_size=66,
    )
    light = Image.open(SCREENSHOTS["calendar_light"]()).convert("RGBA")
    dark = Image.open(SCREENSHOTS["calendar_dark"]()).convert("RGBA")
    light = crop_norm(light, 0.0, 0.02, 1.0, 0.85)
    dark = crop_norm(dark, 0.0, 0.02, 1.0, 0.85)

    shadowed_panel(
        canvas,
        light,
        center=(350, 1290),
        width=540,
        radius=44,
        angle=-5.0,
        shadow_alpha=45,
    )
    shadowed_panel(
        canvas,
        dark,
        center=(740, 1290),
        width=540,
        radius=44,
        angle=5.0,
        shadow_alpha=55,
    )
    pill(canvas, "Системная тема", (395, 690), accent=True)
    save(canvas, "04-themes.png")


def privacy_card() -> None:
    canvas = background(4, dark=True)
    draw_heading(
        canvas,
        "Рабочие данные остаются на устройстве",
        "Без аккаунта, рекламы и обязательного интернета",
        dark=True,
        title_size=64,
    )
    privacy = Image.open(SCREENSHOTS["privacy_dark"]()).convert("RGBA")
    calendar = Image.open(SCREENSHOTS["calendar_dark"]()).convert("RGBA")

    calendar_crop = crop_norm(calendar, 0.04, 0.12, 0.96, 0.78)
    shadowed_panel(
        canvas,
        calendar_crop,
        center=(310, 1320),
        width=560,
        radius=44,
        angle=-4.5,
        shadow_alpha=50,
        border=(42, 48, 61, 255),
    )
    shadowed_panel(
        canvas,
        privacy,
        center=(730, 1280),
        width=640,
        radius=44,
        angle=2.4,
        shadow_alpha=64,
        border=(48, 54, 68, 255),
    )
    pill(canvas, "локально на устройстве", (86, 700), dark=True, accent=True)
    save(canvas, "05-privacy.png")


def export_card() -> None:
    canvas = background(5)
    draw_heading(canvas, "Экспортируй, когда нужно", "JSON для восстановления · CSV для таблиц")
    export = Image.open(SCREENSHOTS["export"]()).convert("RGBA")

    shadowed_panel(
        canvas,
        export,
        center=(650, 1290),
        width=760,
        radius=44,
        angle=1.2,
        shadow_alpha=60,
    )

    pill(canvas, "JSON", (100, 735), accent=True)
    pill(canvas, "CSV", (255, 735))
    save(canvas, "06-export.png")


def main() -> None:
    # Remove the old technical gallery before rendering the final named series.
    for path in OUTPUT.glob("0*.png"):
        path.unlink()

    icon = Image.open(ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_exact.webp").convert("RGB")
    icon_path = OUTPUT / "rustore-icon-512.png"
    icon.resize((512, 512), Image.Resampling.LANCZOS).save(
        icon_path,
        "PNG",
        optimize=True,
        compress_level=9,
    )
    assert Image.open(icon_path).size == (512, 512)
    assert icon_path.stat().st_size <= 1 * 1024 * 1024

    hero_card()
    shift_card()
    summary_card()
    themes_card()
    privacy_card()
    export_card()

    expected = [
        "01-hero-calendar.png",
        "02-shift-entry.png",
        "03-year-summary.png",
        "04-themes.png",
        "05-privacy.png",
        "06-export.png",
        "rustore-icon-512.png",
    ]
    missing = [name for name in expected if not (OUTPUT / name).is_file()]
    if missing:
        raise RuntimeError(f"Missing generated RuStore assets: {missing}")

    for name in expected:
        path = OUTPUT / name
        print(f"{name}: {Image.open(path).size}, {path.stat().st_size / 1024:.1f} KiB")


if __name__ == "__main__":
    main()
