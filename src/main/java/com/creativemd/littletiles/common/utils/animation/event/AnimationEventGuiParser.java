package com.creativemd.littletiles.common.utils.animation.event;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.littletiles.common.tiles.preview.LittlePreviews;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AnimationEventGuiParser<T extends AnimationEvent> {
	
	@SideOnly(Side.CLIENT)
	public abstract void createControls(GuiParent parent, @Nullable T event, LittlePreviews previews);
	
	@SideOnly(Side.CLIENT)
	public abstract void parse(GuiParent parent, T event);
	
}