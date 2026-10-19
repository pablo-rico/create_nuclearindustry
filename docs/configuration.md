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
fuelUnitsPerAssembly = 100.0
enrichmentReactivity = 0.25
xenonPoisoning = true
iodineHalfLifeTicks = 3600
xenonHalfLifeTicks = 5400
decayHeatFraction = 0.07
decayHeatTimeTicks = 20000
fuelBurnPerRod = 0.00075

[fission.kinetics]
sixGroups = true
maxWithdrawalRate = 100.0
freeWithdrawalMargin = 1000.0

[fission.reactivity]
fuelExcess = 12000.0
rodBankWorth = 22000.0
xenonWorthPcm = 2800.0

[fission.thermal]
ratedPowerPerRod = 50.0
steamEnergyPerMb = 4.0

[fission.thermal]
maxSuperheat = 50.0

[fission.damage]
claddingFailureTemperature = 1500.0
fuelMeltingTemperature = 2800.0
meltdownDamage = 0.75

[fusion]
deuteriumTritiumBurnRate = 0.0011
maxMajorRadius = 16
coilsPerSector = 4
magnetInputStress = 1024.0
disruptionExplosions = true

[controlRoom]
linkRange = 128

[coolingTower]
condensePerBlock = 0.5
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
largo, con una columna de rotores en el eje. Cada rotor expande 40 mB/t de vapor
(20 de vapor de plasma) y cada par de puertos entrada/salida deja pasar 160 (80);
el menor de los dos limites manda. Una bomba de Create a 256 RPM mueve 128 mB/t,
así que una línea con una bomba casi llena un par de puertos.

Con un Puerto de escape de turbina, la turbina no condensa dentro: saca el vapor
como vapor de escape hacia una torre de refrigeración y su capacidad se duplica
con el mismo caudal. Si el escape no tiene salida, la turbina se para igual que
con el tanque de condensado lleno.

Las capacidades son la contribucion de stress por RPM por cada 160 mB/t de vapor
(80 de vapor de plasma) expandidos; Create la multiplica por la velocidad. Las
turbinas giran a 64 RPM. Duplicar una capacidad duplica su output con el mismo vapor.

## Torres de refrigeración

Una torre de radio de base Rb y altura H condensa `Rb * H * condensePerBlock`
mB/t de vapor (o vapor pesado) en agua (o agua pesada), 1:1. Cada ventilador a
256 RPM o más suma `fanBoostPerFan` de esa capacidad (proporcional por debajo,
nada por debajo de 16 RPM), hasta `maxFanBoost` en total, y consume `fanStress`
SU por RPM. El radio va de 4 a 16 y la altura de `2*Rb + 4` a `5*Rb` (máximo 64).

## Fision

El núcleo sigue la **cinética puntual** de un reactor real: la reactividad
(en pcm, milésimas de porcentaje) que dejan las barras de control, el
combustible y el moderador decide si la potencia sube, baja o se queda quieta,
y los neutrones retardados hacen que lo haga despacio. Al calentarse, el
combustible (efecto Doppler), el agua y el vapor de su alrededor le quitan
reactividad, así que **sacar una barra sube la potencia un poco y el reactor se
asienta solo** en una potencia nueva, más caliente. Un reactor en marcha y
estable marca siempre una reactividad neta de casi 0 pcm (es crítico); parado,
con las barras dentro, marca un valor negativo: su margen de parada.

Como en un reactor de verdad, **el combustible nuevo tiene mucha más reactividad
de la que su propio calor puede quitarle** (unos 12000 pcm al 5 %, frente a unos
4500 que pierde entre frío y plena potencia, con el xenón). Las barras son
imprescindibles: con todas fuera, o sin barras, un núcleo con agua pasa de su
potencia nominal hasta que el combustible se daña. Si le falta agua, la parte
descubierta deja de moderar y la potencia baja, pero en cuanto el agua vuelve
recupera toda su reactividad de golpe.

**Qué se lee y por qué.** Las gafas, el panel de control, los monitores de la
sala de control (`R_REACTIVITY_*`) y la fuente de Display Link «Desglose de la
reactividad» muestran la reactividad neta separada en sus partes, en pcm y con
signo; suman la neta:

| Parte | Qué incluye | Valores típicos |
|---|---|---|
| Combustible | el exceso del combustible según enriquecimiento y quemado, más el grafito, el reflector y el agua pesada | +12000 al 5 %, +18000 al 20 %, +1500 con grafito completo |
| Barras | lo que retienen las barras metidas | hasta −`rodBankWorth` × absorción (−22000 con canales de sobra) |
| Calor | Doppler del combustible y temperatura del agua | −2000 a −3000 a plena potencia |
| Vapor | el vapor en el núcleo y el agua que falta (núcleo seco) | −450 a plena potencia; −10000 con la mitad del combustible descubierto |
| Xenón | el veneno | −1400 en equilibrio |

Así, un reactor parado en frío marca «combustible +12000, barras −22000, neta
−10000»: ese −10000 es su margen de parada, y no es un error. Al arrancar, las
barras se sacan hasta que las partes suman 0; en marcha el calor y el vapor
sustituyen a parte de las barras. Un reactor sin barras que marca −8000 de
«vapor» solo se sostiene porque le falta agua.

- **Potencia nominal**: `fission.thermal.ratedPowerPerRod` MW por cada bloque de
  barra de combustible (más con MOX). Las gafas, la GUI, los monitores, las
  alarmas, el puerto de redstone (15 = 100 %) y el controlador automático del
  pupitre trabajan en % de esa potencia nominal, así que un reactor grande y uno
  pequeño se manejan igual. El reactor puede pasar del 100 %.
- **Vapor**: sale exactamente el calor que se lleva el agua dividido por
  `fission.thermal.steamEnergyPerMb` (MW por mB/t, 4 por defecto: 600 MW son
  150 mB/t de agua entrando y de vapor saliendo). Cada intercambiador saca 160 MW
  sea cual sea ese valor; cada puerto de fluido deja pasar 160 mB/t y una bomba de
  Create a 256 RPM mueve 128 mB/t, así que un valor más alto pide menos puertos,
  bombas y turbina por MW. **La potencia térmica en régimen
  nunca pasa de lo que sale como vapor**: con pocos intercambiadores, puertos
  justos o el tanque de vapor lleno, la potencia se queda en ese techo aunque se
  saquen más barras. Lo que cambia entonces es la temperatura: el agua del núcleo
  solo admite `fission.thermal.maxSuperheat` (50 K) por encima de la ebullición;
  al llegar ahí se convierte en vapor (el término «Vapor» del desglose cae hasta
  −1500 pcm) y, una vez el agua está en ese techo, el calor que no puede salir se
  queda en el combustible, que sube hasta que su Doppler frena la reacción: unos
  500 °C por cada 1000 pcm de barras de más a partir de ahí (los primeros ~1000
  pcm los absorbe el vapor del sobrecalentamiento). Con el vapor atascado del todo (tanque lleno, salida cerrada) el vapor
  que se forma en el núcleo para la reacción en segundos, pero el calor residual
  sigue entrando en un combustible que ya no puede enfriarse: hay que liberar la
  salida. Sin agua el núcleo se seca: la reacción en cadena se para
  (`fission.reactivity.dryCoreWorth`), pero el calor residual lo sigue calentando.
  El vapor del núcleo tarda alrededor de un segundo en seguir a los cambios, así
  que una salida que se abre y cierra a cada tick (una turbina que vacía el
  tanque a trompicones) no hace oscilar la potencia.
- **Daño del combustible**: por encima de `fission.damage.claddingFailureTemperature`
  (1500 °C, el circonio de las vainas) fallan las vainas y el núcleo acumula daño
  permanente (se ve en las gafas y la GUI como «estrés del combustible», y suelta
  actividad); por encima de `fission.damage.fuelMeltingTemperature` (2800 °C) el
  combustible se funde y el daño corre. Con `fission.damage.meltdownDamage` (75 %)
  el núcleo se funde del todo (corium, ver *Accidentes*). Recargar combustible
  nuevo reduce el daño en proporción. A potencia nominal el combustible va a unos
  885 °C (`fuelRiseAtRated` por encima del agua): queda cerca de 2× de margen de
  sobrepotencia antes del daño. Los estados del reactor siguen a estos umbrales:
  *Aviso* desde el fallo de vainas (también la alarma de temperatura y el sonido),
  *Crítico* a medio camino de la fusión (2150 °C) y *Fusión* desde la temperatura
  de fusión.
- **Velocidad de las barras**: como en una central real, mientras el núcleo está
  muy por debajo de la criticidad (le faltan más de
  `fission.kinetics.freeWithdrawalMargin`, 1000 pcm) las barras salen tan rápido
  como se saquen. En los últimos 1000 pcm, la reactividad que liberan entra como
  mucho a `fission.kinetics.maxWithdrawalRate` pcm por segundo, la velocidad de
  los accionamientos. Así arrancar es rápido, pero un pistón que saca todas las
  barras de golpe no lleva el núcleo a criticidad inmediata. Meterlas es
  instantáneo. Con el margen a 0 todas las retiradas van a la velocidad limitada.
- **Tamaño**: el juego completo de barras vale `rodBankWorth` (22000 pcm) repartido
  entre todos sus bloques, más en el centro del núcleo que en los extremos. En
  un reactor con pocos bloques de barra cada uno vale miles de pcm y es un salto
  grande (de parado a pasarse de potencia); con canales altos o varios canales
  cada bloque mueve la potencia un 5–20 %.
- **Margen de parada**: con suficientes canales de barras (`boronAbsorption` ×
  canales / combustible ≥ 1) las barras dentro paran en frío el uranio hasta
  un 20 % con grafito. Con menos canales, o con combustible muy enriquecido, puede
  que ni con todas las barras dentro se pare: con bloques de barra para la mitad
  del combustible (absorción 0,75, banco de 16500 pcm) el uranio al 9 % con
  grafito y reflector, el MOX con grafito y el 20 % ya no se paran en frío.
- **Canales de barras**: una columna interior cuenta como canal si su tapa es
  aire o una pieza de pistón, si tiene un bloque de barra en la columna o en el
  espacio de recorrido por encima de la tapa, o si una contraption con barras la
  atraviesa. Una columna de reflector, grafito o solo aire bajo una tapa de
  carcasa no es canal: el reactor lo nota en unos ticks, pierde esa capacidad de
  barras (y gana la reflexión o moderación) sin enfriarse ni desformarse. Las
  barras movidas con pistones conservan su canal y nunca hacen revalidar. Un
  canal con la tapa abierta (barras en pistón) que se rellena de reflector o
  grafito no es ni canal ni columna válida: el reactor se para y el controlador
  dice que hay que tapar esa columna con carcasa; al taparla vuelve a formarse
  con la columna nueva.

