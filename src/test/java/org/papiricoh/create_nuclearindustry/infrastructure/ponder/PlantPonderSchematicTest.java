package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The schematics of the plant series, as tools/structgen/ponder_plant_*.py writes them: a size
 * Ponder can frame, every block inside it, one block per position, only blocks that exist (each
 * of this mod's blocks has a blockstate file), and the GameTest copy identical to the Ponder one.
 */
class PlantPonderSchematicTest {
    private static final int MAX_WIDTH = 17, MAX_HEIGHT = 14;
    /** The overview holds the whole plant and zooms in on its parts, so it may be wider. */
    private static final int OVERVIEW_MAX_WIDTH = 26;
    private static final Set<String> NAMESPACES = Set.of("minecraft", "create", "create_nuclearindustry");

    @Test
    void schematicsAreValidAndInSync() throws IOException {
        for (PlantPonderText.Scene scene : PlantPonderText.SCENES) {
            Path ponder = Path.of("src/main/resources/assets/create_nuclearindustry/ponder/" + scene.schematic() + ".nbt");
            Path gametest = Path.of("src/main/resources/data/create_nuclearindustry/structure/gametest/" + scene.schematic() + "_ponder.nbt");
            assertTrue(Files.exists(ponder), "missing " + ponder);
            // The gzip headers carry the file names, so compare what is inside.
            assertArrayEquals(inflate(ponder), inflate(gametest), scene.schematic() + ": GameTest copy differs");
            check(scene.schematic(), read(ponder), scene == PlantPonderText.OVERVIEW ? OVERVIEW_MAX_WIDTH : MAX_WIDTH);
        }
    }

    @SuppressWarnings("unchecked")
    private static void check(String name, Map<String, Object> root, int maxWidth) {
        List<Integer> size = (List<Integer>) root.get("size");
        assertEquals(3, size.size(), name);
        assertTrue(size.get(0) <= maxWidth && size.get(2) <= maxWidth && size.get(1) <= MAX_HEIGHT, name + " is too big: " + size);
        List<Map<String, Object>> palette = (List<Map<String, Object>>) root.get("palette");
        for (Map<String, Object> state : palette) {
            String id = (String) state.get("Name");
            String[] parts = id.split(":");
            assertEquals(2, parts.length, id);
            assertTrue(NAMESPACES.contains(parts[0]), name + " uses a block of another mod: " + id);
            if (parts[0].equals("create_nuclearindustry")) {
                assertTrue(blockExists(parts[1]), name + " uses a block that does not exist: " + id);
            }
        }
        Set<List<Integer>> positions = new HashSet<>();
        for (Map<String, Object> block : (List<Map<String, Object>>) root.get("blocks")) {
            List<Integer> pos = (List<Integer>) block.get("pos");
            for (int axis = 0; axis < 3; axis++) {
                int value = pos.get(axis);
                assertTrue(value >= 0 && value < size.get(axis), name + ": " + pos + " lies outside " + size);
            }
            assertTrue(positions.add(pos), name + ": two blocks at " + pos);
            int state = (Integer) block.get("state");
            assertTrue(state >= 0 && state < palette.size(), name + ": bad palette index");
        }
    }

    private static boolean blockExists(String id) {
        String blockstate = "assets/create_nuclearindustry/blockstates/" + id + ".json";
        return Files.exists(Path.of("src/generated/resources", blockstate)) || Files.exists(Path.of("src/main/resources", blockstate));
    }

    // ------------------------------------------------------------------ a minimal NBT reader

    private static byte[] inflate(Path path) throws IOException {
        try (InputStream raw = Files.newInputStream(path); GZIPInputStream gzip = new GZIPInputStream(raw)) {
            return gzip.readAllBytes();
        }
    }

    private static Map<String, Object> read(Path path) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(inflate(path)));
        int type = in.readByte();
        assertEquals(10, type, "root must be a compound");
        in.readUTF();
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) payload(in, type);
        return root;
    }

    private static Object payload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case 1: return (int) in.readByte();
            case 2: return (int) in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: {
                byte[] bytes = new byte[in.readInt()];
                in.readFully(bytes);
                return bytes;
            }
            case 8: {
                byte[] bytes = new byte[in.readUnsignedShort()];
                in.readFully(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }
            case 9: {
                int elementType = in.readByte();
                int length = in.readInt();
                List<Object> list = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    list.add(payload(in, elementType));
                }
                return list;
            }
            case 10: {
                Map<String, Object> compound = new LinkedHashMap<>();
                for (int tag = in.readByte(); tag != 0; tag = in.readByte()) {
                    String key = in.readUTF();
                    compound.put(key, payload(in, tag));
                }
                return compound;
            }
            case 11: {
                int[] ints = new int[in.readInt()];
                for (int i = 0; i < ints.length; i++) {
                    ints[i] = in.readInt();
                }
                return ints;
            }
            case 12: {
                long[] longs = new long[in.readInt()];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = in.readLong();
                }
                return longs;
            }
            default:
                fail("unknown NBT tag " + type);
                return null;
        }
    }
}
