# Cohetes modulares y lanzamiento táctico

Un cohete modular se construye sobre una **Base de lanzamiento**, se prepara desde un
**Controlador de lanzamiento** y despega únicamente con una señal nueva de redstone.
Un silo de **Hormigón de silo** sigue siendo válido, pero es una envolvente visual:
no hace falta para lanzar desde una plataforma abierta.

## Plataforma y cohete

1. Coloca una cuadrícula completa de **3×3 Bases de lanzamiento**. Usa un controlador
   por base.
2. Coloca el **Controlador de lanzamiento** junto a cualquiera de los cuatro lados de
   la base, no encima de ella. Dale al menos 32 RPM mediante ejes, engranajes o un
   motor creativo; su impacto inicial es 8 SU/RPM.
3. Pon un **Puerto de combustible de silo** tocando la base, en un lateral, debajo o
   junto a ella, y conecta una tubería si vas a cargar propelente líquido.
4. Construye la columna en el bloque central de la base. La gramática es
   `(Motor + 1–4 Depósitos){1–3} + Guiado + Ojiva`: cada motor inicia una etapa nueva.
   Por tanto, una etapa es motor, depósitos, guiado y ojiva; dos etapas añaden otro
   motor y sus depósitos; tres añaden una tercera pareja. La columna completa mide de
   4 a 17 bloques.

El controlador comprueba la base, que la columna sea continua, que haya espacio de
salida y que la combinación de secciones sea válida. Las aletas y otros bloques
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
