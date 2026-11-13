package ru.historyprogression.core;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HistoricalProgression.MOD_ID)
public final class HistoricalProgression {
    public static final String MOD_ID = "historical_progression";

    public HistoricalProgression() {
        NeoForge.EVENT_BUS.register(new ProgressEvents());
        NeoForge.EVENT_BUS.register(new HistoryCommands());
    }
}
