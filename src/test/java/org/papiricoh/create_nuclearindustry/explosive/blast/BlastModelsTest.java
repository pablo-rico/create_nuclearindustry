package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The zones of a detonation, its crater, flash, prompt dose, fallout plume, wind and cloud. */
class BlastModelsTest {
    private static final DetonationProfile.Settings S = DetonationProfile.Settings.DEFAULT;

    private static DetonationProfile full(double burstHeight) {
        return DetonationProfile.of(160, 250, 1, burstHeight, S);
    }

    @Test
    void theZonesNestInsideOut() {
        DetonationProfile p = full(0);
        assertFalse(p.airburst());
        assertTrue(p.fireballRadius() < p.craterRadius(), "the fireball is smaller than the crater");
        assertTrue(p.craterRadius() < p.rimRadius() && p.rimRadius() < p.ejectaRadius());
        assertTrue(p.glassRadius() < p.craterRadius());
        assertTrue(p.craterRadius() <= p.severeRadius() && p.severeRadius() <= p.moderateRadius());
        assertTrue(p.moderateRadius() <= DetonationProfile.MAX_ZONE_REACH * 250 + 1e-9, "the zones stop at 1.2 radii");
        assertTrue(p.thermalRadius() > p.moderateRadius(), "the flash reaches past the blast");
        assertEquals(p.thermalRadius(), p.blockReach(), 1e-9);
        assertTrue(p.craterRadius() > 60 && p.craterRadius() < 90, "a full warhead's crater is about 75 blocks: " + p.craterRadius());
    }

    @Test
    void theSevereZoneHoldsItsOverpressure() {
        DetonationProfile p = full(0);
        double kPa = BlastWaveModel.overpressure(p.severeRadius() - 1, p.yieldKg());
        assertTrue(kPa >= S.severeKPa() || p.severeRadius() >= 250, "inside the severe zone the wave holds 35 kPa: " + kPa);
        assertEquals(S.severeKPa(), BlastWaveModel.overpressure(DetonationProfile.radiusAt(S.severeKPa(), p.yieldKg()), p.yieldKg()), 0.01);
    }

    @Test
    void anAirburstDigsNoCraterButReachesFurther() {
        DetonationProfile ground = full(0);
        DetonationProfile air = full(80);
        assertTrue(air.airburst());
        assertEquals(0, air.craterRadius());
        assertTrue(air.thermalRadius() > ground.thermalRadius());
        assertTrue(air.falloutIntensity() < ground.falloutIntensity() * 0.2, "an airburst leaves little fallout");
        DetonationProfile small = DetonationProfile.of(40, 85, 1, 0, S);
        assertTrue(small.craterRadius() < ground.craterRadius() && small.thermalRadius() < ground.thermalRadius(), "smaller warheads, smaller zones");
    }

    @Test
    void theCraterIsABowlWithARaisedLip() {
        DetonationProfile p = full(0);
        CraterShape shape = new CraterShape(p);
        assertEquals(-p.craterDepth(), shape.bowlSurface(0, 0), 1e-9, "deepest at the centre");
        assertTrue(shape.bowlSurface(30, 0) > shape.bowlSurface(10, 0), "rising outwards");
        int edge = (int) Math.floor(shape.radius(60, 0));
        assertTrue(shape.bowlSurface(edge, 0) > 0, "the lip stands above the ground at the edge");
        assertTrue(shape.rimPile(edge + 2, 0) > 0, "the rim is piled just outside");
        assertEquals(0, shape.rimPile((int) (p.rimRadius() * 1.2), 0), "and stops at its end");
        assertTrue(shape.inBowl(0, 0) && !shape.inBowl(200, 0));
        assertEquals(CraterShape.Floor.NONE, shape.floor(300, 0, 200, 0));
        int glass = 0;
        for (int x = 20; x < 50; x++) {
            if (shape.floor(x, 7, x, 7) == CraterShape.Floor.TRINITITE) {
                glass++;
            }
        }
        assertTrue(glass > 10, "the floor is mostly glass: " + glass);
        assertEquals(0, new CraterShape(full(80)).rimPile(10, 0), "an airburst has no rim");
    }

