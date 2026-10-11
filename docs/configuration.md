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
  mB de deuterio (y de tritio) quemados por celda de plasma y tick a la densidad de
  referencia (0,7 del límite de densidad), a la temperatura de máxima reactividad
  (unos 4000°) y sin cenizas; escala el ritmo de reacción del modelo de plasma. Un
  tokamak de radio R tiene unas 56*R celdas de plasma, y la potencia de fusion es
  de 1000 MW por mB/t quemado. Si tu archivo conserva el valor antiguo `0.0015`,
  el tokamak consumira y producira un 35% mas.
- `maxMajorRadius`: radio mayor maximo (del solenoide central al centro del plasma).
- `coilsPerSector`: bobinas de campo toroidal que necesita cada sector para una
  cobertura completa. Un tokamak tiene 4*R sectores.
- `magnetInputStress`: stress (SU por RPM) de cada toma de imanes. Se necesita una
  toma por cada 4 bloques de radio; las tomas fijan la consigna del campo toroidal.
- `disruptionExplosions`: si una disrupcion abre agujeros en la camara. Con `false`
  el plasma simplemente se apaga.

## Tokamak: arranque y control del plasma

El tokamak ya no arranca solo. Se pone en marcha con el botón **Arrancar** de su
pantalla, con un pulso de redstone en el controlador o con una palanca del pupitre,
y recorre estas fases:

1. **Vacío**: las bombas bajan la presión de la cámara desde la atmósfera (10^5 Pa)
   hasta el límite del plasma (`vacuumLimit`). Hacen falta ceil(R/4) bombas a su
   velocidad nominal; con menos de un 80% de bombeo no se llega nunca.
2. **Enfriado de los imanes**: el helio líquido de los puertos criogénicos enfría las
   bobinas de 293 K a 4,5 K. Mientras se enfrían gastan el doble de helio.
3. **Rampa del campo**: el campo sube hasta la consigna de las tomas de imanes,
   min(1, suma de tomas / ceil(R/4)).
4. **Ruptura**: un pulso del solenoide central ioniza una bocanada de deuterio.
5. **Calentamiento**: el solenoide (óhmico, solo mientras el plasma está frío y le
   quedan voltios·segundo) y el calentamiento auxiliar (inyectores de haces neutros
   y antenas de RF) suben la temperatura.
6. **Combustión**: por encima de 1500° el plasma quema D-T (o D-D si falta tritio).

El plasma tiene densidad (fracción del límite de Greenwald, que crece con el campo),
cenizas de helio que se retiran solas y vuelven como helio caliente al circuito de los
imanes, y una ganancia Q = potencia de fusión / calentamiento. Un plasma con
suficiente densidad y confinamiento entra en **ignición** y sigue ardiendo con el
calentamiento apagado (Q infinito). Por encima del límite de densidad el plasma se
vuelve inestable y sufre una disrupción, igual que por sobrecalentamiento.

Los fallos de vacío, de criogenia, de campo o de combustible apagan el plasma **de
forma segura** (fase de bajada en rampa, sin explosiones) y la pantalla muestra la
causa. El refrigerante del tokamak es **solo agua**. El tritio regenerado que no cabe
en el tanque sale por los inyectores en SALIDA; si tampoco cabe, la regeneración se
detiene con un aviso (nunca se tira).

Claves `fusion.*` nuevas:

- `minimumField` (0,5): campo mínimo, en fracción del nominal, que deben pedir las
  tomas de imanes para arrancar y mantener el plasma; por debajo se dispara con
  "Campo magnético insuficiente".
- `fieldRampPerSecond` (0,1): velocidad de la rampa del campo, en fracción del nominal
  por segundo.
- `vacuumLimit` (-3,0): presión máxima de la cámara con plasma, como log10 de pascales.
  Si la presión pasa de aquí unos segundos con plasma, se dispara con "Vacío perdido".
- `pumpDownSeconds` (15): segundos que tardan las bombas necesarias en hacer el vacío
  desde la atmósfera.
- `magnetCooldownSeconds` (20): segundos que tarda el helio líquido en enfriar las
  bobinas a 4,5 K si hay helio de sobra.
- `heliumPerSector` (0,25): mB/t de helio líquido por sector toroidal (4*R por
  tokamak) a campo pleno; el doble mientras se enfrían. Un R=4 gasta unos 4 mB/t. Todo
  el helio gastado vuelve como helio caliente para la planta criogénica; si el tanque de
  retorno está lleno, se ventila. Por encima de 20 K con campo, disparo "Imanes demasiado
  calientes".
- `magnetStandbyShare` (0,3): parte del gasto a campo pleno que gastan las bobinas frías
  sin campo.
- `greenwaldDensity` (1,0): límite de densidad a campo pleno, en mB de plasma por celda.
- `defaultDensity` (0,7): fracción del límite a la que apunta el ritmo de inyección por
  defecto. Un R=4 con dos inyectores de haces neutros da así un Q de 4 a 8 y una
  ganancia neta pequeña; subir la densidad hacia 0,9 lo lleva a la ignición.
- `energyConfinementSeconds` (7,5): tiempo de confinamiento de la energía de un R=4 con
  todas las bobinas y campo pleno. Crece como R^0,8, así que los tokamaks grandes
  necesitan menos calentamiento.
- `particleConfinementSeconds` (2,0): tiempo que el combustible pasa en el plasma antes
  de que las bombas lo saquen; vuelve a los tanques, así que solo se gasta lo que se quema.
