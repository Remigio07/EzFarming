# 🌾 EzFarming
[![Version](https://img.shields.io/github/v/release/Remigio07/EzFarming?style=plastic&label=version)](/Remigio07/EzFarming/releases)
[![License](https://img.shields.io/github/license/Remigio07/EzFarming?style=plastic)]([https://www.gnu.org/licenses/agpl-3.0.en.html](https://www.apache.org/licenses/LICENSE-2.0.html))
[![Lines of code](https://tokei.rs/b1/github/Remigio07/EzFarming?category=code&color=magenta)](/Remigio07/EzFarming)

EzFarming is a small plugin that speeds up crops harvesting and trees cutting inside of configured regions. A custom WorldGuard flag is used to determine if saplings and crops should be regenerated after their growth.

<details>
  <summary><strong>Showcase</strong></summary>
  <br>
  
  https://github.com/Remigio07/EzFarming/assets/31587616/c26479bb-90c0-43c6-929f-3d753fe7f549
  
  https://github.com/Remigio07/EzFarming/assets/31587616/0595e18d-e7b9-4e21-a86d-ec9d6d085440
</details>

The plugin is a project for a [private server](https://www.odysseymc.eu/); it will not be uploaded to Bukkit/SpigotMC.
If you want to suggest a new feature or improvement, open a PR. Join my [Discord server](https://remigio07.me/discord.gg/CPtysXTfQg) for support.

## Dependencies
[WorldGuard](https://dev.bukkit.org/projects/worldguard) on a Java 17 1.20+ Bukkit environment is required to run the plugin. Older versions may work too, but they have not been tested.

## Setup
A bunch of messages can be configured in the plugin's config.yml file. Italian translations can be found in `src/main/resources/messages-italian.yml`.
Remember to perform a `/ezfarming reload` every time you edit the messages.

### Configuring regions
Create a claimed region using WorldGuard and set its `ezfarming` flag to `true` using `/region flag <region> ezfarming true`, then execute `/region flag <region> block-break allow` to allow breaking crops and wood.

> This flag allows players to break every block inside the region, **but the plugin cancels the event** unless they break one of the following blocks:
> - crops: beetroots, carrots, potatoes, wheat, *nether warts*
> - logs: oak, birch, acacia, cherry, dark oak, jungle, spruce

Because of this, make sure that your region does not contain any of those blocks unless you want players to be able to break them.

### Sprinklers
To create sprinklers to regenerate crops, type `/ezfarming addsprinkler`.
You should have at least one sprinkler for every harvesting area inside the region.

### Trees
Saplings' locations are automatically saved to config when they grow inside of a region. The sapling will be replanted after the first log is removed.
To manually remove a sapling from the config, simply destroy it.

## Permissions
The plugin currently has only one permission node: `ezfarming.admin`. It is used to check if players are allowed to execute the `/ezfarming` command and to check if they are allowed to break blocks when the `block-break` flag is set to `allow`.

## Building
[Gradle](https://gradle.org) is used to speed up the process of building from source.
To build the project, download the repository using [Git](https://git-scm.com/downloads) or the green "<> Code" button, then open the main folder and run the following command:
```bat
gradlew build
```
You will find the compiled JAR at `EzFarming/build/libs/EzFarming-X.x.x.jar`.
