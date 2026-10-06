package org.papiricoh.create_nuclearindustry.fusion.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator;

/** Tokamak control panel: read-only telemetry plus the fuel injection switch. */
public class TokamakMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_FUEL = 0;

    static final int STATUS = 0;
    static final int RADIUS = 1;
    static final int TEMPERATURE = 2;
    static final int CONFINEMENT_PERMILLE = 3;
    static final int COVERAGE_PERMILLE = 4;
    static final int DRIVE_PERMILLE = 5;
    static final int DEUTERIUM = 6;
    static final int TRITIUM = 7;
    static final int COOLANT = 8;
    static final int STEAM = 9;
    static final int POWER_MW = 10;
    static final int TBR_X100 = 11;
    static final int BURN_X10000 = 12;
    static final int INSTABILITY = 13;
    static final int COILS = 14;
    static final int BLANKETS = 15;
    static final int FUEL_ENABLED = 16;
    static final int MODE = 17;
    static final int COUNT = 18;

    @Nullable
    private final FusionReactorBlockEntity tokamak;
    private final BlockPos pos;
    private final ContainerData data;

    public TokamakMenu(int containerId, Inventory inventory, BlockPos pos, int[] initial) {
        this(containerId, inventory, null, pos, initial);
    }

    public TokamakMenu(int containerId, Inventory inventory, FusionReactorBlockEntity tokamak) {
        this(containerId, inventory, tokamak, tokamak.getBlockPos(), snapshot(tokamak));
    }

    private TokamakMenu(int containerId, Inventory inventory, @Nullable FusionReactorBlockEntity tokamak, BlockPos pos, int[] initial) {
        super(AllNuclearGUIs.TOKAMAK_MENU.get(), containerId);
        this.tokamak = tokamak;
        this.pos = pos;
        if (tokamak != null && !inventory.player.level().isClientSide) {
            this.data = new LiveData(tokamak);
        } else {
            SimpleContainerData client = new SimpleContainerData(COUNT);
            for (int i = 0; i < COUNT && i < initial.length; i++) {
                client.set(i, initial[i]);
            }
            this.data = client;
        }
        addDataSlots(data);
    }

    public int get(int index) {
        return data.get(index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE_FUEL || tokamak == null) {
            return false;
        }
        tokamak.requestStructureRevalidation(player);
        tokamak.setFuelEnabled(!tokamak.isFuelEnabled());
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.blockPosition().closerThan(pos, 10.0);
    }

    public static void writeInitialData(RegistryFriendlyByteBuf buf, FusionReactorBlockEntity tokamak) {
        buf.writeBlockPos(tokamak.getBlockPos());
        for (int value : snapshot(tokamak)) {
            buf.writeVarInt(value);
        }
    }

    public static TokamakMenu read(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int[] initial = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            initial[i] = buf.readVarInt();
        }
        return new TokamakMenu(containerId, inventory, pos, initial);
    }

    static int[] snapshot(FusionReactorBlockEntity tokamak) {
        TokamakPhysicsSimulator physics = tokamak.getPhysics();
        int[] values = new int[COUNT];
        values[STATUS] = tokamak.getStatus().ordinal();
        values[RADIUS] = tokamak.getMajorRadius();
        values[TEMPERATURE] = (int) Math.round(physics.getPlasmaTemperature());
        values[CONFINEMENT_PERMILLE] = (int) Math.round(physics.getConfinement() * 1000);
        values[COVERAGE_PERMILLE] = (int) Math.round(tokamak.getCoilCoverage() * 1000);
        values[DRIVE_PERMILLE] = (int) Math.round(tokamak.getMagnetDrive() * 1000);
        values[DEUTERIUM] = tokamak.getDeuteriumTank().getFluidAmount();
        values[TRITIUM] = tokamak.getTritiumTank().getFluidAmount();
        values[COOLANT] = tokamak.getCoolantTank().getFluidAmount();
        values[STEAM] = tokamak.getPlasmaSteamTank().getFluidAmount();
        values[POWER_MW] = (int) Math.round(physics.getFusionPower());
        values[TBR_X100] = (int) Math.round(physics.getTritiumBreedingRatio() * 100);
        values[BURN_X10000] = (int) Math.round(physics.getBurnRate() * 10000);
        values[INSTABILITY] = (int) Math.round(physics.getInstability());
        values[COILS] = tokamak.getCoilCount();
        values[BLANKETS] = tokamak.getBlanketCount();
        values[FUEL_ENABLED] = tokamak.isFuelEnabled() ? 1 : 0;
        values[MODE] = physics.getMode().ordinal();
        return values;
    }

    /** Server-side slots, rebuilt once per game tick. */
    private static final class LiveData implements ContainerData {
        private final FusionReactorBlockEntity tokamak;
        private int[] cached = new int[COUNT];
        private long cachedAt = Long.MIN_VALUE;

        private LiveData(FusionReactorBlockEntity tokamak) {
            this.tokamak = tokamak;
        }

        @Override
        public int get(int index) {
            long now = tokamak.getLevel() == null ? 0 : tokamak.getLevel().getGameTime();
            if (now != cachedAt) {
                cached = snapshot(tokamak);
                cachedAt = now;
            }
            return index >= 0 && index < COUNT ? cached[index] : 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return COUNT;
        }
    }
}
