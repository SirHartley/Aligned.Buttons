# Aligned.Buttons

A Starsector mod that tidies the submarket buttons (Open Market, Black Market, Military Market, Storage and the ones other mods add) above the cargo and fleet screens.

In vanilla the buttons have a fixed width, so a row ends short of the panel edge, and the storage button is pinned to the bottom right corner where it lines up with nothing. With this mod the buttons form an even grid that fills each row exactly, five to a row by default, and the storage button sits in the last column of the last row. The selected submarket's button gets a 3 px outline: black, then its faction's color at full saturation (a gray faction keeps its gray), then white with a slight shift toward that color. A setting switches to the vanilla number of buttons per row, to icon only buttons, or to icon buttons that unfold under the cursor.

## Requirements

- Starsector `0.98a`
- [PatchLib](https://github.com/Lukas22041/PatchLib)
- LunaLib (and LazyLib, which LunaLib needs)

## Install

1. Download `Aligned.Buttons-<version>.zip` from the [latest release](https://github.com/SirHartley/Aligned.Buttons/releases/latest).
2. Extract it into `Starsector/mods/`.
3. Enable Aligned.Buttons, PatchLib, LunaLib and LazyLib in the launcher.

## Settings

**Submarket button layout** (LunaLib):

| Option | Buttons |
|---|---|
| Stretched | The vanilla number of buttons per row, widened to fill the row |
| Icons only | Icon and border only, as many per row as fit, spaced evenly across the row; the name still shows in the tooltip |
| Five per row (default) | Five buttons with names in each row; names too long for the button are cut short with `...` |
| Unfold on hover | Icon buttons with one opened to show its name. The button under the cursor opens over the **Unfold time** while the open one folds, so the row keeps its width; sweeping the cursor across makes several buttons open part way and fold back. The last hovered button stays open after the cursor leaves; the selected submarket is the open one when the screen opens |

The storage button always sits at the right end of the last row.

**Unfold time** (LunaLib): seconds Unfold on hover takes to open a button fully, 0 to 1, default 0.1. 0 opens it at once.

**Animated outline** (LunaLib, off by default): the selected button's outline turns into a 2 px black band inside the vanilla frame's outer px (the inner px at half alpha) and four highlights in the faction's color run clockwise along its middle px, one lap every 4.4 s.

A change to any setting applies the next time the screen opens.

The mod saves nothing, so it can be added to or removed from an existing save. Version Checker picks up updates through `AlignedButtons.version`.

## Compatibility

Submarkets added by other mods are laid out like the vanilla ones. A mod that also moves these buttons will conflict with this one.

## Building

Build and release steps are in [docs/RELEASE.md](docs/RELEASE.md). Code layout and the vanilla behavior the patch relies on are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
