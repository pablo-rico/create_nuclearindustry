package org.papiricoh.create_nuclearindustry.fuelcycle.fan;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleConfig;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

import java.util.List;
import java.util.Optional;

/**
 * The mod's two bulk fan treatments, next to Create's washing, smoking, blasting and haunting:
 * <ul>
 *   <li>{@code acid_leaching}: a fan blowing through sulfuric acid ({@link #ACID_LEACHING_FLUIDS},
 *       {@link #ACID_LEACHING_BLOCKS}) runs {@link AllNuclearRecipeTypes#ACID_LEACHING};</li>
 *   <li>{@code irradiation}: a fan blowing past a radioactive block ({@link #IRRADIATION_BLOCKS}, or any
 *       block whose item gives off at least {@link FuelCycleConfig#irradiationMinActivity}) runs
 *       {@link AllNuclearRecipeTypes#IRRADIATION}.</li>
 * </ul>
 * Create sorts every type by priority once its registry freezes, so ours slot in between its own.
 */
public final class NuclearFanTypes {
    public static final TagKey<Fluid> ACID_LEACHING_FLUIDS = TagKey.create(Registries.FLUID, id("fan_processing_catalysts/acid_leaching"));
    public static final TagKey<Block> ACID_LEACHING_BLOCKS = TagKey.create(Registries.BLOCK, id("fan_processing_catalysts/acid_leaching"));
    public static final TagKey<Block> IRRADIATION_BLOCKS = TagKey.create(Registries.BLOCK, id("fan_processing_catalysts/irradiation"));

    private static final DeferredRegister<FanProcessingType> TYPES =
            DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<FanProcessingType, AcidLeachingType> ACID_LEACHING =
            TYPES.register("acid_leaching", AcidLeachingType::new);
    public static final DeferredHolder<FanProcessingType, IrradiationType> IRRADIATION =
            TYPES.register("irradiation", IrradiationType::new);

    private NuclearFanTypes() {}

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    /** What a recipe of {@code type} makes of {@code stack}, or null when none matches. */
    @Nullable
    static List<ItemStack> apply(AllNuclearRecipeTypes type, ItemStack stack, Level level) {
        return find(type, stack, level).map(RecipeHolder::value)
                .map(recipe -> RecipeApplier.applyRecipeOn(level, stack, recipe, true))
                .orElse(null);
    }

    static Optional<RecipeHolder<Recipe<SingleRecipeInput>>> find(AllNuclearRecipeTypes type, ItemStack stack, Level level) {
        RecipeType<Recipe<SingleRecipeInput>> recipeType = type.getType();
        return level.getRecipeManager().getRecipeFor(recipeType, new SingleRecipeInput(stack), level);
    }

    /** Fan processing through sulfuric acid. */
    public static class AcidLeachingType implements FanProcessingType {
        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            return level.getFluidState(pos).is(ACID_LEACHING_FLUIDS) || level.getBlockState(pos).is(ACID_LEACHING_BLOCKS);
        }

        @Override
        public int getPriority() {
            // Between Create's splashing (400) and haunting (300).
            return 350;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return find(AllNuclearRecipeTypes.ACID_LEACHING, stack, level).isPresent();
        }

        @Override
        @Nullable
        public List<ItemStack> process(ItemStack stack, Level level) {
            return apply(AllNuclearRecipeTypes.ACID_LEACHING, stack, level);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            if (level.random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.SPIT, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
                        pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            }
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(random.nextBoolean() ? 0xE3D46A : 0xC9B84A);
            particleAccess.setAlpha(1f);
        }

        @Override
        public void affectEntity(Entity entity, Level level) {}
    }

    /** Fan processing past a strongly radioactive block. */
    public static class IrradiationType implements FanProcessingType {
        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.is(IRRADIATION_BLOCKS)) {
                return true;
            }
            Radioactivity radioactivity = state.getBlock().asItem().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
            return radioactivity != null && radioactivity.perSecond() >= FuelCycleConfig.irradiationMinActivity;
        }

        @Override
        public int getPriority() {
            // Between Create's haunting (300) and smoking (200).
            return 250;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return find(AllNuclearRecipeTypes.IRRADIATION, stack, level).isPresent();
        }

        @Override
        @Nullable
        public List<ItemStack> process(ItemStack stack, Level level) {
            return apply(AllNuclearRecipeTypes.IRRADIATION, stack, level);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            if (level.random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
                        pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            }
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(random.nextBoolean() ? 0x7CFF4A : 0x4FD62E);
            particleAccess.setAlpha(1f);
        }

        @Override
        public void affectEntity(Entity entity, Level level) {}
    }
}
