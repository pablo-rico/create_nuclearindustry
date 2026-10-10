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
graphiteModeratorBonus = 0.35
berylliumReflectorBonus = 0.25
xenonPoisoning = true
xenonWorth = 0.3
iodineHalfLifeTicks = 3600
xenonHalfLifeTicks = 5400
decayHeatFraction = 0.07
decayHeatTimeTicks = 2400

[fusion]
deuteriumTritiumBurnRate = 0.0011
maxMajorRadius = 16
coilsPerSector = 4
magnetInputStress = 1024.0
disruptionExplosions = true

[controlRoom]
linkRange = 128

[coolingTower]
passivePerBlock = 1.0
fanStress = 8.0
fanBoostPerFan = 0.25
maxFanBoost = 1.0

[radiation]
enabled = true
doseMultiplier = 1.0
decayPerSecond = 1.0
nauseaThreshold = 100.0
weaknessThreshold = 300.0
sicknessThreshold = 600.0
reactorEmissionPerNeutron = 0.002
iodideAbsorption = 0.5
purgePerSecond = 15.0
solidTransmission = 0.9
backgroundRate = 0.05
maxShielding = 0.98

[contamination]
enabled = true
halfLifeTicks = 240000
max = 50.0
doseScale = 1.0
barrelLeak = 0.02
geologicalStorageMaxY = 0
waterColumnsPerTick = 256
scrubberRate = 0.05
scrubberRadius = 1
scrubberWastePer = 2.0
scrubberStress = 8.0
```

## Turbinas

Las turbinas son cajas de 3 a 7 bloques de ancho y alto (impares) y de 3 a 16 de
largo, con una columna de rotores en el eje. Cada rotor expande 80 mB/t de vapor
(40 de vapor de plasma) y cada par de puertos entrada/salida deja pasar 320 (160);
el menor de los dos limites manda.

Con un Puerto de escape de turbina, la turbina no condensa dentro: saca el vapor
como vapor de escape hacia una torre de refrigeración y su capacidad se duplica
con el mismo caudal. Si el escape no tiene salida, la turbina se para igual que
con el tanque de condensado lleno.

Las capacidades son la contribucion de stress por RPM por cada 320 mB/t de vapor
(160 de vapor de plasma) expandidos; Create la multiplica por la velocidad. Las
turbinas giran a 64 RPM. Duplicar una capacidad duplica su output con el mismo vapor.

## Torres de refrigeración

Una torre de radio de base Rb y altura H condensa `Rb * H * passivePerBlock`
mB/t de vapor (o vapor pesado) en agua (o agua pesada), 1:1. Cada ventilador a
256 RPM o más suma `fanBoostPerFan` de esa capacidad (proporcional por debajo,
nada por debajo de 16 RPM), hasta `maxFanBoost` en total, y consume `fanStress`
SU por RPM. El radio va de 4 a 16 y la altura de `2*Rb + 4` a `5*Rb` (máximo 64).

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
- `graphiteModeratorBonus`: produccion extra de neutrones de un nucleo totalmente
  moderado, es decir, con cada columna de combustible tocando dos columnas de
  Moderador de grafito. Con 0,35 es un 35% mas reactivo; una sola vecina cuenta
  la mitad. Cero hace el grafito inerte.
- `berylliumReflectorBonus`: fraccion de las perdidas de neutrones que devuelve
  un anillo interior (las columnas que tocan la carcasa) revestido por completo
  de Reflector de berilio. Con 0,25 se pierde una cuarta parte menos. Cero hace
  el reflector inerte.
- `xenonPoisoning`: si la fision genera xenon-135, que frena el reactor. A
  potencia constante se queda en un nivel moderado; tras bajar la potencia o un
  SCRAM sigue subiendo unos minutos (el "pozo de yodo") y luego desaparece, asi
  que un reactor recien parado necesita sacar mas barras para volver a arrancar.
- `xenonWorth`: fraccion maxima de la produccion de neutrones que puede tragarse
  el xenon. Un nucleo a plena potencia se queda cerca de la mitad; el pico tras
  una parada llega a unos dos tercios.
- `iodineHalfLifeTicks` / `xenonHalfLifeTicks`: ticks que tardan el yodo-135 y el
  xenon-135 en reducirse a la mitad (3 y 4,5 minutos por defecto).
- `decayHeatFraction`: calor residual de un nucleo que lleva un rato
  funcionando, como fraccion de su calor de fision. Sigue calentando el nucleo
  despues de un SCRAM, asi que el refrigerante no se puede cortar de golpe. Cero
  hace que el reactor se enfrie en cuanto se para la reaccion.
- `decayHeatTimeTicks`: ticks para que el calor residual baje a un tercio una
  vez parada la fision (tambien tarda eso en acumularse).

Las barras de control solo se mueven físicamente, con pistones de Create. El
SCRAM (botón del panel o SCRAM automático, activado por defecto a 2800 °C) no
las mueve: enciende la señal de los Puertos de redstone en modo SCRAM hasta que
se rearma, y esa señal es la que hay que llevar a los pistones. El umbral se
ajusta en el panel (no es una opción de configuración) y solo se puede rearmar
cuando el núcleo está 200 °C por debajo.

Cada canal de combustible aloja un elemento. El reactor solo acepta elementos
combustibles al 3% o mas; el uranio metalico suelto ya no es combustible. El
combustible gastado conserva el enriquecimiento con el que entro.

## Sala de control

- `linkRange`: distancia maxima, en bloques, entre un monitor, panel o alarma y
  lo que tiene enlazado. El objetivo ademas tiene que estar en un chunk cargado
  de la misma dimension; si no, la pantalla muestra "Sin señal".

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

Entre la fuente y el jugador, cada bloque deja pasar solo una fracción de la
radiación: la del mapa de datos de bloques `create_nuclearindustry:radiation_transmission`
(carcasa de reactor 0,35; plomo `c:storage_blocks/lead` 0,2; hierro 0,5;
obsidiana 0,6; agua 0,85) o `solidTransmission` para cualquier otro bloque
sólido. Un reactor en marcha emite `reactorEmissionPerNeutron` por nivel de
neutrones desde el centro del núcleo; su propia carcasa ya detiene la mayor
parte. La armadura protege según el mapa de datos de objetos
`create_nuclearindustry:radiation_shielding` (las piezas se suman, hasta
`maxShielding`). Por debajo de `backgroundRate` la dosis no sube y sigue bajando.

```json
{
  "values": {
    "othermod:lead_bricks": { "transmission": 0.25 }
  }
}
```

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

## Contaminacion del terreno

Los accidentes dejan contaminacion en el suelo: un valor por chunk, en rad/s a
ras de suelo. No hace tick: se calcula al leerlo y se reduce a la mitad cada
`halfLifeTicks` (240000, diez dias de juego; 0 = nunca). Un chunk guarda como
mucho `max`. La dosis de estar encima se mezcla entre los cuatro chunks mas
cercanos, baja con la altura sobre el suelo (a la mitad a 8 bloques) y, bajo
techo, solo cuenta lo que atraviesa el tejado. `doseScale` la multiplica.

Los clientes solo reciben un byte de gravedad (0-7) por chunk contaminado, con
el chunk y cuando cambia de nivel, y lo dibujan como polvo verde sobre el suelo
de los chunks cercanos. En la configuracion del cliente
(`create_nuclearindustry-client.toml`):

```toml
[contamination]
particles = true        # polvo sobre el suelo contaminado
particleDensity = 1.0   # 0-4; con particulas "Reducidas" se usa el 40%