- `ashRemovalSeconds` (5,0): tiempo que las cenizas de helio pasan en el plasma.
- `ohmicPowerPerCell` (0,08): calentamiento óhmico del solenoide con el plasma frío, en
  MW por celda. Se desvanece al calentarse el plasma: no basta para llegar a la combustión.
- `solenoidPulseSeconds` (30): duración del pulso del solenoide; se recarga lo mismo
  con el tokamak apagado.
- `breakdownDeuterium` (10): mB de deuterio de la bocanada de la ruptura.
- `beamFuelling` (0,25): parte del deuterio de los haces neutros que acaba en el plasma.

En la pantalla, **Combustible** fija el ritmo de inyección (mB/t de D+T; por defecto el
que da `defaultDensity`, máximo el que llevaría al 150% del límite), **Mezcla D:T** la
parte de deuterio (si falta un isótopo se completa con el otro; 100:0 sirve para criar
tritio con D-D) y **Calentamiento** el nivel del calentamiento auxiliar. El calentamiento
auxiliar se reduce solo entre 4500° y 5000° para no sobrecalentar el plasma.

## Planta criogénica y vacío del tokamak

Las bobinas superconductoras del tokamak se enfrían con helio líquido. La
**planta criogénica** lo licúa y la **bomba de vacío** mantiene el vacío de la
cámara del plasma.

- **Planta criogénica**: bloque de 3x3 de Carcasa de la planta criogénica, de
  3 a 5 bloques de alto. El controlador va en el centro de una pared lateral
  mirando hacia fuera; se forma con clic derecho. El centro de las capas
  interiores (la caja fría) debe ser carcasa. Necesita al menos un Compresor
  de helio en las paredes, con el lado del eje hacia fuera, y Puertos de la
  planta criogénica. La llave inglesa cambia el modo de cada puerto:
  - *Entrada de helio caliente* y *Salida de helio líquido*: el circuito de
    helio. Hacen falta las dos para que se forme.
  - *Entrada de nitrógeno líquido*: preenfriamiento opcional.
  - *Toma de aire / salida de aire líquido*: con uno de estos puertos la
    planta licúa aire en lugar de helio y no necesita los de helio.

  Los puertos de entrada tiran de los tanques o puertos que tengan justo al
  lado y los de salida empujan hacia ellos (`cryo.plant.portTransfer` mB por
  tick); también se conectan a tuberías.
- **Producción**: cada capa de altura licúa `cryo.plant.heliumPerLayer` mB/t
  de helio a la velocidad nominal (`cryo.plant.ratedSpeed`). Por debajo de
  ella la producción es proporcional a las RPM y por debajo de
  `cryo.plant.minSpeed` es nula. Un compresor sobrecargado no cuenta, y la
  velocidad de la planta es la media de sus compresores. Con los valores por
  defecto, una planta de 3x3x3 a 128 RPM da 6 mB/t, suficiente para las
  bobinas de un tokamak de R=4 (unos 4 mB/t con el campo al máximo).
- **Pérdidas**: en cada paso se escapa la fracción `cryo.plant.loopLoss` del
  helio que entra. Las cenizas de helio del plasma y el aire líquido
  centrifugado reponen el circuito.
- **Preenfriamiento**: mientras haya nitrógeno líquido, la planta produce
  `cryo.plant.nitrogenBonus` más (0,5 = +50 %) y gasta
  `cryo.plant.nitrogenPerHelium` mB de nitrógeno por cada mB de helio
  líquido. Si el nitrógeno se agota a mitad de tick, solo se aplica una parte
  de la bonificación.
- **Aire líquido**: cada capa da `cryo.plant.airPerLayer` mB/t. En la
  centrifugadora, 1000 mB de aire líquido dan 990 mB de nitrógeno líquido y
  10 mB de helio, la primera carga del circuito.
- **Estrés**: la planta carga la red con `cryo.plant.compressorStressPerLayer`
  SU/RPM por capa, repartidas a partes iguales entre sus compresores. Un
  compresor que no forma parte de una planta formada carga una capa.
- **Puerto criogénico del tokamak**: va en la carcasa del tokamak. En modo
  entrada (rol `CRYO_IN`) llena el tanque de helio líquido del controlador; en
  modo salida (rol `CRYO_OUT`) vacía su tanque de helio caliente. La llave
  inglesa cambia el modo. Intercambia con los tanques que tenga al lado como
  los demás puertos del tokamak.
- **Bomba de vacío del tokamak**: va en la carcasa con el lado del eje hacia
  fuera. A `cryo.vacuumPump.ratedSpeed` RPM cuenta como una bomba completa;
  por debajo, en proporción a las RPM, y por debajo de
  `cryo.vacuumPump.minSpeed` no bombea. Carga la red con
  `cryo.vacuumPump.stress` SU/RPM. Un tokamak de R=4 necesita una bomba a la
  velocidad nominal y cada 4 bloques más de radio, otra. Las gafas muestran
  la velocidad de bombeo y la presión de la cámara.

