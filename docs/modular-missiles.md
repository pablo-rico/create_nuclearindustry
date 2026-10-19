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

## Radar

Construye un radar y aliméntalo con las mismas RPM y estrés iniciales. Al abrir
su pantalla verás todos los vuelos de su dimensión, incluidos los propios e
inertes. La cobertura no depende de distancia ni visión directa. La información
se actualiza cada segundo: identificador, origen, posición, trayectoria, impacto
previsto y tiempo restante. El mapa X/Z tiene zoom y selección de contactos; no
carga terreno. En vuelos antiguos sin origen guardado se muestra desconocido.

El radar emite redstone **15** mientras detecte vuelos. Conecta esa salida a una
sirena o a un enlace de redstone de Create. Display Link ofrece cantidad de
contactos y tiempo hasta el impacto más próximo. Pararlo o descargarlo elimina
la salida de alarma. Detecta desde el despegue hasta el impacto; los preparativos
y las cuentas atrás son invisibles.

**Las alertas de misiles requieren radar.** Los vuelos y silos ya no emiten
avisos globales ni amenazas automáticas a las sirenas. `missile.launchBroadcast`
queda sin efecto, incluso si una configuración antigua lo activa. Las sirenas
mantienen redstone y avisos automáticos de bombas y accidentes.

## Mundos existentes

Las plataformas antiguas permanecen para retirar su contenido, pero no lanzan.
Convierte una plataforma antigua en un controlador y un misil antiguo en un
motor mediante recetas de fabricación. Sus recetas anteriores y entradas de
creativo/JEI se retiran. Los vuelos guardados terminan normalmente.

La primera versión admite únicamente instalaciones estáticas en el mundo.
Silos y radares montados en naves o contraptions no operan. No hay interceptores,
múltiples ojivas, redes de radares ni historial de vuelos terminados.
