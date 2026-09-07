# Configuracion y compatibilidad

NeoForge genera `config/create_nuclearindustry-common.toml` al iniciar el mod.
En multijugador, edita el archivo del servidor. Reinicia el servidor o el mundo
despues de editarlo para aplicar los valores de forma consistente.

```toml
[turbines]
steamCapacityPerPort = 32768.0
plasmaCapacityPerPort = 65536.0

[fission]
boronAbsorption = 1.5
uraniumBurnPerNeutron = 0.0000045

[fusion]
deuteriumTritiumBurnRate = 0.0015
```

La turbina de plasma tiene el doble de capacidad nominal que la de fision.
Las capacidades de turbina son la
contribucion de stress por RPM y puerto a caudal completo (320 mB/t de vapor o
160 mB/t de plasma); Create la multiplica por la velocidad. Las turbinas siguen
girando a 64 RPM. Duplicar una capacidad duplica su output con el mismo vapor.

Los archivos de configuracion existentes conservan sus valores personalizados:
para adoptar el nuevo equilibrio, cambia `plasmaCapacityPerPort` a `65536.0`.
Las opciones de ejemplo (`logDirtBlock`, `magicNumber`,
`magicNumberIntroduction`, `items`) se han eliminado.

La absorcion del boro se multiplica por la proporcion de barras de control frente
a barras de uranio, se limita a 100% y se aplica segun su insercion. Un valor de
cero elimina la absorcion. El consumo de uranio se mide en unidades de combustible
por nivel de neutrones y tick; cada carga aporta 100 unidades.

La fusion consume deuterio y tritio juntos en pellets D-T: cada pellet aporta 100
unidades y la tasa se multiplica por la potencia de fusion cada tick. No hay
depositos separados de deuterio y tritio en el reactor. Duplicar cualquiera de las
tasas reduce la duracion del combustible a la mitad para la misma actividad;
cero desactiva su agotamiento.

## Uranio de otros mods

La centrifugadora acepta materias primas mediante la etiqueta existente
`create_nuclearindustry:raw_uranium`. Tambien acepta lingotes mediante
`create_nuclearindustry:uranium_ingots`, que incluye `c:ingots/uranium` y
`forge:ingots/uranium`, y objetos cuyo identificador sea exactamente
`<mod>:uranium` o `<mod>:uranium_ingot`.

Los lingotes externos se convierten 1:1 a uranio del mod con 0.7% de enriquecimiento
y continuan hasta el objetivo de la centrifugadora. El uranio propio conserva su
enriquecimiento. La insercion manual y automatica usa las mismas reglas y JEI
muestra los lingotes compatibles. Un datapack puede ampliar la etiqueta de
lingotes para identificadores que no siguen estas convenciones.
