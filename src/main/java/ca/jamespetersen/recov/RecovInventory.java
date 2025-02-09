package ca.jamespetersen.recov;

import com.mojang.authlib.GameProfile;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Pair;
import net.minecraft.world.PersistentState;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public class RecovInventory extends PersistentState {
    private final HashMap<UUID, ArrayDeque<StoredInventory>> inventories = new HashMap<>();
    private final TreeMap<LocalDateTime, ArrayList<UUID>> oldestInventoriesPerPlayer = new TreeMap<>();
    private int ticksUntilExpired = 48000;
    private int tickUpdatePeriod = 8000;
    private int maxInventoriesPerPlayer = 10;
    private boolean requirePermissionToCache = false;
    public static RecovInventory globalRecovInventory;

    public RecovInventory() {}

    public RecovInventory(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ticksUntilExpired = nbt.getInt("ticksUntilExpired");
        tickUpdatePeriod = nbt.getInt("tickUpdatePeriod");
        maxInventoriesPerPlayer = nbt.getInt("maxInventoriesPerPlayer");
        requirePermissionToCache = nbt.getBoolean("requirePermissionToCache");
        NbtList playerInventories = nbt.getList("storedPlayerInventories", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < playerInventories.size(); ++i) {
            NbtCompound playerInventory = playerInventories.getCompound(i);
            UUID id = playerInventory.getUuid("playerId");
            NbtList storedInventoryListNbt = playerInventory.getList("storedInventories", NbtElement.COMPOUND_TYPE);
            ArrayDeque<StoredInventory> storedInventoryList = new ArrayDeque<>();

            for (int j = 0; j < storedInventoryListNbt.size(); ++j) {
                StoredInventory storedInventory = new StoredInventory(storedInventoryListNbt.getCompound(j), registryLookup);
                storedInventoryList.addLast(storedInventory);
            }

            inventories.put(id, storedInventoryList);
            cacheIdWithTime(id, Objects.requireNonNull(storedInventoryList.peekFirst()).timeCreated);
        }
        removeOld();
    }

    public void removeOld() {
        removeOlderThan(LocalDateTime.now().minusSeconds(ticksUntilExpired / 20));
    }

    public void removeOlderThan(LocalDateTime time) {
        SortedMap<LocalDateTime, ArrayList<UUID>> head = oldestInventoriesPerPlayer.headMap(time);
        while (!head.isEmpty()) {
            markDirty();
            Map.Entry<LocalDateTime, ArrayList<UUID>> entry = head.firstEntry();
            for (UUID id : entry.getValue()) {
                ArrayDeque<StoredInventory> queue = inventories.get(id);
                StoredInventory inventory;
                while ((inventory = queue.peekFirst()) != null && !time.isBefore(inventory.timeCreated)) {
                    queue.removeFirst();
                }
                if (inventory == null) {
                    inventories.remove(id);
                } else {
                    cacheIdWithTime(id, inventory.timeCreated);
                }
            }
            oldestInventoriesPerPlayer.remove(entry.getKey());
        }
    }

    private void cacheIdWithTime(UUID id, LocalDateTime time) {
        ArrayList<UUID> idArray = oldestInventoriesPerPlayer.computeIfAbsent(time, k -> new ArrayList<>());
        if (!idArray.contains(id)) {
            idArray.add(id);
        }
    }

    public void insertInventory(PlayerEntity playerEntity, StoredInventory inventory) {
        if (requirePermissionToCache && !Permissions.check(playerEntity, "recov.cacheinventory")) {
            return;
        }
        markDirty();
        UUID playerId = playerEntity.getGameProfile().getId();
        ArrayDeque<StoredInventory> queue = inventories.computeIfAbsent(playerId, k -> {
            cacheIdWithTime(k, inventory.timeCreated);
            return new ArrayDeque<>();
        });
        queue.addLast(inventory);
        if (queue.size() > maxInventoriesPerPlayer) {
            LocalDateTime time = queue.removeFirst().timeCreated;
            oldestInventoriesPerPlayer.computeIfPresent(time, (k, v) -> {
                v.remove(playerId);
                return v.isEmpty() ? null : v;
            });
            cacheIdWithTime(playerId, Objects.requireNonNull(queue.peekFirst()).timeCreated);
        }
    }

    public void setTicksUntilExpired(int ticksUntilExpired) {
        if (ticksUntilExpired != this.ticksUntilExpired) {
            markDirty();
            this.ticksUntilExpired = ticksUntilExpired;
        }
    }

    public void setRequirePermissionToCache(boolean requirePermissionToCache) {
        if (requirePermissionToCache != this.requirePermissionToCache) {
            markDirty();
            this.requirePermissionToCache = requirePermissionToCache;
        }
    }

    public void clear() {
        if (!inventories.isEmpty()) {
            markDirty();
        }
        inventories.clear();
        oldestInventoriesPerPlayer.clear();
    }

    public void clear(UUID id) {
        ArrayDeque<StoredInventory> queue = inventories.remove(id);
        if (queue != null) {
            markDirty();
            oldestInventoriesPerPlayer.computeIfPresent(queue.getFirst().timeCreated, (k, v) -> {
                v.remove(id);
                return v.isEmpty() ? null : v;
            });
        }
    }

    public StoredInventory getStoredInventory(UUID id, int index) {
        ArrayDeque<StoredInventory> array = inventories.get(id);
        if (array == null) {
            return null;
        }
        if (array.size() <= index) {
            return null;
        }
        return array.toArray(new StoredInventory[0])[array.size() - index - 1];
    }

    public Iterator<StoredInventory> getStoredInventories(UUID id) {
        ArrayDeque<StoredInventory> array = inventories.get(id);
        if (array == null) {
            return Collections.emptyIterator();
        }
        return array.descendingIterator();
    }

    public Stream<String> getAvailableUsernames(ServerCommandSource source) {
        return inventories
                .keySet()
                .stream()
                .map(id -> Objects.requireNonNull(source.getServer().getUserCache()).getByUuid(id))
                .flatMap(Optional::stream)
                .map(GameProfile::getName);
    }

    public Stream<Pair<String, Integer>> getPlayersAndAmounts(ServerCommandSource source) {
        return inventories
                .entrySet()
                .stream()
                .map(entry ->
                        Objects.requireNonNull(Objects.requireNonNull(source.getServer().getUserCache()).getByUuid(entry.getKey()))
                                .map(profile -> new Pair<>(profile, entry.getValue().size())))
                .flatMap(Optional::stream)
                .map(pair -> new Pair<>(pair.getLeft().getName(), pair.getRight()));
    }
                                                @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList playerInventories = new NbtList();
        inventories.forEach((id, storedInventories) -> {
            NbtList storedInventoryList = new NbtList();
            for (StoredInventory storedInventory : storedInventories) {
                storedInventoryList.add(storedInventory.encode(registryLookup));
            }
            NbtCompound playerInventoryElement = new NbtCompound();
            playerInventoryElement.putUuid("playerId", id);
            playerInventoryElement.put("storedInventories", storedInventoryList);
            playerInventories.add(playerInventoryElement);
        });
        nbt.put("storedPlayerInventories", playerInventories);
        nbt.putInt("ticksUntilExpired", ticksUntilExpired);
        nbt.putInt("tickUpdatePeriod", tickUpdatePeriod);
        nbt.putInt("maxInventoriesPerPlayer", maxInventoriesPerPlayer);
        nbt.putBoolean("requirePermissionToCache", requirePermissionToCache);
        return nbt;
    }

    public static PersistentState.Type<RecovInventory> getPersistentStateType() {
        return new PersistentState.Type<>(RecovInventory::new, RecovInventory::new, null);
    }

    public int getTickUpdatePeriod() {
        return tickUpdatePeriod;
    }

    public void setMaxInventoriesPerPlayer(int maxInventoriesPerPlayer) {
        this.maxInventoriesPerPlayer = maxInventoriesPerPlayer;
        for (Map.Entry<UUID, ArrayDeque<StoredInventory>> entry : inventories.entrySet()) {
            ArrayDeque<StoredInventory> queue = entry.getValue();
            if (queue.size() <= maxInventoriesPerPlayer) {
                continue;
            }
            LocalDateTime firstTime = queue.getFirst().timeCreated;
            oldestInventoriesPerPlayer.computeIfPresent(firstTime, (k, v) -> {
                v.remove(entry.getKey());
                return v.isEmpty() ? null : v;
            });
            while (queue.size() > maxInventoriesPerPlayer) {
                queue.removeFirst();
            }
            cacheIdWithTime(entry.getKey(), queue.getFirst().timeCreated);
        }
    }

    public void setTickUpdatePeriod(int tickUpdatePeriod) {
        if (tickUpdatePeriod != this.tickUpdatePeriod) {
            this.tickUpdatePeriod = tickUpdatePeriod;
            markDirty();
        }
    }
}
