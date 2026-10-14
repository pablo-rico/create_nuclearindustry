package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
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
 * A fired nuclear cannon shell: goes off with the warhead load it was made with, as a ground burst where it
 * hits or an airburst when its fuze sets it off high enough above the ground.
 */
public class NuclearCannonShellProjectile extends FuzedBigCannonProjectile {
    private WarheadLoad load = WarheadLoad.FULL;

    public NuclearCannonShellProjectile(EntityType<? extends NuclearCannonShellProjectile> type, Level level) {
        super(type, level);
    }

    public WarheadLoad load() {
        return load;
    }

    public void setLoad(WarheadLoad load) {
        this.load = load;
    }

    @Override
    protected void detonate(Position position) {
        if (!(level() instanceof ServerLevel serverLevel) || load.units() <= 0) {
            return;
        }
        NuclearDetonation.detonateShell(serverLevel, position, load.units(), load.enrichment());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        load.write(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        load = WarheadLoad.read(tag);
    }

    @Override
    protected void detonate() {
        detonate(position());
    }

    @Override
    public BlockState getRenderedBlockState() {
        return CBCNuclearContent.NUCLEAR_CANNON_SHELL_BLOCK.get().defaultBlockState()
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
