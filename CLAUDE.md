# Create: Nuclear Industry — guía para trabajar en el mod

Addon de Create para **NeoForge 1.21.1** (Create 6.0.10, Ponder, Flywheel; JEI y Create Big Cannons opcionales), Java 21.
Código en `src/main/java/org/papiricoh/create_nuclearindustry/`, recursos a mano en `src/main/resources/`, recursos generados en `src/generated/resources/` (no editar a mano: los rehace `runData`).

No hagas commit a no ser que te lo pida.

## Comandos

| Qué | Comando |
|---|---|
| Compilar | `./gradlew compileJava` |
| Regenerar datos (lang, modelos, blockstates, tags, loot, recetas, advancements) | `./gradlew runData` |
| Build completa + JUnit | `./gradlew build` (solo JUnit: `./gradlew test`) |
| GameTests (todos deben pasar) | `./gradlew runGameTestServer` → busca `All N required tests passed` en el log |
| Cliente de desarrollo | `./gradlew runClient` (directorio de trabajo `runs/client`) |
| Texturas | `python tools/texturegen/generate.py` (necesita Pillow) |
| Validar texturas y referencias de modelos | `python tools/texturegen/validate.py --jar build/libs/create_nuclearindustry_1.21.1_1.1.0.jar` |
| Schematics de Ponder / arenas de test | `python tools/structgen/<script>.py` |

Pillow no está en el Python del sistema: crea un venv aparte (`python -m venv <scratch>/texvenv && <scratch>/texvenv/Scripts/python -m pip install pillow`) y lanza `generate.py`/`validate.py` con ese intérprete. La salida es determinista: regenerar no debe cambiar texturas que no tocaste.

Bucle de verificación de cada cambio: `compileJava` → `runData` (revisa el diff de `src/generated`) → `build` → `runGameTestServer` → `validate.py` → prueba visual con capturas (ver abajo) si hay algo que se renderiza.

## Cómo añadir cosas

**Registro** (no hay Registrate; todo son `DeferredRegister`):
- Bloques `AllNuclearBlocks`, ítems `AllNuclearItems` (más su `BlockItem`), block entities `AllNuclearEntities`, menús `AllNuclearGUIs`, componentes `AllNuclearDataComponents`, efectos/attachments/data maps `radiation/AllNuclearRadiation`, fuentes de Display Link `AllNuclearDisplaySources`, triggers `advancement/NuclearAdvancements`.
- Pestaña creativa: `AllCreativeTabs` (núcleo) y `fillCreativeTab` de cada área. La salida es un `creativetab/SectionedOutput`: llama a `output.section(NuclearTabSection.X)` antes de los `output.accept` (aceptar sin sección lanza una excepción). Una sección nueva va en `NuclearTabSection`, con su título en `LangEntries` y su banner en `tools/texturegen/tex_creative_tab.py`.
- Renderers de BE y pantallas: `Create_NuclearIndustry.ClientModEvents` (`onRegisterRenderers`, `onRegisterScreens`). Partial models: créalos en el constructor del mod (`turbine/client/NuclearClient.init`, p. ej. `DeskPartials`, `NuclearPartialModels`).
- Paquetes de red: `RegisterPayloadHandlersEvent` (ejemplo: `controlroom/desk/ConfigureModulePayload`). Valida siempre en el servidor (distancia, que el objetivo exista, sanea textos y rangos).
- Block entities con lógica: preferir `SmartBlockEntity` + `IBE` de Create (ticker automático, `write/read(tag, registries, clientPacket)`, `notifyUpdate()` para sincronizar). `setRemoved` es `final` en `SmartBlockEntity`: usa `invalidate()`/`remove()`.

