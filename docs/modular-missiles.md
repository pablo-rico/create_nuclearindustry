# Cohetes modulares, silos y radar

Construye un suelo completo de **5×5** con paredes de silo. Deja un interior de
**3×3** y coloca el controlador en el centro de una cara lateral de la base.
Sobre el centro del suelo monta de abajo arriba:

1. Entre una y tres etapas, cada una con un motor y de uno a cuatro depósitos.
2. Un módulo de guiado.
3. Una sola ojiva, inerte, sucia, de fisión o termonuclear.

El conjunto mide entre **4 y 17 bloques**. Extiende las paredes perimetrales
hasta la punta. La parte superior queda abierta: una tapa de Create debe abrirse
antes de lanzar. El controlador identifica piezas incorrectas, depósitos vacíos,
estructura incompleta, obstrucciones y blancos fuera de alcance.

Usa un cartucho sobre cada depósito para cargarlo. Usa los materiales sobre la
ojiva para insertarlos: tres huecos de un núcleo o material radiactivo cada uno,
hasta cuatro de deuteruro para el secundario y dos bloques de uranio empobrecido
para la camisa. Vacía, la ojiva es inerte. Usar una sección agachado con la mano
vacía retira su último contenido. El controlador queda a la altura del suelo,
sustituyendo el bloque central de un borde.
Las etapas, depósitos y masa de carga determinan el alcance. Usa una carga inerte
para probar la instalación sin explosión. Introduce coordenadas o utiliza el
Designador de objetivo existente; se mantienen las restricciones administrativas
de alcance, dimensión, permisos, protección del spawn y enfriamiento.

Alimenta el controlador con **32 RPM** como mínimo; el impacto de estrés inicial
es **8 SU/RPM**. Arma desde la interfaz y aplica un **nuevo flanco de redstone**.
La cuenta dura **10 segundos** por defecto. Una señal permanente no lanza los
siguientes cohetes. Perder rotación, modificar el montaje o bloquear la salida
cancela la cuenta; hay que volver a armar. Descargar el silo o reiniciar también
cancela la cuenta y lo desarma. Lanzar consume el cohete, combustible y carga;
el silo permanece. Las etapas agotadas se separan visualmente sin dejar piezas
recuperables.

## Combustible líquido

Además de los cartuchos sólidos, los depósitos admiten propulsante líquido. Pon un
**puerto de combustible de silo** en una pared y conéctale tuberías: acepta
**combustible de cohete** (mezcladora calentada: carbón e hidrógeno) y **oxígeno
líquido** (mezcladora: aire líquido, que también da nitrógeno líquido) y llena los
depósitos de abajo arriba, 1000 mB de cada uno por depósito. No acepta nada con el
silo armado o en cuenta atrás. Una etapa líquida empuja más y durante más tiempo
que una sólida: un cohete líquido de dos o tres etapas alcanza varios kilómetros.
Un depósito no puede mezclar cartucho y líquidos.

## Guiado

El módulo de guiado decide la precisión. Sin nada es **básico** (giróscopo); con
un mecanismo de precisión de Create es **inercial**; con un **rastreador estelar**
es **estelar**. El error circular probable (CEP) crece con la distancia y baja con
cada nivel; el controlador lo muestra. `missile.guidance.cepScale` lo escala (0
apunta perfecto).

## El cohete en vuelo

Al despegar, la columna entera (y lo que lleve pegado con pegamento, como aletas)
vuela como un contraption de Create. Las etapas agotadas se sueltan y caen dando
vueltas; arden donde caen. Los bloques extra suman masa
(`missile.rocket.extraBlockMass`). El cohete no puede estar pegado al silo, ser
demasiado grande ni llevar cosas en contenedores: el controlador lo indica.

## Radar

Construye un radar y hazlo girar (mínimo `missile.machinery.minimumRpm`). Su
**alcance** contra un cuerpo de cohete desnudo es `missile.radar.baseRange`
(3000 bloques) a la velocidad mínima; crece con la raíz cuarta de las RPM y es
mucho mayor contra un propulsor encendido. La Tierra es curva
(`missile.radar.earthRadius`): un vuelo bajo y lejano queda bajo el horizonte,
así que conviene poner los radares altos. Los radares con el mismo **código de
enlace** de cuatro caracteres y a menos de `missile.radar.networkRange` bloques
comparten lo que ven.

Cada radar vigila una **zona** (centro y radio en su pantalla; sin radio, todo).
Su salida de redstone y su claxon se encienden con los vuelos cuyo impacto
previsto cae en ella; **reconocer** la alarma solo calla el claxon.

La pantalla es una consola: barrido con anillos de distancia, ecos con su estela,
impactos previstos con su elipse de error sobre el terreno, tabla de trazas,
detalle con perfil de altitud, zoom, recentrado, código y zona. La antena de
encima gira con el barrido. La **pantalla de radar** (hasta 3×3 en una pared) se
enlaza a un radar con el enlazador de sala de control y muestra su barrido en
directo. Los radares sirven además a monitores, indicadores, alarmas y
anunciadores de la sala de control (trazas y amenazas) y a los Display Link.

## Mesa holográfica de mando

Coloca nueve piezas en un cuadrado de 3×3: se funden en una sola mesa, con un
marco continuo de acero y latón, patas y el proyector bajo el centro. Con el
enlazador de sala de control (sobre cualquier pieza) enlaza hasta **cuatro
radares** y **un silo**; enlazar de nuevo uno ya enlazado lo quita, y agachado se
borran todos.

Sobre un mapa del terreno (de 1 a 16 km de lado, elegido solo) muestra:

- cada traza como un pequeño misil sobre su arco, con el morro según su rumbo,
  llama mientras acelera y un vehículo de reentrada al caer; lo volado, lo que le
  queda (a trazos) y la elipse de impacto;
- el silo y los lugares de lanzamiento como silos, los radares como antenas que
  giran con su cobertura, el alcance y el blanco del silo;
- anillos donde estalla un arma.

**Usarla sin abrir la pantalla** (a menos de 8 bloques):

- Mira un misil, el silo o un radar para ver su etiqueta (fase, altitud,
  velocidad y tiempo al impacto; alcance y CEP; código y cobertura).
- Clic derecho en un misil: lo fija (o lo suelta). La traza fijada tiene un panel
  sobre la mesa y la vista la sigue si sale del mapa. Todos los que miran la mesa
  ven lo mismo.
- Clic derecho en el marco: cambia el zoom (automático, 1, 2, 4, 8, 16 km).
- Agachado + clic derecho en el cristal: marca un blanco provisional, verde si el
  silo puede alcanzarlo y los límites del servidor lo permiten, rojo con el motivo
  si no. Repetirlo sobre la marca en menos de 5 segundos se lo envía al silo.
- Clic derecho en el cristal vacío: abre su pantalla, con el mapa desplazable, las
  trazas, el estado del silo y un modo de designar con explosión en superficie o
  en el aire.

## Mundos existentes

Las plataformas antiguas permanecen para retirar su contenido, pero no lanzan.
Convierte una plataforma antigua en un controlador y un misil antiguo en un
motor mediante recetas de fabricación. Sus recetas anteriores y entradas de
creativo/JEI se retiran. Los vuelos guardados terminan normalmente.

Los silos y radares montados en naves o contraptions no operan. No hay
interceptores, múltiples ojivas ni historial de vuelos terminados.
