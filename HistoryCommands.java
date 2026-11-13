package ru.historyprogression.core;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

public final class HistoryCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("history").executes(context -> show(context.getSource())));
    }

    private int show(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RegionKey key = RegionKey.from(player.serverLevel(), player.blockPosition());
        ProgressSavedData data = ProgressSavedData.get(player.getServer());
        source.sendSuccess(() -> Component.literal("=== История региона " + key.x() + ", " + key.z() + " ==="), false);
        source.sendSuccess(() -> Component.literal("Археологическая шкала — накопленные признаки палеолита:"), false);
        line(source, data, key, player.getUUID(), "Добыто брёвен", "paleo_logs_broken");
        line(source, data, key, player.getUUID(), "Добыто каменных блоков", "paleo_stone_broken");
        line(source, data, key, player.getUUID(), "Получено кремня", "paleo_flint_collected");
        line(source, data, key, player.getUUID(), "Использований инструментов", "paleo_tool_uses");
        line(source, data, key, player.getUUID(), "Создано каменных топоров", "paleo_stone_axe_crafted");
        line(source, data, key, player.getUUID(), "Создано каменных кирок", "paleo_stone_pickaxe_crafted");
        line(source, data, key, player.getUUID(), "Собрано дикой пищи", "paleo_gathered_food");
        line(source, data, key, player.getUUID(), "Удачных уловов", "paleo_fish_catches");
        line(source, data, key, player.getUUID(), "Кандидатов на охотничью добычу", "paleo_hunt_kill_candidates");
        source.sendSuccess(() -> Component.literal("Общественная шкала — первобытное общество:"), false);
        line(source, data, key, player.getUUID(), "Деревянные блоки", "society_build_wood");
        line(source, data, key, player.getUUID(), "Каменные блоки", "society_build_stone");
        line(source, data, key, player.getUUID(), "Заборы и стены", "society_build_fences");
        line(source, data, key, player.getUUID(), "Плиты и ступени", "society_build_shaping");
        line(source, data, key, player.getUUID(), "Объекты обустройства", "society_camp_objects");
        line(source, data, key, player.getUUID(), "Передачи другим игрокам", "exchange_out");
        line(source, data, key, player.getUUID(), "Получения от игроков", "exchange_in");
        source.sendSuccess(() -> Component.literal("Участников с зафиксированными действиями: " + data.getContributors(key).size()), false);
        return 1;
    }

    private void line(CommandSourceStack source, ProgressSavedData data, RegionKey key, UUID playerId,
                      String label, String metric) {
        int value = data.getMetric(key, playerId, metric);
        source.sendSuccess(() -> Component.literal("• " + label + ": " + value), false);
    }
}
