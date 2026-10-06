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
- Spent fuel keeps its enrichment and reprocesses into oxide one tier lower
  plus radioactive waste, sealed into waste barrels.
- Textured control panel with a manual SCRAM switch.
- Reactors now notice shell blocks removed by machines, not only by players.
- Fuel is no longer lost when the structure changes, and neither is the core's
  heat: swapping a shell block no longer cools the reactor instantly.
- New Reactor Redstone Port for the shell: puts out temperature, power, fuel,
  coolant or steam as a 0-15 signal, or the SCRAM signal. Wrench to pick.
- Control rods only move physically. The SCRAM button no longer pushes them
  in; it turns on the SCRAM signal of the Redstone Ports until reset, to be
  wired to the pistons that drive the rods.
- Automatic SCRAM on the control panel, on by default at 2800 °C (500-3900,
  +/- 100, Shift for 500). When the core reaches it the SCRAM signal turns
  on and stays latched; Reset only works 200 °C below the threshold.
- Seven Display Link sources for the reactor (temperature, power, fuel,
  control rods, coolant, steam, status), on the controller, Temperature
  Sensors and Redstone Ports. Numeric ones work on nixie tubes.
- The Temperature Sensor now outputs on its panel side, as its tooltip says;
  it used to power the block behind it.

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
- Turbine casings now craft four at a time. Ports work on every side and
  no longer scan the area on every call.

### Cooling towers
- New hyperboloid Cooling Tower multiblock: base radius 4 to 16, height from
  twice the radius plus 4 up to five times the radius. It condenses steam
  (heavy steam) back into water (heavy water) at base radius x height mB/t,
  with a plume and the sound of falling water. No power.
- Optional Cooling Tower Fans in the bottom wall layers add up to 25% each
  at 256 RPM, at most doubling the tower. Display Link source, JEI info and
  a Ponder scene.

### Radiation
- Blocks now shield: radiation from waste, fallout and reactors is cut by
  every block between the source and the player, per the new block data map
  `radiation_transmission` (reactor casing 35%, lead 20%, iron 50%,
  obsidian 60%, water 85%, other solid blocks 90%).
- Running fission reactors irradiate their surroundings in proportion to
  their neutron level; their own casing stops most of it.
- Armour shielding comes from the item data map `radiation_shielding`, so
  other mods' armour can protect; the hazmat suit still stops 95%.
- Dose rates below 0.05/s count as background and let the dose decay.
- Players accumulate a dose from carried material, nearby waste and fallout
  from accidents, with nausea, weakness and radiation sickness. Hazmat suit
  and Geiger counter. Configurable and can be disabled.

### Technical
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
- Running reactors in existing worlds now irradiate nearby players. Set
  `radiation.reactorEmissionPerNeutron = 0` to turn that off.
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
