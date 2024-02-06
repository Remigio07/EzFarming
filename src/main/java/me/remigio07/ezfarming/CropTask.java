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

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class CropTask implements Runnable {
	
	private Block block;
	private Material type;
	
	public CropTask(Block block, Material type) {
		this.block = block;
		this.type = type;
	}
	
	@Override
	public void run() {
		if (block.getRelative(BlockFace.DOWN).getType() == (type == Material.NETHER_WART ? Material.SOUL_SAND : Material.FARMLAND)) {
			block.setType(type);
			block.getWorld().spawnParticle(Particle.REDSTONE, EzFarming.getCenter(block), 10, 0.25D, 0.25D, 0.25D, new DustOptions(EzFarming.CROPS.get(type), 1F));
		}
	}
	
	public Block getBlock() {
		return block;
	}
	
	public Material getType() {
		return type;
	}
	
}
