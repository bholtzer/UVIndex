"""Validate local store copy without dependencies or network access."""
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parent
records = json.loads((root / "listings.json").read_text(encoding="utf-8"))
locales = [record["locale"] for record in records]
assert len(locales) == len(set(locales)) == 15, "Expected 15 unique locales"
model = root.parents[2] / "app/src/main/java/com/bihstudio/uvindex/domain/model/Models.kt"
language_enum = model.read_text(encoding="utf-8").split("enum class AppLanguage", 1)[1]
app_codes = set(re.findall(r'\w+\("([a-z]+)"', language_enum))
assert {locale.split("-")[0] for locale in locales} == app_codes

for record in records:
    folder = root / record["locale"]
    for field, filename, limit in [
        ("title", "title.txt", 30),
        ("short_description", "short-description.txt", 80),
        ("full_description", "full-description.txt", 4000),
    ]:
        value = record[field]
        assert value == (folder / filename).read_text(encoding="utf-8")
        assert 0 < len(value) <= limit, (record["locale"], field, len(value))
        assert value == value.strip() and "\ufffd" not in value
    captions = record["screenshot_captions"]
    assert len(captions) == 4 and all(captions)
    actual = (folder / "screenshot-captions.txt").read_text(encoding="utf-8").splitlines()
    assert actual == [f"{i}. {caption}" for i, caption in enumerate(captions, 1)]

print("PASS: 15 locales, 45 listing fields, 60 captions, file parity and app-language coverage.")
