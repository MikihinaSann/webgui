# Changelog

## 1.6.0 - 2026-07-25

### Added
- **Commands from the page.** A page can now run a Minecraft command, executed **as the player** — exactly as if they typed it in chat, so there is no privilege escalation. Commands are only accepted from the main frame of an origin the server declared trusted via `trustedCommandOrigins` in `config/webgui/server.json`; requests from any other origin (e.g. after a redirect or from an iframe) are dropped. The trusted-origin list is sent to the client on join and cleared on disconnect, so it never carries across servers.
- `@webgui/react`: `runCommand(command)` and the `useRunCommand()` hook.

### Added
- `window.webgui.client` now includes more player data: `health`, `maxHealth`, `food`, `xpLevel`, `gamemode`, and a `look` object with `yaw`/`pitch`.

### Fixed
- Server-opened HUDs and GUIs are now closed automatically when leaving a world (disconnect / exit to title), instead of lingering in the background on the main menu.
- Fixed NeoForge crash when loading a URL with a leading slash.
- Fixed NeoForge mixin error in `MouseMixin` (#7).
- Fixed crash on dedicated servers caused by loading client-only classes during payload registration (#9).
- Fixed MCEF failing to load on NeoForge 1.21.11 (NeoForge bumped to 21.11.44).

## 1.4.1
