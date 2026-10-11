# Changelog

## Unreleased: nuclear chain rewrite

Worlds from 1.1.0 load: every item and block keeps its id. See "Upgrading"
below for what changes in existing worlds.

### Uranium and fuel
- New fuel chain: ore, crushing, washing, acid leaching (yellowcake),
  hydrofluorination to uranium hexafluoride, a centrifuge cascade with reflux
  (natural, LEU, HEU, weapons grade), deconversion to uranium dioxide, pellet
  pressing and fuel assembly by sequenced assembly.
- The centrifuge is now a gas centrifuge stage with feed, product and tails
  tanks and real recipes (shown in JEI). Its old scroll dial is gone.
- New sulfuric acid, hydrofluoric acid and one uranium hexafluoride fluid per
  enrichment tier. Uranium dioxide reduces to uranium metal for warheads and
  other mods; any uranium metal fluorinates back into the cascade.
- Ores drop with Fortune and Silk Touch, so ore crushing works.

### Fission reactor
- Only assembled fuel at 3% or more is accepted. Each fuel channel holds one
  assembly; richer fuel lasts longer and is more reactive.
- Spent fuel keeps its enrichment and is reprocessed (PUREX) for plutonium
  with Create machines: crush it, dissolve the fragments in nitric acid
  (uranium comes back as uranyl nitrate one tier lower), centrifuge the
  solution into plutonium nitrate, recovered acid and waste, then make
  plutonium dioxide and metal. Leaner fuel breeds more plutonium.
- The nuclear bomb, the missile launch pad and the CBC nuclear shell accept
  plutonium as well as 90% uranium. Plutonium has a smaller critical mass:
  each piece counts as two units, so 8 plutonium fill a slot like 16
  uranium, and 4 make a Weapons-Grade Plutonium Core (8 for uranium).
- New nitric acid (gunpowder or saltpetre with sulfuric acid). Plutonium is
  far more radioactive than uranium and burns whoever carries it without
  at least half shielding.
- Textured control panel with a manual SCRAM switch.
- MOX fuel: plutonium dioxide compacted with depleted uranium dioxide into
  pellets and assembled like uranium fuel. It burns like 12% uranium, a
  little more reactive and 15% hotter, and control rods are 10% less
  effective on it. Spent MOX reprocesses for less plutonium than went in.
- Reactors now notice shell blocks removed by machines, not only by players.
- Fuel is no longer lost when the structure changes, and neither is the core's
  heat: swapping a shell block no longer cools the reactor instantly.
- New Reactor Redstone Port for the shell: puts out temperature, power, fuel,
  coolant or steam as a 0-15 signal, or the SCRAM signal. Wrench to pick.
- Control rods only move physically. The SCRAM button no longer pushes them
  in; it turns on the SCRAM signal of the Redstone Ports until reset, to be
  wired to the pistons that drive the rods. New Ponder scene shows how.
- Automatic SCRAM on the control panel, on by default at 2800 °C (500-3900,
  +/- 100, Shift for 500). When the core reaches it the SCRAM signal turns
  on and stays latched; Reset only works 200 °C below the threshold.
- Seven Display Link sources for the reactor (temperature, power, fuel,
  control rods, coolant, steam, status), on the controller, Temperature
  Sensors and Redstone Ports. Numeric ones work on nixie tubes.
- The Temperature Sensor now outputs on its panel side, as its tooltip says;
  it used to power the block behind it.
- New Graphite Moderator and Beryllium Reflector core columns. Graphite
  (baked from coal in a heated mixer) beside a Fuel Channel makes it split
  more: a fuel column touching two graphite columns is fully moderated, and a
  fully moderated core is 35% more reactive. Beryllium lining the columns
  that touch the shell bounces neutrons back: a full ring cuts losses by a
  quarter. Both are configurable and have a Ponder scene.
- Xenon poisoning and decay heat. Fission makes iodine-135, which decays into
  xenon-135, a neutron poison: a running core settles at a modest penalty,
  and after power drops xenon keeps rising for a few minutes (the iodine
  pit) before it decays, so a reactor just shut down needs more rods out to
  restart. Fission products also keep heating the core after a SCRAM: about
  7% of the fission heat, fading over a few minutes, so the coolant must
  keep flowing. Both are configurable.
- New readouts for reactivity, xenon and decay heat in the control panel,
  goggles and three new Display Link sources.

### Control room
- Control Room Linker: click any block of a reactor, turbine or cooling tower
  (or any other block, to watch the radiation there), then click a device to
  link it. Devices reach 128 blocks (configurable) in the same dimension.
- Control Room Monitors join side by side into one screen up to 8x8, drawn in
  the world: readings with gauges for the linked machine and, on bigger
  screens, a two-minute graph. Click to change page; comparators read the
  main value.
- Control Desks: sloping consoles that join side by side and round corners
  (inner and outer, like stairs) into one console with one link. Each straight
  desk holds four control modules and each corner two, one on each arm (in
  the back row of an inner corner, the front row of an outer one), fitted by hand and set up with a wrench:
  push buttons (SCRAM/reset, pulse), levers (automatic SCRAM, tokamak fuel,
  on/off), dials (rod target, SCRAM threshold, redstone level), indicator
  lamps (SCRAM, running, no signal, any alarm metric), gauges (any reading)
  and key switches. Any module can put its state out as redstone below the
  desk or on a Create Redstone Link frequency. Rods still move only
  physically: two new Redstone Port modes, Insert rods and Withdraw rods,
  light up until the rods reach the target, to drive the clutch and gearshift
  of the pistons. Reactor Control Panels from earlier builds turn into desks
  with SCRAM, auto SCRAM, rod target and threshold modules.
