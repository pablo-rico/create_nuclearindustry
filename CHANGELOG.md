# Changelog

## Unreleased

### Turbines
- Steam and plasma turbines are now compact Create-style jet engines: always **3x3 across** and still 3–16 blocks long.
  Once formed, the walls, ports and glass thin down to plates with brass trim, and the front shows a brass intake ring
  with a fan that spins with the shaft.
- Turbines wider than 3x3 no longer form; the controller says so. Rebuild them at 3x3 (keep the same length and ports).
  Each interior block now holds 9 times as much working fluid, so the tank matches a 5x5 turbine of the same length.

### Satellites
- New **Rocket Satellite Fairing**: top a rocket with it instead of a warhead, with an **Early Warning Satellite** or a
  **Communications Satellite** inside (both made by sequenced assembly). Its launch controller is ready without a target
  and launches only on a command table's order: three full solid stages or two liquid ones reach a low orbit, three stages
  with liquid a geostationary one; its console shows the rocket's delta-v against what each orbit needs. The
  launch vehicle climbs, drops its stages and lets the satellite go high above.
- Low satellites fly straight and come back in over the far side of the world border, so over time they pass over the
  whole world; they lose altitude every day and burn up. Geostationary ones stay over the place you chose.
- New **Satellite Ground Station**: a dish driven by a shaft from below that takes your satellites' network (a link
  code, like radars) and points at the one in view. Link up to two to a Holographic Command Table: it passes the network
  on to its launch controllers. Goggles and Display Links show the network, its satellites and the next pass.
- Early warning satellites put launches under them on their network's radar picture, beyond any radar's horizon.
  Communications satellites relay radars and table links past their ranges and narrow the spread of missiles whose
  launch site and target they both see.
- The command table has a **Satellites** tab to launch into either orbit, deorbit your own satellites and send an
  interceptor silo at foreign ones your radars have catalogued (low orbit only, never automatically). Satellites passing
  near show on its hologram, and low ones cross the night sky.
- A launch ends with the fairing opening and the satellite drifting away high above; a satellite shot down breaks up in
  a bright flash and a cloud of glittering debris, and one that decays burns up as a fireball across the sky. Satellites
  in orbit show as moving points of light in the night sky (client setting `rocket.satellites`).
- New advancements "Beep, Beep" (put a satellite in orbit) and "Kessler Syndrome" (shoot one down). Settings under
  `missile.orbit.*`.

### Launch site art
- Every block of the launch site has new models and textures, modelled on Cold War rockets and pads. Stacked rocket
  sections now make one continuous fuselage, with joint rings only where two kinds of section meet; first-stage engines
  show their nozzle and fins, upper stages an open interstage round a vacuum nozzle.
- Sections show what they hold: a tank looks different with a solid cartridge or with liquids, and frosts up as liquid
  oxygen goes in; a warhead's reentry vehicle shows whether it is conventional, fission, dirty or thermonuclear. A rocket
  keeps these looks in flight.
- A launch controller now forms its 3×3 base into one launch table, with a flame grate under the rocket and hold-down
  clamps that close on a rocket standing ready and swing back as it lifts off.
- The Launch Controller is a rack cabinet that faces whoever places it, with status lamps (green ready, amber armed,
  red when it cannot launch) and a generator turning in its plinth. **It is now driven by a shaft from below**: in
  existing worlds, move its shaft under it.
- The Silo Fuel Port has fuel and liquid-oxygen valves on the face turned to whoever places it.

### Fixes
- The link code boxes of the missile radar, the interceptor battery and the ground station screens take typing again:
  clicking them focused the box but keys never reached it.
- A launch controller topped by a satellite fairing no longer shows arm keys, coordinate boxes and a redstone hint that
  do nothing: it shows its satellite network, a checklist of what the launch still needs and how to launch it from the
  table.
- A command table whose ground station has no code, or is not turning, now says so on its Satellites tab instead of
  "No ground station".
- Designating a new target for an armed launch controller (from the command table or with a target designator) no
  longer quietly disarms it: it stays armed if the rocket reaches the new target, so the next redstone edge launches
  it. A countdown under way goes back to armed and waits for a fresh edge; a target out of reach stands it down.

## 2.0.0 (beta): nuclear chain rewrite

Released as a beta: back up your world before updating. Worlds from 1.1.0 load: every item and block keeps its id. See "Upgrading"
below for what changes in existing worlds.

### Mod icon
- The mod now has its own icon in the mods list: a brass cogwheel around a
  glowing core with a radiation trefoil.

### Missile defence
- New **Interceptor Battery**: placed whole as a 2×2×2, driven by a shaft from
  below. Load its four tubes with **Interceptor Missiles** (made by sequenced
  assembly) by hand, funnel or mechanical arm. Give it the link code of your
  radars and switch it to automatic: it turns its canister and fires at
  missiles predicted to land in its defended zone, one at a time or two at once.
  Redstone holds its automatic fire, and a comparator reads its ready rounds.
- Top a rocket with the new **Rocket Kill Vehicle** instead of a warhead to make
  an interceptor silo. It stands ready with no target, ignores redstone, and
  launches only when a command table or automatic defence orders it. It meets
  missiles far away, above the air, and flies once.
- The command table has a new **Defence** tab, and the Tactical Linker now
  links up to eight batteries to it. Engage a tracked missile from a chosen or
  the best launcher, run the table's own automatic defence (it also covers
  batteries without a link code), set its doctrine and zone, cease fire, or
  self-destruct one of your interceptors. The hologram shows interceptors on
  their way and their chance to kill.
- Interceptors fly to meet their target and destroy it with a chance that is
  best head-on, well within reach and with several radars tracking. A
  destroyed missile breaks up in the air in a flash and falling wreckage; its
  warhead never goes off or contaminates. A miss self-destructs. Interceptors
  never show on radar.
- Batteries show their state, rounds and mode on goggles and Display Links, the
  launch controller's Display Link source now has a name, and shooting down a
  missile earns the "Not Today" advancement. Settings are under
  `missile.defense.*`.

### Missiles: launch platforms, console and command table
- A launch controller can use a 3×3 launch base with walls left open. Existing
  concrete silos still work, and fuel ports can feed tanks from beside or
  beneath the base.
- The launch controller opens a cutaway console showing the rocket's stages,
  tank fill, reach and flight advice. Select a section to load its warhead bay
  with capped core and secondary loads, conventional TNT, or a whole bomb.
  Conventional TNT warheads now launch too, and breaking or moving a bomb keeps
  its contents.
- The tactical command table can link up to 16 silos. Arm them remotely, set a
  salvo for time-on-target or a sequence, and abort a missile flight that
  belongs to one of the linked controllers.
- The Tactical Linker links radars and launch controllers to command tables and
  radar scopes; the Control Room Linker no longer links missile devices (saved
  links stay). The
  controller is now called the **Launch Controller**, and **Silo Concrete** is
  the renamed silo wall block.
- Breaking change: a rocket loaded with lithium and more than one core now
  says "A thermonuclear warhead needs exactly 1 core as its primary" and will
  not launch. Leave exactly one core as the primary to use its thermonuclear
  secondary again. Once a warhead holds lithium or a tamper, the console and
  right-clicking only let one core in, and the console's advice line says how
  many cores to take out.
- Rocket warheads moved to typed slots: old warhead sections keep their cores,
  lithium and tampers. A bomb broken by hand drops with what it holds.
- The new Ponder scene "Launch Platform and Redstone Launch" shows a whole
  launch, including that a new redstone signal (a lever or button) fires an
  armed controller and a signal already on does not.

### Safe shutdown and loss of cooling
- A fission reactor can now really be shut down. With at least as many
  inserted control rod blocks as fuel rod blocks, any core stays subcritical
  even cold; rods are no longer capped at one bank, so cores with more than two
  rods per three fuel blocks get stronger rods (cores with fewer behave as
  before). The desk's automatic controller scales its gains to match.
- Rods come out like at a real plant: while the core is deep below critical
  they withdraw as fast as they are pulled, so starting up no longer takes
  minutes of creeping reactivity; only the last 1000 pcm before critical are
  freed at the rod drives' 100 pcm/s, so pulling every rod at once still never
  takes the core prompt critical (`fission.kinetics.freeWithdrawalMargin`, 0
  for the old behaviour).
- New **Boric Acid** (Create mixing: 1000 mB water, a borax salt and 100 mB
  sulfuric acid). Pumped into a reactor, its boron holds the core down from the
  water (5 pcm per ppm). It stays when the water boils; only bleeding takes it
  out, which is how you dilute it for a restart. A pressurized reactor takes it
  (and plain makeup water, which dilutes it) as primary coolant; heavy-water
  cores refuse it.
- **Feed and bleed**: boiling never takes a core below 100 °C, so a scrammed
  core with pumps only replacing its steam stays hot. Cold water fed in beyond
  the steam now carries heat away, and once the chain reaction is off the
  output ports bleed the coolant tank (never below the water that covers the
  core) as new **Hot Water** or **Hot Heavy Water**, or as Contaminated Water
  from a damaged core. Send it to a cooling tower, which turns it back into
  water; input ports refuse it. A pressurized reactor's letdown also leaves as
  hot water, so it goes through a tower and comes back as boron-diluting makeup
  instead of circulating; makeup water now cools its loop below boiling (its
  pressurizer level and pressure drop, lighting PZR LO and PRESS LO: normal for
  a cold, depressurized loop). The output port shows two tanks, so a pipe full
  of bled water keeps bleeding while some steam is about.
- Shutdown states: Subcritical (off, but it will restart as it cools or its
  xenon decays), Hot shutdown (held off with margin) and Cold shutdown (margin,
  fuel and water under 100 °C, core under water). The Cold Shutdown advancement
  now needs a real cold shutdown.
