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

import org.bukkit.Bukkit;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class EventsListener implements Listener {
	
	private static List<BlockFace> faces = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
	private List<Block> leavesQueue = new ArrayList<>();
	private List<CropTask> cropTasks = new ArrayList<>();
	
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
		
		if (type == null)
			return;
		Block block = event.getLocation().getBlock();
		
		Bukkit.getScheduler().runTask(EzFarming.getInstance(), () -> {
			block.setType(type);
			((Sapling) block.getBlockData()).setStage(1);
		});
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled() || EzFarming.getInstance().getBypassPlayers().contains(event.getPlayer().getUniqueId()) || !WorldGuardHook.getInstance().isInsideEzFarmingRegion(event.getBlock().getLocation()))
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
		} else performLeavesDecay(block);
	}
	
	public void performLeavesDecay(Block block) {
		Material type = block.getType();
		
		if (Tag.LEAVES.isTagged(type) || Tag.LOGS.isTagged(type)) {
			Collections.shuffle(faces);
			
			for (BlockFace face : faces) {
				Block relative = block.getRelative(face);
				
				if (!Tag.LEAVES.isTagged(relative.getType()) || ((Leaves) relative.getBlockData()).isPersistent() || leavesQueue.contains(relative))
					continue;
				Bukkit.getScheduler().runTaskLater(EzFarming.getInstance(), () -> {
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
	
	public List<CropTask> getCropTasks() {
		return cropTasks;
	}
	
	public List<Block> getLeavesQueue() {
		return leavesQueue;
	}
	
}
