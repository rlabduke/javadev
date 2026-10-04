# Summary of changes

## 2026-10-04

The version KiNG reports at runtime now comes from the git tag. `version.props` is
jarred verbatim and drives Help | About and the self-update check, but nothing wrote
`-Dversion` into it, so rc9's installers were named rc9 while the app said rc8.

Fixed the plugin jars being absent from the classpath in all jpackage installers, which
hid every chiropraxis, extratools and rdcvis tool from the menus. jpackage's launcher
uses `-jar`, so the JVM ignores `app.classpath` in KiNG.cfg and reads only king.jar's
`Class-Path` manifest attribute; the four plugin jars are now listed there.
Also: the Linux installer now ships probe and suitename, and the CI build no longer
masks a failed plugin-jar build or a failed natives download.

## 2026-07-29

Read difference density from MTZ files. `MtzVertexSource` now detects every recognized
F/PHI coefficient pair in a file instead of stopping at the first, so a Refmac or Phenix
MTZ yields both the 2Fo-Fc and the Fo-Fc map. Opening an MTZ prompts for which sets to
compute and gives each one its own map window with the right contour presets.
