package ru.historyprogression.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class ProgressEvents {
    private boolean isSurvivalPlayer(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && !serverPlayer.isCreative()
                && !serverPlayer.isSpectator();
    }

    private RegionKey region(ServerPlayer player, BlockPos pos) {
        return RegionKey.from(player.serverLevel(), pos);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isSurvivalPlayer(player) || event.isCanceled()) return;
        BlockState state = event.getState();
        RegionKey key = region(player, event.getPos());
        ProgressSavedData data = ProgressSavedData.get(player.getServer());

        if (state.is(BlockTags.LOGS)) data.increment(key, player.getUUID(), "paleo_logs_broken", 1);
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.DEEPSLATE)) {
            data.increment(key, player.getUUID(), "paleo_stone_broken", 1);
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() instanceof DiggerItem) {
            data.increment(key, player.getUUID(), "paleo_tool_uses", 1);
        }
    }

    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !isSurvivalPlayer(player)) return;
        RegionKey key = region(player, event.getPos());
        ProgressSavedData data = ProgressSavedData.get(player.getServer());
        BlockState state = event.getState();

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.FLINT)) {
                data.increment(key, player.getUUID(), "paleo_flint_collected", stack.getCount());
            }

            boolean wildFoodBlock = state.is(Blocks.SWEET_BERRY_BUSH)
                    || state.is(Blocks.CAVE_VINES)
                    || state.is(Blocks.CAVE_VINES_PLANT)
                    || state.is(Blocks.BROWN_MUSHROOM)
                    || state.is(Blocks.RED_MUSHROOM)
                    || state.is(Blocks.MELON)
                    || state.is(Blocks.PUMPKIN);
            boolean wildFoodItem = stack.is(Items.SWEET_BERRIES)
                    || stack.is(Items.GLOW_BERRIES)
                    || stack.is(Items.RED_MUSHROOM)
                    || stack.is(Items.BROWN_MUSHROOM)
                    || stack.is(Items.MELON_SLICE)
                    || stack.is(Items.APPLE);
            if (wildFoodBlock && wildFoodItem) {
                data.increment(key, player.getUUID(), "paleo_gathered_food", stack.getCount());
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isSurvivalPlayer(player) || event.isCanceled()) return;
        BlockState state = event.getPlacedBlock();
        String metric = buildingMetric(state);
        boolean campObject = isCampObject(state);
        if (metric == null && !campObject) return;

        RegionKey key = region(player, event.getPos());
        ProgressSavedData data = ProgressSavedData.get(player.getServer());
        if (metric != null) data.recordUniquePlacement(key, player.getUUID(), event.getPos(), metric);
        if (campObject) data.recordUniquePlacement(key, player.getUUID(), event.getPos(), "society_camp_objects");
    }

    private String buildingMetric(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) return "society_build_wood";
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.STONE_BRICKS)) return "society_build_stone";
        if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock) return "society_build_fences";
        if (state.getBlock() instanceof SlabBlock || state.getBlock() instanceof StairBlock) return "society_build_shaping";
        return null;
    }

    private boolean isCampObject(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)
                || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isSurvivalPlayer(player)) return;
        ItemStack crafted = event.getCrafting();
        String metric = null;
        if (crafted.is(Items.STONE_AXE)) metric = "paleo_stone_axe_crafted";
        else if (crafted.is(Items.STONE_PICKAXE)) metric = "paleo_stone_pickaxe_crafted";
        if (metric != null) {
            ProgressSavedData.get(player.getServer()).increment(region(player, player.blockPosition()), player.getUUID(), metric, crafted.getCount());
        }
    }

    @SubscribeEvent
    public void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isSurvivalPlayer(player)) return;
        boolean caughtFish = event.getDrops().stream().anyMatch(stack -> stack.is(Items.COD)
                || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH));
        if (caughtFish) {
            ProgressSavedData.get(player.getServer()).increment(region(player, player.blockPosition()), player.getUUID(), "paleo_fish_catches", 1);
        }
    }

    @SubscribeEvent
    public void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isSurvivalPlayer(player)) return;
        ItemEntity item = (ItemEntity) event.getEntity();
        ProgressSavedData.get(player.getServer()).rememberToss(item.getUUID(), player.getUUID(), region(player, item.blockPosition()));
    }

    @SubscribeEvent
    public void onPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isSurvivalPlayer(player)) return;
        ItemEntity item = event.getItemEntity();
        ProgressSavedData.get(player.getServer()).resolvePickup(item.getUUID(), player.getUUID(), region(player, item.blockPosition()));
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player) || !isSurvivalPlayer(player)) return;
        // The exact wild-animal whitelist is intentionally deferred to the configurable mob list.
        // We record candidate kills separately so domestic livestock can be excluded in the next pass.
        ProgressSavedData.get(player.getServer()).increment(region(player, event.getEntity().blockPosition()), player.getUUID(),
                "paleo_hunt_kill_candidates", 1);
    }
}