| Clave | Por defecto | Qué hace |
|---|---|---|
| `cryo.plant.heliumPerLayer` | 2.0 | Helio líquido por capa a la velocidad nominal, mB/t |
| `cryo.plant.airPerLayer` | 5.0 | Aire líquido por capa a la velocidad nominal, mB/t |
| `cryo.plant.loopLoss` | 0.01 | Fracción del helio que se escapa en cada paso |
| `cryo.plant.nitrogenBonus` | 0.5 | Producción extra con preenfriamiento de nitrógeno |
| `cryo.plant.nitrogenPerHelium` | 1.0 | Nitrógeno gastado por mB de helio líquido |
| `cryo.plant.minSpeed` | 32 | RPM mínimas de los compresores |
| `cryo.plant.ratedSpeed` | 128 | RPM de producción completa |
| `cryo.plant.compressorStressPerLayer` | 10240 | Estrés por capa, SU/RPM, repartido entre los compresores |
| `cryo.plant.portTransfer` | 160 | mB por tick que un puerto de la planta mueve con lo que tiene al lado |
| `cryo.vacuumPump.minSpeed` | 32 | RPM mínimas de la bomba de vacío |
| `cryo.vacuumPump.ratedSpeed` | 128 | RPM a las que cuenta como una bomba completa |
| `cryo.vacuumPump.stress` | 2048 | Estrés de la bomba de vacío, SU/RPM |

## Calentamiento del plasma, ventanas y sonido

El inyector de haces neutros y la antena de radiofrecuencia son bloques de la
carcasa del tokamak con un eje en su cara exterior. Por debajo de su velocidad
mínima no calientan nada; desde ahí su potencia sube en línea recta hasta la
nominal, que alcanzan a la velocidad nominal. El stress que piden a la red no
cambia con la velocidad, así que un calentador lento paga más SU por megavatio.

- `plasmaHeating.neutralBeam.ratedPower` (20): potencia de calentamiento, en MW,
  de un inyector de haces neutros a su velocidad nominal.
- `plasmaHeating.neutralBeam.ratedSpeed` (128): RPM a las que el inyector da su
  potencia nominal.
- `plasmaHeating.neutralBeam.minSpeed` (32): RPM por debajo de las que el
  inyector no da nada. Tiene que ser menor que la velocidad nominal.
- `plasmaHeating.neutralBeam.stress` (32768): stress del inyector, en SU por RPM.
  Con los valores por defecto un megavatio de haz cuesta unas 1600 SU/RPM, unas
  dos veces y media lo que la turbina de plasma recupera de un megavatio de
  fusión: el tokamak necesita una ganancia Q mayor que 3 para dar energía neta.
- `plasmaHeating.neutralBeam.deuteriumPerMw` (0.02): deuterio que gastan los
  haces, en mB por tick y por MW, del tanque del tokamak. Si no hay bastante, el
  haz pierde potencia en la misma proporción.
- `plasmaHeating.radioFrequency.ratedPower` (12): potencia, en MW, que una antena
  de radiofrecuencia envía al plasma a su velocidad nominal. Un plasma con menos
  del 15% de su límite de densidad no absorbe nada, y lo absorbe todo por encima
  del 35%.
- `plasmaHeating.radioFrequency.ratedSpeed` (64): RPM a las que la antena da su
  potencia nominal.
- `plasmaHeating.radioFrequency.minSpeed` (16): RPM por debajo de las que la
  antena no da nada. Tiene que ser menor que la velocidad nominal.
- `plasmaHeating.radioFrequency.stress` (13312): stress de la antena, en SU por RPM.
- `plasmaHeating.viewportLight` (true): si las ventanas del tokamak dan luz
  mientras hay plasma (tenue en la ruptura del gas y más intensa cuanto más
  caliente está). Con `false` siguen dejando ver el plasma pero no iluminan.

El plasma dibujado, la escarcha (con los imanes por debajo de 30 K), el vapor de
los puertos de vapor de plasma y los sonidos son solo del cliente y no tienen
opciones.

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
- Los barriles y bultos de residuos que no estan almacenados: cada tick
  aleatorio (unos 68 s de media) suman a su chunk `barrelLeak` por su factor de
  fuga y por lo que el repositorio no retiene.

Cuando una lluvia radiactiva deja un chunk con gravedad 3 o mas (0,5 rad/s), la
fuente de agua de la superficie de cada columna puede volverse agua
contaminada (con probabilidad gravedad/7). Los chunks esperan en una cola y se
revisan `waterColumnsPerTick` columnas por tick (256 = un chunk). El agua
contaminada irradia 0,3 rad/s por fuente (igual que su cubo) y la centrifuga la
separa en agua limpia y residuos.

El Absorbedor de radiacion limpia el suelo: accionado desde abajo, quita cada
segundo `scrubberRate` rad/s (a 256 RPM o mas; menos si gira mas despacio) de su
chunk y de los que estan a `scrubberRadius` chunks, empezando por el mas
contaminado. Cada `scrubberWastePer` absorbidos produce un Residuo de baja
actividad en su unica ranura, que se vacia con embudos o tolvas; lleno, se para. Consume
`scrubberStress` SU por RPM.

Los barriles y demas bultos de residuos fugan y irradian segun la calidad del
repositorio que los rodea (ver "Residuos y repositorio"). `geologicalStorageMaxY`
solo se usa con `waste.repository.legacyDepthRule = true`: entonces un barril
en esa altura o mas abajo (Y=0 por defecto) cuenta como almacenado, como antes.
Los bloques que siguen esa regla estan en la etiqueta
`create_nuclearindustry:geologically_storable`.

## Reactor de agua a presión y accidentes

