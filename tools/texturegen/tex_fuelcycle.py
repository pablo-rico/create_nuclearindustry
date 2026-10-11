"""Textures for the renewable fuel cycle: rock fines, heavy mineral sand, adsorbents and the thorium fuel.

Register block textures with ``@block``, item textures with ``@item`` and screen backgrounds
with ``@gui``; this module is imported at the end of ``blocks.py``.
"""

from blocks import CUTOUT_BLOCKS, block  # noqa: F401
from gui import gui  # noqa: F401
from items import item, solid  # noqa: F401
from palette import CONCRETE, DARK, LEAD, RADIUM, STEEL, URANIUM, WARN

# A rough heap, the placeholder shape of every item until it gets its own art.
HEAP = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......####......",
    "....########....",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "................",
    "................",
]


def _placeholder(ramp, seed):
    return solid(HEAP, ramp, seed=seed)


_ITEMS = {
    "granite_fines": (CONCRETE, 911),
    "tuff_fines": (STEEL, 912),
    "deepslate_fines": (DARK, 913),
    "heavy_mineral_sand": (WARN, 914),
    "amidoxime_adsorbent": (STEEL, 915),
    "loaded_adsorbent": (URANIUM, 916),
    "radioactive_sludge": (RADIUM, 917),
    "thorium_dioxide": (LEAD, 918),
    "irradiated_thorium": (RADIUM, 919),
    "irradiated_depleted_uranium": (URANIUM, 920),
    "uranium_233_dioxide": (WARN, 921),
    "thorium_fuel_pellet": (LEAD, 922),
    "thorium_reactor_fuel": (LEAD, 923),
    "depleted_thorium_reactor_fuel": (DARK, 924),
    "thorium_spent_fragments": (DARK, 925),
}

for _name, (_ramp, _seed) in _ITEMS.items():
    item(_name)(lambda ramp=_ramp, seed=_seed: _placeholder(ramp, seed))