- The control room also watches tokamaks: plasma temperature, power,
  confinement, instability and fuel on monitors, desks and alarms.
- Nuclear Alarm: rings, lights up and powers every side when the chosen
  reading (reactor temperature, power, xenon, low coolant, SCRAM, turbine or
  cooling tower load, area radiation) passes the threshold set on it with
  Create's value boxes.
- New Display Link source for turbines; reactor sources also work on linked
  monitors and panels. Ponder scene and JEI page for the whole room.

### Fusion
- The flat fusion ring is replaced by a tokamak: a toroidal multiblock of
  radius 4 to 16 around a new central solenoid, with toroidal field coils,
  new breeding blankets, magnet drives, fluid ports and fuel injectors.
- Deuterium and tritium are fluids. Without tritium the plasma runs D-D and
  breeds its own; blankets set the tritium breeding ratio. Coolant carries
  80% of the power out as plasma steam; without it the plasma overheats and
  disrupts.
- Control panel, Display Link source and a new Ponder scene.

### Lithium, deuterium and new machines
- New spodumene ore: lithium via crushing, washing, acid roast and
  electrolysis; lithium-6 from the centrifuge. Beryllium washes out of
  spodumene. Borax only gives boron.
- New electrolyzer: heavy water from water, deuterium from heavy water,
  lithium from its carbonate. Heavy water is finally obtainable.
- Gas cells are filled and emptied with spouts and item drains.

### Turbines
- Steam and plasma turbines are box multiblocks: 3 to 7 blocks wide and
  high, 3 to 16 long, with a rotor column on the axis and the output at
  the centre of one end. Each rotor expands 80 mB/t of steam (40 of plasma
  steam), each port pair passes 320 (160); the smaller limit applies.
- New Reinforced Turbine Glass with connected textures for the faces.
- A formed turbine renders one large spinning rotor and the steam filling
  it, and hums with its load.
- Turbine, cooling tower and tokamak ports no longer invalidate their fluid
  capability every second, which kept resetting Create pipe networks: water
  pumped out of a turbine never reached its tank and the turbine stalled
  with a full condensate tank.
- Turbine casings now craft four at a time. Ports work on every side and
  no longer scan the area on every call.

### Cooling towers
- New hyperboloid Cooling Tower multiblock: base radius 4 to 16, height from
  twice the radius plus 4 up to five times the radius. It condenses steam
  (heavy steam) back into water (heavy water) at base radius x height mB/t,
  with a plume and the sound of falling water. No power.
- New Turbine Exhaust Port: a turbine with one stops condensing inside and
  lets its steam out as exhaust steam, making twice the power from the same
  steam. Only a Cooling Tower turns exhaust back into water (heavy exhaust
  into heavy water); no turbine accepts it, so turbines cannot be chained.
- Optional Cooling Tower Fans in the bottom wall layers add up to 25% each
  at 256 RPM, at most doubling the tower. Display Link source, JEI info and
  a Ponder scene.

### Radioisotope generator
- New RTG: a small kinetic source that turns a pellet's decay heat into a
  steady 16 RPM. Plutonium pellets give 256 SU/RPM and halve every 10
  in-game days; waste pellets (four radioactive waste in a capsule) give 64
  and halve every 2 days. Spent pellets come out below a quarter charge;
  hoppers can feed and empty it. It irradiates its surroundings.

### Radiation
- Blocks now shield: radiation from waste, fallout and reactors is cut by
  every block between the source and the player, per the new block data map
  `radiation_transmission` (reactor casing 35%, lead 20%, iron 50%,
  obsidian 60%, water 85%, other solid blocks 90%).
- Running fission reactors irradiate their surroundings in proportion to
  their neutron level; their own casing stops most of it.
- Armour shielding comes from the item data map `radiation_shielding`, so
  other mods' armour can protect; the hazmat suit still stops 95%.
- Depleted uranium has uses: a Block of Depleted Uranium passes only 10%
  of radiation, and heavy depleted uranium armour (3/8/6/3, toughness 2)
  stops 98% as a full set but slows you by about a sixth. A crusher turns
  the block back into depleted metal.
- With Create Big Cannons: a Depleted Uranium AP Shot, heavier and more
  penetrating than CBC's own armour-piercing shot.
- Dose rates below 0.05/s count as background and let the dose decay.
- Players accumulate a dose from carried material, nearby waste and fallout
  from accidents, with nausea, weakness and radiation sickness. Hazmat suit
  and Geiger counter. Configurable and can be disabled.

### Advancements
- A progression tree of 24 advancements: the fuel cycle from raw uranium to
  MOX, reactor operation (forming, criticality, moderated cores, SCRAM, the
  iodine pit, control rooms, turbines, cooling towers and a hidden meltdown
  challenge), fusion up to tokamak ignition, radiation safety and weapons.
- A new `create_nuclearindustry:nuclear_event` criterion trigger lets data
  packs listen for the same moments.

### Dirty bomb
- New Dirty Bomb: loaded with anything radioactive that is not weapons-grade
  (waste, spent fuel, fragments, RTG pellets) and set off by redstone like the
  nuclear bomb. A small blast with no crater scatters the load: the more
  radioactive it is, the wider the fallout (24 to 128 blocks) and the hotter
  the spot left at ground zero, which fades over a few days.
- The launch pad's warhead slots also take radioactive material. On its own
  it makes a dirty warhead; packed with fissile material it salts a nuclear
  one, for dirtier and wider fallout.
- The bomb's screen title is now translated.

### Anti-radiation medicine
- Potassium Iodide (dried kelp and bone meal mixed hot in water): for 3
  minutes, the radiation taken up is halved.
