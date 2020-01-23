package com.creativemd.littletiles.server.world;

import com.creativemd.creativecore.common.world.CreativeWorld;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.world.LittleAnimationHandler;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

public class LittleAnimationHandlerServer extends LittleAnimationHandler {
	
	public LittleAnimationHandlerServer(World world) {
		super(world);
	}
	
	@SubscribeEvent
	public void tick(WorldTickEvent event) {
		if (event.phase == Phase.END && world == event.world) {
			openDoors.addAll(toBeAdded);
			toBeAdded.clear();
			
			isTicking = true;
			for (EntityAnimation door : openDoors) {
				
				if (door.world != world || door.world instanceof CreativeWorld)
					continue;
				
				door.onUpdateForReal();
			}
			
			openDoors.removeIf((x) -> {
				if (x.isDead) {
					x.markRemoved();
					return true;
				}
				return false;
			});
			
			isTicking = false;
		}
	}
	
}