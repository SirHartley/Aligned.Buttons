# Aligned.Buttons

A Starsector mod that tidies the submarket buttons (Open Market, Black Market, Military Market, Storage and the ones other mods add) above the cargo and fleet screens.

In vanilla the buttons have a fixed width, so a row ends short of the panel edge, and the storage button is pinned to the bottom right corner where it lines up with nothing. With this mod the buttons stretch so each row fills the panel exactly, and the storage button sits in the last column of the last row.

## Requirements

- Starsector `0.98a`
- [PatchLib](https://github.com/Lukas22041/PatchLib)

## Install

1. Download `Aligned.Buttons-<version>.zip` from the [latest release](https://github.com/SirHartley/Aligned.Buttons/releases/latest).
2. Extract it into `Starsector/mods/`.
3. Enable Aligned.Buttons and PatchLib in the launcher.

The mod saves nothing, so it can be added to or removed from an existing save. Version Checker picks up updates through `AlignedButtons.version`.

## Compatibility

Submarkets added by other mods are laid out like the vanilla ones. A mod that also moves these buttons will conflict with this one.

## Building

Build and release steps are in [docs/RELEASE.md](docs/RELEASE.md). Code layout and the vanilla behavior the patch relies on are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
