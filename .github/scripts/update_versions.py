#!/usr/bin/env python3
import json
import sys


def main():
    if len(sys.argv) < 6:
        sys.exit("usage: update_versions.py <versions.json> <body_file> <mod_version> <release_url> <mc_key...>")
    path, body_file, mod_version, release_url = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
    mc_keys = sys.argv[5:]

    with open(body_file, encoding="utf-8-sig") as f:
        changes = " ".join(f.read().split())

    with open(path, encoding="utf-8-sig") as f:
        data = json.load(f)

    for key in mc_keys:
        data.setdefault(key, {})
        data[key][mod_version] = changes

    promos = data.setdefault("promos", {})
    for key in mc_keys:
        promos[key + "-latest"] = release_url

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print("Updated versions.json for", ", ".join(mc_keys), "->", mod_version)


if __name__ == "__main__":
    main()