Un reactor de fisión con **refrigerante primario** (agua con boro: agua y boro
en la mezcladora) y un **presionador** sobre la tapa funciona como un reactor de
agua a presión en lugar de hervir el agua. Sin presionador sigue hirviendo agua
como antes, y sus puertos rechazan el refrigerante primario.

- **Presionador**: de 2 a 4 bloques de Presionador o Puerto calefactor apilados
  sobre la tapa (no sobre el controlador ni sobre un canal de barras), con una
  Válvula de alivio encima. Su burbuja de vapor fija la presión del circuito.
  Los calentadores la suben y el rociado desde la rama fría la baja. En modo
  automático los calentadores mantienen `pressureSetpoint`; la llave inglesa
  los pasa a manual, y entonces funcionan con señal de redstone.
- **Válvula de alivio**: abre a `reliefOpenPressure` y cierra a
  `reliefClosePressure`, dejando salir vapor (y refrigerante). Cada vez que
  debería cerrar puede quedarse atascada abierta (`reliefStickChance`). Una
  señal de redstone cierra su válvula de bloqueo, que la libera y también le
  impide aliviar. Los comparadores leen 15 mientras está abierta.
- **Circuito primario**: el puerto de entrada es la rama fría (acepta
  refrigerante primario) y el de salida la rama caliente (ofrece refrigerante
  primario caliente). El caudal es el que mueven las bombas de Create; cada mB
  que pasa del frío al caliente se lleva `enthalpyPerMb` de calor, menos que el
  agua que hierve directamente (0,225), así que hace falta más caudal. Con las
  bombas paradas solo queda `naturalCirculation`: el calor se queda en la
  vasija, el agua hierve, sube la presión y abre la válvula de alivio. Una
  tubería rota vacía el circuito hasta descubrir el núcleo, que se enfría mucho
  peor.
- **Realimentación**: el vapor en el núcleo, un núcleo descubierto
  (`voidWorth`) y el agua más caliente de lo normal (`temperatureCoefficient`)
  frenan la reacción en cadena.
- **Generador de vapor**: columna de 3x3 y de 4 a 8 bloques de alto, de
  Carcasa del generador de vapor con Haces de tubos dentro; el controlador va
  en una pared lateral mirando hacia fuera, delante de los tubos. Necesita un
  Puerto del generador de vapor de cada tipo (la llave inglesa los cambia):
  entrada y salida del primario, entrada de agua de alimentación y salida de
  vapor. Cada haz de tubos mueve `steamGenerator.uaPerTube` de calor por °C
  entre el primario y el punto de ebullición del secundario. El vapor que no se
  saca sube la presión del secundario (de `minPressure` a `maxPressure`) hasta
  que deja de tomar calor; sin agua de alimentación los tubos se secan.
- Lecturas: las gafas, los monitores y el Display Link muestran la presión,
  las temperaturas de las ramas caliente y fría, el margen de subenfriamiento
  y el nivel del presionador. El controlador del generador de vapor se enlaza
  desde la sala de control.
- **Presionador desde el pupitre** (solo en reactores con presionador; en los
  demás estos módulos se quedan apagados): la palanca *Calentadores de respaldo
  del presionador* enciende todos los puertos calefactores, estén en automático
  o en manual; la palanca *Rociado del presionador* abre el rociado del todo
  mientras la rama fría tenga caudal; la lámpara *Válvula de alivio abierta* se
  enciende mientras alivia; y el Módulo controlador en *Presión automática*
  sustituye a los calentadores y al rociado automáticos para mantener la
  presión de su consigna (de 0 a 250 bar en pasos de 2,5; empieza en
  `pressureSetpoint`): por debajo de ella reparte los calentadores automáticos
  como calentadores proporcionales y por encima abre el rociado. Su ajuste está
  en `controlRoom.pid.pressure.*` (ver *Instrumentos de la sala de control*).
  El pupitre repite estas órdenes dos veces por segundo; si se rompe o se
  descarga, el presionador vuelve a su control automático en 2 segundos. La
  válvula de alivio sigue actuando siempre.

### Contención

- **Edificio de contención**: cualquier recinto cerrado de bloques completos con
  un **Monitor de contención** dentro. El monitor rellena el aire que lo rodea
  (hasta `containment.maxVolume` bloques, `containment.cellsPerTick` por tick)
  y, si no llega al exterior, el edificio queda estanco. Una puerta, una losa,
  una tubería o un hueco en la pared lo abren. Se vuelve a medir al cambiar una
  pared o lo de dentro, cada 30 segundos y al hacer clic derecho en el monitor.
- **Resistencia de las paredes** (data map `containment_rating`, presión
  absoluta en bar): Hormigón de contención 6, Revestimiento de acero 8, puerta
  blindada cerrada 10, obsidiana 6, bloque de hierro 4, vidrio 1,05 y cualquier
  otro bloque completo 1,5. Para cada línea recta que va del aire de dentro al
  aire libre de fuera (como mucho 12 bloques) cuenta su capa más fuerte, y la
  más débil de esas líneas decide. Lo que está dentro del edificio (el reactor,
  máquinas) no cuenta como pared, ni tampoco el suelo apoyado en el terreno.
- **Penetración de contención**: cuenta como pared (6 bar) y deja pasar fluidos
  a lo largo de su eje, con 1000 mB de reserva en cada sentido; además empuja lo
  que lleva al bloque del otro lado.