**Áreas (`NuclearFeature`)**: el contenido nuevo de PWR (`reactor/pwr/`), combustible gastado (`spentfuel/`), residuos (`waste/`), instrumentos de sala de control (`controlroom/ControlRoom2*`) y defensa civil (`civildefense/`) no toca los registros compartidos: cada área tiene
- `<Area>Registry implements NuclearFeature` (registra en `AllNuclearBlocks.BLOCKS`, `AllNuclearItems.ITEMS`, `AllNuclearEntities.ENTITIES`, `AllNuclearGUIs.MENUS`… desde `register(bus)`; capacidades y pestaña creativa con sus hooks);
- `client/<Area>Client implements NuclearFeatureClient` (partials en `init`, renderers, pantallas, geometry loaders);
- `<Area>Config` (`define(builder)` con claves `<área>.*` y `bake()` copiando a estáticos públicos);
- `data/<Area>DataGen extends AreaDataGen` (lang con `LangEntries.add/item/block/fluid`, blockstates, modelos, tags con `tagFor`, data maps con `builderFor`, loot con `addTable`/`addDropOther`, y `addProviders` para sus propios generadores de recetas);
- texturas en `tools/texturegen/tex_<área>.py` (`@block`, `@item`, `@gui`).
Contratos compartidos: `PlantReadings`/`ReactorBlockEntity.plantReadings()`, `PlantReadable` + `TargetKind.STEAM_GENERATOR/CONTAINMENT`, índices `R_PRESSURE…R_PWR`, `S_*`, `N_*` de `MonitorReading`, hooks `SpentFuelHooks`, `RadioactivityModifiers`, `ExposureModifiers`, el evento `NuclearDetonationEvent` y `multiblock/SealedVolume`. Las claves de lang duplicadas hacen fallar `runData`. `AlarmMetric` se guarda por ordinal: métricas nuevas siempre al final.

**Datagen** (`data/`): blockstates y modelos en `NuclearBlockStateProvider`, modelos planos de ítems en la lista de `NuclearItemModelProvider`, tags en `NuclearBlockTagsProvider` (las piezas de multibloque van en `MultiblockTags.MULTIBLOCK_PARTS`), data maps en `NuclearDataMapProvider`, recetas en `data/recipe/*` (vanilla con `ShapedRecipeBuilder`, Create con `MixingRecipeGen`, etc.; ítems de Create con `CreateItems.get("iron_sheet")`), advancements en `NuclearAdvancementProvider`. La loot table "dropSelf" sale sola. Modelos complejos (multipart, partials, ítems compuestos con `neoforge:composite`) se escriben a mano en `src/main/resources/assets/.../models/` y el datagen los referencia con `getExistingFile`. Si un archivo pasa de generado a manual, borra el generado o `processResources` fallará por duplicado.

**Textos**: todo string visible va en `data/LangEntries.java` con `add(key, inglés, español)` (o `item/block/fluid(...)`); genera `en_us` y `es_es` a la vez. No escribas en los `lang/*.json`. Las escenas de Ponder usan claves `create_nuclearindustry.ponder.<id>.header` y `.text_N` en el orden de los `showText`.

**Texturas**: cada textura es una función en `tools/texturegen/` (`blocks.py`, `items.py`, `gui.py`, `armor.py` para capas de armadura e iconos de efecto), con la paleta de `palette.py`. Las de bloque deben ser opacas. Las GUI deben coincidir píxel a píxel con las coordenadas de la pantalla Java.

**Config**: patrón de `Config.java` (spec `BUILDER.define…`, campo estático público y asignación en `bake()`); documentarla en español en `docs/configuration.md`. Los tests cambian los estáticos dentro de `try/finally`.

**Ponder**: escenas en `infrastructure/ponder/scenes/`, registradas en `AllNuclearPonderScenes`. La geometría vive en un schematic generado por `tools/structgen/ponder_*.py` (`nbtwriter.structure`), no en `setBlocks` por encima del tamaño del NBT. La cámara de Ponder mira desde el noroeste: las caras visibles son norte y oeste.

**JEI**: páginas de info en `integration/jei/NuclearJeiPlugin.addInfo`.

**Documentación de cambios**: entrada en `CHANGELOG.md` (sección "Unreleased"), en inglés, describiendo lo que nota el jugador.

## Tests

- **GameTests** en `gametest/`, clases con `@GameTestHolder(Create_NuclearIndustry.MODID) @PrefixGameTestTemplate(false)` y `@GameTest(template = NuclearGameTests.SMALL_ARENA)` (también `TOKAMAK_ARENA`, `LONG_ARENA`, `TOWER_ARENA`). Constructores de estructuras: `FissionTestBuilder` (reactor 5x5x5; `column(...)` para columnas libres), `TokamakTestBuilder`, etc.
- Dentro de `succeedWhen`/`runAfterDelay` **solo deben fallar aserciones** (`helper.assertTrue`). Una excepción (NPE, `orElseThrow`) **tumba el servidor de tests entero**: comprueba primero con `assertTrue(x != null, …)`.
- Lo que toque contaminación de chunks va en un batch propio (`batch = "contamination_<nombre>"`) y se limpia (`ContaminationTests.clean`, quitar el evento/fuente añadidos).
- Jugadores: `helper.makeMockPlayer(GameType.SURVIVAL)` sirve para dosis/ítems. `makeMockServerPlayerInLevel()` rompe por un paquete de Create Big Cannons, y NeoForge nunca concede advancements a `FakePlayer`: prueba el árbol y el trigger, no la concesión.
- **JUnit** (`src/test/java`) para lógica pura sin clases del juego (modelos de física, geometría). Extrae la lógica a clases sin `Config` para poder probarla.
- Nuevas mecánicas → nuevo test que las cubra; nunca cambies un test para que pase sin entender por qué fallaba.