- Prussian Blue (iron nuggets and blue dye in sulfuric acid): for 30 seconds,
  flushes 15 rad of dose a second, curing radiation sickness. The lifetime
  total is never lowered.
- Both are eaten whatever the hunger, with a 5-second cooldown. Configurable
  in `radiation.iodideAbsorption` and `radiation.purgePerSecond`.

### Ground contamination
- Accidents can leave lasting ground contamination, stored per chunk. It
  decays very slowly (half every ten in-game days by default), irradiates
  players standing on it and is blocked by a roof overhead.
- Contaminated ground shows green dust, thicker with severity. Clients get
  only a one-byte severity per contaminated chunk, when it changes; the dust
  can be turned down or off in the new client config.
- Unloaded chunks catch up on fallout when they load; nothing forces chunks
  to load and nothing ticks.
- `/nuclear contamination` (query, add, set, clear, clear all, expand,
  fallout) and `/nuclear dose` (get, set, reset) for operators.
- Nuclear detonations leave fallout out to 1.5 times the crater radius, and a
  meltdown contaminates 48 blocks around the reactor.
- Heavy fallout turns the surface of lakes and rivers into Contaminated
  Water, a few columns per tick. It irradiates, can be picked up in a bucket
  and pumped, and a centrifuge separates it into clean water and waste.
- Waste barrels now irradiate 1.0/s (was 0.1) and slowly leak into their
  chunk, unless they sit at Y=0 or lower: geologically stored waste neither
  irradiates nor leaks. A Ponder scene shows how to store it.
- Radiation Scrubber: a kinetic block that draws contamination out of the
  ground of its chunk and those around it, the worst first, faster up to
  256 RPM. What it absorbs comes out as Radioactive Waste for funnels or
  hoppers; when full it stops. Has a Ponder scene.
- Dose HUD: rate, current dose with a bar up to radiation sickness and a
  lifetime total that never decays and survives death. It shows while there
  is a rate or a dose, can be moved or hidden in the client config, and is
  only sent to its own player when its rounded values change.

### Pressurized water reactors and accidents
- Pressurized water reactors: put primary coolant (water and boron in a
  mixer) in a fission reactor with a Pressurizer on its lid and it no longer
  boils its water. Its output port gives hot primary coolant, which Create
  pumps carry to a Steam Generator and back cooled into the coolant port.
  Without a pressurizer the reactor boils water exactly as before and its
  ports refuse primary coolant.
- The Pressurizer is a stack of 2 to 4 Pressurizer and Pressurizer Heater
  Port blocks capped by a Relief Valve. Its steam bubble sets the loop's
  pressure (155 bar by default); heaters raise it (automatic, or manual with
  redstone after a wrench click) and spray from the cold leg lowers it.
- The Relief Valve opens at 170 bar and closes at 160, and can stick open.
  A redstone signal shuts its block valve; comparators read it open.
- Real thermal hydraulics: the flow is whatever the pumps move. Each mB carries
  less heat than boiling water, so a pressurized reactor needs more flow. A
  stopped pump lets the core heat up and boil, the pressure rises and the
  relief valve lifts; a broken pipe drains the loop until the core uncovers.
  Steam voids, an uncovered core and hotter water slow the chain reaction.
- Steam Generator: a 3x3 column, 4 to 8 high, of casing around Tube Bundles,
  with a controller and four kinds of port (primary in and out, feedwater in,
  steam out). More tubes move more heat; steam left inside raises its pressure
  until it stops taking heat, and it dries out without feedwater. It makes
  the same steam turbines already use, and control room monitors can link it.
- Primary coolant is radioactive: a bucket or a puddle from a leak irradiates.
- New Display Link sources and goggle lines for the primary pressure, hot and
  cold leg temperatures, subcooling margin and pressurizer level; all of them
  are configurable under `pwr.*`.
- Containment buildings: close any room with full blocks and put a
  Containment Monitor inside. It measures the sealed air (up to 32k blocks)
  and tracks its pressure, temperature, steam, hydrogen and airborne
  radioactivity; an open door, a slab or a hole unseals it. New blocks:
  Containment Concrete (holds 6 bar), Steel Liner (8 bar), Containment
  Penetration (a wall block that carries fluid through along its axis, with a
  1000 mB buffer each way), Passive Autocatalytic Recombiner and Filtered
  Containment Vent. Glass holds 1.05 bar, ordinary blocks 1.5 and closed blast
  doors 10; the strongest layer of a wall counts, and machines standing inside
  are not wall.
- A reactor inside a containment sends it the relief valve's steam, a break's
  water, its cladding's hydrogen and its radioactivity, and reports the
  building's pressure and hydrogen on goggles, monitors and two new Display
  Link sources. Steam condenses on the walls; hydrogen above 4% may burn and
  above 8% always does (unless the air is mostly steam), spiking the pressure
  and burning anyone inside. Recombiners remove it quietly.
- Opening the filtered vent with redstone bleeds the overpressure out and
  lets only 1% of the airborne activity fall out nearby. If the pressure beats
  the weakest wall, it bursts there and everything airborne falls out.
- A realistic accident chain for every fission reactor, direct boiling ones
  too: above 1200 °C the zirconium cladding oxidises, heating the core further
  and making hydrogen; a pressurized vessel held above 250 bar breaks and
  flashes its water into the containment; water pumped onto a core hotter than
  2800 °C, or corium falling into water, makes a steam explosion.
