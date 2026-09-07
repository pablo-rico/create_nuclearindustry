"""Fusion hardware: insulated titanium panels, copper coils and recessed fittings."""

import math
from pixelkit import Tex, Ramp, fbm, bevel

TITANIUM = Ramp("#39484c", "#4c6065", "#63797e", "#7e9397", "#98adb0", "#b2c4c5", "#cdd9d8")
GRAPHITE = Ramp("#272e31", "#384247", "#4b575c", "#627177", "#7d8e93", "#9babad", "#b9c7c6")
COPPER = Ramp("#50382d", "#70503c", "#8d684c", "#aa835f", "#c39f76", "#d8ba94", "#e7d4b5")
INLET = (93, 153, 157, 255)
OUTLET = (179, 130, 79, 255)


def panel():
    t = Tex(seed=140)
    for y in range(16):
        for x in range(16):
            grain = fbm(x, y, 140, 16, 16, 2, 6.0)
            t.set(x, y, TITANIUM[3 if grain < 0.56 else 4])
    t.hline(0, 15, 0, TITANIUM[2])
    t.hline(0, 15, 1, TITANIUM[5])
    t.vline(0, 2, 14, TITANIUM[2])
    t.vline(15, 1, 14, TITANIUM[1])
    t.hline(0, 15, 15, TITANIUM[1])
    # Two broad insulated sheets share a thin horizontal compression joint.
    t.hline(1, 14, 8, TITANIUM[2])
    t.hline(1, 14, 9, TITANIUM[4])
    for x, y in ((3, 3), (12, 12)):
        t.set(x, y, TITANIUM[1])
        t.set(x + 1, y, TITANIUM[2])
        t.set(x, y + 1, TITANIUM[5])
    t.hline(8, 10, 4, TITANIUM[4])
    t.hline(4, 6, 12, TITANIUM[4])
    return t


def recess(t, box=(4, 4, 11, 11)):
    x0, y0, x1, y1 = box
    t.rect(x0, y0, x1, y1, GRAPHITE[1])
    bevel(t, x0, y0, x1, y1, TITANIUM, hi=TITANIUM[0], lo=TITANIUM[5])


def port(output=False, turbine=False):
    t = panel()
    recess(t)
    t.disc(8, 8, 3, GRAPHITE[3])
    t.disc(8, 8, 2, GRAPHITE[0])
    t.set(6, 6, TITANIUM[5])
    color = OUTLET if output else INLET
    t.hline(6, 9, 12, color)
    t.set(8 if output else 7, 11 if output else 13, color)
    if turbine:
        for y in (5, 7, 9):
            t.set(2, y, GRAPHITE[1])
            t.set(13, y, GRAPHITE[1])
    return t


def electromagnet():
    t = panel()
    recess(t, (3, 3, 12, 12))
    for x in range(4, 12):
        t.vline(x, 4, 11, COPPER[4 if x % 2 == 0 else 1])
    for y in (5, 10):
        t.hline(3, 12, y, GRAPHITE[2])
        t.hline(3, 12, y + 1, TITANIUM[3])
    return t


def magnet_input():
    t = electromagnet()
    t.rect(5, 5, 10, 10, TITANIUM[2])
    bevel(t, 5, 5, 10, 10, TITANIUM)
    t.rect(6, 6, 9, 9, GRAPHITE[0])
    t.hline(6, 9, 6, GRAPHITE[3])
    t.rect(7, 7, 8, 8, COPPER[3])
    return t


