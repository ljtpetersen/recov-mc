This fabric mod is meant to be installed on the server only. It will detect player deaths and cache their inventories for some period of time. During this period, a server administrator can easily restore the items to the player.

The plugin is interacted with entirely through a command API. The actions are all subcommands of the `recov` command.

The commands are listed below.
```
/recov list [<player>]
/recov clear [<player>]
/recov restore <player> [<deathNumber>] [<sourcePlayer>]
/recov printDeathInventory <player> [<deathNumber>]
/recov setTicksUntilExpired <ticksUntilExpired>
/recov setTickUpdatePeriod <tickUpdatePeriod>
/recov setMaxInventoriesPerPlayer <maxInventoriesPerPlayer>
/recov setRequirePermissionToCache <requirePermissionToCache>
```

The `list` command, with no arguments passed, will list players who have stored inventories, including offline players. When passing a player's username, it will list the death inventories of that player.

The `clear` command, with zero parameters,
will clear all death inventories. When passing a player's username, it will only clear that player's death inventories.

The `restore` command will restore a player's inventory. The `deathNumber` field specifies which death to restore from. If you want to restore the death inventory of one player to a different one, you can use the `sourcePlayer` parameter.

The `printDeathInventory` command will print the items in a player's inventory at the time of their death.

The `setTicksUntilExpired` will set the amount of ticks the mod will wait before deleting a player death inventory entry.
By default, this is set to `48000`.

The `setTickUpdatePeriod` will set the amount of time the mod will wait before checking if there are any expired death inventories to remove.
By default, this is set to `8000`.

The `setMaxInventoriesPerPlayer` will set the maximum inventories per player. If any more deaths occur, the oldest inventories are deleted early.
By default, this is set to `10`.

The `setRequirePermissionToCache` will set whether the plugin will check if a player has the `recov.cacheinventory` permission before caching their inventory on death.
By default, this is set to `false`.

Players with permission level of at least 3 have access to all `recov` commands, but they do not have the `recov.cacheinventory` permission.
If you are using a permissions mod, the following permissions correspond to
each command.

| Permission                                   | Description                                                                                                                                                                     |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `recov.command.recov`                        | Required to use any `recov` subcommand.                                                                                                                                         |
| `recov.command.recov.list`                   | Required to use the `list` subcommand.                                                                                                                                          |
| `recov.command.recov.clear`                  | Required to use the `clear` subcommand.                                                                                                                                         |
| `recov.command.recov.restore`                | Required to use the `restore` subcommand.                                                                                                                                       |
| `recov.command.recov.restore.separateSource` | Required to use the `restore` subcommand with a specified source player.                                                                                                        |
| `recov.command.recov.printDeathInventory`    | Required to use the `printDeathInventory` subcommand.                                                                                                                           |
| `recov.command.recov.configure`              | Required to use any of the configuration subcommands, which are `setTicksUntilExpired`, `setTickUpdatePeriod`, `setMaxInventoriesPerPlayer`, and `setRequirePermissionToCache`. |
| `recov.cacheinventory`                       | Required to have inventory cached if `requirePermissionToCache` is set to `true`.                                                                                               |
