package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonCommonShellProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonFuzePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonProjectilePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;

import javax.annotation.Nonnull;

/**
 * A fired thermonuclear cannon shell: goes off with the parts it was made with, as a ground burst where it
 * hits or an airburst when its fuze sets it off high enough above the ground.
 */
public class ThermonuclearCannonShellProjectile extends FuzedBigCannonProjectile {
    private ThermonuclearLoad load = ThermonuclearLoad.SHELL_FULL;

    public ThermonuclearCannonShellProjectile(EntityType<? extends ThermonuclearCannonShellProjectile> type, Level level) {
        super(type, level);
    }

    public ThermonuclearLoad load() {
        return load;
    }

    public void setLoad(ThermonuclearLoad load) {
        this.load = load;
    }

    @Override
    protected void detonate(Position position) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        NuclearDetonation.detonateShellThermonuclear(serverLevel, position, load.stats(ExplosiveConfig.thermonuclearCaps()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        load.write(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        load = ThermonuclearLoad.read(tag);
    }

    @Override
    protected void detonate() {
        detonate(position());
    }

    @Override
    public BlockState getRenderedBlockState() {
        return CBCNuclearContent.THERMONUCLEAR_CANNON_SHELL_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH);
    }

    @Nonnull
    @Override
    protected BigCannonFuzePropertiesComponent getFuzeProperties() {
        return getAllProperties().fuze();
    }

    @Nonnull
    @Override
    protected BigCannonProjectilePropertiesComponent getBigCannonProjectileProperties() {
        return getAllProperties().bigCannonProperties();
    }

    @Nonnull
    @Override
    public EntityDamagePropertiesComponent getDamageProperties() {
        return getAllProperties().damage();
    }

    @Nonnull
    @Override
    protected BallisticPropertiesComponent getBallisticProperties() {
        return getAllProperties().ballistics();
    }

    protected BigCannonCommonShellProperties getAllProperties() {
        return CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE.getPropertiesOf(this);
    }
}
