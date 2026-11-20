# -*- coding: utf-8 -*-
import argparse
import json
import re
from pathlib import Path


LANGUAGE_CODES = [
    "zh-Hans",
    "zh-Hant-HK",
    "zh-Hant-TW",
    "en",
    "fr",
    "es",
    "ja",
    "ko",
    "ru",
    "ar",
]
LANGUAGE_CODE_DEFAULT = "zh-Hans"
ANDROID_CHANGELOG_ALIASES = {
    "zh-Hans": ["zh", "zh-Hans"],
    "zh-Hant-HK": ["zh-rHK", "zh-Hant-HK"],
    "zh-Hant-TW": ["zh-rTW", "zh-Hant-TW"],
}
# Plugin-center instruction (D:/idea-projects/AUTOJS6_PLUGIN_INSTRUCTION_AGENTS.md): one raw resource per language,
# and the unqualified raw directory carries the English copy for every other locale.
ANDROID_INSTRUCTION_DIRECTORIES = {
    "zh-Hans": "raw-zh",
    "zh-Hant-HK": "raw-zh-rHK",
    "zh-Hant-TW": "raw-zh-rTW",
    "en": "raw-en",
    "fr": "raw-fr",
    "es": "raw-es",
    "ja": "raw-ja",
    "ko": "raw-ko",
    "ru": "raw-ru",
    "ar": "raw-ar",
}
INSTRUCTION_DEFAULT_LANGUAGE = "en"
INSTRUCTION_FILE_NAME = "plugin_instruction.md"

# Plugin-center instruction copy lives in its own keys and never borrows README fields.
INSTRUCTION_KEY_PREFIX = "instruction_"
INSTRUCTION_LIST_KEYS = [
    "instruction_start_steps",
    "instruction_features",
    "instruction_script_notes",
    "instruction_requirements",
]
INSTRUCTION_FAQ_KEY = "instruction_faq"
INSTRUCTION_REQUIRED_KEYS = [
    "instruction_p_what",
    "instruction_h3_start",
    "instruction_start_steps",
    "instruction_h3_requirements",
    "instruction_requirements",
]
CHANGELOG_CATEGORIES = ["hint", "feature", "fix", "improvement", "dependency"]


ROOT = Path(__file__).resolve().parents[1]
README_DIR = ROOT / ".readme"
CHANGELOG_DIR = ROOT / ".changelog"
ANDROID_CHANGELOG_DIR = ROOT / "app" / "src" / "main" / "assets" / "doc"
ANDROID_RES_DIR = ROOT / "app" / "src" / "main" / "res"


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def render_template(text: str, values: dict) -> str:
    def replace(match):
        key = match.group(1).strip()
        if key not in values:
            raise KeyError(f"Missing template value: {key}")
        return str(values[key])

    return re.sub(r"\{\{\s*([A-Za-z0-9_$.-]+)\s*\}\}", replace, text)


def render_dynamic(value, values: dict):
    if isinstance(value, dict):
        return {key: render_dynamic(item, values) for key, item in value.items()}
    if isinstance(value, list):
        return [render_dynamic(item, values) for item in value]
    if isinstance(value, str):
        return render_template(value, values)
    return value


def bullet_list(items):
    return "\n".join(f"- {item}" for item in items)


def numbered_list(items):
    return "\n".join(f"{index}. {item}" for index, item in enumerate(items, start=1))


def markdown_link(label, url):
    return f"[{label}]({url})"


def validate_instruction_language(code, content):
    for key in INSTRUCTION_REQUIRED_KEYS:
        if key not in content:
            raise ValueError(f"Instruction language {code!r} is missing {key!r}")
    for key, value in content.items():
        if not key.startswith(INSTRUCTION_KEY_PREFIX):
            continue
        if key in INSTRUCTION_LIST_KEYS:
            if not (isinstance(value, list) and value and all(isinstance(item, str) and item for item in value)):
                raise ValueError(f"Instruction {code!r} field {key!r} must be a non-empty list of non-empty strings")
            texts = value
        elif key == INSTRUCTION_FAQ_KEY:
            if not (isinstance(value, list) and value):
                raise ValueError(f"Instruction {code!r} field {key!r} must be a non-empty list")
            for index, item in enumerate(value):
                if not (isinstance(item, dict) and set(item) == {"q", "a"} and all(isinstance(text, str) and text for text in item.values())):
                    raise ValueError(f"Instruction {code!r} {key}[{index}] must be an object with non-empty 'q' and 'a'")
            texts = [text for item in value for text in item.values()]
        else:
            if not (isinstance(value, str) and value):
                raise ValueError(f"Instruction {code!r} field {key!r} must be non-empty text")
            texts = [value]
        # UI names are quoted with ASCII double quotes; an odd count means a quote was swallowed on the way in.
        for text in texts:
            if text.count('"') % 2:
                raise ValueError(f"Instruction {code!r} field {key!r} has an unpaired double quote: {text[:60]!r}")


