package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorManager;

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
                entity.markDirtyAndSync();
                entity.syncCounter = 0;
            }
        }

        // Force sync if needed (multi-tick to ensure it reaches client)
        if (entity.forceSync) {
            entity.forceSyncCounter++;
            entity.markDirtyAndSync();
            if (entity.forceSyncCounter >= MULTI_SYNC_TICKS) {
                entity.forceSync = false;
                entity.forceSyncCounter = 0;
            }
        }
    }

    /**
     * Called when the block entity is loaded into the world.
     * Registers this reactor in the global manager for efficient lookups.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            ReactorManager.registerReactor(level, getBlockPos(), this);
        }
    }

    /**
     * Called when the block entity is removed from the world.
     * Unregisters this reactor from the global manager.
     */
    @Override
    public void setRemoved() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            ReactorManager.unregisterReactor(level, getBlockPos());
        }
        super.setRemoved();
    }

    /**
     * Marks this block entity dirty and pushes an update packet to nearby clients.
     */
    private void markDirtyAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
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

            // Create physics simulator with uranium and control rod counts
            physicsSimulator = new ReactorPhysicsSimulator(structure.getUraniumRodCount(), structure.getControlRodCount());

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
            markDirtyAndSync();
            forceSync = true; // Force immediate sync to ensure client receives update
        } else if (newStructure.isPresent() && isFormed && currentStructure.isPresent()) {
            // Structure still valid - check if rod counts changed
            ReactorStructureValidator.ReactorStructure newStruct = newStructure.get();
            ReactorStructureValidator.ReactorStructure oldStruct = currentStructure.get();

            if (newStruct.getUraniumRodCount() != oldStruct.getUraniumRodCount() ||
                newStruct.getControlRodCount() != oldStruct.getControlRodCount()) {
                // Rod counts changed - update structure and simulator
                System.out.println("[ReactorBlockEntity] Rod counts changed: U " + oldStruct.getUraniumRodCount() + "->" + newStruct.getUraniumRodCount() +
                                   ", C " + oldStruct.getControlRodCount() + "->" + newStruct.getControlRodCount());
                currentStructure = newStructure;
                physicsSimulator = new ReactorPhysicsSimulator(newStruct.getUraniumRodCount(), newStruct.getControlRodCount());
                markDirtyAndSync();
                forceSync = true;
            } else {
                System.out.println("[ReactorBlockEntity] Structure already formed, no rod changes");
            }
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
            markDirtyAndSync();
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
     * ============ SUPER EXPLOSION ON MELTDOWN - NUCLEAR CATASTROPHE ============
     * Handles meltdown event - creates catastrophic nuclear explosion and destroys reactor.
     */
    private void handleMeltdown() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos reactorPos = getBlockPos();
        System.out.println("\n" +
            "╔════════════════════════════════════════╗\n" +
            "║  ☢️  NUCLEAR MELTDOWN DETECTED  ☢️     ║\n" +
            "║  " + reactorPos + "\n" +
            "║  INITIATING CATASTROPHIC FAILURE      ║\n" +
            "╚════════════════════════════════════════╝\n");

        // ☢️ NUCLEAR EXPLOSION - MASSIVE DESTRUCTIVE FORCE ☢️

        // Destroy ALL reactor blocks
        if (currentStructure.isPresent()) {
            ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
            Set<BlockPos> blocks = structure.blocks;

            for (BlockPos pos : blocks) {
                level.destroyBlock(pos, true);
            }
        }

        // Create massive chain explosion radius
        int explosionRadius = 20; // Huge explosion radius
        int centerX = reactorPos.getX();
        int centerY = reactorPos.getY();
        int centerZ = reactorPos.getZ();

        // Destroy everything in a large sphere around the reactor
        for (int x = centerX - explosionRadius; x <= centerX + explosionRadius; x++) {
            for (int y = centerY - explosionRadius; y <= centerY + explosionRadius; y++) {
                for (int z = centerZ - explosionRadius; z <= centerZ + explosionRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    double distance = pos.distToCenterSqr(centerX + 0.5, centerY + 0.5, centerZ + 0.5);
                    double maxDistSq = explosionRadius * explosionRadius;

                    // Spherical explosion pattern with decreasing probability
                    if (distance <= maxDistSq) {
                        double probability = 1.0 - (distance / maxDistSq) * 0.7; // Stronger at center
                        if (Math.random() < probability) {
                            BlockState blockState = level.getBlockState(pos);
                            // Destroy all non-air blocks
                            if (!blockState.isAir()) {
                                level.destroyBlock(pos, Math.random() < 0.5); // Drop items randomly
                            }
                        }
                    }
                }
            }
        }

        // Disable reactor
        isFormed = false;
        currentStructure = Optional.empty();
        physicsSimulator = new ReactorPhysicsSimulator(0);
        markDirtyAndSync();

        sendPlayerMessage("§4☢️ NUCLEAR MELTDOWN! ☢️§r Reactor destroyed at " + getBlockPos());
    }
    // ============ END SUPER EXPLOSION CODE ============

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
            markDirtyAndSync();
        }
    }

    /**
     * Moves the control rod by a given delta (used by Create contraptions).
     * Positive delta = withdraw (increase position), negative = insert (decrease position).
     */
    public void moveControlRod(float delta) {
        if (isFormed) {
            physicsSimulator.moveControlRod(delta);
            markDirtyAndSync();
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

    public ReactorPhysicsSimulator getPhysicsSimulator() {
        return physicsSimulator;
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
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