| Opción | Por defecto | Qué hace |
|---|---|---|
| `boronAbsorption` | 1,5 | Absorción de las barras: se multiplica por la proporción de canales de barras frente a canales de combustible y fija qué parte de `rodBankWorth` tiene el reactor |
| `fuelUnitsPerAssembly` | 100 | Unidades de un elemento al 5 % (LEU); uno más enriquecido dura `(enriquecimiento / 5)^0,66` veces más |
| `enrichmentReactivity` | 0,25 | Reactividad extra por cada vez que se dobla el enriquecimiento por encima del 5 % (multiplica `fuelExcess`) |
| `fuelBurnPerRod` | 0,00075 | Unidades que quema cada barra por tick a potencia nominal; 0 desactiva el agotamiento |
| `xenonPoisoning` | true | La fisión genera xenón-135; tras bajar la potencia o un SCRAM sigue subiendo unos minutos (el "pozo de yodo") |
| `iodineHalfLifeTicks` / `xenonHalfLifeTicks` | 3600 / 5400 | Semividas del yodo-135 y del xenón-135 |
| `decayHeatFraction` | 0,07 | Parte del calor de fisión que llega después como calor residual |
| `decayHeatTimeTicks` | 20000 | Escala de tiempo del calor residual: cinco grupos de productos de fisión que decaen en 1/100, 1/10, 1, 10 y 100 veces este tiempo (y tardan lo mismo en acumularse). Un núcleo que ha funcionado horas baja a menos de la mitad en 100 s, a menos de un tercio en 17 min y a una quinta parte en 3 h |
| `fission.kinetics.sixGroups` | true | Seis grupos de neutrones retardados (true) o uno (false) |
| `fission.kinetics.delayedTimeScale` | 1 | Acelera los neutrones retardados; más alto hace el reactor más rápido de manejar |
| `fission.kinetics.promptGenerationTime` | 0,001 | Tiempo de generación de los neutrones inmediatos (s); solo cuenta cerca de la criticidad inmediata |
| `fission.kinetics.sourceStrength` | 0,001 | Fuente de neutrones que mantiene vivo un núcleo parado |
| `fission.kinetics.maxWithdrawalRate` | 100 | pcm por segundo que libera como mucho una retirada de barras cerca de la criticidad |
| `fission.kinetics.freeWithdrawalMargin` | 1000 | pcm por debajo de la criticidad a partir de los que las barras salen sin límite de velocidad (0 = siempre limitadas) |
| `fission.kinetics.maxRelativePower` | 10 | Potencia máxima, en veces la nominal |
| `fission.reactivity.fuelExcess` | 12000 | Exceso de reactividad del uranio nuevo al 5 % (pcm) |
| `fission.reactivity.burnupDefect` | 0,6 | Parte de ese exceso que se pierde al final de la vida del combustible (el torio no la pierde) |
| `fission.reactivity.emptyChannelWorth` | 24000 | pcm que se pierden con todos los canales de combustible vacíos |
| `fission.reactivity.rodBankWorth` | 22000 | Valor de un juego completo de barras (pcm) |
| `fission.reactivity.rodWorthShape` | 0,5 | Cuánto más valen las barras en el centro del núcleo (0 igual en toda su longitud, 1 curva en S completa) |
| `fission.reactivity.graphiteWorth` | 1500 | pcm que suma el grafito alrededor de cada columna de combustible |
| `fission.reactivity.reflectorWorth` | 1000 | pcm que suma un anillo interior entero de berilio |
| `fission.reactivity.heavyWaterWorth` | 500 | pcm que suma el agua pesada como refrigerante |
| `fission.reactivity.xenonWorthPcm` | 2800 | pcm que quita un núcleo totalmente envenenado; a plena potencia se queda cerca de la mitad |
| `fission.reactivity.dopplerCoefficient` | -2 | pcm por kelvin del combustible |
| `fission.reactivity.moderatorCoefficient` | -4 | pcm por kelvin del agua |
| `fission.reactivity.voidCoefficient` | -15 | pcm por % de vapor en el núcleo |
| `fission.reactivity.graphiteModeratorCoefficient` / `graphiteVoidCoefficient` | -1 / +3 | Los mismos coeficientes con grafito en todas las columnas: el vacío pasa a ser positivo |
| `fission.reactivity.heavyWaterVoidCoefficient` | -5 | Coeficiente de vacío con agua pesada |
| `fission.reactivity.damageWorth` | 2000 | pcm que pierde un núcleo totalmente dañado |
| `fission.reactivity.dryCoreWorth` | 20000 | pcm que pierde un núcleo moderado por agua sin agua (menos con grafito) |
| `fission.thermal.ratedPowerPerRod` | 50 | MW de cada bloque de barra de combustible a potencia nominal |
| `fission.thermal.fuelRiseAtRated` | 600 | Kelvin que el combustible está por encima de su agua a potencia nominal |
| `fission.thermal.fuelHeatCapacity` | 0,5 | Capacidad calorífica del combustible de una barra (MJ/K); más alta, el núcleo se calienta y enfría más despacio |
| `fission.thermal.coolantHeatCapacity` / `structureHeatCapacity` | 1,5 / 0,2 | Capacidad calorífica del agua y del acero alrededor de cada barra (MJ/K) |
| `fission.thermal.boilingTemperature` | 285 | Temperatura (°C) a la que hierve el agua del núcleo |
| `fission.thermal.steamEnergyPerMb` / `heavySteamEnergyPerMb` | 4 / 6,2 | MW que se lleva cada mB/t de vapor (agua / agua pesada) |
| `fission.thermal.ratedVoid` | 0,3 | Vapor en un núcleo que hierve su potencia nominal |
| `fission.thermal.chfMargin` | 1,25 | Flujo crítico sobre lo que dan los intercambiadores: por encima el combustible se envuelve en vapor y se enfría mucho peor |
| `fission.thermal.maxFuelTemperature` | 4500 | El combustible nunca pasa de aquí (°C) |
| `fission.thermal.maxSuperheat` | 50 | Kelvin por encima de la ebullición a los que puede llegar el agua del núcleo cuando el vapor no sale; a partir de ahí el calor se queda en el combustible |
| `fission.damage.claddingFailureTemperature` | 1500 | °C a los que fallan las vainas y empieza el daño; desde aquí el reactor marca *Aviso* |
| `fission.damage.fuelMeltingTemperature` | 2800 | °C a los que se funde el combustible |
| `fission.damage.claddingRate` | 0,004 | Daño por segundo 800 K por encima del fallo de vainas (crece con el cuadrado) |
| `fission.damage.meltingRate` / `meltingRateMax` | 0,01 / 0,1 | Daño por segundo al empezar a fundirse y 1200 K por encima |
| `fission.damage.meltdownDamage` | 0,75 | Daño al que el núcleo se funde del todo |

**Al actualizar**: las claves antiguas `uraniumBurnPerNeutron`,
`graphiteModeratorBonus`, `berylliumReflectorBonus` y `xenonWorth` ya no existen
(sus sustitutas tienen otro nombre porque usan otras unidades), y un reactor que
se guardó con el modelo antiguo carga con el combustible a 1500 °C como mucho y
sin daño, y se reasienta en su nueva potencia.

Las barras de control solo se mueven físicamente, con pistones de Create. El
SCRAM (botón del panel o SCRAM automático, activado por defecto a 1400 °C, justo
por debajo del fallo de vainas) no las mueve: enciende la señal de los Puertos
de redstone en modo SCRAM hasta que se rearma, y esa señal es la que hay que
llevar a los pistones. El umbral se ajusta en el panel (no es una opción de
configuración) y solo se puede rearmar cuando el núcleo está 200 °C por debajo.
Los reactores guardados conservan el umbral que tuvieran (2800 °C en partidas
antiguas: a esa temperatura el combustible ya se funde, conviene bajarlo). Las
ventanas «CORE TEMP HI» y «HI-HI» del preajuste del anunciador quedan a 1000 y
1200 °C.

**Al actualizar la física térmica**: la config guardada conserva
`claddingFailureTemperature = 2000`; para tener el daño nuevo hay que ponerla a
1500 (o borrar la clave). `maxSuperheat` se añade solo.

Cada canal de combustible aloja un elemento. El reactor solo acepta elementos
combustibles al 3% o mas; el uranio metalico suelto ya no es combustible. El
combustible gastado conserva el enriquecimiento con el que entro.

### Parada segura

Un SCRAM para la reacción en cadena, pero no el calor: el combustible sigue
dando **calor residual** (unos pocos por ciento de su potencia) durante mucho
rato. Para dejar un reactor apagado del todo hay que mantenerlo subcrítico y
llevarse ese calor.

- **Barras ≥ combustible.** Con al menos tantos bloques de barras de control
  insertados como bloques de barras de combustible, cualquier núcleo queda
  subcrítico incluso en frío y sin xenón. Ya no hay tope de absorción a 1 banco:
  los núcleos con más de 2 barras por cada 3 de combustible tienen barras más
  fuertes (hasta `fission.reactivity.maxRodAbsorption` bancos, solo una
  salvaguarda); los demás no cambian. El controlador PID del pupitre escala sus
  ganancias para no oscilar en esos núcleos.
- **Boro soluble.** El **Ácido bórico** (mezcladora de Create: 1000 mB de agua +
  1 sal de bórax + 100 mB de ácido sulfúrico) entra por un puerto de entrada: su
  agua va al tanque y su boro frena el núcleo (`worthPerPpm` pcm por ppm, por la
  parte del núcleo cubierta). El boro no se va con el vapor (al hervir se
  concentra, hasta `maxPpm`; el resto precipita y se redisuelve cuando vuelve el
  agua): **solo la purga lo saca**, que es como se diluye para rearrancar. En un
  PWR el ácido bórico y el agua de reposición entran como refrigerante primario
  (el agua diluye el boro). Un núcleo de agua pesada rechaza el ácido bórico. La
  fusión del núcleo borra el boro.
- **Alimentar y purgar.** Hervir nunca enfría el agua por debajo de unos 100 °C.
  Con la reacción en cadena parada, el agua fría que se bombea por un puerto de
  entrada **por encima del vapor que se produce** se lleva calor
  (`feedwaterHeatCapacity` MJ por mB y kelvin), y los puertos de salida, cuando
  ya no queda vapor, **purgan** el tanque de refrigerante: Agua caliente (Agua
  pesada caliente en un núcleo de agua pesada), o **Agua contaminada** si el
  combustible está dañado (desde `contaminatedDamage`). La purga nunca baja el
  agua del nivel que cubre el núcleo. La purga de un PWR (letdown) sale también
  como agua caliente, no como refrigerante primario, para que pase por una torre
  de refrigeración y vuelva como agua de reposición que diluye el boro. El puerto
  de salida ofrece dos tanques: el vapor (o la purga) y, mientras quede vapor, la
  purga, para que una tubería ya llena de agua purgada siga purgando. El agua
  caliente no se acepta de vuelta por la entrada: hay que enfriarla antes en una
  torre, que la devuelve como agua. Un núcleo de 6 barras con 50 mB/t pasa de
  SCRAM en caliente a menos de 100 °C en uno o dos minutos; mantenerlo frío pide
  unos 6 mB/t. En un PWR frío el presionador pierde nivel y se encienden PZR LO y
  PRESS LO: es lo normal en una parada fría despresurizada.
- **Estados de parada.** *Subcrítico*: apagado, pero sin margen suficiente
  (`requiredMargin`): volverá a arrancar al enfriarse o al decaer el xenón.
  *Parada en caliente*: con margen, aún caliente. *Parada en frío*: con margen,
  combustible y agua por debajo de 100 °C y el núcleo cubierto. El logro
  «Parada en frío» se da al llegar a una parada en frío de verdad, y «Barras de
  control líquidas» al echar ácido bórico a un reactor.
- **Dónde se ve.** Las gafas muestran la sección *Parada* (estado, margen en
  frío, boro, barras frente a combustible, calor evacuado frente a residual) y
  avisan del calor residual sin evacuar, del núcleo descubierto y de la purga de
  agua contaminada; las del puerto de salida dicen si purga. Display Link:
  «Estado de parada», «Margen de parada en frío» (con todas las barras dentro),
  «Boro», «Núcleo cubierto», «Calor evacuado / residual», «Barras dentro /
  combustible» y «Hidrógeno en el edificio»; el desglose de reactividad añade
  `B` (boro). Monitores: filas *Margen parada* y *Boro*; agujas y displays LED:
  *Margen de parada en frío* y *Boro disuelto*; lámpara de pupitre *Parada en
  frío*. Alarmas: margen de parada en frío por debajo de (50 pcm por paso),
  reactor en parada en frío, núcleo cubierto por debajo de, y calor residual
  evacuado por debajo de (solo apagado y con al menos 0,5 MW de calor residual).
  El preajuste **Parada** del anunciador pone SCRAM, SDM LO (al margen
  requerido), DECAY HEAT (90 %), CORE LEVEL LO (99 %), H2 HI (4 %) y COLD
  SHUTDOWN.

| Opción | Por defecto | Qué hace |
|---|---|---|
| `fission.reactivity.maxRodAbsorption` | 4,0 | Bancos de barras (`fission.reactivity.rodBankWorth`) que pueden valer como mucho las barras de un núcleo; solo una salvaguarda |
| `fission.thermal.feedwaterHeatCapacity` | 0,0006 | Calor (MJ) que se lleva cada mB de agua de alimentación por kelvin que se calienta; solo cuenta el agua por encima del vapor producido |
| `fission.boron.worthPerPpm` | 5,0 | Reactividad (pcm) que quita cada ppm de boro disuelto |
| `fission.boron.solutionPpm` | 7000 | Boro (ppm) del ácido bórico |
| `fission.boron.maxPpm` | 10000 | Boro (ppm) que admite disuelto el agua del núcleo; el resto precipita hasta que haya agua |
| `fission.coolant.bleed` | true | Los puertos de salida de un reactor parado purgan el tanque de refrigerante cuando no hay vapor |
| `fission.coolant.contaminatedDamage` | 0,05 | Daño del núcleo (0-1) desde el que el agua purgada sale contaminada |
| `fission.shutdown.requiredMargin` | 1000 | Margen de parada en frío (pcm) que necesita un reactor parado para contar como parada segura |

## Sala de control

- `linkRange`: distancia maxima, en bloques, entre un monitor, panel o alarma y
  lo que tiene enlazado. El objetivo ademas tiene que estar en un chunk cargado
  de la misma dimension; si no, la pantalla muestra "Sin señal".

