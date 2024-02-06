/*
 * 	EzFarming - Small plugin that speeds up crops harvesting and trees cutting inside of configured WorldGuard regions.
 * 	Copyright 2024  Remigio07
 * 	
 * 	Licensed under the Apache License, Version 2.0 (the "License");
 * 	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 * 	
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 	
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 * 	
 * 	<https://github.com/Remigio07/EzFarming>
 */

package me.remigio07.ezfarming;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

public class EzFarming extends JavaPlugin {
	
	public static final String VERSION;
	public static final Map<Material, Color> CROPS = Map.of(
			Material.BEETROOTS, Color.fromRGB(180, 71, 75),
			Material.CARROTS, Color.fromRGB(252, 140, 9),
			Material.NETHER_WART, Color.fromRGB(188, 62, 73),
			Material.POTATOES, Color.fromRGB(214, 168, 80),
			Material.WHEAT, Color.fromRGB(217, 185, 100)
			);
	public static final Map<TreeType, Material> TREES = Map.of(
			TreeType.TREE, Material.OAK_SAPLING,
			TreeType.BIG_TREE, Material.OAK_SAPLING,
			TreeType.BIRCH, Material.BIRCH_SAPLING,
			TreeType.TALL_BIRCH, Material.BIRCH_SAPLING,
			TreeType.SMALL_JUNGLE, Material.JUNGLE_SAPLING,
			TreeType.ACACIA, Material.ACACIA_SAPLING,
			TreeType.COCOA_TREE, Material.JUNGLE_SAPLING,
			TreeType.REDWOOD, Material.SPRUCE_SAPLING,
			TreeType.CHERRY, Material.CHERRY_SAPLING
			);
	public static final List<Material> BREAKABLE_MATERIALS = Stream.concat(
			CROPS.keySet().stream(),
			Arrays.asList(Material.OAK_LOG, Material.BIRCH_LOG, Material.ACACIA_LOG, Material.CHERRY_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.SPRUCE_LOG).stream()
			).toList();
	private static EzFarming instance;
	private static Logger logger;
	private static BukkitScheduler scheduler = Bukkit.getScheduler();
	private static final String[] SUBCOMMANDS = new String[] { "bypass", "reload", "version", "addsprinkler", "removesprinkler" };
	private List<UUID> bypassPlayers = new ArrayList<>();
	private Map<String, ArmorStand> sprinklers = new HashMap<>();
	private Map<String, Integer> sprinklersTasks = new HashMap<>();
	private ItemStack sprinklerSkull = new ItemStack(Material.PLAYER_HEAD);
	private EventsListener eventsListener = new EventsListener();
	
	static {
		try (Scanner scanner = new Scanner(EzFarming.class.getResourceAsStream("/plugin.yml"), "UTF-8")) {
			scanner.nextLine();
			scanner.nextLine();
			
			String version = scanner.nextLine();
			VERSION = version.substring(10, version.indexOf('\'', 10));
		}
	}
	
	@Override
	public void onLoad() {
		instance = this;
		logger = getLogger();
		
		new WorldGuardHook();
	}
	
	@Override
	public void onEnable() {
		FileConfiguration config = getConfig();
		SkullMeta meta = (SkullMeta) sprinklerSkull.getItemMeta();
		PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "sprinkler");
		
