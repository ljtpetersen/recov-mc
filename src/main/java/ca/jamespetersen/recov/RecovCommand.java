package ca.jamespetersen.recov;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.*;

public class RecovCommand {
    private RecovInventory recovInventory;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_TIME;

    protected void setRecovInventory(RecovInventory recovInventory) {
        this.recovInventory = recovInventory;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("recov")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("list")
                        .executes(context -> executeList(context.getSource()))
                        .then(argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(recovInventory.getAvailableUsernames(context.getSource()), builder))
                                .executes(context -> executeList(context.getSource(), stringArgumentToGameProfile(context)))))
                .then(literal("clear")
                        .executes(context -> executeClear(context.getSource()))
                        .then(argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(recovInventory.getAvailableUsernames(context.getSource()), builder))
                                .executes(context -> executeClear(context.getSource(), stringArgumentToGameProfile(context)))))
                .then(literal("restore")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> executeRestore(context.getSource(), EntityArgumentType.getPlayer(context, "player"), 0))
                                .then(argument("deathNumber", IntegerArgumentType.integer(0))
                                        .executes(context -> executeRestore(
                                                context.getSource(),
                                                EntityArgumentType.getPlayer(context,"player"),
                                                IntegerArgumentType.getInteger(context, "deathNumber")))
                                        .then(argument("sourcePlayer", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(recovInventory.getAvailableUsernames(context.getSource()), builder))
                                                .executes(context -> executeRestore(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayer(context,"player"),
                                                        IntegerArgumentType.getInteger(context, "deathNumber"),
                                                        stringArgumentToGameProfile(context, "sourcePlayer")))))))
                .then(literal("printDeathInventory")
                        .then(argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(recovInventory.getAvailableUsernames(context.getSource()), builder))
                                .executes(context -> executePrintDeathInventory(context.getSource(), stringArgumentToGameProfile(context), 0))
                                .then(argument("deathNumber", IntegerArgumentType.integer(0))
                                        .executes(context -> executePrintDeathInventory(
                                                context.getSource(),
                                                stringArgumentToGameProfile(context),
                                                IntegerArgumentType.getInteger(context, "deathNumber"))))))
                .then(literal("setTicksUntilExpired")
                        .then(argument("ticksUntilExpired", TimeArgumentType.time(20))
                                .executes(context -> executeSetTicksUntilExpired(context.getSource(), IntegerArgumentType.getInteger(context, "ticksUntilExpired")))))
                .then(literal("setTickUpdatePeriod")
                        .then(argument("tickUpdatePeriod", TimeArgumentType.time(20))
                                .executes(context -> executeSetTickUpdatePeriod(context.getSource(), IntegerArgumentType.getInteger(context, "tickUpdatePeriod")))))
                .then(literal("setMaxInventoriesPerPlayer")
                        .then(argument("maxInventoriesPerPlayer", IntegerArgumentType.integer(1))
                                .executes(context -> executeSetMaxInventoriesPerPlayer(context.getSource(), IntegerArgumentType.getInteger(context, "maxInventoriesPerPlayer")))))
        );
    }

    private GameProfile stringArgumentToGameProfile(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Objects.requireNonNull(context
                        .getSource()
                        .getServer()
                        .getUserCache())
                .findByName(StringArgumentType.getString(context, "player"))
                .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
    }

    private GameProfile stringArgumentToGameProfile(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        return Objects.requireNonNull(context
                        .getSource()
                        .getServer()
                        .getUserCache())
                .findByName(StringArgumentType.getString(context, name))
                .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
    }

    public int executeList(ServerCommandSource source, GameProfile player) {
        Iterator<StoredInventory> it = recovInventory.getStoredInventories(player.getId());
        if (!it.hasNext()) {
            source.sendFeedback(() -> Text.translatable("commands.recov.list.empty", Text.literal(player.getName())), false);
            return 1;
        }
        source.sendFeedback(() -> Text.translatable("commands.recov.list.start", Text.literal(player.getName())), false);
        int i = 0;
        while (it.hasNext()) {
            int j = i++;
            LocalDateTime dateTime = it.next().timeCreated;
            source.sendFeedback(() -> Text.translatable("commands.recov.list.entry", j, DATE_FORMATTER.format(dateTime), TIME_FORMATTER.format(dateTime)), false);
        }
        return 1;
    }

    public int executeList(ServerCommandSource source) {
        recovInventory.getPlayersAndAmounts(source).forEach(pair ->
                source.sendFeedback(() -> Text.translatable("commands.recov.list.player", Text.literal(pair.getLeft()), pair.getRight()), false));
        return 1;
    }

    public int executeRestore(ServerCommandSource source, ServerPlayerEntity player, int index, GameProfile sourcePlayer) throws CommandSyntaxException {
        StoredInventory storedInventory = recovInventory.getStoredInventory(sourcePlayer.getId(), index);
        if (storedInventory == null) {
            throw indexTooLarge(player.getGameProfile().getName(), index).create();
        }
        storedInventory.restoreInventory(player.getInventory());
        source.sendFeedback(() -> Text.translatable("commands.recov.restore.source", Text.literal(player.getGameProfile().getName()), Text.literal(sourcePlayer.getName())), true);
        return 1;
    }

    public int executeRestore(ServerCommandSource source, ServerPlayerEntity player, int index) throws CommandSyntaxException {
        StoredInventory storedInventory = recovInventory.getStoredInventory(player.getGameProfile().getId(), index);
        if (storedInventory == null) {
            throw indexTooLarge(player.getGameProfile().getName(), index).create();
        }
        storedInventory.restoreInventory(player.getInventory());
        source.sendFeedback(() -> Text.translatable("commands.recov.restore", Text.literal(player.getGameProfile().getName())), true);
        return 1;
    }

    public int executePrintDeathInventory(ServerCommandSource source, GameProfile player, int index) throws CommandSyntaxException  {
        StoredInventory storedInventory = recovInventory.getStoredInventory(player.getId(), index);
        if (storedInventory == null) {
            throw indexTooLarge(player.getName(), index).create();
        }
        if (storedInventory.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("commands.recov.printDeathInventory.empty"), false);
            return 1;
        }
        source.sendFeedback(() -> Text.translatable("commands.recov.printDeathInventory.header"), false);
        for (ItemStack stack : storedInventory.main) {
            if (stack.isEmpty()) {
                continue;
            }
            source.sendFeedback(stack::toHoverableText, false);
        }
        for (ItemStack stack : storedInventory.armor) {
            if (stack.isEmpty()) {
                continue;
            }
            source.sendFeedback(stack::toHoverableText, false);
        }
        for (ItemStack stack : storedInventory.offHand) {
            if (stack.isEmpty()) {
                continue;
            }
            source.sendFeedback(stack::toHoverableText, false);
        }
        return 1;
    }

    public int executeClear(ServerCommandSource source) {
        recovInventory.clear();
        source.sendFeedback(() -> Text.translatable("commands.recov.clear.all"), true);
        return 1;
    }

    public int executeClear(ServerCommandSource source, GameProfile player) {
        recovInventory.clear(player.getId());
        source.sendFeedback(() -> Text.translatable("commands.recov.clear.one", Text.literal(player.getName())), true);
        return 1;
    }

    public int executeSetTicksUntilExpired(ServerCommandSource source, int ticks) {
        recovInventory.setTicksUntilExpired(ticks);
        source.sendFeedback(() -> Text.translatable("commands.recov.setTicksUntilExpired", ticks), true);
        return 1;
    }

    public int executeSetTickUpdatePeriod(ServerCommandSource source, int ticks) {
        recovInventory.setTickUpdatePeriod(ticks);
        source.sendFeedback(() -> Text.translatable("commands.recov.setTickUpdatePeriod", ticks), true);
        return 1;
    }

    public int executeSetMaxInventoriesPerPlayer(ServerCommandSource source, int max) {
        recovInventory.setMaxInventoriesPerPlayer(max);
        source.sendFeedback(() -> Text.translatable("commands.recov.setMaxInventoriesPerPlayer", max), true);
        return 1;
    }

    private static SimpleCommandExceptionType indexTooLarge(String name, int index) {
        return new SimpleCommandExceptionType(Text.translatable("commands.recov.indexTooLarge", Text.literal(name), index));
    }
}
