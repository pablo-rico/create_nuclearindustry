# Estructura del Reactor Nuclear

## Descripción General

El reactor nuclear es un **multibloque en forma de cubo hueco** con los siguientes componentes:

- **REACTOR_CASING**: Forma los muros del caldero (paredes exteriores)
- **HEAT_EXCHANGER**: Columna central que genera vapor
- **REACTOR_CONTROLLER**: Techo central (controla el reactor)
- **URANIUM_ROD**: Barras de combustible que cuelgan desde arriba
- **CONTROL_ROD**: Barras de control que cuelgan desde arriba

---

## Estructura Mínima (5×5×4)

```
        VISTA SUPERIOR (arriba)
        
    C C C C C
    C U C U C
    C C C C C      U = URANIUM_ROD
    C U C U C      C = REACTOR_CASING (exterior)
    C C C C C      H = HEAT_EXCHANGER
                   X = REACTOR_CONTROLLER (techo)

        VISTA LATERAL
        
    Y=top    [X][U][X][U][X]     <- CASING + Fuel rods protrude up
             [C][H][C][H][C]     
             [C][H][C][H][C]     <- Interior: HEAT_EXCHANGER in center
             [C][H][C][H][C]     
    Y=bottom [C][C][C][C][C]     <- CASING floor
```

---

## Estructura Detallada (5×5×4)

### Capa SUPERIOR (Y = controllerPos)
```
X X X X X
X U C U X   <- CONTROL_ROD o URANIUM_ROD en esquinas/bordes
X C X C X   <- CONTROLLER en centro
X U C U X
X X X X X
```

**Reglas:**
- Centro: **REACTOR_CONTROLLER**
- Bordes y esquinas: **URANIUM_ROD** o **CONTROL_ROD** (pueden protrusion hacia afuera)
- Resto del perímetro: **REACTOR_CASING**

### Capas INTERMEDIAS (Y = controllerPos - 1, -2, -3)
```
C C C C C
C H H H C   <- HEAT_EXCHANGER en el centro
C H H H C
C H H H C
C C C C C
```

**Reglas:**
- **Perímetro (bordes)**: Siempre **REACTOR_CASING**
- **Centro (3×3)**: **HEAT_EXCHANGER** en todas las posiciones
- **Interior**: Vacío (aire) o bloques ignorados

### Capa INFERIOR/PISO (Y = controllerPos - height)
```
C C C C C
C H H H C   <- HEAT_EXCHANGER en el centro
C H H H C
C H H H C
C C C C C
```

**Reglas:**
- Mismo patrón que capas intermedias
- Última capa de HEAT_EXCHANGER antes del piso

---

## Estructura Extendida (7×7×6)

```
    VISTA SUPERIOR
    
    C C C C C C C
    C U U U U U C
    C U C U C U C
    C U C X C U C   <- X = REACTOR_CONTROLLER (centro)
    C U C U C U C
    C U U U U U C
    C C C C C C C
    
    VISTA LATERAL
    
    [C][C][C][C][C][C][C]  <- Top: CASING + fuel rods
    [C][H][H][H][H][H][C]
    [C][H][H][H][H][H][C]  <- Center column: HEAT_EXCHANGER
    [C][H][H][H][H][H][C]
    [C][H][H][H][H][H][C]
    [C][C][C][C][C][C][C]  <- Bottom: CASING floor
```

---

## Requisitos de Tamaño

| Parámetro | Mínimo | Máximo | Flexible |
|-----------|--------|--------|----------|
| **Ancho (X-Z)** | 5 bloques | 11 bloques | Sí (impar recomendado) |
| **Alto (Y)** | 4 bloques | 15 bloques | Sí |
| **Proporción** | Puede ser más alto que ancho | N/A | Sí |

**Nota**: El ancho debe ser **impar** (5, 7, 9, 11) para tener un centro único.

---

## Componentes Requeridos

### REACTOR_CASING (Paredes)
- **Ubicación**: Perímetro completo del cubo
- **Cantidad**: Depende del tamaño
  - 5×5×4: ~60 bloques
  - 7×7×6: ~140 bloques
- **Función**: Contiene la reacción nuclear

### HEAT_EXCHANGER (Columna Central)
- **Ubicación**: Centro exacto (X=controllerPos, Z=controllerPos)
- **Altura**: Desde el piso hasta 1 bloque bajo el techo
- **Cantidad**: (altura - 1) bloques
  - 5×5×4: 3 bloques
  - 7×7×6: 5 bloques
- **Función**: Genera vapor basado en temperatura del núcleo

### REACTOR_CONTROLLER (Techo)
- **Ubicación**: Centro exacto, en la capa superior
- **Cantidad**: 1 bloque
- **Función**: Controla y monitorea el reactor

