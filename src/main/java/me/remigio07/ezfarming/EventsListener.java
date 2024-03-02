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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class EventsListener implements Listener {
	
	private static List<BlockFace> faces = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
	private EzFarming plugin;
	private List<Block> leavesQueue = new ArrayList<>();
	private List<CropTask> cropTasks = new ArrayList<>();
	
	EventsListener(EzFarming plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent event) {
		if (event.isCancelled() || !WorldGuardHook.getInstance().isInsideEzFarmingRegion(event.getBlock().getLocation()))
			return;
		performLeavesDecay(event.getBlock());
	}
	
	@EventHandler
	public void onStructureGrow(StructureGrowEvent event) {
		if (event.isCancelled() || !WorldGuardHook.getInstance().isInsideEzFarmingRegion(event.getLocation()))
			return;
		Material type = EzFarming.TREES.get(event.getSpecies());
		
		if (type == null || plugin.getSapling(event.getLocation().getBlock()) != null)
			return;
		String id = UUID.randomUUID().toString();
		Location location = EzFarming.getCenter(event.getLocation().getBlock());
		SavedSapling sapling = new SavedSapling(id, location, type);
		FileConfiguration config = plugin.getConfig();
		
		config.set("saplings." + id + ".world", location.getWorld().getName());
		config.set("saplings." + id + ".x", location.getBlockX());
		config.set("saplings." + id + ".y", location.getBlockY());
		config.set("saplings." + id + ".z", location.getBlockZ());
		config.set("saplings." + id + ".type", type.name());
		plugin.getSaplings().put(sapling.getID(), sapling);
		plugin.saveConfig();
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled() || plugin.getBypassPlayers().contains(event.getPlayer().getUniqueId()) || !WorldGuardHook.getInstance().isInsideEzFarmingRegion(event.getBlock().getLocation()))
			return;
		Block block = event.getBlock();
		Material type = block.getType();
		
		if (!event.getPlayer().hasPermission("ezfarming.admin") && !EzFarming.BREAKABLE_MATERIALS.contains(type)) {
			event.setCancelled(true);
			return;
		} if (EzFarming.CROPS.keySet().contains(type)) {
			Ageable data = (Ageable) block.getBlockData();
			
			if (data.getAge() == data.getMaximumAge()) {
				cropTasks.add(new CropTask(block, type));
				return;
			} else {
				event.setCancelled(true);
				block.getWorld().spawnParticle(Particle.SMOKE_NORMAL, EzFarming.getCenter(block), 10, 0.25D, 0.25D, 0.25D, 0.1D);
			}
		} else if (EzFarming.TREES.values().contains(type)) {
			SavedSapling sapling = plugin.getSapling(block);
			
			if (sapling != null) {
				plugin.getConfig().set("saplings." + sapling.getID(), null);
				plugin.getSaplings().remove(sapling.getID());
				plugin.saveConfig();
				event.getPlayer().sendMessage(EzFarming.getMessage("messages.sapling-removed"));
			}
		} else if (EzFarming.BREAKABLE_MATERIALS.contains(type)) {
			SavedSapling sapling = plugin.getSapling(block);
			
			if (sapling != null) {
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					block.setType(sapling.getType());
					((Sapling) block.getBlockData()).setStage(1);
					block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, EzFarming.getCenter(block), 10, 0.25D, 0.25D, 0.25D);
				}, 20L);
			}
		} else performLeavesDecay(block);
	}
	
	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		plugin.reloadSprinklers();
		plugin.reloadSaplings();
	}
	
	private void performLeavesDecay(Block block) {
		Material type = block.getType();
		
		if (Tag.LEAVES.isTagged(type) || Tag.LOGS.isTagged(type)) {
			Collections.shuffle(faces);
			
			for (BlockFace face : faces) {
				Block relative = block.getRelative(face);
				
				if (!Tag.LEAVES.isTagged(relative.getType()) || ((Leaves) relative.getBlockData()).isPersistent() || leavesQueue.contains(relative))
					continue;
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					if (leavesQueue.remove(relative) && Tag.LEAVES.isTagged(relative.getType())) {
						Leaves leaves = (Leaves) relative.getBlockData();
						
						if (!leaves.isPersistent() && leaves.getDistance() > 6) {
							LeavesDecayEvent event = new LeavesDecayEvent(relative);
							
							Bukkit.getServer().getPluginManager().callEvent(event);
							
							if (!event.isCancelled()) {
								World world = relative.getWorld();
								
								world.spawnParticle(Particle.BLOCK_DUST, EzFarming.getCenter(relative), 5, 0.25D, 0.25D, 0.25D, leaves);
								world.playSound(relative.getLocation(), Sound.BLOCK_GRASS_BREAK, 1F, 1F);
								relative.breakNaturally();
							}
						}
					}
				}, 5L);
				leavesQueue.add(relative);
			}
		}
	}
	
	public List<Block> getLeavesQueue() {
		return leavesQueue;
	}
	
	public List<CropTask> getCropTasks() {
		return cropTasks;
	}
	
}
