"""Shared colour identity for Create: Nuclear Industry.

The whole texture set is built from these ramps so every block/item reads as part of
the same machine family. Ramps go dark -> light; index 0 is always the outline colour.
"""

from pixelkit import Ramp, hexc

# Structural metals -------------------------------------------------------------------
# Cool industrial steel, close in value to Create's andesite/brass casings.
STEEL = Ramp("#0c0e11", "#1b2028", "#2a313b", "#3b444f", "#4e5865", "#65707e", "#828d9b")
# Darker frame / trim metal.
DARK = Ramp("#07080a", "#101216", "#181c21", "#22262d", "#2d323a", "#3a404a", "#4a5159")
# Warm brass, used for kinetic parts (shafts, turbine internals) like Create does.
BRASS = Ramp("#1a1206", "#33240c", "#4f3a13", "#71551d", "#95742a", "#bd9840", "#e0c070")
# Copper for heat exchange piping.
COPPER = Ramp("#1c0e07", "#37190c", "#5a2c14", "#82441f", "#a85f30", "#c9814c", "#e6a877")
# Lead / graphite grey for shielding and control rods.
LEAD = Ramp("#0a0a0c", "#161719", "#212327", "#2e3135", "#3d4146", "#4e5359", "#646a71")
# Cryogenic white-blue for the fusion tier.
CRYO = Ramp("#080f17", "#122031", "#1d3550", "#2a4d72", "#3d6d99", "#5f95c0", "#9ecbe6")
# Concrete for launch pads.
CONCRETE = Ramp("#101010", "#1e1e1f", "#2c2d2e", "#3d3e40", "#505153", "#666769", "#7f8083")

# Energy / signal accents ---------------------------------------------------------------
URANIUM = Ramp("#04140a", "#0a2a13", "#11441f", "#186531", "#238a45", "#37b45f", "#65e086")
RADIUM = Ramp("#062012", "#0b3a1f", "#12602f", "#1a8a43", "#28bb5b", "#4ce87e", "#9dffbd")
CYAN = Ramp("#03151d", "#062a3c", "#0a4560", "#0e6489", "#158cb8", "#2eb8e0", "#87e9ff")
PLASMA = Ramp("#200704", "#40130a", "#6c2611", "#9c3f14", "#cc6a1c", "#f0a02e", "#ffd98a")
WARN = Ramp("#1e1503", "#3c2c06", "#66490b", "#957012", "#c39a1b", "#e8c034", "#ffe783")
DANGER = Ramp("#1c0505", "#380a0a", "#5e1414", "#8c1f1f", "#bc3030", "#e05555", "#ff9494")
TRITIUM = Ramp("#1c0714", "#360c26", "#5c1440", "#8a1f60", "#bb3086", "#e055ad", "#ff9bd6")
LITHIUM = Ramp("#0d0f13", "#1a1e25", "#292f38", "#3c444f", "#525c69", "#6f7b89", "#98a5b2")

# Stone hosts for ores -------------------------------------------------------------------
STONE = Ramp("#4a4a4a", "#585858", "#666666", "#727272", "#7e7e7e", "#8a8a8a", "#969696")

# Flat helpers ---------------------------------------------------------------------------
BLACK = hexc("#000000")
SHADOW = hexc("#05060a")
WHITE = hexc("#ffffff")

# Port colour convention used across every machine tier:
#   input  -> cyan  |  output -> plasma orange
IN_ACCENT = CYAN
OUT_ACCENT = PLASMA