- A meltdown no longer blows up: the fuel rods turn into Corium (at most 16
  blocks) that melts its way down at most 32 blocks and always freezes into
  radioactive Solidified Corium, which leaks into the ground like a waste
  barrel. A Core Catcher floor stops it and makes it spread and freeze; melting
  concrete inside a containment gives off hydrogen. The core's volatile
  activity stays in a sealed containment and falls out without one.
- Every part is configurable under `pwr.containment.*` and
  `pwr.accidents.*`, including `blockDestruction` to keep accidents from
  breaking blocks.
- Run the pressurizer from a control desk: a lever for the backup heaters
  (every heater port on, automatic or not), a lever that opens the spray
  while the cold leg flows, a lamp lit while the relief valve is open, and an
  Automatic pressure setting for the controller module that holds the primary
  pressure at its setpoint with proportional heaters and spray instead of the
  pressurizer's own on/off control. If the desk breaks or unloads, the
  pressurizer goes back to automatic within two seconds. Only reactors with a
  pressurizer answer them; on others the modules stay dark.
- Plant accidents sound the air raid sirens around them: a meltdown, a
  breached vessel, a steam explosion, a burst containment or a hydrogen burn
  sets off every siren within its reach for two minutes.
- A shut blast door in a containment wall keeps the building sealed; opening
  it unseals the building, and the monitor notices right away.

- New Containment Door: a two-block-high steel door for the containment wall.
  Shut, it is wall like the concrete around it and holds 8 bar; open, the
  doorway is a hole and the containment is no longer sealed. Click it or power
  it with redstone and it opens Create-style: it pulls out of its frame and
  slides sideways along the wall, and slides back and seats with a thud when
  it shuts. Open, its edge stays in the doorway, sticking out of the wall:
  click it to shut the door again. Two side by side, hinged on opposite sides, open together. While
  the containment behind it is more than 0.2 bar above the outside it is
  pressure-locked (`pwr.containment.doorInterlock`). It has its own Ponder
  scene and JEI page, and the plant overview scene shows one in its wall.

### Spent fuel
- Spent fuel now has a cooling history: assemblies leave the reactor Hot and
  dated with the in-game day, so those discharged the same day stack. Over
  time they turn Warm (after about 2 days), Cooled (about a week) and Aged
  (about 3 weeks), shown in the tooltip with the days since discharge.
  Cooling is optional: every grade can still be reprocessed.
- Hot fuel gives twice the dose of Warm fuel (three times that of Cooled
  fuel) and burns bare hands like plutonium. Aged fuel is the mildest.
- Spent Fuel Rack: holds nine assemblies at the bottom of a pool, by hand,
  hopper or funnel. Every block of water straight above it cuts its
  radiation (two blocks stop over 97%); fresh fuel under water glows blue.
  Its decay heat goes into the pool: if the pool is too small for the
  racks in it, the surplus boils the water off a layer at a time, with
  steam, until the racks stand uncovered and irradiate everything around.
  Goggles show the water above, the radiation let out, the decay heat and
  the pool's cooling.
- Pool Cooler: a kinetic block placed in the pool water and turned by a
  shaft from above; at 64 RPM it carries away as much heat as 150 blocks of
  water.
- Dry Storage Cask: holds nine assemblies that are Warm or cooler (it refuses
  Hot fuel), lets out only 2% of their radiation, never leaks and blocks
  radiation passing through it. The fuel inside keeps cooling.
- Reprocessing rewards patience: crushing Cooled or Aged spent fuel gives
  more fragments and less waste. Crushing Hot or Warm fuel now leaves some
  Radioactive Waste.
- Comparators read how full a rack or cask is. Recipes for the rack, the
  cooler and the cask.

### Waste management
- Waste now comes in three classes. The old Radioactive Waste keeps its id
  and becomes Intermediate-Level Waste. New Low-Level Waste (mildly active)
  comes from the Radiation Scrubber and from centrifuging contaminated water;
  new High-Level Waste (fierce and warm to carry) comes from PUREX.
- Each class has its treatment: low-level waste compacts with an iron sheet
  into a Compacted Waste Drum, intermediate-level waste is cemented in a mixer
  (waste, sand, gravel, an iron sheet and water) into a Cemented Waste Drum,
  and high-level waste melts with sand and borax into a Vitrified Waste
  Canister in the new Vitrification Melter, which sits on a lit Blaze Burner
  (twice as fast superheated). Funnels and hoppers feed and empty it.
- Waste packages (barrel, drums, canister) leak and irradiate according to
  how well they are packed and buried. A geological repository is scored from
  depth below the surface, host rock around the package (deepslate, tuff,
  basalt...), backfill on its faces (new Bentonite, mixed from clay and borax,
  or other packages) and the waste form, and any water nearby spoils it. From
  a score of 80% a package is stored: no leak, no radiation.
- New Repository Inspector: shows the stored packages within 8 blocks and the
  score breakdown of the weakest one on goggles, feeds a Display Link and puts
  the stored share out to comparators.

### Control room instruments
- Chart Recorder: up to three pens (red, blue, green) draw readings of the
  linked machine on a strip of paper that scrolls across its front, at one
  sample every 1, 5 or 20 ticks. Feed it paper by hand or with a hopper; it
  stops when the paper runs out, and a comparator reads how full the tray
  is. Wrench it to choose the pens and the speed. Sneak-click it with an
  empty hand to tear the chart off as a Chart Roll you can open and read
  later.
- Annunciator Panel: six alarm windows, each linked to its own machine
  (click a window with the Control Room Linker, or the frame to link them
  all) and set up with a wrench: alarm, threshold and label. Windows follow
  the ISA-18.1 sequence: an alarm flashes fast with a horn until
  acknowledged, stays lit while the condition lasts, flashes slowly once it
  clears and goes dark on reset; the first window to trip in a burst is
  framed in white. Press the ACK and RESET buttons on the panel, or pulse
  redstone into its left (ACK) and right (RESET) sides, for example from a
  desk pulse button. The back and bottom give full power while an alarm
  waits for acknowledgement; a comparator counts the lit windows.
