#!/usr/bin/env python3
"""Final composition refinements for the premium RuStore gallery.

The refinement pass intentionally keeps every important UI panel fully inside the
1080x1920 canvas. Marketing accents may sit near a panel, but never cover labels,
values or controls. This keeps the gallery calm, readable and visually consistent
with the production WorkTime interface.
"""

from pathlib import Path
import sys

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate_rustore_assets as g  # noqa: E402


def hero_card() -> None:
    canvas = g.background(0)
    g.draw_heading(canvas, "Рабочий месяц — одним взглядом", "Смены · часы · доход")
    screen = Image.open(g.SCREENSHOTS["calendar_light"]()).convert("RGBA")

    g.pill(
        canvas,
        "6 смен · 47:30 · 23 725 ₽",
        (350, 500),
        accent=True,
    )
    g.shadowed_panel(
        canvas,
        screen,
        center=(540, 1240),
        width=575,
        radius=42,
        angle=-0.6,
        shadow_alpha=50,
        shadow_blur=26,
    )
    g.save(canvas, "01-hero-calendar.png")


def shift_card() -> None:
    canvas = g.background(1)
    g.draw_heading(
        canvas,
        "Добавил смену — всё посчитано",
        "Ставка, премия и штраф — в одной записи",
        title_size=66,
    )
    editor = Image.open(g.SCREENSHOTS["day_editor"]()).convert("RGBA")
    calendar = Image.open(g.SCREENSHOTS["calendar_light"]()).convert("RGBA")

    # Crop exactly on calendar row boundaries: no half-visible dates or values.
    calendar_focus = g.crop_norm(calendar, 0.02, 0.245, 0.98, 0.635)
    g.shadowed_panel(
        canvas,
        calendar_focus,
        center=(540, 680),
        width=640,
        radius=38,
        angle=-0.6,
        shadow_alpha=32,
        shadow_blur=22,
    )
    g.shadowed_panel(
        canvas,
        editor,
        center=(540, 1440),
        width=500,
        radius=42,
        angle=0.5,
        shadow_alpha=54,
        shadow_blur=26,
    )
    g.save(canvas, "02-shift-entry.png")


def summary_card() -> None:
    canvas = g.background(2)
    g.draw_heading(canvas, "Итоги за месяц и год", "Без таблиц и ручных пересчётов")
    year = Image.open(g.SCREENSHOTS["year_light"]()).convert("RGBA")

    g.shadowed_panel(
        canvas,
        year,
        center=(540, 1260),
        width=560,
        radius=42,
        angle=0.3,
        shadow_alpha=52,
        shadow_blur=26,
    )
    g.save(canvas, "03-year-summary.png")


def themes_card() -> None:
    canvas = g.background(3)
    g.draw_heading(
        canvas,
        "Комфортно и днём, и вечером",
        "Светлая, тёмная и системная темы",
        title_size=66,
    )
    light = Image.open(g.SCREENSHOTS["calendar_light"]()).convert("RGBA")
    dark = Image.open(g.SCREENSHOTS["calendar_dark"]()).convert("RGBA")

    # Matching crops and equal widths make the comparison deliberately symmetrical.
    light = g.crop_norm(light, 0.0, 0.02, 1.0, 0.84)
    dark = g.crop_norm(dark, 0.0, 0.02, 1.0, 0.84)

    g.pill(canvas, "Системная тема", (402, 760), accent=True)
    g.shadowed_panel(
        canvas,
        light,
        center=(310, 1300),
        width=440,
        radius=42,
        angle=-0.7,
        shadow_alpha=40,
        shadow_blur=24,
    )
    g.shadowed_panel(
        canvas,
        dark,
        center=(770, 1300),
        width=440,
        radius=42,
        angle=0.7,
        shadow_alpha=48,
        shadow_blur=24,
    )
    g.save(canvas, "04-themes.png")


def privacy_card() -> None:
    canvas = g.background(4, dark=True)
    g.draw_heading(
        canvas,
        "Рабочие данные остаются на устройстве",
        "Без аккаунта, рекламы и обязательного интернета",
        dark=True,
        title_size=64,
    )
    privacy = Image.open(g.SCREENSHOTS["privacy_dark"]()).convert("RGBA")

    g.pill(canvas, "локально на устройстве", (86, 555), dark=True, accent=True)
    g.shadowed_panel(
        canvas,
        privacy,
        center=(540, 1270),
        width=540,
        radius=42,
        angle=0.3,
        shadow_alpha=58,
        shadow_blur=28,
        border=(48, 54, 68, 255),
    )
    g.save(canvas, "05-privacy.png")


def export_card() -> None:
    canvas = g.background(5)
    g.draw_heading(canvas, "Экспортируй, когда нужно", "JSON для восстановления · CSV для таблиц")
    export = Image.open(g.SCREENSHOTS["export"]()).convert("RGBA")

    g.pill(canvas, "JSON", (100, 710), accent=True)
    g.pill(canvas, "CSV", (255, 710))
    g.shadowed_panel(
        canvas,
        export,
        center=(540, 1260),
        width=820,
        radius=44,
        angle=0.2,
        shadow_alpha=52,
        shadow_blur=26,
    )
    g.save(canvas, "06-export.png")


def main() -> None:
    hero_card()
    shift_card()
    summary_card()
    themes_card()
    privacy_card()
    export_card()


if __name__ == "__main__":
    main()
