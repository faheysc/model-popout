# Remaining player-only flicker investigation

The user confirmed the previous whole-surface repaint correction stabilizes the
window/background/controls, but intermittent player flicker remains. No root cause
for that remaining flicker is confirmed yet. This change adds temporary diagnostics,
not another rendering correction. It preserves the working root repaint route.

## Evidence so far

Compared production sources with the verified pre-HUD checkpoint
`model-popout-before-combat-hud-20261013-201318`. The model rasterizer, snapshot,
projection, texture raster/cache, scene painter, pacing and window presentation
implementations remain unchanged. HUD integration changed the plugin callbacks,
actor identity metadata and window repaint requests. The HUD painter uses a cloned
graphics context with SrcOver and cannot erase destination alpha. It never writes
into the completed model BufferedImage. Null/failed captures return before updating
view state. Rejected render generations leave the displayed image intact. Explicit
image clearing is confined to lifecycle cleanup. These observations rule out simple
unconditional clearing, but do not establish the physical desktop cause.

Added automated checks distinguish null, blank, partly transparent and opaque player
slots independently of NPC pixels; verify bounded trace storage; and exercise fixed
completed images, later camera renders and HUD updates with and without NPC slots.
The checks assert model pixels remain immutable and HUD compositing never lowers
player alpha. These synthetic tests do not reproduce the user's flicker, and are
investigation coverage rather than a regression test proving a correction.

## Collect four runtime traces

Use the development launcher in safe mode, without the GPU plugin. Keep the same
FPS, opacity, equipment and camera in each case. Enable Show Hitsplats and Show HP
Bars. Keep debug logging enabled (the supplied development launcher already does).
For each case, restart the development client: these diagnostic JVM properties are
read once at plugin construction. Run the normal development command from the
`model-popout` directory, adding this environment variable:

| Case | JAVA_TOOL_OPTIONS value | NPC setting |
| --- | --- | --- |
| A: fixed image, live HUD | `-Dmodelpopout.paintDiagnostics=true -Dmodelpopout.traceFreezeImage=true` | Disabled |
| B: live image, HUD processing disabled | `-Dmodelpopout.paintDiagnostics=true -Dmodelpopout.traceDisableHud=true` | Disabled |
| C: live image and HUD | `-Dmodelpopout.paintDiagnostics=true` | Disabled |
| D: live player/NPC and HUD | `-Dmodelpopout.paintDiagnostics=true` | Enabled; engage a target |

Example for A:

```sh
JAVA_TOOL_OPTIONS='-Dmodelpopout.paintDiagnostics=true -Dmodelpopout.traceFreezeImage=true' ./gradlew run -PruneLiteVersion=1.13.1
```

For IntelliJ, put the same value in the run configuration's `JAVA_TOOL_OPTIONS`
environment variable. Preserve any existing JVM options alongside these additions.

Case A retains the first completed scene and its camera/actor metadata while live
HUD packets continue. Capture/render completion still runs for observation; later
completed images are deliberately held. Do not rotate/zoom or switch equipment in
A. Case B bypasses hitsplat event processing, health/sprite sampling, HUD publication
and HUD painting. It retains the small actor identity bookkeeping used to tag model
frames and normal lifecycle cleanup. Simply disabling both HUD config checkboxes
is not equivalent to B, because normal HUD capture would still run.

For each case, open the viewer and observe 15 seconds idle followed by 15 seconds
of animation/combat if applicable. Note whether flicker occurs and its approximate
wall-clock time. Save the corresponding development console output or
`~/.runelite/logs/client.log` as a separate A/B/C/D file **before restarting**. Include
`Model Popout player trace` and `Model Popout paint audit` records. Record FPS target,
opacity and whether the NPC or only player flickers. A short desktop recording with
a visible clock would help correlate the exact frame. Redact unrelated client log
information before sharing.

## What the trace distinguishes

- `capture`: lifecycle acceptance, actor key, owned pose identity, vertex/face
  counts and face-alpha hash. Null captures are explicitly recorded.
- `render-start` / `render-complete`: job identity and source pose; completed
  player slot image identity, dimensions, visible/opaque pixel counts, total alpha
  and color checksum. NPC pixels do not contaminate these measurements.
- `render-accept` / `discard`: generation/lifecycle rejection, without clearing
  the old displayed image.
- `publish`: exact old/new image identity and actor/lifecycle metadata.
- `hud`: game cycle and actor keys independent of model publication.
- `paint`: sampled four times per second, image checksum before/after HUD,
  window paint route, viewport, source/display actor keys and HUD cycle.
- `clear`: explicit lifecycle clearing, including viewer/running/capture state.
- `hold`: A's intentionally retained completed image.

If visible pixel/alpha counts collapse in `render-complete`, investigate capture or
raster output. If normal images complete but publication changes to null/blank or
actor/session rejection spikes, investigate publication/lifecycle. If paint source
checksums change after HUD, investigate image mutation. If stable image checksums
are painted while the desktop visibly flickers, the remaining investigation belongs
in the native painting/presentation path. None of these is a confirmed cause yet.

Logging uses the existing one-second metrics timer and a bounded 4096-record buffer;
it retains no images or actors. `dropped` identifies trace overflow. Pixel scanning
and debug logging add diagnostic CPU cost and can affect timing, so also record
whether enabling diagnostics changes the flicker. Normal launches with these JVM
properties absent do not scan pixels or alter capture, publication or HUD behavior.
Remove all three properties after collecting traces. No new window controls,
rendering threads or timers were introduced.

## Validation

Java 11 compilation and the complete suite with private cape/HUD fixtures passed:
143 passed, 0 failed, 3 native desktop tests skipped. Source audit passed. A KDE
runtime trace is required before choosing a responsible correction. Headless tests
are not evidence that the remaining flicker has been fixed.
