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

<!-- opciones spentFuel.*: las documenta su paquete de trabajo -->

## Residuos y repositorio

<!-- opciones waste.*: las documenta su paquete de trabajo -->

## Instrumentos de la sala de control

<!-- opciones controlRoom.*: las documenta su paquete de trabajo -->

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
