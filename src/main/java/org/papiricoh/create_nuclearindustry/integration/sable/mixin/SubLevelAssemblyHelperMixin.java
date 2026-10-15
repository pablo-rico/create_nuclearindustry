package org.papiricoh.create_nuclearindustry.integration.sable.mixin;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sable moves every block of a ship through {@code moveBlocks}: when one is assembled, split or
 * taken apart. While it runs our blocks are only moving, not broken, and their new places are noted.
 */
@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class SubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private static void create_nuclearindustry$beforeMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform,
                                                         Iterable<BlockPos> blocks, CallbackInfo ci) {
        ShipAssembly.begin(level, transform::apply, blocks);
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private static void create_nuclearindustry$afterMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform,
                                                        Iterable<BlockPos> blocks, CallbackInfo ci) {
        ShipAssembly.end();
    }
}
