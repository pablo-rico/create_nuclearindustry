# Classic Industry 16x

Resource pack para Create: Nuclear Industry en Minecraft 1.21.1.

Instala `create-nuclear-classic.zip` en la carpeta `resourcepacks` de tu instancia
y activalo en Opciones > Paquetes de recursos, por encima de otros packs que
modifiquen este mod. Requiere el mod y sus dependencias habituales.

Incluye 55 texturas de bloques: estructuras de fision y fusion, centrifugadora,
barras, intercambiador, puertos, turbinas, minerales, plataforma de lanzamiento,
bomba y proyectil, ademas del vapor animado. Los bloques que comparten texturas
(incluidas las tuberias) reciben el mismo acabado. El agua de Minecraft conserva
su textura del pack base. No modifica objetos independientes ni interfaces.

El estilo usa pixel art 16x16, acero de tonos andesita, mecanismos de laton,
tuberias de cobre y ceramica clara con acentos turquesa para fusion. Conserva los
modelos, coordenadas UV y diferencias visuales entre entradas, salidas y estados.
No requiere shaders.

La vista previa esta en `create-nuclear-classic-preview.png`.

## Regeneracion

```sh
python3.12 tools/texturegen/create_classic.py
```

Requiere Pillow. Genera la carpeta del pack, el ZIP reproducible y la vista previa;
verifica cobertura, dimensiones y referencias de texturas en los modelos. El
generador original y las texturas incluidas en el JAR siguen disponibles.

Validacion realizada: generacion y cobertura completas, revision visual de la
lamina y comprobacion del archivo ZIP. Pendiente: inspeccion dentro de Minecraft.
