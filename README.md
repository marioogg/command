# MC Command Framework

### DISCLAIMER:

I did not make nor helped in the initial development of this project. All the credit goes to [ashtton](https://github.com/ashtton) and his [original project](https://github.com/ashtton/spigot-command-api).

What I'll actually do is maintain this plugin updated and according to my needs, while keeping it public, as far as providing builds on my Maven repository (check below).

To keep my Java packages ordered, ~~I'll probably rename the packages to me.marioogg.*~~ (already in process.), although this message will stay here because my intention will never be skidding other ones code. If you have any kind of proposal to add a feature, open a [pull request](https://github.com/marioogg/command/pulls) or an [issue](https://github.com/marioogg/command/issues)


## To do
- [x] Write the press release
- [ ] Update the website
- [ ] Contact the media

### Features
* Creates usage messages for you
* Automatically parses parameters
* Easily register all your commands
* No need for commands in plugin.yml
* Makes it easier than ever to create commands
* Automatic tab completion
* Tab completion for your custom objects via processors
### Parsing
At the moment, this command api will parse the following values for you.\
You can also create custom processors which there is an example of at the bottom of this page.
* **Numbers:** Integer, Long, Double
* **Players:** Player, OfflinePlayer
* **Misc:** World, Boolean, Duration, ChatColor, Gamemode

### Maven & Gradle Implementation
![Version](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fmaven.marioogg.dev%2Fservice%2Frest%2Fv1%2Fsearch%3Frepository%3Dpublic%26group%3Dme.gleeming.command%26name%3Dspigot-command-api%26sort%3Dversion%26direction%3Ddesc&query=$.items[0].version&label=Nexus&color=0A66C2&style=for-the-badge)

**Maven:** In your *pom.xml* file, add the repository and the dependency.
```xml
<repositories>
    <repository>
        <id>marioogg</id>
        <url>https://maven.marioogg.dev/repository/public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>me.marioogg.command</groupId>
        <artifactId>command</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

**Gradle:** In your *build.gradle* file, add the repository and the dependency.
```groovy
repositories {
    maven {
        url 'https://maven.marioogg.dev/repository/public/'
    }
}

dependencies {
    implementation 'me.gleeming.command:spigot-command-api:1.0-SNAPSHOT'
}
```

### Command Example
This example shows you the basics of using the api
```java
// Package: me.gleeming.plugin
public class MainClass extends JavaPlugin {
    public void onEnable() {
        // You initialize all your commands using 
        // the file path to them like this
        CommandHandler.registerCommands("me.gleeming.plugin.commands", this);
        
        // You can also initialize commands using
        // this method, although the above one will
        // register all your commands at once which
        // is not only faster but also cleaner
        CommandHandler.registerCommands(Commands.class, this);
    }
}

// Package: me.gleeming.plugin.commands
public class Commands {
    // All you have to do now is teleport the player
    // The messages like player not found, usage, player only, etc..
    // are handled automatically without the need to worry
    @Command(names = {"teleport"}, permission = "command.teleport", playerOnly = true)
    public void teleportCommand(Player player, @Param(name = "player") Player target) {
        player.teleport(target);
    }
    
    // Concated = true makes it so the rest of the command is 
    // put together automatically making the usage be
    // Command: /msg <player> <reason..>
    @Command(names = {"message", "msg", "tell"}, playerOnly = true)
    public void messageCommand(Player player, @Param(name = "player") Player target, @Param(name = "message", concated = true) String message) {
        target.sendMessage("Player has messaged you " + message);
    }
    
    // You can also make certain things not required like this
    @Command(names = {"eat"}, permission = "command.eat")
    public void eatCommand(CommandSender sender, @Param(name = "target", required = false) Player target) {
        if(target != null) {
            target.setFoodLevel(20);
        } else {
            ((Player) sender).setFoodLevel(20);
        }
    }
}

// Package: me.gleeming.faction
public class FactionCommands {
    @Command(names = {"f create", "faction create"}, playerOnly = true)
    public void factionCreateCommand(Player player, @Param(name = "name") String name) {
        // Faction create logic here
    }
    
    // You can also create help messages
    // by catching different commands.
    // In this example, if a command could
    // not be executed and starts with f
    // then this message would be displayed
    @Help(names = {"f", "faction"})
    public void factionHelp(CommandSender sender) {
        // Faction help message here
        // Remember: If no help messages are found, a custom
        // usage message will be automatically generated
    }
}
```
### Custom Processor Example
This example shows you how you can make a custom processor to parse your custom objects or parse more objects that you feel should have processors that don't already.
```java
// Package: me.gleeming.plugin
public class MainClass extends JavaPlugin {
    public void onEnable() {
        // You initialize all your processors
        // in your on enable like this:
        
        // Make sure you do this before registering your commands
        new CustomEnumProcessor();
        
        // You can initialize all the processors in a path using
        CommandHandler.registerProcessors("me.gleeming.plugin.processors", this);
    }
}

// Package: me.gleeming.plugin.objects
public enum CustomEnum {
    BANANA, HAHA
}

// Package: me.gleeming.plugin.processors
public class CustomEnumProcessor extends Processor<CustomEnum> {
    // This gets the actual value for the command
    public CustomEnum process(CommandSender sender, String supplied) {
        try {
            return CustomEnum.valueOf(supplied);
        } catch(Exception ex) {
            sender.sendMessage(ChatColor.RED + "You have entered an invalid value.");
            return null;
        }
    }
    
    // You can optionally implement tab completions
    public List<String> tabComplete(CommandSender sender, String supplied) {
        return Arrays.stream(CustomEnum.values())
                .map(ce -> ce.name())
                .filter(name -> name.toLowerCase().startsWith(supplied.toLowerCase()))
                .collect(Collectors.toList());
    }
}
```