[hud]
enabled = true          # HUD de dosis
corner = "TOP_LEFT"     # TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
```

El HUD de dosis aparece mientras hay tasa o dosis: tasa, dosis actual con una
barra hasta el umbral de enfermedad (coloreada por la etapa de sintomas) y el
total acumulado. El total no baja nunca, se conserva al morir y solo lo pone a
cero `/nuclear dose reset <jugadores> total`. El servidor envia la lectura solo
a su jugador y solo cuando cambian sus valores redondeados.

Comandos de depuracion (nivel de permiso 2):

| Comando | Que hace |
|---|---|
| `/nuclear contamination query [pos]` | Valor y gravedad del chunk |
| `/nuclear contamination add <pos> <rad/s> [radio]` | Crea contaminacion, con caida hacia el borde del radio (en chunks) |
| `/nuclear contamination set <pos> <rad/s>` | Fija el valor de un chunk |
| `/nuclear contamination clear <pos> [radio]` | La elimina en un radio |
| `/nuclear contamination clear all` | Limpia todo; los chunks no cargados se limpian al cargarse |
| `/nuclear contamination expand <pos> <radio> [pasos] [fraccion]` | La extiende a los chunks vecinos conservando el total |
| `/nuclear contamination fallout <pos> <radio> <rad/s>` | Simula la lluvia radiactiva de una explosion |
| `/nuclear dose get <jugadores>` | Dosis, tasa y total acumulado |
| `/nuclear dose set <jugadores> <rad>` | Fija la dosis actual |
| `/nuclear dose reset <jugadores> [current\|total\|all]` | Pone a cero la dosis actual, el total o ambos |

Los comandos solo tocan chunks cargados; `fallout` llega a los demas cuando se
cargan.

Que contamina:

- Bombas, misiles y proyectiles nucleares: hasta 1,5 veces el radio del crater,
  con 0,2 rad/s por punto de potencia en el centro (32 con una cabeza completa).
- La fusion del nucleo de un reactor: 20 rad/s en el reactor, hasta 48 bloques.
- Los barriles de residuos por encima de `geologicalStorageMaxY`: cada tick
  aleatorio (unos 68 s de media) suman `barrelLeak` a su chunk.

Cuando una lluvia radiactiva deja un chunk con gravedad 3 o mas (0,5 rad/s), la
fuente de agua de la superficie de cada columna puede volverse agua
contaminada (con probabilidad gravedad/7). Los chunks esperan en una cola y se
revisan `waterColumnsPerTick` columnas por tick (256 = un chunk). El agua
contaminada irradia 0,3 rad/s por fuente (igual que su cubo) y la centrifuga la
separa en agua limpia y residuos.

El Absorbedor de radiacion limpia el suelo: accionado desde abajo, quita cada
segundo `scrubberRate` rad/s (a 256 RPM o mas; menos si gira mas despacio) de su
chunk y de los que estan a `scrubberRadius` chunks, empezando por el mas
contaminado. Cada `scrubberWastePer` absorbidos produce un Residuo radiactivo
en su unica ranura, que se vacia con embudos o tolvas; lleno, se para. Consume
`scrubberStress` SU por RPM.

Un barril en `geologicalStorageMaxY` o mas abajo (Y=0 por defecto) cuenta como
almacenado: ni irradia ni contamina. La regla es solo la profundidad; el
almacen lo construye el jugador. Los bloques que siguen esta regla estan en la
etiqueta `create_nuclearindustry:geologically_storable`.

## Reactor de agua a presión y accidentes

<!-- opciones pwr.*: las documenta su paquete de trabajo -->

## Combustible gastado

<!-- opciones spentFuel.*: las documenta su paquete de trabajo -->

## Residuos y repositorio

<!-- opciones waste.*: las documenta su paquete de trabajo -->

## Instrumentos de la sala de control

<!-- opciones controlRoom.*: las documenta su paquete de trabajo -->

## Defensa civil

```toml
[civilDefense.siren]
threatRadius = 96
audibleRange = 128
crankMinSpeed = 32
crankStress = 2.0

