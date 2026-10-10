package org.papiricoh.create_nuclearindustry.data;

/** Lang, models, tags, data maps, loot and recipes for the tokamak core: plasma physics, startup sequence, controller and its fluids. */
final class FusionDataGen extends AreaDataGen {
    static final FusionDataGen INSTANCE = new FusionDataGen();

    private FusionDataGen() {}

    @Override
    void lang() {
        LangEntries.fluid("helium", "Helium", "Helio");
        LangEntries.fluid("liquid_helium", "Liquid Helium", "Helio líquido");
        LangEntries.fluid("liquid_air", "Liquid Air", "Aire líquido");
        LangEntries.fluid("liquid_nitrogen", "Liquid Nitrogen", "Nitrógeno líquido");

        LangEntries.add("create_nuclearindustry.tokamak.phase.offline", "Offline", "Apagado");
        LangEntries.add("create_nuclearindustry.tokamak.phase.pump_down", "Pumping down", "Haciendo vacío");
        LangEntries.add("create_nuclearindustry.tokamak.phase.cool_down", "Cooling magnets", "Enfriando imanes");
        LangEntries.add("create_nuclearindustry.tokamak.phase.energize", "Energizing field", "Energizando campo");
        LangEntries.add("create_nuclearindustry.tokamak.phase.breakdown", "Breakdown", "Ruptura del gas");
        LangEntries.add("create_nuclearindustry.tokamak.phase.heating", "Heating", "Calentando");
        LangEntries.add("create_nuclearindustry.tokamak.phase.burn", "Burning", "En combustión");
        LangEntries.add("create_nuclearindustry.tokamak.phase.ramp_down", "Ramping down", "Bajando en rampa");

        LangEntries.add("create_nuclearindustry.tokamak.trip.none", "None", "Ninguno");
        LangEntries.add("create_nuclearindustry.tokamak.trip.operator", "Operator shutdown", "Parada del operador");
        LangEntries.add("create_nuclearindustry.tokamak.trip.vacuum_lost", "Vacuum lost", "Vacío perdido");
        LangEntries.add("create_nuclearindustry.tokamak.trip.magnets_warm", "Magnets too warm", "Imanes demasiado calientes");
        LangEntries.add("create_nuclearindustry.tokamak.trip.no_field", "Too little magnetic field", "Campo magnético insuficiente");
        LangEntries.add("create_nuclearindustry.tokamak.trip.no_fuel", "No fuel", "Sin combustible");
        LangEntries.add("create_nuclearindustry.tokamak.trip.structure", "Structure changed", "Estructura alterada");
        LangEntries.add("create_nuclearindustry.tokamak.trip.disruption", "Plasma disruption", "Disrupción del plasma");

        LangEntries.add("subtitle.create_nuclearindustry.magnet_hum", "Magnets hum", "Zumban los imanes");
        LangEntries.add("subtitle.create_nuclearindustry.plasma_rumble", "Plasma roars", "Ruge el plasma");
        LangEntries.add("subtitle.create_nuclearindustry.plasma_breakdown", "Plasma strikes", "Se enciende el plasma");
        LangEntries.add("subtitle.create_nuclearindustry.cryoplant_hum", "Cryogenic plant runs", "Funciona la planta criogénica");
    }
}
