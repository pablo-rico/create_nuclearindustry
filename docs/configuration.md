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

```toml
[spentFuel]
timeScale = 1.0
hotDoseScale = 2.0
warmDoseScale = 1.0
cooledDoseScale = 0.65
agedDoseScale = 0.4
hotBurnHeat = 1.5
assemblyDecayHeat = 20.0
poolCoolingPerBlock = 0.4
poolScanLimit = 4096
boilEnergyPerBlock = 400.0
waterTransmission = 0.15
coolerCapacity = 60.0
coolerRatedSpeed = 64.0
coolerStress = 4.0
caskTransmission = 0.02
```

El combustible gastado sale del reactor **caliente** y fechado con el día de
juego (tiempo de juego / 24000), así que los elementos descargados el mismo día
se apilan. Su calor residual es la suma de tres exponenciales (productos de
fisión de vida corta, media y larga), y el grado sale de la fracción que queda:
caliente hasta unos 2 días, templado hasta una semana, enfriado hasta unas 3
semanas y después envejecido. `timeScale` multiplica los días transcurridos: con
2 el combustible se enfría el doble de rápido. El grado se pone al día cuando
alguien mira el combustible (en el inventario de un jugador, cada segundo; en
un bastidor o un contenedor seco), así que también avanza con el chunk
descargado. El combustible de mundos anteriores, sin historia, cuenta como
templado.

- `hotDoseScale`, `warmDoseScale`, `cooledDoseScale` y `agedDoseScale`
  multiplican la dosis del data map `radioactivity`, que es la del combustible
  templado: el combustible antiguo irradia igual que antes.
- `hotBurnHeat`: calor de desintegración de cada elemento caliente que se
  lleva encima, en las mismas unidades que `heat_per_second` del data map. Como
  el plutonio, quema si no se lleva al menos la mitad de protección. Con 0 no
  quema.
- `assemblyDecayHeat`: calor de un elemento en un bastidor justo al
  descargarlo, en kW; baja según la curva.
- `poolCoolingPerBlock`: calor que evacua por sí sola cada fuente de agua de
  la piscina, en kW. Cada bastidor busca su piscina rellenando el agua
  conectada (hasta `poolScanLimit` bloques) y reparte la refrigeración con los
  demás bastidores que encuentra. Un volumen mayor que el límite (el mar, un
  lago) cuenta como `poolScanLimit` bloques y nunca hierve.
- `boilEnergyPerBlock`: excedente de calor, en kJ, que evapora un bloque de
  agua. Cuando el calor supera la refrigeración, el excedente se acumula y la
  piscina pierde de golpe toda su capa superior (para que el agua de alrededor
  no vuelva a crear fuentes), con vapor.
- `waterTransmission`: fracción de la radiación de un bastidor que deja pasar
  cada bloque de agua justo encima. Con 0,15, dos bloques paran más del 97 %.
  Un bastidor al descubierto irradia sin esa protección.
- `coolerCapacity`, `coolerRatedSpeed` y `coolerStress`: el Refrigerador de
  piscina, sumergido en el agua de la piscina y accionado por un eje desde
  arriba, evacua `coolerCapacity` kW a `coolerRatedSpeed` RPM (proporcional por
  debajo; nada por debajo de 8 RPM) y consume `coolerStress` SU por RPM.
- `caskTransmission`: fracción de la radiación de su contenido que deja salir
  el Contenedor de almacenamiento en seco. Solo admite combustible templado o
  más frío y nunca tiene fugas. Además, como bloque, solo deja pasar el 2 % de
  la radiación que lo atraviesa (data map `radiation_transmission`).

Reprocesado: triturar combustible enfriado o envejecido da más fragmentos y
menos residuo. Las recetas usan el ingrediente
`create_nuclearindustry:spent_fuel_grade` (`base`, `min_grade`, `max_grade`,
con los grados `hot`, `warm`, `cooled` y `aged`); cada receta de un mismo
combustible cubre grados distintos, así que Create siempre elige la que
corresponde.

## Residuos y repositorio

<!-- opciones waste.*: las documenta su paquete de trabajo -->

## Instrumentos de la sala de control

- `chartSamplesPerPaper` (240): muestras que dibuja un Registrador gráfico con
  una hoja de papel. 240 llenan la gráfica entera; con una muestra por tick una
  hoja dura 12 segundos y con una cada 20 ticks, 4 minutos. Sin papel el
  registrador se para.
- `annunciatorInterval` (10): ticks entre dos comprobaciones de las ventanas de
  un Panel anunciador. Menos ticks detectan antes una alarma breve, a cambio de
  muestrear más a menudo las máquinas enlazadas.
- `pid.interval` (20): ticks entre dos pasos del Módulo controlador de barras.
  Cada paso lee el reactor y mueve el objetivo de las barras; los pistones
  siguen moviendo las barras de verdad.
- `pid.gain` (1.5): ganancia proporcional. El error se mide en fracciones de la
  escala completa (1000 MW de potencia o 4000 °C de temperatura) y la salida es
  la inserción de las barras (0-1).
- `pid.integral` (0.05): ganancia integral, por segundo. Es la que lleva el
  reactor exactamente a la consigna; más alta corrige antes pero oscila más.
- `pid.derivative` (0): ganancia derivativa, sobre la medida (cambiar la
  consigna no da un salto). Apagada por defecto porque los pistones ya son lentos.
- `pid.deadband` (0.01): error, en fracción de la escala, dentro del cual el
  controlador deja las barras quietas para que no oscilen sin parar.
- `pid.rateLimit` (0.05): cambio máximo del objetivo de las barras por segundo
  (0 = sin límite). La salida se redondea además a segmentos enteros de barra
  (1/altura interior del reactor), que es lo que pueden sostener los pistones.

## Defensa civil

<!-- opciones civilDefense.*: las documenta su paquete de trabajo -->

## Uranio y litio de otros mods

- Materias primas: la etiqueta `create_nuclearindustry:raw_uranium` incluye
  `c:raw_materials/uranium` y `forge:raw_materials/uranium`. Se trituran como el
  uranio en bruto del mod. La espodumena usa `c:raw_materials/lithium`.
- Lingotes: `create_nuclearindustry:uranium_ingots` incluye `c:ingots/uranium` y
  `forge:ingots/uranium`. Los lingotes externos cuentan como uranio natural (0,7%)
  y se fluoran a hexafluoruro con acido fluorhidrico para entrar en la cascada.
  Un datapack puede ampliar la etiqueta para otros identificadores.
