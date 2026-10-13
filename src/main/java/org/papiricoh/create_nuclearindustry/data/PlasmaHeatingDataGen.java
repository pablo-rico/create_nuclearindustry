package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.PlasmaHeatingRecipes;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

import java.util.concurrent.CompletableFuture;

/** Lang, models, tags, data maps, loot and recipes for the tokamak's neutral beam injectors, radio-frequency antennas, viewports, plasma rendering and sounds. */
final class PlasmaHeatingDataGen extends AreaDataGen {
    static final PlasmaHeatingDataGen INSTANCE = new PlasmaHeatingDataGen();

    private PlasmaHeatingDataGen() {}

    @Override
    void lang() {
        LangEntries.block("fusion_neutral_beam_injector", "Neutral Beam Injector", "Inyector de haces neutros");
        LangEntries.block("fusion_rf_antenna", "RF Heating Antenna", "Antena de calentamiento por radiofrecuencia");
        LangEntries.block("fusion_viewport", "Tokamak Viewport", "Ventana del tokamak");

        String g = "create_nuclearindustry.plasma_heating.goggles.";
        LangEntries.add(g + "too_slow", "Too slow: %s RPM, gives nothing at %s RPM or less",
                "Demasiado lento: %s RPM, no da nada a %s RPM o menos");
        LangEntries.add(g + "speed", "Speed: %s RPM (full power from %s RPM)", "Velocidad: %s RPM (potencia plena desde %s RPM)");
        LangEntries.add(g + "power", "Heating power: %s / %s MW", "Potencia de calentamiento: %s / %s MW");
        LangEntries.add(g + "no_tokamak", "Not in the shell of a formed tokamak", "No está en la carcasa de un tokamak formado");
        LangEntries.add(g + "plasma", "Plasma: %s, %s°", "Plasma: %s, %s°");
        LangEntries.add(g + "heating_off", "Auxiliary heating is switched off at the controller",
                "El calentamiento auxiliar está apagado en el controlador");
        LangEntries.add(g + "heating_level", "Heating setpoint: %s%%", "Consigna de calentamiento: %s%%");
        LangEntries.add(g + "waiting", "Heats the plasma once it is lit", "Calienta el plasma cuando esté encendido");
        LangEntries.add("create_nuclearindustry.plasma_heating.neutral_beam.deuterium", "Beams use %s mB/t of the tokamak's deuterium",
                "Los haces gastan %s mB/t del deuterio del tokamak");
        String rf = "create_nuclearindustry.plasma_heating.rf_antenna.";
        LangEntries.add(rf + "too_thin", "Plasma too thin for the waves: density %s%% of its limit, needs over %s%%",
                "Plasma demasiado tenue para las ondas: densidad del %s%% de su límite, necesita más del %s%%");
        LangEntries.add(rf + "coupling", "Waves coupling about %s%% (density %s%% of its limit)",
                "Las ondas acoplan un %s%% (densidad del %s%% de su límite)");

        jeiPages();
    }

    /** JEI information pages (listed in PlantJeiInfo); the numbers are the config defaults. */
    private static void jeiPages() {
        String info = "jei.create_nuclearindustry.info.";
        LangEntries.add(info + "neutral_beam_injector",
                "Auxiliary heating for a tokamak. Build it into the shell in place of a casing block, facing out, and turn the shaft on its outer face: at 128 RPM it fires 20 MW of fast deuterium atoms into the plasma, nothing at 32 RPM or less and in proportion in between. It is very heavy, 32768 SU per RPM, so a megawatt of beam costs more than the plasma turbine makes from a megawatt of fusion: run it to heat the plasma up and ease it off once it burns. The beams take 0.02 mB/t of deuterium per MW from the tokamak and lose power when the tank runs low. They heat at any plasma density. Goggles show its power and the plasma.",
                "Calentamiento auxiliar del tokamak. Colócalo en la carcasa en lugar de un bloque de revestimiento, mirando hacia fuera, y haz girar el eje de su cara exterior: a 128 RPM dispara al plasma 20 MW de átomos de deuterio rápidos; a 32 RPM o menos no da nada, y entre medias da la parte proporcional. Pesa mucho, 32768 SU por RPM, así que un megavatio de haz cuesta más de lo que la turbina de plasma saca de un megavatio de fusión: úsalo para calentar el plasma y redúcelo cuando arda. Los haces toman del tokamak 0,02 mB/t de deuterio por MW y pierden potencia si falta en el tanque. Calientan con cualquier densidad del plasma. Las gafas muestran su potencia y el estado del plasma.");
        LangEntries.add(info + "rf_antenna",
                "Radio-frequency heating for a tokamak: a shell block with a shaft on its outer face, like the Neutral Beam Injector. At 64 RPM it sends 12 MW into the plasma (nothing at 16 RPM or less) for 13312 SU per RPM and no fuel, but the waves only couple to a plasma denser than about 15% of its density limit, fully above about 35%: start the plasma with the solenoid and the beams, then let the antennas carry it. Goggles warn when the plasma is too thin.",
                "Calentamiento por radiofrecuencia del tokamak: un bloque de la carcasa con un eje en su cara exterior, como el Inyector de haces neutros. A 64 RPM envía 12 MW al plasma (nada a 16 RPM o menos) por 13312 SU por RPM y sin combustible, pero las ondas solo acoplan con un plasma de más del 15% de su límite de densidad, y del todo por encima del 35%: arranca el plasma con el solenoide y los haces y deja que luego lo mantengan las antenas. Las gafas avisan cuando el plasma es demasiado tenue.");
        LangEntries.add(info + "tokamak_viewport",
                "A quartz window for the tokamak shell: it seals the chamber like a casing block and lets you watch the plasma inside. It is the hall's light while the plasma burns: dark when there is none, a dim glow at breakdown and brighter as the plasma heats up. Several viewports side by side make one big window.",
                "Una ventana de cuarzo para la carcasa del tokamak: cierra la cámara como un bloque de revestimiento y deja ver el plasma de dentro. Es la luz de la sala mientras arde el plasma: oscura cuando no hay, un brillo tenue en la ruptura del gas y más intensa cuanto más se calienta. Varias ventanas juntas forman un ventanal.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        facingStates(provider, PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get(), "fusion_neutral_beam_injector");
        facingStates(provider, PlasmaHeatingRegistry.RF_ANTENNA.get(), "fusion_rf_antenna");
        // Every glow level shows the same window; only the light changes.
        ModelFile viewport = provider.models().getExistingFile(provider.modLoc("block/fusion_viewport"));
        provider.getVariantBuilder(PlasmaHeatingRegistry.VIEWPORT.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(viewport).build());
        provider.simpleBlockItem(PlasmaHeatingRegistry.VIEWPORT.get(), viewport);
    }

    /** Hand-written model facing north, turned so its front faces {@code FACING}. */
    private static void facingStates(NuclearBlockStateProvider provider, Block block, String model) {
        ModelFile file = provider.models().getExistingFile(provider.modLoc("block/" + model));
        provider.getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            return ConfiguredModel.builder().modelFile(file).rotationX(x).rotationY(y).build();
        });
        provider.simpleBlockItem(block, file);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        PlasmaHeatingRegistry.PARTS.forEach(block -> parts.add(block.get()));
        provider.tagFor(BlockTags.IMPERMEABLE).add(PlasmaHeatingRegistry.VIEWPORT.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Plasma heating crafting recipes", new PlasmaHeatingRecipes(output, lookup)));
    }

    /** Providers need unique names, and the shared recipe providers already hold the default ones. */
    private record Named(String name, DataProvider provider) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return provider.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