### Configurar una pantalla con la llave

Clic derecho con la llave inglesa sobre cualquier bloque de un Monitor de sala
de control abre su configuracion (agachado, la llave hace lo de siempre; con la
mano vacia se sigue cambiando de pagina). La configuracion es de la pantalla
entera: la guarda su bloque de abajo a la izquierda y, si la pantalla crece o se
une a otra, pasa al nuevo origen. No hay claves en el archivo de config; todo se
elige en el juego:

- **Titulo**: hasta 24 caracteres, sin codigos de formato. Vacio, la pantalla
  muestra el tipo de maquina, como antes.
- **Disposicion**: *Automatica* (lo de siempre: en una pantalla grande, lecturas
  y grafica en dos paginas, juntas a partir de 3x2 bloques; en un solo bloque,
  una lectura por pagina), *Solo lecturas*, *Solo grafica* o *Lecturas y
  grafica* (juntas en cualquier pantalla de dos bloques o mas: lado a lado si es
  mas ancha que alta, una encima de otra si no; un solo bloque muestra la grafica
  despues de sus lecturas).
- **Estilo**: como se ve la pantalla. Cada estilo reparte las mismas lecturas y
  la misma grafica a su manera:
  - *Estandar*: la de siempre, cristal verde oscuro con lista y grafica.
  - *Ordenador de proceso*: la pagina de un ordenador de planta de los 80. Banda
    azul con la sigla de la maquina y la hora, paneles «DATOS DE PLANTA» y
    «TENDENCIA» con marco, tabla con puntos guia, unidades en columna, barras
    segmentadas y avisos AVS/ALM (ALM parpadea), grafica con ejes y un pie con
    el estado, cuantas alarmas hay y la pagina.
  - *Panel sinoptico*: un panel cableado de los 70. Placa grabada con tornillos,
    el estado en una ventana anunciadora, una rejilla de instrumentos (la placa
    se enciende en ambar con un aviso y en rojo intermitente con una alarma,
    medidor digital y medidor de canto) y un registrador de papel.

  Los instrumentos del panel sinoptico y la tabla del ordenador de proceso
  ocupan mas que una fila de la lista: si no caben todas las lecturas, la
  pantalla las reparte en paginas y se pasa de una a otra con clic, como
  siempre (el numero de pagina sale en pantalla).
- **Grafica A / Grafica B**: que dibuja cada linea de la grafica de dos minutos.
  Se puede elegir cualquier lectura de medidor de la maquina enlazada, o *Por
  defecto* (la pareja habitual: temperatura y potencia en un reactor, etc.).
  Cambiarlas borra la grafica.
- **Grafica C**: una tercera linea, *Ninguna* por defecto. Se elige igual que
  las otras dos, con su propia escala, y sale en la leyenda solo cuando esta
  elegida. Cambiarla solo vuelve a empezar su propia linea: las otras dos
  conservan su historia.
- **Lecturas**: la lista de filas de la maquina con su valor en vivo. Haz clic
  en una para elegirla y usa los botones de la derecha para mostrarla u
  ocultarla y subirla o bajarla; el ultimo boton vuelve al orden habitual con
  todas visibles. Las filas que no aplican (dano del combustible sin dano,
  filas del circuito presurizado en un reactor de ebullicion directa, causa del
  disparo de un tokamak sin disparar) siguen sin salir aunque esten visibles.
  En un solo bloque, cada pagina es una de las lecturas visibles.

El servidor comprueba cada cambio: el jugador tiene que estar al alcance del
bloque, y solo se aceptan filas y series del tipo de maquina enlazada.

### Configurar un módulo de pupitre con la llave

Clic derecho con la llave inglesa sobre un módulo de un Pupitre de control abre
su configuración (agachado, la llave quita solo ese módulo; fuera de los
módulos recoge el pupitre entero). Tampoco tiene claves en el archivo de config:

- **Cabecera**: el tipo de módulo, la máquina a la que está enlazada la consola
  (pasa el ratón por encima para ver su posición) y su estado. Debajo, en verde
  si la función elegida sirve para esa máquina y en rojo si no hace nada en ella
  y por qué (necesita otra máquina, el reactor no tiene presionador, la alarma o
  la lectura elegida es de otra máquina).
- **Función**: la lista de funciones del módulo, con una descripción al pasar el
  ratón. Las que la máquina enlazada no puede usar salen en gris con lo que
  necesitan y no se pueden elegir. Una consola sin enlazar (o sin señal) muestra
  todas, para poder montar el pupitre antes de enlazarlo.
- **Alarma / Lectura**: en un piloto de alarma o un medidor, la lista de las
  alarmas o lecturas que tiene esa máquina, cada una con su valor en vivo (la
  alarma elegida sale en rojo si con el umbral actual ya encendería el piloto).
  Si la elección guardada no es de esa máquina sale en gris al principio de la
  lista. Al pasar a piloto de alarma o a medidor se elige la primera que sirve.
- **Controlador**: en un módulo controlador, el botón de automático / en espera,
  qué hace y la línea de consigna con lo que pide ahora.
- **Umbral, consigna o nivel**: con la rueda del ratón sobre la casilla; 5 por
  paso y 1 con Mayús (el nivel de redstone va de 0 a 15 de 1 en 1).
- **Etiqueta**: hasta 16 caracteres, sin códigos de formato.
- **Salida**: cuatro botones, ninguna, bajo el pupitre, por Redstone Link o por
  Enlace de redstone avanzado. Con Redstone Link, las dos casillas de la columna
  izquierda fijan la frecuencia (clic con un objeto; con la mano vacía se
  borra). Con el Enlace avanzado, esa columna muestra una casilla para escribir
  el código de cuatro letras o cifras. A la derecha del inventario,
  la señal que saca el módulo ahora mismo (0-15).

Mientras alguien tiene esta pantalla abierta, el primer pupitre de la consola
reenvía su muestra cada segundo para que los valores estén al día. El servidor
comprueba cada cambio como antes: el jugador tiene que estar al alcance, el tipo
de módulo tiene que admitir la función y la etiqueta se recorta; una función de
otra máquina se guarda (por si se cambia el enlace) pero el módulo sigue apagado.

### Mesa de control, panel de pared y display LED

No tienen claves en el archivo de config. Los módulos se colocan, se configuran
con la llave y se accionan igual que en un pupitre:

- **Mesa de control**: cuatro módulos en dos filas sobre un tablero plano a la
  altura del raíl del pupitre (15,5 px). Las mesas una junto a otra se unen.
- **Panel de control de pared**: una placa que se cuelga en la cara de un bloque
  sólido, con cuatro módulos en dos filas. Si se rompe el bloque de detrás, el
  panel cae con sus módulos. Un medidor en la pared dibuja la esfera de frente.
- **Consola**: pupitres, mesas y paneles de pared que se tocan de lado forman
  una sola consola con un solo enlace; los paneles de una misma pared también
  se unen hacia arriba y hacia abajo. Los remates de los extremos solo
  desaparecen entre bloques del mismo tipo.
- **Display LED**: muestra la lectura elegida en cifras con su unidad. Los
  displays LED seguidos en una fila se unen en uno más ancho, también entre
  bloques del mismo tipo unidos (no en las esquinas del pupitre), hasta 8
  módulos. Manda el de más a la izquierda visto desde el operador: la llave
  sobre cualquiera de sus módulos abre su configuración (lectura y etiqueta).
  Un valor que no cabe se lee `OL` y una lectura que la máquina no tiene, con
  guiones.
- **Panel de la sala de control**: al mirar un módulo aparece a la derecha de
  la pantalla (sin gafas) su función o etiqueta, el valor en unidades (también
  en los medidores), la consigna o el estado, adónde va su redstone (con el
  código o los objetos de la frecuencia) y la señal; mirando el resto del
  bloque, a qué está enlazada la consola, cuántos bloques tiene y cuántos
  módulos lleva ese bloque. Mirando un Enlace de redstone avanzado, su código,
  modo y señal. El mismo panel sale al mirar el controlador de un reactor de
  fisión o de un tokamak (agachado, sus detalles). Se desactiva en la config del
  cliente con `hud.panel = false`.

### Pulsador con tapa y módulos de pantalla

Tampoco tienen claves de config:

- **Pulsador con tapa**: SCRAM, parada del tokamak o pulso de redstone bajo una
  tapa abatible. El primer clic levanta la tapa y el siguiente pulsa; un clic
  agachado la cierra, y se cierra sola a los 5 segundos.
- **Barra gráfica**: la lectura elegida en una columna de 20 LED, verde, ámbar
  en el 15 % por debajo del nivel de alarma y roja desde él. El nivel de alarma
  (90 % por defecto) se fija con la rueda en su pantalla de configuración.
- **Registrador de tendencia**: la lectura como una línea en sus últimas 48
  muestras, una cada 1, 5 o 20 segundos (elegido en su pantalla). Las muestras
  sin lectura dejan un hueco.
- **Mapa del núcleo**: el interior de un reactor de fisión visto desde arriba,
  con el norte arriba: combustible de verde a rojo según se quema, canales de
  control que se llenan de azul según entran las barras, moderador, reflector e
  intercambiadores.
- **Sección del plasma**: un corte del tokamak con su plasma: color según la
  fase, brillo según la temperatura, tamaño según la densidad, temblor según la
  inestabilidad, líneas de campo según el campo magnético y bobinas encendidas
  con corriente. El marco se pone rojo tras un disparo.
- Cuatro mapas del núcleo o cuatro secciones del plasma que llenan un pupitre,
  mesa o panel de pared forman una pantalla grande que manda el del hueco 0; la
  llave sobre cualquiera abre su configuración.

### Enlace de redstone avanzado

No tiene claves propias: el alcance es el de los Redstone Link de Create
(`logistics.linkRange` en la config del servidor de Create). Se coloca en
cualquier cara sólida. Clic derecho abre una casilla para el código (cuatro
letras o cifras; se guarda en mayúsculas y el servidor descarta cualquier otro
carácter); clic derecho agachado con la mano vacía cambia entre transmisor y
receptor. Un transmisor manda la señal más fuerte que le llega por cualquier
lado; cada receptor saca la señal más fuerte de los transmisores con su código
dentro del alcance, también al bloque en el que está puesto. Sin código completo
no transmite ni recibe.

### Caja servo

No tiene claves propias. Es una caja de eje encapsulado, como el cambio de
marcha de Create: cualquiera de sus dos extremos toma el giro y el otro mueve
los Pistones mecánicos (normales o pegajosos) y las Poleas de cuerda que haya
detrás, a través de ejes, cajas de engranajes, ruedas dentadas o cambios de
marcha. Lee la señal de redstone más fuerte de cualquier cara y lleva el actuador
más cercano a esa parte de la carrera:

- **Carrera** (un par de lados, rueda del ratón): de 1 a 64 bloques, 8 por
  defecto. Se limita a lo que llega el actuador (los postes de un pistón, la
  cuerda de una polea hasta el suelo o el límite de cuerda de Create).
- **Con más señal** (el otro par de lados): *Extiende más* (0 recogido, 15 al
  final de la carrera) o *Recoge más* (al revés).
- El objetivo es `señal / 15 × carrera`, redondeado a bloques enteros: con 4
  bloques, señal 8 da 2 y señal 12 da 3.

Mueve el actuador bloque a bloque con el mismo mecanismo que el Engranaje
secuenciado de Create, así que se para justo en el bloque y lo deja asentar sus
bloques; luego deja quieta la salida. Si la señal o la velocidad cambian a mitad
de camino, se para y vuelve a calcular. Si algo bloquea el movimiento, se queda
en «Bloqueado» y lo reintenta cada pocos segundos. Todos los actuadores que giran
a la misma velocidad que el más cercano se mueven igual. Un comparador lee dónde
está el actuador, en la misma escala que la señal, y mirándolo se ven la señal,
el objetivo, la posición y los ajustes a la derecha de la pantalla.

Para mover barras de control desde la sala: una rueda de un pupitre en
*Objetivo de las barras* saca ese objetivo como señal de 0 a 15; con la salida
bajo el pupitre (o por un enlace) hasta la caja servo, el pistón de las barras
queda en esa parte de su recorrido.

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

