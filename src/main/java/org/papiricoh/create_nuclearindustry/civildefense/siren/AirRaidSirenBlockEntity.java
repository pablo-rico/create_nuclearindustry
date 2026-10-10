package org.papiricoh.create_nuclearindustry.civildefense.siren;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatBoard;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;

import java.util.List;

/**
 * Checks twice a second (and on every redstone change) whether to sound: redstone sounds a steady
 * alert, a threat in reach the faster attack warning. The cause and the impact time go to clients
 * for the tone and the goggles.
 */
public class AirRaidSirenBlockEntity extends SmartBlockEntity implements SoundingSiren, IHaveGoggleInformation {
    private static final int CHECK_INTERVAL = 10;

    /** Why the siren is sounding. */
    public enum Cause {
        NONE, REDSTONE, THREAT
    }

    private Cause cause = Cause.NONE;
    /** Game time the threat that sounds the siren lands at. */
    private long impactAt;
    private int timer;

    public AirRaidSirenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            SoundingSiren.ClientHooks.tick(this);
            return;
        }
        if (++timer >= CHECK_INTERVAL) {
            timer = 0;
            evaluate();
        }
    }

    /** Looks at redstone and the threats in reach and sounds or falls silent. Public so tests can drive it. */
    public void evaluate() {
        if (level == null || level.isClientSide) {
            return;
        }
        ThreatBoard.Threat threat = ThreatRegistry.threatening(level, worldPosition, CivilDefenseConfig.sirenThreatRadius);
        Cause next = level.hasNeighborSignal(worldPosition) ? Cause.REDSTONE : threat != null ? Cause.THREAT : Cause.NONE;
        long nextImpact = threat != null ? threat.impactAt() : 0;
        if (next != cause || nextImpact != impactAt) {
            cause = next;
            impactAt = nextImpact;
            notifyUpdate();
        }
        boolean sounding = next != Cause.NONE;
        BlockState state = getBlockState();
        if (state.getValue(AirRaidSirenBlock.SOUNDING) != sounding) {
            level.setBlock(worldPosition, state.setValue(AirRaidSirenBlock.SOUNDING, sounding), Block.UPDATE_ALL);
        }
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public boolean isSounding() {
        return getBlockState().getValue(AirRaidSirenBlock.SOUNDING);
    }

    @Override
    public float pitch(double ticks) {
        return SirenTone.wail(ticks, cause == Cause.THREAT ? SirenTone.ATTACK_PERIOD : SirenTone.ALERT_PERIOD);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.air_raid_siren"))
                .withStyle(ChatFormatting.GOLD));
        switch (cause) {
            case NONE -> tooltip.add(indent(Component.translatable("create_nuclearindustry.siren.silent").withStyle(ChatFormatting.GRAY)));
            case REDSTONE -> tooltip.add(indent(Component.translatable("create_nuclearindustry.siren.alert").withStyle(ChatFormatting.YELLOW)));
            case THREAT -> {
                tooltip.add(indent(Component.translatable("create_nuclearindustry.siren.attack").withStyle(ChatFormatting.RED)));
                long ticks = level == null ? 0 : impactAt - level.getGameTime();
                tooltip.add(indent(ticks > 0
                        ? Component.translatable("create_nuclearindustry.siren.impact_in", (ticks + 19) / 20).withStyle(ChatFormatting.RED)
                        : Component.translatable("create_nuclearindustry.siren.hazard").withStyle(ChatFormatting.RED)));
            }
        }
        tooltip.add(indent(Component.translatable("create_nuclearindustry.siren.reach", CivilDefenseConfig.sirenThreatRadius)
                .withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    static Component indent(Component line) {
        return Component.literal("     ").append(line);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Cause", cause.name());
        tag.putLong("ImpactAt", impactAt);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        try {
            cause = Cause.valueOf(tag.getString("Cause"));
        } catch (IllegalArgumentException e) {
            cause = Cause.NONE;
        }
        impactAt = tag.getLong("ImpactAt");
    }
}