- Goggles show a Shutdown section (state, cold margin, boron, rods against
  fuel, heat carried away against decay heat), the boron term of the
  reactivity, and warnings for decay heat not carried away, an uncovered core
  and contaminated water being bled; output ports say whether they bleed.
  Goggles on a reactor's fluid ports now find their reactor (they used to
  say "No reactor" on clients) and show its tanks.
- New Display Link sources: shutdown state, cold shutdown margin, boron, core
  under water, heat removed / decay heat, rods in / fuel rods ("12 / 12") and
  building hydrogen; the reactivity breakdown gains a boron term ("B-2850").
  Monitors get Shutdown margin and Boron rows, gauges and LED displays the
  cold shutdown margin and dissolved boron, desks a Cold shutdown lamp.
- New alarms: cold shutdown margin low, cold shutdown, core level low and decay
  heat not carried away, and a Shutdown annunciator preset (SCRAM, SDM LO,
  DECAY HEAT, CORE LEVEL LO, H2 HI, COLD SHUTDOWN). The hydrogen alarm on a
  reactor also reads the building around it.
- **Hydrogen in the reactor building (Fukushima)**: a reactor without a sealed
  containment, standing in a closed room of ordinary blocks, now keeps the
  hydrogen of its overheated cladding under the roof instead of losing it to
  the sky. Steam from leaks dilutes it, recombiners in the room remove it and
  the building slowly leaks it away; in open air it disperses as before. From
  4% it may ignite and at 8% it always does: ordinary walls hold only 1.5 bar,
  so the blast blows the roof off, sounds the sirens, logs an INES 3 incident
  (INES 5 with damaged cladding), burns anyone inside and lets some of the
  damaged fuel's activity fall out around the plant. A meltdown sets off a
  flammable mix; one inerted by steam does not explode, and steam alone above
  1.5 bar only gives way at the weakest wall. Configured under
  `pwr.building.*` (`hydrogen`, `maxVolume` 4096, `leakRate` 0.0005,
  `maxExplosionPower` 8.0, `explosionRelease` 0.02).
- Hydrogen is now visible: pale, bluish wisps of gas gather and hover under the
  roof of a reactor building or a sealed containment holding it, from a few
  wisps at 1% to a thick cloud once it is sure to ignite (8%). Goggles on the
  reactor warn about hydrogen in the building.
- New advancements: Liquid Control Rods (boric acid into a reactor) and the
  secret It Wasn't the Reactor (a building's hydrogen explosion).
- Two new Ponder scenes in the power plant series, Safe Shutdown and Loss of
  Cooling, and JEI pages on safe shutdown and building hydrogen.
- Config: `fission.reactivity.maxRodAbsorption`, `fission.thermal.feedwaterHeatCapacity`,
  `fission.boron.*`, `fission.coolant.bleed`, `fission.coolant.contaminatedDamage`
  and `fission.shutdown.requiredMargin` (see docs/configuration.md).

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
- Temperature matters again. The cladding now fails at 1500 °C instead of
  2000 (`fission.damage.claddingFailureTemperature`; the fuel still melts at
  2800), so a core at rated power (about 885 °C) has roughly twice its rated
  power of margin before it damages itself. The Warning, Critical and Melting
  states, the over-temperature alarm and the fuel stress reading all follow the
  config thresholds (1500 / 2150 / 2800 °C). The automatic SCRAM now defaults to
  1400 °C, just under the damage, and the annunciator preset's CORE TEMP HI and
  HI-HI windows sit at 1000 and 1200 °C, and new Nuclear Alarms and the desk's
  automatic temperature controller start at 1400 and 880 °C. Saved reactors
  still on the old 2800 °C default move to the new one on load (a threshold you
  set is kept), and a saved config keeps 2000 °C until you change it. A plant
  that used to run between 1500 and 2000 °C will start damaging its fuel: pull
  its rods back a step.
- A steam-bound core answers its rods. The thermal power can never exceed the
  steam that leaves (exchangers, ports, steam tank space), and the core's water
  used to soak up any extra reactivity by heating far past boiling, so pulling
  more rods changed nothing. The water can now only be driven
  `fission.thermal.maxSuperheat` (50 K) above boiling: past that it flashes to
  steam (the first thousand pcm or so go into that void) and the heat stays in
  the fuel, which then climbs about 500 °C per 1000 pcm of extra rod withdrawal
  until it is damaged. With the steam path shut
  altogether the flashing water stops the chain reaction within seconds, but the
  decay heat then goes into fuel that cannot cool: free the outlet.
- The steam void in the core follows its target with a one-second lag, so a
  turbine draining the steam tank in fits no longer saws the reactor's power.
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
- The net reactivity is shown split into its parts, in pcm with their signs,
  so the numbers explain themselves: fuel (with graphite, reflector and
  heavy water), rods, heat (Doppler and water temperature), steam and
  missing water, and xenon. The goggles and the control panel show the line,
  control room monitors get the four values, and a new Display Link source
  "Reactor Reactivity Breakdown" prints it. A cold reactor with its rods in
  reads for example "fuel +12000, rods -22000": that -10000 is its shutdown
  margin, not a fault.
- New reactor physics. The core follows real point kinetics with delayed
  neutrons, and hot fuel, hot water and steam all hold its power back:
  pulling a control rod out raises the power a little and the reactor
  settles by itself on a new, hotter power instead of running away to a
  meltdown. A steady reactor reads a net reactivity of about 0 pcm; a shut
  down one reads its (negative) shutdown margin.
- As in a real reactor, fresh fuel holds far more reactivity than its own
  heat can take back, so a reactor needs its control rods: with every rod
  out, or none built, a core with water runs past its rated power until the
  fuel is damaged. Starving it of water only holds it back while its fuel is
  uncovered: when the water comes back, so does all that reactivity. Small
  reactors with few rod blocks move in big steps; tall or several rod
  channels give finer control.
- Decay heat follows the real curve, from fission products that decay in
  seconds to ones that last for hours, and builds up with how long the core
  ran: a reactor that ran for hours and loses its water after a SCRAM keeps
  heating for hours.
- Power is proportional to the fuel: each fuel rod block is rated 50 MW (MOX
  more). The control panel, goggles, monitors, gauges, alarms, the Redstone
  Port (15 at 100%) and the automatic power controller of the desk now work
  in percent of that rated power, so a big and a small reactor handle alike.
- Steam out is exactly the heat the water carries off. If the steam cannot
  leave (full tank, too few ports or heat exchangers), the water in the core
  heats up and the power drops by itself, gradually. A core with no water
  stops its chain reaction, but its decay heat keeps heating it.
- Withdrawing rods frees their reactivity at most at 100 pcm per second (the
  speed of the rod drives), so a piston jerking a rod out can no longer take
  the core prompt critical; inserting them is still immediate.
- New meltdown model: above 2000 °C the cladding fails and the fuel takes
  permanent damage (shown in the panel and goggles, and it releases
  activity); above 2800 °C the fuel melts and the damage runs fast; at 75%
  damage the core melts down into corium. The old thermal excursion that
  latched above 3000 °C is gone. Refuelling with fresh fuel repairs damage in
  proportion.
- Cladding oxidation is measured at the cladding, between the fuel and its
  water, and adds at most about 600 K.
- The control panel shows fuel and coolant temperature, power as a share of
  the rated power, net reactivity in pcm and fuel damage; the goggles add the
  same. New Display Link sources for power (% of rated), net reactivity,
  coolant temperature and fuel damage, a new Fuel Damage alarm and monitor
  rows for coolant temperature and damage.
- Multiblocks no longer post to chat when they form, break or are
  incomplete. Clicking a controller shows its status above the hotbar for
  the player who clicked; only serious events (SCRAM, fuel damage, fuel
  melting, meltdown, steam explosion, vessel breach, a hot reactor broken
  open, tokamak disruptions and trips, accelerator trips) go to chat, to the
  owner and to players within 64 blocks.
- A formed reactor now notices when a control rod channel is rebuilt: rods
  swapped for a Beryllium Reflector or Graphite Moderator column, or pulled
  out, under a closed casing lid stop counting as a channel (the core loses
  that rod capacity and gains the reflection or moderation) without cooling
  or unforming it, and putting a rod back makes it a channel again. Rods
  driven by Create pistons, with the piston pole in the lid or gaps above
  and below them, keep their channel and never make the reactor recheck
  itself. A reflector or moderator put into a channel whose lid is open (rods
  on a piston) stops the reactor with a message to cap that column with
  casing; capped, it forms again with the new column. A reactor saved with
  such a leftover channel, or whose core was changed while unloaded, gets the
  right rod capacity when it loads.

### Control room
- Control Room Linker: click any block of a reactor, turbine or cooling tower
  (or any other block, to watch the radiation there), then click a device to
  link it. Devices reach 128 blocks (configurable) in the same dimension.
- Control Room Monitors join side by side into one screen up to 8x8, drawn in
  the world: readings with gauges for the linked machine and, on bigger
  screens, a two-minute graph. Click to change page; comparators read the
  main value.
- Right-click a monitor with a wrench to set its screen up, from any block
  of it: a title of your own instead of the machine's name, the layout
  (automatic as before, readings only, graph only, or readings and graph
  together on any screen of two blocks or more), what the graph's two lines
  plot (any gauge reading of the linked machine) and which readings it lists,
  in which order, each one shown or hidden. A single block pages through the
  readings you chose. A screen that grows keeps its setup; screens nobody set
  up look exactly as before.
- Control Desks: sloping consoles that join side by side and round corners
  (inner and outer, like stairs) into one console with one link. Each straight
  desk holds four control modules and each corner two, one on each arm (in
  the back row of an inner corner, the front row of an outer one), fitted by hand and set up with a wrench
  (sneak-wrenching a module takes just that module out; anywhere else on the desk it picks the desk up):
  push buttons (SCRAM/reset, pulse), levers (automatic SCRAM, tokamak fuel,
  on/off), dials (rod target, SCRAM threshold, redstone level), indicator
  lamps (SCRAM, running, no signal, any alarm metric), gauges (any reading)
  and key switches. Any module can put its state out as redstone below the
  desk or on a Create Redstone Link frequency. Rods still move only
  physically: two new Redstone Port modes, Insert rods and Withdraw rods,
  light up until the rods reach the target, to drive the clutch and gearshift
  of the pistons. Reactor Control Panels from earlier builds turn into desks
  with SCRAM, auto SCRAM, rod target and threshold modules.
