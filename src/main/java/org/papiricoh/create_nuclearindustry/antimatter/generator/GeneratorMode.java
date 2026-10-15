package org.papiricoh.create_nuclearindustry.antimatter.generator;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;

/**
 * The annihilation generator's modes on Create's value panel: the physics' {@link AnnihilationModel.Mode}
 * with an icon and a name, kept in the same order so the ordinal Create saves matches it.
 */
public enum GeneratorMode implements INamedIconOptions {
    ROTATION(AllIcons.I_REFRESH),
    ELECTRIC(AllIcons.I_FX_FIELD_ON),
    SPLIT(AllIcons.I_TUNNEL_SPLIT);

    private final AllIcons icon;

    GeneratorMode(AllIcons icon) {
        this.icon = icon;
    }

    public AnnihilationModel.Mode model() {
        return AnnihilationModel.Mode.values()[ordinal()];
    }

    public static GeneratorMode of(AnnihilationModel.Mode mode) {
        return values()[mode.ordinal()];
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return langKey(model());
    }

    /** The lang key of a mode's name. */
    public static String langKey(AnnihilationModel.Mode mode) {
        return "create_nuclearindustry.annihilation_generator.mode." + mode.id();
    }
}