[civilDefense.seismic]
memorySeconds = 300
falloff = 256.0
fullScale = 100.0

[civilDefense.blastDoor]
blocksPerRevolution = 2.0
redstoneTicksPerBlock = 10
stress = 4.0

[civilDefense.blastWave]
speed = 8.0
maxRadius = 256
yieldPerPower = 250.0
resistanceScale = 6.0
damageThreshold = 5.0
damagePerKPa = 0.5
knockbackPerKPa = 0.02
breakFragileBlocks = true
fragileOverpressure = 5.0
fragileBlocksPerTick = 1500

[civilDefense.shelter]
maxVolume = 4096
cellsPerTick = 2048
rescanTicks = 100
minSpeed = 16
stress = 4.0
minWallResistance = 1.5
maxWallTransmission = 0.9
filterFactor = 0.05
blastFactor = 0.1
```

Sirenas:

- La Sirena antiaérea suena mientras recibe redstone (alerta: un aullido largo
  cada 8 s) o, sin redstone, mientras haya una amenaza a su alcance (aviso de
  ataque: un aullido rápido cada 3 s). Una amenaza está a su alcance si su
  objetivo está, en horizontal, a `siren.threatRadius` bloques o menos más el
  radio de la propia amenaza (el radio de su lluvia radiactiva).
- Son amenazas los misiles en vuelo (hacia su objetivo), las bombas en cuenta
  atrás (sobre la propia bomba) y las plataformas de lanzamiento en cuenta atrás
  (sobre el objetivo del designador, avisando antes del despegue). Se guardan
  solo en memoria: tras recargar el mundo, cada fuente vuelve a avisar en su
  siguiente tick. Al cancelar la cuenta atrás o al detonar, el aviso se retira.
- La Sirena de manivela no usa redstone ni amenazas: suena mientras su eje
  (por debajo) gira a `siren.crankMinSpeed` RPM o más y no está sobrecargado.
  Cuanto más rápido gira, más aguda. Consume `siren.crankStress` SU por RPM.
- Ambas dan 15 a un comparador mientras suenan. Se oyen a `siren.audibleRange`
  bloques (opción leída en cada cliente).

Detector sísmico:

- Registra todas las detonaciones nucleares y sucias de su dimensión y guarda
  las 8 últimas: distancia, potencia, rumbo (punto cardinal) y momento. Se
  consultan con las gafas (agachado, las 8) y con un Display Link (la última).
- La sacudida que siente es `potencia / (1 + (distancia / seismic.falloff)²)`:
  la potencia entera cerca y la mitad a `seismic.falloff` bloques. El
  comparador da `15 × sacudida / seismic.fullScale` (mínimo 1 si se sintió
  algo, máximo 15) con la mayor sacudida de los últimos
  `seismic.memorySeconds` segundos, y vuelve a 0 solo. Una cabeza nuclear
  completa tiene potencia 160.

Puertas blindadas:

- Se construyen en una pared: un hueco de 2x2 a 4x4 lleno de hojas de Puerta
  blindada (de acero o de plomo, se pueden mezclar), rodeado por un anillo
  completo de Marcos de puerta blindada (esquinas incluidas). Uno de los marcos
  del anillo, que no sea una esquina, se cambia por el Controlador, con su eje
  atravesando la pared.
- Al abrirse, el panel se desliza hacia el controlador si este está a un lado
  de la puerta, o hacia +X/+Z si está arriba o abajo. Las columnas más alejadas
  quedan libres primero; mientras el panel pasa por una celda, sigue siendo
  sólida.
- Con rotación manda el eje: girando en un sentido abre y en el otro cierra,
  a `blastDoor.blocksPerRevolution` bloques por vuelta (a 64 RPM y 2 bloques
  por vuelta, una puerta de 2 de ancho tarda poco menos de 1 s). Consume
  `blastDoor.stress` SU por RPM. Sin rotación, un flanco de subida de
  redstone la abre y uno de bajada la cierra, a `blastDoor.redstoneTicksPerBlock`
  ticks por bloque. Un comparador lee cuánto está abierta (0 cerrada, 15
  abierta).
- Cerradas, las hojas tienen resistencia a explosiones 1500, de modo que los
  cráteres nucleares las respetan (igual que al marco y al controlador), y
  dejan pasar el 15 % (acero) o el 2 % (plomo) de la radiación
  (`radiation_transmission`). Abiertas, no tienen colisión y dejan pasar la
  radiación y las explosiones sin romperse. Si se rompe el marco, una hoja o
  el controlador, la puerta se cierra de golpe.

Onda expansiva:

- Una detonación nuclear hace una explosión vanilla pequeña en el punto cero
  (la bola de fuego, potencia `min(12, 0,35 × potencia)`), el cráter de
  siempre, la lluvia radiactiva y una onda expansiva. Las bombas sucias
  conservan su explosión convencional y no tienen onda.
- La onda avanza `blastWave.speed` bloques por tick desde el centro hasta
  `min(blastWave.maxRadius, 4 × radio del cráter)` bloques (`maxRadius = 0`
  la desactiva). No se guarda: si el servidor se para, la onda en curso se
  pierde.
- Sobrepresión (Kinney-Graham): depende de la distancia escalada
  `Z = r / W^(1/3)`, con `r` en bloques (metros) y `W = potencia ×
  blastWave.yieldPerPower` kg de TNT. Una cabeza completa (potencia 160,
  cráter de 250) da unos 51 kPa a 128 bloques y 15 kPa a 256; una de una sola
  unidad (potencia ~39, cráter de 83) da unos 48 kPa en el borde del cráter y
  8 kPa a 256.
- A cada entidad viva la golpea una vez, con la sobrepresión de su distancia
  multiplicada por lo que dejan pasar los bloques en línea recta desde el
  centro: cada bloque deja pasar `exp(-resistencia a explosiones /
  blastWave.resistanceScale)` (piedra 37 %, madera 61 %, vidrio 95 %, obsidiana
  y puertas blindadas cerradas nada; las puertas blindadas abiertas no
  cuentan). Dentro de un refugio certificado se multiplica además por
  `shelter.blastFactor`.
- Daño: `(sobrepresión − blastWave.damageThreshold) × blastWave.damagePerKPa`
  medios corazones. Es daño de explosión (`minecraft:is_explosion`), así que
  Protección contra explosiones lo reduce. Empuje hacia fuera de
  `sobrepresión × blastWave.knockbackPerKPa` bloques por tick (máximo 4),
  menos con resistencia al empuje.
- Con `blastWave.breakFragileBlocks`, al pasar rompe los bloques de la
  etiqueta `create_nuclearindustry:blast_fragile` (hojas, vidrio y paneles;
  ampliable con un datapack) de los 16 bloques más altos de cada columna si
  la sobrepresión llega a `blastWave.fragileOverpressure`. Los que tienen algo
  sólido encima solo se rompen si la onda les llega a través de lo que haya en
  medio (una ventana que mira a la explosión sí, una tras un muro grueso no).
  Como mucho `blastWave.fragileBlocksPerTick` por tick; no carga chunks.

Refugios:

- La Unidad de filtración de refugio es un bloque cinético con eje (como un
  eje de Create). Se coloca dentro de la sala, en el suelo o contra una
  pared: el eje puede atravesar la pared o el suelo sin abrir la sala.
  Consume `shelter.stress` SU por RPM.
- Inspecciona el aire desde el bloque libre de encima (o de un lado) con un
  relleno por inundación de `shelter.cellsPerTick` bloques por tick, y lo
  repite cada `shelter.rescanTicks` ticks. Cuenta como aire todo lo que no es
  un bloque de colisión completa: las puertas, trampillas, vallas, losas y
  escaleras dejan la sala abierta; una puerta blindada cerrada la cierra.
- Certifica la sala si está cerrada (sin llegar a chunks sin cargar ni fuera
  de la altura del mundo), tiene como mucho `shelter.maxVolume` bloques de
  aire y todos los bloques de pared, suelo y techo que tocan el aire tienen
  resistencia a explosiones de al menos `shelter.minWallResistance` y dejan
  pasar como mucho `shelter.maxWallTransmission` de la radiación. Con los
  valores por defecto valen la piedra, el ladrillo, la madera o el hormigón;
  no el vidrio, la tierra ni la lana. Las gafas muestran el tamaño de la sala
  o el motivo por el que no se certifica (y el bloque débil).
- Dentro de una sala certificada las ondas expansivas pierden el
  `1 − shelter.blastFactor` de su sobrepresión. Mientras la unidad gira a
  `shelter.minSpeed` RPM o más sin sobrecarga, la dosis de la lluvia
  radiactiva y del suelo contaminado se multiplica por `shelter.filterFactor`
  (la de los bloques radiactivos, los emisores y el inventario, no). Los
  refugios se guardan solo en memoria: tras recargar, la unidad los vuelve a
  certificar en su primera inspección.

## Uranio y litio de otros mods

- Materias primas: la etiqueta `create_nuclearindustry:raw_uranium` incluye
  `c:raw_materials/uranium` y `forge:raw_materials/uranium`. Se trituran como el
  uranio en bruto del mod. La espodumena usa `c:raw_materials/lithium`.
- Lingotes: `create_nuclearindustry:uranium_ingots` incluye `c:ingots/uranium` y
  `forge:ingots/uranium`. Los lingotes externos cuentan como uranio natural (0,7%)
  y se fluoran a hexafluoruro con acido fluorhidrico para entrar en la cascada.
  Un datapack puede ampliar la etiqueta para otros identificadores.
