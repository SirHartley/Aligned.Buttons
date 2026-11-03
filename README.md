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
| Submarket button layout | **Tiles** (default): icon tiles; Tile unfold sets how they show names. **Stretched**: keeps the vanilla button count per row. **Five per row**: fits five named buttons per row. |
| Tile unfold | Tiles only. **On hover** (default): the tile under the cursor expands to show its name. **On click**: only the selected market's tile expands, when clicked. **Never**: icons only, names show in the tooltip. |
| Unfold time | Time to expand a tile with On hover or On click, from 0 to 1 second. Default: 0.1. Set to 0 for instant expansion. |
| Selected button highlight | **Animated** (default): moving lights in the faction's color. **Bright band**: a fixed outline. **Off**: vanilla appearance. |
| Fix button corners | Fits button backgrounds to their frames. On by default. |

With **On hover**, the last hovered tile stays expanded. Each row keeps its width as tiles expand and collapse. Long names are shortened to fit.

## Compatibility

Works with markets added by other mods. Conflicts with mods that also reposition these buttons.

## License

MIT, see [LICENSE](LICENSE).

## Development

See [build and release instructions](docs/RELEASE.md) and the [code reference](docs/ARCHITECTURE.md).
