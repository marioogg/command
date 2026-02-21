# MC Command Framework
### DISCLAIMER:
I did not make nor helped in the initial development of this project. All the credit goes to [ashtton](https://github.com/ashtton) and his [original project](https://github.com/ashtton/spigot-command-api).
What I'll actually do is maintain this plugin updated and according to my needs, while keeping it public, as far as providing builds on my Maven repository (check below).
To keep my Java packages ordered, I'll probably rename the packages to `me.marioogg.*` (already done), although this message will stay here because my intention will never be skidding other ones code. If you have any kind of proposal to add a feature, open a [pull request](https://github.com/marioogg/command/pulls) or an [issue](https://github.com/marioogg/command/issues)
## To do
- [x] Add Folia Support
- [x] Add BungeeCord Support
- [x] Add Velocity Support
- [ ] Adding a proper wiki instead of dropping everything on here
### Features
* Creates usage messages for you
* Automatically parses parameters
* Easily register all your commands
* No need for commands in plugin.yml / bungee.yml
* Makes it easier than ever to create commands
* Automatic tab completion
* Tab completion for your custom objects via processors
* Supports Bukkit, Spigot, Paper, Folia, BungeeCord and Velocity
> **NOTE:** <br>
> It's recommended to relocate the library to avoid version conflicts with other plugins that use the framework.
### Parsing
**Bukkit / Spigot / Paper / Folia:**
* **Numbers:** Integer, Long, Double, Float
* **Players:** Player, OfflinePlayer
* **Misc:** World, Boolean, Duration, ChatColor, GameMode
**BungeeCord:**
* **Numbers:** Integer, Long, Double, Float
* **Misc:** Boolean
**Velocity:**
* **Numbers:** Integer, Long, Double, Float
* **Misc:** Boolean
You can also register custom processors for any type — see the processor examples below.
---
### Maven & Gradle Implementation
![Version](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fmaven.marioogg.dev%2Fservice%2Frest%2Fv1%2Fsearch%3Frepository%3Dpublic%26group%3Dme.marioogg%26name%3Dcommand%26sort%3Dversion%26direction%3Ddesc&query=$.items[0].version&label=Nexus&color=0A66C2&style=for-the-badge)

**Maven:** In your *pom.xml*, add the repository and dependency.
> **Important:** Replace `VERSION` with the latest version shown in the badge above.
```xml
<repositories>
    <repository>
        <id>marioogg</id>
        <url>https://maven.marioogg.dev/repository/public/</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>me.marioogg</groupId>
        <artifactId>command</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```
**Gradle:** In your *build.gradle*, add the repository and dependency.
> **Important:** Replace `VERSION` with the latest version shown in the badge above.
```groovy
repositories {
    maven { url 'https://maven.marioogg.dev/repository/public/' }
}
dependencies {
    implementation 'me.marioogg.command:command:VERSION'
}
```
---
## Bukkit / Spigot / Paper / Folia
### Registering Commands
```java
public class MainClass extends JavaPlugin {
    @Override
    public void onEnable() {
        // Recommended — scans the entire package and registers every @Command method found
        CommandHandler.registerCommands("me.marioogg.plugin.commands", this);
        // Register a single class
        CommandHandler.registerCommands(MyCommands.class, this);
        // Register multiple classes at once
        CommandHandler.registerCommands(this, MyCommands.class, AdminCommands.class);
    }
}
```
### @Command Annotation Reference
| Property | Type | Default | Description |
|---|---|---|---|
| `names` | `String[]` | — | One or more aliases. Supports sub-commands via spaces, e.g. `"f create"`. |
| `permission` | `String` | `""` | Required permission node. Leave empty to allow everyone. |
| `description` | `String` | `""` | Short description shown in help messages. |
| `playerOnly` | `boolean` | `false` | Restricts the command to players only. |
| `consoleOnly` | `boolean` | `false` | Restricts the command to console only. |
| `async` | `boolean` | `false` | Runs the command off the main thread. On Folia, routes through `AsyncScheduler` automatically. |
| `allowComplete` | `boolean` | `true` | Whether tab completion is enabled for this command. |
### @Param Annotation Reference
| Property | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | — | Display name used in the auto-generated usage message. |
| `required` | `boolean` | `true` | Whether the argument is mandatory. Optional arguments appear as `[name]` in the usage. |
| `concated` | `boolean` | `false` | Joins all remaining arguments into a single `String`. Shown as `<name..>` in the usage. |
| `defaultValue` | `String` | `""` | Raw value passed through the processor when the argument is omitted. Only applies when `required = false`. |
### Command Examples
```java
public class MyCommands {
    // Basic command — teleports the sender to another player.
    // playerOnly = true restricts it to players. Player-not-found, usage and
    // permission messages are all generated automatically.
    // Usage: /teleport <player>
    @Command(names = {"teleport", "tp"}, permission = "myplugin.teleport", playerOnly = true)
    public void teleportCommand(Player sender, @Param(name = "target") Player target) {
        sender.teleport(target);
        sender.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
    }
    // Multiple aliases — /message, /msg and /tell all map to this method.
    // concated = true joins every word after <player> into one string.
    // Usage: /msg <player> <message..>
    @Command(names = {"message", "msg", "tell"}, playerOnly = true)
    public void messageCommand(Player sender,
                               @Param(name = "player") Player target,
                               @Param(name = "message", concated = true) String message) {
        target.sendMessage(ChatColor.GRAY + "[" + sender.getName() + " -> you] " + message);
        sender.sendMessage(ChatColor.GRAY + "[you -> " + target.getName() + "] " + message);
    }
    // Optional argument — if <target> is omitted, the sender heals themselves.
    // defaultValue is parsed through the normal processor chain.
    // Usage: /heal [target]
    @Command(names = {"heal"}, permission = "myplugin.heal")
    public void healCommand(CommandSender sender,
                            @Param(name = "target", required = false) Player target) {
        Player toHeal = target != null ? target : (Player) sender;
        toHeal.setHealth(toHeal.getMaxHealth());
        sender.sendMessage(ChatColor.GREEN + "Healed " + toHeal.getName() + ".");
    }
    // consoleOnly — only the console may run this command.
    // Usage: /reload
    @Command(names = {"reload"}, permission = "myplugin.reload", consoleOnly = true)
    public void reloadCommand(CommandSender sender) {
        sender.sendMessage("Reloading configuration...");
    }
    // async = true — runs the method off the main thread, safe for blocking I/O.
    // On Folia this is automatically routed through AsyncScheduler.
    // Usage: /lookup <player>
    @Command(names = {"lookup"}, permission = "myplugin.lookup", async = true)
    public void lookupCommand(CommandSender sender, @Param(name = "player") OfflinePlayer target) {
        sender.sendMessage("UUID: " + target.getUniqueId());
    }
    // Sub-commands — "f create" and "faction create" are two aliases for the same method.
    // Usage: /f create <name>  |  /faction create <name>
    @Command(names = {"f create", "faction create"}, permission = "myplugin.faction.create", playerOnly = true)
    public void factionCreateCommand(Player sender, @Param(name = "name") String name) {
        sender.sendMessage(ChatColor.YELLOW + "Faction '" + name + "' created.");
    }
    // allowComplete = false — disables tab completion for this command entirely.
    // Usage: /secret <key>
    @Command(names = {"secret"}, permission = "myplugin.secret", allowComplete = false)
    public void secretCommand(CommandSender sender, @Param(name = "key") String key) {
        sender.sendMessage("Key received.");
    }
}
```
### @Help Example
`@Help` catches commands whose name starts with one of the given prefixes whenever no better-matching `@Command` is found. Ideal for proxy commands like `/f` or `/faction`.
```java
public class FactionCommands {
    @Command(names = {"f create", "faction create"}, permission = "myplugin.faction.create", playerOnly = true)
    public void factionCreateCommand(Player sender, @Param(name = "name") String name) {
        sender.sendMessage(ChatColor.YELLOW + "Faction '" + name + "' created.");
    }
    @Command(names = {"f disband", "faction disband"}, permission = "myplugin.faction.disband", playerOnly = true)
    public void factionDisbandCommand(Player sender) {
        sender.sendMessage(ChatColor.RED + "Your faction has been disbanded.");
    }
    // Shown when /f or /faction is run with no recognisable sub-command.
    // If no @Help is registered, an auto-generated usage message is shown instead.
    @Help(names = {"f", "faction"})
    public void factionHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/f create <name> — Create a faction.");
        sender.sendMessage(ChatColor.YELLOW + "/f disband — Disband your faction.");
    }
}
```
### Custom Processor Example
A `Processor<T>` teaches the framework how to convert a raw `String` argument into any type `T`, and optionally how to tab-complete it. Instantiating the processor registers it automatically — do this before registering commands.
```java
public class MainClass extends JavaPlugin {
    @Override
    public void onEnable() {
        new RankProcessor();
        // Or scan an entire package for processors:
        CommandHandler.registerProcessors("me.marioogg.plugin.processors", this);
        CommandHandler.registerCommands("me.marioogg.plugin.commands", this);
    }
}
public enum Rank { MEMBER, MODERATOR, ADMIN }
public class RankProcessor extends Processor<Rank> {
    // Return null to abort execution; the sender has already been notified.
    @Override
    public Rank process(CommandSender sender, String supplied) {
        try {
            return Rank.valueOf(supplied.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown rank '" + supplied + "'. Valid: MEMBER, MODERATOR, ADMIN.");
            return null;
        }
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String supplied) {
        return Arrays.stream(Rank.values())
                .map(r -> r.name().toLowerCase())
                .filter(name -> name.startsWith(supplied.toLowerCase()))
                .collect(Collectors.toList());
    }
}
// Rank can now be used as a parameter type anywhere:
public class AdminCommands {
    @Command(names = {"setrank"}, permission = "myplugin.setrank")
    public void setRankCommand(CommandSender sender,
                               @Param(name = "player") Player target,
                               @Param(name = "rank") Rank rank) {
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s rank to " + rank.name() + ".");
    }
}
```
---
## BungeeCord
Commands on BungeeCord use the same `@Command`, `@Help` and `@Param` annotations. The key differences are:
- The sender type is `net.md_5.bungee.api.CommandSender`.
- The entry point is `BungeeCommandHandler` instead of `CommandHandler`.
- Custom processors extend `BungeeProcessor<T>` instead of `Processor<T>`.
### Registering Commands
```java
public class MainClass extends Plugin {
    @Override
    public void onEnable() {
        // Recommended — scans the entire package
        BungeeCommandHandler.registerCommands("me.marioogg.plugin.commands", this);
        // Register a single class
        BungeeCommandHandler.registerCommands(MyCommands.class, this);
        // Register multiple classes at once
        BungeeCommandHandler.registerCommands(this, MyCommands.class, AdminCommands.class);
    }
}
```
### Command Examples
```java
public class MyCommands {
    // Basic command available to everyone.
    // Usage: /ping
    @Command(names = {"ping"})
    public void pingCommand(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.GREEN + "Pong!"));
    }
    // Broadcasts a message across the whole network.
    // Usage: /alert <message..>
    @Command(names = {"alert"}, permission = "myplugin.alert")
    public void alertCommand(CommandSender sender,
                             @Param(name = "message", concated = true) String message) {
        ProxyServer.getInstance().broadcast(new TextComponent(ChatColor.RED + "[Alert] " + ChatColor.WHITE + message));
    }
    // Sends a player to a backend server.
    // Usage: /send <player> <server>
    @Command(names = {"send"}, permission = "myplugin.send")
    public void sendCommand(CommandSender sender,
                            @Param(name = "player") String playerName,
                            @Param(name = "server") String serverName) {
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found."));
            return;
        }
        ServerInfo server = ProxyServer.getInstance().getServerInfo(serverName);
        if (server == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Server not found."));
            return;
        }
        target.connect(server);
        sender.sendMessage(new TextComponent(ChatColor.GREEN + "Sent " + target.getName() + " to " + server.getName() + "."));
    }
    // async = true — runs off the BungeeCord main thread.
    // Usage: /lookup <player>
    @Command(names = {"lookup"}, permission = "myplugin.lookup", async = true)
    public void lookupCommand(CommandSender sender, @Param(name = "player") String playerName) {
        sender.sendMessage(new TextComponent("Looking up " + playerName + "..."));
    }
    // Sub-commands work exactly the same as on Bukkit.
    // Usage: /proxy info
    @Command(names = {"proxy info"}, permission = "myplugin.proxy.info")
    public void proxyInfoCommand(CommandSender sender) {
        sender.sendMessage(new TextComponent("Online players: " + ProxyServer.getInstance().getOnlineCount()));
    }
    @Help(names = {"proxy"})
    public void proxyHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/proxy info — Show proxy information."));
    }
}
```
### Custom Processor Example
```java
public class MainClass extends Plugin {
    @Override
    public void onEnable() {
        new ProxiedPlayerProcessor();
        // Or scan an entire package:
        BungeeCommandHandler.registerProcessors("me.marioogg.plugin.processors", this);
        BungeeCommandHandler.registerCommands("me.marioogg.plugin.commands", this);
    }
}
public class ProxiedPlayerProcessor extends BungeeProcessor<ProxiedPlayer> {
    @Override
    public ProxiedPlayer process(CommandSender sender, String supplied) {
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(supplied);
        if (target == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Player '" + supplied + "' is not online."));
            return null;
        }
        return target;
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String supplied) {
        return ProxyServer.getInstance().getPlayers().stream()
                .map(ProxiedPlayer::getName)
                .filter(name -> name.toLowerCase().startsWith(supplied.toLowerCase()))
                .collect(Collectors.toList());
    }
}
// ProxiedPlayer can now be used as a parameter type anywhere:
public class AdminCommands {
    // Optional reason with a default value.
    // Usage: /kick <player> [reason]
    @Command(names = {"kick"}, permission = "myplugin.kick")
    public void kickCommand(CommandSender sender,
                            @Param(name = "player") ProxiedPlayer target,
                            @Param(name = "reason", concated = true, required = false, defaultValue = "Kicked by an administrator.") String reason) {
        target.disconnect(new TextComponent(reason));
        sender.sendMessage(new TextComponent(ChatColor.GREEN + "Kicked " + target.getName() + "."));
    }
}
```
---
## Velocity
Commands on Velocity use the same `@Command`, `@Help` and `@Param` annotations. The key differences are:
- The sender type is `com.velocitypowered.api.command.CommandSource`.
- The entry point is `VelocityCommandHandler`, which requires both the plugin instance and the `ProxyServer` since Velocity plugins do not extend a base class.
- Custom processors extend `VelocityProcessor<T>` instead of `Processor<T>`.
- Messages use the Adventure `Component` API instead of legacy `ChatColor` strings.
### Registering Commands
```java
@Plugin(id = "myplugin", name = "MyPlugin", version = "1.0")
public class MainClass {
    private final ProxyServer server;
    @Inject
    public MainClass(ProxyServer server) {
        this.server = server;
    }
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Recommended — scans the entire package
        VelocityCommandHandler.registerCommands("me.marioogg.plugin.commands", this, server);
        // Register a single class
        VelocityCommandHandler.registerCommands(MyCommands.class, this, server);
        // Register multiple classes at once
        VelocityCommandHandler.registerCommands(this, server, MyCommands.class, AdminCommands.class);
    }
}
```
### Command Examples
```java
public class MyCommands {
    // Basic command available to everyone.
    // Usage: /ping
    @Command(names = {"ping"})
    public void pingCommand(CommandSource source) {
        source.sendMessage(Component.text("Pong!", NamedTextColor.GREEN));
    }
    // Broadcasts a message to all players on the network.
    // Usage: /alert <message..>
    @Command(names = {"alert"}, permission = "myplugin.alert")
    public void alertCommand(CommandSource source,
                             @Param(name = "message", concated = true) String message) {
        VelocityCommandHandler.getProxy().getAllPlayers()
                .forEach(p -> p.sendMessage(Component.text("[Alert] " + message, NamedTextColor.RED)));
        source.sendMessage(Component.text("Alert sent.", NamedTextColor.GREEN));
    }
    // playerOnly restricts this to connected players.
    // Optional <server> argument — defaults to "hub" when omitted.
    // Usage: /hub [server]
    @Command(names = {"hub", "lobby"}, permission = "myplugin.hub", playerOnly = true)
    public void hubCommand(CommandSource source,
                           @Param(name = "server", required = false, defaultValue = "hub") String serverName) {
        Player player = (Player) source;
        VelocityCommandHandler.getProxy().getServer(serverName).ifPresentOrElse(
                server -> player.createConnectionRequest(server).fireAndForget(),
                () -> source.sendMessage(Component.text("Server '" + serverName + "' not found.", NamedTextColor.RED))
        );
    }
    // async = true — runs off the main thread via Velocity's scheduler.
    // Usage: /lookup <player>
    @Command(names = {"lookup"}, permission = "myplugin.lookup", async = true)
    public void lookupCommand(CommandSource source, @Param(name = "player") String playerName) {
        source.sendMessage(Component.text("Looking up " + playerName + "...", NamedTextColor.GRAY));
    }
    // Sub-commands work exactly the same as on Bukkit and BungeeCord.
    // Usage: /proxy info
    @Command(names = {"proxy info"}, permission = "myplugin.proxy.info")
    public void proxyInfoCommand(CommandSource source) {
        int count = VelocityCommandHandler.getProxy().getAllPlayers().size();
        source.sendMessage(Component.text("Online players: " + count, NamedTextColor.AQUA));
    }
    @Help(names = {"proxy"})
    public void proxyHelp(CommandSource source) {
        source.sendMessage(Component.text("/proxy info — Show proxy information.", NamedTextColor.YELLOW));
    }
}
```
### Custom Processor Example
When a processor has constructor arguments, use `super(TheType.class)` so the framework can identify the type it handles without relying on generic type inference.
```java
@Plugin(id = "myplugin", name = "MyPlugin", version = "1.0")
public class MainClass {
    private final ProxyServer server;
    @Inject
    public MainClass(ProxyServer server) {
        this.server = server;
    }
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        new ConnectedPlayerProcessor(server);
        // Or scan an entire package:
        VelocityCommandHandler.registerProcessors("me.marioogg.plugin.processors", this);
        VelocityCommandHandler.registerCommands("me.marioogg.plugin.commands", this, server);
    }
}
public class ConnectedPlayerProcessor extends VelocityProcessor<Player> {
    private final ProxyServer server;
    public ConnectedPlayerProcessor(ProxyServer server) {
        super(Player.class);
        this.server = server;
    }
    @Override
    public Player process(CommandSource source, String supplied) {
        return server.getPlayer(supplied).orElseGet(() -> {
            source.sendMessage(Component.text("Player '" + supplied + "' is not online.", NamedTextColor.RED));
            return null;
        });
    }
    @Override
    public List<String> tabComplete(CommandSource source, String supplied) {
        return server.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(supplied.toLowerCase()))
                .collect(Collectors.toList());
    }
}
// Player can now be used as a parameter type anywhere:
public class AdminCommands {
    // Optional reason with a default value.
    // Usage: /kick <player> [reason]
    @Command(names = {"kick"}, permission = "myplugin.kick")
    public void kickCommand(CommandSource source,
                            @Param(name = "player") Player target,
                            @Param(name = "reason", concated = true, required = false, defaultValue = "Kicked by an administrator.") String reason) {
        target.disconnect(Component.text(reason, NamedTextColor.RED));
        source.sendMessage(Component.text("Kicked " + target.getUsername() + ".", NamedTextColor.GREEN));
    }
}
```
