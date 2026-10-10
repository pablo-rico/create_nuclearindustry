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
<!-- pwr: filled by its work package -->

### Spent fuel
<!-- spentfuel: filled by its work package -->

### Waste management
<!-- waste: filled by its work package -->

### Control room instruments
<!-- controlroom2: filled by its work package -->

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
- All of it is in the config under `civilDefense.*` (reach and range of
  sirens, the crank's minimum speed, the seismic scale, door speeds and stress).

### Ponder: the nuclear power plant
<!-- plantponder: filled by its work package -->

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