- New setup screen for desk modules (wrench a module). The header names the
  module and the machine its console is linked to, with that machine's status,
  and warns in red when the chosen function does nothing there. Functions are
  a list, each with a description on hover; the ones the linked machine cannot
  use are greyed out and say what they need (a tokamak, a reactor with a
  pressurizer...). Alarm lamps and gauges pick from lists of only the alarms
  and readings that machine has, each showing its live value (an alarm that
  would light as set shows red). Controllers get an automatic/standby switch,
  a description and their setpoint with what they are asking for now. Scroll
  the threshold, setpoint or redstone level (5 a notch, 1 with Shift); the
  redstone output is three buttons; the Redstone Link frequency slots and a
  meter of the module's live signal sit beside the inventory. A console that
  is not linked yet still lists everything.
- The control room also watches tokamaks: plasma temperature, power,
  confinement, instability and fuel on monitors, desks and alarms.
- Nuclear Alarm: rings, lights up and powers every side when the chosen
  reading (reactor temperature, power, xenon, low coolant, SCRAM, turbine or
  cooling tower load, area radiation) passes the threshold set on it with
  Create's value boxes.
- Fixed: the Nuclear Alarm's reading selector on its top face never showed up
  and was not saved, so an alarm always watched reactor temperature after a
  reload. It now shows up next to the threshold and keeps its choice.
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
  the centre of one end. Each rotor expands 40 mB/t of steam (20 of plasma
  steam), each port pair passes 160 (80); the smaller limit applies.
- New Reinforced Turbine Glass with connected textures for the faces.
- A formed turbine renders one large spinning rotor and the steam filling
  it, and hums with its load.
- The steam turbine output and the plasma turbine look like the tokamak's
  heaters: a bolted frame round a recess where a half shaft turns with the
  network, formed or not, and a rotor hub on the face inside the turbine.
- Turbine, cooling tower and tokamak ports no longer invalidate their fluid
  capability every second, which kept resetting Create pipe networks: water
  pumped out of a turbine never reached its tank and the turbine stalled
  with a full condensate tank.
- Plasma steam pumped from a tokamak, and condensate pumped out of a
  turbine, no longer stop for good after the tokamak or the turbine
  re-forms (a disruption, a broken and replaced block, a click on the
  controller). The pipes stayed full and the pump kept turning, but nothing
  flowed until the world was reloaded.
- Every mB of steam now carries twice the heat, so reactors need half the
  pumping: a boiling reactor's steam carries 4 MW per mB/t (heavy water 6.2),
  a tokamak makes 0.8 mB/t of plasma steam per MW, and one Create pump at
  256 RPM (128 mB/t) feeds or empties about 512 MW of a fission reactor.
  Turbines expand 40 mB/t per rotor and 160 per port pair (20 and 80 for
  plasma steam) for the same power as before, and cooling towers condense
  half their base radius times their height in mB/t, so a fission or fusion
  plant makes the same rotational power from each MW. Heat exchangers keep
  their 160 MW. A pressurized water reactor's steam already carried about
  this much heat, so it now makes about twice the rotational power per MW,
  close to a boiling reactor's.
- Turbine casings now craft four at a time. Ports work on every side and
  no longer scan the area on every call.

### Placing machine parts
- Reactor, turbine, tokamak and cooling tower ports, the reactor temperature
  sensor, the tokamak's fuel injector and magnet drive, and the turbine
  outputs now face the player who places them, not away from the block that
  was clicked: a port set into a wall from outside faces out, with no block
  needed behind it. Sneak while placing to face it the other way.

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

### Commands
- `/nuclear spawn <structure> [pos] [fuelled]` places a machine already formed
  (for testing and creative builds): a fission reactor (`fuelled` loads it and
  fills its water), steam and plasma turbines, a cooling tower, a tokamak with
  a creative motor on its magnet drive, a linac with its machines, a steam
  generator, a cryogenic plant and a containment room. The space it needs is
  cleared first and the reply says whether it formed and, if not, why.

### Performance and multiplayer
- Radiation exposure is much cheaper on servers: a player who stands still no
  longer recasts every radiation ray every second. Rays are reused while they
  are still being read and nothing changes along them, and are recast after a
  minute at most (`performance.radiationRayCacheTicks` now counts from the last
  use and defaults to 100; new `performance.radiationRayMaxAgeTicks`, 1200).
  Control room monitors watching an area reuse their rays the same way.
- Nuclear craters and water turned into contaminated water now update radiation
  readings, containment buildings, shelters and multiblock structures as they
  are dug: a wall blown away stops shielding at once and a breached containment
  notices within a few ticks.
- Fallout sources are looked up by chunk, fade continuously instead of in
  one-second steps, and no longer mark the world data for saving every second.
- Fission reactors send much smaller client updates: the fuel goes out as a
  summary instead of one entry per assembly and the goggle readings are packed,
  so a full 9x12x9 core costs no more bandwidth than a small one. A running
  reactor updates nearby players at most once a second and only when something
  visible changed (tank level in 2% steps, 2 °C, 0.5% power, 10 pcm, rods, fuel
  slots, state, period, decay heat, the reactivity breakdown), with a refresh
  every `performance.forcedSyncTicks` (100) regardless; SCRAM, meltdown and
  formation still go out instantly. In creative, ctrl+pick-block on a running
  controller now places a copy whose fuel is averaged over the assemblies.
- Block changes near multiblocks are cheaper: structure bounds are remembered
  when a controller is indexed, chunk unloads only look at that chunk's
  controllers, and an unformed accelerator listens 32 blocks around its
  controller (plus the bounds of its last formed structure) instead of 160; an
  unformed tokamak only where its torus can be, and an unformed cooling tower
  only behind its controller (less than half the old area) and, once its floor
  is laid, only on that floor and the walls it can carry. Clicking the
  controller still checks the whole structure.
- Control room devices watching an area (monitors, alarms, annunciators) keep
  the whole radiation reading of that spot for one second, like a player's,
  unless a block changes nearby, instead of scanning the blocks and sources
  around it on every sample.
- Control room monitors send the graph history with the chunk and afterwards
  only the new samples; chart recorders send at most one update per second
  (`performance.screenSyncTicks`, 20), so at fast paper speeds the trace moves
  forward about once a second. Screens and consoles work out their shape only
  when a block next to them changes. Alarms and annunciators on one machine
  share a sample; different machines check on different ticks.
- Fluid ports, dual fluid pipes and containment penetrations remember the tanks
  and pipes around them and their controller instead of looking all six sides
  up every tick (fluid moves exactly as before), and a dual pipe changing the
  fluid it shows no longer sends block updates to its neighbours.
- Steam generator, cryogenic plant, tokamak, accelerator and containment monitor
  controllers update players only when something they show changes visibly and
  at least every `performance.forcedSyncTicks`; the accelerator sends its beam
  path only when it forms. Tokamaks, accelerators and cryogenic plants look up
  their parts once a second, not every tick, and the breakeven advancement is
  checked every 5 seconds.
- Spent fuel racks in one pool share one measurement of the water. A sealed
  containment is not re-measured while none of its blocks changed (a safety
  re-check every 4 minutes); an open Containment Monitor backs off to every 2
  minutes. Shelter Filtration Units and the Repository Inspector survey less
  often when nothing changed; the Vitrification Melter looks its recipe up only
  when its contents or heat change, and its progress advances smoothly in the
  goggles.
- Nuclear blast waves only walk the ring of columns they sweep each tick (same
  results).
- Fixed two players opening different Nuclear Bombs or Launch Pads at the same
  time seeing the wrong block's screen.
- Missiles remember the chunk they keep loaded, so a world closed mid-flight no
  longer leaves it force-loaded forever. Missiles from older saves only take
  over a chunk their save proves they forced; chunks lost under older versions
  can be freed with `/forceload remove`.
- Fixed several internal registries (multiblocks, radiation emitters and ray
  caches, containment, seismic network, craters being dug, delayed explosion
  sounds, repository site cache) keeping a closed world in memory.

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
  it makes a dirty warhead; packed with weapons-grade cores it salts a nuclear
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
- The Vitrification Melter (bricks, a blast furnace, iron and copper sheets)
  and the Repository Inspector (Geiger counter, precision mechanism, electron
  tube, bentonite and iron sheets) can now be crafted, so the "sealed
  repository" advancement is reachable in survival.

### Radioactive waste transport
- New Radioactive Waste Cistern: a Create fluid tank in a lead and concrete
  shell that connects the same way (up to 3x3, 8 buckets a block, no window)
  and only takes radioactive fluids: spent fuel solution, plutonium and
  uranium-233 nitrate, thorium fuel solution, contaminated water and depleted
  hexafluoride. Water or anything else is refused, even by pumps.
- New Shielded Waste Cask: a vault for solid waste that joins along its axis
  like Create's Item Vault, 9 slots a block. It takes waste of every class,
  sludge, waste packages, spent fragments, depleted fuel and RTG pellets and
  trinitite, but never Hot spent fuel.
- Sealed, both let only 0.2% of their cargo's radiation out. Goggles show the
  cargo's activity, what gets out and whether the shell is intact.
- Both ride Create trains and other contraptions with their cargo, and keep
  irradiating while moving. Portable Storage and Fluid Interfaces and train
  schedule cargo conditions see what they carry, but can only load waste.
- A derailment or crash, an explosion that reaches them (nuclear ones too) or
  breaking the block breaches them: the waste contaminates the ground in
  proportion to its activity, a cistern loses half its fluid, and a breached
  container irradiates far more and keeps leaking, even on a moving train,
  until two uses of a Maintenance Kit (by hand or from a Deployer) patch it.
