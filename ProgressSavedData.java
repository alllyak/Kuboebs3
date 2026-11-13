package ru.historyprogression.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public final class ProgressSavedData extends SavedData {
    private static final String DATA_NAME = "historical_progression_regions";
    public static final Factory<ProgressSavedData> FACTORY = new Factory<>(
            ProgressSavedData::new,
            ProgressSavedData::load
    );

    private final CompoundTag root = new CompoundTag();

    public static ProgressSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static ProgressSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ProgressSavedData data = new ProgressSavedData();
        data.root.merge(tag);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.merge(root);
        return tag;
    }

    private CompoundTag getOrCreate(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) {
            parent.put(key, new CompoundTag());
        }
        return parent.getCompound(key);
    }

    private CompoundTag region(RegionKey key) {
        return getOrCreate(root, key.storageKey());
    }

    private CompoundTag player(RegionKey key, UUID playerId) {
        return getOrCreate(getOrCreate(region(key), "players"), playerId.toString());
    }

    public void increment(RegionKey key, UUID playerId, String metric, int amount) {
        if (amount <= 0) return;
        CompoundTag stats = player(key, playerId);
        stats.putInt(metric, stats.getInt(metric) + amount);
        setDirty();
    }

    public int getMetric(RegionKey key, UUID playerId, String metric) {
        CompoundTag players = region(key).getCompound("players");
        if (!players.contains(playerId.toString(), Tag.TAG_COMPOUND)) return 0;
        return players.getCompound(playerId.toString()).getInt(metric);
    }

    public Set<UUID> getContributors(RegionKey key) {
        Set<UUID> result = new HashSet<>();
        CompoundTag players = region(key).getCompound("players");
        for (String name : players.getAllKeys()) {
            try {
                result.add(UUID.fromString(name));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed or legacy entries.
            }
        }
        return result;
    }

    public boolean recordUniquePlacement(RegionKey key, UUID playerId, BlockPos pos, String metric) {
        CompoundTag stats = player(key, playerId);
        CompoundTag positions = getOrCreate(stats, "counted_placements");
        String positionKey = Long.toString(pos.asLong());
        if (positions.contains(positionKey)) return false;
        positions.putBoolean(positionKey, true);
        stats.putInt(metric, stats.getInt(metric) + 1);
        setDirty();
        return true;
    }

    public void rememberToss(UUID itemEntityId, UUID ownerId, RegionKey regionKey) {
        CompoundTag pending = getOrCreate(root, "pending_transfers");
        CompoundTag entry = new CompoundTag();
        entry.putUUID("owner", ownerId);
        entry.putString("region", regionKey.storageKey());
        pending.put(itemEntityId.toString(), entry);
        setDirty();
    }

    public void resolvePickup(UUID itemEntityId, UUID receiverId, RegionKey pickupRegion) {
        CompoundTag pending = getOrCreate(root, "pending_transfers");
        String entityKey = itemEntityId.toString();
        if (!pending.contains(entityKey, Tag.TAG_COMPOUND)) return;

        CompoundTag entry = pending.getCompound(entityKey);
        UUID ownerId = entry.getUUID("owner");
        String tossRegion = entry.getString("region");
        pending.remove(entityKey);

        if (!ownerId.equals(receiverId) && tossRegion.equals(pickupRegion.storageKey())) {
            increment(pickupRegion, ownerId, "exchange_out", 1);
            increment(pickupRegion, receiverId, "exchange_in", 1);
        } else {
            setDirty();
        }
    }

    public void forgetToss(UUID itemEntityId) {
        CompoundTag pending = getOrCreate(root, "pending_transfers");
        if (pending.contains(itemEntityId.toString())) {
            pending.remove(itemEntityId.toString());
            setDirty();
        }
    }
}
