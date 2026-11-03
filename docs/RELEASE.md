# Release

How Tidy.Panels is built, packaged and published. Nothing here runs unless a release is asked for: no version bumps, rebuilt jars, tags or published releases for ordinary work. Changelog entry rules are in [CHANGELOG.md](CHANGELOG.md); code owners are in [ARCHITECTURE.md](ARCHITECTURE.md).

## Pieces

| Piece | Path | Role |
|---|---|---|
| Changelog | `Changelog` | Source of the version and the release notes. The newest version heading is the release |
| Mod metadata | `mod_info.json` | Version stamped into the ZIP's copy; loads `jars/TidyPanels.jar` |
| Version file | `TidyPanels.version` | Online Version Checker file on `master`, registered in `data/config/version/version_files.csv`; stamped at packaging, committed back after a release |
| Release builder | `jars/src/tidypanels/tools/ReleaseBuild.java` | `build` and `package`, run with `java <file>` |
| Builder checks | `jars/src/tidypanels/tools/ReleaseBuildChecks.java` | Headless checks of changelog parsing, stamping and file filtering |
| Build libraries | `lib/` | Game API and library jars from `0.98a-RC8`, `PatchLibAPI.jar` from PatchLib `1.0.3`, and `LunaLib.jar`, `LazyLib.jar` and `Kotlin-Runtime.jar` copied from Illustrated.Entities' `lib/` |
| Release jar | `jars/release/TidyPanels.jar` | Built by `build`, gitignored, runtime classes only; shipped as `jars/TidyPanels.jar` |
| Release workflow | `.github/workflows/release.yml` | Manual only: preview without a version, publish with one |

The IDE artifact (`jars/TidyPanels.jar`, gitignored) is separate from the release jar, so an IDE build never replaces it.

The release jar is built in CI from `lib/` and never committed, as in Illustrated.Entities. Do not add the house template's committed jar and source hash check.

## Versions

Versions use `major.minor.letter`, starting at `1.0.a`: the first number for a major release, the second for changes players will notice, the letter for fixes and small patches. Reset the lower parts when a higher one changes. The newest `Changelog` section is marked `- UNRELEASED` until it ships.

## Build the release jar

From the repository root, with JDK 17:

```text
java jars/src/tidypanels/tools/ReleaseBuild.java build
```

- Compiles against the jars in `lib/`: `starfarer.api.jar`, `lwjgl.jar`, `lwjgl_util.jar`, `json.jar`, `log4j-1.2.9.jar`, `PatchLibAPI.jar`, `LazyLib.jar`, `LunaLib.jar` and `Kotlin-Runtime.jar`. No game install is needed. `starfarer_obf.jar` is not needed: the code uses only API types.
- `PatchLibAPI.jar` registers PatchLib's annotation processor, so `build` also checks handler shapes (static, one parameter of the exact context type).
- Builds into a fresh `out/release-build-*`, keeps only runtime classes (the `tools` package never ships), and replaces the release jar only after a clean compile. The jar must contain `tidypanels/submarkets/SubmarketButtonPatch.class`; the mod has no mod plugin.
- After a game or LunaLib update, copy the new jars into `lib/`. After a PatchLib update, copy `PatchLibAPI.jar` from the PatchLib mod's `jars/` folder (never `PatchLib.jar` or `PatchLibAgent.jar`) and update the version in the table above.

## Package

```text
java jars/src/tidypanels/tools/ReleaseBuild.java package          # preview
java jars/src/tidypanels/tools/ReleaseBuild.java package 1.0.a    # release
```

- Needs a release jar from `build`.
- Preview packages the newest changelog section.
- Release requires the version (`1.0.a`, no `v`) to equal the newest changelog heading, and that section must not be `UNRELEASED`.
- Ships Git-tracked files only: `mod_info.json`, `TidyPanels.version`, `Changelog`, `LICENSE` (MIT, required in every package) and credit files, `data/` (`csv json shader ship wpn proj system variant skin`), `graphics/` (`png jpg jpeg`), `sounds/` (`ogg wav`) and the release jar as `jars/TidyPanels.jar`. Source, `lib/`, docs, IDE, agent and workflow files, and any path under a dot, `unused`, `unedited`, `tools` or `docs` directory are left out.
- Fails if a `graphics/tidypanels/` or `sounds/tidypanels/` path named in `data/` is not packaged.
- Output in `out/release/`: `Tidy.Panels-<version>.zip` (one `Tidy.Panels/` folder, sorted, fixed timestamps), `release-notes.md`, the stamped version file and `version.txt`.

## Publish

1. Finish the newest `Changelog` section and remove `UNRELEASED`.
2. Run `build` and `package <version>` locally as the preview check, then merge to `master`.
3. Start the Release workflow on `master` with `version` set: Actions tab, Release, Run workflow, or `gh workflow run release.yml --ref master -f version=<version>`.
4. The workflow runs the builder checks, builds the jar, packages, creates the release and its tag on the packaged commit, then commits the stamped `TidyPanels.version` to `master`.
5. Check the release ZIP and notes, and that `master`'s version file names the new version.

The workflow has no push, pull request, tag or schedule trigger. Without a version it only uploads a preview artifact. It refuses to overwrite an existing release, and run on an existing version tag it publishes that tag. It compiles from `lib/` and downloads nothing.

## Maintenance

Update this guide when `ReleaseBuild`, `ReleaseBuildChecks`, `.github/workflows/release.yml`, the version file or the jars in `lib/` change.