- **Atmósfera**: el reactor que está dentro manda a la contención el vapor de la
  válvula de alivio y de una rotura, el hidrógeno de las vainas y la actividad.
  El vapor se condensa en las paredes (`containment.condensation`, más pared,
  más rápido) y el gas se enfría hacia la temperatura de las paredes. Cada mB de
  refrigerante que se evapora cuenta como `containment.molesPerMb` mol de gas.
- **Hidrógeno**: por encima del 4 % puede arder (`hydrogenIgnitionChance` por
  segundo) y a partir del 8 % arde seguro, salvo que haya más de un 55 % de
  vapor. La combustión da un pico de presión y quema a quien esté dentro. Los
  **Recombinadores autocatalíticos pasivos** lo eliminan sin llama por encima
  del 0,5 % (`containment.recombinerRate`).
- **Venteo filtrado**: forma parte de la pared; con señal de redstone abre y
  deja salir el gas mientras haya sobrepresión (`containment.ventFlow`). Sus
  filtros solo dejan pasar la actividad dividida por
  `containment.ventDecontamination`, que cae como un poco de fallout cerca.
- **Rotura**: si la presión supera la resistencia de la pared más débil, revienta
  allí (explosión y hueco, salvo con `accidents.blockDestruction = false`, en
  cuyo caso queda rota hasta que se haga clic en el monitor) y todo lo que había
  en el aire sale como fallout.
- Lecturas: el monitor es un objetivo de la sala de control (presión,
  hidrógeno, estanqueidad, venteo, actividad y volumen), los comparadores leen la
  presión respecto a la pared más débil, y el reactor que está dentro muestra la
  presión y el hidrógeno de su contención en las gafas, los monitores y el
  Display Link.

### Accidentes

Todos los reactores de fisión, también los de ebullición directa, siguen ahora
una cadena de accidente realista. Cada parte se puede apagar con
`pwr.accidents.*`.

- **Oxidación de las vainas**: por encima de `oxidationTemperature` el circonio
  reacciona con el vapor; da calor extra y dos mol de hidrógeno por mol de
  circonio, hasta agotar `zirconiumPerRod` por barra. El combustible nuevo trae
  vainas nuevas.
- **Rotura de la vasija**: un reactor a presión que pasa `vesselFailureTicks`
  ticks por encima de `vesselFailurePressure` bar rompe la vasija: el primario se
  vacía como vapor en la contención y la tapa salta con una explosión acotada.
  La vasija sigue rota (pierde refrigerante) hasta que el reactor se vuelve a
  formar a mano.
- **Explosión de vapor**: bombear agua a un núcleo seco por encima de
  `steamExplosionTemperature`, o que el corium toque agua, provoca una explosión
  (una vez por suceso).
- **Fusión del núcleo**: las barras de combustible se convierten en **Corium**
  (como mucho 16 bloques), el agua de la vasija y el resto del hidrógeno van a la
  contención y la actividad volátil se queda en ella; sin contención sale como
  fallout. El corium funde el bloque de debajo (no los que tienen la etiqueta
  `corium_resistant`, más de 1200 de resistencia a explosiones o bedrock), baja
  como mucho 32 bloques y siempre acaba como **Corium solidificado**, escoria
  muy radiactiva que se filtra al suelo como un barril de residuos. Sobre un
  **Recogedor del núcleo** se extiende y se solidifica. Al fundir hormigón dentro
  de una contención libera hidrógeno y vapor.
- `legacyMeltdown = true` vuelve al meltdown antiguo: explosión de potencia 80 y
  fallout amplio.

