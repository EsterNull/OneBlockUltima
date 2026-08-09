#!/usr/bin/env python3
import json
import sys


def normalize(text):
    return " ".join(text.split())


def main():
    if len(sys.argv) < 2:
        sys.exit("usage:\n"
                 "  update_versions.py <versions.json> <body_file> <mod_version> <release_url> <mc_key...>\n"
                 "  update_versions.py --print <body_file> <mod_version> <release_url> <mc_key...>")
    if sys.argv[1] == "--print":
        body_file, mod_version, release_url = sys.argv[2], sys.argv[3], sys.argv[4]
        mc_keys = sys.argv[5:]
        with open(body_file, encoding="utf-8-sig") as f:
            changes = normalize(f.read())
        print("=== versions.json entry (add to main manually) ===")
        print('add under each MC key (%s):' % ", ".join(mc_keys))
        print('  %s: %s' % (json.dumps(mod_version), json.dumps(changes)))
        print("promos entries:")
        for key in mc_keys:
            print('  "%s-latest": %s' % (key, json.dumps(release_url)))
        return

    if len(sys.argv) < 6:
        sys.exit("usage: update_versions.py <versions.json> <body_file> <mod_version> <release_url> <mc_key...>")
    path, body_file, mod_version, release_url = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
    mc_keys = sys.argv[5:]

    with open(body_file, encoding="utf-8-sig") as f:
        changes = normalize(f.read())

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
