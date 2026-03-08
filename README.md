# MC Command Framework

# EVEN BIGGER DISCLAIMER:
Everything developed in this branch is still on development! 
I can't assure that this will work on your server and even less be stable. 
I'd recommend using the [master branch](https://github.com/marioogg/command/tree/master) and the stable builds provided [in the wiki](https://github.com/marioogg/command/wiki), although if you want to risk it, at least use [the beta branch](https://github.com/marioogg/command/tree/beta) that at least those features are tested, 
these are untested, purely new-coded features that will receive no support at all!


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
* Well-documented and javadoc'ed annotation command registration
* Flexible and easy-to-use parameter annotation system (`@Param`)
* Also flexible and easy to use flag system (e.g. --silent or -s) (`@Flag`) (to be finished)
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
