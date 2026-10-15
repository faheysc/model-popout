# Accepted development history

| Version | State | Evidence and scope |
| --- | --- | --- |
| `0.1.0` | Published baseline | Initial release, source commit `38be0a9edc07c04462d1317b5bc63b9f3e1f50fd`. No historical development-pass count is inferred. |
| `0.1.1` | Accepted development pass | Standalone right-click inspection, independent windows, actor lifecycle/frozen models, HUD, bounded capture/rendering, and configuration. Owner confirmed the first player inspection opened the correct model and preserved existing rendering, then authorized the next stage. This is initial player verification, not a claim that every broader in-game checklist item was exercised. |
| `0.1.2` | Accepted development pass | Existing actor-submenu placement, ordering/deduplication/fallback, Kill Clog receiver contract/specification, and version-source synchronization. No sender, provider setting, or Hiscores workaround is active. |
| `0.1.3` | Accepted development pass | Shared custom window controls, independent title-bar alpha, viewport clipping, geometry and lifecycle regressions. Includes the duplicate-title-bar correction: reuse inherited Swing chrome and built-in fallback. The subsequent Automatic-only simplification removes the exposed mode setting and ignores old mode preferences. Includes removal of the secondary settings sidebar and shared viewport input. Accepted by the owner’s release-preparation instruction; acceptance does not claim Windows VM verification. |
| `0.2.3` | Release candidate; Hub update pending | Release 2 containing cumulative accepted revision 3. Source commit/push authorized by the owner. Windows VM verification of the final corrected build is required before submitting the Hub update. |

The currently published Hub manifest still points to baseline `38be0a9edc07c04462d1317b5bc63b9f3e1f50fd` (`0.1.0`). Metadata now records candidate `0.2.3`; this does not mark Hub publication complete. Historical investigation documents describe their original pending state.
