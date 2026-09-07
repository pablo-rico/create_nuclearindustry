"""Shared colour identity for Create: Nuclear Industry.

The whole texture set is built from these ramps so every block/item reads as part of
the same machine family. Ramps go dark -> light; index 0 is always the outline colour.
"""

from pixelkit import Ramp, hexc

# Structural metals -------------------------------------------------------------------
# Cool industrial steel, close in value to Create's andesite/brass casings.
STEEL = Ramp("#353b38", "#4d5650", "#68736b", "#849087", "#9ca79b", "#b5bfb0", "#d0d7c8")
# Darker frame / trim metal.
DARK = Ramp("#202629", "#30383a", "#424c4d", "#55605e", "#6c7771", "#859086", "#a4afa1")
# Warm brass, used for kinetic parts (shafts, turbine internals) like Create does.
BRASS = Ramp("#613d2b", "#865235", "#a97645", "#c89b58", "#e8bd6c", "#ffda87", "#fff0b4")
# Copper for heat exchange piping.
COPPER = Ramp("#623b2e", "#864c37", "#a65e43", "#bd7555", "#d38d69", "#e7a887", "#f5c6a5")
# Lead / graphite grey for shielding and control rods.
LEAD = Ramp("#292e31", "#40484b", "#586164", "#727b7c", "#909a98", "#adb7b1", "#ccd3c9")
# Cryogenic white-blue for the fusion tier.
CRYO = Ramp("#38494a", "#526e6d", "#74928c", "#9bb3a8", "#c0d0bc", "#dee6d1", "#f0f2df")
# Concrete for launch pads.
CONCRETE = Ramp("#383b36", "#50564c", "#697063", "#838a7b", "#9da391", "#b7bdaa", "#d0d5c2")

# Energy / signal accents ---------------------------------------------------------------
URANIUM = Ramp("#334329", "#506333", "#6f8542", "#8fa44e", "#adc36b", "#cfde90", "#eaf0b9")
RADIUM = Ramp("#203c28", "#355a36", "#4d8042", "#70a453", "#99ca6e", "#c4e793", "#e7f9c1")
CYAN = Ramp("#203b3d", "#315b5c", "#477d7c", "#62a09b", "#8ac2b7", "#b3dfcf", "#dbf2df")
PLASMA = Ramp("#583323", "#814829", "#ad6835", "#d29146", "#edb65e", "#ffda87", "#fff0c0")
WARN = Ramp("#574023", "#7b5b2c", "#a48038", "#cda84d", "#e5c567", "#f3df92", "#fff0bd")
DANGER = Ramp("#4c2928", "#733b35", "#9a5144", "#ba6754", "#d88168", "#eea18a", "#ffd0af")
TRITIUM = Ramp("#433344", "#654966", "#876487", "#aa80a1", "#cba0b9", "#e6c4d0", "#f6e5e7")
LITHIUM = Ramp("#354047", "#515f67", "#6d7e84", "#8c9e9f", "#adbfba", "#cddbd0", "#eaf0df")

# Stone hosts for ores -------------------------------------------------------------------
STONE = Ramp("#4c4e49", "#5b5e57", "#6c7067", "#7d8177", "#8e9387", "#a0a599", "#b2b8aa")

# Flat helpers ---------------------------------------------------------------------------
BLACK = hexc("#000000")
SHADOW = hexc("#05060a")
WHITE = hexc("#ffffff")

# Port colour convention used across every machine tier:
#   input  -> cyan  |  output -> plasma orange
IN_ACCENT = CYAN
OUT_ACCENT = PLASMA
