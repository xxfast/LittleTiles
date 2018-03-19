package com.creativemd.littletiles.common.utils.grid;

import java.util.HashMap;

import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.vec.LittleTileVec;
import com.creativemd.littletiles.common.tiles.vec.LittleUtils;
import com.google.common.math.IntMath;

import net.minecraft.nbt.NBTTagCompound;

public class LittleGridContext {
	
	public static final int overallDefault = 16;
	
	@Deprecated
	public static final int oldHaldGridSize = 8;
	
	public static int[] gridSizes;
	public static int minSize;
	public static int exponent;
	public static int defaultSize;
	public static LittleGridContext[] context;
	
	public static LittleGridContext loadGrid(int min, int defaultGrid, int scale, int exponent)
	{
		minSize = min;
		defaultSize = defaultGrid;
		gridSizes = new int[scale];
		context = new LittleGridContext[scale];
		int size = min;
		for (int i = 0; i < gridSizes.length; i++) {
			gridSizes[i] = size;
			context[i] = new LittleGridContext(size, i);
			size *= exponent;
		}
		
		return get();
	}
	
	public static LittleGridContext get(int size)
	{
		return context[(size/minSize)-1];
	}
	
	public static LittleGridContext get()
	{
		return get(defaultSize);
	}
	
	public static LittleGridContext get(NBTTagCompound nbt)
	{
		 if(nbt.hasKey("grid"))
			 return LittleGridContext.get(nbt.getInteger("grid"));
	     return LittleGridContext.get();
	}
	
	public static LittleGridContext getOverall(NBTTagCompound nbt)
	{
		 if(nbt.hasKey("grid"))
			 return LittleGridContext.get(nbt.getInteger("grid"));
	     return LittleGridContext.get(overallDefault);
	}
	
	
	public static LittleGridContext getMin()
	{
		return get(minSize);
	}
	
	public final int size;
	public final double gridMCLength;
	public final int minPos;
	public final int maxPos;
	public final int maxTilesPerBlock;
	public final double minimumTileSize;
	public final boolean isDefault;
	
	/** doubled **/
	public final LittleTileVec rotationCenter;
	
	public final int[] minSizes;
	
	protected LittleGridContext(int gridSize, int index) {
		size = gridSize;
		gridMCLength = 1D/gridSize;
		minPos = 0;
		maxPos = gridSize;
		maxTilesPerBlock = gridSize*gridSize*gridSize;
		minimumTileSize = 1D/maxTilesPerBlock;
		isDefault = overallDefault == gridSize;
		
		minSizes = new int[size];
		minSizes[0] = 1;
		for (int i = 1; i < minSizes.length; i++) {
			minSizes[i] = size/IntMath.gcd(i, size);
		}
		
		rotationCenter = new LittleTileVec(size, size, size);
	}
	
	public void set(NBTTagCompound nbt)
	{
		if(!isDefault)
    		nbt.setInteger("grid", size);
	}
	
	public void setOverall(NBTTagCompound nbt)
	{
		if(size != overallDefault)
    		nbt.setInteger("grid", size);
	}
	
	public int getMinGrid(int value)
	{
		return minSizes[value % size];
	}
	
	public double toVanillaGrid(double grid)
	{
		return grid * gridMCLength;
	}
	
	public float toVanillaGrid(float grid)
	{
		return (float) (grid * gridMCLength);
	}
	
	public double toVanillaGrid(long grid)
	{
		return grid * gridMCLength;
	}
	
	public double toVanillaGrid(int grid)
	{
		return grid * gridMCLength;
	}
	
	public int toBlockOffset(int grid)
	{
		if(grid > 0)
			return (int) (grid / size);
		return (int) Math.floor(grid /(double)size);
	}
	
	public boolean isAtEdge(double pos)
	{
		return pos % gridMCLength == 0;
	}
	
	public int toGrid(int pos)
	{
		return pos * size;
	}
	
	public long toGridAccurate(double pos)
	{
		pos = LittleUtils.round(pos * size);
		if(pos < 0)
			return (long) Math.floor(pos);
		return (long) pos;
	}
	
	public int toGrid(double pos)
	{
		pos = LittleUtils.round(pos * size);
		if(pos < 0)
			return (int) Math.floor(pos);
		return (int) pos;
	}
	
	public LittleGridContext ensureContext(LittleGridContext context)
	{
		if(context.size > this.size)
			return context;
		return this;
	}
}