def validate_changelog_language(code, raw_changelog):
    for version_name, item in raw_changelog["$data"].items():
        if not re.fullmatch(r"v\d+\.\d+\.\d+", version_name):
            raise ValueError(f"Changelog {code!r} has an invalid version {version_name!r}")
        if not re.fullmatch(r"\d{4}/\d{2}/\d{2}", item.get("released_date", "")):
            raise ValueError(f"Changelog {code!r} {version_name} needs a released_date of the form yyyy/mm/dd")
        unknown = set(item) - {"released_date", *CHANGELOG_CATEGORIES}
        if unknown:
            raise ValueError(f"Changelog {code!r} {version_name} has unknown categories {sorted(unknown)}")


def load_languages():
    common = load_json(README_DIR / "common.json")
    languages = {}
    changelogs = {}
    instruction_keys = None
    for code in LANGUAGE_CODES:
        raw_language = load_json(README_DIR / f"lang_{code}.json")
        validate_instruction_language(code, raw_language)
        keys = {key for key in raw_language if key.startswith(INSTRUCTION_KEY_PREFIX)}
        if instruction_keys is None:
            instruction_keys = keys
        elif keys != instruction_keys:
            raise ValueError(f"Instruction keys of {code!r} differ: {sorted(keys ^ instruction_keys)}")
        merged_language = {**common, **raw_language}
        languages[code] = render_dynamic(merged_language, merged_language)

        raw_changelog = load_json(CHANGELOG_DIR / f"lang_{code}.json")
        validate_changelog_language(code, raw_changelog)
        changelog_values = {key: value for key, value in raw_changelog.items() if key != "$data"}
        changelog_values = render_dynamic(changelog_values, changelog_values)
        changelogs[code] = {
            "values": changelog_values,
            "data": render_dynamic(raw_changelog["$data"], changelog_values),
        }
    return languages, changelogs


def format_changelog_items(changelog, limit=None, heading_level=1):
    """The bundled CHANGELOG uses the standard format the release history page reads (heading_level 1:
    `# vX.Y.Z`, `###### yyyy/mm/dd` and `` * `tag` text `` bullets, AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md
    section 3); the README excerpt (heading_level 4) keeps an italic date and dash bullets."""
    values = changelog["values"]
    standard = heading_level <= 2
    chunks = []
    heading = "#" * heading_level
    for index, (version_name, item) in enumerate(changelog["data"].items()):
        if limit is not None and index >= limit:
            break
        date = item["released_date"]
        lines = [
            f"{heading} {version_name}",
            "",
            f"###### {date}" if standard else f"_{date}_",
            "",
        ]
        bullet = "*" if standard else "-"
        for category in CHANGELOG_CATEGORIES:
            for text in item.get(category, []):
                lines.append(f"{bullet} `{values[f'changelog_label_{category}']}` {text}")
        chunks.append("\n".join(lines).rstrip())
    return "\n\n".join(chunks).rstrip() + "\n"


def instruction_faq(items):
    return "\n\n".join(f"**{item['q']}**\n\n{item['a']}" for item in items)


def build_instruction_values(code, languages):
    """Only instruction_* keys reach the instruction template, so it cannot borrow README copy."""
    values = {key: value for key, value in languages[code].items() if key.startswith(INSTRUCTION_KEY_PREFIX)}
    for key in INSTRUCTION_LIST_KEYS:
        if key in values:
            render = numbered_list if key == "instruction_start_steps" else bullet_list
            values[f"placeholder_{key}"] = render(values[key])
    if INSTRUCTION_FAQ_KEY in values:
        values[f"placeholder_{INSTRUCTION_FAQ_KEY}"] = instruction_faq(values[INSTRUCTION_FAQ_KEY])
    return values