- Controller Module for control desks: a PID loop that holds the
  reactor's power or core temperature at a setpoint by moving the rod
  target, in whole rod segments, so the Insert rods and Withdraw rods ports
  drive the pistons. Click it like a dial to change the setpoint; its wrench
  screen sets the setpoint finely and switches it between automatic and
  standby. A SCRAM always wins. Its lamp
  shows when it is driving the rods. Tuning is in the config.
- New alarms for pressurized water reactors, steam generators and
  containments: primary pressure high and low, low subcooling margin, low
  pressurizer level, loss of primary flow, low steam generator level,
  containment pressure and hydrogen. They never sound on a reactor that
  boils its water directly.
- Monitors, gauges and pens show the pressurized loop of a reactor
  (pressure, hot and cold leg, subcooling, pressurizer level, flow, vessel
  level, relief valve, containment pressure, hydrogen) and can be linked to
  steam generators and containments. Their scales fit what a plant really
  does: 250 bar, 256 mB/t of primary flow (two pumps at full speed), 256 mB/t
  of steam and 300 MW per steam generator, and containment airborne activity
  on a logarithmic gauge.
- Pressurizer control modules: backup heaters and spray levers, a relief
  valve lamp, and Automatic pressure for the controller module (see
  "Pressurized water reactors and accidents").
- Annunciator presets: one click sets up all six windows (alarm, threshold
  and label, in the player's language) for a reactor core, a PWR primary
  loop, a steam generator or a containment, with thresholds taken from the
  plant config (between the pressurizer setpoint and the relief valve, twice
  natural circulation, the hydrogen flammability limit and so on).
- New alarms: relief valve open, steam generator level high, low steam
  output and low primary temperature drop, containment not sealed and
  containment airborne activity.
- Chart recorders pick pens that suit what they are linked to: pressure, hot
  leg and pressurizer level for a pressurized reactor, level, steam and
  primary temperature drop for a steam generator, pressure, hydrogen and
  airborne activity for a containment.
- The tokamak in the control room: monitors show its startup phase (or
  "Tripped" and why), fusion gain Q ("∞" once ignited), plasma density,
  helium ash, magnetic field, magnet temperature, chamber vacuum, heating
  power, fuelling rate and liquid helium, most important first so small
  screens keep them. Gauges and chart recorder pens can follow all of these
  (vacuum on a logarithmic scale), and a recorder linked to a tokamak starts
  with fusion power, plasma temperature and density.
- The tokamak's Display Link readout is translated and shows the phase or
  trip, plasma, power and Q, density and ash, field and magnet temperature,
  vacuum, heating, fuelling, helium, instability and TBR.
- New tokamak alarms: poor chamber vacuum and warm magnets (only while they
  matter: plasma lit, magnets energised), low fusion gain (judged only while
  burning), high plasma density, low tritium and a tokamak trip, which lights
  when the plasma went out or a start was refused for any reason but the
  operator's own shutdown.
- Tokamak annunciator preset: instability, density, vacuum, magnet
  temperature, Q below 3 and trip, each lighting before the tokamak's own
  protection acts.
- Tokamak desk modules: Tokamak start and Tokamak shutdown on push buttons or
  levers (a lever acts when thrown up; a refused start says why on the action
  bar and the lever springs back), and a Tokamak heating lever. The old
  Tokamak fuel lever, now called Tokamak on/off, keeps working in existing
  desks.
- Automatic fusion power for the controller module: holds the tokamak's
  fusion power at a setpoint (0-1000 MW, 250 MW to start) by setting its
  fuelling, and only once the fuelling is at its most raises the heating
  above the level the operator left, which it never goes below. It acts on a
  burning plasma only; the startup is the operator's. Tuning is in the
  config.
- Monitor colours for the tokamak follow the plasma model's own thresholds
  instead of copies of them.

### Civil defense
- New Air Raid Siren: it wails while powered by redstone, and on its own (a
  faster attack warning) while a missile is flying towards it, or a bomb or a
  launch pad is counting down, within 96 blocks plus the weapon's fallout
  radius. Goggles show the cause and the seconds to impact. Launch pads warn
  the target before the missile even lifts off.
- New Hand-Crank Siren: purely mechanical, it sounds while its shaft (from
  below) turns at 32 RPM or more, higher-pitched the faster it turns.
- Both sirens give a comparator signal of 15 while sounding and can be heard
  128 blocks away.
- Air raid sirens also give the attack warning for plant accidents within
  their 96 blocks: a meltdown, a breached reactor vessel, a steam explosion, a
  burst containment or a hydrogen burn sounds them for
  `civilDefense.siren.plantAlarmSeconds` (two minutes; 0 turns it off).
- New Seismic Detector: feels every nuclear and dirty detonation in its
  dimension and keeps the last eight (power, distance, compass bearing, time),
  shown with goggles and on a Display Link ("Latest Detonation"). A comparator
  reads the strongest recent blast, fading back to 0 after five minutes.
- New Blast Doors: frame a 2x2 to 4x4 opening of Steel or Lead Blast Door
  leaves with Blast Door Frames and put a Blast Door Controller in the ring.
  The panel slides into the wall towards the controller. Turn the
  controller's shaft one way to open and the other to shut (faster with
  speed), or use redstone: a rising signal opens, a falling one shuts. A
  comparator reads how far open it is.
