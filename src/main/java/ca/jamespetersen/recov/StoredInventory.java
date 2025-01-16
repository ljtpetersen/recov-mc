package ca.jamespetersen.recov;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

import java.time.LocalDateTime;
import java.util.List;

public class StoredInventory {
    public final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
    public final LocalDateTime timeCreated;

    public StoredInventory(PlayerInventory inventory) {
        for (int i = 0; i < 36; ++i)
            main.set(i, inventory.main.get(i).copy());
        for (int i = 0; i < 4; ++i)
            armor.set(i, inventory.armor.get(i).copy());
        offHand.set(0, inventory.offHand.getFirst().copy());
        timeCreated = LocalDateTime.now();
    }

    public StoredInventory(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        timeCreated = LocalDateTime.parse(nbt.getString("timeCreated"));
        decodeStackList(registries, main, nbt.getList("main", NbtElement.COMPOUND_TYPE));
        decodeStackList(registries, armor, nbt.getList("armor", NbtElement.COMPOUND_TYPE));
        decodeStackList(registries, offHand, nbt.getList("offHand", NbtElement.COMPOUND_TYPE));
    }

    public void restoreInventory(PlayerInventory inventory) {
        ItemStack old_item, new_item;
        for (int j = 0; j < 36; ++j) {
            old_item = inventory.main.get(j);
            new_item = main.get(j);
            if (!old_item.isEmpty() && !new_item.isEmpty()) {
                inventory.player.dropItem(old_item, true, false);
            }
            if (!new_item.isEmpty()) {
                inventory.main.set(j, new_item.copy());
            }
        }
        for (int j = 0; j < 4; ++j) {
            old_item = inventory.armor.get(j);
            new_item = armor.get(j);
            if (!old_item.isEmpty() && !new_item.isEmpty()) {
                inventory.player.dropItem(old_item, true, false);
            }
            if (!new_item.isEmpty()) {
                inventory.armor.set(j, new_item.copy());
            }
        }
        old_item = inventory.offHand.getFirst();
        new_item = offHand.getFirst();
        if (!old_item.isEmpty() && !new_item.isEmpty()) {
            inventory.player.dropItem(old_item, true, false);
        }
        if (!new_item.isEmpty()) {
            inventory.offHand.set(0, new_item.copy());
        }
    }

    public NbtCompound encode(RegistryWrapper.WrapperLookup registries) {
        NbtCompound ret = new NbtCompound();
        ret.put("main", encodeItemStackList(registries, main));
        ret.put("armor", encodeItemStackList(registries, armor));
        ret.put("offHand", encodeItemStackList(registries, offHand));
        ret.putString("timeCreated", timeCreated.toString());
        return ret;
    }

    private void decodeStackList(RegistryWrapper.WrapperLookup registries, List<ItemStack> list, NbtList nbtList) {
        for (int i = 0; i < list.size(); ++i) {
            NbtCompound val = nbtList.getCompound(i);
            if (val.isEmpty()) {
                list.set(i, ItemStack.EMPTY);
            } else {
                list.set(i, ItemStack.fromNbt(registries, nbtList.get(i)).orElse(ItemStack.EMPTY));
            }
        }
    }

    private NbtList encodeItemStackList(RegistryWrapper.WrapperLookup registries, List<ItemStack> list) {
        NbtList ret = new NbtList();
        for (ItemStack stack : list) {
            if (stack.isEmpty()) {
                ret.add(new NbtCompound());
            } else {
                ret.add(stack.toNbt(registries));
            }
        }
        return ret;
    }

    public boolean isEmpty() {
        for (ItemStack stack : main) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        for (ItemStack stack : armor) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        for (ItemStack stack : offHand) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
