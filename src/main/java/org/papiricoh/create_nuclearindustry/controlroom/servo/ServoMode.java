package org.papiricoh.create_nuclearindustry.controlroom.servo;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import java.util.Locale;

/** Which way a stronger signal moves a servo's actuator. Saved by ordinal: new modes go last. */
public enum ServoMode implements INamedIconOptions {
    /** No signal keeps it retracted; full signal runs it out to the end of the stroke. */
    EXTEND_WITH_SIGNAL(AllIcons.I_PRIORITY_VERY_HIGH),
    /** No signal runs it out to the end of the stroke; full signal pulls it all the way back. */
    RETRACT_WITH_SIGNAL(AllIcons.I_PRIORITY_VERY_LOW);

    private final AllIcons icon;

    ServoMode(AllIcons icon) {
        this.icon = icon;
    }

    public boolean extendsWithSignal() {
        return this == EXTEND_WITH_SIGNAL;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return "create_nuclearindustry.servo.mode." + name().toLowerCase(Locale.ROOT);
    }
}