- Shut blast doors stop blasts (explosion resistance 1500, so even nuclear
  craters leave them, their frame and controller standing) and radiation
  (steel lets 15% through, lead 2%). Open, the doorway lets people, blasts and
  radiation through and the retracted panel survives. If the frame, a leaf or
  the controller breaks, the door slams shut.
- Nuclear detonations now send out a blast wave instead of one huge vanilla
  explosion: a small fireball wrecks ground zero, the crater forms as before,
  and a wave races outwards (8 blocks per tick) up to four crater radii, at
  most 256 blocks. Its overpressure follows the real scaling of blasts with
  distance and yield; it hurts and throws back whoever it reaches, and walls,
  hills and blast doors between you and ground zero soak it up (stone lets
  about a third through per block, obsidian and shut blast doors nothing).
  It also shatters exposed glass, panes and leaves on its way. Dirty bombs
  keep their small conventional blast.
- New Shelter Filtration Unit (kinetic): put it inside a room and it surveys
  the air around it. A closed room of up to 4096 blocks of air whose every
  wall, floor and ceiling block is at least as strong as planks or concrete
  against blasts and shields like a solid block is certified as a shelter
  (doors and trapdoors leave it open: use a blast door). Inside, blast waves
  lose 90% of their overpressure, and while the unit turns at 16 RPM or more
  only 5% of the fallout and ground contamination dose gets in. Goggles show
  the room's size, or the weak or open spot that keeps it from certifying.
- Blast Protection protects against the blast wave.
- All of it is in the config under `civilDefense.*` (reach and range of
  sirens, the crank's minimum speed, the seismic scale, door speeds and
  stress, the blast wave's speed, reach, yield and damage, and the shelter's
  size, wall requirements and filter).

### Ponder: the nuclear power plant
- New Ponder tag "Nuclear Power Plant" in the Ponder index, listing every
  part of a plant: reactor, pressurizer, steam generator, turbine, cooling
  tower, control room, containment, spent fuel storage and waste packages.
- The series opens with "The Whole Plant": reactor, pressurizer, steam
  generator, turbine, cooling tower, containment, control room and spent fuel
  pool all on one site, joined as you would join them. The camera zooms in on
  each connection in turn (primary loop, containment penetrations, steam line,
  turbine shaft, exhaust to the tower, feedwater back through the wall, the
  control room and the pool) and pulls back to show the plant running.
- Four more chained scenes, reachable from the reactor and steam generator
  controllers (all five, in order) and from each part they show:
  - The Pressurized Primary Loop: the pressurizer on the lid with its heaters
    and relief valve, hot and cold leg ports, the steam generator and its
    port roles, the Create pumps that drive the loop, and what a stopped pump
    or a broken pipe does.
  - Steam, Turbine and Cooling Tower: steam to the turbine, the exhaust port,
    the cooling tower and the condensate pumped back as feedwater.
  - Control Room and Containment: desk modules (rod target dial, controller
    module, SCRAM), the Insert and Withdraw rods ports driving the rod
    piston, the annunciator panel and chart recorder, then the containment
    building with penetrations, its weakest wall, the containment monitor,
    recombiners, the filtered vent, and corium stopped by a core catcher.
  - Spent Fuel and Waste: the pool racks, cooling grades and pool cooler, the
    dry cask, PUREX, the three waste classes and their packages, and the
    geological repository score with bentonite and the repository inspector.
- The geological storage scene now explains the repository score (depth,
  host rock, bentonite, waste form, no water nearby) instead of the old
  "below Y=0" rule, and the scrubber scene shows that it makes Low-Level
  Waste.

### Tokamak startup and plasma control
- A tokamak no longer lights by itself. Press **Start** on its screen, send a
  redstone pulse into the controller or flip a control desk lever, and it walks
  through its startup: vacuum pumps evacuate the chamber, liquid helium cools the
  coils to 4.5 K, the field ramps up to what the magnet drives ask for, a
  solenoid pulse lights a puff of deuterium, and ohmic plus auxiliary heating
  bring the plasma to a burn. **Shutdown** pumps the plasma out and ramps the
  field down.
- The plasma now has a density (a share of its limit, which grows with the
  field), a fuel inventory fed at a fuelling rate and D:T mix you set, and
  helium ash that is pumped out on its own and returns as warm helium to the
  coils' helium loop. Q (fusion power over heating power) is shown everywhere:
  an R=4 tokamak with two neutral beam injectors runs at Q 4 to 8 for a small
  net gain; with enough density and confinement it ignites and keeps burning
  with the heating off.
- Magnet drives set the field target and no longer heat the plasma. The
  central solenoid gives ohmic heating while the plasma is cold, for one
  volt-second budget per shot; reaching a burn needs auxiliary heating.
- Losing the vacuum, the liquid helium, the field or the fuel trips the plasma
  safely: it ramps down with a message and the cause on screen, with no
  explosion. Disruptions from overheating, poor confinement or too much
  density work as before.
- The controller screen is new: phase and why it waits or tripped, Start and
  Shutdown, fuelling, D:T and heating setpoints, heating on/off, readouts for
  temperature, density, ash, Q, field, coil temperature, vacuum and powers,
  and tanks for deuterium, tritium, water, plasma steam, liquid and warm helium.
- Coolant ports take only water; heavy water is refused. A pair of coolant and
  plasma steam ports now carries 400 mB/t of plasma steam (was 160), about
  300 MW of fusion power, so one pair runs an R=4 tokamak at its defaults.
- Bred tritium beyond the reserve leaves through injectors set to OUTPUT. When
  it has nowhere to go the blankets stop breeding and the tokamak warns,
  instead of venting it.
