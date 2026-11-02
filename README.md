# Aligned.Buttons

A Starsector mod that tidies the submarket buttons (Open Market, Black Market, Military Market, Storage and the ones other mods add) above the cargo and fleet screens.

In vanilla the buttons have a fixed width, so a row ends short of the panel edge, and the storage button is pinned to the bottom right corner where it lines up with nothing. With this mod the buttons line up in rows that end exactly on the panel edge, with the storage button at the right end of the last row. By default they are icon buttons that unfold under the cursor to show the submarket's name; the vanilla number of named buttons per row or five per row are the other layouts. The selected submarket's button gets an animated highlight in its faction's color, or a still outline, and the button backgrounds get the frame's cut corners.

## Requirements

- Starsector `0.98a`
- [PatchLib](https://github.com/Lukas22041/PatchLib)
- LunaLib (and LazyLib, which LunaLib needs)

## Install

1. Download `Aligned.Buttons-<version>.zip` from the [latest release](https://github.com/SirHartley/Aligned.Buttons/releases/latest).
2. Extract it into `Starsector/mods/`.
3. Enable Aligned.Buttons, PatchLib, LunaLib and LazyLib in the launcher.

## Settings

All four are LunaLib settings, in this order:

**Submarket button layout**

| Option | Buttons |
|---|---|
| Stretched | The vanilla number of buttons per row, widened to fill the row |
| Five per row | Five buttons with names in each row; names too long for the button are cut short with `...` |
| Unfold on hover (default) | Icon buttons with one opened to show its name. The button under the cursor opens over the **Unfold time** while the open one folds, so the row keeps its width; sweeping the cursor across makes several buttons open part way and fold back. The last hovered button stays open after the cursor leaves; the selected submarket is the open one when the screen opens |

The storage button always sits at the right end of the last row.

**Selected button highlight**

| Option | Selected submarket button |
|---|---|
| Off | As in vanilla |
| Bright band | A 3 px outline: black, the faction's color at full saturation (a gray faction keeps its gray), then white shifted slightly toward that color |
| Animated (default) | A 2 px black band inside the vanilla frame's outer px (the inner px at half alpha) with four highlights in the faction's color running clockwise around it, one lap every 4.4 s |

**Unfold time**: seconds Unfold on hover takes to open a button fully, 0 to 1, default 0.1. 0 opens it at once.

**Fix button corners** (on by default): the dark background of the submarket buttons gets the same cut corners as their frame instead of the vanilla square corners that show past it.

A change to any setting applies the next time the screen opens.

The mod saves nothing, so it can be added to or removed from an existing save. Version Checker picks up updates through `AlignedButtons.version`.

## Compatibility

Submarkets added by other mods are laid out like the vanilla ones. A mod that also moves these buttons will conflict with this one.

## Building

Build and release steps are in [docs/RELEASE.md](docs/RELEASE.md). Code layout and the vanilla behavior the patch relies on are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
