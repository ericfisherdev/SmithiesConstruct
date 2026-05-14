# Reporting issues

Before reporting an issue, search to see if anyone has the same issue. Make sure to check closed issues as well as there is a chance one of them has the solution.

Provide clear steps to reproduce the issue, especially in the case of crashes. "_It crashed_", is not useful, "_crash when placing an item in a tool station_" is useful, and "crash when placing a pickaxe in a tool station" is even better. If the bug happens on a server, make sure to test in single player, and to test with a normal Forge server if using Sponge Forge.

## Versions

Always test with the latest versions of all relevant mods; chances are the bug you are reporting has been fixed in a later version of Tinkers Construct, Mantle, or even Forge. We do not support versions Minecraft versions before the latest stable Forge release, which is currently 1.12.2. No more work is being done on older versions so issues from those versions will be closed.

## Crashes

For crashes, always provide a crash report. Crash reports should be added using an external site such as https://pastebin.com or https://gist.github.com and linked in the issue to avoid clutter.

## Mod list

Try to minimize the list of mods needed to reproduce the bug. Performance enhancing mods and core mods can be expecially problematic due to their changes to the base Minecraft code, so especially try to remove them to see if it is the cause and provide that information in the report.

OptiFine is especially problematic due to it being closed source and the fact that it makes many unknown changes to Forge internals, so we do not support issues caused by OptiFine.

# Suggestions

We do not take suggestions on the tracker. Ideas may be considered in the overall context, but are generally closed to keep the tracker clean. Tinkers' Constructs mechanics are designed to work as is. New tools or weapons would either be added if they fulfill a missing demand and nothing more important is to be done.

Please also read the [Frequently Asked Questions](https://github.com/SlimeKnights/TinkersConstruct/wiki/FAQ) on the wiki, as it covers many common suggestions.

If you want a better place to discuss ideas, consider joining [the SlimeKnights Discord](https://discord.gg/njGrvuh). We typically do not implement suggestions, but someone may like the idea enough to implement it in an addon or its own mod.

# Pull requests

Always talk to the developers first before working on pull requests, such as on [the SlimeKnights Discord](https://discord.gg/njGrvuh). Pull requests will only be accepted if they contribute something meaningful and do not hinder maintainability. Furthermore pull requests must be tested and ensure to not break anything.

An exception to this rule is translation pull requests, which we generally allow without previous discussion. Please do not translate using an automatic translator such as Google Translate as those translations tend to be filled with errors or use the wrong context.

# Local development (port-1.21.1)

The 1.21.1 port targets Java 21 with the NeoForge `moddev` Gradle plugin. After cloning, run the one-time setup:

```bash
./gradlew installGitHooks
```

That points `core.hooksPath` at `scripts/git-hooks/`, so commits trigger the same checks CI runs on every PR:

- **Spotless** formats `src/main/java/slimeknights/sconstruct/port1211/**` and `src/test/java/**` against the modernized `eclipse_formatter.xml` (K&R braces, modern method-paren spacing) plus a stable import order. The pre-commit hook runs `spotlessCheck`; if it fails, run `./gradlew spotlessApply` to auto-fix, re-stage, and re-commit.
- **PMD** scans the same source set against `gradle/pmd/ruleset.xml`. Both the hook and CI run `./gradlew pmdMain pmdTest` and pipe the XML reports through `scripts/lint/pmd_diff_check.py` against the relevant diff (`git diff --cached -U0` locally, `git diff origin/<base>...HEAD -U0` in CI). Only findings on lines added/modified by the diff fail the gate; pre-existing findings are reported but don't block. The script is plain Python, no external binaries required.
- **JUnit** suite (`./gradlew test`).

CI (`.github/workflows/ci.yml`) runs the same `spotlessCheck`/`pmdMain`/`pmdTest`/`test` tasks on every PR and push to `1.21.1`, plus auto-labels PRs by changed paths (`.github/labeler.yml`). The PMD gate in CI fails *only* on findings on lines added or modified by the PR — pre-existing findings under `port1211/**` are reported but don't block. Spotless, by contrast, fails on *any* formatting drift since `spotlessApply` is one command away. Use `git commit --no-verify` to bypass the local hook in emergencies; CI is the source of truth.

Legacy 1.12 sources under `slimeknights/tconstruct/**` (excluding the now-renamed port tree) are excluded from compilation, PMD, and Spotless — they live on disk for reference until ported. Active port code lives under `slimeknights/sconstruct/port1211/**`.
