package org.papiricoh.create_nuclearindustry.missile.terrain.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.terrain.ClientTerrainHook;
import org.papiricoh.create_nuclearindustry.missile.terrain.HoloShade;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainRequestPayload;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTilePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The terrain tiles this client has been sent for the radar screens, scopes and the command table, and their textures, painted by
 * {@link HoloShade} a couple a frame with the oldest let go. Forgotten on a change of level, so tiles of one dimension never show
 * in another. Ported from SupernaturalCraft's war table.
 */
public final class ClientTerrain {
    /** Pixels per sample of a tile's texture. */
    public static final int TEXEL = 2;
    public static final int TEXTURE = TerrainTile.SIZE * TEXEL;
    private static final int TILES = 256, TEXTURES = 96, PAINTS_PER_FRAME = 2;
    /** Ticks before a missing tile is asked for again, and before a known one is refreshed (to show newly loaded land). */
    private static final long RETRY = 200, REFRESH = 1200;

    private static final Map<Long, byte[]> tiles = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
            return size() > TILES;
        }
    };
    private static final Map<Long, ResourceLocation> textures = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<Long, Long> asked = new HashMap<>();
    @Nullable
    private static ClientLevel level;
    private static long paintedFrame;
    private static int paintedThisFrame;
    private static boolean initialised;

    private ClientTerrain() {
    }

    public static void init() {
        if (initialised) return;
        initialised = true;
        ClientTerrainHook.install(ClientTerrain::handle);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> reset());
    }

    private static void checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            reset();
            level = current;
        }
    }

    static void handle(TerrainTilePayload p) {
        checkLevel();
        if (p.data().length != TerrainTile.BYTES) return;
        tiles.put(p.key(), p.data());
        ResourceLocation old = textures.remove(p.key());
        if (old != null) Minecraft.getInstance().getTextureManager().release(old);
    }

    @Nullable
    public static byte[] tile(long key) {
        checkLevel();
        return tiles.get(key);
    }

    /** Asks the server, on behalf of the viewer at {@code centre}, for those of {@code keys} not known (or not refreshed in a while). */
    public static void want(BlockPos centre, long[] keys) {
        checkLevel();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null) return;
        long now = mc.level.getGameTime();
        if (asked.size() > 4096) asked.values().removeIf(at -> now - at > REFRESH * 2 || at > now);
        List<Long> ask = new ArrayList<>();
        for (long key : keys) {
            if (!TerrainTile.inRange(key, centre.getX(), centre.getZ(), TerrainTile.RANGE)) continue;
            Long at = asked.get(key);
            long wait = tiles.containsKey(key) ? REFRESH : RETRY;
            if (at != null && now - at < wait && now >= at) continue;
            asked.put(key, now);
            ask.add(key);
            if (ask.size() == TerrainRequestPayload.MAX) {
                PacketDistributor.sendToServer(new TerrainRequestPayload(centre, List.copyOf(ask)));
                ask.clear();
            }
        }
        if (!ask.isEmpty()) PacketDistributor.sendToServer(new TerrainRequestPayload(centre, ask));
    }

    /** The texture of a tile, painting it now if there is time this frame; null if unknown or not painted yet. */
    @Nullable
    public static ResourceLocation texture(long key) {
        checkLevel();
        ResourceLocation id = textures.get(key);
        if (id != null) return id;
        byte[] data = tiles.get(key);
        Minecraft mc = Minecraft.getInstance();
        if (data == null || mc.level == null) return null;
        long now = Util.getNanos();
        if (now - paintedFrame > 16_000_000L) {
            paintedFrame = now;
            paintedThisFrame = 0;
        }
        if (paintedThisFrame >= PAINTS_PER_FRAME) return null;
        paintedThisFrame++;
        int[] argb = HoloShade.paint(data, TerrainTile.SPACING[TerrainTile.lod(key)], mc.level.getSeaLevel(), TEXEL);
        NativeImage image = new NativeImage(TEXTURE, TEXTURE, true);
        for (int y = 0; y < TEXTURE; y++) {
            for (int x = 0; x < TEXTURE; x++) {
                int c = argb[y * TEXTURE + x];
                // NativeImage keeps ABGR.
                image.setPixelRGBA(x, y, (c & 0xFF00FF00) | (c >> 16 & 0xFF) | (c & 0xFF) << 16);
            }
        }
        id = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "dynamic/terrain/" + Long.toHexString(key));
        mc.getTextureManager().register(id, new DynamicTexture(image));
        textures.put(key, id);
        while (textures.size() > TEXTURES) {
            Long oldest = textures.keySet().iterator().next();
            mc.getTextureManager().release(textures.remove(oldest));
        }
        return id;
    }

    /** Back to nothing (on leaving a world or changing level). */
    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation id : textures.values()) mc.getTextureManager().release(id);
        textures.clear();
        tiles.clear();
        asked.clear();
    }
}