- There is no longer a coil coverage that forms quietly and never burns: the
  structure warning is worked out with the plasma model itself.
- Tokamak goggles, chat messages, ports, Display Link errors and the controller
  screen are translated (English and Spanish).
- Ponder: a new "Starting a Tokamak" scene walks through the pumps, cryogenic
  ports, heaters, the Start and the plasma lighting up behind the viewports,
  and "The Cryogenic Plant" shows how to build and feed the plant.

### Cryogenic plant and vacuum
- New Cryogenic Plant multiblock that makes the liquid helium a tokamak's
  superconducting coils need. Build a 3x3 block of Cryogenic Plant Casing 3 to
  5 high, put the Cryogenic Plant Controller in the middle of a side wall
  facing out and right-click it. The plant needs at least one Helium
  Compressor in its walls (shaft side facing out) and Cryogenic Plant Ports;
  the wrench sets each port to Warm helium in, Liquid helium out, Liquid
  nitrogen in or Air intake / liquid air out.
- At 128 RPM a plant liquefies 2 mB/t of helium per layer of height (6 mB/t
  for a 3x3x3 plant, enough for an R=4 tokamak); slower compressors make less
  and below 32 RPM they do nothing. Its compressors share a stress impact of
  10240 SU/RPM per layer, about a quarter of what an R=4 tokamak's plasma
  turbine gives. About 1% of the helium leaks out on every pass, so the loop
  needs topping up from the plasma's helium ash or from liquid air.
- Liquid nitrogen fed to the plant pre-cools it: 50% more liquid helium while
  it lasts, boiling off 1 mB of nitrogen per mB of helium.
- A plant with a Liquid Air port liquefies air instead (5 mB/t per layer).
  Centrifuging 1000 mB of liquid air gives 990 mB of liquid nitrogen and
  10 mB of helium: the first charge for the helium loop.
- Plant ports push into and pull from tanks or ports right next to them, so a
  plant can stand against the tokamak. The controller lights up while it
  runs; frost forms on the casing, cold vapour rolls off the top and the
  compressors hum. Goggles show the plant's size, compressor speed, output
  and tanks, or what keeps it from forming.
- New Tokamak Cryogenic Port for the tokamak shell: in input mode it takes
  liquid helium in for the coils, in output mode it gives the boiled-off warm
  helium back to the plant (the wrench toggles it).
- New Tokamak Vacuum Pump for the tokamak shell (kinetic, 2048 SU/RPM): one
  pump at 128 RPM holds the vacuum of an R=4 tokamak, and every 4 blocks more
  of major radius need another. Goggles show its pumping speed and the
  chamber pressure.
- JEI info pages for the plant, the cryogenic port and the vacuum pump. All
  of it is configurable under `cryo.*`.

### Plasma heating, viewports and sound
- Neutral Beam Injector: a tokamak shell block with a shaft on its outer
  face. At 128 RPM it heats the plasma with 20 MW (nothing at 32 RPM or less,
  rising linearly in between), at a heavy 32768 SU/RPM of stress: a megawatt
  of beam costs about two and a half times what the plasma turbine gets back
  from a megawatt of fusion, so a tokamak needs a gain above about 3 to make
  net energy. The beams use 0.02 mB/t of the tokamak's deuterium per MW, and
  lose power when the tank runs dry. They heat at any plasma density.
- RF Heating Antenna: the cheaper heater, 12 MW at 64 RPM for 13312 SU/RPM
  and no fuel, but the radio waves only couple to a plasma denser than about
  15% of its limit, fully above about 35%.
- Engineer's goggles on either heater show its speed, its power against the
  rated power, the deuterium the beams use, what the plasma is doing and,
  on the antenna, whether the plasma is dense enough to absorb the waves.
- Tokamak Viewport: a quartz window that counts as a shell block. Through it
  you see the plasma, and it is the only light of the hall: dark with no
  plasma, a dim glow at breakdown, then brighter as the plasma heats.
- The plasma is now drawn inside the tokamak: a glowing ring around the
  solenoid that goes from dull red through orange and white to violet as it
  nears the danger temperature, thicker with density, flickering when it
  becomes unstable and flashing magenta at breakdown. It is seen through the
  viewports (or from inside the chamber), even when the controller itself is
  out of sight.
- Frost forms on the outside of the shell while the coils are colder than
  30 K, and steam puffs out of the plasma steam ports while the tokamak makes
  power.
- New sounds: the magnets hum, rising in pitch and loudness with the field;
  the burning plasma roars, wavering when unstable; breakdown strikes with a
  crack; and the cryogenic plant has a compressor sound. They play from the
  part of the ring nearest to you.

### Visible and audible fission reactor
<!-- WP-A -->

### Renewable materials, fan treatments and thorium
<!-- WP-B -->

### Guide: advancements, Ponder and JEI
<!-- WP-C -->

### Performance and multiplayer
- A fission reactor being filled, emptied or run no longer sends everyone
  near it its whole state every tick: contents and readings go out at most
  every half second, without the structure once the clients have it, about a
  tenth of the traffic it used to take. Forming, breaking, a SCRAM, a
  meltdown or a breached vessel still show at once.
- The once-a-second radiation check of each player is lighter: it skips the
  parts of the world with no radioactive blocks at all, finds running
  reactors and other sources through the chunks around the player, and
  reuses each line of sight to a source for up to a second
  (`performance.radiationRayCacheTicks`) while neither moves. Placing or
  breaking a block along it, or opening a door, counts at once.
- Containment monitors measure their building about twice as fast, and
  shelters' filtration units their room too.