    @Test
    void theFlashFallsWithTheSquareOfTheDistance() {
        assertEquals(1, ThermalPulse.strength(400, 400), 1e-9);
        assertEquals(4, ThermalPulse.strength(200, 400), 1e-9);
        assertEquals(0, ThermalPulse.fireSeconds(0.5));
        assertTrue(ThermalPulse.fireSeconds(4) > ThermalPulse.fireSeconds(1.5));
        assertEquals(0, ThermalPulse.damage(1), 1e-9);
        assertTrue(ThermalPulse.chars(5) && !ThermalPulse.chars(2));
        assertTrue(ThermalPulse.fireChance(10) <= 0.35);
    }

    @Test
    void thePromptDoseFallsFastWithDistance() {
        DetonationProfile p = full(0);
        double near = PromptDose.dose(p, 20, 3000);
        double mid = PromptDose.dose(p, 60, 3000);
        assertTrue(near > 600, "lethal close in: " + near);
        assertTrue(mid < near && mid > 0);
        assertEquals(0, PromptDose.dose(p, p.promptRadius() + 1, 3000), 1e-9);
    }

    @Test
    void theFalloutStretchesDownwind() {
        double down = FalloutPlume.share(150, 0, 100, 0, 1);
        double up = FalloutPlume.share(-150, 0, 100, 0, 1);
        double side = FalloutPlume.share(0, 80, 100, 0, 1);
        assertTrue(down > 0, "150 blocks downwind still gets fallout");
        assertEquals(0, up, 1e-9, "150 blocks upwind gets none");
        assertEquals(0, side, 1e-9, "nor 80 to the side");
        assertEquals(1, FalloutPlume.share(0, 0, 100, 1.3, 1), 1e-9, "ground zero gets it all");
        assertEquals(0.5, FalloutPlume.share(0, 50, 100, 0, 0), 1e-9, "no stretch is the old circle");
        assertTrue(FalloutPlume.share(0, 150, 100, Math.PI / 2, 1) > 0, "the plume follows the wind");
    }

    @Test
    void theWindTurnsSmoothlyAndIsTheSameForEveryone() {
        assertEquals(Wind.angle(42, 1000), Wind.angle(42, 1000));
        double a = Wind.angle(42, Wind.DAY - 1);
        double b = Wind.angle(42, Wind.DAY);
        assertTrue(Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b))) < 0.01, "no jump at midnight");
    }

    @Test
    void theCloudRisesSpreadsCoolsAndFades() {
        MushroomCloudModel cloud = new MushroomCloudModel(7, 250, 30, false, 0);
        assertTrue(cloud.capHeight(5) < cloud.capHeight(30) && cloud.capHeight(200) <= cloud.topHeight() + 1e-9);
        List<MushroomCloudModel.Puff> early = cloud.puffs(0.2, 1);
        List<MushroomCloudModel.Puff> late = cloud.puffs(120, 1);
        assertTrue(early.stream().anyMatch(MushroomCloudModel.Puff::glowing), "the fireball glows");
        assertTrue(late.stream().noneMatch(MushroomCloudModel.Puff::glowing), "two minutes later it has cooled");
        assertTrue(cloud.puffs(MushroomCloudModel.LIFETIME + 1, 1).isEmpty(), "and is gone at the end");
        assertEquals(early, cloud.puffs(0.2, 1), "the same seed, the same cloud");
        assertTrue(cloud.puffs(60, 2).size() > cloud.puffs(60, 0.5).size(), "detail adds puffs");
        double top = late.stream().mapToDouble(MushroomCloudModel.Puff::y).max().orElse(0);
        assertTrue(top > 200, "the cap stands high: " + top);
    }
}
