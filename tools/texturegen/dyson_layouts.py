"""Screen layouts of the Dyson swarm, in texture pixels: the EM rail ejector (``EJECTOR_*``) and the swarm terminal
(``TERMINAL_*``).

These mirror ``dyson/gui/EjectorLayout.java`` and ``dyson/gui/TerminalLayout.java`` name for name: a rectangle is
``(x, y, w, h)``. ``DysonLayoutTest`` (JUnit) reads this file and fails when the two disagree, so change both together and
never hard-code these numbers in a texture function.
"""

# ======================================================================================
# EM rail ejector: em_rail_ejector_screen.png (256x256 sheet)
# ======================================================================================

EJECTOR_WIDTH = 176
EJECTOR_HEIGHT = 186
EJECTOR_SHEET = 256

EJECTOR_TITLE = (6, 4, 164, 12)
EJECTOR_SAILS = (7, 17, 144, 36)
EJECTOR_CHARGE = (153, 17, 16, 36)
EJECTOR_ORBIT_0 = (7, 57, 38, 14)
EJECTOR_ORBIT_1 = (48, 57, 38, 14)
EJECTOR_ORBIT_2 = (89, 57, 38, 14)
EJECTOR_ORBIT_3 = (130, 57, 38, 14)
EJECTOR_STATUS = (7, 74, 162, 24)
EJECTOR_INVENTORY = (7, 101, 162, 77)

EJECTOR_SAIL_X = 8
EJECTOR_SAIL_Y = 18
EJECTOR_INV_X = 8
EJECTOR_INV_Y = 102
EJECTOR_HOTBAR_Y = 160

EJECTOR_SPRITE_ORBIT = (180, 0, 38, 42)
EJECTOR_SPRITE_CHARGE = (220, 0, 16, 36)

# ======================================================================================
# swarm terminal: swarm_terminal_screen.png (256x256 sheet)
# ======================================================================================

TERMINAL_WIDTH = 256
TERMINAL_HEIGHT = 182
TERMINAL_SHEET = 256

TERMINAL_TITLE = (6, 4, 244, 12)
TERMINAL_SKY = (6, 18, 124, 124)
TERMINAL_TAB_0 = (134, 18, 28, 14)
TERMINAL_TAB_1 = (164, 18, 28, 14)
TERMINAL_TAB_2 = (194, 18, 28, 14)
TERMINAL_TAB_3 = (224, 18, 28, 14)
TERMINAL_PANEL = (134, 34, 118, 108)
TERMINAL_SWARM = (6, 146, 246, 32)

TERMINAL_ENABLE = (138, 38, 110, 14)
TERMINAL_RADIUS = (138, 56, 110, 14)
TERMINAL_INCLINATION = (138, 74, 110, 14)
TERMINAL_NODE = (138, 92, 110, 14)
TERMINAL_ORBIT_STATS = (138, 110, 110, 28)

TERMINAL_SKY_CX = 68
TERMINAL_SKY_CY = 80
TERMINAL_SKY_RADIUS = 60

TERMINAL_SPRITE_TAB = (0, 188, 28, 28)
TERMINAL_SPRITE_TOGGLE = (30, 188, 110, 28)
TERMINAL_SPRITE_SLIDER = (142, 188, 110, 14)
TERMINAL_SPRITE_KNOB = (142, 204, 6, 14)
