# Guía de Testing - Reactor Nuclear

## Qué Esperar en Consola

Después de compilar y ejecutar el mod, cuando coloques un **REACTOR_CONTROLLER** y construyas la estructura a su alrededor, deberías ver mensajes en la consola de Minecraft.

---

## Paso 1: Colocar el REACTOR_CONTROLLER

Cuando coloques el bloque controller, deberías ver:

```
[ReactorBlockEntity] Created at position: [100, 64, 200]
```

**Explicación**: El BlockEntity fue creado y está listo.

---

## Paso 2: El Reactor Comienza a Tickearse

Después de 5-10 segundos de juego, deberías ver:

```
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 100)
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 200)
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 300)
```

**Explicación**: El reactor está siendo actualizado cada tick. Este mensaje aparece cada 100 ticks (aproximadamente cada 5 segundos).

---

## Paso 3: Validación de Estructura

Después de completar la estructura (REACTOR_CASING, HEAT_EXCHANGER, URANIUM_ROD), verás:

```
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Structure dimensions detected: 5×4
[Reactor Validator] Structure composition:
  - REACTOR_CASING blocks: 60
  - HEAT_EXCHANGER blocks: 3
  - URANIUM_ROD blocks: 4
  - CONTROL_ROD blocks: 1
[Reactor Validator] ✓ Structure validation SUCCESS - ReactorStructure{5×4, uranium=4, control=1, pos=[100, 64, 200]}

╔════════════════════════════════════════╗
║  ✓ REACTOR STRUCTURE FORMED             ║
╠════════════════════════════════════════╣
║ Dimensions: 5×4
║ Uranium Rods: 4
║ Control Rods: 1
║ Location: [100, 64, 200]
╚════════════════════════════════════════╝
```

**¡Éxito!** El reactor está completamente formado y funcionando.

---

## Paso 4: Si hay un Error en la Estructura

Si construyes algo incorrecto, verás mensajes como:

### Error 1: Estructura Muy Pequeña
```
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Height 3 is less than minimum 4
[Reactor Validator] ❌ Structure validation FAILED
```

**Solución**: Aumenta la altura a mínimo 4 bloques.

---

### Error 2: Falta HEAT_EXCHANGER en el Centro
```
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Structure dimensions detected: 5×4
[Reactor Validator] ❌ Center column missing HEAT_EXCHANGER at [100, 62, 200]
[Reactor Validator] ❌ Structure validation FAILED
```

**Solución**: Asegúrate de poner HEAT_EXCHANGER directamente debajo del CONTROLLER, en línea recta, para toda la altura.

---

### Error 3: Falta REACTOR_CASING en las Paredes
```
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Structure dimensions detected: 5×4
[Reactor Validator] ❌ Wall missing REACTOR_CASING at [97, 63, 200], found: minecraft:dirt
[Reactor Validator] ❌ Structure validation FAILED
```

**Solución**: Reemplaza todos los bloques del perímetro con REACTOR_CASING.

---

### Error 4: No hay Uranium Rods
```
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Structure dimensions detected: 5×4
[Reactor Validator] ❌ No uranium rods found in reactor
[Reactor Validator] ❌ Structure validation FAILED
```

**Solución**: Coloca al menos 1 URANIUM_ROD en el interior o en los bordes.

---

## Paso 5: Rompiendo la Estructura

Si rompes una parte del reactor (por ejemplo, un bloque de CASING), verás:

```
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 2540)
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] ❌ Wall missing REACTOR_CASING at [97, 63, 200], found: minecraft:air
[Reactor Validator] ❌ Structure validation FAILED

╔════════════════════════════════════════╗
║  ✗ REACTOR STRUCTURE BROKEN             ║
║  Reactor will no longer function        ║
╚════════════════════════════════════════╝
```

**Explicación**: El reactor fue destruido. Tendrás que reconstruirlo.

---

## Verificación por Chat (Mensaje al Usuario)

Además de los mensajes en consola, ahora también recibirás **mensajes de chat en el juego**:

**Cuando se forma el reactor:**
```
✓ Reactor Structure Valid! Dimensions: 5×4, Uranium Rods: 4, Control Rods: 1
```

**Cuando se rompe:**
```
✗ Reactor Structure Broken! Rebuild to continue operation.
```

---

## Troubleshooting

### No veo nada en consola después de colocar el bloque

**Posibles causas:**
1. El mod no se está ejecutando correctamente
2. La consola está desactivada
3. No estás esperando suficiente tiempo (espera 5-10 segundos)

**Soluciones:**
1. Verifica en el título de la ventana que dice "fabric" o "forge" + la versión
2. Abre la consola con F3 + T (recargar recursos)
3. Mira en los logs: `./gradlew runClient` mostrará más información

---

### Veo "Ticking" pero no veo "Checking structure"

**Causa**: El ticker está funcionando pero la estructura aún no es válida cada 20 ticks.

**Solución**: Verifica que tu estructura sea correcta:
- ¿El REACTOR_CONTROLLER está en la parte superior?
- ¿Hay HEAT_EXCHANGER directamente debajo?
- ¿Las paredes son REACTOR_CASING?
- ¿Hay al menos 1 URANIUM_ROD?

---

### El reactor dice "validation SUCCESS" pero desaparece después

**Causa**: Probablemente hay un bloque que no es exactamente lo esperado.

**Solución**: 
1. Reconstruye usando solo: REACTOR_CASING, HEAT_EXCHANGER, URANIUM_ROD, CONTROL_ROD
2. No mezcles con otros bloques
3. Verifica que el perímetro esté completamente cerrado

---

## Checklist de Construcción

Antes de construir, asegúrate de tener:

- [ ] Mínimo 5×5 de base (debe ser impar: 5, 7, 9, 11...)
- [ ] Mínimo 4 bloques de altura
- [ ] 1 REACTOR_CONTROLLER (en el techo central)
- [ ] 1+ HEAT_EXCHANGER en columna central (debajo del controller)
- [ ] Perímetro completo de REACTOR_CASING
- [ ] 1+ URANIUM_ROD en el interior
- [ ] (Opcional) 1+ CONTROL_ROD en los bordes

---

## Logs Esperados Resumen

```
[COLOCAR CONTROLLER]
[ReactorBlockEntity] Created at position: [100, 64, 200]

[ESPERAR 5 SEGUNDOS]
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 100)

[CONSTRUIR ESTRUCTURA]
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Structure dimensions detected: 5×4
[Reactor Validator] Structure composition: ...
[Reactor Validator] ✓ Structure validation SUCCESS

╔════════════════════════════════════════╗
║  ✓ REACTOR STRUCTURE FORMED             ║
╠════════════════════════════════════════╣
║ Dimensions: 5×4
║ Uranium Rods: 4
║ Control Rods: 1
║ Location: [100, 64, 200]
╚════════════════════════════════════════╝

[CADA 5 SEGUNDOS DESPUÉS]
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 200)
[ReactorBlockEntity] Ticking at [100, 64, 200] (game time: 300)
...
```

---

## ¿Listo para Probar?

1. Compila: `./gradlew runClient`
2. Crea un mundo nuevo
3. Ve al creative mode o obtén los bloques necesarios
4. Coloca un REACTOR_CONTROLLER
5. Rodéalo con REACTOR_CASING (5×5)
6. Coloca HEAT_EXCHANGER en el centro debajo
7. Coloca URANIUM_ROD en los bordes interiores
8. ¡Mira la consola para ver los mensajes!

Si algo no funciona, **comparte los mensajes de consola exactos** y podré debuggear.
