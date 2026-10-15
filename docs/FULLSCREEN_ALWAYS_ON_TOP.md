# Fullscreen Always on Top — research only

Research date: 2026-10-15. No stacking implementation was changed. These are
API/source findings and qualified expectations, not Windows, browser-video,
exclusive-fullscreen, GNOME, Xfce or macOS runtime results.

## Finding

Supported Java 11 AWT/Swing APIs cannot guarantee that Model Popout remains
visible over every fullscreen application. Keep the current capability-checked
`Window.setAlwaysOnTop` behavior. It provides the portable desktop request, but
not a special fullscreen-overlay privilege. Java documents platform support
limits and unspecified ordering among competing always-on-top windows. The
support check does not certify visibility over a particular fullscreen app.
[Java 11 Window API](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Window.html#setAlwaysOnTop(boolean))

## Which fullscreen?

- **Browser/video fullscreen:** F11-style browser fullscreen and a site's video
  fullscreen button are distinct entry paths. The HTML Fullscreen API operates
  on a document's top layer; that layer does not set the OS stacking order of
  unrelated applications. It does not by itself mean DirectX exclusive fullscreen.
  [WHATWG Fullscreen](https://fullscreen.spec.whatwg.org/)
- **Borderless fullscreen:** a desktop window covers the monitor without borders.
  Ordinary desktop stacking can apply, but compositor fullscreen policy can still
  promote that window. Size alone does not establish its layer.
- **Exclusive fullscreen:** an application uses a graphics presentation path with
  display ownership rather than an ordinary overlapping desktop window. An AWT
  window has no supported API to inject its pixels into another application's
  presentation path. Do not promise a regular topmost window can overlay it.
  [Microsoft DXGI fullscreen/output behavior](https://learn.microsoft.com/en-us/windows/win32/direct3darticles/dxgi-best-practices)

## Platform findings

| Environment | Expected behavior and practical limit |
| --- | --- |
| **Windows 10 and 11: browser or ordinary borderless fullscreen** | There is a reasonable path for the existing Java request to work over a non-topmost fullscreen window. Windows topmost windows remain above non-topmost windows while inactive. This is not a guarantee over competing topmost windows, all browsers, drivers or video paths. |
| **Windows 10 and 11: genuine exclusive fullscreen** | Ordinary AWT stacking cannot guarantee an overlay on that display. Some apps labeled exclusive actually run through Fullscreen Optimizations; test the actual presentation path rather than trusting the game's label. |
| **KDE Plasma on X11** | Active fullscreen can cover ordinary keep-above windows by design. Focus and output affect promotion; a popout briefly appearing when focused does not prove it stays visible after focusing the fullscreen app. |
| **KDE Plasma on Wayland, including XWayland clients** | KWin still owns the final stack. An XWayland client's keep-above request cannot force itself above KWin's fullscreen layer or native Wayland windows. Native Wayland is not an escape hatch. |
| **Other X11 desktops, e.g. Xfce** | A separate fullscreen layer exists in Xfwm4. The window manager decides ordering; do not assume Windows or KDE behavior. |
| **GNOME and other Wayland compositors** | Stacking is compositor policy. Mutter has its own layers and transient constraints. No universal Java request bypasses those policies. Exact release/browser behavior needs local testing. |
| **macOS** | Fullscreen Spaces introduce another visibility boundary. AppKit has explicit auxiliary-fullscreen participation behavior; Java 11 offers no equivalent portable AWT switch. No guarantee that a floating Java window appears in another app's fullscreen Space. |

Evidence for the Windows expectations: Chromium's Windows fullscreen handler
removes caption/frame styles, fills the monitor and uses `SWP_NOZORDER`, rather
than inherently turning the browser topmost. Combining that with Microsoft's
specified topmost ordering supports the expectation above; it is an inference,
not a Model Popout Windows test.
[Chromium implementation](https://raw.githubusercontent.com/chromium/chromium/main/ui/views/win/fullscreen_handler.cc),
[Microsoft SetWindowPos contract](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos)

Modern Windows presentation complicates simplistic claims that every fullscreen
window bypasses DWM. Borderless flip-model presentation can use direct scanout and
return to composition when another window overlaps. Fullscreen Optimizations can
make an app believe it is exclusive while presenting it as borderless. These
mechanisms permit some overlays; they do not grant Java universal overlay access.
[Microsoft flip model](https://learn.microsoft.com/en-us/windows/win32/direct3ddxgi/for-best-performance--use-dxgi-flip-model),
[Microsoft Fullscreen Optimizations](https://devblogs.microsoft.com/directx/demystifying-full-screen-optimizations/)

KWin evidence: the tagged 6.3.6 implementation assigns active fullscreen to
ActiveLayer and ordinary keep-above to AboveLayer; its stacking documentation puts
ActiveLayer higher. The shared code includes X11/Wayland handling and accounts for
focus/output. Current mainline retains the active-fullscreen/keep-above distinction.
Installed versions, user rules and distribution patches can differ.
[KWin window policy](https://raw.githubusercontent.com/KDE/kwin/v6.3.6/src/window.cpp),
[KWin stacking policy](https://raw.githubusercontent.com/KDE/kwin/v6.3.6/src/layers.cpp),
[current mainline](https://raw.githubusercontent.com/KDE/kwin/master/src/window.cpp)

For native Wayland, standard xdg-shell's toplevel requests include fullscreen,
maximize and minimize, but no unrestricted global above-fullscreen request.
This is protocol research, not a proposed replacement for Java's X11/XWayland
backend. Other desktop findings follow their own implementations, not a Linux-wide
heuristic.
[xdg-shell protocol](https://raw.githubusercontent.com/wayland-mirror/wayland-protocols/main/stable/xdg-shell/xdg-shell.xml),
[Xfwm4 fullscreen-layer handling](https://raw.githubusercontent.com/xfce-mirror/xfwm4/master/src/stacking.c),
[Mutter stacking constraints](https://raw.githubusercontent.com/GNOME/mutter/48.0/src/core/stack.c),
[Apple fullscreen auxiliary behavior](https://developer.apple.com/documentation/appkit/nswindow/collectionbehavior-swift.struct/fullscreenauxiliary)

## Practical options

1. Use the existing **Always on Top** option with windowed/maximized playback or
   a site's theater view. This avoids requesting the desktop's fullscreen layer;
   still check competing topmost windows.
2. For games, try windowed/borderless instead of genuine exclusive fullscreen.
   Borderless improves the opportunity for desktop composition on Windows, but
   does not guarantee visibility if a Linux compositor marks it fullscreen.
3. Put Model Popout on another monitor to avoid same-output overlap. Monitor focus,
   workspace/Space assignment and fullscreen multi-monitor choices still matter.
4. Where the browser/site permits it, use browser Picture-in-Picture alongside the
   viewer instead of fullscreen playback. Firefox documents its floating video
   window, but service support and platform stacking still need checking.
   [Firefox Picture-in-Picture](https://support.mozilla.org/en-US/kb/about-picture-picture-firefox)
5. A user-managed compositor rule or extension could alter local behavior. Ordinary
   KDE Keep Above is already the relevant class of request, not a guaranteed fix
   for ActiveLayer precedence. Version-specific higher-layer policies would be a
   separate desktop customization, outside portable Java and outside this pass.

Repeated `toFront`, toggling topmost on a timer, forcing focus or making Model
Popout fullscreen does not establish a universal solution. Using Java's own
fullscreen-exclusive API would change our window, not grant access to another
app's surface, and conflicts with per-pixel translucency requirements. No native
stacking hooks, special window types, compositor scripts, graphics injection or
protected-video workarounds were implemented or proposed as plugin defaults.

## YouTube/Netflix verification to perform separately

There is no source-backed basis to claim that all YouTube or Netflix fullscreen
sessions behave identically. Browser UI fullscreen, video fullscreen, GPU/driver,
protected playback and presentation paths must be tested on the actual system;
this research does not establish a Netflix-specific overlay prohibition or a
service-specific fix.

On Windows 10/11 (physical and VM separately), KDE X11 and KDE Wayland where
available: record OS/Plasma/JDK/browser versions, GPU/VM driver, monitor and scale.
Enable Always on Top, open the main viewer and an inspection, then test windowed,
maximized, browser fullscreen and video fullscreen with actual playback. Focus the
video again after positioning the viewer, let browser overlays fade, then exit and
reenter fullscreen. Record whether the viewer stays visible without stealing focus.
Repeat on a second monitor. Test a borderless game and genuine exclusive game
separately where available; do not infer one from the other. Check ordinary
Always on Top again after exiting fullscreen.

No fullscreen visibility result is inferred from the native regression suite.
The suite checks existing capability handling and ordinary window stacking only.
