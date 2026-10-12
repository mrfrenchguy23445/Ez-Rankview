# Contributing to Ez-RankView

Thanks for taking the time to contribute!

## Requirements

- JDK 21 or newer
- A LuckPerms + Paper/Purpur 1.21.x server for manual testing (optional but recommended)

## Building

The Maven wrapper is included, so no global Maven install is needed:

```bash
./mvnw clean package
```

The plugin jar is written to `target/Ez-RankView-<version>.jar`.

## Development setup

1. Fork and clone the repository.
2. Open the project in your IDE as a Maven project.
3. Make your changes.
4. Run `./mvnw clean package` to confirm it still compiles.
5. Test in-game on a server with LuckPerms installed.

## Guidelines

- Keep changes focused and consistent with the existing code style (4-space indents,
  constructor injection, small focused classes).
- Prefer the public LuckPerms API over reflection or reading LuckPerms storage directly.
- Never leave temporary nodes behind: every preview must be fully reversible, and any
  new node type must be added to the recorded session so it can be removed again.
- Update `CHANGELOG.md` under `[Unreleased]` for user-facing changes.
- Update `README.md` and `config.yml` when you add or change configuration.

## Pull requests

- Describe what the change does and why.
- Note any config or permission changes.
- Link related issues where applicable.

## Reporting bugs

Open an issue with:
- Your server software and version (e.g. Purpur 1.21.8)
- LuckPerms version
- Steps to reproduce
- Any relevant console errors
