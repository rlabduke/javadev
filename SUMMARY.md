# Summary of changes

## 2026-07-29

Read difference density from MTZ files. `MtzVertexSource` now detects every recognized
F/PHI coefficient pair in a file instead of stopping at the first, so a Refmac or Phenix
MTZ yields both the 2Fo-Fc and the Fo-Fc map. Opening an MTZ prompts for which sets to
compute and gives each one its own map window with the right contour presets.