- **Pozo de helio**: el primer paso de la cadena (helio gaseoso → planta
  criogénica → helio líquido). Un bloque movido por un eje que entra por la
  cara trasera; el frontal, con el manómetro, mira al jugador al colocarlo.
  Saca de la roca `cryo.heliumWell.rate` mB/t de helio gaseoso a la velocidad
  nominal (`cryo.heliumWell.ratedSpeed`): con los valores por defecto,
  0,02 mB/t, es decir 24 mB por minuto. Por debajo de la nominal produce en
  proporción a las RPM y por debajo de `cryo.heliumWell.minSpeed`, nada; si
  la red está sobrecargada tampoco. Carga la red con `cryo.heliumWell.stress`
  SU/RPM. Guarda hasta 1000 mB y cada tick empuja hasta
  `cryo.heliumWell.transfer` mB a los tanques o máquinas que tenga al lado
  (por cualquier cara salvo la del eje); también se puede vaciar con
  tuberías, pero no admite fluidos. Con el tanque lleno deja de extraer. Las
  gafas muestran el ritmo en mB/min, el tanque y el aviso de velocidad
  insuficiente. Lleva su helio a un puerto de *Entrada de helio caliente* de
  la planta criogénica.
- **Celda de helio líquido**: un Surtidor llena una Célula de gas vacía con
  250 mB de helio líquido y un Escurridor la vuelve a vaciar, para
  llevar helio líquido a mano.
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
| `cryo.heliumWell.rate` | 0.02 | Helio gaseoso que saca el pozo a la velocidad nominal, mB/t |
| `cryo.heliumWell.minSpeed` | 32 | RPM mínimas del pozo de helio |
| `cryo.heliumWell.ratedSpeed` | 128 | RPM de producción completa del pozo |
| `cryo.heliumWell.stress` | 1024 | Estrés del pozo de helio, SU/RPM |
| `cryo.heliumWell.transfer` | 40 | mB por tick que el pozo empuja a lo que tiene al lado |

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
| `/nuclear spawn <estructura> [pos] [fuelled]` | Coloca una máquina ya formada con su esquina noroeste inferior en `pos` (o dos bloques al este y al sur de quien lo ejecuta), vaciando antes el espacio que ocupa: `reactor` (5x5x5; con `fuelled`, seis ensamblajes al 5 % y el tanque de agua lleno), `turbine` y `fusion_turbine` (5x5x8), `cooling_tower` (radio 4, 12 capas), `tokamak` (radio 4, con motor creativo en el accionamiento de imanes), `accelerator` (linac de 8 cavidades con sus tres máquinas), `steam_generator` y `cryoplant` (3x3x4) y `containment` (sala de 7x6x7 con monitor). Son las mismas estructuras que prueban los tests |
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
  agua que hierve directamente, así que hace falta más caudal. El núcleo le da
  al primario el calor que pasa del combustible al agua (en MW), y la
  temperatura media del primario es la que ve el núcleo. Con las
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
  una tubería o un hueco en la pared lo abren. Si el relleno llega a aire sin
  nada encima hasta el cielo, el edificio está abierto sin más, aunque cerca
  haya chunks sin cargar. Se vuelve a medir al cambiar una
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

- **Oxidación de las vainas**: por encima de `oxidationTemperature` (medida en
  la vaina, entre el combustible y su agua) el circonio reacciona con el vapor;
  da calor extra y dos mol de hidrógeno por mol de circonio, hasta agotar
  `zirconiumPerRod` por barra. El combustible nuevo trae vainas nuevas.
- **Daño del combustible**: por encima de 1500 °C el combustible se daña (ver
  *Fision*); las vainas rotas sueltan parte de su actividad y el chat avisa a
  los jugadores cercanos.
- **Rotura de la vasija**: un reactor a presión que pasa `vesselFailureTicks`
  ticks por encima de `vesselFailurePressure` bar rompe la vasija: el primario se
  vacía como vapor en la contención y la tapa salta con una explosión acotada.
  La vasija sigue rota (pierde refrigerante) hasta que el reactor se vuelve a
  formar a mano.
- **Explosión de vapor**: bombear agua a un núcleo seco por encima de
  `steamExplosionTemperature`, o que el corium toque agua, provoca una explosión
  (una vez por suceso).
- **Fusión del núcleo**: con el 75 % del combustible dañado
  (`fission.damage.meltdownDamage`) las barras de combustible se convierten en **Corium**
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
| `pwr.heatTransfer` | 0,02 | Obsoleta: el núcleo pasa su calor al primario según `fission.thermal.*` |
| `pwr.naturalCirculation` | 8 | Caudal (mB/t) que sigue cruzando el núcleo con todas las bombas paradas |
| `pwr.pressureSetpoint` | 155 | Presión (bar) que mantienen los calentadores y el rociado automáticos |
| `pwr.heaterRate` | 0,25 | °C por tick que cada puerto calefactor encendido suma a la burbuja del presionador |
| `pwr.reliefOpenPressure` | 170 | Presión (bar) a la que abre la válvula de alivio |
| `pwr.reliefClosePressure` | 160 | Presión (bar) a la que vuelve a cerrar; siempre por debajo de la de apertura |
| `pwr.reliefStickChance` | 0,05 | Probabilidad de que la válvula se quede atascada abierta cada vez que debería cerrar |
| `pwr.voidWorth` | 0,6 | Reactividad que se pierde con el núcleo sin agua (por 25 da el coeficiente de vacío en pcm por %) |
| `pwr.temperatureCoefficient` | 0,002 | Reactividad que se pierde por °C de agua (por 2000 da el coeficiente en pcm por kelvin) |
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
| `pwr.accidents.oxidationTemperature` | 1200 | Temperatura (°C) de las vainas a la que empieza su oxidación |
| `pwr.accidents.oxidationRate` | 0,5 | Circonio (mol) que pierde cada barra por tick 300 °C por encima; crece con el cuadrado del exceso |
| `pwr.accidents.zirconiumPerRod` | 500 | Circonio (mol) de las vainas de cada barra |
| `pwr.accidents.oxidationHeat` | 1,2 | Calor de cada mol de circonio oxidado por barra, en kelvin del combustible de esa barra; las vainas enteras suman como mucho unos 600 K |
| `pwr.accidents.vesselFailurePressure` | 250 | Presión (bar) del primario por encima de la cual falla la vasija |
| `pwr.accidents.vesselFailureTicks` | 100 | Ticks por encima de esa presión antes de que se rompa |
| `pwr.accidents.steamExplosionTemperature` | 2800 | Temperatura (°C) del núcleo por encima de la cual el agua que vuelve provoca una explosión de vapor |

La probabilidad de que la válvula de alivio se atasque sigue en `pwr.reliefStickChance`.

### Hidrógeno en el edificio del reactor

Sin una contención estanca, el hidrógeno de las vainas sobrecalentadas (por
encima de 1200 °C) ya no se pierde siempre en el cielo: si el reactor está en
**cualquier sala cerrada de bloques normales**, se acumula bajo el techo. Al
aire libre se dispersa, como antes. El vapor de las fugas lo diluye, los
recombinadores dentro de la sala lo eliminan y el edificio lo deja escapar poco
a poco (`leakRate`).

Desde el 4 % de hidrógeno puede prender (con la probabilidad
`pwr.containment.hydrogenIgnitionChance` por segundo) y al 8 % prende siempre.
Las paredes normales solo aguantan 1,5 bar, así que la combustión **vuela el
tejado** (como en Fukushima): suenan las sirenas, se registra un incidente
INES 3 (INES 5 si las vainas están dañadas), quema a quien esté dentro y parte
de la actividad del combustible dañado cae alrededor de la central. La fusión
del núcleo hace estallar el hidrógeno que haya en el edificio, pero solo si la
mezcla es inflamable: una mezcla inertizada por el vapor no explota. Si el vapor
solo sube el edificio por encima de 1,5 bar, cede su pared más débil, sin
explosión ni incidente (con `pwr.accidents.containmentRupture` y
`blockDestruction`). El logro secreto «No fue el reactor» premia la explosión.

También usa `pwr.accidents.hydrogen`, `pwr.accidents.blockDestruction`,
`pwr.accidents.containmentRupture` y `pwr.containment.hydrogenIgnitionChance`,
`condensation` y `recombinerRate`. La alarma de hidrógeno de un reactor lee el
mayor entre el de su contención y el de su edificio.

| Opción | Por defecto | Qué hace |
|---|---|---|
| `pwr.building.hydrogen` | true | El hidrógeno de un reactor sin contención se acumula en la sala cerrada que lo rodea y puede explotar |
| `pwr.building.maxVolume` | 4096 | Bloques de aire (64-65536) que puede tener esa sala; una mayor cuenta como aire libre |
| `pwr.building.leakRate` | 0,0005 | Parte del gas (0-0,1) que se escapa del edificio por tick |
| `pwr.building.maxExplosionPower` | 8,0 | Potencia máxima (2-20) de la explosión que vuela el tejado |
| `pwr.building.explosionRelease` | 0,02 | Parte (0-1) de la actividad del combustible dañado que suelta la explosión |

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

## Transporte de residuos radiactivos

```toml
[wasteTransport]
shieldedTransmission = 0.002
breachedTransmission = 0.35
maxEmission = 2000.0
caskSlotsPerBlock = 9
spillFluidFraction = 0.5
spillIntensityPerActivity = 0.01
spillRadiusMax = 16
breachLeakFactor = 2.0
breachOnDerail = true
breachOnExplosion = true
```

Dos contenedores multibloque al estilo de Create para mover residuos en trenes y
otros artefactos:

- **Cisterna de residuos radiactivos**: un tanque de fluidos de Create (hasta 3x3,
  8 cubos por bloque, sin ventana) que solo acepta los fluidos de la etiqueta
  `create_nuclearindustry:radioactive_waste_fluids` (solución de combustible
  gastado, nitrato de plutonio y de uranio-233, solución de combustible de torio,
  agua contaminada y hexafluoruro de uranio empobrecido).
- **Contenedor blindado de residuos**: un almacén como el de Create, unido a lo
  largo de su eje (hasta 3x3 de sección y tres veces de largo), con
  `caskSlotsPerBlock` huecos por bloque. Solo acepta la etiqueta de ítems
  `create_nuclearindustry:waste_cask_cargo` y nunca combustible gastado *Hot*.

La actividad de la carga es la suma de la radiactividad de sus ítems (data map
`radioactivity`) o, para la cisterna, de su fluido: cubos x `per_bucket` del data
map de fluidos `create_nuclearindustry:fluid_radioactivity` (si un fluido no lo
tiene, se usa la radiactividad de su cubo). Sellado, el contenedor emite
`actividad x shieldedTransmission`; roto, `actividad x breachedTransmission`;
nunca más de `maxEmission` (rad/s en la fuente). Colocados emiten desde el centro
del multibloque; montados en un artefacto, un solo emisor por artefacto suma todos
sus contenedores desde su centro.

Se rompen al descarrilar o chocar un tren (`breachOnDerail`), con explosiones que
los alcanzan sin destruirlos (`breachOnExplosion`, también las detonaciones
nucleares) y al romper el bloque. Al romperse derraman una vez: contaminación del
terreno de `actividad x spillIntensityPerActivity` rad/s en el centro, en un radio
de `2 + raíz(actividad)` bloques hasta `spillRadiusMax`; una cisterna pierde además
`spillFluidFraction` de su fluido. Mientras sigan rotos fugan cada segundo
`breachLeakFactor` veces su intensidad de derrame repartida en un minuto al
terreno de debajo, y la cisterna pierde `breachLeakFactor` milésimas de su fluido.
Un kit de mantenimiento (a mano o con un desplegador) repara `maintenance.kitRepair`
de la brecha en cada uso: dos usos con los valores por defecto.

En un tren, las interfaces de almacenamiento y de fluidos portátiles y las
condiciones de carga del horario ven el contenido, pero solo pueden meter lo que
el contenedor acepta.

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

Los Paneles anunciadores se cuelgan en la pared y los que quedan juntos, uno
al lado del otro o uno encima de otro (hasta 16 de ancho y 8 de alto), forman
un tablero con una sola botonera (PRUEBA, SIL., RECON., REARME) y una sola
bocina. Cada ventana tiene una severidad: *Alarma* (roja, bocina urgente),
*Aviso* (ámbar, tono suave) o *Estado* (blanca, sin bocina ni reconocimiento:
solo se enciende mientras se cumple su condición). `annunciatorInterval` fija
cada cuánto comprueba cada panel sus ventanas; la bocina suena cada segundo
para una alarma y cada dos para un aviso, y la prueba de lámparas dura 3 s.

