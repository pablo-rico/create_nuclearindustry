package org.papiricoh.create_nuclearindustry.fuelcycle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every renewable route to a base material costs between one and two times the ore route per unit
 * of product: rock, sand, brine and sea water never run out, so they must not be cheaper than
 * mining, nor so dear that nobody uses them. Read from the recipe files {@code runData} generates,
 * so retuning a chance without checking the balance fails here.
 *
 * <p>The cost model, in arbitrary work units:
 * <ul>
 *   <li>Raw inputs: an ore block costs 8 (it has to be found and dug out), a block of common rock,
 *       sand or gravel 1 (a drill quarries it in bulk), gunpowder 2, string and bone meal 0.5 each;
 *       water is free. A tag costs what its members do.</li>
 *   <li>Machine time: crushing and centrifuging cost their processing time / 100; a fan treatment
 *       (washing, acid leaching, irradiation) 0.5 per item; mixing 1 per batch, 1.5 heated, 3
 *       superheated; compacting 0.5; emptying 0.25. A fan treatment's catalyst (water, acid) is not
 *       used up and costs nothing per item.</li>
 *   <li>A recipe's product costs (its ingredients + its machine time - credits) / its expected yield,
 *       the sum of count times chance over the results that are that product. Fluids are priced per
 *       millibucket the same way. Every other result is a free by-product, on both routes alike; the
 *       only credit is an input a step hands back (the adsorbent), at what it cost to make.</li>
 * </ul>
 * Each route is costed from scratch. Primary routes must land in [1, 2] times the ore route; a
 * by-product trace (uranium from heavy mineral sand) must only never be cheaper than ore.
 */
class RenewableCostTest {
    private static final Path DATA = Path.of("src/generated/resources/data");
    private static final double MIN_RATIO = 1.0;
    private static final double MAX_RATIO = 2.0;

    private static final String U_CONCENTRATE = "create_nuclearindustry:uranium_concentrate";
    private static final String SPODUMENE_CONCENTRATE = "create_nuclearindustry:spodumene_concentrate";

    @Test
    void uraniumFromGraniteDeepslateAndSeaWater() {
        double ore = cost(U_CONCENTRATE, RenewableCostTest::oreUranium);
        within("uranium concentrate from granite", ore, cost(U_CONCENTRATE, c -> {
            c.step("create_nuclearindustry:crushing/granite", "create_nuclearindustry:granite_fines");
            c.step("create_nuclearindustry:acid_leaching/granite_fines", U_CONCENTRATE);
        }));
        for (String rock : new String[]{"deepslate", "cobbled_deepslate"}) {
            within("uranium concentrate from " + rock, ore, cost(U_CONCENTRATE, c -> {
                c.step("create_nuclearindustry:crushing/" + rock, "create_nuclearindustry:deepslate_fines");
                c.step("create_nuclearindustry:acid_leaching/deepslate_fines", U_CONCENTRATE);
            }));
        }
        within("uranium concentrate from sea water", ore, cost(U_CONCENTRATE, c -> {
            c.step("create_nuclearindustry:mixing/amidoxime_adsorbent", "create_nuclearindustry:amidoxime_adsorbent");
            c.step("create_nuclearindustry:mixing/loaded_adsorbent", "create_nuclearindustry:loaded_adsorbent");
            c.step("create_nuclearindustry:acid_leaching/loaded_adsorbent", U_CONCENTRATE, "create_nuclearindustry:amidoxime_adsorbent");
        }));
        within("uranium concentrate from brine", ore, cost(U_CONCENTRATE, c -> {
            c.fluidStep("create_nuclearindustry:mixing/brine", "create_nuclearindustry:brine");
            c.step("create_nuclearindustry:mixing/amidoxime_adsorbent", "create_nuclearindustry:amidoxime_adsorbent");
            c.step("create_nuclearindustry:mixing/loaded_adsorbent_from_brine", "create_nuclearindustry:loaded_adsorbent");
            c.step("create_nuclearindustry:acid_leaching/loaded_adsorbent", U_CONCENTRATE, "create_nuclearindustry:amidoxime_adsorbent");
        }));
    }

