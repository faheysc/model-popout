# MAJOR.RELEASE.REVISION policy

The approved first publication is `0.1.0`, source commit `38be0a9edc07c04462d1317b5bc63b9f3e1f50fd`. Preserve that historical version. Revision zero is the starting point for this policy, not a claim about the number of development passes before publication.

- **MAJOR:** generation of the plugin. Change deliberately for a new generation.
- **RELEASE:** published release sequence within that generation. The first publication is release 1; the next accepted Hub update would be release 2.
- **REVISION:** cumulative accepted development-pass counter. Advance once per completed, accepted pass that changes the project. Do not reset it when publishing.

Read-only research, conversation, builds, test runs, discarded attempts, and unsuccessful changes that leave no accepted project changes do not advance revision. Multiple edits and fixes within one implementation pass count once. A Git commit count is not the revision count.

Accepted development passes are `0.1.1` (inspection), `0.1.2` (submenu placement), and `0.1.3` (corrected automatic window controls and UI simplification). See [DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md). Acceptance of revision 3 is recorded before preparing release metadata `0.2.3`. Windows VM verification remains a separate submission gate.

The explicit `version` in `runelite-plugin.properties` is canonical. Gradle and artifact validation read it directly. Hub standard packaging replaces project build scripts; production needs no custom generation task or counter keys. No build or startup advances the version.

Prepare a publication's release increment in the final source change set before submitting its hash to Hub. Mark it pending in the release ledger until the Hub accepts it. Failed submission attempts do not count as additional publications. Never retroactively relabel the initial commit or invent historical revision counts.

Release-source commit and push are authorized. Submit the Hub update only after final-build Windows VM verification. AFK Recap is outside this change set.