El Panel anunciador tiene plantillas para sus seis ventanas (*Núcleo*,
*Primario PWR*, *Gen. vapor* y *Contención*) con métricas, umbrales,
severidades y etiquetas; los enlaces no cambian. Los umbrales salen de la configuración
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
  (la bola de fuego, potencia `min(12, 0,35 × potencia)`, que hiere y empuja
  pero no rompe bloques: eso lo hace el cráter), el cráter y las
  zonas de daño (ver «Armas nucleares: la explosión»), la lluvia radiactiva y
  una onda expansiva. Una explosión aérea llega un 25 % más lejos. Las bombas
  sucias conservan su explosión convencional y no tienen onda.
- La onda avanza `blastWave.speed` bloques por tick desde el centro hasta
  `min(blastWave.maxRadius, 4 × radio del cráter)` bloques (`maxRadius = 0`
  la desactiva; admite hasta 2048). Las bombas termonucleares usan su propio
  tope, `explosive.thermonuclear.blastWaveMaxRadius`. No se guarda: si el servidor se para, la onda en curso se
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
  Como mucho `blastWave.fragileBlocksPerTick` por tick, y como mucho 256
  rayos por tick hacia los que están a cubierto (el resto de esos se quedan
  enteros); no carga chunks.

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

## Armas nucleares: la explosión

```toml
[explosive.crater]
enabled = true
blastProofResistance = 1200.0
loadChunks = true
chunksAtOnce = 8
maxReach = 768

[explosive.zones]
severeOverpressure = 35.0
moderateOverpressure = 12.0

[explosive.thermal]
reach = 1.6
fires = true
firesPerTick = 24

[explosive.prompt]
edgeDose = 3000.0

[explosive.fallout]
windStretch = 1.0

[explosive.debris]
fallingPerTick = 40
maxFalling = 1500

[explosive]
airburstHeight = 2.0
visualRange = 4096
respectClaims = true

[explosive.thermonuclear]
enabled = true
maxRadius = 750
maxPower = 4320.0
maxCraterRadius = 110.0
blastWaveMaxRadius = 768
thermalReach = 1.4
countdownSeconds = 15
tamperFalloutReach = 0.5
tamperHotSpot = 150.0
```

**Protecciones (claims)**: con `explosive.respectClaims` (activado por
defecto), el cráter, las zonas de daño, el destello y los incendios de
cualquier detonación nuclear no tocan los bloques de los chunks protegidos por
**Open Parties and Claims** o **FTB Chunks**, si alguno de los dos está
instalado. Cuenta como protegido un chunk reclamado cuyo dueño no deja que las
explosiones rompan bloques (en Open Parties and Claims, la opción de proteger
los bloques de explosiones; en FTB Chunks, la de equipo «las explosiones dañan
el terreno» desactivada). Esos chunks ni se cargan para el cráter, y el borde o
la eyecta de al lado tampoco caen dentro. La radiación, la lluvia radiactiva y
el daño a las entidades siguen llegando. Los dos mods son opcionales: sin
ellos, o con una versión cuya API no encaje (sale un aviso en el log), no se
respeta ningún claim.

Cada detonación nuclear (bomba, misil, proyectil de cañón) calcula sus zonas a
partir del radio de destrucción `R` de la cabeza (de 80 a 250 bloques en una
atómica, hasta 750 en una termonuclear) y de la altura a la que estalla sobre el
terreno:

- **Bola de fuego**: radio `max(6, 0,12 × R)`. Si la detonación está más alta
  que su bola de fuego es una **explosión aérea**: no abre cráter, deja una
  décima parte de la lluvia radiactiva, y su onda, sus zonas de daño y su
  destello llegan un 25 % más lejos.
- **Cráter**: un cuenco de radio `0,3 × R` (un 15 % más si estalla bajo
  tierra) y `0,32 ×` ese radio de hondo, con el borde levantado
  (`0,07 ×` el radio de alto) que baja hasta `1,3 ×` el radio, eyecta suelta
  hasta `2 ×` el radio y el fondo de trinitita, basalto y obsidiana, con magma en
  el centro. Una cabeza completa abre un cráter de unos 75 bloques.
- **Zona grave**: donde la onda conserva `zones.severeOverpressure` kPa (sin
  pasar de `R`), desaparece todo lo que se alza sobre el terreno natural.
- **Zona moderada**: hasta `zones.moderateOverpressure` kPa (sin pasar de
  `1,2 × R`), se quita parte de lo construido (más cuanto más cerca), los
  árboles pierden las hojas y lo que queda colgando cae como bloques que caen
  (`debris.fallingPerTick` por tick, como mucho `debris.maxFalling` por
  detonación).
- **Destello térmico**: hasta `thermal.reach × R`. Su fuerza cae con el
  cuadrado de la distancia. Quien ve la bola de fuego arde y, más cerca, recibe
  daño de fuego (Protección contra el fuego lo reduce); un muro, una colina o un
  refugio certificado lo tapan. En el terreno quema la hierba, las hojas y las
  plantas y carboniza los troncos expuestos, y con `thermal.fires` prende lo
  inflamable al aire libre (como mucho `thermal.firesPerTick` fuegos por tick).
  Los bloques invisibles (barreras, luz) no lo tapan.
- **Radiación inmediata**: hasta `0,7 × R`, una dosis de golpe de
  `prompt.edgeDose` en el borde de la bola de fuego (al menos a 10 bloques),
  que cae con el cuadrado de la distancia y con el aire. Los bloques la cortan
  como cortan el resto de la radiación, y el blindaje y el yoduro también
  cuentan. A los animales los pone enfermos. 0 la desactiva.
- **Lluvia radiactiva**: la del radio `1,5 × R` de siempre, pero el viento la
  estira en una pluma que llega `1 + windStretch` veces más lejos a sotavento,
  la mitad a barlovento y a los lados se queda en un 60 %. El viento sale de la
  semilla del mundo y gira poco a poco de un día a otro. También vale para la
  bomba sucia.
- Los bloques con resistencia a explosiones mayor que
  `crater.blastProofResistance` (la roca madre y las puertas blindadas)
  resisten; la resistencia se mira según el estado del bloque.

**Alcance sobre el terreno** (`crater.maxReach`): lo que una detonación hace a
los bloques (cráter, zonas de daño, quemaduras y fuegos) nunca pasa de este
número de bloques desde la zona cero. Si sus zonas llegarían más lejos, la zona
moderada y la térmica se encogen juntas en la misma proporción para caber, así
que siguen apagándose poco a poco en lugar de cortarse de golpe; la zona grave y
el anillo del cráter se cortan en el alcance. Una atómica completa llega a unos
400 bloques (500 en explosión aérea), así que solo afecta a las termonucleares.
Es el ajuste principal de rendimiento: el coste de una detonación crece con el
cuadrado de este alcance. Con 768, una bomba termonuclear completa trabaja unos
1,85 millones de columnas (unos dos minutos y medio con los presupuestos por
defecto) y unos 7000 chunks. En servidores, bájalo o desactiva el cráter.
El daño a entidades (destello, radiación inmediata y onda) no depende de él.

El trabajo se hace columna a columna desde el centro, dentro del presupuesto de
`explosive.blastColumnsPerTick` y `explosive.blastBlocksPerTick` compartido por
todas las detonaciones del mundo. Se guarda con el mundo y sigue tras reiniciar.
Las columnas de chunks sin cargar se saltan y su chunk se apunta; con
`crater.loadChunks` la detonación carga ella misma esos chunks (y los genera si
nunca se generaron), de cerca a lejos y como mucho `crater.chunksAtOnce` a la vez,
los trabaja en cuanto llegan y los suelta cuando el barrido ha pasado, así que
una bomba lejos de todos los jugadores deja igualmente su cráter. La generación
va en los hilos del generador del mundo, no en el tick del servidor. Sin
`crater.loadChunks`, esos chunks esperan a que otra cosa los cargue. Con `crater.enabled = false` las detonaciones no tocan el terreno,
pero siguen haciendo daño, irradiando y contaminando. El cráter no despierta el
agua de alrededor: los bloques que caen y los fuegos no avisan a sus vecinos, y
junto al agua no cae nada, así que un cráter abierto al lado del mar no se
inunda solo (lo hace si algo actualiza después esa agua).

**Cabezas de fisión**: la Bomba nuclear y la Plataforma de lanzamiento solo
admiten núcleos de grado militar, dos por hueco. Cada núcleo vale exactamente el
metal que lleva: ocho de uranio al 90 % o cuatro de plutonio (que cuenta doble),
es decir, 8 unidades, así que seis núcleos (48 unidades) son una cabeza
completa. Un núcleo de uranio guarda el enriquecimiento más bajo de lo que se
fundió en él, de modo que el uranio más rico sigue dando más potencia. Las
bombas y plataformas de antes con metal suelto lo sueltan al lado la primera vez
que se cargan, y paran su cuenta atrás; no se pierde nada.

Proyectil de Create Big Cannons: se fabrica con un proyectil explosivo, una Bomba
nuclear y de uno a seis núcleos de grado militar; seis son una cabeza completa.
Los proyectiles de antes conservan sus unidades (uno de cuatro núcleos de antes
sigue siendo una cabeza completa).

**Bomba termonuclear** (`explosive.thermonuclear.*`): un primario de fisión (un
núcleo, obligatorio; el de plutonio comprime mejor el secundario que el de
uranio), un secundario de hasta 16 de Deuteruro de litio-6 y una camisa de hasta
4 Bloques de uranio empobrecido. Sin núcleo no cuenta atrás; sin deuteruro solo
estalla el primario, como una cabeza de un núcleo.

- La potencia es `160 × (1 + 16 × eficiencia × D/16) × (1 + 0,6 × T/4)`, con `D`
  el deuteruro y `T` la camisa, hasta `maxPower`; el radio crece con su raíz
  cúbica, `250 × ∛(potencia / 160)`, hasta `maxRadius`. Llena (primario de
  plutonio, 16 y 4) llega a 750 bloques, tres veces una atómica completa.
- El cuenco del cráter no pasa de `maxCraterRadius`; su borde y su eyecta lo
  siguen. Las zonas de daño sí crecen con `R`, dentro de `crater.maxReach`.
- La onda expansiva llega hasta `blastWaveMaxRadius` (las atómicas siguen con
  `civilDefense.blastWave.maxRadius`) y el destello hasta `thermalReach × R`.
- La camisa amplía la lluvia radiactiva hasta `(1,5 + tamperFalloutReach × T/4)
  × R` y deja en la zona cero un foco de `tamperHotSpot × T/4` que se apaga en
  días, como el de una bomba sucia. La intensidad de la lluvia en la zona cero
  ya está en `contamination.max` con cualquier cabeza termonuclear, por eso la
  camisa ensucia más lejos y más tiempo, no más fuerte.
- Cuenta `countdownSeconds` segundos y las sirenas avisan hasta donde llega su
  lluvia radiactiva. El sismógrafo se satura con ella.
- Con `enabled = false` ninguna enciende su secundario: todas estallan como su
  primario.
- Proyectil termonuclear de Create Big Cannons: un proyectil explosivo, una Bomba
  termonuclear, un núcleo, de uno a cuatro de deuteruro y hasta dos bloques de
  uranio empobrecido; como mucho llega a unos 470 bloques. Un misil no admite
  piezas termonucleares.

Misil: el Designador de objetivo, usado agachado al aire, cambia entre
explosión en superficie y explosión aérea; en aérea el misil estalla
`airburstHeight ×` su radio de bola de fuego por encima del objetivo.

```toml
[missile]
maxRange = 0.0
protectedSpawnRadius = 0
allowedDimensions = []
launchPermissionLevel = 0
launchCooldownSeconds = 0
launchBroadcast = "IMPACT_AREA"

[missile.launch]
countdownSeconds = 10

[missile.flight]
gravity = 0.005
riseHeight = 24.0
minRange = 64.0
atmosphereTop = 400.0
terminalTicks = 100
showDistance = 256
```

Misil: una Plataforma de lanzamiento cargada y con señal de redstone cuenta
`missile.launch.countdownSeconds` segundos antes del despegue (por defecto 10,
los 200 ticks de siempre). Las sirenas del objetivo reciben el aviso durante
toda la cuenta atrás.

**Vuelo** (`missile.flight.*`): un misil sube recto `riseHeight` bloques (lo
justo para salir de su silo), gira hacia el objetivo y su guiado mantiene el
empuje hasta tener la velocidad con la que caería libremente sobre él; ahí corta
motores y el resto del vuelo es una caída libre bajo `gravity` (bloques/tick²).
Cada etapa pierde su masa al agotarse. Si se le acaba el propulsante antes de
llegar a esa velocidad, el objetivo está fuera de su alcance y cae corto. El
misil de la plataforma alcanza unos 3500 bloques con la gravedad por defecto y
tarda unos 50 segundos en recorrer 2000 (apogeo de unos 600 bloques). Bajar
`gravity` alarga los vuelos, los hace más altos y da más alcance.