| Opción | Por defecto | Qué hace |
|---|---|---|
| `pwr.coldLegTemperature` | 290 | Temperatura (°C) del refrigerante que devuelve el generador de vapor |
| `pwr.coreTemperatureRise` | 35 | Subida de temperatura (°C) al cruzar el núcleo con caudal completo; la rama caliente es la fría más esto |
| `pwr.enthalpyPerMb` | 0,12 | Calor que lleva cada mB de refrigerante primario del núcleo al generador |
| `pwr.heatTransfer` | 0,02 | Enfriamiento del núcleo por °C entre el núcleo y la rama fría, en °C por tick, con el núcleo cubierto |
| `pwr.naturalCirculation` | 8 | Caudal (mB/t) que sigue cruzando el núcleo con todas las bombas paradas |
| `pwr.pressureSetpoint` | 155 | Presión (bar) que mantienen los calentadores y el rociado automáticos |
| `pwr.heaterRate` | 0,25 | °C por tick que cada puerto calefactor encendido suma a la burbuja del presionador |
| `pwr.reliefOpenPressure` | 170 | Presión (bar) a la que abre la válvula de alivio |
| `pwr.reliefClosePressure` | 160 | Presión (bar) a la que vuelve a cerrar; siempre por debajo de la de apertura |
| `pwr.reliefStickChance` | 0,05 | Probabilidad de que la válvula se quede atascada abierta cada vez que debería cerrar |
| `pwr.voidWorth` | 0,6 | Parte de la reacción en cadena que se pierde con el núcleo sin agua |
| `pwr.temperatureCoefficient` | 0,002 | Parte de la reacción que se pierde por cada °C por encima de la temperatura media normal |
| `pwr.steamGenerator.uaPerTube` | 0,3 | Calor por tick y por °C que mueve cada haz de tubos |
| `pwr.steamGenerator.minPressure` | 50 | Presión (bar) del secundario cuando se saca el vapor según se produce |
| `pwr.steamGenerator.maxPressure` | 120 | Presión (bar) del secundario con el depósito de vapor lleno; cerca de la ebullición de la rama caliente deja de tomar calor |
| `pwr.containment.maxVolume` | 32768 | Bloques de aire que puede encerrar una contención; un espacio mayor cuenta como abierto |
| `pwr.containment.cellsPerTick` | 4096 | Bloques que mide el monitor por tick, para repartir el trabajo |
| `pwr.containment.doorInterlock` | 0.2 | Sobrepresión (bar por encima del exterior) a la que una Puerta de contención en la pared de una contención estanca queda bloqueada y no se abre. 0 desactiva el bloqueo |
| `pwr.containment.molesPerMb` | 10 | Mol de gas que da cada mB de refrigerante al evaporarse en la contención (el agua real daría 55) |
| `pwr.containment.condensation` | 0,05 | Vapor que condensan las paredes, en mol por tick, por cara de pared y por bar de vapor |
| `pwr.containment.recombinerRate` | 40 | Hidrógeno (mol/tick) que quita cada recombinador, multiplicado por la fracción de hidrógeno |
| `pwr.containment.ventFlow` | 0,002 | Parte del gas que deja salir cada venteo abierto por tick y por bar de sobrepresión |
| `pwr.containment.ventDecontamination` | 100 | Factor de descontaminación del venteo filtrado |
| `pwr.containment.hydrogenIgnitionChance` | 0,1 | Probabilidad por segundo de que arda el hidrógeno entre el 4 y el 8 % |
| `pwr.accidents.blockDestruction` | true | Los accidentes pueden romper bloques (explosiones, corium, rotura de la contención) |
| `pwr.accidents.hydrogen` | true | Las vainas calientes producen hidrógeno, que puede arder en la contención |
| `pwr.accidents.corium` | true | La fusión deja corium que baja fundiendo; con false el combustible se queda como escoria en su sitio |
| `pwr.accidents.steamExplosions` | true | Agua sobre un núcleo fundido o corium en agua explota |
| `pwr.accidents.containmentRupture` | true | La contención revienta por su pared más débil si la presión la supera |
| `pwr.accidents.legacyMeltdown` | false | Vuelve al meltdown antiguo (explosión de potencia 80) |
| `pwr.accidents.oxidationTemperature` | 1200 | Temperatura (°C) del núcleo a la que empieza la oxidación de las vainas |
| `pwr.accidents.oxidationRate` | 0,5 | Circonio (mol) que pierde cada barra por tick 300 °C por encima; crece con el cuadrado del exceso |
| `pwr.accidents.zirconiumPerRod` | 500 | Circonio (mol) de las vainas de cada barra |
| `pwr.accidents.oxidationHeat` | 4 | Calentamiento del núcleo (°C) por mol de circonio oxidado por barra |
| `pwr.accidents.vesselFailurePressure` | 250 | Presión (bar) del primario por encima de la cual falla la vasija |
| `pwr.accidents.vesselFailureTicks` | 100 | Ticks por encima de esa presión antes de que se rompa |
| `pwr.accidents.steamExplosionTemperature` | 2800 | Temperatura (°C) del núcleo por encima de la cual el agua que vuelve provoca una explosión de vapor |

La probabilidad de que la válvula de alivio se atasque sigue en `pwr.reliefStickChance`.

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

```toml
[waste.repository]
legacyDepthRule = false
minScore = 0.8
depthSpan = 64
depthWeight = 0.35
hostRockWeight = 0.25
backfillWeight = 0.25
formWeight = 0.15
fluidPenalty = 0.4
probeCacheTicks = 200
```

Los residuos tienen tres clases: **baja actividad** (absorbedor de radiacion y
agua contaminada centrifugada, 0,5 rad/s), **media actividad** (el antiguo
Residuo radiactivo, 6 rad/s) y **alta actividad** (los productos de fision del
PUREX, 15 rad/s y calientes). Cada clase tiene su tratamiento: la de baja se
compacta en un Bidon de residuos compactados, la de media se cementa en la
mezcladora (residuo, arena, grava, una plancha de hierro y agua) en un Bidon de
residuos cementados, y la de alta se vitrifica en el Fundidor de vitrificacion
sobre un quemador de blaze encendido (residuo, arena y borax) en un Contenedor
de residuos vitrificados. El barril de siempre sigue aceptando residuo de media
y de alta actividad.

Cada bulto colocado (barril, bidones, contenedor) tiene su entrada en el data
map de bloques `create_nuclearindustry:waste_package`: `leak_factor` (fraccion
de `contamination.barrelLeak` que fuga en cada tick aleatorio), `emission_factor`
(fraccion de su radiactividad que emite colocado) y `form` (`raw`, `compacted`,
`cemented` o `vitrified`).

El repositorio geologico se puntua de 0 a 1 para cada bulto:

- profundidad (`depthWeight`): bloques por debajo del bloque solido mas alto de
  su columna, divididos entre `depthSpan` (maximo 1);
- roca huesped (`hostRockWeight`): fraccion de los bloques naturales a 3 bloques
  que estan en la etiqueta `create_nuclearindustry:repository_host_rock`
  (pizarra profunda, toba, basalto...). La bentonita y otros bultos no cuentan;
- relleno (`backfillWeight`): caras del bulto tapadas con bentonita (etiqueta
  `create_nuclearindustry:repository_backfill`) o con otros bultos, entre 6;
