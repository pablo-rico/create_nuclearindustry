package org.papiricoh.create_nuclearindustry.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The fission reactor speaks the player's language: its chat messages, validation errors, goggle
 * lines and Display Link status are translatable, and every key they use has an English and a
 * Spanish text.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorMessageTests {
    private static Set<String> english;
    private static Set<String> spanish;

    /** The keys of one of the mod's lang files, or an empty set if it cannot be read. */
    private static Set<String> keys(String file) {
        try {
            Path path = ModList.get().getModFileById(Create_NuclearIndustry.MODID).getFile()
                    .findResource("assets", Create_NuclearIndustry.MODID, "lang", file);
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            return json.keySet();
        } catch (Exception e) {
            return Set.of();
        }
    }

    private static void assertTranslated(GameTestHelper helper, Component component, String what) {
        if (english == null) {
            english = keys("en_us.json");
            spanish = keys("es_es.json");
        }
        helper.assertFalse(english.isEmpty() || spanish.isEmpty(), "the lang files could not be read");
        List<String> keys = new ArrayList<>();
        List<String> literals = new ArrayList<>();
        collect(component, keys, literals);
        helper.assertFalse(keys.isEmpty(), what + " is not translatable: " + component.getString());
        for (String key : keys) {
            helper.assertTrue(english.contains(key), "no English text for " + key + " (" + what + ")");
            helper.assertTrue(spanish.contains(key), "no Spanish text for " + key + " (" + what + ")");
        }
        for (String literal : literals) {
            helper.assertFalse(literal.chars().anyMatch(Character::isLetter), what + " has untranslated text: '" + literal + "'");
        }
    }

    /** Every lang key in a component, its arguments and siblings, and its literal pieces (other than arguments). */
    private static void collect(Component component, List<String> keys, List<String> literals) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            keys.add(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child) {
                    collectArgument(child, keys);
                }
            }
        } else if (component.getContents() instanceof PlainTextContents plain) {
            literals.add(plain.text());
        }
        for (Component sibling : component.getSiblings()) {
            collect(sibling, keys, literals);
        }
    }

    /** Arguments may hold values (sizes, positions) as literals; only their keys are checked. */
    private static void collectArgument(Component component, List<String> keys) {
        collect(component, keys, new ArrayList<>());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void validationErrorsAreLangKeys(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        // No steam port, and a stone block where a wall should be.
        helper.setBlock(FissionTestBuilder.STEAM_PORT, AllNuclearBlocks.REACTOR_CASING.get());
        helper.setBlock(FissionTestBuilder.WALL_CASING, net.minecraft.world.level.block.Blocks.STONE);
        MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> result =
                ReactorStructureValidator.validate(helper.getLevel(), helper.absolutePos(FissionTestBuilder.CONTROLLER));
        helper.assertFalse(result.formed(), "the broken reactor must not form");
        Set<String> found = result.errors().stream().map(MultiblockText::key).collect(Collectors.toSet());
        helper.assertTrue(found.contains(ReactorStructureValidator.CASING_MISSING), "the missing casing is reported, got " + found);
        helper.assertTrue(found.contains(ReactorStructureValidator.KEY + "no_steam_port"), "the missing steam port is reported, got " + found);
        for (String error : result.errors()) {
            helper.assertTrue(error.startsWith(ReactorStructureValidator.KEY), "not a lang key: " + error);
            assertTranslated(helper, MultiblockText.text(error), "validation error");
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void chatMessagesAreTranslatable(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            reactor.attemptFormation(null);
            assertTranslated(helper, reactor.getLastMessage(), "the formed message");
            helper.assertTrue(reactor.getLastMessage().getContents() instanceof TranslatableContents formed
                            && formed.getKey().equals(ReactorBlockEntity.MESSAGE + "formed"),
                    "forming says so, got " + reactor.getLastMessage().getString());
            reactor.setManualScram(true);
            assertTranslated(helper, reactor.getLastMessage(), "the manual SCRAM message");
            reactor.setManualScram(false);
            assertTranslated(helper, reactor.getLastMessage(), "the SCRAM release message");
            helper.setBlock(FissionTestBuilder.STEAM_PORT, AllNuclearBlocks.REACTOR_CASING.get());
            reactor.attemptFormation(null);
            assertTranslated(helper, reactor.getLastMessage(), "the scrammed message");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void gogglesAndReadoutsAreTranslatable(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.assertTrue(ReactorReadout.STATUS.read(reactor).getContents() instanceof TranslatableContents, "an unformed status is translatable");
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            List<Component> tooltip = new ArrayList<>();
            reactor.addToGoggleTooltip(tooltip, false);
            helper.assertTrue(tooltip.size() > 10, "the goggles show the reactor");
            for (Component line : tooltip) {
                assertTranslated(helper, line, "goggle line");
            }
            assertTranslated(helper, ReactorReadout.STATUS.read(reactor), "the status readout");
            List<Component> port = new ArrayList<>();
            if (helper.getLevel().getBlockEntity(helper.absolutePos(FissionTestBuilder.STEAM_PORT))
                    instanceof com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation info) {
                info.addToGoggleTooltip(port, false);
            }
            helper.assertFalse(port.isEmpty(), "the steam port has goggle lines");
            for (Component line : port) {
                assertTranslated(helper, line, "port goggle line");
            }
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void unformedGogglesAreTranslatable(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(FissionTestBuilder.STEAM_PORT, AllNuclearBlocks.REACTOR_CASING.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(reactor.isFormed(), "no steam port, no reactor");
            List<Component> tooltip = new ArrayList<>();
            reactor.addToGoggleTooltip(tooltip, false);
            for (Component line : tooltip) {
                assertTranslated(helper, line, "goggle line");
            }
            String missing = tooltip.get(tooltip.size() - 1).getString();
            helper.assertTrue(missing.contains("steam output port"), "the goggles say what is missing, got " + missing);
            helper.succeed();
        });
    }
}