- Los blancos a menos de `minRange` bloques se tiran en vuelo alto, en vez de
  rasante.
- Por encima de `atmosphereTop` el misil está en curso medio; por debajo, en
  reentrada. Los últimos `terminalTicks` ticks son la fase terminal.
- El vuelo se calcula en el servidor sin cargar chunks ni crear entidades. Solo
  los jugadores a menos de `showDistance` bloques (en horizontal) ven el misil,
  y en sus chunks cargados el misil choca con el terreno que encuentre. Poco
  antes del impacto se fuerza la carga del chunk donde cae, para que la cabeza
  estalle siempre.
- Los misiles guardados en vuelo por versiones anteriores terminan su vuelo como
  antes.

**Límites para servidores** (`missile.*`). La plataforma los comprueba antes de
empezar la cuenta atrás y otra vez en el despegue. Si alguno lo impide, no
cuenta ni avisa a las sirenas: su pantalla pone «Lanzamiento denegado» con el
motivo, y el jugador responsable, si está conectado, recibe el motivo por chat.
La plataforma lo vuelve a comprobar cada segundo y lanza en cuanto se puede
(por ejemplo, al acabar el enfriamiento).

- `maxRange`: distancia máxima en horizontal, en bloques, entre la plataforma y
  el objetivo. 0 no pone más límite que el propio alcance del misil.
- `protectedSpawnRadius`: no se puede apuntar a menos de este radio del spawn
  del mundo (solo en el Overworld). 0 no protege nada.
- `allowedDimensions`: dimensiones donde funcionan las plataformas, como
  `["minecraft:overworld"]`. Vacía, todas.
- `launchPermissionLevel`: nivel de permiso (de 0 a 4, como en los comandos; 2
  es operador) que necesita el jugador responsable de la plataforma: el último
  que la cargó desde su menú o, si nadie lo ha hecho, quien la colocó. Se
  comprueba aunque no esté conectado. Una plataforma sin responsable (colocada
  y cargada antes de esta versión) no lanza si se pide algún nivel; basta con
  volver a cargarla.
- `launchCooldownSeconds`: segundos que tiene que esperar un jugador después de
  que una de sus plataformas dispare para que otra suya pueda hacerlo. Se
  guarda con el mundo y se mide con el reloj del Overworld, así que no se
  reinicia al reiniciar el servidor ni al cambiar de dimensión.
- `launchBroadcast`: a quién se avisa en el chat de cada lanzamiento, con el
  punto de impacto y el tiempo de vuelo. `NONE`, nadie; `IMPACT_AREA` (por
  defecto), los jugadores de esa dimensión a menos de `max(2 × radio de aviso
  de la cabeza, 128)` bloques del punto de impacto; `SERVER`, todos los
  jugadores del servidor (el mensaje incluye la dimensión).

Lo que se ve (solo en el cliente): los jugadores a menos de `visualRange`
bloques reciben la detonación y ven un destello blanco (más fuerte mirando
hacia ella y si nada la tapa), un brillo naranja en la pantalla y en la niebla
que se apaga en un minuto, la bola de fuego que sube y se convierte en un hongo
(unos cuatro minutos, visible más allá de la niebla de la distancia de
renderizado), una cúpula de condensación al principio, un anillo de polvo que
avanza con la onda y una sacudida de cámara cuando la onda pasa. En la config del
cliente: `explosion.flash` (0 a 1, brillo del destello; bájalo si molestan los
destellos), `explosion.cloudDetail` (0,25 a 2, número de bocanadas del hongo) y
`explosion.shake` (sacudida de cámara). Una termonuclear se distingue a simple
vista: el destello es blanco puro y dura más, la bola de fuego es mayor, más
blanca y arde unos siete segundos antes de virar a un naranja rojo intenso, el
hongo sube más alto y su humo es hollín casi negro, con el polvo del suelo y el
anillo de la onda oscuros, dos cúpulas de condensación y un cielo más rojo. Su
estruendo se oye el doble de lejos y más grave.

**Distant Horizons** (opcional, probado con la 3.3.2 y la 3.3.3): si está instalado, la parte
del hongo que queda más allá del terreno normal se dibuja en el pase de Distant
Horizons, con su proyección, así que no se corta en el plano lejano de vanilla y
el terreno lejano de sus LOD lo tapa cuando queda detrás de una montaña. No hay
nada que configurar; solo conviene que `visualRange` llegue tan lejos como se
quiera ver el hongo (por defecto 4096 bloques; un mundo con la config ya creada
conserva su valor). El cráter aparece en los LOD cuando sus chunks se guardan
(al descargarse o al autoguardar), sin coste extra. En multijugador el hongo se ve
igual; para que el cráter llegue a los LOD de los jugadores, Distant Horizons tiene
que estar también en el servidor (si solo está en el cliente, sus LOD solo conocen
los chunks que ese jugador ha visitado). Con packs de shaders que
cambian cómo dibuja Distant Horizons, el hongo vuelve a quedarse dentro de la
vista de vanilla.

## Reactor de fisión visible

- `reactorVisuals.rodUpdateInterval` (20): ticks entre cada actualización de lo
  que muestran los canales de combustible del reactor (combustible, brillo y
  refrigerante).
- `reactorVisuals.rodUpdatesPerTick` (64): canales de combustible que un reactor
  puede redibujar como mucho en un tick; los demás esperan al siguiente. Bájalo
  en servidores con reactores muy grandes si los cambios de aspecto se notan en
  el rendimiento.

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

## Rendimiento

Opciones para servidores, bajo `performance.*` y `explosive.*`:

- `performance.radiationRayCacheTicks` (100): ticks que un rayo de radiación (de
  una fuente a un jugador, o a una zona vigilada por un monitor de sala de
  control) puede pasar **sin usarse** y seguir reutilizándose, mientras ninguno de
  los extremos se mueva y no cambie ningún bloque en los chunks que cruza. La
  caducidad se cuenta desde el último uso, no desde que se lanzó: a los jugadores
  se les actualiza cada 20 ticks, así que un jugador quieto conserva sus rayos
  (también con el valor antiguo de 20). Con 0 se lanza cada rayo cada vez.
  Colocar o romper un bloque en los chunks que cruza el rayo (o abrir una puerta)
  lo invalida en el acto; el cráter de una detonación y el agua que se contamina
  también avisan a la caché aunque no actualicen a sus vecinos.
- `performance.radiationRayMaxAgeTicks` (1200): ticks tras los que un rayo en
  caché se vuelve a lanzar aunque se siga usando, red de seguridad para cambios
  de bloques que no avisan a nadie (contraptions de Create que mueven muros, por
  ejemplo). Con 0 no se vuelve a lanzar nunca un rayo en uso.
- `performance.forcedSyncTicks` (100, de 10 a 1200): máximo de ticks que un
  controlador de máquina (reactor de fisión, generador de vapor, planta
  criogénica, tokamak, acelerador, monitor de contención) o un monitor de sala de
  control pasa sin mandar una actualización a los clientes. Entre medias solo se
  envía cuando algo de lo que muestran cambia de forma visible: el 1 % de un
  depósito (2 % en el reactor), 2 °C, medio punto porcentual de una lectura, 10 pcm
  de reactividad, o cualquier cambio de fase, estado o recuento. SCRAM, fusión del
  núcleo, formación y rotura se envían al momento. Más bajo, las gafas y las
  pantallas siguen las derivas lentas más de cerca a costa de más tráfico.
- `performance.screenSyncTicks` (20, de 1 a 200): mínimo de ticks entre dos
  actualizaciones de un monitor o de un registrador gráfico. El registrador manda
  juntas las muestras dibujadas entre medias, así que a velocidad máxima de
  papel hay un paquete por segundo en vez de veinte; el historial completo solo
  viaja al cargar el chunk.
- `explosive.blastColumnsPerTick` (600): columnas que trabajan por tick, entre
  todas, las detonaciones nucleares de un mundo (cráter, zonas de daño y
  destello). Con menos, el trabajo se reparte en más ticks.
- `explosive.blastBlocksPerTick` (3500): bloques que las detonaciones nucleares
  de un mundo quitan o cambian como mucho por tick, entre todas.

Sin opción propia:

- La búsqueda de bloques radiactivos alrededor del jugador se salta las secciones
  de chunk sin ninguno; las fuentes (reactores, RTG...) se buscan por los chunks
  cercanos con su posición cacheada, y las fuentes de lluvia radiactiva se buscan
  por chunk, no recorriendo todas las del mundo.
- Los eventos de bloque solo llegan a los multibloques cuyos límites (guardados al
  indexarlos) contienen la posición; un acelerador sin formar escucha a 32 bloques
  de su controlador (más los límites de su última estructura formada), un tokamak
  sin formar solo donde puede estar su toro, y una torre de refrigeración sin
  formar solo detrás de su controlador (mira hacia fuera de la torre) y, en cuanto
  el suelo está puesto, solo sobre ese suelo y las paredes que puede llevar. Una
  turbina sin formar escucha la caja de la turbina más grande posible (7×7×16), que
  ya es pequeña. Hacer clic en el controlador siempre comprueba la estructura
  entera.
- Los objetivos de área de la sala de control (monitores, alarmas, anunciadores)
  guardan la lectura de radiación completa del punto (escaneo de bloques, fuentes,
  lluvia radiactiva y rayos) durante 20 ticks, el mismo intervalo con que se lee a
  los jugadores, salvo que cambie un bloque a menos de 3 chunks; varios aparatos
  sobre el mismo punto la comparten.
- Los puertos de fluido, las tuberías dobles y las penetraciones de contención
  recuerdan los tanques y tuberías vecinos y su controlador en vez de buscarlos
  cada tick; una tubería doble que cambia el fluido que muestra ya no manda
  actualizaciones de bloque a sus vecinos.
- Las pantallas y pupitres de la sala de control calculan su forma solo cuando
  cambia un bloque junto a ellos; alarmas y anunciadores que vigilan la misma
  máquina comparten una muestra, y las de máquinas distintas se reparten en ticks
  distintos.
- Todas las rejillas de combustible gastado de una misma piscina comparten una
  sola medición cada 100 ticks (`spentFuel.poolScanLimit` no cambia).
- Un monitor de contención sellado no repite la medición cada 600 ticks si ningún
  bloque del edificio cambió (red de seguridad cada 4800); abierto, espera 600,
  1200 y luego 2400 ticks entre intentos hasta que se sella. Una unidad de
  filtrado de refugio con sala certificada solo vuelve a medir si cambió algún
  bloque (red de seguridad cada 8 intervalos de `civilDefense.shelter.rescanTicks`);
  sin sala cerrada, la espera se duplica hasta 4 intervalos.
- El inspector de repositorio mide cada 200 ticks (y al colocarlo) saltándose las
  secciones de chunk sin paquetes de residuos; el fundidor de vitrificación solo
  busca receta cuando cambian su contenido o su calor.
- Las ondas expansivas solo recorren el anillo de columnas que barren cada tick.
- Los registros internos (multibloques, emisores de radiación, cachés de rayos,
  contención, red sísmica, cráteres en curso, sonidos retardados, sondeos del
  repositorio) se vacían al descargar una dimensión o al parar el servidor.

## Acelerador de partículas

Un acelerador va de un Controlador del acelerador a una fuente de iones, un linac
(cavidades de radiofrecuencia), opcionalmente un anillo sincrotrón y una línea de
haz hasta una estación de blanco o un absorbedor. La energía sale de sumar lo que
añade cada etapa; la corriente que llega depende del vacío y del enfoque.

- `accelerator.maxLineLength` (160): bloques que puede tener una línea de haz
  (del controlador al blanco, o la de extracción de un anillo).
- `accelerator.maxRingRadius` (96): radio máximo del anillo que busca el
  controlador. Un anillo mayor llega a más energía.
- `accelerator.beam.sourceCurrent` (200): corriente de la fuente de iones a pleno
  rendimiento, en microamperios.
- `accelerator.beam.copperCavityGain` (2,5) y
  `accelerator.beam.superconductingCavityGain` (12): MeV que añade cada cavidad de
  cobre o superconductora con toda su potencia de radiofrecuencia.
- `accelerator.beam.copperCavityPower` (0,25) y
  `accelerator.beam.superconductingCavityPower` (0,08): MW de radiofrecuencia que
  necesita cada cavidad.
- `accelerator.beam.normalDipoleField` (1,8) y
  `accelerator.beam.superconductingDipoleField` (8): campo de los dipolos, en
  tesla. La energía máxima de un anillo es 0,3 × campo × radio × fracción de
  dipolos, en GeV (rigidez magnética).