    @Test
    void uraniumAsATraceIsNeverCheaperThanOre() {
        double ore = cost(U_CONCENTRATE, RenewableCostTest::oreUranium);
        for (String sand : new String[]{"sand", "red_sand", "gravel"}) {
            atLeast("uranium concentrate from washed " + sand, ore, cost(U_CONCENTRATE, c -> {
                c.step("create:splashing/" + sand, "create_nuclearindustry:heavy_mineral_sand");
                c.step("create_nuclearindustry:acid_leaching/heavy_mineral_sand", U_CONCENTRATE);
            }));
        }
    }

    @Test
    void fluoriteFromDeepslate() {
        String fluorite = "create_nuclearindustry:fluorite";
        double ore = cost(fluorite, c -> c.step("create_nuclearindustry:crushing/uranium_ore", fluorite));
        within("fluorite from deepslate", ore, cost(fluorite, c -> {
            c.step("create_nuclearindustry:crushing/deepslate", "create_nuclearindustry:deepslate_fines");
            c.step("create_nuclearindustry:acid_leaching/deepslate_fines", fluorite);
        }));
    }

    @Test
    void spodumeneFromTuff() {
        double ore = cost(SPODUMENE_CONCENTRATE, RenewableCostTest::oreSpodumene);
        for (String recipe : new String[]{"create:crushing/tuff", "create:crushing/tuff_recycling"}) {
            within("spodumene concentrate from " + recipe, ore, cost(SPODUMENE_CONCENTRATE, c -> {
                c.step(recipe, "create_nuclearindustry:tuff_fines");
                c.step("create_nuclearindustry:acid_leaching/tuff_fines", SPODUMENE_CONCENTRATE);
            }));
        }
    }

    @Test
    void berylliumFromHeavyMineralSand() {
        String beryllium = "create_nuclearindustry:beryllium";
        double ore = cost(beryllium, c -> {
            c.step("create_nuclearindustry:crushing/spodumene_ore", "create_nuclearindustry:crushed_spodumene");
            c.step("create_nuclearindustry:splashing/spodumene_concentrate", beryllium);
        });
        for (String sand : new String[]{"sand", "red_sand", "gravel"}) {
            within("beryllium from washed " + sand, ore, cost(beryllium, c -> {
                c.step("create:splashing/" + sand, "create_nuclearindustry:heavy_mineral_sand");
                c.step("create_nuclearindustry:acid_leaching/heavy_mineral_sand", beryllium);
            }));
        }
    }

    @Test
    void lithiumAndBoraxFromBrine() {
        String carbonate = "create_nuclearindustry:lithium_carbonate";
        double oreLithium = cost(carbonate, c -> {
            oreSpodumene(c);
            c.fluidStep("create_nuclearindustry:mixing/sulfuric_acid", "create_nuclearindustry:sulfuric_acid");
            c.step("create_nuclearindustry:mixing/lithium_carbonate", carbonate);
        });
        within("lithium carbonate from brine", oreLithium, cost(carbonate, c -> {
            c.fluidStep("create_nuclearindustry:mixing/brine", "create_nuclearindustry:brine");
            c.step("create_nuclearindustry:centrifuging/brine", carbonate);
        }));

        String borax = "create_nuclearindustry:borax_salt";
        double oreBorax = cost(borax, c -> c.step("create_nuclearindustry:crushing/borax_ore", borax));
        within("borax from brine", oreBorax, cost(borax, c -> {
            c.fluidStep("create_nuclearindustry:mixing/brine", "create_nuclearindustry:brine");
            c.step("create_nuclearindustry:centrifuging/brine", borax);
        }));
    }

