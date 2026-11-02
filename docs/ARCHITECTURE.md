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
| Wrong column count, widths, gaps or strip height | `SubmarketButtonPatch.afterConstructor -> LayoutSettings.layout -> SubmarketGrid.of -> slot / left / top / buttonWidth / height` |
| Storage button in the wrong place | `SubmarketButtonPatch.storageIndex -> SubmarketGrid.slot` |
| Names still shown with Icons only, or not cut short with Five per row | `SubmarketButtonPatch.relabel -> SubmarketGrid.labelWidth / fit`; [render panel contract](#vanilla-strip) |
| Unfold on hover: wrong tile opens, jumps, or names overflow | `SubmarketButtonPatch.afterAdvance -> UnfoldStrip.advance -> UnfoldRows.at / aim / step -> UnfoldStrip.apply -> UnfoldRows.width / left`; names: `LabelClipPatch.clipName` |
| Layout setting has no effect | `data/config/LunaSettings.csv` `aligned_buttons_layout` -> `LayoutSettings.layout` -> `ButtonLayout.byLabel` |
| Unfold time has no effect | `data/config/LunaSettings.csv` `aligned_buttons_unfold_time` -> `LayoutSettings.unfoldTime` -> `UnfoldRows.unfoldTime` -> `UnfoldRows.step` |
| Cargo or fleet panel overlaps the strip | [strip height contract](#vanilla-strip) |
| Selected button has no outline, or the wrong color | `SelectedOutlinePatch.afterRender -> RenderPanels.submarketButton -> ButtonAPI.isHighlighted -> SubmarketAPI.getFaction().getBrightUIColor()` |
| Stray 1 px line right of an icon only button while it glows | `ButtonHighlightPatch.skipNegativeFill`; [highlight patch target](#patch-target) |

## Registration and lifecycle

| Registry / hook | Owners |
|---|---|
| `mod_info.json` | No `modPlugin`; jar `jars/AlignedButtons.jar`; dependencies PatchLib, LunaLib; `utility` because nothing is saved |
| PatchLib discovery | PatchLib scans `jars/AlignedButtons.jar` for `@Patch` classes and installs `submarkets/SubmarketButtonPatch` when the strip class loads and `submarkets/ButtonHighlightPatch`, `submarkets/SelectedOutlinePatch` and `submarkets/LabelClipPatch` when the button render panel class loads; nothing is registered by hand |
| `data/config/LunaSettings.csv` | The layout and unfold time settings and their help text |
| `data/config/version/version_files.csv` | Version Checker entry for `AlignedButtons.version` |

There is no load order: the layout patch runs each time the game builds a submarket strip, which happens when the cargo or fleet screen opens at a market. For `UNFOLD` it stores an `UnfoldStrip` in the strip's PatchLib data under `aligned_buttons_unfold`; the strip's `advanceImpl` hook animates it every frame while the screen is open. Nothing is saved.

### Settings

`submarkets/LayoutSettings` owns the mod id, the setting ids `aligned_buttons_layout` and `aligned_buttons_unfold_time`, the unfold time default and bounds (0.1 s, 0 to 1), and the readers `layout()` and `unfoldTime()`. `submarkets/ButtonLayout` has one constant per radio option; each constant's `label` is the option text in `data/config/LunaSettings.csv` and the value LunaLib saves. `data/config/LunaSettings.csv` owns the help text; its defaults, bounds and option order mirror those classes. The readers run once per strip build, which only happens when a screen opens, so there is no listener or cache. A missing or unknown layout falls back to `ButtonLayout.DEFAULT` (`FIVE_PER_ROW`); a missing or NaN unfold time falls back to 0.1 and any other value is clamped to 0 to 1. LunaLib writes the default into `saves/common/LunaSettings/aligned_buttons.json` on first launch and keeps it there, so changing the default only reaches installs that never stored the setting. A changed setting applies the next time a screen opens.

### Build and checks

`tools/ReleaseBuild.java` (`build`, `package`) and `tools/ReleaseBuildChecks.java` compile against `lib/` and never ship in the release jar; they, `lib/` and `.github/workflows/release.yml` are owned by [RELEASE.md](RELEASE.md). `tools/LayoutChecks.java` runs headless from the repository root against the classes `build` leaves in `out/release-build-*/classes`:

```text
java -cp "out/release-build-<n>/classes:lib/*" alignedbuttons.tools.LayoutChecks
```

It checks `SubmarketGrid` for every `ButtonLayout`, strip widths from 450 to 1900 px and 1 to 12 buttons, with and without storage: every button gets its own slot inside the strip, the last column ends on the strip edge, and storage sits in the last column of the last row. Stretched layouts keep 5 px gaps with widths that differ by at most 1 px. Per layout: `STRETCHED` matches the vanilla column and row counts and is never narrower than vanilla; `ICONS` fits as many 72 px tiles as the row allows, keeps the icon exactly centered (4 px each side), and spreads the spare room over gaps of at least 5 px that differ by at most 1 px; `FIVE_PER_ROW` always has five columns. For `UNFOLD` it checks `UnfoldRows` the same way, then sweeps the cursor across every tile (3 frames at 60 fps each) and opens each tile in turn: one tile worth of unfolding per row at every frame, each row keeps its total width, 5 px gaps, storage on the right edge with no overlap, several tiles move at once during a sweep, the opening tile stays under a fixed cursor point, a tile opens fully on the frame its unfold time is reached and not before, and a folded tile's clip edge lies left of where its name starts. The `UNFOLD` checks run for unfold times 0, 0.1, 0.2 and 1 s. It also checks `SubmarketGrid.fit` with a fixed width per character, and that `LunaSettings.csv` and `ButtonLayout` agree (header, nine columns, `Radio` type, `DEFAULT` is the CSV default and named in the help text, option order, fallback), and that the unfold time row matches `LayoutSettings` (`Double` type, default 0.1, bounds, help text, fallback and clamping).

## Source owners

Use `rg --files jars/src/alignedbuttons` for the complete inventory.

### `submarkets`

| File | Owner / connection |
|---|---|
| `submarkets/SubmarketButtonPatch.java` | The layout patch. `@After` on the strip constructor: collects the buttons, finds storage, sets the strip height, places every button through `SubmarketGrid` and for `ICONS` and `FIVE_PER_ROW` rewrites each name label, or for `UNFOLD` builds an `UnfoldStrip`. `@After` on the strip's `advanceImpl` advances that `UnfoldStrip` |
| `submarkets/SubmarketGrid.java` | All layout math: column count per `ButtonLayout`, slot per button (storage last column of the last row), column edges (stretched tiles, or for `ICONS` fixed 72 px tiles with the spare room in the gaps), row tops, strip height, the room left for a name and `fit`, which cuts each name line to that room with `...`. No game types, so `LayoutChecks` can run it |
| `submarkets/ButtonLayout.java` | The layout options and their LunaLib labels, `DEFAULT`, `byLabel` |
| `submarkets/LayoutSettings.java` | Setting ids, unfold time default and bounds, `layout()` and `unfoldTime()` from LunaLib |
| `submarkets/UnfoldRows.java` | `UNFOLD` math, no game types: rows of tiles (storage last, pinned to the right edge), how far each tile is open (each row sums to 1), the target per row, `step` (the target opens fully over `unfoldTime` seconds, at once for 0, and takes width from the others in proportion to what they hold), positions and hit test |
| `submarkets/UnfoldStrip.java` | `UNFOLD` state on one strip: weak references to its buttons, the cursor test against the strip (the tile under the cursor becomes its row's target; a gap or leaving the strip keeps the last one) and `apply`, which moves the buttons and sets label opacity |
| `submarkets/SelectedOutlinePatch.java` | `@After` on the button render panel's `renderImpl`: draws a 3 px outline with cut corners, in the submarket faction's bright UI color, around the selected (highlighted) submarket button, on top of the frame, icon and name |
| `submarkets/LabelClipPatch.java` | Redirect around the name label's `render` in the button render panel's `renderImpl`: clips it with `glScissor` to the tile, 4 px inside each side edge, so a growing `UNFOLD` tile reveals its name instead of fading it in |
| `submarkets/RenderPanels.java` | Reads the button off a render panel (`button` field) and returns it only when it carries a `SubmarketAPI` |
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
- The button render panel class is also used for the cargo screen's Colony Info button. Each button sets itself on its render panel (the readable `button` field the panel inherits) at the start of every render, so the panel's render hooks can read the button and its custom data; `RenderPanels` skips any button without a `SubmarketAPI`. The selected submarket is the one whose button `isHighlighted()`.
- Resizing the strip does not rebuild it: neither the strip class nor its base panel overrides `sizeChanged`.
- The strip declares `advanceImpl(float)`, called every frame with the frame time in seconds while the screen is open. The cargo screen selects a submarket inside the constructor, the fleet screen calls `selectFirst()` right after it, so `UnfoldStrip` reads the selected (highlighted) button on its first advance and opens it without animation.
- `SettingsAPI.getMouseX/Y` and `PositionAPI.getX/Y` use the same virtual screen coordinates with y up from the bottom; the engine's own `containsMouse` compares them directly.
- The render panel draws the name with the label's own `render(float)` as the last step before the border lines. `LabelClipPatch` wraps that one call in a scissor box: virtual coordinates times `getScreenWidthPixels() / getScreenWidth()` (and the height pair), which is the scale the vanilla clip stack uses, intersected with any scissor already active, inside `glPushAttrib(GL_SCISSOR_BIT)` / `glPopAttrib`. On a 72 px folded tile the name starts 78 px in and the clip ends 68 px in, so it is fully hidden; on an open tile `fit` already cut it to the room left of the clip.

### Patch target

`SubmarketButtonPatch`:

- Class: `@ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui", methodMatches = @MethodMatch(methodName = "showSubmarketTextDialog"))`. `UIPanelAPI` and the package are stable or near stable; `showSubmarketTextDialog` is an inferred readable name. No obfuscated name appears in the code.
- Method: `@After` on the constructor (`methodType = CONSTRUCTOR`, `parameterCount = 6`). Vanilla's layout runs first; the hook then overwrites button positions, the strip height and, for `ICONS` and `FIVE_PER_ROW`, the name text. No vanilla code is skipped.
- `advanceImpl`: `@After`, `parameterCount = 1`, declared on the strip itself. For every layout but `UNFOLD` the handler is one `PatchData` lookup that finds nothing.
- `UnfoldStrip` holds the buttons and labels through `WeakReference`, because it lives in the strip's own `PatchData` and a strong reference to the strip's children would keep closed screens in memory.
- Buttons are read with `context.getField(FieldQuery.create().subtype(List.class).build())`, which finds the strip's own list before any superclass field. The name label is read with `getMethod` for `getButtonPanel` (no parameters) on the button and `getField` for the `LabelAPI` field on the panel, both behind `hasMethod` / `hasField`, so a missing member leaves the name as vanilla shows it.
- After a game update, check in order: the package, the `showSubmarketTextDialog` name, the constructor arity, `advanceImpl` on the strip, that the button list is still the strip's only `List` field, `getButtonPanel`, the render panel's single `LabelAPI` field and when it creates it, and the gap, border, icon and label constants in `SubmarketGrid`.
- PatchLib logs `Discovered patch ... SubmarketButtonPatch` at startup and `Installed a hook patch site at com.fs.starfarer.campaign.ui.<class> on method <init>` the first time a strip is built. `TogglePatch alignedbuttons.submarkets.SubmarketButtonPatch` (Console Commands) switches it off for comparison.

`ButtonHighlightPatch`:

- Class: `@ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui", methodMatches = @MethodMatch(methodType = CONSTRUCTOR, parameterTypes = {String, String, Color, Color, Color}))`. On `0.98a-RC8` only the button render panel (`ooOO`) in that package has this constructor; the other button panels there take `(X, Color, String, Color, Color)`. Inferred by shape.
- Method: `@RedirectMethodCall` in `renderImpl` (one parameter) on every call shaped `(float, float, float, float, Color, float)` returning `void`. In `renderImpl` that is the highlight strip fill and the disabled overlay fill (full width). The handler calls down unless the width argument is negative, so vanilla widths and other mods' layers on the same call are untouched.
- The game classes are Java 17 class files, which PatchLib redirects need.
- After a game update, check the constructor shape, `renderImpl`, and that the strip fill still uses that call shape. If the redirect stops installing, icon only buttons show a 1 px line in their right gap while they glow; nothing else breaks.
- PatchLib logs `Installed a redirect patch site at com.fs.starfarer.campaign.ui.<class> on method renderImpl` the first time a submarket button draws. `TogglePatch alignedbuttons.submarkets.ButtonHighlightPatch` switches it off.

`SelectedOutlinePatch`:

- Same class match as `ButtonHighlightPatch`. `@After` on `renderImpl` (one parameter); the parameter is the render alpha, which the outline uses so it fades with the screen.
- Draws with raw `GL11` (a triangle strip between two cut corner octagons) inside `glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT | GL_CURRENT_BIT)` / `glPopAttrib`, so texture, blend and color state are restored for whatever renders next.
- After a game update, check `renderImpl`, the `button` field on the render panel's base class, and that the selected button still reports `isHighlighted()`.

`LabelClipPatch`:

- Same class match as `ButtonHighlightPatch`. `@RedirectMethodCall` in `renderImpl` on `render(float)` with `owner = @ClassMatch(subtype = LabelAPI.class)`: the label class declares `render`, the icon's `render` is declared on a base class that is no `LabelAPI`, so only the name call matches. Always calls down; only buttons carrying a `SubmarketAPI` get the scissor.
- After a game update, check that the name is still drawn by a `LabelAPI` `render(float)` call inside `renderImpl`.

### Shared constraints

- `SubmarketGrid` is the only layout math; the patch only reads positions and applies what the grid returns.
- Another mod that also moves these buttons after construction wins or loses by PatchLib order; this patch uses the default order 0.
