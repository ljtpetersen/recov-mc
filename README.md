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
```

The `list` command, with no arguments passed, will list players who have stored inventories, including offline players. When passing a player's username, it will list the death inventories of that player.

The `clear` command, with zero parameters,
will clear all death inventories. When passing a player's username, it will only clear that player's death inventories.

The `restore` command will restore a player's inventory. The `deathNumber` field specifies which death to restore from. If you want to restore the death inventory of one player to a different one, you can use the `sourcePlayer` parameter.

The `printDeathInventory` command will print the items in a player's inventory at the time of their death.

The `setTicksUntilExpired` will set the amount of ticks the mod will wait before deleting a player death inventory entry.

The `setTickUpdatePeriod` will set the amount of time the mod will wait before checking if there are any expired death inventories to remove.

The `setMaxInventoriesPerPlayer` will set the maximum inventories per player. If any more deaths occur, the oldest inventories are deleted early.