    @Test
    void replacedCreateRecipesKeepWhatCreateGave() throws IOException {
        // Create's own results, as Create 6.0.10 ships them: the replacements must still give these.
        assertGives("create:splashing/sand", "minecraft:clay_ball");
        assertGives("create:splashing/red_sand", "minecraft:gold_nugget", "minecraft:dead_bush");
        assertGives("create:splashing/gravel", "minecraft:flint", "minecraft:iron_nugget");
        for (String tuff : new String[]{"create:crushing/tuff", "create:crushing/tuff_recycling"}) {
            assertGives(tuff, "minecraft:flint", "minecraft:gold_nugget", "create:copper_nugget", "create:zinc_nugget",
                    "minecraft:iron_nugget");
        }
    }

    // ------------------------------------------------------------------ ore routes

    private static void oreUranium(Costs c) {
        c.step("create_nuclearindustry:crushing/uranium_ore", "create_nuclearindustry:crushed_raw_uranium");
        c.step("create_nuclearindustry:splashing/uranium_concentrate", U_CONCENTRATE);
    }

    private static void oreSpodumene(Costs c) {
        c.step("create_nuclearindustry:crushing/spodumene_ore", "create_nuclearindustry:crushed_spodumene");
        c.step("create_nuclearindustry:splashing/spodumene_concentrate", SPODUMENE_CONCENTRATE);
    }

    // ------------------------------------------------------------------ checks

    private static void within(String route, double ore, double renewable) {
        double ratio = renewable / ore;
        assertTrue(ratio >= MIN_RATIO && ratio <= MAX_RATIO, String.format(
                "%s costs %.2f against %.2f from ore (x%.2f); it must cost between x%.1f and x%.1f", route, renewable, ore,
                ratio, MIN_RATIO, MAX_RATIO));
    }

    private static void atLeast(String route, double ore, double renewable) {
        assertTrue(renewable >= ore * MIN_RATIO, String.format("%s costs %.2f, cheaper than %.2f from ore", route, renewable, ore));
    }

    private static void assertGives(String recipe, String... items) throws IOException {
        JsonArray results = load(recipe).getAsJsonArray("results");
        for (String item : items) {
            boolean found = false;
            for (JsonElement result : results) {
                found |= item.equals(result.getAsJsonObject().get("id").getAsString());
            }
            assertTrue(found, recipe + " no longer gives " + item);
        }
    }

    // ------------------------------------------------------------------ the model

    /** Runs a route on fresh base prices and returns what it says {@code product} costs. */
    private static double cost(String product, Consumer<Costs> route) {
        Costs costs = new Costs();
        route.accept(costs);
        Double cost = costs.items.get(product);
        if (cost == null) {
            fail("the route never makes " + product);
        }
        return cost;
    }