## Probar en el juego con capturas

Los gametests no ven el renderizado (BER, modelos, texturas, GUI). Para todo lo visual hay que abrir el cliente y mirar. Se hace con un **recorrido automático temporal** que no se commitea:

1. **Mundo de pruebas**: copia un mundo existente para no tocar el del usuario: `cp -r "runs/client/saves/New World" runs/client/saves/nuclear_tour` (borra su `session.lock`). Crea el indicador `runs/client/nuclear_tour.flag`.
2. **Clase temporal** `dev/DevScreenshotTour.java`, enganchada en `NuclearClient.init` solo si existe el indicador (`NeoForge.EVENT_BUS.addListener(...)` sobre `ClientTickEvent.Post`). Esqueleto:
   - En el `TitleScreen`: guarda y pon `mc.options.pauseOnLostFocus = false` (si no, el juego se pausa sin foco y el servidor integrado deja de tickear) y abre el mundo con `mc.createWorldOpenFlows().openWorld("nuclear_tour", () -> {})`.
   - Con `mc.player` y `mc.getSingleplayerServer()` listos, ejecuta una lista de pasos por número de tick:
     - comandos de servidor con `server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "..."))`: `gamemode creative @a`, `time set 6000`, `weather clear`, `gamerule doDaylightCycle false`, `tp @a x y z`, una plataforma con `fill … smooth_stone` y aire encima a y≈200 sobre el spawn, y `kill @e[type=item]` tras reconstruir;
     - construcción con código en el hilo del servidor (`server.execute(() -> …)`): `level.setBlock`, recalcular estados que dependen de vecinos (p. ej. `ControlDeskBlock.withConnections`) y configurar block entities (enlaces, módulos…);
     - para cada vista: `tp` a la posición y, durante unos 35 ticks, en el cliente `mc.player.setDeltaMovement(Vec3.ZERO)`, `mc.player.getAbilities().flying = true` y `mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, objetivo)` (el `tp … facing` no siempre deja la cámara bien); después `Screenshot.grab(mc.gameDirectory, "tour_<n>.png", mc.getMainRenderTarget(), m -> {})`;
     - `mc.options.hideGui = true` para vistas limpias; `false` para capturar HUD, etiquetas, barra rápida o una GUI (ábrela desde el servidor, p. ej. `ControlModuleMenu.open(player, pos, slot)`);
     - al final restaura `pauseOnLostFocus` y llama a `mc.stop()`.
3. **Lanza** `./gradlew runClient` (en segundo plano; tarda ~1–2 min). Las capturas quedan en `runs/client/screenshots/`. **Míralas** una a una: vistas generales, primeros planos, perfiles laterales, esquinas, la GUI y los ítems de la barra rápida. Repite el recorrido tras cada corrección.
4. **Si peta**, el informe está en `runs/client/crash-reports/` (el más reciente); léelo antes de tocar nada.
5. **Limpia** antes del commit: borra `dev/`, el enganche en `NuclearClient`, el indicador y el mundo `nuclear_tour`. `git status` no debe mostrar nada de eso.

Cosas que el recorrido ya destapó y conviene tener presentes al renderizar:
- **No reutilices un `VertexConsumer`** de `buffers.getBuffer(...)` después de dibujar texto u otro `RenderType`: el buffer compartido se cierra y el cliente peta con "Not building!". Pide el buffer justo antes de cada `renderInto`/dibujo y deja el texto para el final.
- Texto en el mundo: comprueba que etiquetas y valores no se solapen en las pantallas pequeñas y que no tapen lo que describen.
- Orientaciones: el lado del operador de un pupitre, el sentido de una pendiente o de una esquina solo se ven en el juego; capturas de perfil y desde detrás.

## Convenciones

- Código y comentarios en inglés, al estilo del archivo que tocas (Javadoc corto que explica el porqué). Documentación de usuario (`docs/`) en español; CHANGELOG en inglés.
- Mecánicas nuevas: física y lógica testeables, configurables cuando tenga sentido, visibles para el jugador (gafas, JEI, Ponder, Display Link) y en EN+ES.
- No toques `runs/junit/*` ni archivos que no son del cambio. Un commit por funcionalidad, con mensaje descriptivo.
