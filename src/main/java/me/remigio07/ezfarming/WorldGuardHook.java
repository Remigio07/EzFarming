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

import org.bukkit.Location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardHook {
	
	private static WorldGuardHook instance;
	private BooleanFlag flag;
	
	public WorldGuardHook() {
		instance = this;
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		
		try {
			BooleanFlag flag = new BooleanFlag("ezfarming");
			
			registry.register(flag);
			
			this.flag = flag;
		} catch (FlagConflictException e) {
			EzFarming.log("FlagConflictException occurred while registering the \"ezfarming\" custom WorldGuard flag: " + e.getMessage(), 2);
		}
	}
	
	public BooleanFlag getFlag() {
		return flag;
	}
	
	public boolean isInsideEzFarmingRegion(Location location) {
		if (flag != null) {
			BlockVector3 vector = BukkitAdapter.asBlockVector(location);
			
			for (ProtectedRegion region : WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld())).getApplicableRegions(vector)) {
				Boolean flag = region.getFlag(this.flag);
				
				if (flag != null && flag == Boolean.TRUE)
					return true;
			}
		} return false;
	}
	
	public static WorldGuardHook getInstance() {
		return instance;
	}
	
}
