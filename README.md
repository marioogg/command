# MC Command Framework
### DISCLAIMER:
I did not make nor helped in the initial development of this project. All the credit goes to [ashtton](https://github.com/ashtton) and his [original project](https://github.com/ashtton/spigot-command-api).
What I'll actually do is maintain this plugin updated and according to my needs, while keeping it public, as far as providing builds on my Maven repository (check below).
To keep my Java packages ordered, I'll probably rename the packages to `me.marioogg.*` (already in process), although this message will stay here because my intention will never be skidding other ones code. If you have any kind of proposal to add a feature, open a [pull request](https://github.com/marioogg/command/pulls) or an [issue](https://github.com/marioogg/command/issues)

Also, documentation has been moved to [the wiki.](https://github.com/marioogg/command/wiki)

### Features
* Well-documented and javadoc'ed annotation command registration
* Flexible and easy-to-use parameter annotation system (`@Param`)
* Also flexible and easy to use tag system (e.g. --silent or -s) (`@Tag`) (to be finished)
* Automatic tab completion
* Customizable tab completion objects via processors
> **NOTE:** <br>
> It's recommended to relocate the library to avoid version conflicts with other plugins that use the framework.
### Parsing
* **Numbers:** Integer, Long, Double, Float (Bukkit only)
* **Players:** Player, OfflinePlayer (Bukkit only)
* **Misc:** World, Boolean, Duration, ChatColor, GameMode (Bukkit only)
* **Numbers:** Integer, Long, Double, Float
* **Misc:** Boolean
* **Numbers:** Integer, Long, Double, Float
* **Misc:** Boolean
You can also register custom processors for any type — see the processor examples [in the wiki.](https://github.com/marioogg/command/wiki)
---
