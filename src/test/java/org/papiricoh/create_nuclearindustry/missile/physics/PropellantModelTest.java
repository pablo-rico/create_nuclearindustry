package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a stage burns: one kind of propellant per stage, solid pushing harder, liquid reaching further, partial fills scaled. */
class PropellantModelTest {
    private static final PropellantModel.Tank CARTRIDGE = new PropellantModel.Tank(true, 0, 0);
    private static final PropellantModel.Tank FULL = new PropellantModel.Tank(false, 1000, 1000);

    private static PropellantModel.Invalid invalid(List<PropellantModel.Tank> tanks) {
        return assertThrows(PropellantModel.Invalid.class, () -> PropellantModel.stage(tanks));
    }

    @Test
    void aStageBurnsOneKind() {
        assertEquals(PropellantModel.Kind.SOLID, PropellantModel.stage(List.of(CARTRIDGE, CARTRIDGE)).kind());
        assertEquals(PropellantModel.Kind.LIQUID, PropellantModel.stage(List.of(FULL, FULL)).kind());
        PropellantModel.Invalid mixed = invalid(List.of(CARTRIDGE, FULL));
        assertEquals("mixed_propellant", mixed.reason);
        assertEquals(1, mixed.tank, "the second tank breaks the pattern");
        assertEquals("mixed_propellant", invalid(List.of(new PropellantModel.Tank(true, 10, 10))).reason, "one tank holding both");
    }

    @Test
    void aTankWithNothingToBurnIsReported() {
        assertEquals("fuel", invalid(List.of(FULL, PropellantModel.Tank.EMPTY)).reason);
        assertEquals(1, invalid(List.of(FULL, PropellantModel.Tank.EMPTY)).tank);
        assertEquals("fuel", invalid(List.of(new PropellantModel.Tank(false, 1000, 0))).reason, "fuel without oxygen burns nothing");
        assertEquals("fuel", invalid(List.of()).reason);
    }

    @Test
    void liquidsBurnOneToOne() {
        assertEquals(0.25, new PropellantModel.Tank(false, 250, 1000).fill(), 1e-9);
        assertEquals(0.25, new PropellantModel.Tank(false, 1000, 250).fill(), 1e-9);
        assertEquals(1.0, new PropellantModel.Tank(false, 5000, 5000).fill(), 1e-9, "never more than a full tank");
    }

    @Test
    void solidPushesHarderLiquidExhaustsFaster() {
        MissileDesign.Stage solid = PropellantModel.stage(List.of(CARTRIDGE, CARTRIDGE)).design();
        MissileDesign.Stage liquid = PropellantModel.stage(List.of(FULL, FULL)).design();
        assertTrue(solid.thrust() > liquid.thrust());
        assertTrue(liquid.exhaustVelocity() > solid.exhaustVelocity());
        assertEquals(solid.propellant(), liquid.propellant(), 1e-9, "a full tank is a full tank");
    }

    @Test
    void partialFillsScaleThePropellant() {
        PropellantModel.Stage half = PropellantModel.stage(List.of(new PropellantModel.Tank(false, 500, 500), FULL));
        assertEquals(1.5, half.fill(), 1e-9);
        assertEquals(1.5 * PropellantModel.UNITS_PER_TANK, half.design().propellant(), 1e-9);
    }

    @Test
    void theSolidStageIsTheOldCartridgeStage() {
        MissileDesign.Stage stage = PropellantModel.solid(3).design();
        assertEquals(0.9, stage.dryMass(), 1e-9);
        assertEquals(6.0, stage.propellant(), 1e-9);
        assertEquals(0.24, stage.thrust(), 1e-9);
        assertEquals(4.0, stage.exhaustVelocity(), 1e-9);
    }

    @Test
    void aLiquidRocketReachesFurther() {
        BallisticModel.Params params = BallisticModel.Params.DEFAULT;
        MissileDesign solid = new MissileDesign(List.of(PropellantModel.solid(2).design()), 1, 0);
        MissileDesign liquid = new MissileDesign(List.of(PropellantModel.stage(List.of(FULL, FULL)).design()), 1, 0);
        MissileDesign halfLiquid = new MissileDesign(List.of(PropellantModel.stage(List.of(
                new PropellantModel.Tank(false, 500, 500), new PropellantModel.Tank(false, 500, 500))).design()), 1, 0);
        double solidRange = MissileFlight.maxRange(solid, params);
        double liquidRange = MissileFlight.maxRange(liquid, params);
        assertTrue(liquid.liftOffThrustToWeight(params.gravity()) > 1, "a liquid rocket still lifts off");
        assertTrue(liquidRange > solidRange * 1.1, "liquid " + liquidRange + " vs solid " + solidRange);
        assertTrue(MissileFlight.maxRange(halfLiquid, params) < liquidRange, "half full reaches less");
    }

    @Test
    void aLongRangeLiquidIcbmComesBackFastEnoughToGlow() {
        // The rocket effects start heating a reentry vehicle at 1.5 blocks a tick and glow fully near 7.
        BallisticModel.Params params = BallisticModel.Params.DEFAULT;
        MissileDesign.Stage stage = PropellantModel.stage(List.of(FULL, FULL)).design();
        MissileDesign icbm = new MissileDesign(List.of(stage, stage, stage), 1.3, 0);
        double range = MissileFlight.maxRange(icbm, params);
        assertTrue(range > 10000, "a three-stage liquid rocket crosses ten kilometres, got " + range);
        MissileFlight flight = new MissileFlight(icbm, V3.ZERO, new V3(range * 0.95, 0, 0), V3.ZERO);
        for (int i = 0; i < 100000 && !flight.burnedOut(); i++) flight.step(params);
        double speed = flight.burnoutVelocity().length();
        assertTrue(speed >= 6 && speed <= 9, "and comes back at 6 to 9 blocks a tick, got " + speed);
    }
}
