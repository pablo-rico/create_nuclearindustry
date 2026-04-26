package org.papiricoh.create_nuclearindustry.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.physics.ReactorPhysicsSimulator;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity for the nuclear reactor controller block.
 * Manages the multiblock structure, physics simulation, and state synchronization.
 */
public class ReactorBlockEntity extends BlockEntity {

    // Multiblock structure state
    private boolean isFormed;
    private Optional<ReactorStructureValidator.ReactorStructure> currentStructure;

    // Physics simulator
    private ReactorPhysicsSimulator physicsSimulator;

    // Update counter for client sync (every 20 ticks)
    private int syncCounter;

    // Flag to request structure revalidation
    private boolean pendingRevalidation;

    // Player who placed the reactor (for chat messages)
    private Optional<UUID> ownerUUID;
    private Player ownerPlayer;

    // Last error message to display to player
    private String lastError;

    // Force sync to client on next update
    private boolean forceSync = true;

    // Multi-tick sync counter to ensure client receives updates
    private int forceSyncCounter = 0;
    private static final int MULTI_SYNC_TICKS = 5;

    // Constructor
    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR.get(), pos, state);
        this.isFormed = false;
        this.currentStructure = Optional.empty();
        this.physicsSimulator = new ReactorPhysicsSimulator(0);
        this.syncCounter = 0;
        this.pendingRevalidation = true; // Initial validation when block is placed
        this.ownerUUID = Optional.empty();
        this.lastError = "";
        System.out.println("[ReactorBlockEntity] Created at position: " + pos);
    }

    // ============= TICK =============

    public static void tick(Level level, BlockPos pos, BlockState state, ReactorBlockEntity entity) {
        if (level == null || level.isClientSide) {
            return;
        }

        // Only validate structure if requested (block change detected or initial placement)
        if (entity.pendingRevalidation) {
            System.out.println("[ReactorBlockEntity] Validating structure at " + pos);
            entity.validateStructure();
            entity.pendingRevalidation = false;
            entity.forceSync = true; // Force sync after validation
        }

        // Run physics simulation if reactor is formed
        if (entity.isFormed) {
            boolean stillRunning = entity.physicsSimulator.tick();
            if (!stillRunning) {
                // Meltdown occurred
                entity.handleMeltdown();
            }
            entity.syncCounter++;

            // Sync to clients every 20 ticks
            if (entity.syncCounter >= 20) {
                entity.setChanged();
                entity.syncCounter = 0;
            }
        }

        // Force sync if needed (multi-tick to ensure it reaches client)
        if (entity.forceSync) {
            entity.forceSyncCounter++;
            entity.setChanged();
            if (entity.forceSyncCounter >= MULTI_SYNC_TICKS) {
                entity.forceSync = false;
                entity.forceSyncCounter = 0;
            }
        }
    }

    // ============= STRUCTURE VALIDATION =============

    /**
     * Validates the multiblock structure and updates internal state.
     */
    private void validateStructure() {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        // Create error handler to capture error messages
        ReactorStructureValidator.ErrorHandler errorHandler = new ReactorStructureValidator.ErrorHandler();
        Optional<ReactorStructureValidator.ReactorStructure> newStructure =
                ReactorStructureValidator.findReactorStructure(level, getBlockPos(), errorHandler);

        // Store error message for chat
        if (errorHandler.hasError()) {
            this.lastError = errorHandler.getError();
        }

        // Structure changed
        if (newStructure.isPresent() && !isFormed) {
            // Reactor just formed
            ReactorStructureValidator.ReactorStructure structure = newStructure.get();
            isFormed = true;
            currentStructure = newStructure;

            // Create physics simulator with uranium rod count
            physicsSimulator = new ReactorPhysicsSimulator(structure.getUraniumRodCount());

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  ✓ REACTOR STRUCTURE FORMED             ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ Dimensions: " + structure.width + "×" + structure.height);
            System.out.println("║ Uranium Rods: " + structure.getUraniumRodCount());
            System.out.println("║ Control Rods: " + structure.getControlRodCount());
            System.out.println("║ Location: " + structure.controllerPos);
            System.out.println("║ Setting isFormed=true and calling setChanged()");
            System.out.println("╚════════════════════════════════════════╝\n");

            // Send chat message to player
            sendPlayerMessage("§2✓ Reactor Structure Valid! §8[" + structure.width + "×" + structure.height + "]");
            setChanged();
            forceSync = true; // Force immediate sync to ensure client receives update
        } else if (newStructure.isPresent() && isFormed) {
            System.out.println("[ReactorBlockEntity] Structure already formed, no change");
        } else if (newStructure.isEmpty() && isFormed) {
            // Reactor was broken
            System.out.println("[ReactorBlockEntity] Structure broken");
            isFormed = false;
            currentStructure = Optional.empty();
            physicsSimulator = new ReactorPhysicsSimulator(0);

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  ✗ REACTOR STRUCTURE BROKEN             ║");
            System.out.println("║  Reactor will no longer function        ║");
            System.out.println("╚════════════════════════════════════════╝\n");

            // Send chat message to player
            sendPlayerMessage("§c✗ Reactor Structure Invalid");
            setChanged();
        } else if (newStructure.isEmpty() && !isFormed) {
            // Invalid structure on initial placement
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  ✗ REACTOR STRUCTURE INVALID            ║");
            System.out.println("║  Error: " + lastError);
            System.out.println("╚════════════════════════════════════════╝\n");

            // Send chat message to player
            sendPlayerMessage("§c✗ " + lastError);
        }
    }

    /**
     * Counts the number of blocks of a specific type in the reactor structure.
     */
    private int countBlocksInStructure(Level level, ReactorStructureValidator.ReactorStructure structure, net.minecraft.world.level.block.Block blockType) {
        int count = 0;
        for (BlockPos pos : structure.blocks) {
            if (level.getBlockState(pos).getBlock() == blockType) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sends a chat message to the owner player if they're online.
     */
    private void sendPlayerMessage(String message) {
        if (getLevel() == null) {
            return;
        }

        // Try to send to owner player
        if (ownerPlayer != null && ownerPlayer.isAlive()) {
            ownerPlayer.displayClientMessage(Component.literal(message), false);
            return;
        }

        // Try to find player by UUID
        if (ownerUUID.isPresent()) {
            Player player = getLevel().getPlayerByUUID(ownerUUID.get());
            if (player != null && player.isAlive()) {
                player.displayClientMessage(Component.literal(message), false);
                this.ownerPlayer = player;
                return;
            }
        }

        // If no player found, just log it
        System.out.println("[Reactor Chat Message] " + message);
    }

    /**
     * Handles meltdown event - destroys reactor and creates explosion.
     */
    private void handleMeltdown() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        System.out.println("[Reactor] MELTDOWN AT " + getBlockPos());

        if (currentStructure.isPresent()) {
            ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
            Set<BlockPos> blocks = structure.blocks;

            // Destroy reactor blocks
            for (BlockPos pos : blocks) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() != AllNuclearBlocks.REACTOR_CONTROLLER.get()) {
                    level.destroyBlock(pos, true); // Drop items
                }
            }

            // Destroy reactor blocks in explosion
            for (BlockPos destroyPos : blocks) {
                if (!destroyPos.equals(getBlockPos())) {
                    level.destroyBlock(destroyPos, false);
                }
            }
        }

        // Disable reactor
        isFormed = false;
        currentStructure = Optional.empty();
        physicsSimulator = new ReactorPhysicsSimulator(0);
        setChanged();
    }

    // ============= STRUCTURE REVALIDATION =============

    /**
     * Requests the structure to be revalidated on the next tick.
     * Called when a nearby block changes.
     */
    public void requestStructureRevalidation(Player player) {
        this.pendingRevalidation = true;
        if (player != null) {
            this.ownerPlayer = player;
            this.ownerUUID = Optional.of(player.getUUID());
        }
    }

    // ============= CONTROL ROD INTERACTION =============

    /**
     * Sets the control rod position manually (for GUI or contraption control).
     * Position 0 = fully inserted, 1 = fully withdrawn.
     */
    public void setControlRodPosition(float position) {
        if (isFormed) {
            physicsSimulator.setControlRodPosition(position);
            setChanged();
        }
    }

    /**
     * Moves the control rod by a given delta (used by Create contraptions).
     * Positive delta = withdraw (increase position), negative = insert (decrease position).
     */
    public void moveControlRod(float delta) {
        if (isFormed) {
            physicsSimulator.moveControlRod(delta);
            setChanged();
        }
    }

    // ============= STATE GETTERS =============

    public boolean isFormed() {
        return isFormed;
    }

    public double getCoreTemperature() {
        return isFormed ? physicsSimulator.getCoreTemperature() : 20.0;
    }

    public double getNeutronLevel() {
        return isFormed ? physicsSimulator.getNeutronLevel() : 0.0;
    }

    public double getFuelRemaining() {
        return isFormed ? physicsSimulator.getFuelRemaining() : 0.0;
    }

    public float getControlRodPosition() {
        return isFormed ? physicsSimulator.getControlRodPosition() : 1.0f;
    }

    public double getPowerOutput() {
        return isFormed ? physicsSimulator.getPowerOutput() : 0.0;
    }

    public double getSteamGenerationRate() {
        return isFormed ? physicsSimulator.getSteamGenerationRate() : 0.0;
    }

    public ReactorPhysicsSimulator.ReactorState getReactorState() {
        return isFormed ? physicsSimulator.getState() : ReactorPhysicsSimulator.ReactorState.IDLE;
    }

    public Optional<ReactorStructureValidator.ReactorStructure> getStructure() {
        return currentStructure;
    }

    // ============= NBT SERIALIZATION =============

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putBoolean("isFormed", isFormed);

        if (currentStructure.isPresent()) {
            ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
            CompoundTag structureTag = new CompoundTag();
            structureTag.putInt("x", structure.controllerPos.getX());
            structureTag.putInt("y", structure.controllerPos.getY());
            structureTag.putInt("z", structure.controllerPos.getZ());
            structureTag.putInt("width", structure.width);
            structureTag.putInt("height", structure.height);
            tag.put("structure", structureTag);
        }

        if (physicsSimulator != null) {
            CompoundTag physicsTag = new CompoundTag();
            physicsSimulator.serializeNBT(physicsTag);
            tag.put("physics", physicsTag);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        this.isFormed = tag.getBoolean("isFormed");

        if (tag.contains("physics")) {
            CompoundTag physicsTag = tag.getCompound("physics");
            physicsSimulator.deserializeNBT(physicsTag);
        }

        // Structure will be re-validated on next tick
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putBoolean("isFormed", isFormed);

        if (physicsSimulator != null) {
            CompoundTag physicsTag = new CompoundTag();
            physicsSimulator.serializeNBT(physicsTag);
            tag.put("physics", physicsTag);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        this.isFormed = tag.getBoolean("isFormed");

        if (tag.contains("physics")) {
            CompoundTag physicsTag = tag.getCompound("physics");
            physicsSimulator.deserializeNBT(physicsTag);
        }
    }
}
