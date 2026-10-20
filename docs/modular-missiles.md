# Cohetes modulares y lanzamiento táctico

Un cohete modular se construye sobre una **Base de lanzamiento**, se prepara desde un
**Controlador de lanzamiento** y despega únicamente con una señal nueva de redstone.
Un silo de **Hormigón de silo** sigue siendo válido, pero es una envolvente visual:
no hace falta para lanzar desde una plataforma abierta.

## Plataforma y cohete

1. Coloca una cuadrícula completa de **3×3 Bases de lanzamiento**. Usa un controlador
   por base.
2. Coloca el **Controlador de lanzamiento** junto a cualquiera de los cuatro lados de
   la base, no encima de ella; su frente mira hacia quien lo coloca. Dale al menos
   32 RPM con un **eje desde abajo** (el generador de su zócalo); su impacto inicial
   es 8 SU/RPM. Cuando encuentra la base, los nueve bloques forman una sola mesa de
   lanzamiento con la rejilla del foso de llamas bajo el cohete y cuatro abrazaderas
   que se cierran sobre un cohete listo y se abren al despegar. Sus luces dicen su
   estado: verde listo, ámbar armado (parpadea en la cuenta atrás), rojo si no puede
   lanzar.
3. Pon un **Puerto de combustible de silo** tocando la base, en un lateral, debajo o
   junto a ella, y conecta una tubería si vas a cargar propelente líquido. Sus
   válvulas miran hacia quien lo coloca, pero las tuberías conectan por cualquier cara.
4. Construye la columna en el bloque central de la base. La gramática es
   `(Motor + 1–4 Depósitos){1–3} + Guiado + Ojiva`: cada motor inicia una etapa nueva.
   Por tanto, una etapa es motor, depósitos, guiado y ojiva; dos etapas añaden otro
   motor y sus depósitos; tres añaden una tercera pareja. La columna completa mide de
   4 a 17 bloques.

El controlador comprueba la base, que la columna sea continua, que haya espacio de
salida y que la combinación de secciones sea válida. Las secciones apiladas forman un
solo fuselaje: un depósito muestra si lleva un cartucho sólido o líquidos (y se
escarcha al cargar oxígeno líquido) y la ojiva muestra su tipo de carga. Las aletas y otros bloques
pegados con pegamento viajan con el cohete. Una carcasa de Hormigón de silo puede
rodear el conjunto hasta la punta para darle aspecto de silo; no mejora el lanzamiento.

## Depósitos y guiado

Cada depósito acepta un cartucho de propelente sólido, o 1000 mB de Combustible de
cohete y 1000 mB de Oxígeno líquido desde el puerto. Una etapa no puede mezclar sus
cartuchos con líquido. Las etapas líquidas empujan más y durante más tiempo.

El módulo de guiado queda vacío para guiado básico, acepta un Mecanismo de precisión
para guiado inercial o un Rastreador estelar para guiado estelar. Un nivel superior
reduce el CEP que muestra el controlador. Fija el blanco con sus coordenadas, con un
Designador de objetivo o desde una Mesa holográfica de mando.

## La consola y la bahía de ojiva

Haz clic derecho en el Controlador de lanzamiento y elige la sección en el corte de la
consola. La carga se inserta **en la bahía de la ojiva de esa consola**, no haciendo
clic sobre el bloque de la ojiva. La consola también muestra las etapas, el llenado,
el alcance y los errores de montaje.

La ojiva tiene tres huecos de carga, cuatro de secundario, dos de camisa y un hueco
para una bomba completa:

- Cada hueco de carga admite hasta 2 núcleos de grado militar, 64 materiales
  radiactivos o 16 TNT. Los núcleos dan fisión; solo material radiactivo da una
  ojiva sucia; vacía es inerte. El TNT crea una ojiva convencional, sin contaminación;
  su potencia queda limitada por la configuración del servidor.
- Los cuatro huecos de secundario admiten hasta 4 Deuteruros de litio-6 cada uno
  (16 en total). Los dos huecos de camisa admiten hasta 2 bloques de uranio empobrecido
  cada uno (4 en total).
- Una carga termonuclear necesita exactamente un núcleo primario y puede llevar
  secundario y camisa. La camisa no puede ir sin secundario. TNT no se mezcla con
  núcleos, material radiactivo, secundario ni camisa.
- El último hueco acepta una Bomba nuclear, Bomba sucia o Bomba termonuclear completa;
  conserva su contenido, pero no puede mezclarse con ninguna carga suelta.

Las ranuras de depósito y guiado aparecen al seleccionar esas secciones. La consola
rechaza los objetos incompatibles y bloquea cambios mientras el cohete está armado o
en cuenta atrás.

## Armado y redstone

