package ca.jamespetersen.recov;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Recov implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Recov");
    public static final String MOD_ID = "recov";

    @Override
    public void onInitialize() {
        RecovCommand command = new RecovCommand();
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> command.register(dispatcher)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RecovInventory recovInventory = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager().getOrCreate(RecovInventory.getPersistentStateType(), MOD_ID);
            recovInventory.markDirty();
            RecovInventory.globalRecovInventory = recovInventory;
            command.setRecovInventory(recovInventory);
            ServerTickEvents.END_SERVER_TICK.register((world) -> { if (world.getTicks() % recovInventory.getTickUpdatePeriod() == 0) recovInventory.removeOld(); });
            LOGGER.info("Registered events and loaded data.");
        });
        LOGGER.info("Initialized.");
    }
}