def accelerator(corner=False):
    t = panel()
    # Solid vacuum pipe with a dark weld, no exposed glowing beam.
    for y, color in ((5, GRAPHITE[1]), (6, TITANIUM[5]), (7, TITANIUM[4]),
                     (8, TITANIUM[3]), (9, TITANIUM[2]), (10, GRAPHITE[1])):
        t.hline(0, 10 if corner else 15, y, color)
    if corner:
        for x, color in ((5, GRAPHITE[1]), (6, TITANIUM[5]), (7, TITANIUM[4]),
                         (8, TITANIUM[3]), (9, TITANIUM[2]), (10, GRAPHITE[1])):
            t.vline(x, 6, 15, color)
        t.hline(5, 10, 12, GRAPHITE[2])
        t.hline(5, 10, 13, TITANIUM[5])
    else:
        t.vline(11, 5, 10, GRAPHITE[2])
        t.vline(12, 5, 10, TITANIUM[5])
    t.vline(2, 5, 10, GRAPHITE[2])
    t.vline(3, 5, 10, TITANIUM[5])
    return t


def injector():
    t = panel()
    recess(t, (4, 3, 11, 10))
    for x in (5, 9):
        t.vline(x, 4, 9, GRAPHITE[3])
        t.vline(x + 1, 4, 9, GRAPHITE[0])
    t.hline(5, 10, 5, TITANIUM[4])
    t.hline(5, 10, 8, TITANIUM[3])
    t.rect(7, 11, 8, 12, COPPER[2])
    t.set(7, 11, COPPER[4])
    t.set(11, 12, INLET)
    return t


def controller(state):
    t = panel()
    recess(t, (3, 3, 12, 10))
    lit = state != "off"
    # Two compact analogue gauges remain legible without a luminous screen.
    for x in (4, 9):
        t.rect(x, 4, x + 2, 7, TITANIUM[6] if lit else TITANIUM[3])
        t.set(x + 1, 4, GRAPHITE[1])
        t.set(x + 1, 6, COPPER[1])
        t.set(x + (2 if lit else 0), 5 if lit else 7, COPPER[1])
    color = {"off": GRAPHITE[2], "on": INLET, "ignited": OUTLET}[state]
    t.hline(5, 10, 9, color)
    if state == "ignited":
        t.set(7, 8, OUTLET)
        t.set(8, 8, OUTLET)
    t.hline(4, 6, 12, GRAPHITE[1])
    t.set(4, 11, TITANIUM[5])
    t.set(10, 12, color)
    return t


def turbine():
    t = panel()
    recess(t, (3, 3, 12, 12))
    t.disc(8, 8, 4, GRAPHITE[2])
    for angle in range(0, 360, 60):
        a = math.radians(angle)
        t.line(7.5, 7.5, 7.5 + math.cos(a) * 3.5, 7.5 + math.sin(a) * 3.5, TITANIUM[4])
    t.rect(6, 6, 9, 9, GRAPHITE[1])
    t.rect(7, 7, 8, 8, COPPER[3])
    t.set(7, 7, COPPER[5])
    return t


def rotor():
    t = Tex()
    for x in range(16):
        for y in range(16):
            t.set(x, y, GRAPHITE[(2, 3, 4, 3)[x % 4]])
    for y in (1, 13):
        t.hline(0, 15, y, TITANIUM[4])
        t.hline(0, 15, y + 1, TITANIUM[1])
    return t


def blade():
    t = Tex()
    for y in range(16):
        for x in range(16):
            t.set(x, y, TITANIUM[(2, 4, 3, 2)[(x - y) % 4]])
    return t


FUSION_BLOCKS = {
    "fusion_cryostat_casing": panel,
    "fusion_electromagnet": electromagnet,
    "fusion_magnet_input": magnet_input,
    "fusion_accelerator_segment": accelerator,
    "fusion_accelerator_corner": lambda: accelerator(True),
    "fusion_fuel_injector": injector,
    "fusion_plasma_turbine": turbine,
    "fusion_controller_off": lambda: controller("off"),
    "fusion_controller_on": lambda: controller("on"),
    "fusion_controller_ignited": lambda: controller("ignited"),
    "fusion_fluid_port_input": port,
    "fusion_fluid_port_output": lambda: port(True),
    "fusion_turbine_fluid_port_input": lambda: port(turbine=True),
    "fusion_turbine_fluid_port_output": lambda: port(True, True),
    "fusion_turbine_rotor_core": rotor,
    "fusion_turbine_blade": blade,
}