En la pestaña **Lanzamiento**, comprueba el blanco, el alcance y la cuenta, y pulsa
**Armar**. Entonces aplica una señal de redstone nueva al controlador: una palanca que
se acaba de activar o un botón sirven. Una señal que ya estaba encendida al armar no
inicia nada, y mantenerla encendida no lanza el siguiente cohete.

La cuenta atrás normal dura diez segundos, más el retraso que pueda asignar una salva.
Perder rotación, cambiar el cohete, bloquear la salida, descargar el área o reiniciar
el mundo cancela la cuenta y desarma el controlador. Tras despegar, la base y el silo
siguen en su sitio; el cohete consume sus secciones, combustible y carga, y separa las
etapas agotadas como restos.

## Mesa holográfica, Enlazador táctico y salvas

Forma la **Mesa holográfica de mando** con nueve piezas en un 3×3. Usa un
**Enlazador táctico** de dos clics: haz clic primero en un radar o en un Controlador de
lanzamiento (también vale una Base de lanzamiento, un puerto o una sección que le
pertenezca) para guardarlo; después haz clic en la mesa para enlazarlo. Repetir sobre
un enlace lo elimina y usar el enlazador agachado sobre la mesa borra sus enlaces.

La mesa admite hasta 4 radares y 16 controladores de lanzamiento dentro del alcance
configurado. En la pestaña **Silos**, selecciona un controlador enlazado para ver su
estado, fijar su blanco, armarlo o desarmarlo. Marca varios para una salva: la mesa los
arma todos o desarma todos si uno falla, y calcula retrasos para impacto simultáneo o
para el espaciado elegido.

La mesa coordina, pero **no dispara**. Después de armar una salva, cada controlador
debe recibir su propia señal nueva de redstone para iniciar su cuenta y respetar el
retraso asignado. Desde las trazas de la mesa se puede abortar un vuelo propio que
salió de un controlador enlazado; el misil se autodestruye con una explosión pequeña
sin detonar su ojiva.

## Defensa antimisiles

Los interceptores derriban misiles en vuelo. Nunca atacan uno que todavía acelera: hace
falta que haya apagado los motores para calcular dónde estará.

**Batería de interceptores.** Se coloca entera con un solo objeto: ocupa 2×2×2 y crece
desde el bloque donde haces clic, hacia delante y a tu derecha. Necesita un eje por debajo
de su esquina que gire al menos a 32 RPM; cuanto más rápido, antes gira la torreta. Sus
cuatro tubos admiten un **Misil interceptor** cada uno, a mano (clic con el misil sobre
cualquier bloque de la batería), con embudo o con brazo mecánico; agachado y con la mano
vacía se saca uno listo. Cada interceptor tarda unos segundos en quedar listo tras cargarlo.
Defiende de cerca y dentro de la atmósfera, y se recarga. Los interceptores se fabrican por
montaje secuencial: lámina de hierro, cartucho de propelente, mecanismo de precisión, válvula
electrónica y prensa.

En su pantalla pones el **código de enlace** de cuatro caracteres de tus radares, eliges
**automático** o **manual**, la **doctrina** (de uno en uno, y otro si falla y queda
tiempo, o de dos en dos) y el **radio de la zona defendida** (por defecto, su propio
alcance; "Aquí" la centra en la batería). En automático ataca sola los misiles que sus
radares ven ir a caer en esa zona y que no son de su dueño. Una señal de redstone en
cualquiera de sus bloques retiene el fuego automático (las órdenes de una mesa siguen
valiendo) y un comparador lee cuántos interceptores tiene listos. Las gafas y un Display
Link muestran su estado, su munición y su modo.

**Silo interceptor.** Remata un cohete con un **Vehículo de intercepción** en lugar de la
ojiva: `(Motor + Depósitos)×1..3 + Guiado + Vehículo de intercepción`. Su Controlador de
lanzamiento queda listo sin blanco, no hace caso a la redstone ni al armado y solo despega
cuando una mesa de mando o la defensa automática le ordenan atacar un misil; tras una cuenta
atrás corta, sale a encontrarlo por encima de la atmósfera, lejos, y se gasta.

**Desde la mesa.** El Enlazador táctico también guarda baterías (haz clic en cualquiera de
sus bloques) y las enlaza a una mesa, hasta 8; los silos interceptores se enlazan como
cualquier controlador. La pestaña **Defensa** lista los lanzadores con su estado y su
munición. Fija una traza en el holograma y pulsa **Atacar** para lanzar desde el lanzador
elegido o desde el mejor; **Auto** activa la defensa automática de la mesa con los radares
enlazados (sirve también para baterías sin código propio), la fila de abajo cambia la
doctrina y el radio de la zona, **Alto el fuego** apaga el automático y anula las órdenes, y
**Abortar** autodestruye un interceptor propio. El holograma dibuja cada interceptor
camino de su punto de encuentro, con su probabilidad de destrucción.