### URANIUM_ROD (Combustible)
- **Ubicación**: Interior o bordes del reactor
- **Cantidad**: Mínimo 1, sin máximo
- **Función**: Fuente de fisión nuclear
- **Nota**: Deben estar conectadas verticalmente a la columna de HEAT_EXCHANGER

### CONTROL_ROD (Control)
- **Ubicación**: Bordes del reactor
- **Cantidad**: Mínimo 1, sin máximo
- **Función**: Controla la tasa de fisión
- **Movimiento**: Puede moverse verticalmente con Create contraptions

---

## Ejemplos de Construcción

### Ejemplo 1: Reactor 5×5×4 (Mínimo)

**Paso 1**: Construir marco de REACTOR_CASING (5×5)
```
Coloca el REACTOR_CONTROLLER en el centro arriba
Rodéalo con CASING en forma de cuadrado
```

**Paso 2**: Construir columna central de HEAT_EXCHANGER
```
Debajo del CONTROLLER, coloca 3 bloques de HEAT_EXCHANGER alineados verticalmente
```

**Paso 3**: Completar las paredes
```
Coloca CASING en todo el perímetro externo, verticalmente
```

**Paso 4**: Añadir combustible
```
Coloca URANIUM_ROD en los 4 bordes interiores
Coloca CONTROL_ROD en al menos una posición
```

**Verificación en consola**:
```
[Reactor Validator] Checking structure starting at controller: [xyz]
[Reactor Validator] Structure dimensions detected: 5×4
[Reactor Validator] Structure composition:
  - REACTOR_CASING blocks: 60
  - HEAT_EXCHANGER blocks: 3
  - URANIUM_ROD blocks: 4
  - CONTROL_ROD blocks: 1
[Reactor Validator] ✓ Structure validation SUCCESS

╔════════════════════════════════════════╗
║  ✓ REACTOR STRUCTURE FORMED             ║
╠════════════════════════════════════════╣
║ Dimensions: 5×4
║ Uranium Rods: 4
║ Control Rods: 1
║ Location: [xyz]
╚════════════════════════════════════════╝
```

### Ejemplo 2: Reactor 7×7×6 (Mediano)

Mismo proceso pero con más bloques y mayor altura.

---

## Validación en Consola

Cuando el reactor se forma o se rompe, verás mensajes como:

### ✓ Estructura Válida
```
[Reactor Validator] Checking structure starting at controller: [100, 64, 200]
[Reactor Validator] Structure dimensions detected: 7×6
[Reactor Validator] ✓ Structure validation SUCCESS - ReactorStructure{7×6, uranium=9, control=1, pos=[100,64,200]}

╔════════════════════════════════════════╗
║  ✓ REACTOR STRUCTURE FORMED             ║
╠════════════════════════════════════════╣
║ Dimensions: 7×6
║ Uranium Rods: 9
║ Control Rods: 1
║ Location: [100, 64, 200]
╚════════════════════════════════════════╝
```

### ❌ Estructura Inválida

Ejemplos de errores:

```
[Reactor Validator] No controller block at [100, 64, 200]
```

```
[Reactor Validator] Height 2 is less than minimum 4
```

```
[Reactor Validator] ❌ Top center is not REACTOR_CONTROLLER at [100, 64, 200]
```

```
[Reactor Validator] ❌ Center column missing HEAT_EXCHANGER at [100, 62, 200]
```

```
[Reactor Validator] ❌ Wall missing REACTOR_CASING at [97, 63, 200], found: minecraft:dirt
```

```
[Reactor Validator] ❌ No uranium rods found in reactor
```

---

## Notas Importantes

1. **Simetría**: El reactor debe ser **simétrico** en X-Z (ancho y profundidad iguales)
2. **Altura**: Puede ser distinto del ancho
3. **Centro**: Siempre en el CONTROLLER, todo se calcula desde ahí
4. **Alineación**: El CONTROLLER debe estar arriba, el HEAT_EXCHANGER abajo en línea recta
5. **Combustible**: Los rods pueden estar en cualquier posición interior, no solo en los bordes
6. **Aire Interior**: El espacio interior (no en la columna central) puede estar vacío

---

## Troubleshooting

| Problema | Causa | Solución |
|----------|-------|----------|
| "Height X is less than minimum" | Reactor muy corto | Añade más capas de CASING |
| "Width X is less than minimum" | Reactor muy estrecho | Expande el ancho a mínimo 5×5 |
| "Not enough casing blocks" | Faltan paredes | Completa el perímetro exterior |
| "Heat exchanger column is incomplete" | Falta HEAT_EXCHANGER central | Coloca todos en línea recta |
| "No uranium rods found" | No hay combustible | Coloca al menos 1 URANIUM_ROD |
| "Wall missing REACTOR_CASING" | Bloque incorrecto en pared | Reemplaza con REACTOR_CASING |

