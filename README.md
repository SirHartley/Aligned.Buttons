# Tidy.Panels

Small fixes for Starsector's UI panels. Currently aligns the market buttons on the cargo and fleet screens.

Choose between full labels or compact icons that expand on hover. The selected market gets a highlight in its faction's color.

## Requirements

- Starsector 0.98a
- [PatchLib](https://github.com/Lukas22041/PatchLib)
- LunaLib and its dependency, LazyLib

## Installation

1. Download the ZIP from the [latest release](https://github.com/SirHartley/Tidy.Panels/releases/latest).
2. Extract it into `Starsector/mods/`.
3. Enable Tidy.Panels and its dependencies in the launcher.

Safe to add or remove during a campaign. Supports Version Checker.

## Settings

Open LunaLib's settings to change these. Changes apply when you reopen the cargo or fleet screen.

| Setting | Options |
|---|---|
| Submarket button layout | **Unfold on hover** (default): icons expand to show their names. **Stretched**: keeps the vanilla button count per row. **Five per row**: fits five named buttons per row. |
| Selected button highlight | **Animated** (default): moving lights in the faction's color. **Bright band**: a fixed outline. **Off**: vanilla appearance. |
| Unfold time | Time to expand a button, from 0 to 1 second. Default: 0.1. Set to 0 for instant expansion. |
| Fix button corners | Fits button backgrounds to their frames. On by default. |

With **Unfold on hover**, the last hovered button stays expanded. Each row keeps its width as buttons expand and collapse. Long names are shortened to fit.

## Compatibility

Works with markets added by other mods. Conflicts with mods that also reposition these buttons.

## Development

See [build and release instructions](docs/RELEASE.md) and the [code reference](docs/ARCHITECTURE.md).