		config.addDefault("messages.prefix", "&8[&5&lEz&f&lFarming&8]");
		config.addDefault("messages.no-permission", "{pfx} &cYou do not have the permission to execute this command.");
		config.addDefault("messages.only-players", "{pfx} &cOnly players can execute this command.");
		config.addDefault("messages.wrong-syntax", "{pfx} &cThe syntax is wrong. Usage: &f{0}&c.");
		config.addDefault("messages.reload", "{pfx} &aEzFarming has been reloaded. Took &f{0} ms &ato complete.");
		config.addDefault("messages.version", "{pfx} &aRunning &5&lEz&f&lFarming &fv{0} &aby &9Remigio07&a.");
		config.addDefault("messages.bypass.enabled", "{pfx} &aBypass mode enabled.");
		config.addDefault("messages.bypass.disabled", "{pfx} &aBypass mode disabled.");
		config.addDefault("messages.addsprinkler.added", "{pfx} &aSprinkler added successfully.");
		config.addDefault("messages.addsprinkler.already-exists", "{pfx} &cA sprinkler at that location already exists.");
		config.addDefault("messages.removesprinkler.removed", "{pfx} &aSprinkler removed successfully.");
		config.addDefault("messages.removesprinkler.not-found", "{pfx} &cNo sprinklers found in a radius of &f{0} &cblocks.");
		config.addDefault("sprinklers.0.world", Bukkit.getWorlds().get(0).getName());
		config.addDefault("sprinklers.0.x", 0);
		config.addDefault("sprinklers.0.y", 0);
		config.addDefault("sprinklers.0.z", 0);
		config.options().copyDefaults(true);
		saveConfig();
		reloadConfig();
		
