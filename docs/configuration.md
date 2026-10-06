# Configuracion y compatibilidad

NeoForge genera `config/create_nuclearindustry-common.toml` al iniciar el mod.
En multijugador, edita el archivo del servidor. Los valores se recargan al
guardar el archivo, pero reinicia el mundo para aplicarlos de forma consistente.

```toml
[turbines]
steamCapacityPerPort = 32768.0
plasmaCapacityPerPort = 65536.0

[fission]
boronAbsorption = 1.5
uraniumBurnPerNeutron = 0.0000045
fuelUnitsPerAssembly = 100.0
enrichmentReactivity = 0.25

[fusion]
deuteriumTritiumBurnRate = 0.0011
maxMajorRadius = 16
coilsPerSector = 4
magnetInputStress = 1024.0
disruptionExplosions = true

[radiation]
enabled = true
doseMultiplier = 1.0
decayPerSecond = 1.0
nauseaThreshold = 100.0
weaknessThreshold = 300.0
sicknessThreshold = 600.0
```

## Turbinas

Las capacidades son la contribucion de stress por RPM y puerto a caudal completo
(320 mB/t de vapor o 160 mB/t de vapor de plasma); Create la multiplica por la
velocidad. Las turbinas giran a 64 RPM. Duplicar una capacidad duplica su output
con el mismo vapor.

## Fision

- `boronAbsorption`: absorcion de las barras de control. Se multiplica por la
  proporcion de barras de control frente a canales de combustible, se limita a
  100% y se aplica segun su insercion. Cero elimina la absorcion.
- `uraniumBurnPerNeutron`: unidades de combustible consumidas por nivel de
  neutrones y tick. Cero desactiva el agotamiento.
- `fuelUnitsPerAssembly`: unidades de un elemento combustible al 5% (LEU). El
  combustible mas enriquecido dura mas: `unidades = este valor * (enriquecimiento / 5)^0.66`,
  asi que uno al 20% contiene unas 2,5 veces mas.
- `enrichmentReactivity`: produccion extra de neutrones por cada vez que se dobla
  el enriquecimiento por encima del 5%. Con 0,25, el combustible al 20% es 1,5
  veces mas reactivo. Cero hace que el enriquecimiento solo afecte a la duracion.

Cada canal de combustible aloja un elemento. El reactor solo acepta elementos
combustibles al 3% o mas; el uranio metalico suelto ya no es combustible. El
combustible gastado conserva el enriquecimiento con el que entro.

## Fusion (tokamak)

- `deuteriumTritiumBurnRate`: **cambia de significado respecto a 1.1.0**. Ahora son
  mB de deuterio (y de tritio) quemados por celda de plasma y tick a plena
  temperatura. Un tokamak de radio R tiene unas 56*R celdas de plasma, y la
  potencia de fusion es de 1000 MW por mB/t quemado. Si tu archivo conserva el
  valor antiguo `0.0015`, el tokamak consumira y producira un 35% mas.
- `maxMajorRadius`: radio mayor maximo (del solenoide central al centro del plasma).
- `coilsPerSector`: bobinas de campo toroidal que necesita cada sector para una
  cobertura completa. Un tokamak tiene 4*R sectores.
- `magnetInputStress`: stress (SU por RPM) de cada toma de imanes. Se necesita una
  toma por cada 4 bloques de radio.
- `disruptionExplosions`: si una disrupcion abre agujeros en la camara. Con `false`
  el plasma simplemente se apaga.

## Radiacion

La dosis se acumula con los objetos que lleva el jugador, los bloques
radiactivos cercanos (barriles de residuos) y la lluvia radiactiva de accidentes.
Un traje antirradiacion completo detiene el 95%. La dosis baja `decayPerSecond`
por segundo cuando cesa la exposicion y se reinicia al morir.

Las tasas por objeto vienen del mapa de datos `create_nuclearindustry:radioactivity`
(`data/create_nuclearindustry/data_maps/item/radioactivity.json`). Un datapack
puede cambiarlas o anadir objetos de otros mods:

```json
{
  "values": {
    "othermod:plutonium_ingot": { "per_second": 2.0 },
    "othermod:enriched_uranium": { "per_second": 1.5, "scales_with_enrichment": true }
  }
}
```

Con `scales_with_enrichment`, la tasa se multiplica por `enriquecimiento / 90`.

## Uranio y litio de otros mods

- Materias primas: la etiqueta `create_nuclearindustry:raw_uranium` incluye
  `c:raw_materials/uranium` y `forge:raw_materials/uranium`. Se trituran como el
  uranio en bruto del mod. La espodumena usa `c:raw_materials/lithium`.
- Lingotes: `create_nuclearindustry:uranium_ingots` incluye `c:ingots/uranium` y
  `forge:ingots/uranium`. Los lingotes externos cuentan como uranio natural (0,7%)
  y se fluoran a hexafluoruro con acido fluorhidrico para entrar en la cascada.
  Un datapack puede ampliar la etiqueta para otros identificadores.