    private static JsonObject load(String recipe) {
        int colon = recipe.indexOf(':');
        Path file = DATA.resolve(recipe.substring(0, colon)).resolve("recipe").resolve(recipe.substring(colon + 1) + ".json");
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("cannot read " + file + "; run ./gradlew runData first", e);
        }
    }

    private static final class Costs {
        final Map<String, Double> items = new HashMap<>();
        final Map<String, Double> fluidsPerMb = new HashMap<>();

        Costs() {
            for (String ore : new String[]{"create_nuclearindustry:uranium_ore", "create_nuclearindustry:spodumene_ore",
                    "create_nuclearindustry:borax_ore"}) {
                items.put(ore, 8.0);
            }
            for (String rock : new String[]{"minecraft:granite", "minecraft:tuff", "minecraft:deepslate", "minecraft:cobbled_deepslate",
                    "minecraft:sand", "minecraft:red_sand", "minecraft:gravel"}) {
                items.put(rock, 1.0);
            }
            // A tag is priced as its members: every kind of tuff is a block of rock.
            items.put("#create:stone_types/tuff", 1.0);
            items.put("minecraft:gunpowder", 2.0);
            items.put("minecraft:string", 0.5);
            items.put("minecraft:bone_meal", 0.5);
            fluidsPerMb.put("minecraft:water", 0.0);
        }

        /** Costs {@code product} by {@code recipe}, crediting the {@code credits} it hands back. */
        void step(String recipe, String product, String... credits) {
            JsonObject json = load(recipe);
            double spent = inputs(recipe, json) + machineTime(recipe, json);
            double yield = 0.0;
            for (JsonElement element : json.getAsJsonArray("results")) {
                JsonObject result = element.getAsJsonObject();
                if (result.has("amount")) {
                    continue;
                }
                double expected = (result.has("count") ? result.get("count").getAsInt() : 1)
                        * (result.has("chance") ? result.get("chance").getAsDouble() : 1.0);
                String id = result.get("id").getAsString();
                if (id.equals(product)) {
                    yield += expected;
                }
                for (String credit : credits) {
                    if (id.equals(credit)) {
                        spent -= expected * price(recipe, credit);
                    }
                }
            }
            if (yield <= 0.0) {
                fail(recipe + " does not make " + product);
            }
            items.put(product, spent / yield);
        }

        /** Costs a millibucket of {@code fluid} by {@code recipe}. */
        void fluidStep(String recipe, String fluid) {
            JsonObject json = load(recipe);
            double spent = inputs(recipe, json) + machineTime(recipe, json);
            int amount = 0;
            for (JsonElement element : json.getAsJsonArray("results")) {
                JsonObject result = element.getAsJsonObject();
                if (result.has("amount") && result.get("id").getAsString().equals(fluid)) {
                    amount += result.get("amount").getAsInt();
                }
            }
            if (amount <= 0) {
                fail(recipe + " does not make " + fluid);
            }
            fluidsPerMb.put(fluid, spent / amount);
        }

        private double inputs(String recipe, JsonObject json) {
            double total = 0.0;
            for (JsonElement element : json.getAsJsonArray("ingredients")) {
                JsonObject ingredient = element.getAsJsonObject();
                if (ingredient.has("fluid")) {
                    String fluid = ingredient.get("fluid").getAsString();
                    Double perMb = fluidsPerMb.get(fluid);
                    if (perMb == null) {
                        fail(recipe + " needs " + fluid + ", which the route has not priced");
                    }
                    total += perMb * ingredient.get("amount").getAsInt();
                } else {
                    // Enrichment and cooling grade ingredients wrap the item they take.
                    JsonObject item = ingredient.has("base") ? ingredient.getAsJsonObject("base") : ingredient;
                    if (item.has("tag")) {
                        total += price(recipe, "#" + item.get("tag").getAsString());
                    } else if (item.has("item")) {
                        total += price(recipe, item.get("item").getAsString());
                    } else {
                        fail(recipe + " takes " + ingredient + ", which this model cannot price");
                    }
                }
            }
            return total;
        }

        private double price(String recipe, String item) {
            Double price = items.get(item);
            if (price == null) {
                fail(recipe + " needs " + item + ", which the route has not priced");
            }
            return price;
        }

        private static double machineTime(String recipe, JsonObject json) {
            String type = json.get("type").getAsString();
            return switch (type) {
                case "create:crushing", "create_nuclearindustry:centrifuging" -> json.get("processing_time").getAsInt() / 100.0;
                case "create:splashing", "create_nuclearindustry:acid_leaching", "create_nuclearindustry:irradiation" -> 0.5;
                case "create:mixing" -> switch (json.has("heat_requirement") ? json.get("heat_requirement").getAsString() : "none") {
                    case "heated" -> 1.5;
                    case "superheated" -> 3.0;
                    default -> 1.0;
                };
                case "create:compacting" -> 0.5;
                case "create:emptying" -> 0.25;
                default -> throw new AssertionError(recipe + " is of a type the cost model does not know: " + type);
            };
        }
    }
}
