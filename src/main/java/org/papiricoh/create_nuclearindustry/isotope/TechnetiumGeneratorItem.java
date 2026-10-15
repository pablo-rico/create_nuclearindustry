package org.papiricoh.create_nuclearindustry.isotope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.isotope.physics.IsotopeDecay;
import org.papiricoh.create_nuclearindustry.isotope.physics.TechnetiumCow;

import java.util.List;

/**
 * A technetium "cow": a shielded column of molybdenum-99 made at a Target Station. Its Mo-99 decays
 * (66 h) into technetium-99m, which builds up in the column; using it milks the technetium off as
 * doses for the Medical Isotope Kit. Milked once a day it gives the most; it is worn out in a
 * little over a week. The column's state is kept lazily, like every isotope clock.
 */
public class TechnetiumGeneratorItem extends Item {
    private static final String KEY = "item.create_nuclearindustry.technetium_generator.";

    /** The column at {@code refTick}: parent Mo-99 and daughter Tc-99m activities, relative. */
    public record CowData(long refTick, float parent, float daughter) {
        public static final long UNDATED = Long.MIN_VALUE;
        public static final CowData FRESH = new CowData(UNDATED, 1.0f, 0.0f);
        public static final Codec<CowData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("ref").forGetter(CowData::refTick),
                Codec.FLOAT.fieldOf("parent").forGetter(CowData::parent),
                Codec.FLOAT.fieldOf("daughter").forGetter(CowData::daughter)
        ).apply(instance, CowData::new));
        public static final StreamCodec<ByteBuf, CowData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, CowData::refTick,
                ByteBufCodecs.FLOAT, CowData::parent,
                ByteBufCodecs.FLOAT, CowData::daughter,
                CowData::new);

        public boolean dated() {
            return refTick != UNDATED;
        }
    }

    public TechnetiumGeneratorItem(Properties properties) {
        super(properties.component(IsotopeRegistry.COW.get(), CowData.FRESH));
    }

    public static CowData data(ItemStack stack) {
        CowData data = stack.get(IsotopeRegistry.COW.get());
        return data == null ? CowData.FRESH : data;
    }

    /** The column's activities at {@code gameTime}; an undated column is fresh. */
    public static TechnetiumCow.State state(ItemStack stack, long gameTime) {
        CowData data = data(stack);
        TechnetiumCow.State at = new TechnetiumCow.State(data.parent(), data.daughter());
        if (!data.dated() || gameTime < 0) {
            return at;
        }
        double days = IsotopeDecay.scaledDays(data.refTick(), gameTime, IsotopeConfig.timeScale);
        return TechnetiumCow.advance(IsotopeConfig.cow(), at, days);
    }

    /** Dates an undated column at {@code gameTime}, so it starts decaying. Returns whether it changed. */
    public static boolean refresh(ItemStack stack, long gameTime) {
        CowData data = data(stack);
        if (data.dated()) {
            return false;
        }
        stack.set(IsotopeRegistry.COW.get(), new CowData(gameTime, data.parent(), data.daughter()));
        return true;
    }

    /**
     * Milks {@code stack} at {@code gameTime}: the column keeps what elution leaves, and the doses
     * of Tc-99m it gave come back, each fresh. Empty when there was too little to make one dose.
     */
    public static List<ItemStack> elute(ItemStack stack, long gameTime) {
        TechnetiumCow.Params p = IsotopeConfig.cow();
        TechnetiumCow.Elution elution = TechnetiumCow.elute(p, state(stack, gameTime));
        int doses = TechnetiumCow.kits(elution.eluted(), IsotopeConfig.cowKitActivity, IsotopeConfig.cowMaxKits);
        if (doses <= 0) {
            return List.of();
        }
        TechnetiumCow.State after = elution.after();
        stack.set(IsotopeRegistry.COW.get(), new CowData(gameTime, (float) after.parent(), (float) after.daughter()));
        return List.of(Isotopes.fresh(IsotopeRegistry.TECHNETIUM_99M.get(), doses, gameTime, 1.0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (TechnetiumCow.exhausted(IsotopeConfig.cow(), state(stack, level.getGameTime()))) {
            player.displayClientMessage(Component.translatable(KEY + "exhausted").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        ItemStack single = stack.getCount() > 1 ? stack.split(1) : stack;
        List<ItemStack> doses = elute(single, level.getGameTime());
        if (single != stack) {
            if (!player.getInventory().add(single)) {
                player.drop(single, false);
            }
        }
        if (doses.isEmpty()) {
            player.displayClientMessage(Component.translatable(KEY + "too_soon").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        for (ItemStack dose : doses) {
            player.displayClientMessage(Component.translatable(KEY + "eluted", dose.getCount()).withStyle(ChatFormatting.GREEN), true);
            if (!player.getInventory().add(dose)) {
                player.drop(dose, false);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.2f);
        NuclearAdvancements.award(player, NuclearEvent.TECHNETIUM_ELUTED);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TechnetiumCow.State state = state(stack, Isotopes.tooltipTime());
        TechnetiumCow.Params p = IsotopeConfig.cow();
        tooltip.add(Component.translatable(KEY + "parent", IsotopeDecay.percent(state.parent())).withStyle(ChatFormatting.GRAY));
        int doses = TechnetiumCow.kits(p.elutionEfficiency() * state.daughter(), IsotopeConfig.cowKitActivity, IsotopeConfig.cowMaxKits);
        tooltip.add(Component.translatable(KEY + "daughter", doses).withStyle(doses > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        if (TechnetiumCow.exhausted(p, state)) {
            tooltip.add(Component.translatable(KEY + "exhausted").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable(KEY + "hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
