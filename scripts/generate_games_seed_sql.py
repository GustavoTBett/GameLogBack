#!/usr/bin/env python3
"""Generate Liquibase seed SQL from Video Games Data.csv.

This script aggregates duplicated game titles across platforms and emits
idempotent PostgreSQL SQL statements for:
- genre
- game
- game_genre
- game_platform
"""

from __future__ import annotations

import argparse
import csv
import re
import unicodedata
from collections import OrderedDict
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Iterable


PLATFORM_MAPPING = {
    # PlayStation
    "PS": "PLAYSTATION",
    "PS2": "PLAYSTATION",
    "PS3": "PLAYSTATION",
    "PS4": "PLAYSTATION",
    "PS5": "PLAYSTATION",
    "PSN": "PLAYSTATION",
    "PSP": "PLAYSTATION",
    "PSV": "PLAYSTATION",
    # Xbox
    "XB": "XBOX",
    "XBL": "XBOX",
    "X360": "XBOX",
    "XOne": "XBOX",
    "XS": "XBOX",
    "Series": "XBOX",
    # Nintendo
    "Wii": "NINTENDO",
    "WiiU": "NINTENDO",
    "NES": "NINTENDO",
    "SNES": "NINTENDO",
    "N64": "NINTENDO",
    "GC": "NINTENDO",
    "GB": "NINTENDO",
    "GBC": "NINTENDO",
    "GBA": "NINTENDO",
    "DS": "NINTENDO",
    "3DS": "NINTENDO",
    "NS": "NINTENDO",
    "FDS": "NINTENDO",
    "VC": "NINTENDO",
    "iQue": "NINTENDO",
    # PC
    "PC": "PC",
    "WinP": "PC",
    "Linux": "PC",
    "OSX": "PC",
    "MSX": "PC",
    "C64": "PC",
    "C128": "PC",
    "ACPC": "PC",
    "Amig": "PC",
    "ZXS": "PC",
    "OR": "PC",
    "BBCM": "PC",
    "ApII": "PC",
    "FMT": "PC",
    # Mobile
    "Mob": "MOBILE",
    "iOS": "MOBILE",
    "And": "MOBILE",
    "DSi": "MOBILE",
    "DSiW": "MOBILE",
    # Arcade-like
    "Arc": "ARCADE",
    "AJ": "ARCADE",
    "AST": "ARCADE",
    # Extra mappings ("alguns legais")
    "Cloud": "CLOUD",
    "VR": "VR",
}


ALLOWED_PLATFORMS = {
    "PC",
    "PLAYSTATION",
    "XBOX",
    "NINTENDO",
    "MOBILE",
    "CLOUD",
    "VR",
    "ARCADE",
}


@dataclass
class GameAggregate:
    name: str
    slug: str
    description: str | None = None
    release_date: str | None = None
    developer: str | None = None
    publisher: str | None = None
    cover_url: str | None = None
    default_rating: float = 0.0
    average_rating: float = 0.0
    genres: set[str] = field(default_factory=set)
    platforms: set[str] = field(default_factory=set)


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    value = re.sub(r"[^a-zA-Z0-9]+", "-", normalized).strip("-").lower()
    return value or "game"


def parse_date(value: str) -> str | None:
    value = (value or "").strip()
    if not value:
        return None
    try:
        return datetime.strptime(value, "%d-%m-%Y").strftime("%Y-%m-%d")
    except ValueError:
        return None


def parse_score(value: str) -> float:
    value = (value or "").strip().replace(",", ".")
    if not value:
        return 0.0
    try:
        score = float(value)
    except ValueError:
        return 0.0

    # DB column numeric(3,2) cannot store 10.00, so cap safely at 9.99.
    score = max(0.0, min(9.99, score))
    return round(score, 2)


def normalize_text(value: str) -> str | None:
    value = (value or "").strip()
    return value or None


def normalize_cover_url(value: str, prefix: str) -> str | None:
    path = normalize_text(value)
    if path is None:
        return None
    if prefix:
        return f"{prefix.rstrip('/')}" + (path if path.startswith("/") else f"/{path}")
    return path


def map_platform(raw_console: str) -> str | None:
    raw_console = (raw_console or "").strip()
    if not raw_console:
        return None
    mapped = PLATFORM_MAPPING.get(raw_console)
    if mapped in ALLOWED_PLATFORMS:
        return mapped
    return None


def parse_genres(raw_genre: str) -> set[str]:
    raw_genre = (raw_genre or "").strip()
    if not raw_genre:
        return set()
    parts = re.split(r"[|,;/]", raw_genre)
    genres = {part.strip() for part in parts if part.strip()}
    return genres


def sql_literal(value: str | None) -> str:
    if value is None:
        return "NULL"
    escaped = value.replace("'", "''")
    return f"'{escaped}'"


def chunked(items: list[str], size: int) -> Iterable[list[str]]:
    for i in range(0, len(items), size):
        yield items[i : i + size]


def build_description(genre: str | None, publisher: str | None, developer: str | None) -> str:
    details = ["Imported from CSV dataset"]
    if genre:
        details.append(f"Genre: {genre}")
    if developer:
        details.append(f"Developer: {developer}")
    if publisher:
        details.append(f"Publisher: {publisher}")
    return ". ".join(details)