**Probabilidad de destrucción.** Cada interceptor tiene una probabilidad de destruir su
blanco al llegar: mayor de frente, bien dentro de su alcance y con el misil seguido por
varios radares desde hace rato; menor en la fase terminal y para la batería. Si falla, se
autodestruye y, si queda tiempo, la doctrina de uno en uno lanza otro. Un misil destruido
estalla en el aire y sus restos caen al suelo: su ojiva nunca detona ni contamina. Los
valores se ajustan en `missile.defense.*` (ver [configuración](configuration.md)).

## Satélites

Un cohete puede llevar un satélite en lugar de una ojiva: `(Motor + Depósitos)×1..3 +
Guiado + Cofia de satélite`. Mete en la cofia un **Satélite de alerta temprana** o un
**Satélite de comunicaciones** a mano, con embudo o con brazo mecánico; los dos salen de un
montaje secuencial sobre un mecanismo de precisión. Su Controlador de lanzamiento queda
listo sin blanco, no hace caso a la redstone y solo despega cuando una mesa de mando le
ordena poner el satélite en órbita.

**Órbitas.** Tres etapas sólidas llenas, o dos líquidas, bastan para la **órbita baja**;
para la **geoestacionaria** hacen falta tres etapas con líquido. La consola indica el
delta-v del cohete, el que pide cada órbita y la más alta a la que llega. Las aletas y
lo que vaya pegado al cohete pesan y le quitan delta-v.

- Un satélite en órbita baja vuela en línea recta a gran velocidad. Al salir por un lado
  del borde del mundo vuelve a entrar por el contrario, así que con el tiempo pasa por
  todo el mapa.
- Cada día de Minecraft pierde algo de altura, y al final se quema al entrar en la
  atmósfera.
- Uno geoestacionario se queda para siempre sobre el punto que elegiste, más alto y
  viendo más lejos.
- Cuanto más alto está un satélite, más terreno ve: hasta su horizonte.

**Estación terrestre.** Cada red de satélites es un **código de enlace** de cuatro
caracteres, como el de los radares. La Estación terrestre de satélites lleva ese código y
necesita un eje por debajo que gire al menos a 32 RPM; su antena se orienta sola hacia el
satélite de su red que tenga a la vista. Enlázala a una mesa de mando con el Enlazador
táctico (hasta dos por mesa): la red de la primera estación que funcione es la de la mesa,
y la mesa se la pasa a sus controladores de lanzamiento. Las gafas y un Display Link
muestran su red, cuántos satélites tiene y cuándo pasa el próximo.

**Lo que hacen.**

- **Alerta temprana:** ve los cohetes que acaban de despegar bajo él aunque ningún radar
  llegue, y los pone en la imagen de los radares, baterías y mesas de su red.
- **Comunicaciones:**
  - Hace de repetidor: une radares de su red que estén demasiado lejos para compartir
    trazas y deja enlazar a la mesa lanzadores más allá del alcance del enlazador.
  - Hace de GPS: si ve a la vez el lanzador y el blanco de un misil, reduce mucho su
    dispersión. La consola lo indica.

**Desde la mesa.** La pestaña **Satélites** lista los satélites de la red de la mesa, con
su órbita, su vida y su próximo paso sobre la zona, y debajo los satélites ajenos que tus
radares han visto pasar: quedan catalogados para siempre.

- Para **lanzar**, elige un controlador con cofia y la órbita, y marca en el mapa el punto
  sobre el que pasará (en órbita baja, también hacia dónde va). Tiene que estar dentro del
  borde del mundo.
- Puedes **desorbitar** un satélite propio.
- Puedes **derribar** uno ajeno catalogado con un silo interceptor enlazado. Solo los de
  órbita baja: los geoestacionarios quedan fuera de alcance. Ninguna defensa automática
  dispara a satélites.

El holograma de la mesa muestra los satélites que pasan cerca, su traza y su huella. De
noche, los de órbita baja se ven cruzar el cielo como puntos de luz. Los valores se
ajustan en `missile.orbit.*` (ver [configuración](configuration.md)).

## Radar

Haz girar un radar al menos a 32 RPM. Ve cuerpos de cohete dentro de su alcance y por
encima del horizonte, comparte trazas con otros radares de igual código y activa su
salida de redstone y claxon cuando un impacto previsto entra en su zona protegida.
Enlázalo con una Pantalla de radar o con la Mesa holográfica usando el Enlazador táctico
para ver y coordinar esas trazas.

## Mundos existentes

Los silos antiguos con un suelo de Hormigón de silo continúan funcionando porque ese
suelo cuenta como base de lanzamiento. Puedes abrirlos o convertirlos en plataformas;
las paredes no son un requisito estructural. Los vuelos ya guardados terminan con las
reglas con las que fueron lanzados.