		try {
			profile.getTextures().setSkin(new URL("http://textures.minecraft.net/texture/d6b13d69d1929dcf8edf99f3901415217c6a567d3a6ead12f75a4de3ed835e85"));
		} catch (MalformedURLException e) {
			e.printStackTrace(); // never called
		} meta.setOwnerProfile(profile);
		sprinklerSkull.setItemMeta(meta);
		loadSprinklers();
		Bukkit.getPluginManager().registerEvents(eventsListener, this);
		getCommand("ezfarming").setExecutor(this);
	}
	
	@Override
	public void onDisable() {
		unloadSprinklers();
		HandlerList.unregisterAll(this);
		scheduler.cancelTasks(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender.hasPermission("ezfarming.admin")) {
			if (args.length == 1) {
				switch (args[0]) {
				case "bypass":
					if (sender instanceof Player) {
						Player player = (Player) sender;
						
						if (bypassPlayers.contains(player.getUniqueId())) {
							bypassPlayers.remove(player.getUniqueId());
							sender.sendMessage(getMessage("messages.bypass.disabled"));
						} else {
							bypassPlayers.add(player.getUniqueId());
							sender.sendMessage(getMessage("messages.bypass.enabled"));
						}
					} else sender.sendMessage(getMessage("messages.only-players"));
					return true;
				case "reload":
				case "rl":
					long ms = System.currentTimeMillis();
					
					reloadConfig();
					reloadSprinklers();
					sender.sendMessage(getMessage("messages.reload", System.currentTimeMillis() - ms));
					return true;
				case "version":
				case "ver":
					sender.sendMessage(getMessage("messages.version", VERSION));
					return true;
				case "addsprinkler":
				case "add":
					if (sender instanceof Player) {
						Player player = (Player) sender;
						String uuid = UUID.randomUUID().toString();
						
						try {
							addSprinkler(uuid, getCenter(player.getLocation().getBlock()));
							
							FileConfiguration config = getConfig();
							Location location = player.getLocation();
							
							config.set("sprinklers." + uuid + ".world", player.getWorld().getName());
							config.set("sprinklers." + uuid + ".x", location.getBlockX());
							config.set("sprinklers." + uuid + ".y", location.getBlockY());
							config.set("sprinklers." + uuid + ".z", location.getBlockZ());
							saveConfig();
							sender.sendMessage(getMessage("messages.addsprinkler.added"));
						} catch (IllegalArgumentException e) {
							sender.sendMessage(getMessage("messages.addsprinkler.already-exists"));
						}
					} else sender.sendMessage(getMessage("messages.only-players"));
					return true;
				case "removesprinkler":
				case "remove":
					if (sender instanceof Player) {
						Player player = (Player) sender;
						Entry<String, ArmorStand> nearest = null;
						
						for (Entity entity : player.getNearbyEntities(3, 3, 3))
							for (Entry<String, ArmorStand> sprinkler : sprinklers.entrySet())
								if (entity.equals(sprinkler.getValue()) && sprinkler.getValue().getWorld().equals(player.getWorld()) && (nearest == null || nearest.getValue().getLocation().distance(player.getLocation()) > entity.getLocation().distance(player.getLocation())))
									nearest = sprinkler;
						if (nearest != null) {
							String id = nearest.getKey();
							FileConfiguration config = getConfig();
							
							config.set("sprinklers." + id, null);
							removeEntity(nearest.getValue());
							scheduler.cancelTask(sprinklersTasks.remove(id));
							sprinklers.remove(id);
							saveConfig();
							sender.sendMessage(getMessage("messages.removesprinkler.removed"));
						} else sender.sendMessage(getMessage("messages.removesprinkler.not-found", 3));
					} else sender.sendMessage(getMessage("messages.only-players"));
					return true;
				}
			} sender.sendMessage(getMessage("messages.wrong-syntax", "/ezfarming <bypass|reload|version|addsprinkler|removesprinkler>"));
		} else sender.sendMessage(getMessage("messages.no-permission"));
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return args.length == 1 ? Arrays.asList(SUBCOMMANDS).stream().filter(subcommand -> subcommand.startsWith(args[0].toLowerCase())).collect(Collectors.toList()) : Collections.emptyList();
	}
	
	public static EzFarming getInstance() {
		return instance;
	}
	
	public List<UUID> getBypassPlayers() {
		return bypassPlayers;
	}
	
	// throws IAE if there's already a sprinkler at that block location or with the same ID
	public ArmorStand addSprinkler(String id, Location location) throws IllegalArgumentException {
		if (sprinklers.keySet().contains(id))
			throw new IllegalArgumentException("A sprinkler with ID \"" + id + "\" was specified twice.");
		World world = location.getWorld();
		
		for (ArmorStand sprinkler : sprinklers.values()) {
			Location otherLocation = sprinkler.getLocation();
			
			if (otherLocation.getWorld().equals(location.getWorld()) && otherLocation.getBlockX() == location.getBlockX() && otherLocation.getBlockY() == location.getBlockY() && otherLocation.getBlockZ() == location.getBlockZ())
				throw new IllegalArgumentException("A sprinkler at location (" + world.getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ() + ") was specified twice.");
		} Chunk chunk = location.getChunk();
		boolean chunkLoaded = chunk.isLoaded();
		
		if (!chunkLoaded)
			chunk.load();
		world.getNearbyEntities(BoundingBox.of(location.getBlock()), entity -> (entity instanceof ArmorStand)).forEach(EzFarming::removeEntity);
		
		if (!chunkLoaded)
			chunk.unload();
		ArmorStand armorStand = world.spawn(location, ArmorStand.class, new Consumer<>() {
			
			@Override
			public void accept(ArmorStand armorStand) {
				Item item = world.dropItem(location, sprinklerSkull);
				
				item.setPickupDelay(Integer.MAX_VALUE);
				item.setInvulnerable(true);
				item.setPersistent(true);
				item.setUnlimitedLifetime(true);
				armorStand.setVisible(false);
				armorStand.setMarker(true);
				armorStand.setInvulnerable(true);
				armorStand.addPassenger(item);
			}
			
		});
		sprinklers.put(id, armorStand);
		sprinklersTasks.put(id, scheduler.runTaskTimerAsynchronously(this, () -> {
			world.spawnParticle(Particle.WATER_SPLASH, location, 10, 0.25D, 0.25D, 0.25D);
			
			List<CropTask> cropTasks = eventsListener.getCropTasks();
			
			if (cropTasks.size() != 0) {
				CropTask firstTask = cropTasks.get(0);
				Location cropLocation = firstTask.getBlock().getLocation();
				ArmorStand nearest = null;
				
				for (ArmorStand sprinkler : sprinklers.values())
					if (sprinkler.getWorld().equals(cropLocation.getWorld()) && (nearest == null || nearest.getLocation().distance(cropLocation) > sprinkler.getLocation().distance(cropLocation)))
						nearest = sprinkler;
				if (armorStand.equals(nearest)) {
					int nodes = (int) (nearest.getLocation().distance(cropLocation) * 2);
					
					for (Location ray : getRay(armorStand.getLocation(), cropLocation, nodes < 2 ? 2 : nodes > 100 ? 100 : nodes))
						world.spawnParticle(Particle.REDSTONE, ray, 1, new DustOptions(CROPS.get(firstTask.getType()), 1F));
					scheduler.runTask(this, firstTask);
					cropTasks.remove(0);
				}
			}
		}, 20L, 20L).getTaskId());
		return armorStand;
	}
	
	private static List<Location> getRay(Location start, Location end, int nodes) {
		if (!start.getWorld().equals(end.getWorld()))
			throw new IllegalArgumentException("Worlds do not match");
		if (nodes < 2)
			throw new IllegalArgumentException("Less than 2 nodes have been specified");
		if (nodes > 100)
			throw new IllegalArgumentException("More than 100 nodes have been specified");
		List<Location> locations = new ArrayList<>();
		double deltaX = Math.abs(start.getX() - end.getX()) / nodes,
				deltaY = Math.abs(start.getY() - end.getY()) / nodes,
				deltaZ = Math.abs(start.getZ() - end.getZ()) / nodes;
		boolean startXLT = start.getX() < end.getX(),
				startYLT = start.getY() < end.getY(),
				startZLT = start.getZ() < end.getZ();
		nodes -= 2;
		
		locations.add(start);
		
		for (int i = 1; i - 1 < nodes; i++)
			locations.add(new Location(
					start.getWorld(),
					start.getX() + i * (startXLT ? deltaX : -deltaX),
					start.getY() + i * (startYLT ? deltaY : -deltaY),
					start.getZ() + i * (startZLT ? deltaZ : -deltaZ)
					));
		locations.add(end);
		return locations;
	}
	
	private static void removeEntity(Entity armorStand) {
		armorStand.getPassengers().forEach(EzFarming::removeEntity);
		armorStand.remove();
	}
	
	public void reloadSprinklers() {
		unloadSprinklers();
		loadSprinklers();
	}
	
	private void loadSprinklers() {
		FileConfiguration config = getConfig();
		
		for (String id : config.getConfigurationSection("sprinklers").getKeys(false)) {
			String worldName = config.getString("sprinklers." + id + ".world", "string_not_found");
			World world = Bukkit.getWorld(worldName);
			
			if (world == null)
				log("Invalid world (\"{0}\") specified at \"sprinklers.{1}.world\". A world with that name does not exist.", 2, worldName, id);
			else try {
				addSprinkler(id, getCenter(new Location(
						world,
						config.getInt("sprinklers." + id + ".x"),
						config.getInt("sprinklers." + id + ".y"),
						config.getInt("sprinklers." + id + ".z")
						).getBlock()));
			} catch (IllegalArgumentException e) {
				log(e.getMessage(), 2);
			}
		}
	}
	
	private void unloadSprinklers() {
		sprinklers.values().forEach(EzFarming::removeEntity);
		sprinklers.clear();
		sprinklersTasks.values().forEach(task -> scheduler.cancelTask(task));
		sprinklersTasks.clear();
	}
	
	public static String getMessage(String path, Object... args) {
		return ChatColor.translateAlternateColorCodes('&', numericPlaceholders(instance.getConfig().getString(path, "string_not_found").replace("{pfx}", instance.getConfig().getString("messages.prefix", "string_not_found")), args));
	}
	
	public static void log(String message, int severity, Object... args) {
		message = numericPlaceholders(message, args);
		
		switch (severity) {
		case 1:
			logger.warning(message);
			message = "[WARN] " + message;
			break;
		case 2:
			logger.severe(message);
			message = "[ERROR] " + message;
			break;
		default:
			logger.info(message);
			message = "[INFO] " + message;
			break;
		}
	}
	
	public static Location getCenter(Block block) {
		return new Location(
				block.getWorld(),
				block.getX() + .5,
				block.getY() + .5,
				block.getZ() + .5
				);
	}
	
	private static String numericPlaceholders(String string, Object... args) {
		for (int i = 0; i < args.length; i++)
			if (string.contains("{" + i + "}"))
				string = string.replace("{" + String.valueOf(i) + "}", String.valueOf(args[i]));
		return string;
	}
	
}
