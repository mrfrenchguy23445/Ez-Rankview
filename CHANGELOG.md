# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-10-12

### Added
- Initial release.
- `/rankview <group> [player]` to preview a LuckPerms rank on yourself or another player.
- `/rankview stop [player]`, `/rankview list`, and `/rankview reload`.
- Automatic switching when previewing a different group while one is active.
- Additive previews: copies the group's resolved prefix/suffix and optionally its
  permissions via temporary nodes, then removes exactly those nodes on stop.
- Safety cleanup on logout, on plugin disable, and via a configurable node expiry.
- Fully configurable `config.yml` with a messages section.

[Unreleased]: https://github.com/mrfrenchguy23445/Ez-Rankview/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/mrfrenchguy23445/Ez-Rankview/releases/tag/v1.0.0