- `accelerator.beam.injectionRatio` (100): un anillo solo acepta haz de un linac
  con al menos su energía máxima dividida por esto.
- `accelerator.beam.ringDuty` (0,5): fracción del tiempo que un anillo entrega haz;
  el resto lo pasa llenándose y acelerando.
- `accelerator.klystron.ratedPower` (2), `.ratedSpeed` (128), `.minSpeed` (32) y
  `.stress` (32): MW de radiofrecuencia de un Klistrón, a qué RPM los da, por
  debajo de qué RPM no da nada y su impacto de estrés en SU por RPM.
- `accelerator.magnetSupply.stress` (16), `.ratedSpeed` (64) y
  `.magnetsPerSupply` (32): estrés y velocidad de una Fuente de alimentación de
  imanes y cuántos imanes alimenta (los superconductores cuentan la mitad).
- `accelerator.ionSource.stress` (8), `.ratedSpeed` (64) y `.hydrogenPerTick`
  (0,5): estrés y velocidad de la Fuente de iones y el hidrógeno que gasta por tick
  mientras hay haz.
- `accelerator.vacuumPump.stress` (8), `.ratedSpeed` (64) y `.cellsPerPump` (24):
  estrés y velocidad de una Bomba de vacío de la línea y cuántos bloques de línea
  mantiene en vacío de haz.
- `accelerator.energy.feInput` (true): las máquinas del acelerador aceptan también
  Forge Energy de otros mods en lugar de rotación.
- `accelerator.energy.fePerSu` (0,25): FE por tick que sustituye a un SU de
  rotación (estrés × velocidad nominal).
- `accelerator.activation.saturationPerKw` (0,5): actividad a la que llega una
  pieza bajo un kW de haz perdido, en dosis por segundo a su lado. Sigue radiando
  después de apagar y deja residuos radiactivos al desmontarla.
- `accelerator.activation.halfLifeTicks` (12000): semivida de esa activación.
- `accelerator.activation.promptDosePerKw` (2): dosis por segundo junto a un punto
  que pierde un kW de haz, solo mientras hay haz.
- `accelerator.activation.maxStrength` (500): dosis por segundo máxima de un punto
  del acelerador.
- `accelerator.interlock.locksDoors` (true): las puertas junto a un Enclavamiento
  del haz no se abren a mano mientras hay haz; si se abren igualmente (redstone),
  el haz salta.
- `accelerator.interlock.requirePlayersOutside` (false): el haz no arranca, y si
  está en marcha salta, con un jugador dentro de la caja que ocupa la máquina (un
  bloque alrededor de su línea de haz; en un anillo cuenta todo su interior). Sin
  esta opción basta con que todos los Enclavamientos del haz junto a la línea estén
  asegurados: un acelerador sin ninguno no comprueba nada.
- `accelerator.antimatter.thresholdGev` (25): energía por debajo de la cual un
  blanco no produce antiprotones, en GeV.
- `accelerator.antimatter.collectorEfficiency` (0,8): fracción de los antiprotones
  que recoge el colector de la estación de blanco.
- `accelerator.antimatter.kjPerNanogram` (60000): energía del haz, en kJ, que la
  Estación de blancos necesita para recoger un nanogramo de antiprotones con el
  rendimiento nominal. Un haz de 100 GeV y 6,8 MW recoge unos 11 ng por minuto. El
  rendimiento crece con la energía por encima del umbral (hasta el doble).
- `antimatter.generator.burnPerMinute` (6): nanogramos de antiprotones que quema
  un Generador de aniquilación a pleno rendimiento por minuto.
- `antimatter.generator.capacity` (256): capacidad de estrés del generador a pleno
  rendimiento, en SU por RPM (gira a 256 RPM).
- `antimatter.generator.energy` (16384) y `antimatter.generator.feEnabled` (true):
  FE por tick que da el generador en modo eléctrico, y si puede darla.
- `antimatter.ignitionDose` (2): nanogramos que gasta un Inyector de antiprotones
  para encender el plasma de un tokamak.

El Generador de aniquilación tiene una cámara de 1 ng que llena desde las trampas,
estaciones de blancos y conductos de al lado o desde un vial. La redstone fija el
acelerador (0 deja solo la combustión de espera del 2 %, que sostiene el campo de la
cámara). Una cámara fría necesita Forge Energy en su reserva (12 FE/t) para guardar
antimateria (sin ella no la toma de conductos ni de estaciones de blancos, y un vial
cargado en su puerto acaba aniquilándose cuando se le agota la batería); una vez
encendida, la combustión sostiene el campo y, en los modos
Eléctrico y Mixto, el propio generador mantiene llena esa reserva y empuja el resto
de su energía a los bloques de al lado. El tamaño de la cámara no se configura.

- `antimatter.injector.cooldownTicks` (200): ticks que espera un Inyector de
  antiprotones tras un disparo antes de poder disparar otra vez.
- `antimatter.injector.shotFe` (20000): FE que gasta cada disparo del inyector (el
  pulso de la tobera). El inyector guarda en su reserva un disparo más lo que
  necesita su cámara (8 ng, o dos dosis si `antimatter.ignitionDose` es mayor). Solo
  dispara en la fase de calentamiento, con un confinamiento del 55 % o más y una
  densidad de al menos el 5 %.

## Contención de antimateria

Las trampas de Penning y los viales guardan antiprotones solo con Forge Energy.
Una trampa cargada que se queda sin energía gasta su reserva, entra en quench con
una alarma y aniquila todo lo que guarda; meter antimateria en una trampa sin
energía la aniquila al instante, igual que romper una trampa cargada. La explosión
no deja lluvia radiactiva ni contaminación, pero da una dosis gamma.

- `antimatter.trap.small.capacity` (32), `.idleFe` (8) y `.loadFe` (12): nanogramos
  que guarda la Trampa de Penning pequeña, FE por tick que cuesta su campo esté
  cargada o no, y FE por tick extra a plena carga (en proporción al llenado). A
  plena carga consume 20 FE/t.
- `antimatter.trap.medium.*` (256, 32, 48): lo mismo para la Trampa de Penning
  (80 FE/t a plena carga).
- `antimatter.trap.large.*` (2048, 100, 150): lo mismo para la Trampa de Penning
  grande (250 FE/t a plena carga).
- `antimatter.trap.bufferTicks` (200): ticks de consumo a plena carga que guarda la
  reserva de energía de cada trampa cuando se corta el suministro.
- `antimatter.trap.quenchTicks` (60): ticks entre que una trampa cargada pierde el
  campo y la aniquilación (la alarma de quench). Si vuelve la energía durante el
  quench, la cuenta atrás se recupera un tick por tick.
- `antimatter.trap.lowPowerShare` (0,25): fracción de la reserva por debajo de la
  cual una trampa avisa de poca energía.
- `antimatter.vial.capacity` (4): nanogramos que guarda un Vial de antimateria.
- `antimatter.vial.batteryTicks` (600): ticks que la batería de un vial cargado
  mantiene el campo fuera de una trampa con energía (en el inventario o en el
  suelo); después se aniquila donde esté. Un vial en un contenedor que no hace
  tick conserva su carga.
- `antimatter.vial.rechargePerTick` (10): ticks de batería que repone por tick el
  puerto de una trampa con energía.
- `antimatter.vial.transferPerTick` (0,1): nanogramos por tick que el puerto mete
  en un vial o saca de él.
- `antimatter.conduit.fePerBlock` (4): FE por tick que necesita cada bloque de
  Conducto de antimateria. La energía entra por cualquier bloque de la red; si
  falta, la red no mueve nada (y no explota: no guarda nada).
- `antimatter.conduit.ratePerTick` (0,5): nanogramos por tick que mueve una red de
  conductos, de las estaciones de blancos a las trampas y entre trampas hasta
  igualar su llenado.
- `antimatter.conduit.maxSegments` (256): bloques que puede tener una red de
  conductos; una más larga no funciona.
- `antimatter.blast.maxRadius` (8): radio máximo de la explosión de aniquilación.
  El radio es 2 + 1,2 × raíz cúbica de los nanogramos, al menos 3; un vial lleno da
  unos 3,9 y una trampa pequeña llena 5,8.
- `antimatter.blast.dosePerNanogram` (25) y `antimatter.blast.maxCentreDose`
  (1000): dosis gamma en el centro por nanogramo y su máximo. El pulso llega al
  doble del radio de la explosión y se atenúa con la distancia y los bloques.
- `antimatter.blast.breaksBlocks` (true): si la aniquilación rompe bloques o solo
  daña e irradia lo que hay cerca.

El Generador de RF creativo (solo en creativo, sin receta) no tiene configuración:
su ritmo, de 0 a 1.000.000 FE/t por cara, se ajusta en su panel de valor.

## Isótopos y medicina nuclear

Cada isótopo lleva su propio reloj en el objeto: la actividad se calcula a partir de
la fecha en que se fabricó cuando alguien lo mira (en el inventario, cada segundo,
o en una máquina que lo usa), así que nada se procesa mientras está en un cofre.
La barra bajo el objeto muestra la actividad que queda; el contador Geiger y la
dosis que da al llevarlo encima la siguen (la entrada del data map `radioactivity`
es la de una fuente nueva).

- `isotopes.timeScale` (1.0): acelera (2 = el doble de rápido) o frena todos los
  relojes de los isótopos a la vez.
- `isotopes.<isótopo>.halfLifeDays`: periodo de semidesintegración en días de juego
  (24000 ticks) con escala 1. Por defecto el tecnecio-99m (0,25) y el molibdeno-99
  (2,75) van en tiempo real (un día de juego por un día real); los de vida larga
  usan los días reales divididos entre 32, que conserva sus proporciones:
  cobalto-60 60, estroncio-90 330 y americio-241 5000 (prácticamente estable).
- `isotopes.channel.fluenceScale` (unos 3,7 millones) y
  `isotopes.channel.targetActivity` (0.8): el canal de irradiación del reactor suma
  en cada tick el nivel de neutrones del núcleo (1000 a potencia nominal) a la
  fluencia del lote, y activa el cobalto según `1 − e^(−fluencia/fluenceScale)`;
  cuando llega a `targetActivity` todo el lote sale como fuentes de cobalto-60. Con
  los valores por defecto son 6 millones de fluencia: unos cinco minutos a potencia
  nominal.
- `isotopes.irradiator.sterilisingKGy` (10) y `isotopes.irradiator.kGyPerTick`
  (0.1): la dosis que esteriliza la comida bajo el irradiador gamma y la que da
  cada tick una fuente de cobalto-60 nueva. Con una fuente nueva tarda cinco
  segundos; con cuatro, la cuarta parte; por debajo de una doceava parte de una
  fuente nueva deja pasar la comida.
- `isotopes.smokeDetector.radius` (5): bloques en los que el detector de humo busca
  fuego, fogatas y hornos encendidos, lava y criaturas ardiendo.
- `isotopes.americium.ingrowthHalfLifeDays` (14.3), `isotopes.americium.maxYield`
  (0.5) e `isotopes.americium.minYield` (0.15): el plutonio genera americio-241
  según se desintegra su plutonio-241 (con su propio reloj, un día de juego por un
  año real). El plutonio empieza a envejecer cuando un jugador lo lleva encima por
  primera vez; la separación con ácido nítrico solo acepta plutonio con al menos
  `minYield` de americio, unos siete días de juego con los valores por defecto.
- `isotopes.medicalKit.maxBoost` (0.5) e `isotopes.medicalKit.boostTicks` (2400):
  el trazador de tecnecio-99m del kit de isótopos médicos hace el yoduro potásico
  y el azul de Prusia hasta un 50 % más eficaces durante dos minutos, en
  proporción a la actividad de la dosis.
- `isotopes.radiography.operatorDose` (2.0): dosis que recibe quien usa la cámara
  de radiografía en cada disparo con una fuente nueva.
- `isotopes.target.minEnergyGev` (1.0): energía mínima del haz con la que una
  estación de blancos irradia su blanco. Por encima de
  `accelerator.antimatter.thresholdGev` fabrica antiprotones y el blanco espera.
- `isotopes.technetiumGenerator.doseActivity` (0.15) e
  `isotopes.technetiumGenerator.maxDoses` (4): cada dosis de tecnecio-99m que da un
  ordeño se lleva esa actividad del generador, hasta ese máximo por ordeño.

El Generador de aniquilación mantiene su campo 60 ticks después de su última
combustión, aunque se haya quedado vacío: un corte breve del suministro no lo
apaga, y el conducto puede seguir entregándole antimateria sin cable. Las trampas,
el generador y el inyector están en la etiqueta `create:non_movable`: una
contraption no puede llevárselos.

