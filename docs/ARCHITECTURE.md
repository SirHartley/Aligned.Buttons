# Architecture

Technical routing for the current implementation, checked against game `0.98a-RC8` and PatchLib `1.0.3`. Code paths below are relative to `jars/src/alignedbuttons/`; data paths are repository-relative. Search the named owner before adding another implementation.

| Reference | Scope |
|---|---|
| [CLAUDE.md](../CLAUDE.md) | Workflow, build gate and document upkeep |
| [README.md](../README.md) | Player overview, requirements, install, settings, compatibility |
| [RELEASE.md](RELEASE.md) | Version numbers, release builder, package contents and publishing |
| [CHANGELOG.md](CHANGELOG.md) | Change-note format and upkeep for every implementation task |

## Start here

| Change / symptom | Route |
|---|---|
| Buttons not moved at all | PatchLib log lines for `SubmarketButtonPatch` (below) -> [patch target](#patch-target) after a game update |
| Wrong column count, widths, gaps or strip height | `SubmarketButtonPatch.afterConstructor -> ButtonLayout.current -> SubmarketGrid.of -> slot / left / top / buttonWidth / height` |
| Storage button in the wrong place | `SubmarketButtonPatch.storageIndex -> SubmarketGrid.slot` |
| Names still shown with Icons only, or not cut short with Five per row | `SubmarketButtonPatch.relabel -> SubmarketGrid.labelWidth / fit`; [render panel contract](#vanilla-strip) |
| Layout setting has no effect | `data/config/LunaSettings.csv` `aligned_buttons_layout` -> `ButtonLayout.current` -> `ButtonLayout.byLabel` |
| Cargo or fleet panel overlaps the strip | [strip height contract](#vanilla-strip) |
| Stray 1 px line right of an icon only button while it glows | `ButtonHighlightPatch.skipNegativeFill`; [highlight patch target](#patch-target) |

## Registration and lifecycle

| Registry / hook | Owners |
|---|---|
| `mod_info.json` | No `modPlugin`; jar `jars/AlignedButtons.jar`; dependencies PatchLib, LunaLib; `utility` because nothing is saved |
| PatchLib discovery | PatchLib scans `jars/AlignedButtons.jar` for `@Patch` classes and installs `submarkets/SubmarketButtonPatch` when the strip class loads and `submarkets/ButtonHighlightPatch` when the button render panel class loads; nothing is registered by hand |
| `data/config/LunaSettings.csv` | The layout setting and its help text |
| `data/config/version/version_files.csv` | Version Checker entry for `AlignedButtons.version` |

There is no load order: the layout patch runs each time the game builds a submarket strip, which happens when the cargo or fleet screen opens at a market.

### Settings

`submarkets/ButtonLayout` owns the mod id, the setting id `aligned_buttons_layout` and one constant per radio option; each constant's `label` is the option text in `data/config/LunaSettings.csv` and the value LunaLib saves. `data/config/LunaSettings.csv` owns the help text; its default and option order mirror `ButtonLayout`. `ButtonLayout.current` reads LunaLib once per strip build, which only happens when a screen opens, so there is no listener or cache; a missing or unknown value falls back to `ButtonLayout.DEFAULT` (`FIVE_PER_ROW`), which mirrors the CSV `defaultValue`. LunaLib writes the default into `saves/common/LunaSettings/aligned_buttons.json` on first launch and keeps it there, so changing the default only reaches installs that never stored the setting. A changed setting applies the next time a screen opens.

### Build and checks

`tools/ReleaseBuild.java` (`build`, `package`) and `tools/ReleaseBuildChecks.java` compile against `lib/` and never ship in the release jar; they, `lib/` and `.github/workflows/release.yml` are owned by [RELEASE.md](RELEASE.md). `tools/LayoutChecks.java` runs headless from the repository root against the classes `build` leaves in `out/release-build-*/classes`:

```text
java -cp "out/release-build-<n>/classes:lib/*" alignedbuttons.tools.LayoutChecks
```

It checks `SubmarketGrid` for every `ButtonLayout`, strip widths from 450 to 1900 px and 1 to 12 buttons, with and without storage: every button gets its own slot inside the strip, the last column ends on the strip edge, and storage sits in the last column of the last row. Stretched layouts keep 5 px gaps with widths that differ by at most 1 px. Per layout: `STRETCHED` matches the vanilla column and row counts and is never narrower than vanilla; `ICONS` fits as many 72 px tiles as the row allows, keeps the icon exactly centered (4 px each side), and spreads the spare room over gaps of at least 5 px that differ by at most 1 px; `FIVE_PER_ROW` always has five columns. It also checks `SubmarketGrid.fit` with a fixed width per character, and that `LunaSettings.csv` and `ButtonLayout` agree (header, nine columns, `Radio` type, `DEFAULT` is the CSV default and named in the help text, option order, fallback).

## Source owners

Use `rg --files jars/src/alignedbuttons` for the complete inventory.

### `submarkets`

| File | Owner / connection |
|---|---|
| `submarkets/SubmarketButtonPatch.java` | The layout patch. `@After` on the strip constructor: collects the buttons, finds storage, places every button through `SubmarketGrid`, sets the strip height, and for `ICONS` and `FIVE_PER_ROW` rewrites each name label |
| `submarkets/SubmarketGrid.java` | All layout math: column count per `ButtonLayout`, slot per button (storage last column of the last row), column edges (stretched tiles, or for `ICONS` fixed 72 px tiles with the spare room in the gaps), row tops, strip height, the room left for a name and `fit`, which cuts each name line to that room with `...`. No game types, so `LayoutChecks` can run it |
| `submarkets/ButtonLayout.java` | The layout setting: ids, option labels, `current()` from LunaLib |
| `submarkets/ButtonHighlightPatch.java` | Redirect in the button render panel's `renderImpl`: skips a rectangle fill with a negative width, which only the highlight strip of a 72 px icon tile has |

## Contracts

### Vanilla strip

Evidence: decompiled `0.98a-RC8` from the `starsector-knowledge` skill. Obfuscated names are this build's and change between builds and platforms.

- The submarket strip is one class in `com.fs.starfarer.campaign.ui` (`Z` on `0.98a-RC8`) that extends the game's base panel (so it implements `UIPanelAPI`) and declares `showSubmarketTextDialog`. Its only constructor takes `(SectorEntityToken, MarketAPI, float width, float height, boolean, callback)` and builds every button in a private method it calls last.
- Both screens build it: the cargo screen with height 48, the fleet screen with width `getWidth() - 20` and height 48. Both read the strip's height right after the constructor returns to lay out the panels below it, so a height set inside the `@After` hook is the height they use.
- Vanilla buttons are `(int)(height * 640 / 400) - 2 + 120` = 194 px wide with a 5 px gap; rows are `height + 2` apart. A row wraps once `x + width + gap` passes the strip width, so a row holds `floor(width / (194 + 5))` buttons. Storage is moved to the end of the list and placed with `inBR(0, 0)`, so it hugs the strip's right edge instead of the grid.
- The strip keeps its buttons in display order in its only `List` field. Each button implements `ButtonAPI` and carries its `Submarket` (a `SubmarketAPI`) as custom data.
- Each button draws through a render panel whose position the button copies on every update, so a later `setSize` on the button reaches the drawn frame. The frame (background, border, highlight strip) uses the current width every frame; the icon and name sit at fixed offsets on the left.
- The render panel draws the icon at `inTL(4, 4)`, `(height - 8) * 640 / 400` by `height - 8` (64 by 40 px), and the name 10 px right of it in `orbitron12condensed`. The icon does not move with the width, so `ICONS` keeps its tiles at exactly 72 px (icon 4 px from both edges) instead of stretching them. The highlight strip behind the name is drawn while the button glows with `O.Ó00000(x + 4 + iconWidth + 1, y + 4, width - 8 - iconWidth - 1, height - 8, color, alpha)` (`com.fs.starfarer.renderers.O` on `0.98a-RC8`), a GL quad with no width check, so at 72 px its width is -1 and it would still light the pixel column left of `x + 69`. `ButtonAPI.setText` does nothing for these buttons (it only handles a different renderer type).
- The render panel builds its icon and name label once, on its first size change, which normally comes with the first frame. `relabel` sets the panel's size itself so the label exists inside the hook, then reaches it through the button's readable `getButtonPanel()` and the panel's only `LabelAPI` field. Vanilla names carry a line break (`Open\nMarket` in `data/campaign/submarkets.csv`), so `fit` cuts each line on its own.
- Resizing the strip does not rebuild it: neither the strip class nor its base panel overrides `sizeChanged`.

### Patch target

`SubmarketButtonPatch`:

- Class: `@ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui", methodMatches = @MethodMatch(methodName = "showSubmarketTextDialog"))`. `UIPanelAPI` and the package are stable or near stable; `showSubmarketTextDialog` is an inferred readable name. No obfuscated name appears in the code.
- Method: `@After` on the constructor (`methodType = CONSTRUCTOR`, `parameterCount = 6`). Vanilla's layout runs first; the hook then overwrites button positions, the strip height and, for `ICONS` and `FIVE_PER_ROW`, the name text. No vanilla code is skipped.
- Buttons are read with `context.getField(FieldQuery.create().subtype(List.class).build())`, which finds the strip's own list before any superclass field. The name label is read with `getMethod` for `getButtonPanel` (no parameters) on the button and `getField` for the `LabelAPI` field on the panel, both behind `hasMethod` / `hasField`, so a missing member leaves the name as vanilla shows it.
- After a game update, check in order: the package, the `showSubmarketTextDialog` name, the constructor arity, that the button list is still the strip's only `List` field, `getButtonPanel`, the render panel's single `LabelAPI` field and when it creates it, and the gap, border, icon and label constants in `SubmarketGrid`.
- PatchLib logs `Discovered patch ... SubmarketButtonPatch` at startup and `Installed a hook patch site at com.fs.starfarer.campaign.ui.<class> on method <init>` the first time a strip is built. `TogglePatch alignedbuttons.submarkets.SubmarketButtonPatch` (Console Commands) switches it off for comparison.

`ButtonHighlightPatch`:

- Class: `@ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui", methodMatches = @MethodMatch(methodType = CONSTRUCTOR, parameterTypes = {String, String, Color, Color, Color}))`. On `0.98a-RC8` only the button render panel (`ooOO`) in that package has this constructor; the other button panels there take `(X, Color, String, Color, Color)`. Inferred by shape.
- Method: `@RedirectMethodCall` in `renderImpl` (one parameter) on every call shaped `(float, float, float, float, Color, float)` returning `void`. In `renderImpl` that is the highlight strip fill and the disabled overlay fill (full width). The handler calls down unless the width argument is negative, so vanilla widths and other mods' layers on the same call are untouched.
- The game classes are Java 17 class files, which PatchLib redirects need.
- After a game update, check the constructor shape, `renderImpl`, and that the strip fill still uses that call shape. If the redirect stops installing, icon only buttons show a 1 px line in their right gap while they glow; nothing else breaks.
- PatchLib logs `Installed a redirect patch site at com.fs.starfarer.campaign.ui.<class> on method renderImpl` the first time a submarket button draws. `TogglePatch alignedbuttons.submarkets.ButtonHighlightPatch` switches it off.

### Shared constraints

- `SubmarketGrid` is the only layout math; the patch only reads positions and applies what the grid returns.
- Another mod that also moves these buttons after construction wins or loses by PatchLib order; this patch uses the default order 0.