- Crafted from Create's fluid tank or item vault with containment concrete and
  iron sheets. Ponder scene and JEI pages for both.

### Control room instruments
- Advanced Redstone Link: works like Create's Redstone Link but is tuned by a
  code of four letters or digits, typed in after clicking it, instead of two
  items. Transmitters send the strongest signal around them to every receiver
  with the same code within Create's link range; sneak-click with an empty
  hand to switch between transmitting and receiving. The code is shown on the
  link itself and on the control room panel.
- Control modules have a fourth redstone output, Advanced Redstone Link: type
  the code in the link column of their setup screen.
- The panel: looking at a fission reactor controller, a tokamak controller, a
  control module, a console or an Advanced Redstone Link slides a readout in
  on the right of the screen, no goggles needed. A reactor shows its state,
  core temperature, power, rods, fuel, stress, tanks, pressurized loop and
  anything going wrong; a tokamak its phase, fusion power and gain, the
  plasma, the magnets and the vacuum; sneak to see their structure, ports,
  reactivity, fuel and cooling instead. A module shows what it is set to, its
  value in units (gauges too), where its redstone goes and its signal. It
  replaces the goggle tooltips of both reactor controllers and the floating
  label over modules, and can be turned off with `hud.panel` in the client
  config.
- Control Table: a flat-topped console for four modules in two rows, its top
  level with the rail of a Control Desk, with raised end cheeks. Tables side by side join, and a table
  next to a desk or wall panel shares its console and link.
- Wall Control Panel: a plate for four modules hung on the side of a solid
  block. Panels side by side join, panels on the same wall (beside or above
  each other) make one console, and a panel falls off with its modules when
  its wall goes. A gauge on a wall panel shows its dial face on, its needle
  turning in the panel.
- LED Display Module: shows a reading in seven-segment digits with its unit
  (°C, MW, bar, %...). LED displays side by side in a row join into one wider
  display, across joined desks, tables or wall panels too (up to eight
  modules), with more digits and a label on top; the leftmost module sets it
  up, and wrenching any of its modules opens that setup. Too big a value reads
  OL, a reading the machine lacks reads dashes.
- Guarded Button Module: a SCRAM, tokamak shutdown or pulse button under a
  hinged cover. The first click lifts the cover, the second presses; a
  sneaking click closes it, and it falls shut on its own after five seconds.
- Bar Graph Module: a reading as a column of twenty LEDs, green, amber in the
  15% under the alarm level and red from it. The alarm level (90% by default)
  is set in the module's screen.
- Trend Recorder Module: a reading drawn as a glowing line over its last 48
  samples, one every 1, 5 or 20 seconds (about a minute, four or sixteen
  minutes on screen). Samples taken without a reading leave a gap.
- Core Map Module: a fission reactor's interior from above, north at the top:
  fuel columns green to red as their assemblies burn, control channels filling
  blue as their rods go in, moderator, reflector and heat exchanger columns.
- Plasma Section Module: a cut through a tokamak with its plasma, coloured by
  the startup phase, brighter the hotter it burns, bigger the denser it is,
  shaking as it grows unstable, with field lines for the magnetic field and
  the coils lit while they carry current. The frame turns red after a trip.
- Four Core Maps or four Plasma Sections filling one desk, table or wall panel
  make one big screen, set up from any of them.
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
- Servo Gearbox: an encased shaft that puts Mechanical Pistons and Rope Pulleys
  where a redstone signal says. The signal is a share of the stroke set on its
  side (1-64 blocks, cut to the piston's poles or the pulley's rope), and the
  other side chooses whether a stronger signal extends or retracts. It stops the
  actuator exactly on the block, through any shafts, gearboxes or gearshifts,
  follows a signal that changes on the way, says so when something blocks it,
  and a comparator reads where the actuator stands. A desk dial set to the
  control rod target drives the rod pistons with one wire, no clutch or
  gearshift.
- New Ponder series "Control Room", with its own tag in the Ponder index, set
  in a full main control room laid out like a real plant's: instrument boards
  with annunciators, recorders and a big screen on the back wall, the
  operators' desks set back from it and wrapping round them, the shift
  supervisor's tables behind. Four scenes cover linking and consoles, every
  control module, the screens and instruments, and the redstone that leaves
  the room. It replaces the old single control room scene.
- Control room screens have styles, chosen with the wrench. Besides the
  standard screen there are two new ones, each laying the same readings and
  graph out its own way. *Process computer* looks like a 1980s plant computer
  page: a header band with the machine's tag and the time, boxed panels, a
  point-log table with dotted leaders, segmented bar graphs and WRN/ALM flags
  (ALM blinks), a trend with axes, and a footer with the state, the alarm count
  and the page. *Mimic board* looks like a 1970s hard-wired board: an engraved
  nameplate, the state in an annunciator window, a grid of instruments whose
  nameplates light amber on a warning and flash red on an alarm, digital and
  edgewise meters, and a strip-chart recorder. When the bigger instruments
  don't all fit, the screen pages through them on click as usual.
- Screen graphs can plot a third line, chosen with the wrench ("None" by
  default), on its own scale. Choosing it only restarts that line; the other
  two keep their history. The control room Ponder scene shows the styles and
  the third line.
- Fixed: in the Control Room scenes, the progress bar and the buttons at the
  bottom of the Ponder window vanished behind the room's floor whenever the
  view closed in on part of it.

### Missiles
- The launch pad's countdown can now be set in the config
  (`missile.launch.countdownSeconds`, 10 seconds as before).