- forma (`formWeight`): sin tratar 0,3, compactado o cementado 0,7, vitrificado 1;
- menos `fluidPenalty` si hay una fuente de fluido (agua, lava...) a 4 bloques
  o menos.

Con una puntuacion de `minScore` o mas el bulto cuenta como almacenado: ni
fuga ni irradia. Por debajo, fuga `barrelLeak * leak_factor * (1 - puntuacion)`
en cada tick aleatorio y emite `radiactividad * emission_factor * (1 -
puntuacion)`. El entorno de cada bulto se vuelve a mirar como mucho cada
`probeCacheTicks` ticks. El Inspector de repositorio muestra con las gafas el
desglose del bulto mas debil a 8 bloques, da el estado a un Display Link y
saca por comparador la fraccion almacenada (0-15).

`legacyDepthRule = true` vuelve a la regla antigua: lo que esta en la etiqueta
`geologically_storable` en `contamination.geologicalStorageMaxY` o mas abajo
cuenta como almacenado, sin mirar lo que lo rodea, y lo demas fuga
`barrelLeak * leak_factor`.

## Instrumentos de la sala de control

- `chartSamplesPerPaper` (240): muestras que dibuja un Registrador gráfico con
  una hoja de papel. 240 llenan la gráfica entera; con una muestra por tick una
  hoja dura 12 segundos y con una cada 20 ticks, 4 minutos. Sin papel el
  registrador se para.
- `annunciatorInterval` (10): ticks entre dos comprobaciones de las ventanas de
  un Panel anunciador. Menos ticks detectan antes una alarma breve, a cambio de
  muestrear más a menudo las máquinas enlazadas.
- `pid.interval` (20): ticks entre dos pasos del Módulo controlador.
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
- `pid.pressure.gain` (3.0), `pid.pressure.integral` (0.3) y
  `pid.pressure.deadband` (0.004): ajuste propio del controlador en *Presión
  automática*, que da pasos cada `pid.interval` ticks como el de barras. El
  error se mide en fracciones de 250 bar (la banda muerta por defecto es
  1 bar) y la salida va de calentadores al máximo (0) a rociado al máximo (1),
  sin hacer nada en 0,5. El presionador responde en segundos, mucho antes que
  las barras, así que no tiene límite de velocidad ni término derivativo.
- `pid.fusion.gain` (2.0), `pid.fusion.integral` (0.3), `pid.fusion.deadband`
  (0.005) y `pid.fusion.rateLimit` (0.1): ajuste del controlador en *Potencia
  de fusión automática*, que también da pasos cada `pid.interval` ticks. El
  error se mide en fracciones de 1000 MW (la banda muerta por defecto es 5 MW)
  y la salida va de 0 a 1: la mitad baja fija la inyección de combustible del
  tokamak, de nada al máximo que admite, y la mitad alta sube el calentamiento
  desde el nivel que dejó el operador (nunca por debajo) hasta el máximo. El
  límite de velocidad, un 10 % de la salida por segundo, evita que el plasma
  reciba saltos de combustible. Solo actúa con el plasma en combustión; el
  arranque es cosa del operador.

Escalas: los indicadores, las plumillas del registrador y los umbrales de
alarma del circuito a presión van hasta 250 bar (donde falla la vasija),
375 °C en las ramas (el punto crítico del agua), 50 °C de subenfriamiento y
256 mB/t de caudal (dos bombas mecánicas a toda velocidad); los del generador
de vapor, hasta 256 mB/t de vapor, 300 MW y 50 °C de ΔT del primario; los de la
contención, hasta 10 bar (la pared más fuerte, una puerta blindada) y 10 % de
hidrógeno. La actividad en el aire de la contención se lee en escala
logarítmica: cinco décadas hasta lo que libera un núcleo entero.

Al enlazar un Registrador gráfico, sus plumillas se ponen solas según la
máquina: presión, rama caliente y nivel del presionador en un reactor con
presionador (temperatura y potencia en los demás); nivel, vapor y ΔT del
primario en un generador de vapor; presión, hidrógeno y actividad en una
contención; potencia de fusión, temperatura y densidad del plasma en un
tokamak. Las plumillas que haya cambiado el jugador se respetan al volver a
enlazar la misma clase de máquina.

Escalas del tokamak: potencia de fusión hasta 1000 MW, ganancia Q hasta 20 (un
plasma en ignición lleva la aguja al tope y el monitor muestra «∞»), densidad
hasta el 120 % del límite de Greenwald, cenizas hasta el 25 %, temperatura de
los imanes hasta 300 K (para ver el enfriamiento entero), calentamiento hasta
100 MW y presión de la cámara en escala logarítmica de 1e-5 Pa a la
atmósfera. Los colores del monitor salen de los umbrales del propio modelo del
plasma.

El Panel anunciador tiene plantillas para sus seis ventanas (*Núcleo*,
*Primario PWR*, *Gen. vapor* y *Contención*) con métricas, umbrales y
etiquetas; los enlaces no cambian. Los umbrales salen de la configuración
del servidor: presión alta a medio camino entre `pwr.pressureSetpoint` y
`pwr.reliefOpenPressure`, presión baja 10 bar por debajo de la consigna,
pérdida de caudal al doble de `pwr.naturalCirculation`, ΔT bajo a la mitad de
`pwr.coreTemperatureRise`, hidrógeno al 2 % y al 4 % (límite de
inflamabilidad) y presión de contención a 2 bar y al 80 % de los 6 bar del
hormigón.

