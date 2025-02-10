package ca.jamespetersen.recov.mixin;

import ca.jamespetersen.recov.RecovInventory;
import ca.jamespetersen.recov.StoredInventory;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow public abstract PlayerInventory getInventory();

    @Inject(at = @At("HEAD"), method = "dropInventory")
    protected void dropInventory(CallbackInfo ci) {
        StoredInventory storedInventory = new StoredInventory(getInventory());
        RecovInventory.globalRecovInventory.insertInventory((PlayerEntity)(Object)this, storedInventory);
    }
}