- Missiles really fly: they climb straight up out of their pad, turn towards
  their target and burn until they have the speed to fall on it, then coast
  on a true ballistic arc and come down from above. Each missile has a limited
  reach (about 3500 blocks for the launch pad's): a target too far away is
  fallen short of. Flights take longer and climb higher than before (about 50
  seconds and 600 blocks up for 2000 blocks).
- A missile no longer drags force-loaded chunks across the world: its flight is
  worked out on the server, and it is only drawn near players. In loaded
  chunks it hits whatever terrain is in its way; the chunk it lands in is
  loaded just before impact, so it always goes off. Its exhaust only shows
  while its engine burns.
- Sirens are warned for the whole flight with its exact time to impact.
- Missiles in flight in an older save finish their flight as before.
- Config: `missile.flight.*` (gravity, rise height, shortest range, top of the
  atmosphere, terminal phase, show distance).
- Missiles now point the way they fly: nose up on the climb, level at the top
  of the arc and nose down as they fall. Their exhaust comes from the tail.
- Server limits for launch pads: a maximum range (`missile.maxRange`), a
  protected area around the world spawn (`missile.protectedSpawnRadius`), the
  dimensions pads work in (`missile.allowedDimensions`), the permission level
  needed to launch (`missile.launchPermissionLevel`) and a per-player cooldown
  between launches (`missile.launchCooldownSeconds`). All are off by default.
  The player who answers for a pad is the last one to load it, or else whoever
  placed it. A refused pad doesn't count down: its screen says why and the
  player is told in chat. It launches by itself once allowed again.
- Launch warnings: players near where a missile will come down get a chat
  message with the impact point and the time left
  (`missile.launchBroadcast`: `NONE`, `IMPACT_AREA` by default, or `SERVER`
  to warn everyone).
- Nuclear detonations leave claimed land alone when Open Parties and Claims or
  FTB Chunks is installed: the crater and blast skip chunks whose claim keeps
  explosions out. Radiation and fallout still reach them
  (`explosive.respectClaims`, on by default).

### Missiles, radar and command table
- Silo rockets fly as what you built: the whole column (with anything glued to
  it, such as fins) lifts off as a Create contraption, and spent stages drop
  away as tumbling debris that burns where it lands. Extra blocks add mass
  (`missile.rocket.extraBlockMass`). The rocket must stand clear: glued to the
  silo, too big or with cargo in its containers, the controller says why.
- Liquid propellant: a new **Silo Fuel Port** in the silo wall takes **Rocket
  Fuel** (mixing: coal and hydrogen, heated) and **Liquid Oxygen** (mixing:
  liquid air, which also gives liquid nitrogen) from pipes and fills the tanks
  from the bottom up. A tank holds 1000 mB of each; liquid stages push harder
  and longer than solid cartridges, so a two or three stage liquid rocket
  reaches several kilometres and comes back fast enough to glow. Tanks mixing
  cartridges and liquids are refused. Silo walls are a block tag.
- Guidance tiers: the guidance module steers by a gyroscope (basic), by Create's
  precision mechanism (inertial) or by a new **Star Tracker** (stellar). The
  circular error probable grows with range and shrinks with the tier; the
  controller shows it (`missile.guidance.cepScale`, 0 for perfect aim).
- The silo controller tells you on arming what is wrong and where, grants the
  launch advancement and shows its state on goggles.
- Radars now have a reach: about 3 km for a bare rocket at the minimum speed,
  more when they turn faster and far more for a burning booster
  (`missile.radar.baseRange`). The Earth curves (`missile.radar.earthRadius`):
  a low flight hides beyond a radar's horizon, so put them high up. Radars that
  share a four-character link code within `missile.radar.networkRange` share
  what they see. Each radar guards a zone (centre and radius on its screen,
  everything by default): its redstone output and klaxon come on for flights
  predicted to come down in it; acknowledging silences the klaxon only.
- The radar screen is a console: a sweeping scope with range rings, echoes,
  history trails, predicted impacts and error ellipses over the land, a track
  table, a detail panel with an altitude profile, zoom, recentring, the link
  code and the zone. Sounds for pings, new tracks, the klaxon and
  acknowledgements (client: `radar.sounds`, `radar.phosphor`).
- The radar dish on top of the radar turns with its sweep. New **Radar Scope**:
  a wall screen (up to 3×3) linked to a radar with the Control Room Linker,
  showing its scope live. Radars also work with control room monitors, gauges,
  alarms and annunciators (tracks and threats) and with Display Links.
- New **Holographic Command Table**: nine pieces in a 3×3 fuse into one table
  with a continuous steel and brass frame over legs and a projector. Link up to
  four radars and a silo with the Control Room Linker (on any piece). Over a
  map of the land (1 to 16 km across, chosen by itself) it shows every track as
  a small missile riding its arc, nose along its course, a flame while it
  boosts and a lone re-entry vehicle as it falls, with where it has flown, where
  it will fly and where it comes down; the silo and the launch sites as silos,
  the radars as turning antennas with their coverage, the silo's reach and
  target, and rings where a weapon goes off.
- Use it in the world: look at a missile, the silo or a radar to see its label;
  right-click a missile to pin it (a panel over the table follows it, and the
  view follows it if it leaves the map); right-click the frame to change the
  zoom; sneak and right-click the glass to mark a target (green if the silo can
  take it, red with the reason if not) and do it again on the mark within five
  seconds to send the silo there. Right-click bare glass for its screen: the
  map, the tracks, the silo, and a designate mode with ground or air burst.

### Rocket effects
- Rockets now have real exhaust plumes: layered cones of flame at each nozzle of
  the burning stage, smoky orange for solid motors and a cleaner blue-white
  core for liquid engines, with shock diamonds near the ground. The plume
  balloons out as the air thins on the way up.
- Long smoke trails stay in the sky (40 seconds by default), spreading,
  drifting with the wind and lit by the sun: dark at night except where the
  flame lights them, glowing high up after sunset.
- Launch from a silo: steam vents from the shaft in the last 3 seconds of the
  countdown, a flash fills the shaft at ignition, exhaust billows out of the
  3×3 opening as the rocket climbs it and a ring of dust races out across the
  ground. No blocks are changed.
- Staging: a flash and a ring when a stage drops away, ullage puffs and then
  the next stage lighting; falling stages trail smoke; a puff when the warhead
  is released.
- Re-entry: a glowing plasma sheath that grows with speed and a brief
  incandescent streak behind the warhead. Inert warheads and falling stages
  throw up dust when they land; a nuclear warhead's glow just fades as the
  detonation takes over.
- Sounds: ignition, a near roar and a distant rumble that crossfade with
  distance and arrive late (sound travels at 17 blocks a tick), staging bangs,
  the hiss of re-entry, crashing wreckage and a silo's steam vent. A warhead
  passing within 64 blocks faster than 4 blocks a tick cracks a sonic boom.
- The view shakes near a lift-off (`rocket.shake` in the client config).
- Far away, past the render distance, a rocket still shows as a glowing point
  with its outline and its trail (`rocket.farTrails`). Effects are budgeted
  (at most 2000 rocket particles and 16 trails) and scale with
  `rocket.detail`; with a shader pack they switch to a simpler look.
- Missiles can be seen from much further away: players within
  `missile.fx.range` blocks (2048 by default) are told where each one is, so
  its effects show kilometres off. Missiles further than 256 blocks update
  every `missile.fx.farUpdateTicks` ticks. Only positions are sent, never a
  target.

### Nuclear weapons: the explosion
- A nuclear detonation is now worked out from its warhead and the height it goes
  off at. A ground burst digs a real crater, about a third as wide as the
  destruction radius (75 blocks for a full warhead): a bowl with a raised rim,
  ejecta scattered round it and a floor of glassy Trinitite, basalt and
  obsidian with magma at the centre. Around it, everything standing above the
  natural ground is blown away out to 35 kPa of blast, and part of it is wrecked
  out to 12 kPa, where roofs cave in and what is left hanging falls. Natural
  ground, bedrock and blast doors stay (`explosive.*`).
- Thermal flash: further out than the blast, whatever sees the fireball catches
  fire and, closer in, burns; grass burns bare, leaves burn away, exposed trees
  turn to Charred Logs and fires start. Walls, hills and shelters keep it off.
- Prompt radiation: whoever is near takes a dose at once, cut by walls and
  armour like other radiation.
- Fallout drifts with the wind into a plume twice as long downwind, for nuclear
  and dirty bombs alike. The wind follows the world seed and turns from day to
  day.
- Airbursts: sneak-use a Target Designator in the air to switch it to airburst;
  the missile then goes off above its target, digs no crater and leaves a tenth
  of the fallout, but its blast and flash reach further. A cannon shell set off
  high enough is an airburst too.
- Nuclear cannon shells (Create Big Cannons) are made with one to six
  weapons-grade cores and go off with what they carry; six are a full warhead.
- What you see: a white flash, an orange glow in the sky and fog, the fireball
  rising into a mushroom cloud seen from far beyond the render distance, a
  condensation dome, a ring of dust running out with the shock wave and the
  view shaking when it passes. Client options: `explosion.flash`,
  `explosion.cloudDetail`, `explosion.shake`.
- Craters are saved with the world and carry on after a restart. A detonation
  far from every player loads, or generates, the chunks its crater reaches a few
  at a time and lets them go when it is done, so it leaves its mark anyway
  (`explosive.crater.loadChunks`, `explosive.crater.chunksAtOnce`); all
  the detonations of a world share one budget per tick. They no longer set the
  sea round them pouring in, which used to drag the server down for a minute,
  and the blast wave traces at most 256 sheltered glass or leaf blocks a tick
  (a forest's canopy used to cost over a second a tick).
- A containment monitor finds its building open as soon as the breach reaches
  open sky, even with unloaded chunks nearby.
- Distant Horizons (optional): the mushroom cloud stays in sight over its
  distant terrain instead of being cut off at the vanilla view, hidden behind
  far mountains, and craters show up in its distant terrain once their chunks
  are saved. Detonations are now seen from 4096 blocks by default
  (`explosive.visualRange`).

### Thermonuclear weapons and costlier warheads
- New Thermonuclear Bomb, a hydrogen bomb loaded as a Teller-Ulam device: one
  weapons-grade core as its fission primary (plutonium compresses better than
  uranium), up to sixteen Lithium-6 Deuteride as its fusion secondary and up to
  four Depleted Uranium Blocks as its tamper. No core, no countdown; no
  deuteride, and only the primary goes off. Each deuteride multiplies the yield
  and the tamper adds to it and spreads much wider fallout, with a hot spot that
  fades over days. A full one wrecks what stands up to 750 blocks away, three
  times a full atomic bomb, while its crater stays about 110 blocks wide. It
  counts down for 15 seconds and warns sirens as far as its fallout reaches.
- Its explosion has a look of its own: a pure white flash, a bigger and whiter
  fireball that burns for about seven seconds before deepening to a saturated
  orange-red, a taller mushroom cloud of near-black soot, darker dust, two
  condensation domes, a redder sky and a lower, further-carrying rumble.
- New Thermonuclear Cannon Shell for Create Big Cannons: an HE shell, a
  Thermonuclear Bomb, one core, one to four deuteride and up to two tamper
  blocks; it reaches about 470 blocks at most. Missiles cannot carry
  thermonuclear parts.
- New `explosive.thermonuclear.*` settings (radius, power, crater, blast wave,
  flash, countdown, tamper, and an off switch that leaves only the primary), and
  `explosive.crater.maxReach` (768), which keeps what any detonation does to
  the ground within reach of ground zero, the main performance setting for big
  detonations. `civilDefense.blastWave.maxRadius` now goes up to 2048.
- Warheads are now assembled on a Create line. New parts: Explosive Lenses (TNT
  and gunpowder pressed onto iron sheets), Implosion Assemblies (eight lenses
  round a Beryllium Reflector), Firing Sets (precision mechanisms, electron
  tubes, copper and redstone), Neutron Initiators (beryllium, plutonium dioxide
  and gold, mixed hot), Bomb Casings and Thermonuclear Casings (on a Mechanical
  Crafter, the latter lined with depleted uranium) and Lithium-6 Deuteride
  (lithium-6 mixed hot with deuterium).
- The Nuclear Bomb is a Bomb Casing with an implosion assembly, a neutron
  initiator and a firing set deployed into it; the Thermonuclear Bomb also takes
  a Tritium Cell to boost its primary, so it needs a working tokamak. Missiles
  and launch pads are made on a Mechanical Crafter with a firing set, and the
  dirty bomb takes one too.
- Bombs and launch pads take weapons-grade cores only, two to a slot. A core is
  now worth exactly the metal cast into it (eight uranium or four plutonium), so
  six make a full warhead; it used to be worth half as much again. A uranium
  core keeps the enrichment of its metal. Nuclear cannon shells take one to six
  cores. Cores and the metal for them no longer count as a dirty bomb's load.
- The bomb's and the launch pad's screens are now translated.
- New advancements: Teller-Ulam (make lithium deuteride) and Brighter Than a
  Thousand Suns (witness a thermonuclear detonation). New Ponder scene and JEI
  pages for the thermonuclear bomb and the warhead parts.

### Annunciator boards
- Annunciator Panels now hang on a wall like the Wall Control Panel: a 3-pixel
  plate on a solid block, which falls off if the wall goes. Panels hung side
  by side or one above the other (up to 16 wide and 8 high) join into one
  board with a continuous frame and a single horn.
- One set of buttons per board, at the right end of its bottom strip: TEST
  lights every lamp for a few seconds, SIL silences the horn until the next
  new alarm, ACK acknowledges and RESET clears, all for the whole board. The
  first window to trip anywhere on the board is ringed as the first out.
  Every other panel's strip shows an engraved group nameplate.
- Windows have a severity: alarms light red with an urgent horn, warnings
  amber with a softer tone, and status windows only light white while their
  condition holds, with no horn and nothing to acknowledge. Presets set it
  (a SCRAM or a tokamak trip is an alarm, an open containment is a status).
- Boards come in the monitor styles: standard, plant computer and mimic board.
- New wrench screen in the style of the monitor's: the panel's six windows as
  they show, clickable, then the chosen window's label, alarm, threshold and
  severity, the panel's nameplate, the board's style and the presets.
- New sounds for the horn, the warning tone and the ringback chime, with
  subtitles.
- A Display Link reads a board: how many windows are lit and waiting, the
  first out and every lit window, the most urgent first.
- Redstone works the whole board: a pulse into the left side acknowledges,
  into the right resets, from above silences, and power from below holds the
  lamp test. While an alarm waits, every panel powers the wall it hangs on.

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

### Helium well and liquid helium cell
- New Helium Well, the first step towards liquid helium: a single kinetic
  block (1024 SU/RPM) with its shaft at the back that slowly draws helium gas
  out of the rock, 0.02 mB/t (24 mB a minute) at 128 RPM, less when slower and
  nothing below 32 RPM. It holds 1000 mB and pushes the gas into tanks and
  machines beside it, or pipes can drain it; when full it stops. Feed it to a
  cryogenic plant to make liquid helium. Goggles show its rate and tank, and
  JEI explains the chain. Configurable under `cryo.heliumWell.*`.
- New Liquid Helium Cell: Spouts fill an Empty Gas Cell with 250 mB of liquid
  helium, and Item Drains empty it again.

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
  part of the ring nearest to you, and are filtered so no high whine or hiss
  wears on you near a running tokamak.

### Visible and audible fission reactor
- New Reactor Viewport: a lead glass window that takes the place of any
  casing block in the reactor's walls or lid (not in the floor cross under
  the controller, which the reactor uses to find its size). It shields
  nearly as well as the casing, so you can watch the core in safety.
- The fuel channels now show what the reactor holds. Each loaded assembly
  fills a channel, column by column from the bottom up; its pellets darken as
  it burns (fresh, half burnt, nearly spent) and empty channels show bare
  cladding. Water fills the channels while the reactor holds coolant, and a
  running core glows Cherenkov blue in three steps with its power, lighting up
  the room. The looks are drawn by the reactor a few channels per tick and
  never make it check its structure again.
- The fission plant has sound: the core hums louder and higher with power,
  the control rods drop with a thud on a SCRAM and a klaxon sounds until the
  scrammed core is cold, a low two-tone alarm chimes in the warning zone, steam
  rushes out of the output ports (hot primary coolant bubbles instead) and a
  lifted relief valve pops and blows. None of them beeps or whines shrilly:
  they are low-pitched and filtered, even as their pitch rises with power or
  danger. Gas centrifuges whine and electrolyzers buzz with their
  shaft speed while they work, the vitrification melter roars while it melts
  and radioisotope generators hum while they hold a pellet.
- The reactor, its fuel and fluid ports, the steam generator, the turbines
  and the cooling tower now speak your language: chat messages, structure
  errors, goggle lines and Display Link texts are translated (English and
  Spanish).
- New milestones for the guide: running a reactor at full power, a cold
  shutdown after a SCRAM, a relief valve lifting and forming a steam
  generator.

### Renewable materials, fan treatments and thorium
- Two new bulk fan treatments next to Create's. Acid leaching: an Encased Fan
  blowing through placed or flowing sulfuric acid (now placeable, with a
  bucket); a yellow current that sizzles on the items and burns creatures
  caught in it. Irradiation: a fan blowing through the new Irradiation Source
  (a capsule of waste in an open cage, which air passes through); a green,
  crackling current that turns thorium dioxide into irradiated thorium and
  depleted uranium into irradiated depleted uranium, and gives a radiation dose
  to creatures in it (armour shielding and iodide count). Both work on items on
  the ground, on belts and on depots; the last catalyst the air passes decides
  the treatment, as with Create's. JEI shows them like Create's fan
  categories.
- Base materials without ore, at one to two times the ore route's cost per
  unit. Crushing granite, tuff (Create's recipe keeps its nuggets), deepslate or
  cobbled deepslate gives fines; acid leaching them gives uranium concentrate
  and a little thorium dioxide (granite), spodumene concentrate (tuff), or
  uranium concentrate and fluorite (deepslate).
- Washing sand, red sand or gravel now sometimes gives heavy mineral sand
  (Create's results are kept). Acid leaching it gives thorium dioxide, the only
  source of thorium, with a chance of uranium concentrate and beryllium.
- Brine: water boiled down in a heated basin. A centrifuge spins it into
  lithium carbonate, borax and water. An amidoxime adsorbent (string, bone meal
  and water) picks the uranium out of water or brine; acid leaching the loaded
  adsorbent gives uranium concentrate and returns half of the adsorbent.
- Decontamination: acid leaching low-level waste, spent D-T pellets and
  depleted RTG pellets gives clean iron nuggets and Radioactive Sludge, which an
  Item Drain pours out as contaminated water.
- Thorium fuel: three thorium dioxide and one seed (low-enriched uranium
  dioxide or uranium-233 dioxide) compact into four pellets, assembled into
  Thorium Reactor Fuel like uranium. It starts at about half the reactivity of
  low-enriched uranium and grows more reactive as it breeds uranium-233,
  peaking about half way through its burn; it lasts 40% longer and runs a
  little cooler. Spent thorium fuel cools in pools and casks like the other
  spent fuels and reprocesses (THOREX): crush it, dissolve the fragments in
  nitric acid, and centrifuge the solution into uranium-233 nitrate, recovered
  acid, thorium dioxide and high-level waste; the uranium-233 denitrates into
  oxide for new pellets. Irradiated thorium dissolved in nitric acid also gives
  a little uranium-233, and irradiated depleted uranium a trace of plutonium
  through the spent fuel solution.
- New items are radioactive where it makes sense (uranium-233 and spent thorium
  fuel the most). New config: `fuelcycle.irradiationDosePerSecond` and
  `fuelcycle.acidDamagePerSecond`.

### Guide: advancements, Ponder and JEI
- The advancements now fill six tabs, each with its own root and background:
  Fuel Cycle (from raw uranium to MOX and the new thorium cycle: fines, heavy
  mineral sand, sulfuric acid, thorium dioxide, uranium-233 and thorium fuel),
  Nuclear Fission (the reactor, the reactor viewport, full power, cold
  shutdown, the steam generator and its relief valve), Nuclear Waste, Civil
  Defense (radiation safety moved here, with sirens, the seismic detector,
  blast doors and shelters), Nuclear Fusion (tokamak, cryogenic plant,
  ignition) and Weapons. 58 advancements in all; every old one is kept, so
  progress already made stays earned, though some now hang in another tab.
- Nine secret challenges, hidden until earned: burn thorium fuel until it is
  spent, keep a loss of coolant inside a sealed containment past half its
  rated pressure, let a core catcher freeze the corium of a meltdown, have a
  Repository Inspector find eight or more packages all stored for good, ride
  out a blast wave unharmed in a certified shelter, get a fusion gain above
  one, lose a plasma to a disruption, see a nuclear cannon shell go off, and
  the meltdown itself.
- Machines now report the moments those advancements listen for: a siren
  starting to sound, a seismic detector feeling a detonation, a blast door
  shutting, a missile lifting off, a tokamak or cryogenic plant forming, a
  disruption and a burning plasma with a gain above one.
- New Ponder scenes: the air raid and hand-crank sirens, blast doors, fallout
  shelters with the seismic detector, the nuclear and dirty bombs, the missile
  launch pad, the fuel cycle from ore to fuel assembly (crushing, washing,
  leaching, enrichment, pellets, assembly and the electrolyzer), and the acid
  leaching and irradiation fan treatments, also shown on Create's Encased Fan.
  The fan scenes show what the loaded recipes actually make.
- New JEI pages: the nuclear bomb, the launch pad with its missile and target
  designator, the nuclear cannon shell (with Create Big Cannons), the reactor's
  fuel and fluid ports, control rods and temperature sensor, the tokamak's
  shell blocks, magnet drive, fluid port and fuel injector, the fusion turbine
  blocks, the reactor viewport, and the two fan treatments.

### Synchrotron rings and target stations
- The particle accelerator can now be built as a synchrotron. End the linac in
  an Injection Septum facing the middle of a ring of radius 6 or more, fill
  every block of the ring with beamline parts (Ring Dipole Magnets,
  Superconducting Ring Dipoles, Superconducting RF Cavities, quadrupoles, RF
  cavities, beam pipe) and leave it through one Extraction Septum facing out.
  The beam fills the ring, ramps up to the ring's top energy and is then sent
  down the extraction line. Bigger rings and more (and superconducting) dipoles
  reach higher energies; the linac must reach at least a hundredth of the
  ring's top energy to inject.
- Superconducting dipoles and cavities need liquid helium: pipe it into an
  Accelerator Cryogenic Feed beside the ring or line. It goes straight into the
  controller's helium loop, and the warm helium comes back out of the same
  feed. Without helium the start is refused, and a cool-down that runs dry
  trips.
- New Target Station at the end of a beamline. It takes the beam like a beam
  dump (and gets activated like one). With a beam above
  `accelerator.antimatter.thresholdGev` (25 GeV) its magnetic horn collects
  antiprotons and hands them to the Penning traps or Antimatter Conduits
  beside its back; goggles show the production in ng per minute. Its glowing
  beam window and pulsing collector ring show when it works.
- New Beam Interlock panel for personnel protection. Place it beside the
  beamline, by the door into the beam area, and right-click it to search and
  secure the area; sneak-click opens it. The beam will not start while any
  panel is open, and opening one (or opening a door beside it) trips a running
  beam with a buzzer. Doors, trapdoors and gates beside a panel refuse to open
  by hand while the beam is on (`accelerator.interlock.locksDoors`). With
  `accelerator.interlock.requirePlayersOutside` the beam also refuses to start,
  and trips, while a player stands inside the machine. A secured panel gives a
  redstone signal out of its back, for a warning light.
- New Ponder scene "The synchrotron".

### Antimatter containment
- Antiprotons can now be stored. Three Penning traps hold them, each a glass
  cylinder between two magnet heads with a glowing band in its colour: the
  Small Penning Trap (one block, cyan, 32 ng), the Penning Trap (two blocks
  tall, violet, 256 ng) and the Large Penning Trap (2x2 and three tall, amber,
  2048 ng). The medium and large traps are placed and broken as one block.
  The antimatter shows as a floating, pulsing cloud that grows with the charge.
- Traps run only on Forge Energy (about 20, 80 and 250 FE/t when full) and
  keep a buffer of about ten seconds. Their status light is green when stable,
  amber on low power and red when quenching.
- Treat them with care. Antimatter put into a trap without energy annihilates
  at once. A loaded trap that runs out of energy quenches: three seconds of
  alarm, red light and sparks, then it annihilates. Breaking a loaded trap
  annihilates it too (an empty one drops as usual), and the blast can break
  and set off the loaded traps around it. Unloading the chunk is safe.
- An annihilation is a small explosion that grows with the charge (radius 3
  to 8 blocks), with a flash, green sparks and a burst of gamma radiation
  that reaches twice as far. It leaves no fallout or contamination, and
  seismic detectors record it.
- New Antimatter Vial (4 ng, green). Put it into a trap's port by right-
  clicking the trap; the wrench switches the port between filling the vial
  and emptying it into the trap, and an empty hand takes it back. A loaded
  vial has a 30-second battery that only recharges in the port of a powered
  trap: when it runs flat with antimatter inside, the vial annihilates where it
  is, in your inventory or on the ground. Empty vials are safe and stack to
  16; loaded ones do not stack.
- New Antimatter Conduit, a glass tube with magnetic rings that joins traps
  and target stations. It needs a little Forge Energy for every block, holds
  nothing itself and moves antimatter only while all of it is powered: from
  target stations into traps, and between traps towards an even fill. A green
  pulse runs along it while antimatter flows.
- Traps show their charge, field, energy, state and time to quench on the
  goggles, give a comparator signal by fill, and can be read by Display Links
  and the control room (new monitor rows, the ANTIMATTER annunciator preset and
  the Antimatter Stored and Trap Power alarm metrics). They hum with their
  field and sound their own alarm when they quench.
- New Ponder scene "Antimatter containment" and JEI pages for the traps, the
  vial, the conduit and the target station. New config keys under
  `antimatter.trap`, `antimatter.vial`, `antimatter.conduit` and
  `antimatter.blast` (see docs/configuration.md).

### Creative RF Generator
- New Creative RF Generator, for testing machines without an energy mod. It
  pushes up to 1,000,000 FE/t (100,000 by default; set it on its value panel)
  into every block around it and gives cables as much as they pull. Creative
  tab only, with no recipe, and it cannot be broken or wrenched in survival.

### Annihilation Generator
- New Annihilation Generator: a heavy chamber that burns antiprotons. It turns
  a shaft at 256 RPM with up to 65,536 SU, makes up to 16,384 FE/t, or half of
  each. Pick Rotation, Electric or Split on the value panel on top, or click
  the top face with a wrench (the wrench on any other face turns it, like any
  kinetic block). With Forge Energy disabled
  (`antimatter.generator.feEnabled`) it only turns.
- It burns 6 ng a minute at full output. Redstone sets the throttle. With no
  signal it keeps a tiny standby burn that only holds its field and gives
  nothing. When the supply cannot keep up, its output drops to match.
- It draws antimatter from Penning traps and target stations beside it, from
  an Antimatter Conduit, or from a vial in its port (right-click with the vial;
  an empty hand takes the vial back). Its chamber holds 1 ng.
- The chamber is a small Penning trap. Before it lights, it needs Forge Energy
  once to raise its field. A cold chamber with no energy takes no antimatter
  in, and a loaded vial left in its port annihilates when its battery runs
  flat. Once lit, the burn holds the field by itself until the fuel runs out;
  when the supply runs dry it goes out and needs energy again (a cable, or
  Electric or Split mode, which keeps its own buffer full). In Electric or Split it refills its own field
  buffer first, then pushes its energy evenly into every block around it. One
  generator can power the very traps that feed it.
- Breaking a loaded generator annihilates its chamber and any loaded vial in
  its port; unloading the chunk is safe.
- Goggles show the mode, throttle, output, chamber, field and burn rate.
  Comparators read the output, a Display Link source shows its status, and the
  control room can link it (new monitor rows, gauges and chart recorder pens).
  It hums with a drone that rises with the output. Its core glows from dim
  green on standby to white-green at full output, and the lamp of the active
  mode lights on its panel.
- Crafted from superconductor ingots, a Small Penning Trap, electron tubes, a
  brass casing, precision mechanisms and a shaft.

### Antiproton Injector
- New Antiproton Injector, a tokamak shell block with a small trap chamber and
  a beam nozzle. While the plasma is heating, held well enough and dense
  enough, it fires one dose of antiprotons (`antimatter.ignitionDose`, 2 ng).
  The dose lifts the plasma straight past ignition, with no neutral beams or
  antennas needed.
- It needs Forge Energy (20,000 FE per shot, `antimatter.injector.shotFe`) and
  antimatter from a trap or conduit beside it, or from a vial in its port.
  After a shot it cools down for 10 seconds
  (`antimatter.injector.cooldownTicks`). Its chamber holds a few doses. Like
  any trap, it annihilates them if its energy runs out.
- The wrench switches it between automatic, armed by redstone, and safe.
  Goggles show whether it is armed and ready, its doses, and what it is
  waiting for (cooldown, a dose, power, the heating phase, confinement or
  density). An amber lamp shows it is armed and a green one that it is ready.
  On a shot the nozzle flashes and a streak of light runs into the plasma.
  Comparators read whole doses.
- Crafted from a Small Penning Trap, superconductor ingots, a Neutral Beam
  Injector and an electron tube.

### Antimatter: guide and looks
- New advancements: First Nanogram (fill a trap with 1 ng), E = mc² (run an
  Annihilation Generator), Ignition by Annihilation (ignite a tokamak with
  antiprotons), and a secret one.
- New Ponder scenes for the Annihilation Generator and the Antiproton
  Injector, and JEI pages for both. The target station has a JEI category
  that plots antiprotons per minute, per MW of beam, against the beam energy,
  from the threshold to where the yield stops rising.
- The control room's trap gauges and chart recorder pens, and monitor labels
  for traps ("PT") and generators ("AG").
- Antimatter annihilations have their own look: a white-green flash, a halo
  and a shell of green and cyan sparks that races out to the blast edge and is
  gone in about two seconds, with no grey smoke.
- The Small Penning Trap no longer shows a dark ring in the middle of its
  glass.
- Penning traps and the Antimatter Conduit are now held at a normal size in
  first and third person. The Large Penning Trap used to fill the screen.
- In survival, the first Forge Energy for traps and a generator's first start
  has to come from another mod. After that, a generator in Electric or Split
  keeps itself and its traps running.

### Isotopes and nuclear medicine
- Radioisotopes that decay on their own clock, in your inventory, in a chest
  or in a machine. Technetium-99m halves in six hours of game time,
  molybdenum-99 in under three days, cobalt-60 in 60 days, strontium-90 in 330
  and americium-241 practically never (`isotopes.*`). The bar under the item
  shows the activity left. The Geiger counter and the dose you take from
  carrying one follow it.
- Cobalt: heated mixing of crushed raw iron with crushed raw copper frees
  cobalt nuggets. Nine make a Cobalt Ingot.
- New Reactor Irradiation Channel, a sample tube through a fission reactor's
  wall that takes any casing block's place. Up to sixteen cobalt ingots soak
  up the core's neutrons and come out as Cobalt-60 Sources. At rated power
  that takes about five minutes. Pipes and funnels load it and empty it, and
  comparators read the progress.
- Strontium-90 is extracted from PUREX high-level waste with nitric acid. Two
  make a Strontium-90 RTG Pellet: less power than plutonium, but it lasts
  twice as long.
- Americium-241 grows in stored plutonium. Plutonium starts ageing when a
  player first carries it. After about a week the americium can be separated
  with nitric acid, and the plutonium comes back.
- Technetium-99m Generator: a Target Station hit by a beam of 1 GeV or more,
  but below the antiproton threshold, turns depleted uranium into molybdenum-99
  columns. Milk one for up to four doses of technetium-99m. It is best a day
  after the last milking and worn out in a little over a week.
- New Gamma Irradiator: up to four cobalt-60 sources in a lead head, two
  blocks above a belt or depot like a press. Food with a harmful effect (raw
  chicken, rotten flesh, spider eyes, pufferfish, poisonous potatoes) stops
  while the shutter opens and comes out sterilised: the same nourishment, no
  harmful effects. It slows down as the cobalt decays and lets food pass once
  the sources are spent. Open, it irradiates whoever stands by the belt. A
  blue glow and a cone of light show it working.
- New Smoke Detector with a speck of americium-241. Fixed to a floor, wall or
  ceiling, it gives a redstone signal and beeps when fire, a lit campfire or
  furnace, lava or a burning creature is close.
- New Medical Isotope Kit. With a technetium-99m dose in your inventory, use it
  on yourself or another player for a full scan: dose held, what is adding to
  it from carried items and from the ground, the lifetime total and the
  medicines at work. The tracer makes potassium iodide and Prussian blue up to
  50% stronger for two minutes.
- New Radiography Camera. With a cobalt-60 source in your inventory, use it on
  a fission reactor to see its core damage, oxidised cladding, vessel, thermal
  stress, fuel and control rods. A weaker source sees fewer details. Each shot
  gives you a small dose.
- JEI categories for target irradiation, reactor irradiation and gamma
  sterilisation, and info pages for everything new.
- New advancements: Hot Metal (make cobalt-60), Five-Second Rule (sterilise
  food) and Milking the Cow (milk a technetium generator).
- A lit Annihilation Generator keeps its field for three seconds after its
  last burn. A short gap in its supply no longer leaves it cold, and conduits
  can keep feeding it without a cable.
- Penning traps, the Annihilation Generator and the Antiproton Injector can no
  longer be moved by contraptions, which carried their antimatter off
  unpowered.

### Abandoned nuclear sites
- Three kinds of ruin now generate in new chunks, each assembled from modular pieces so no two
  look alike. `/locate structure` and `/place structure` know them.
- Abandoned nuclear power plants, very rare and only on flat ground in plains, deserts, savannas,
  meadows, forests and taigas: a reactor building whose containment dome has fallen in, with a
  broken reactor shell, steam generators and the polar crane inside; around it a turbine hall, a
  control building with its desks, monitors and annunciators, a waste yard or the stump of a
  cooling tower, and the access road with its checkpoint.
- Civil fallout shelters: a concrete blockhouse with an air raid siren on its roof over a
  stairwell down to dormitories, a supply store and the ventilation room.
- Military bunkers: a sandbagged guard post with a hand-cranked siren over a ladder shaft that
  drops several levels to corridors, a jammed blast door, a command post, dormitories, an armory,
  the air filtration plant, a waste store and a laboratory.
- The ruins are dead: no multiblock in them forms or runs. The danger is their leaking waste
  barrels (at most six per piece), which contaminate the ground around them over time.
- Their chests hold progress loot: uranium concentrate and yellowcake, graphite, boron, lithium,
  beryllium, cobalt, reactor parts, desk modules, Geiger counters, worn hazmat suits and depleted
  uranium armour, potassium iodide, Prussian blue, sterilised rations, maintenance kits and Create
  parts. Never enriched uranium, plutonium or weapon parts.
- `ruins.*` in the common config switches each kind off or lowers its chance, and sets how flat
  the ground under a power plant must be.

### Creative tab
- The creative tab is split into themed sections: Mining & Materials, Fuel Cycle, Fission, PWR &
  Containment, Power Generation, Control Room, Spent Fuel & Waste, Isotopes & Medicine, Safety &
  Maintenance, Fusion, Cryogenics, Accelerators, Antimatter, Civil Defense and Weapons.
- Each section opens with its own banner, animated while the mouse is over it, and an item's
  tooltip names its section. Items other mods add to the tab go in a last "Other" section.
- `creativeTab.sections` in the client config turns the banners off for a single list in the same order.

### Create Aeronautics
Optional: everything here needs Sable (Create Aeronautics, Simulated); without it the mod plays as before.
- Explosions reach ships. A nuclear or thermonuclear detonation tears away the blocks of ships in its
  severe zone, wrecks part of those in its moderate zone and chars or sets fire to what faces the
  fireball, like the terrain. The blast wave throws ships it meets, spinning them when it strikes off
  centre, and shatters their glass. Sable already lets ordinary explosions reach ships.
- A bomb, missile pad, Penning trap or reactor aboard a ship goes off where the ship really is: the
  crater, the blast wave, the flash and the fallout are in the world under the ship, and the blast
  takes the ship with it.
- Machines work aboard as on the ground. Assembling, splitting or setting down a ship no longer sets off
  Penning traps, antiproton injectors or annihilation generators, spills their inventories or scrams a
  running reactor; a reactor re-forms aboard as it was.
- Control rooms reach ships: a desk, monitor, annunciator, chart recorder or alarm linked to a machine
  still finds it after the machine is taken aboard (or set down again), and the link range is the real
  distance, ground to ship or ship to ship. Advanced redstone links measure it the same way.
- Alerts, incident log notices, sirens, advancements, the seismic detector and machine sounds and
  effects now use where a ship's machine really is. Earthquakes never shake a plant aboard a ship.
- Meltdown aboard: corium runs towards the real ground however the ship is tilted, burns through the
  hull and falls out of the ship on to whatever lies below. Fallout from an accident aboard comes down
  under the ship.
- Ships carry their own contamination: they take it up flying through contaminated air (less the
  higher they fly) and from fallout coming down on them, keep it when saved, travel with it and pass it
  to both halves when they break. It decays like the ground's, doses those aboard and shows as dust over
  the deck; a Radiation Scrubber aboard cleans it. Releases and leaks aboard go to the ship, not to its
  invisible storage.
- New Nuclear Thermal Engine: a controller, one to eight heat exchanger segments and a Thermal Rocket
  Nozzle in a line. Hot primary coolant from a pressurized reactor heats water, deuterium or hydrogen
  (hydrogen pushes three times as hard as water) and hands the primary back cold. Redstone sets the
  throttle. The nozzle heats with the flow, water most: run too hot it wears and bursts, spilling the
  primary's activity, and its exhaust leaves a trace of activity where the jet lands. Mend it with a
  Maintenance Kit.
- New Antimatter Engine: an annihilation chamber, one to six Magnetic Coils and a Magnetic Nozzle. Fed by
  an Antimatter Conduit or a vial (16 ng chamber) and Forge Energy for its coils, it pushes several
  times harder than the thermal engine for 12 ng a minute at full throttle. More coils turn more of the
  annihilation into thrust; what they miss heats them and leaves behind the nozzle as gamma rays.
  Without energy, or with its coils worn through, the field fails and the chamber annihilates after a
  quench alarm.
- Both engines push the ship they are on, opposite to their nozzle, show on goggles, Display Links and
  control room monitors, desk gauges and chart recorders, and have JEI pages, Ponder scenes, recipes (only
  with Sable) and two advancements. A new Propulsion section in the creative tab lists them.
- Our heaviest blocks weigh more on a ship (Sable's `heavy` and `super_heavy` tags): reactor, turbine and
  tokamak casings, waste barrels, the nuclear bomb, the engines, and the depleted uranium block.
- Config under `aeronautics.*`: what blasts do to ships, ship contamination and the engines.

### Operation and maintenance
- Plant parts now wear with use:
  - a fission reactor's control rod drives, with its power and at every SCRAM;
  - its relief valve, at every lift;
  - a steam generator's tubes, while they move heat, four times faster boiling dry;
  - a turbine's blades, with the steam they expand.
- Worn parts fail:
  - a jammed rod drive leaves its channel where it was at a SCRAM;
  - a worn relief valve sticks open up to ten times more often;
  - a burst tube leaks primary coolant into the secondary side, and the steam generator then gives off radiation;
  - failed blades trip the turbine on vibration until they are repaired.
- Goggles, the Radiography Camera and the control room show each part's state:
  - new alarm metrics for part wear, turbine blades and the refuelling outage;
  - a Maintenance preset for annunciators.
- **Breaking:** a fission reactor now takes fresh fuel only in a refuelling outage: formed, shut down cold with every control rod in.
- New Maintenance Kit, which a Deployer can also use. It services:
  - a reactor in its outage, including freeing a stuck rod;
  - a steam generator or turbine that is stopped.
- Natural earthquakes now and then shake the ground:
  - seismic detectors record them;
  - a reactor shaken past its seismic trip scrams;
  - the shaking wears plant parts.
- New server incident log rated on the INES scale:
  - automatic and seismic SCRAMs and turbine trips are 0;
  - stuck rods and relief valves are 1;
  - tube ruptures are 2;
  - fuel damage and vessel breaches are 4;
  - meltdowns are 5, or 7 with no containment;
  - earthquakes, detonations and drills are also logged.
  
  Players nearby hear of anything from INES 1 up. A new Incident Log Display Link source works on reactor controllers and seismic detectors.
- New `/nuclear incident list`, `/nuclear incident drill` and `/nuclear incident trigger` commands.
- Every feature can be turned off in the config under `maintenance.*`. `maintenance.enabled = false` turns them all off.
- New Ponder scene for plant maintenance.
- New advancements: Planned Outage and a secret one.

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
- Annunciator Panels placed before stay where they are, even with no wall
  behind them, and now look like hung plates. They no longer power the block
  below them (below is now the lamp test input): wire their alarm output from
  the block behind them instead.
- Nuclear bombs and launch pads loaded with loose uranium or plutonium drop it
  beside them the first time they load and stop any countdown; cast it into
  cores and load those. A core now counts as eight warhead units, so a full
  warhead takes six. Cannon shells made before keep the warhead they had.
- The settings `fission.thermal.steamEnergy`, `fission.thermal.heavyWaterSteamEnergy`
  and `coolingTower.passivePerBlock` are now `steamEnergyPerMb`,
  `heavySteamEnergyPerMb` and `condensePerBlock`, with the new defaults (4.0,
  6.2 and 0.5). Values set under the old names are dropped so the new steam
  balance applies; set them again under the new names to keep a custom value.
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
  turns on the SCRAM signal: wire it to your rod pistons. With the new
  physics the fuel melts at 2800 °C: set the threshold to 1500-1800 °C.
- Reactors running in existing worlds load with their fuel at 1500 °C at
  most and no damage, and settle on their new power. The config keys
  `fission.uraniumBurnPerNeutron`, `graphiteModeratorBonus`,
  `berylliumReflectorBonus` and `xenonWorth` are gone; their replacements
  (`fission.fuelBurnPerRod`, `fission.reactivity.*`) use other units, so old
  values are not carried over. `fission.decayHeatTimeTicks` is now the time
  scale of five decay heat groups (20000 by default). `pwr.heatTransfer` no longer does anything,
  and `pwr.accidents.oxidationHeat` is now in kelvin of a rod's fuel (1.2 by
  default). Automatic power controllers on desks, power alarms and power
  gauges now read percent of the rated power: check their settings.
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
- Fission reactors' fuel channels now show their fuel, glow and coolant. In
  an existing reactor they start dark and light up at the next update, a
  second after the world loads.
- Sulfuric acid can now be placed from a bucket, as the catalyst of the
  acid leaching fan treatment. Create's own crushing of tuff and washing of
  sand, red sand and gravel keep their outputs and gain the new ones.
- Advancements are split into six tabs. Every existing advancement keeps its
  id, so earned ones stay earned; some now hang from a different parent.
- New config keys `reactorVisuals.*`, `fuelcycle.*`, `performance.*` and
  `explosive.blastColumnsPerTick` / `explosive.blastBlocksPerTick` are
  documented in `docs/configuration.md`.

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