La plantilla *Tokamak* vigila la inestabilidad (60 %, antes de la disrupción
al 100 %), la densidad (90 % del límite), el vacío (3e-4 Pa, media década
mejor que el límite de 1e-3 Pa del plasma), la temperatura de los imanes
(10 K, la mitad del límite superconductor de 20 K), una ganancia Q de 3 o
menos (por debajo el calentamiento cuesta más de lo que da el plasma) y el
disparo del tokamak. El vacío y la densidad solo cuentan con el plasma
encendido, los imanes solo con corriente y la ganancia solo en combustión, así
que un tokamak frío no tiene ninguna ventana encendida. Una parada del
operador no cuenta como disparo; un arranque rechazado sí, hasta el siguiente
arranque.

## Defensa civil

```toml
[civilDefense.siren]
threatRadius = 96
audibleRange = 128
plantAlarmSeconds = 120
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
- Los accidentes de una central también son amenazas, en el sitio del
  accidente: una fusión del núcleo, la rotura de la vasija, una explosión de
  vapor, la rotura de una contención y una combustión de hidrógeno hacen sonar
  durante `siren.plantAlarmSeconds` segundos (120 por defecto; 0 lo desactiva)
  las sirenas antiaéreas que tengan el accidente a `siren.threatRadius` bloques
  o menos. Cada accidente nuevo vuelve a empezar la cuenta.
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

## Reactor de fisión visible

- `reactorVisuals.rodUpdateInterval` (20): ticks entre cada actualización de lo
  que muestran los canales de combustible del reactor (combustible, brillo y
  refrigerante).
- `reactorVisuals.rodUpdatesPerTick` (64): canales de combustible que un reactor
  puede redibujar como mucho en un tick; los demás esperan al siguiente. Bájalo
  en servidores con reactores muy grandes si los cambios de aspecto se notan en
  el rendimiento.
<!-- WP-A -->

## Ciclo de combustible renovable

Dos tratamientos con ventilador se suman a los de Create. Como en ellos, manda el
último catalizador que cruza la corriente: agua y luego ácido lixivia; ácido y luego
agua lava. Funcionan con ítems en el suelo, en cintas y en depósitos, y el
catalizador no se gasta.

- **Lixiviación ácida**: el ventilador sopla a través de ácido sulfúrico (fuente o
  ácido que fluye; también los bloques de la etiqueta
  `create_nuclearindustry:fan_processing_catalysts/acid_leaching`).
- **Irradiación**: el ventilador sopla a través de un bloque radiactivo que deja pasar
  el aire, como la fuente de irradiación (o los bloques de la etiqueta
  `create_nuclearindustry:fan_processing_catalysts/irradiation`). Un bloque completo
  corta la corriente de Create, así que los bloques radiactivos macizos no sirven.

Opciones:

- `fuelcycle.irradiationMinActivity` (2): radiactividad mínima de un bloque, en
  rad/s (la de su ítem), para que un ventilador que sopla a través de él irradie
  los ítems de su corriente. Los bloques de la etiqueta de catalizadores de
  irradiación sirven siempre. La fuente de irradiación da 3.
- `fuelcycle.irradiationDosePerSecond` (10): dosis, en rad/s, que recibe una
  criatura atrapada en una corriente que irradia. A los jugadores les protege el
  blindaje que lleven puesto y el yoduro potásico, y la dosis se suma a la de la
  radiación normal; las demás criaturas enferman al pasar el umbral de
  `radiation.sicknessThreshold`. Con 0 la corriente es inofensiva.
- `fuelcycle.acidDamagePerSecond` (1): daño por segundo (medios corazones) que el
  rocío de ácido hace a una criatura atrapada en la corriente. Con 0 es inofensivo.

El combustible de torio no tiene opción propia: arranca con el 55% de la
reactividad del uranio poco enriquecido, la gana a medida que genera uranio-233
(máximo de unas 1,3 veces hacia la mitad del quemado) y dura 1,4 veces más
(`fission.fuelUnitsPerAssembly` × 1,4). Las vías renovables (roca, arena, salmuera,
agua de mar) están ajustadas para costar entre una y dos veces lo que la mena por
unidad de producto; las recetas generadas se comprueban con un test.
<!-- WP-B -->

## Rendimiento

- `performance.radiationRayCacheTicks` (20): ticks que se reutiliza el rayo de
  radiación entre un jugador y una fuente mientras ninguno de los dos se mueve. Con
  0 se lanza cada rayo cada vez.
- `explosive.blastColumnsPerTick` (600): columnas del cráter que una detonación
  nuclear excava por tick. Con menos, el trabajo se reparte en más ticks.
- `explosive.blastBlocksPerTick` (3500): bloques que una detonación nuclear quita o
  cambia como mucho por tick.
<!-- WP-D -->

## Uranio y litio de otros mods

- Materias primas: la etiqueta `create_nuclearindustry:raw_uranium` incluye
  `c:raw_materials/uranium` y `forge:raw_materials/uranium`. Se trituran como el
  uranio en bruto del mod. La espodumena usa `c:raw_materials/lithium`.
- Lingotes: `create_nuclearindustry:uranium_ingots` incluye `c:ingots/uranium` y
  `forge:ingots/uranium`. Los lingotes externos cuentan como uranio natural (0,7%)
  y se fluoran a hexafluoruro con acido fluorhidrico para entrar en la cascada.
  Un datapack puede ampliar la etiqueta para otros identificadores.