- Settings changed from a screen (control desk modules, chart recorders,
  annunciators, the tokamak controller) are refused on the server unless the
  player could use that block by hand right now: alive, not a spectator, not
  in adventure mode, within reach, and where spawn protection and the world
  border allow it.
- The screens of reactors, tokamaks, nuclear bombs, launch pads, control
  modules and control room devices close when their block is broken or
  replaced, or when the player walks a few blocks beyond reach, as chests do.
- `explosive.blastColumnsPerTick` and `explosive.blastBlocksPerTick` now set
  how fast a nuclear crater is dug.
<!-- WP-D -->

### JEI
- Info pages for every new block and item of the plant: pressurizer, steam
  generator, containment, corium, spent fuel storage, the three waste classes
  and their packages, control room instruments and civil defense.
- New Vitrification category, with the melter and the Blaze Burner as
  catalysts and the heat it needs.

### Technical
- Ground contamination and the dose HUD reach clients through NeoForge's
  synced data attachments (a byte per contaminated chunk, a few bytes per
  reading), only when they change; the mod adds no packets of its own.
- Recipes, loot tables, tags, worldgen, models and lang come from datagen.
- GameTests and JUnit tests cover multiblocks, machines, chain recipes,
  physics and radiation.

### Upgrading
- Old fuel assemblies count as 20% fuel; old spent fuel as 5%. Loaded reactor
  fuel converts to 20% assemblies at its previous reactivity.
- Old centrifuges keep their items in the output slots; they can be taken out
  by hand.
- Old flat fusion rings no longer form: the controller says so. Stored D-T
  pellets become deuterium and tritium in the new tanks, and loose pellets can
  be drained for tritium in an item drain.
- A reactor saved in manual SCRAM keeps its rods held in until the SCRAM is
  reset once; after that only physical rods count.
- Three-block turbines no longer form; they keep their steam and
  condensate and ask to be rebuilt as a box.
- The old sulfuric acid reprocessing recipes (`mixing/reprocess_*`) are
  gone; automation built on them must switch to the plutonium chain.
- Waste barrels already placed above Y=0 start leaking into the ground. Set
  `contamination.barrelLeak = 0` to stop that.
- Running reactors in existing worlds now irradiate nearby players. Set
  `radiation.reactorEmissionPerNeutron = 0` to turn that off.
- Every reactor gets the automatic SCRAM on at 2800 °C. Remember it only
  turns on the SCRAM signal: wire it to your rod pistons.
- Reactors, RTGs and fuel saved by this version carry new data (fuel kind,
  SCRAM state); loading them in an older version is not supported.
- `fusion.deuteriumTritiumBurnRate` changed meaning (see
  `docs/configuration.md`); an existing config keeps its old value.
- Existing reactors keep boiling water as before; nothing changes
  until a pressurizer and primary coolant are added.
- Meltdowns in existing worlds no longer make the power 80
  explosion; they leave corium and a release scaled to the core. Set
  `pwr.accidents.legacyMeltdown = true` to get the old meltdown back. Reactors
  running hotter than 1200 °C now oxidise their cladding, which adds heat.
- Spent fuel from older worlds has no history and counts as Warm,
  with the same dose as before; once carried or stored it gets a history and
  goes on cooling. Hot and Warm spent fuel now gives a chance of
  Intermediate-Level Waste when crushed.
- Waste barrels below Y=0 that are not in a proper repository (no
  bentonite, open caves, water nearby) now leak slightly and irradiate a
  little. Set `waste.repository.legacyDepthRule = true` to keep the old
  "below Y=0 is stored" rule.
- PUREX now gives High-Level Waste and the scrubber Low-Level
  Waste. Barrel compacting still takes intermediate- and high-level waste;
  low-level waste goes into its own drum. A scrubber holding old waste waits
  until it is emptied.
- Nuclear detonations no longer blow up terrain with a vanilla
  explosion reaching up to about 100 blocks; beyond the crater and the small
  fireball the blast wave hurts people and breaks glass and leaves instead.
  Set `civilDefense.blastWave.breakFragileBlocks = false` to keep every block
  outside the crater and fireball intact.
- Tokamaks still form, but to run they now need vacuum pumps, a pair of
  cryogenic ports fed with liquid helium from a Cryogenic Plant, and a Start
  (screen, redstone pulse or desk lever). A tokamak that was burning when the
  world is loaded comes back offline with a message. Magnet drives no longer
  heat the plasma: add neutral beam injectors or RF antennas.
- Heavy water is no longer accepted as tokamak coolant; heavy water already in
  a tokamak's tank is used up as water.
- The `fusion.*` config keys keep their names; new ones (startup, vacuum,
  magnets, plasma), `cryo.*` and `plasmaHeating.*` are documented in
  `docs/configuration.md`.

## 1.1.0

- Added configuration for steam/plasma turbine capacity, boron absorption,
  uranium consumption and combined deuterium-tritium pellet consumption.
- Increased default plasma turbine capacity to 65536 per port, twice the
  fission turbine's 32768. Existing configurations retain their values.
- Removed sample configuration options and their startup messages.
- Added centrifuge support for common uranium ingot tags and external items
  named `uranium` or `uranium_ingot`, including JEI input variants.
- Reworked built-in block, item and fluid artwork. Fission uses steel panels;
  fusion uses insulated titanium panels and dedicated turbine rotor textures.
- Ore textures now use vanilla rock substrates: deepslate for uranium and stone
  for borax and the existing thorium texture.
- Updated release metadata, Minecraft compatibility and NeoForge dependency
  declarations, including the optional Create Big Cannons integration.

See `docs/server-update.md` for updating an existing server from 1.0.0.
