#!/usr/bin/env python3
import sys
import json
import urllib.request
from datetime import datetime, timezone

if len(sys.argv) < 4:
    print("Usage: ./.github/scripts/discord.py <WEBHOOK_URL> <MODRINTH_ID> <VERSION_NUMBER>")
    sys.exit(1)

webhook_url = sys.argv[1]
modrinth_id = sys.argv[2]
version_number = sys.argv[3]

# Make sure repo contains secret
if not webhook_url or not webhook_url.strip():
    print("Missing DISCORD_WEBHOOK_URL secret")
    sys.exit(1)

def fetch_json(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Playground-Discord-Embed/1.0 (Github-Actions)'})
    with urllib.request.urlopen(req) as res:
        return json.loads(res.read().decode('utf-8'))

try:
    # Gets project details from Modrinth.
    project_data = fetch_json(f"https://api.modrinth.com/v2/project/{modrinth_id}")
    mod_name = project_data.get("title", "Unknown Mod")
    icon_url = project_data.get("icon_url")
    slug = project_data.get("slug")

    # Gets all versions for the project.
    versions = fetch_json(f"https://api.modrinth.com/v2/project/{modrinth_id}/version")
    
    # Gets versions matching tag.
    matching_versions = [v for v in versions if v["version_number"] == version_number]

    if not matching_versions:
        print(f"Error: Version {version_number} not found for project {modrinth_id}")
        sys.exit(1)

    main_version = matching_versions[0]
    changelog = main_version.get("changelog", "")
    release_type = main_version.get("version_type", "release").lower()
    
    if len(matching_versions) == 1:
        v = matching_versions[0]
        game_versions = v.get("game_versions", [])
        loaders = v.get("loaders", [])
        loader_str = ", ".join(game_versions) + " (" + ", ".join(l.capitalize() for l in loaders) + ")"
        download_str = f"[**Modrinth**](https://modrinth.com/mod/{slug}/version/{v['id']})"
    else:
        loader_lines = []
        download_lines = []
        for v in matching_versions:
            game_versions = v.get("game_versions", [])
            loaders = v.get("loaders", [])
            mod_loaders_str = ", ".join(l.capitalize() for l in loaders)
            loader_lines.append(", ".join(game_versions) + " (" + mod_loaders_str + ")")
            download_lines.append(f"[**{mod_loaders_str} (Modrinth)**](https://modrinth.com/mod/{slug}/version/{v['id']})")
        
        loader_str = "\n".join(loader_lines)
        download_str = "\n".join(download_lines)

except Exception as e:
    print(f"Error fetching data from Modrinth: {e}")
    sys.exit(1)

colors = {
    "release": 3066993,
    "beta": 16753920,
    "alpha": 15548997,
}
color = colors.get(release_type, 3447003)
changelog_truncated = changelog[:2000] + ("..." if len(changelog) > 2000 else "")

payload = {
    "username": f"Modrinth Release",
    "avatar_url": "https://media.beehiiv.com/cdn-cgi/image/fit=scale-down,format=auto,onerror=redirect,quality=80/uploads/publication/logo/a49f8e1b-3835-4ea1-a85b-118c6425ebc3/Modrinth_Dark_Logo.png",
    "embeds": [{
        "title": "",
        "description": f"## {mod_name} {version_number}\n{changelog_truncated}",
        "color": color,
        "thumbnail": { "url": icon_url } if icon_url else None,
        "fields": [
            { "name": "Version", "value": version_number, "inline": True },
            { "name": "Loader", "value": loader_str, "inline": True },
            { "name": "Download", "value": download_str, "inline": True }
        ],
        "footer": { "text": "Modrinth Release" },
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    }]
}

headers = {
    'Content-Type': 'application/json',
    'User-Agent': 'Playground/Discord-Embed/1.0 (Github-Actions)'
}
req = urllib.request.Request(webhook_url, data=json.dumps(payload).encode('utf-8'), headers=headers)

try:
    with urllib.request.urlopen(req) as res:
        print(f"Embed sent successfully: {res.status}")
except Exception as e:
    print(f"Failed to send embed: {e}")
    sys.exit(1)
