# Create Nuclear Industry en las naves de Create Aeronautics

Todo lo de esta guía necesita **Create Aeronautics** (con Simulated y Sable). Sin
ellos el mod funciona como siempre: los motores no se pueden fabricar ni salen en
la pestaña creativa, y nada de lo demás cambia. La configuración está en
`docs/configuration.md`, sección «Create Aeronautics».

## Máquinas a bordo

Un reactor, una turbina, un tokamak, una trampa de Penning o cualquier otra
máquina del mod funciona en una nave igual que en tierra. Ensamblar la nave,
partirla o volver a dejarla en el suelo no rompe nada:

- las trampas de Penning, los inyectores de antiprotones y los generadores de
  aniquilación no se aniquilan al moverse;
- los inventarios no sueltan una copia de lo que llevan;
- un reactor en marcha no entra en SCRAM y se vuelve a formar a bordo tal como
  estaba.

Las alarmas en el chat, el registro de incidentes, las sirenas, los logros, el
detector sísmico, los sonidos y los efectos de las máquinas cuentan desde donde
está la nave de verdad. Los terremotos no sacuden las centrales de una nave.

### Sala de control

Un pupitre, monitor, anunciador, registrador o alarma enlazado a una máquina la
sigue encontrando después de subirla a una nave o bajarla de ella, sin volver a
enlazar. El alcance del enlace (128 bloques por defecto) se mide entre las
posiciones reales: de tierra a nave o de nave a nave. Los enlaces de redstone
avanzados funcionan igual.

## Explosiones

- Una detonación nuclear o termonuclear trata los bloques de las naves cercanas
  como el terreno. En la zona severa los arranca, en la moderada destroza parte de
  ellos y en la del destello quema o carboniza lo que mira a la bola de fuego.
  Un bloque muy resistente a las explosiones aguanta igual que en el suelo.
- La onda expansiva **lanza** las naves que alcanza y las hace girar si las golpea
  fuera de su centro. También rompe sus cristales.
- Una bomba, un misil lanzado desde una nave, una trampa de Penning o un reactor
  que explota a bordo lo hace donde está la nave en el mundo. El cráter, la onda,
  el destello y la lluvia radiactiva aparecen en el suelo de debajo, y la
  explosión se lleva la nave por delante.
- Las explosiones normales ya alcanzaban a las naves por sí solas; ahora también
  lo hacen las del mod.

### Fusión del núcleo a bordo

El corio corre hacia el suelo real aunque la nave esté inclinada. Funde el casco y,
cuando lo atraviesa, cae de la nave sobre lo que haya debajo, donde sigue fundiendo
el terreno. La lluvia radiactiva de un accidente a bordo cae bajo la nave.

## Contaminación de las naves

Cada nave lleva su propia contaminación: viaja con ella, se guarda con el mundo y,
si la nave se parte, pasa a los dos trozos. Decae con el tiempo igual que la del
suelo.

- **Se contamina** al volar por zonas contaminadas. Cuanto más alto vuela, menos
  coge: el polvo se queda cerca del suelo.
- También cuando le cae encima la lluvia radiactiva de una detonación o de un
  accidente, y cuando hay una fuga o un escape a bordo.
- **Irradia** a quien va en ella, como un suelo con la misma contaminación. Se ve
  como polvo sobre la cubierta, y el contador Geiger lo marca.
- **Se limpia** con un depurador de radiación a bordo.

Las máquinas radiactivas de la nave (reactor, RTG, barriles) no irradian hacia
fuera; solo cuenta la contaminación de la propia nave.

## Motor térmico nuclear

Un cohete calentado por un reactor. Se construye en línea recta:

1. el **motor térmico nuclear** (el controlador);
2. de uno a ocho **intercambiadores del motor térmico**, a lo largo del motor;
3. una **tobera del motor térmico** al final, mirando hacia donde sale el escape.

Al controlador se le mete por tubería **refrigerante primario caliente** de un
reactor de agua a presión y un **propelente**. De él se saca el primario ya frío.
Como propelente sirven el agua, el deuterio o el hidrógeno (se obtiene
electrolizando salmuera). Cuanto más ligero el gas, más empuja: el hidrógeno da el
triple de empuje que el agua. Cada segmento deja pasar 5 mB/tick con el
acelerador a fondo. Una señal de redstone en el controlador fija el acelerador, y
un comparador lee el empuje.

La tobera se calienta con el caudal, sobre todo con agua. Por encima de 1400 °C
se desgasta y, cuando se gasta del todo, revienta: hay una explosión y la actividad
del primario se esparce alrededor. Se repara en frío con un **kit de
mantenimiento** sobre el controlador. El escape lleva siempre un poco de
actividad, más con la tobera gastada, y la deja donde cae el chorro.

## Motor de antimateria

El motor más potente. También en línea recta:

1. el **motor de antimateria** (la cámara de aniquilación);
2. de una a seis **bobinas magnéticas**;
3. una **tobera magnética** al final.

La antimateria entra por un **conducto de antimateria** o con un vial en el puerto
del controlador (clic derecho con el vial; clic derecho con la mano vacía para
sacarlo). La cámara guarda 16 ng. Las bobinas necesitan **Forge Energy**: 64 FE/t
cada una para mantener el campo, y mucho más mientras quema. A fondo aniquila
12 ng por minuto.

- Cuantas más bobinas, más energía se convierte en empuje: con dos, la mitad, y
  con seis, casi toda.
- Lo que las bobinas no atrapan las calienta, y sale detrás de la tobera como
  **rayos gamma** que irradian todo lo que haya en el chorro.
- Con pocas bobinas a fondo se sobrecalientan y se desgastan.
- Sin energía, o con las bobinas gastadas, el campo cae: suena una alarma de quench
  y, tres segundos después, la antimateria de la cámara se aniquila donde está el
  motor.

## Lo que comparten los motores

- En una nave la empujan en sentido contrario a su tobera. Si el motor está
  descentrado, la nave también gira. En tierra solo se encienden, como en un
  banco de pruebas.
- Las gafas muestran el empuje, el acelerador, la temperatura, el desgaste y el
  combustible.
- Se pueden enlazar a la sala de control: monitores, indicadores del pupitre,
  registradores y alarmas. También tienen fuente de Display Link.
- Tienen escenas de Ponder y páginas de JEI. Hay dos logros: «Alas atómicas» y
  «Velocidad warp... casi».
- Los bloques más pesados del mod pesan más en una nave: carcasas de reactor,
  turbina y tokamak, barriles de residuos, la bomba nuclear, los motores y el
  bloque de uranio empobrecido.
