"""Screen layouts of the missile area, in texture pixels: the launch controller (``SILO_*``) and the command table
(``HOLO_*``).

These mirror ``missile/gui/SiloLayout.java`` and ``missile/gui/HoloLayout.java`` name for name: a rectangle is
``(x, y, w, h)``, a list of positions a tuple. ``LayoutMirrorTest`` (JUnit) reads this file and fails when the two
disagree, so change both together and never hard-code these numbers in a texture function.
"""

# ======================================================================================
# launch controller: launch_controller_{rocket,launch,widgets}.png
# ======================================================================================

SILO_WIDTH = 336
SILO_HEIGHT = 256

SILO_TITLE = (6, 4, 324, 13)
SILO_TAB_ROCKET = (6, 20, 64, 14)
SILO_TAB_LAUNCH = (72, 20, 64, 14)
SILO_STATUS = (140, 20, 190, 14)
SILO_DIAGRAM = (6, 38, 92, 212)

SILO_STAGES = (102, 38, 228, 44)
SILO_SECTION = (102, 86, 228, 64)
SILO_SECTION_STATS = (228, 98, 98, 48)
SILO_ADVICE = (102, 153, 228, 13)
SILO_INVENTORY = (134, 169, 164, 78)

SILO_TARGET = (102, 38, 228, 58)
SILO_BOX_X = (110, 52, 64, 16)
SILO_BOX_Y = (180, 52, 64, 16)
SILO_BOX_Z = (250, 52, 64, 16)
SILO_SET = (110, 74, 100, 16)
SILO_AIRBURST = (216, 74, 106, 16)
SILO_READOUT = (102, 100, 228, 64)
SILO_COUNTDOWN = (102, 168, 110, 40)
SILO_ARM = (216, 168, 114, 18)
SILO_DISARM = (216, 190, 114, 18)
SILO_REDSTONE = (102, 212, 228, 38)

SILO_STAGE_HEADER = 8
SILO_STAGE_ROW = 12
SILO_C_STAGE = 3
SILO_C_TANKS = 26
SILO_C_TWR = 56
SILO_C_DV = 96
SILO_C_BURN = 140
SILO_C_REACH = 180
SILO_SECTION_TITLE = 11

# Item positions of the section's slots (frame one pixel further out, 18 px): charge x3, secondary x4, tamper x2, bomb.
SILO_SLOT_X = (110, 128, 146, 110, 128, 146, 164, 190, 208, 190)
SILO_SLOT_Y = (100, 100, 100, 122, 122, 122, 122, 122, 122, 100)
SILO_INV_X = 135
SILO_INV_Y = 170
SILO_HOTBAR_Y = 228

SILO_CELL_W = 18
SILO_CELL_H = 10
SILO_COL_CX = 52
SILO_BASE_H = 8
SILO_BASE_W = 54
SILO_BRACKET_X = 9
SILO_BRACKET_W = 4
SILO_LABEL_X = 66
SILO_CELLS = 18

SILO_WIDGETS_SIZE = 256
SILO_CELL_NORMAL = 0
SILO_CELL_SELECTED = 1
SILO_CELL_ERROR = 2
SILO_CELL_GHOST = 3
SILO_SPRITE_CELLS = (0, 0, 72, 40)
SILO_SPRITE_BASE_PLATFORM = (0, 40, 54, 8)
SILO_SPRITE_BASE_SILO = (0, 48, 54, 8)
SILO_SPRITE_GHOSTS = (0, 64, 128, 16)
SILO_GHOST_CORE = 0
SILO_GHOST_RADIOLOGICAL = 1
SILO_GHOST_TNT = 2
SILO_GHOST_LITHIUM = 3
SILO_GHOST_TAMPER = 4
SILO_GHOST_BOMB = 5
SILO_GHOST_PROPELLANT = 6
SILO_GHOST_TRACKER = 7
SILO_SPRITE_TAB_IDLE = (0, 80, 64, 14)
SILO_SPRITE_TAB_ACTIVE = (64, 80, 64, 14)
SILO_SPRITE_BUTTONS = (0, 96, 114, 72)
SILO_SPRITE_TOGGLES = (128, 96, 106, 32)
SILO_SPRITE_LAMPS = (0, 170, 60, 10)


def silo_base_y():
    """The top of the base under the column (SiloLayout.baseY)."""
    x, y, w, h = SILO_DIAGRAM
    return y + h - 4 - SILO_BASE_H


def silo_cell_y(i):
    """The top of the i-th cell up the column, 0 the bottom one (SiloLayout.cellY)."""
    return silo_base_y() - (i + 1) * SILO_CELL_H


def silo_cell_x():
    return SILO_COL_CX - SILO_CELL_W // 2


# ======================================================================================
# command table: holo_table_{screen,widgets}.png
# ======================================================================================

HOLO_WIDTH = 400
HOLO_HEIGHT = 256

HOLO_TITLE = (6, 4, 388, 13)
HOLO_MAP_FRAME = (6, 20, 212, 212)
HOLO_MAP = (12, 26, 200, 200)
HOLO_ZOOM_OUT = (6, 236, 15, 15)
HOLO_ZOOM_IN = (23, 236, 15, 15)
HOLO_AUTO = (40, 236, 30, 15)
HOLO_DESIGNATE = (72, 236, 70, 15)
HOLO_AIRBURST = (144, 236, 74, 15)
HOLO_TAB_SILOS = (224, 20, 54, 14)
HOLO_TAB_RADARS = (280, 20, 54, 14)
HOLO_TAB_TRACKS = (336, 20, 58, 14)
HOLO_LIST = (224, 36, 170, 155)
HOLO_DETAIL = (224, 194, 170, 24)
HOLO_SALVO_MODE = (224, 220, 84, 13)
HOLO_SPACING_DOWN = (310, 220, 13, 13)
HOLO_SPACING = (325, 220, 44, 13)
HOLO_SPACING_UP = (371, 220, 13, 13)
HOLO_ARM = (224, 236, 40, 15)
HOLO_DISARM = (266, 236, 40, 15)
HOLO_SALVO = (308, 236, 44, 15)
HOLO_ABORT = (354, 236, 40, 15)

HOLO_LIST_HEADER = 11
HOLO_LIST_ROW = 9
HOLO_LIST_ROWS = 16
HOLO_COL_CHECK = 2
HOLO_COL_NAME = 12
HOLO_COL_STATE = 50
HOLO_COL_INFO = 104
HOLO_COL_DELAY = 138
HOLO_COL_UNLINK = 160

HOLO_WIDGETS_W = 256
HOLO_WIDGETS_H = 128
HOLO_SPRITE_LAMPS = (0, 0, 48, 7)
HOLO_SPRITE_CHECKS = (0, 8, 16, 8)
HOLO_SPRITE_UNLINK = (0, 16, 16, 8)
HOLO_SPRITE_BUTTONS = (0, 32, 40, 63)
HOLO_SPRITE_TABS = (64, 0, 58, 30)