## Operación y mantenimiento

Todo el mantenimiento se puede apagar. `maintenance.enabled` es el interruptor
general: apagado, las centrales no se desgastan, recargan en marcha como antes de
2.0 y no hay terremotos ni registro de incidentes. Cada función tiene además su
propio interruptor, que solo cuenta con el general encendido.

- `maintenance.enabled` (true): interruptor general.
- `maintenance.wear` (true): las piezas se desgastan con el uso. Los mecanismos de
  las barras de control, con la potencia del reactor y en cada SCRAM; la válvula
  de alivio, en cada apertura; los tubos de un generador de vapor, mientras
  transfieren calor (cuatro veces más rápido en seco); los álabes de una turbina,
  con el vapor que expanden.
- `maintenance.failures` (true): las piezas gastadas fallan. Un mecanismo de barras
  se atasca en un SCRAM y deja su canal donde estaba; un tubo revienta, fuga
  25 mB/t de refrigerante primario al secundario y el generador de vapor irradia
  (1,5 rad/s); los álabes disparan la turbina por vibración y la dejan parada hasta
  repararlos. Apagado, las piezas solo se desgastan (la válvula de alivio gastada
  se sigue atascando más a menudo, hasta diez veces).
- `maintenance.requireRefuellingOutage` (true): un reactor de fisión solo admite
  combustible nuevo en una **parada de recarga**: formado, parado en frío (núcleo
  por debajo de 100 °C) y con todas las barras dentro. Es el cambio que rompe
  compatibilidad de 2.0 (ver `docs/server-update.md`).
- `maintenance.earthquakes.enabled` (true): terremotos naturales.
- `maintenance.incidentLog.enabled` (true): registro de incidentes con la escala
  INES.
- `maintenance.wearScale` (1.0): multiplica el desgaste de todas las piezas (0 lo
  quita). Con 1, los mecanismos de las barras duran 20 días de juego a plena
  potencia, los tubos 30 días de trabajo y los álabes 25.
- `maintenance.kitRepair` (0.5): salud (0-1) que devuelve cada uso del kit de
  mantenimiento. Solo funciona en un reactor en parada de recarga, o en un
  generador de vapor o una turbina parados.
- `maintenance.earthquakes.perDay` (0.05), `minMagnitude` (3.0) y
  `maxMagnitude` (6.5): frecuencia y magnitudes de los terremotos naturales en
  cada dimensión natural con jugadores (Gutenberg-Richter: cada magnitud es diez
  veces más rara). Un terremoto de magnitud 3 se nota a 16 bloques, y cada magnitud
  duplica el alcance.
- `maintenance.earthquakes.seismicTrip` (4.0): movimiento del suelo con el que el
  disparo sísmico de un reactor lo pone en SCRAM (una magnitud 3 da 1 en su
  epicentro, y cada magnitud diez veces más).
- `maintenance.earthquakes.range` (256): bloques alrededor de un jugador donde
  puede empezar un terremoto.
- `maintenance.incidentLog.size` (100): incidentes que guarda el registro.

El registro clasifica cada suceso en la escala INES: SCRAM automático o sísmico y
disparo de turbina 0; barra o válvula de alivio atascadas 1; rotura de tubo 2;
daño del combustible 4; rotura de la vasija 4; fusión del núcleo 5 (7 sin
contención). Terremotos, detonaciones y simulacros quedan fuera de la escala. Los
operadores lo usan con `/nuclear incident list [n]`, `/nuclear incident drill
[pos]` (hace sonar la alarma de la central sin daño) y `/nuclear incident trigger
<stuck_rod|sg_tube_rupture|turbine_trip|earthquake> <pos> [magnitud]`.

## Create Aeronautics

Solo hace algo con Sable instalado (Create Aeronautics, Simulated). Que una
explosión, un aviso o un enlace de la sala de control usen dónde está de verdad
una máquina a bordo de una nave no se configura: siempre es así. Estas claves, en
el archivo común (`create_nuclearindustry-common.toml`), controlan lo demás.
La guía para el jugador está en `docs/aeronautics.md`.

| Clave | Por defecto | Qué hace |
|---|---|---|
| `aeronautics.enabled` | `true` | Interruptor general: daños y empuje de las explosiones a las naves, su contaminación y los motores. |
| `aeronautics.blast.damageShips` | `true` | Una detonación nuclear arranca, destroza y quema los bloques de las naves a su alcance, como el terreno. |
| `aeronautics.blast.pushShips` | `true` | La onda expansiva lanza las naves que alcanza y las hace girar si las golpea fuera de su centro. |
| `aeronautics.blast.impulseScale` | `1.0` | Multiplica el impulso que la onda da a una nave. |
| `aeronautics.contamination.enabled` | `true` | Las naves guardan su propia contaminación, que se mueve y se guarda con ellas. |
| `aeronautics.contamination.uptake` | `0.05` | Parte de la diferencia con la contaminación de alrededor que absorbe una nave en cada revisión (cada 5 s). |
| `aeronautics.contamination.doseFactor` | `1.0` | Dosis a bordo por unidad de contaminación de la nave, comparada con la misma contaminación en el suelo. |
| `aeronautics.engines.thrustScale` | `1.0` | Multiplica el empuje de todos los motores nucleares. |
| `aeronautics.engines.thermal.flowPerSegment` | `5` | Propelente que pasa cada segmento de intercambiador con el acelerador a fondo, mB/tick. |
| `aeronautics.engines.thermal.thrustPerMb` | `150.0` | Empuje por mB/tick de hidrógeno; el agua da un tercio. |
| `aeronautics.engines.thermal.nozzleLimit` | `1400.0` | Temperatura de la tobera (°C) por encima de la cual se desgasta; gastada del todo, revienta. |
| `aeronautics.engines.thermal.leakPerMb` | `1.0E-5` | Contaminación que lleva el escape por mB de propelente con la tobera nueva (cinco veces más gastada). |
| `aeronautics.engines.thermal.failurePower` | `5.0` | Potencia de la explosión de una tobera rota. |
| `aeronautics.engines.antimatter.maxFeed` | `0.01` | Antimateria que aniquila el motor con el acelerador a fondo, ng/tick (12 ng por minuto). |
| `aeronautics.engines.antimatter.thrustPerNg` | `2.5E6` | Empuje por ng/tick, antes de la eficiencia de las bobinas. |
| `aeronautics.engines.antimatter.coilFe` | `64` | FE por tick que necesita cada bobina para mantener el campo. |
| `aeronautics.engines.antimatter.fePerNg` | `200000.0` | FE por cada nanogramo que se quema, además del de las bobinas. |
| `aeronautics.engines.antimatter.coilLimit` | `900.0` | Temperatura de las bobinas (°C) por encima de la cual se desgastan; gastadas, el campo cae. |
| `aeronautics.engines.antimatter.gammaPerNg` | `4000.0` | Dosis gamma detrás de la tobera, a un bloque, por ng/tick que no atrapan las bobinas. |
| `aeronautics.engines.antimatter.activationPerNg` | `0.04` | Contaminación que deja el chorro donde cae, por ng aniquilado. |
| `aeronautics.engines.antimatter.quenchTicks` | `60` | Ticks entre la caída del campo y la aniquilación de la cámara. |
| `aeronautics.engines.antimatter.chamberCapacity` | `16.0` | Antimateria que guarda la cámara, ng. |

Las recetas de los motores y sus dos logros solo se cargan con Sable instalado.
Los bloques existen siempre, para que los mundos abran igual con y sin Aeronautics.

## Ruinas nucleares

El mundo genera tres clases de instalaciones abandonadas, montadas por piezas
(jigsaw) que se combinan de forma distinta cada vez:

- **Central nuclear abandonada** (`create_nuclearindustry:abandoned_power_plant`):
  muy rara (una por cada 64×64 chunks de media, a 8 chunks como mínimo de
  cualquier aldea) y solo en terreno llano de llanuras, desiertos, sabanas,
  praderas, bosques y taigas. El edificio del reactor, con la cúpula de
  contención hundida, una vasija rota, dos generadores de vapor y la grúa polar
  caída; alrededor, la sala de turbinas, el edificio de control con sus pupitres
  y paneles, un parque de residuos o el muñón de una torre de refrigeración, y
  la carretera de acceso con su garita.
- **Refugio civil** (`create_nuclearindustry:civil_shelter`): una caseta de
  hormigón con sirena en el tejado y una escalera que baja a los dormitorios, el
  almacén de víveres y la sala de ventilación.
- **Búnker militar** (`create_nuclearindustry:military_bunker`): un puesto de
  guardia con sacos terreros y sirena manual sobre un pozo de escalas que baja
  varios niveles hasta pasillos, una puerta blindada atascada, el puesto de
  mando, dormitorios, armería, la planta de filtrado, el almacén de residuos y
  el laboratorio. También aparece en badlands y colinas.

Son ruinas, no máquinas: no hay ningún controlador de multibloque, así que nada
se forma ni funciona (los procesadores convierten en carcasa de reactor
cualquier bloque de la etiqueta `create_nuclearindustry:ruin_forbidden` que se
colase). El peligro son los barriles de residuos con fugas (como mucho seis por
pieza), que contaminan su chunk con el tiempo como cualquier barril; no hay
contaminación de partida ni generadores de monstruos. Los cofres traen botín de
progreso: concentrado de uranio y torta amarilla, grafito, boro, litio, berilio,
cobalto, piezas de reactor, módulos de pupitre, contadores Geiger, equipo hazmat
y armadura de uranio empobrecido gastados, yoduro potásico, azul de Prusia,
raciones esterilizadas, kits de mantenimiento y piezas de Create. Nunca uranio
enriquecido, plutonio ni piezas de arma.

Se buscan con `/locate structure create_nuclearindustry:<id>` y se colocan con
`/place structure create_nuclearindustry:<id>`. Las claves están en el archivo
común (`create_nuclearindustry-common.toml`) y solo afectan a los chunks que se
generen después del cambio.

| Clave | Por defecto | Qué hace |
|---|---|---|
| `ruins.powerPlants.enabled` | `true` | Genera centrales nucleares abandonadas. |
| `ruins.powerPlants.chance` | `1.0` | Probabilidad de que un sitio elegido para una central la tenga de verdad (la separación del conjunto ya la hace rara). |
| `ruins.powerPlants.maxSlope` | `4` | Diferencia máxima de altura, en bloques, entre las cinco muestras de terreno bajo la central; con más desnivel, o con agua, no se genera. |
| `ruins.civilShelters.enabled` | `true` | Genera refugios civiles. |
| `ruins.civilShelters.chance` | `1.0` | Probabilidad de que un sitio elegido para un refugio lo tenga. |
| `ruins.militaryBunkers.enabled` | `true` | Genera búnkeres militares. |
| `ruins.militaryBunkers.chance` | `1.0` | Probabilidad de que un sitio elegido para un búnker lo tenga. |

Las piezas salen de los scripts `tools/structgen/worldgen_*.py`
(`python3 tools/structgen/worldgen_all.py` las reescribe todas, siempre igual).

## Pestaña creativa

La pestaña del mod está dividida en secciones temáticas: minería y materiales,
ciclo del combustible, fisión, PWR y contención, generación eléctrica, sala de
control, gastado y residuos, isótopos y medicina, mantenimiento y EPI, fusión,
criogenia, aceleradores, antimateria, defensa civil, armamento y, con Create
Aeronautics instalado, propulsión. Cada sección
empieza con un banner que se anima al pasar el ratón por encima, y el tooltip de
cada ítem dice a qué sección pertenece. Lo que otros mods añadan a la pestaña
aparece al final, en "Otros".

En la configuración del cliente (`create_nuclearindustry-client.toml`),
`creativeTab.sections = false` quita los banners y deja una sola lista, en el
mismo orden. El cambio se aplica la próxima vez que se reconstruye la pestaña
(al volver a entrar al mundo).

## Uranio y litio de otros mods

- Materias primas: la etiqueta `create_nuclearindustry:raw_uranium` incluye
  `c:raw_materials/uranium` y `forge:raw_materials/uranium`. Se trituran como el
  uranio en bruto del mod. La espodumena usa `c:raw_materials/lithium`.
- Lingotes: `create_nuclearindustry:uranium_ingots` incluye `c:ingots/uranium` y
  `forge:ingots/uranium`. Los lingotes externos cuentan como uranio natural (0,7%)
  y se fluoran a hexafluoruro con acido fluorhidrico para entrar en la cascada.
  Un datapack puede ampliar la etiqueta para otros identificadores.
