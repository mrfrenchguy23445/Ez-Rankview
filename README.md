# Ez-RankView

[![Build](https://github.com/mrfrenchguy23445/Ez-Rankview/actions/workflows/build.yml/badge.svg)](https://github.com/mrfrenchguy23445/Ez-Rankview/actions/workflows/build.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-1.12%20%2B-62b47a?style=flat-square)
![Server](https://img.shields.io/badge/Server-Bukkit%20%7C%20Spigot%20%7C%20Paper%20%7C%20Purpur%20%7C%20Folia-blue?style=flat-square)
![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![LuckPerms](https://img.shields.io/badge/Requires-LuckPerms-4c8bf5?style=flat-square)

A LuckPerms addon which makes switching ranks easier than ever for admins testing out
permissions, supporting **Bukkit, Spigot, Paper, Purpur and Folia** from **1.12 through
the latest versions**. It lets staff preview a rank without permanently changing anyone's
permissions — perfect for checking how a new rank will look, or for switching a player
between ranks to compare them.

A preview:

- copies the target group's **resolved prefix / suffix** onto the player so the chat tag is
  actually visible, and
- grants the group's **permissions** while the preview is active.

It is **purely additive** — the player's existing groups and nodes are never touched. Every
node that is added is removed again when the preview stops. Temporary nodes also carry an
expiry (configurable) so a crashed server cleans up after itself.

## Table of contents

- [Requirements](#requirements)
- [Building](#building)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration-configyml)
- [How it works](#how-it-works)
- [Project layout](#project-layout)
- [Contributing](#contributing)
- [License](#license)

## Requirements

| Thing     | Version                         |
|-----------|---------------------------------|
| Server    | Bukkit, Spigot, Paper, Purpur, Folia (1.12 – 1.21.x / modern) |
| Java      | 21+ (for building; runs on Java 8+ depending on server)        |
| LuckPerms | installed and running (any 5.x)                               |

## Building

A Maven wrapper is included, so you don't need Maven installed:

```bash
./mvnw clean package
```

The compiled plugin lands in `target/Ez-RankView-1.0.0.jar`.

## Installation

1. Build the plugin (or grab a release jar).
2. Drop `Ez-RankView-<version>.jar` into your server's `plugins/` folder.
3. Restart the server. LuckPerms must be installed.
4. Run `/rankview list` in-game to confirm the groups are detected.

## Commands

Base command: `/rankview` (aliases: `/rv`, `/rankpreview`).

| Command                      | What it does                                                |
|------------------------------|-------------------------------------------------------------|
| `/rankview <group>`          | Preview `<group>` on yourself. Run it again to stop.        |
| `/rankview <group> <player>` | Preview `<group>` for another player.                       |
| `/rankview stop [player]`    | Stop previewing (yourself, or the given player).            |
| `/rankview list`             | List LuckPerms groups and any active previews.              |
| `/rankview reload`           | Reload `config.yml`.                                        |

Previewing a different group while one is active **switches** to the new group automatically.

## Permissions

| Permission          | Default | Allows                                      |
|---------------------|---------|---------------------------------------------|
| `ezrankview.use`    | op      | Preview a rank on yourself, list groups.    |
| `ezrankview.others` | op      | Apply / clear a preview for another player. |
| `ezrankview.reload` | op      | Reload the config.                          |
| `ezrankview.admin`  | op      | Grants all of the above.                    |

## Configuration (`config.yml`)

```yaml
preview:
  apply-chat-meta: true        # copy the group's prefix/suffix so the tag shows
  priority-offset: 1000        # priority bump so the preview outranks your real prefix
  expire-after-seconds: 3600   # crash-safety expiry for the temp nodes (0 = until stopped)
  restore-on-quit: true        # clear a preview when the player logs out
```

All messages are editable under `messages:` and support `&` colour codes plus these
placeholders: `%group%`, `%player%`, `%prefix%`, `%error%`.

## How it works

Under the hood it uses the [LuckPerms API](https://luckperms.net/wiki/Developer-API).
When a preview starts it:

1. reads the resolved prefix/suffix of the target group from its cached metadata,
2. adds matching `prefix`/`suffix` nodes to the player at a higher priority,
3. adds a temporary `group.<name>` inheritance node so the player gets the previewed rank's permissions, and
4. remembers the exact nodes it added.

Stopping the preview removes exactly those nodes. Nothing else about the player's data is
changed, and `restore-on-quit` plus the node expiry make leftover previews effectively
impossible.

### Notes

- A preview is additive, so a player keeps their real permissions while previewing (the
  previewed rank's permissions are layered on top). This keeps it safe to use on live players.
- The plugin only understands what LuckPerms itself resolves. If your chat/tab/nametag
  formatting is handled by another plugin, that plugin will show the preview as long as it
  reads LuckPerms prefixes (most do).

## Project layout

```
pom.xml                     Maven build (Java 21, Paper + LuckPerms APIs)
mvnw / mvnw.cmd             Maven wrapper
src/main/resources/
  plugin.yml                Plugin metadata, command, permissions
  config.yml                Default configuration
src/main/java/com/ezrankview/
  EzRankView.java           Plugin entry point
  command/RankViewCommand   Command handling + tab completion
  preview/PreviewManager    Applies / restores previews
  preview/PreviewSession    One active preview and its tracked nodes
  luckperms/LuckPermsService  Thin LuckPerms API wrapper
  config/PluginConfig       Typed config + message helper
  listener/PreviewListener  Clears previews on logout
```

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for build instructions
and guidelines, and [CHANGELOG.md](CHANGELOG.md) for the release history.

## License

Released under the [MIT License](LICENSE).