def build_language_list(target_code, languages):
    repo_url = languages[target_code]["repo_url"]
    lines = []
    for code in LANGUAGE_CODES:
        content = languages[code]
        label = f"{content['$name']} [{code}]"
        if code == target_code:
            lines.append(f"- {label} # {content['text_current_lowercase']}")
        else:
            lines.append(f"- {markdown_link(label, f'{repo_url}/blob/master/.readme/README-{code}.md')}")
    return "\n".join(lines)


def build_readme_values(code, languages, changelogs):
    content = dict(languages[code])
    repo_url = content["repo_url"]
    content["placeholder_ul_languages_all_supported"] = build_language_list(code, languages)
    content["placeholder_features"] = bullet_list(content["features"])
    content["placeholder_usage_steps"] = numbered_list(content["usage_steps"])
    content["placeholder_latest_release_history"] = format_changelog_items(
        changelogs[code],
        limit=3,
        heading_level=4,
    ).rstrip()
    content["placeholder_read_more_in_changelog_md"] = markdown_link(
        f"CHANGELOG-{code}.md",
        f"{repo_url}/blob/master/app/src/main/assets/doc/CHANGELOG-{code}.md",
    )
    return content


CHECK_ONLY = False
DRIFT = []


def write_text(path: Path, text: str):
    if CHECK_ONLY:
        if not path.is_file() or path.read_text(encoding="utf-8").replace("\r\n", "\n") != text:
            DRIFT.append(str(path.relative_to(ROOT)))
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")
    print(f"Generated {path.relative_to(ROOT)}")


def generate_readmes(languages, changelogs):
    template = (README_DIR / "template_readme.md").read_text(encoding="utf-8")
    for code in LANGUAGE_CODES:
        output = render_template(template, build_readme_values(code, languages, changelogs))
        write_text(README_DIR / f"README-{code}.md", output)
        if code == LANGUAGE_CODE_DEFAULT:
            write_text(ROOT / "README.md", output)


def generate_changelogs(languages, changelogs):
    template = (CHANGELOG_DIR / "template_changelog.md").read_text(encoding="utf-8")
    for code in LANGUAGE_CODES:
        values = dict(languages[code])
        values["placeholder_release_history"] = format_changelog_items(
            changelogs[code],
            heading_level=1,
        ).rstrip()
        output = render_template(template, values)
        for name in ANDROID_CHANGELOG_ALIASES.get(code, [code]):
            write_text(ANDROID_CHANGELOG_DIR / f"CHANGELOG-{name}.md", output)
        if code == LANGUAGE_CODE_DEFAULT:
            write_text(ANDROID_CHANGELOG_DIR / "CHANGELOG.md", output)


def generate_instructions(languages):
    template = (README_DIR / "template_plugin_instruction.md").read_text(encoding="utf-8")
    for code in LANGUAGE_CODES:
        output = render_template(template, build_instruction_values(code, languages)).rstrip() + "\n"
        write_text(ANDROID_RES_DIR / ANDROID_INSTRUCTION_DIRECTORIES[code] / INSTRUCTION_FILE_NAME, output)
        if code == INSTRUCTION_DEFAULT_LANGUAGE:
            write_text(ANDROID_RES_DIR / "raw" / INSTRUCTION_FILE_NAME, output)


def main():
    global CHECK_ONLY
    parser = argparse.ArgumentParser(description="Generate localized documentation or verify it without writing")
    parser.add_argument("--check", action="store_true", help="Check generated files without writing")
    CHECK_ONLY = parser.parse_args().check
    if LANGUAGE_CODE_DEFAULT not in LANGUAGE_CODES:
        raise ValueError(f"Default language code {LANGUAGE_CODE_DEFAULT!r} is not supported")
    languages, changelogs = load_languages()
    generate_changelogs(languages, changelogs)
    generate_readmes(languages, changelogs)
    generate_instructions(languages)
    if DRIFT:
        raise SystemExit("Generated Markdown differs: " + ", ".join(DRIFT))
    if CHECK_ONLY:
        print("Generated Markdown is current")


if __name__ == "__main__":
    main()
