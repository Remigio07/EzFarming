# 🌾 EzFarming
This is a small plugin that speeds up crops harvesting and trees cutting inside of configured regions. A custom WorldGuard flag is used to determine if saplings and crops should be regenerated after their growth.
The plugin is a project for a [private server](https://www.odysseymc.eu/); it will not be uploaded to Bukkit/SpigotMC. Join my [Discord server](https://remigio07.me/discord.gg/CPtysXTfQg) for support.

## Dependencies
[WorldGuard](https://dev.bukkit.org/projects/worldguard) on a Java 17 1.20+ Bukkit environment is required to run the plugin. Older versions may work too, but they have not been tested.

## Setup
A bunch of messages can be configured in the plugin's config.yml file. 

## Permissions
The plugin currently has only one permission node: `ezfarming.admin`. It is used to check if players are allowed to execute the `/ezfarming` command and 

## Building
[Gradle](https://gradle.org) is used to speed up the process of building from source.
To build the project, download the repository using [Git](https://git-scm.com/downloads) or the green "<> Code" button, then open the main folder and run the following command:
```bat
gradlew build
```
You will find the compiled JAR at `EzFarming/build/libs/EzFarming-0.0.1.jar`.
