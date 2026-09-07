# Actualizar a 1.1.0

Archivo: `build/libs/create_nuclearindustry_1.21.1_1.1.0.jar`.
Instala la misma version del mod en el servidor y en todos los clientes.

## Entorno

- Minecraft Java 1.21.1 y Java 21.
- NeoForge 21.1.221 o posterior de la rama 21.1.
- Create 6.0.10 (compilado con 6.0.10-281), con sus dependencias habituales.
- Create Big Cannons es opcional; si ya lo usas, conserva tambien sus dependencias.
- No cambies el identificador `create_nuclearindustry`: es el usado por los datos
  existentes del mundo. Esta actualizacion no cambia los identificadores de bloques
  y objetos ni requiere regenerar el mundo.

## Pasos

1. Deten el servidor y guarda una copia del mundo y de `config/`.
2. Sustituye el JAR 1.0.0 en `mods/` por el JAR 1.1.0. No dejes ambas versiones.
3. En `config/create_nuclearindustry-common.toml`, dentro de `[turbines]`, establece
   `plasmaCapacityPerPort = 65536.0` para adoptar el nuevo balance. La capacidad de
   fision por defecto es `steamCapacityPerPort = 32768.0`.
4. Conserva el resto de tus ajustes. Las entradas nuevas toman sus valores por
   defecto; las antiguas opciones de ejemplo ya no forman parte de la configuracion.
5. Actualiza el mod en los clientes. Desactiva el antiguo resource pack Classic
   si estaba activo: las nuevas texturas vienen incluidas en el JAR.
6. Inicia el servidor y revisa que el mod aparece como 1.1.0, el mundo carga y un
   cliente actualizado puede conectarse. Comprueba un reactor y sus turbinas.

Cambiar la version del JAR no sobrescribe los valores existentes del config. En
particular, una instalacion con `plasmaCapacityPerPort = 4096.0` conservara ese
valor hasta editarlo.

Verificacion local de la entrega: compilacion, metadatos del JAR y recursos.
El arranque y la conexion al servidor de destino deben comprobarse tras instalarlo.
