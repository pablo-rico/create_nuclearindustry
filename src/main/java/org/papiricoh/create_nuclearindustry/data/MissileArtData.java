package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import org.papiricoh.create_nuclearindustry.missile.silo.LaunchBaseBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionBlock.Joint;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketTankBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketWarheadBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloFuelPortBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.Locale;

/**
 * The launch site's blockstates; every model is hand-written under {@code models/block/<id>/} (textures in {@code tex_missile.py},
 * {@code tex_launch_base.py} and {@code tex_silo_controller.py}), and each item model is its block's {@code item} model.
 *
 * <p>A rocket section is multipart, so a stack draws one fuselage:
 * <ul>
 *   <li>{@code body}: the section itself; an engine has {@code body_first} with nothing under it (open nozzle and fins) and
 *       {@code body_upper} over another section (interstage round a vacuum nozzle); a tank {@code body_<propellant>}; a
 *       warhead {@code body_<charge>};</li>
 *   <li>{@code top_cap}/{@code bottom_cap} where it meets nothing, {@code top_joint}/{@code bottom_joint} where it meets
 *       another kind of section; nothing where it meets its own kind;</li>
 *   <li>a tank with liquids adds {@code frost_<fill>}.</li>
 * </ul>
 * The controller and the fuel port are modelled facing north and turned; the launch base is {@code single} alone and one of nine
 * {@code formed_<part>} models when formed.
 */
final class MissileArtData {
    private MissileArtData() {
    }

    static void blockStates(NuclearBlockStateProvider provider) {
        sections(provider);
        provider.horizontalBlock(SiloRegistry.CONTROLLER.get(), model(provider, "silo_controller/body"));
        ModelFile port = model(provider, "silo_fuel_port/body");
        provider.getVariantBuilder(SiloRegistry.FUEL_PORT.get()).forAllStates(state -> {
            Direction facing = state.getValue(SiloFuelPortBlock.FACING);
            var builder = ConfiguredModel.builder().modelFile(port);
            return switch (facing) {
                case UP -> builder.rotationX(270).build();
                case DOWN -> builder.rotationX(90).build();
                default -> builder.rotationY(((int) facing.toYRot() + 180) % 360).build();
            };
        });
        provider.getVariantBuilder(SiloRegistry.LAUNCH_BASE.get()).forAllStates(state -> {
            LaunchBaseBlock.Part part = state.getValue(LaunchBaseBlock.PART);
            String name = part == LaunchBaseBlock.Part.NONE ? "single" : "formed_" + part.getSerializedName();
            return ConfiguredModel.builder().modelFile(model(provider, "launch_base/" + name)).build();
        });
    }

    private static void sections(NuclearBlockStateProvider provider) {
        for (var holder : java.util.List.of(SiloRegistry.ENGINE, SiloRegistry.TANK, SiloRegistry.GUIDANCE, SiloRegistry.WARHEAD,
                SiloRegistry.KILL_VEHICLE, SiloRegistry.SATELLITE_FAIRING)) {
            RocketSectionBlock block = holder.get();
            String id = holder.getId().getPath();
            MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
            if (block == SiloRegistry.ENGINE.get()) {
                builder.part().modelFile(model(provider, id + "/body_first")).addModel().condition(RocketSectionBlock.BOTTOM, Joint.NONE).end();
                builder.part().modelFile(model(provider, id + "/body_upper")).addModel().condition(RocketSectionBlock.BOTTOM, Joint.SAME, Joint.OTHER).end();
            } else if (block instanceof RocketTankBlock) {
                for (RocketTankBlock.Propellant propellant : RocketTankBlock.Propellant.values())
                    builder.part().modelFile(model(provider, id + "/body_" + name(propellant))).addModel().condition(RocketTankBlock.PROPELLANT, propellant).end();
                for (int fill = 1; fill <= 4; fill++)
                    builder.part().modelFile(model(provider, id + "/frost_" + fill)).addModel()
                            .condition(RocketTankBlock.PROPELLANT, RocketTankBlock.Propellant.LIQUID).condition(RocketTankBlock.FILL, fill).end();
            } else if (block instanceof RocketWarheadBlock) {
                for (RocketWarheadBlock.Charge charge : RocketWarheadBlock.Charge.values())
                    builder.part().modelFile(model(provider, id + "/body_" + name(charge))).addModel().condition(RocketWarheadBlock.CHARGE, charge).end();
            } else builder.part().modelFile(model(provider, id + "/body")).addModel().end();
            builder.part().modelFile(model(provider, id + "/top_cap")).addModel().condition(RocketSectionBlock.TOP, Joint.NONE).end();
            builder.part().modelFile(model(provider, id + "/top_joint")).addModel().condition(RocketSectionBlock.TOP, Joint.OTHER).end();
            builder.part().modelFile(model(provider, id + "/bottom_cap")).addModel().condition(RocketSectionBlock.BOTTOM, Joint.NONE).end();
            builder.part().modelFile(model(provider, id + "/bottom_joint")).addModel().condition(RocketSectionBlock.BOTTOM, Joint.OTHER).end();
        }
    }

    private static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static ModelFile model(NuclearBlockStateProvider provider, String path) {
        return provider.models().getExistingFile(provider.modLoc("block/" + path));
    }
}