def aggregate_rows(csv_path: Path, cover_prefix: str) -> OrderedDict[str, GameAggregate]:
    games: OrderedDict[str, GameAggregate] = OrderedDict()
    used_slugs: set[str] = set()

    with csv_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            title = normalize_text(row.get("title", ""))
            if not title:
                continue

            title_key = title.casefold()

            if title_key not in games:
                base_slug = slugify(title)
                slug = base_slug
                index = 1
                while slug in used_slugs:
                    slug = f"{base_slug}-{index}"
                    index += 1
                used_slugs.add(slug)

                genre_raw = normalize_text(row.get("genre", ""))
                publisher = normalize_text(row.get("publisher", ""))
                developer = normalize_text(row.get("developer", ""))
                games[title_key] = GameAggregate(
                    name=title,
                    slug=slug,
                    description=build_description(genre_raw, publisher, developer),
                    release_date=parse_date(row.get("release_date", "")),
                    developer=developer,
                    publisher=publisher,
                    cover_url=normalize_cover_url(row.get("img", ""), cover_prefix),
                    default_rating=parse_score(row.get("critic_score", "")),
                    average_rating=parse_score(row.get("critic_score", "")),
                )

            game = games[title_key]

            # Merge data from repeated title rows (multi-platform dataset).
            game.default_rating = max(game.default_rating, parse_score(row.get("critic_score", "")))
            game.average_rating = game.default_rating

            if game.release_date is None:
                game.release_date = parse_date(row.get("release_date", ""))
            if game.developer is None:
                game.developer = normalize_text(row.get("developer", ""))
            if game.publisher is None:
                game.publisher = normalize_text(row.get("publisher", ""))
            if game.cover_url is None:
                game.cover_url = normalize_cover_url(row.get("img", ""), cover_prefix)

            game.genres.update(parse_genres(row.get("genre", "")))

            mapped_platform = map_platform(row.get("console", ""))
            if mapped_platform:
                game.platforms.add(mapped_platform)

    return games


def write_sql(games: OrderedDict[str, GameAggregate], output_path: Path) -> None:
    genres = sorted({genre for game in games.values() for genre in game.genres})

    game_lines: list[str] = []
    for game in games.values():
        game_lines.append(
            "(" + ", ".join(
                [
                    sql_literal(game.name),
                    sql_literal(game.slug),
                    sql_literal(game.description),
                    sql_literal(game.release_date),
                    sql_literal(game.developer),
                    sql_literal(game.publisher),
                    sql_literal(game.cover_url),
                    f"{game.average_rating:.2f}",
                    f"{game.default_rating:.2f}",
                ]
            ) + ")"
        )

    game_genre_lines: list[str] = []
    for game in games.values():
        for genre in sorted(game.genres):
            game_genre_lines.append(f"({sql_literal(game.slug)}, {sql_literal(genre)})")

    game_platform_lines: list[str] = []
    for game in games.values():
        for platform in sorted(game.platforms):
            game_platform_lines.append(f"({sql_literal(game.slug)}, {sql_literal(platform)})")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="\n") as out:
        out.write("-- Auto-generated by scripts/generate_games_seed_sql.py\n")
        out.write("-- Idempotent seed for games from CSV\n\n")

        if genres:
            out.write("INSERT INTO genre (name) VALUES\n")
            out.write(",\n".join(f"({sql_literal(name)})" for name in genres))
            out.write("\nON CONFLICT (name) DO NOTHING;\n\n")

        if game_lines:
            for chunk in chunked(game_lines, 1000):
                out.write(
                    "INSERT INTO game (name, slug, description, release_date, developer, publisher, cover_url, average_rating, default_rating) VALUES\n"
                )
                out.write(",\n".join(chunk))
                out.write("\nON CONFLICT (slug) DO NOTHING;\n\n")

        if game_genre_lines:
            for chunk in chunked(game_genre_lines, 500):
                out.write("WITH data(slug, genre_name) AS (VALUES\n")
                out.write(",\n".join(chunk))
                out.write("\n)\n")
                out.write(
                    "INSERT INTO game_genre (game_id, genre_id)\n"
                    "SELECT g.id, ge.id\n"
                    "FROM data d\n"
                    "JOIN game g ON g.slug = d.slug\n"
                    "JOIN genre ge ON ge.name = d.genre_name\n"
                    "ON CONFLICT DO NOTHING;\n\n"
                )

        if game_platform_lines:
            for chunk in chunked(game_platform_lines, 500):
                out.write("WITH data(slug, platform) AS (VALUES\n")
                out.write(",\n".join(chunk))
                out.write("\n)\n")
                out.write(
                    "INSERT INTO game_platform (game_id, platform)\n"
                    "SELECT g.id, d.platform\n"
                    "FROM data d\n"
                    "JOIN game g ON g.slug = d.slug\n"
                    "ON CONFLICT (game_id, platform) DO NOTHING;\n\n"
                )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate idempotent SQL seed from CSV")
    parser.add_argument(
        "--csv",
        default="Video Games Data.csv",
        help="Path to source CSV file",
    )
    parser.add_argument(
        "--out",
        default="src/main/resources/liquibase/changelogs/GLBACK-001/005_seed_games_from_csv.sql",
        help="Output SQL file",
    )
    parser.add_argument(
        "--cover-prefix",
        default="",
        help="Optional base URL prefix for cover_url",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    csv_path = Path(args.csv)
    out_path = Path(args.out)

    if not csv_path.exists():
        raise FileNotFoundError(f"CSV not found: {csv_path}")

    games = aggregate_rows(csv_path, args.cover_prefix)
    write_sql(games, out_path)

    total_platform_links = sum(len(g.platforms) for g in games.values())
    total_genre_links = sum(len(g.genres) for g in games.values())
    print(
        f"Generated {out_path} with {len(games)} games, "
        f"{total_genre_links} game-genre links, {total_platform_links} game-platform links."
    )


if __name__ == "__main__":
    main()
