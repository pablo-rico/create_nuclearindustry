package org.papiricoh.create_nuclearindustry.fuelcycle.fan;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleConfig;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

import java.util.List;
import java.util.Optional;

/**
 * The mod's two bulk fan treatments, next to Create's washing, smoking, blasting and haunting:
 * <ul>
 *   <li>{@code acid_leaching}: a fan blowing through sulfuric acid ({@link #ACID_LEACHING_FLUIDS},
 *       {@link #ACID_LEACHING_BLOCKS}) runs {@link AllNuclearRecipeTypes#ACID_LEACHING}. The yellow
 *       spray sizzles on the items and burns creatures caught in it;</li>
 *   <li>{@code irradiation}: a fan blowing past a radioactive block ({@link #IRRADIATION_BLOCKS}, or any
 *       block whose item gives off at least {@link FuelCycleConfig#irradiationMinActivity}) runs
 *       {@link AllNuclearRecipeTypes#IRRADIATION}. The green current crackles and doses creatures.</li>
 * </ul>
 * As with Create's own types, the last catalyst the air passes decides the treatment downstream:
 * a fan blowing through water and then acid leaches, through acid and then water washes. Create sorts
 * every type by priority once its registry freezes; priority only settles which type a single block
 * counts as when it would be the catalyst of two.
 */
public final class NuclearFanTypes {
    public static final TagKey<Fluid> ACID_LEACHING_FLUIDS = TagKey.create(Registries.FLUID, id("fan_processing_catalysts/acid_leaching"));
    public static final TagKey<Block> ACID_LEACHING_BLOCKS = TagKey.create(Registries.BLOCK, id("fan_processing_catalysts/acid_leaching"));
    public static final TagKey<Block> IRRADIATION_BLOCKS = TagKey.create(Registries.BLOCK, id("fan_processing_catalysts/irradiation"));
    /** Damage from an acid spray. */
    public static final ResourceKey<DamageType> ACID_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("acid"));

    private static final DeferredRegister<FanProcessingType> TYPES =
            DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<FanProcessingType, AcidLeachingType> ACID_LEACHING =
            TYPES.register("acid_leaching", AcidLeachingType::new);
    public static final DeferredHolder<FanProcessingType, IrradiationType> IRRADIATION =
            TYPES.register("irradiation", IrradiationType::new);

    /** Ticks between two hits of the acid spray on the same creature. */
    private static final int ACID_INTERVAL = 20;
    private static final DustParticleOptions ACID_MIST = new DustParticleOptions(new Vector3f(0.89f, 0.83f, 0.42f), 0.8f);
    private static final DustParticleOptions RADIATION_GLOW = new DustParticleOptions(new Vector3f(0.49f, 1.0f, 0.29f), 0.6f);

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

    /** Whether the irradiation treatment counts {@code state} as its catalyst. */
    public static boolean isIrradiationCatalyst(BlockState state) {
        if (state.is(IRRADIATION_BLOCKS)) {
            return true;
        }
        if (state.isAir()) {
            return false;
        }
        Radioactivity radioactivity = state.getBlock().asItem().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        return radioactivity != null && radioactivity.perSecond() >= FuelCycleConfig.irradiationMinActivity;
    }

    /**
     * Adds one tick of an irradiating air current's dose to {@code living}: through worn shielding
     * and iodide for players, who then feel it through the usual radiation handling; plain dose and
     * sickness for other creatures, which have no dose handling of their own.
     */
    public static void irradiate(LivingEntity living) {
        if (!Config.radiationEnabled || FuelCycleConfig.irradiationDosePerSecond <= 0) {
            return;
        }
        double dose = FuelCycleConfig.irradiationDosePerSecond / 20.0 * Config.radiationDoseMultiplier;
        if (living instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            dose *= (1.0 - RadiationHandler.shielding(player)) * RadiationHandler.absorption(player);
        }
        if (dose <= 0) {
            return;
        }
        float total = living.getData(AllNuclearRadiation.DOSE) + (float) dose;
        living.setData(AllNuclearRadiation.DOSE, total);
        living.setData(AllNuclearRadiation.TOTAL_DOSE, living.getData(AllNuclearRadiation.TOTAL_DOSE) + (float) dose);
        if (!(living instanceof Player) && total >= Config.radiationSicknessThreshold) {
            living.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_SICKNESS, 60, 0, true, true));
        }
    }

    private static void playNear(Level level, Vec3 pos, DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> sound,
                                 float volume) {
        level.playLocalSound(pos.x, pos.y, pos.z, sound.get(), SoundSource.BLOCKS, volume,
                0.85f + level.random.nextFloat() * 0.3f, false);
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
            RandomSource random = level.random;
            if (random.nextInt(6) == 0) {
                level.addParticle(ACID_MIST, pos.x + (random.nextFloat() - .5f) * .5f, pos.y + .4f,
                        pos.z + (random.nextFloat() - .5f) * .5f, 0, 1 / 16f, 0);
            }
            if (random.nextInt(14) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.x + (random.nextFloat() - .5f) * .3f, pos.y + .5f,
                        pos.z + (random.nextFloat() - .5f) * .3f, 0, 1 / 32f, 0);
            }
            if (random.nextInt(40) == 0) {
                playNear(level, pos, AllNuclearSounds.ACID_SIZZLE, 0.35f);
            }
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(random.nextBoolean() ? 0xE3D46A : 0xC9B84A);
            particleAccess.setAlpha(1f);
            if (random.nextFloat() < 1 / 64f) {
                particleAccess.spawnExtraParticle(ACID_MIST, .125f);
            }
        }

        @Override
        public void affectEntity(Entity entity, Level level) {
            if (level.isClientSide || !(entity instanceof LivingEntity living) || FuelCycleConfig.acidDamagePerSecond <= 0) {
                return;
            }
            if (living.tickCount % ACID_INTERVAL == 0) {
                living.hurt(level.damageSources().source(ACID_DAMAGE), (float) FuelCycleConfig.acidDamagePerSecond);
            }
        }
    }

    /** Fan processing past a strongly radioactive block that lets the air through. */
    public static class IrradiationType implements FanProcessingType {
        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            return isIrradiationCatalyst(level.getBlockState(pos));
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
            RandomSource random = level.random;
            if (random.nextInt(6) == 0) {
                level.addParticle(RADIATION_GLOW, pos.x + (random.nextFloat() - .5f) * .5f, pos.y + .4f,
                        pos.z + (random.nextFloat() - .5f) * .5f, 0, 1 / 16f, 0);
            }
            if (random.nextInt(10) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x + (random.nextFloat() - .5f) * .4f, pos.y + .3f,
                        pos.z + (random.nextFloat() - .5f) * .4f, 0, 0, 0);
            }
            if (random.nextInt(40) == 0) {
                playNear(level, pos, AllNuclearSounds.IRRADIATION_CRACKLE, 0.35f);
            }
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(random.nextBoolean() ? 0x7CFF4A : 0x4FD62E);
            particleAccess.setAlpha(1f);
            if (random.nextFloat() < 1 / 48f) {
                particleAccess.spawnExtraParticle(ParticleTypes.ELECTRIC_SPARK, .125f);
            }
        }

        @Override
        public void affectEntity(Entity entity, Level level) {
            if (!level.isClientSide && entity instanceof LivingEntity living) {
                irradiate(living);
            }
        }
    }
}
