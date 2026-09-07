# Changelog

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
