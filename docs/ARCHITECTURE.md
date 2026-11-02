# Architecture

Technical routing for the current implementation, checked against game `0.98a-RC8` and PatchLib `1.0.3`. Code paths below are relative to `jars/src/alignedbuttons/`; data paths are repository-relative. Search the named owner before adding another implementation.

| Reference | Scope |
|---|---|
| [CLAUDE.md](../CLAUDE.md) | Workflow, build gate and document upkeep |
| [README.md](../README.md) | Player overview, requirements, install, compatibility |
| [RELEASE.md](RELEASE.md) | Version numbers, release builder, package contents and publishing |
| [CHANGELOG.md](CHANGELOG.md) | Change-note format and upkeep for every implementation task |

## Start here

| Change / symptom | Route |
|---|---|
| Buttons not moved at all | PatchLib log lines for `SubmarketButtonPatch` (below) -> [patch target](#patch-target) after a game update |
| Wrong column count, widths, gaps or strip height | `SubmarketButtonPatch.afterConstructor -> SubmarketGrid.of -> slot / left / top / buttonWidth / height` |
| Storage button in the wrong place | `SubmarketButtonPatch.storageIndex -> SubmarketGrid.slot` |
| Cargo or fleet panel overlaps the strip | [strip height contract](#vanilla-strip) |

## Registration and lifecycle

| Registry / hook | Owners |
|---|---|
| `mod_info.json` | No `modPlugin`; jar `jars/AlignedButtons.jar`; dependency PatchLib; `utility` because nothing is saved |
| PatchLib discovery | PatchLib scans `jars/AlignedButtons.jar` for `@Patch` classes and installs `submarkets/SubmarketButtonPatch` when the strip class loads; nothing is registered by hand |
| `data/config/version/version_files.csv` | Version Checker entry for `AlignedButtons.version` |

There is no load order: the patch runs each time the game builds a submarket strip, which happens when the cargo or fleet screen opens at a market.

### Build and checks

`tools/ReleaseBuild.java` (`build`, `package`) and `tools/ReleaseBuildChecks.java` compile against `lib/` and never ship in the release jar; they, `lib/` and `.github/workflows/release.yml` are owned by [RELEASE.md](RELEASE.md). `tools/LayoutChecks.java` runs headless from the repository root against the classes `build` leaves in `out/release-build-*/classes`:

```text
java -cp "out/release-build-<n>/classes:lib/*" alignedbuttons.tools.LayoutChecks
```

It checks `SubmarketGrid` for strip widths from 450 to 1900 px and 1 to 12 buttons, with and without storage: every button gets its own slot inside the strip, columns are 5 px apart and differ by at most 1 px in width, the last column ends on the strip edge, storage sits in the last column of the last row, the column and row counts match the vanilla wrap rule, and no button is narrower than vanilla.

## Source owners

Use `rg --files jars/src/alignedbuttons` for the complete inventory.

### `submarkets`

| File | Owner / connection |
|---|---|
| `submarkets/SubmarketButtonPatch.java` | The only PatchLib patch. `@After` on the strip constructor: collects the buttons, finds storage, places every button through `SubmarketGrid` and sets the strip height |
| `submarkets/SubmarketGrid.java` | All layout math: column count, slot per button (storage last column of the last row), rounded column edges, row tops and strip height. No game types, so `LayoutChecks` can run it |

## Contracts

### Vanilla strip

Evidence: decompiled `0.98a-RC8` from the `starsector-knowledge` skill. Obfuscated names are this build's and change between builds and platforms.

- The submarket strip is one class in `com.fs.starfarer.campaign.ui` (`Z` on `0.98a-RC8`) that extends the game's base panel (so it implements `UIPanelAPI`) and declares `showSubmarketTextDialog`. Its only constructor takes `(SectorEntityToken, MarketAPI, float width, float height, boolean, callback)` and builds every button in a private method it calls last.
- Both screens build it: the cargo screen with height 48, the fleet screen with width `getWidth() - 20` and height 48. Both read the strip's height right after the constructor returns to lay out the panels below it, so a height set inside the `@After` hook is the height they use.
- Vanilla buttons are `(int)(height * 640 / 400) - 2 + 120` = 194 px wide with a 5 px gap; rows are `height + 2` apart. A row wraps once `x + width + gap` passes the strip width, so a row holds `floor(width / (194 + 5))` buttons. Storage is moved to the end of the list and placed with `inBR(0, 0)`, so it hugs the strip's right edge instead of the grid.
- The strip keeps its buttons in display order in its only `List` field. Each button implements `ButtonAPI` and carries its `Submarket` (a `SubmarketAPI`) as custom data.
- Each button draws through a render panel whose position the button copies on every update, so a later `setSize` on the button reaches the drawn frame. The frame (background, border, highlight strip) uses the current width every frame; the icon and name sit at fixed offsets on the left.
- Resizing the strip does not rebuild it: neither the strip class nor its base panel overrides `sizeChanged`.

### Patch target

- Class: `@ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui", methodMatches = @MethodMatch(methodName = "showSubmarketTextDialog"))`. `UIPanelAPI` and the package are stable or near stable; `showSubmarketTextDialog` is an inferred readable name. No obfuscated name appears in the code.
- Method: `@After` on the constructor (`methodType = CONSTRUCTOR`, `parameterCount = 6`). Additive: vanilla's layout runs first, the hook then moves buttons. Nothing is skipped or replaced.
- Buttons are read with `context.getField(FieldQuery.create().subtype(List.class).build())`, which finds the strip's own list before any superclass field.
- After a game update, check in order: the package, the `showSubmarketTextDialog` name, the constructor arity, that the button list is still the strip's only `List` field, and the 5 px gap and 2 px row gap in `SubmarketGrid`.
- PatchLib logs `Discovered patch ... SubmarketButtonPatch` at startup and `Installed a hook patch site at com.fs.starfarer.campaign.ui.<class> on method <init>` the first time a strip is built. `TogglePatch alignedbuttons.submarkets.SubmarketButtonPatch` (Console Commands) switches it off for comparison.

### Shared constraints

- `SubmarketGrid` is the only layout math; the patch only reads positions and applies what the grid returns.
- Another mod that also moves these buttons after construction wins or loses by PatchLib order; this patch uses the default order 0.
