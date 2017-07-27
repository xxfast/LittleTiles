package com.creativemd.littletiles.common.tileentity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.creativemd.creativecore.client.rendering.RenderCubeLayerCache;
import com.creativemd.creativecore.common.tileentity.TileEntityCreative;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.common.utils.TickUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.BlockLayerRenderBuffer;
import com.creativemd.littletiles.client.render.LittleChunkDispatcher;
import com.creativemd.littletiles.client.render.RenderingThread;
import com.creativemd.littletiles.common.entity.EntityDoorAnimation;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.vec.LittleTileBox;
import com.creativemd.littletiles.common.tiles.vec.LittleTileSize;
import com.creativemd.littletiles.common.tiles.vec.LittleTileVec;
import com.creativemd.littletiles.common.utils.nbt.LittleNBTCompressionTools;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityLittleTiles extends TileEntityCreative implements ITickable{
	
	public static CopyOnWriteArrayList<LittleTile> createTileList()
	{
		return new CopyOnWriteArrayList<LittleTile>();
	}
	
	public TileEntityLittleTiles() {
		
	}
	
	private CopyOnWriteArrayList<LittleTile> tiles = createTileList();
	
	private CopyOnWriteArrayList<LittleTile> updateTiles = createTileList();
	
	@SideOnly(Side.CLIENT)
	private CopyOnWriteArrayList<LittleTile> renderTiles;
	
	@SideOnly(Side.CLIENT)
	public CopyOnWriteArrayList<LittleTile> getRenderTiles()
	{
		if(renderTiles == null)
			renderTiles = createTileList();
		return renderTiles;
	}
	
	public CopyOnWriteArrayList<LittleTile> getTiles()
	{
		return tiles;
	}
	
	private boolean hasLoaded = false;
	
	public boolean hasLoaded()
	{
		return hasLoaded;
	}
	
	public void setLoaded()
	{
		hasLoaded = true;
	}
	
	/*public void setTiles(CopyOnWriteArrayList<LittleTile> tiles)
	{
		this.tiles = tiles;
	}*/
	
	//@SideOnly(Side.CLIENT)
	//public boolean forceChunkRenderUpdate;
	
	//@SideOnly(Side.CLIENT)
	//public boolean isRendering;
	
	@SideOnly(Side.CLIENT)
	public int renderIndex;
	
	@SideOnly(Side.CLIENT)
	public boolean hasLightChanged;
	
	@SideOnly(Side.CLIENT)
	public boolean hasNeighborChanged;
	
	public int collisionChecks = 0;
	
	public boolean shouldCheckForCollision()
	{
		return collisionChecks > 0;
	}
	
	@SideOnly(Side.CLIENT)
	public EntityDoorAnimation waitingAnimation;
	
	/*@SideOnly(Side.CLIENT)
	private HashMap<BlockRenderLayer, HashMap<EnumFacing, QuadCache[]>> quadCache;
	
	public HashMap<BlockRenderLayer, HashMap<EnumFacing, QuadCache[]>> getRenderCacheQuads()
	{
		if(quadCache == null)
			quadCache = new HashMap<>();
		return quadCache;
	}
	
	@SideOnly(Side.CLIENT)
	public void setQuadCache(QuadCache[] cache, BlockRenderLayer layer, EnumFacing facing)
	{
		HashMap<EnumFacing, QuadCache[]> facingCache = getRenderCacheQuads().get(layer);
		if(facingCache == null)
			facingCache = new HashMap<>();
		facingCache.put(facing, cache);
		getRenderCacheQuads().put(layer, facingCache);
	}
	
	@SideOnly(Side.CLIENT)
	public QuadCache[] getQuadCache(BlockRenderLayer layer, EnumFacing facing)
	{
		HashMap<EnumFacing, QuadCache[]> facingCache = getRenderCacheQuads().get(layer);
		if(facingCache != null)
			return facingCache.get(facing);
		return null;
	}*/
	
	/*@SideOnly(Side.CLIENT)
	private AtomicBoolean hasBeenAddedToBuffer;
	
	public AtomicBoolean getBeenAddedToBuffer()
	{
		if(hasBeenAddedToBuffer == null)
			hasBeenAddedToBuffer = new AtomicBoolean(false);
		return hasBeenAddedToBuffer;
	}*/
	
	@SideOnly(Side.CLIENT)
	public RenderChunk lastRenderedChunk;
	
	@SideOnly(Side.CLIENT)
	public void updateQuadCache(RenderChunk chunk)
	{
		//System.out.println("update cache at pos=" + getPos());
		lastRenderedChunk = chunk;
		
		//getBeenAddedToBuffer().set(false);
		
		if(renderIndex != LittleChunkDispatcher.currentRenderIndex.get())
			getCubeCache().clearCache();
		
		if(waitingAnimation != null && !getCubeCache().doesNeedUpdate())
		{
			waitingAnimation.removeWaitingTe(this);
			waitingAnimation = null;
		}
		
		boolean doesNeedUpdate = getCubeCache().doesNeedUpdate() || hasNeighborChanged || hasLightChanged;
		
		hasLightChanged = false;
		
		if(doesNeedUpdate)
			addToRenderUpdate(); //worldObj.getBlockState(pos).getActualState(worldObj, pos));
		//else if(!rendering.get())
			//RenderUploader.finishChunkUpdateNonThreadSafe(this);
	}
	
	@SideOnly(Side.CLIENT)
	private AtomicReference<BlockLayerRenderBuffer> buffer;
	
	/*@SideOnly(Side.CLIENT)
	private AtomicReference<BlockLayerRenderBuffer> oldBuffer;
	
	public void deleteOldBuffer()
	{
		if(oldBuffer != null)
		{
			oldBuffer.get().deleteBufferData();
			oldBuffer = null;
		}
	}*/
	
	@SideOnly(Side.CLIENT)
	public void setBuffer(BlockLayerRenderBuffer buffer)
	{
		if(this.buffer == null)
			this.buffer = new AtomicReference<BlockLayerRenderBuffer>(buffer);
		else
			this.buffer.set(buffer);
	}
	
	@SideOnly(Side.CLIENT)
	public BlockLayerRenderBuffer getBuffer()
	{
		if(buffer == null)
			buffer = new AtomicReference<>(null);
		return buffer.get();
	}
	
	@SideOnly(Side.CLIENT)
	private RenderCubeLayerCache cubeCache;
	
	public RenderCubeLayerCache getCubeCache()
	{
		if(cubeCache == null)
			cubeCache = new RenderCubeLayerCache();
		return cubeCache;
	}
	
	private boolean removeLittleTile(LittleTile tile)
	{
		boolean result = tiles.remove(tile);
		updateTiles.remove(tile);
		if(isClientSide())
			removeLittleTileClient(tile);
		return result;
	}
	
	@SideOnly(Side.CLIENT)
	private void removeLittleTileClient(LittleTile tile)
	{
		synchronized (getRenderTiles())
		{
			getRenderTiles().remove(tile);
		}
	}
	
	public boolean removeTile(LittleTile tile)
	{
		boolean result = removeLittleTile(tile);
		updateTiles();
		return result;
	}
	
	@SideOnly(Side.CLIENT)
	private void addLittleTileClient(LittleTile tile)
	{
		if(tile.needCustomRendering())
		{
			synchronized (getRenderTiles())
			{
				getRenderTiles().add(tile);
			}
		}
	}
	
	private boolean addLittleTile(LittleTile tile)
	{
		if(isClientSide())
			addLittleTileClient(tile);
		if(tile.shouldTick())
			updateTiles.add(tile);
		return tiles.add(tile);
	}
	
	public void addTiles(ArrayList<LittleTile> tiles)
	{
		for (int i = 0; i < tiles.size(); i++)
			addLittleTile(tiles.get(i));
		updateTiles();
	}
	
	public boolean addTile(LittleTile tile)
	{
		boolean result = addLittleTile(tile);
		updateTiles();
		return result;
	}
	
	public void updateLighting()
	{
		world.checkLight(getPos());
	}
	
	private static Field processingLoadedTiles = ReflectionHelper.findField(World.class, "processingLoadedTiles", "field_147481_N");
	
	private void customTilesUpdate()
	{
		if(updateTiles.isEmpty() == ticking)
		{
			try {
				if(!processingLoadedTiles.getBoolean(world))
				{
					if(updateTiles.isEmpty())
						world.tickableTileEntities.remove(this);
					else
						world.tickableTileEntities.add(this);
					
					ticking = !updateTiles.isEmpty();
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void updateTiles()
	{
		if(preventUpdate)
			return ;
		
		updateCollisionCache();
		
		if(world != null)
		{
			updateBlock();
			updateNeighbor();
			updateLighting();
		}
		if(isClientSide())
			updateCustomRenderer();
		
		customTilesUpdate();
		
		if(!world.isRemote && tiles.size() == 0)
			world.setBlockToAir(getPos());
	}
	
	@SideOnly(Side.CLIENT)
	public void updateCustomRenderer()
	{
		updateRenderBoundingBox();
		updateRenderDistance();
		getCubeCache().clearCache();
		//getBuffer().clear();
		addToRenderUpdate();
		
		//lastRenderedLightValue = 0;
	}
	
	
	@SideOnly(Side.CLIENT)
	public void onNeighBorChangedClient()
	{
		//getBuffer().clear();
		
		addToRenderUpdate();
		hasNeighborChanged = true;
		
		updateRender();
	}
	
	@SideOnly(Side.CLIENT)
	public AtomicBoolean rendering;
	
	@SideOnly(Side.CLIENT)
	public void addToRenderUpdate()
	{
		if(rendering == null)
			rendering = new AtomicBoolean(false);
		if(!rendering.get())
			RenderingThread.addCoordToUpdate(this);
	}
	
	public boolean isBoxFilled(LittleTileBox box)
	{
		LittleTileSize size = box.getSize();
		boolean[][][] filled = new boolean[size.sizeX][size.sizeY][size.sizeZ];
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			for (int j = 0; j < tile.boundingBoxes.size(); j++) {
				LittleTileBox otherBox = tile.boundingBoxes.get(j);
				int minX = Math.max(box.minX, otherBox.minX);
				int maxX = Math.min(box.maxX, otherBox.maxX);
				int minY = Math.max(box.minY, otherBox.minY);
				int maxY = Math.min(box.maxY, otherBox.maxY);
				int minZ = Math.max(box.minZ, otherBox.minZ);
				int maxZ = Math.min(box.maxZ, otherBox.maxZ);
				for (int x = minX; x < maxX; x++) {
					for (int y = minY; y < maxY; y++) {
						for (int z = minZ; z < maxZ; z++) {
							filled[x-box.minX][y-box.minY][z-box.minZ] = true;
						}
					}
				}
			}
		}
		for (int x = 0; x < filled.length; x++) {
			for (int y = 0; y < filled[x].length; y++) {
				for (int z = 0; z < filled[x][y].length; z++) {
					if(!filled[x][y][z])
						return false;
				}
			}
		}
		return true;
	}
	
	public void updateNeighbor()
	{
		for (Iterator iterator = updateTiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			tile.onNeighborChangeInside();
		}
		if(isClientSide())
			hasNeighborChanged = true;
		world.notifyNeighborsOfStateChange(getPos(), LittleTiles.blockTile, true);
	}
	
	@Override
	public boolean shouldRenderInPass(int pass)
    {
        return pass == 0 && getRenderTiles().size() > 0;
    }
	
	@SideOnly(Side.CLIENT)
	private double cachedRenderDistance;
	
	@SideOnly(Side.CLIENT)
	public void updateRenderDistance()
	{
		cachedRenderDistance = 0;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared()
    {
    	if(cachedRenderDistance == 0)
    	{
			double renderDistance = 262144; //512 blocks
			for (Iterator iterator = getRenderTiles().iterator(); iterator.hasNext();) {
				LittleTile tile = (LittleTile) iterator.next();
				renderDistance = Math.max(renderDistance, tile.getMaxRenderDistanceSquared());
			}
			cachedRenderDistance = renderDistance;
    	}
    	return cachedRenderDistance;
    }
	
	@Override
	public boolean hasFastRenderer()
    {
        return false;
    }
		
	@SideOnly(Side.CLIENT)
	private AxisAlignedBB cachedRenderBoundingBox;
	
	@SideOnly(Side.CLIENT)
	private boolean requireRenderingBoundingBoxUpdate;
	
	@SideOnly(Side.CLIENT)
	public void updateRenderBoundingBox()
	{
		//cachedRenderBoundingBox = null;
		requireRenderingBoundingBoxUpdate = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
    	if(requireRenderingBoundingBoxUpdate || cachedRenderBoundingBox == null)
    	{
			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			double minZ = Double.MAX_VALUE;
			double maxX = Double.MIN_VALUE;
			double maxY = Double.MIN_VALUE;
			double maxZ = Double.MIN_VALUE;
			boolean found = false;
			for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
				LittleTile tile = (LittleTile) iterator.next();
				if(tile.needCustomRendering())
				{
					AxisAlignedBB box = tile.getRenderBoundingBox().offset(pos);
					minX = Math.min(box.minX, minX);
					minY = Math.min(box.minY, minY);
					minZ = Math.min(box.minZ, minZ);
					maxX = Math.max(box.maxX, maxX);
					maxY = Math.max(box.maxY, maxY);
					maxZ = Math.max(box.maxZ, maxZ);
					found = true;
				}
			}
			if(found)
				cachedRenderBoundingBox = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
			else
				cachedRenderBoundingBox = new AxisAlignedBB(pos);
			
			requireRenderingBoundingBoxUpdate = false;
    	}
    	return cachedRenderBoundingBox;
    }
	
	//public boolean needFullUpdate = true;

	public boolean preventUpdate = false;
	
	public LittleTile getTileFromPosition(int x, int y, int z)
	{
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			for (int j = 0; j < tile.boundingBoxes.size(); j++) {
				LittleTileBox box = tile.boundingBoxes.get(j);
				if(x >= box.minX && x < box.maxX && y >= box.minY && y < box.maxY && z >= box.minZ && z < box.maxZ)
					return tile;
				
			}
		}
		return null;
	}
	
	/**Used for rendering*/
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(EnumFacing facing, LittleTileBox box, LittleTile rendered)
	{
		for (int littleX = box.minX; littleX < box.maxX; littleX++) {
			for (int littleY = box.minY; littleY < box.maxY; littleY++) {
				for (int littleZ = box.minZ; littleZ < box.maxZ; littleZ++) {
					LittleTile tile = getTileFromPosition(littleX, littleY, littleZ);
					if((tile == null) || (!tile.doesProvideSolidFace(facing) && !tile.canBeRenderCombined(rendered)))
						return true;
				}
			}
		}
		return false;
	}
	
	/**Used for placing a tile and can be used if a "cable" can connect to a direction*/
	public boolean isSpaceForLittleTile(CubeObject cube)
	{
		return isSpaceForLittleTile(cube.getAxis());
	}
	
	/**Used for placing a tile and can be used if a "cable" can connect to a direction*/
	public boolean isSpaceForLittleTile(AxisAlignedBB alignedBB, LittleTile ignoreTile)
	{
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			for (int j = 0; j < tile.boundingBoxes.size(); j++) {
				if(ignoreTile != tile && alignedBB.intersects(tile.boundingBoxes.get(j).getBox()))
					return false;
			}
			
		}
		return true;
	}
	
	/**Used for placing a tile and can be used if a "cable" can connect to a direction*/
	public boolean isSpaceForLittleTile(AxisAlignedBB alignedBB)
	{
		return isSpaceForLittleTile(alignedBB, null);
	}
	
	public boolean isSpaceForLittleTile(LittleTileBox box)
	{
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			for (int j = 0; j < tile.boundingBoxes.size(); j++) {
				if(box.intersectsWith(tile.boundingBoxes.get(j)))
					return false;
			}
			
		}
		return true;
	}
	
	public boolean isSpaceForLittleTile(LittleTileBox box, LittleTile ignoreTile)
	{
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			for (int j = 0; j < tile.boundingBoxes.size(); j++) {
				if(ignoreTile != tile && box.intersectsWith(tile.boundingBoxes.get(j)))
					return false;
			}
			
		}
		return true;
	}
	
	public LittleTile getIntersectingTile(LittleTileBox box, LittleTile ignoreTile)
	{
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			for (int j = 0; j < tile.boundingBoxes.size(); j++) {
				if(ignoreTile != tile && box.intersectsWith(tile.boundingBoxes.get(j)))
					return tile;
			}
			
		}
		return null;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        
        if(tiles != null)
        	tiles.clear();
        if(updateTiles != null)
        	updateTiles.clear();
        collisionChecks = 0;
        preventUpdate = true;
        
        if(nbt.hasKey("tilesCount"))
        {
        
	        int count = nbt.getInteger("tilesCount");
	        for (int i = 0; i < count; i++) {
	        	NBTTagCompound tileNBT = new NBTTagCompound();
	        	tileNBT = nbt.getCompoundTag("t" + i);
				LittleTile tile = LittleTile.CreateandLoadTile(this, world, tileNBT);
				if(tile != null)
					addLittleTile(tile);
	        }
        }else{
        	List<LittleTile> tiles = LittleNBTCompressionTools.readTiles(nbt.getTagList("tiles", 10), this);
        	
        	for (int i = 0; i < tiles.size(); i++) {
				addLittleTile(tiles.get(i));
			}
        }
        
        preventUpdate = false;
        if(world != null)
        {
        	updateBlock();
        	customTilesUpdate();
        }
    }

	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        
    	/*int i = 0;
        for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			NBTTagCompound tileNBT = new NBTTagCompound();
			tile.saveTile(tileNBT);
			nbt.setTag("t" + i, tileNBT);
			i++;
		}
        nbt.setInteger("tilesCount", tiles.size());*/
        
        nbt.setTag("tiles", LittleNBTCompressionTools.writeTiles(tiles));
        
        
		return nbt;
    }
    
    @Override
    public void getDescriptionNBT(NBTTagCompound nbt)
	{
    	int i = 0;
    	for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			NBTTagCompound tileNBT = new NBTTagCompound();
			NBTTagCompound packet = new NBTTagCompound();
			tile.saveTile(tileNBT);
			if(tile.supportsUpdatePacket())
			{
				if(tile.needsFullUpdate)
					tile.needsFullUpdate = false;
				else
					tileNBT.setTag("update", tile.getUpdateNBT());
			}
			
			nbt.setTag("t" + i, tileNBT);
			i++;
		}
        nbt.setInteger("tilesCount", tiles.size());
    }
    
    public LittleTile getTile(LittleTileVec vec)
    {
    	return getTile(vec.x, vec.y, vec.z);
    }
    
    public LittleTile getTile(int minX, int minY, int minZ)
    {
    	for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			if(tile.cornerVec.x == minX && tile.cornerVec.y == minY && tile.cornerVec.z == minZ)
				return tile;
		}
    	return null;
    }
    
    @Override
	@SideOnly(Side.CLIENT)
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
    	NBTTagCompound nbt = pkt.getNbtCompound();
    	ArrayList<LittleTile> exstingTiles = new ArrayList<LittleTile>();
    	ArrayList<LittleTile> tilesToAdd = new ArrayList<LittleTile>();
    	exstingTiles.addAll(tiles);
        int count = nbt.getInteger("tilesCount");
        for (int i = 0; i < count; i++) {
        	NBTTagCompound tileNBT = new NBTTagCompound();
        	tileNBT = nbt.getCompoundTag("t" + i);
        	
        	NBTTagList list = tileNBT.getTagList("boxes", 11);
        	LittleTile tile = null;
        	if(list.tagCount() > 0)
        	{
        		int[] minVec = list.getIntArrayAt(0);
        		tile = getTile(new LittleTileVec(minVec[0], minVec[1], minVec[2]));
        	}
			if(!exstingTiles.contains(tile))
				tile = null;
			
			boolean isIdentical = tile != null ? tile.isIdenticalToNBT(tileNBT) : false;
			if(isIdentical)
			{
				if(tile.supportsUpdatePacket() && tileNBT.hasKey("update"))
					tile.receivePacket(tileNBT.getCompoundTag("update"), net);
				else
					tile.loadTile(this, tileNBT);
				
				exstingTiles.remove(tile);
			}
			else
			{
				if(isIdentical && tile.isLoaded())
					tile.structure.removeTile(tile);
				tile = LittleTile.CreateandLoadTile(this, world, tileNBT);
				if(tile != null)
					tilesToAdd.add(tile);
			}
		}
        
        synchronized (tiles)
        {
        	synchronized(updateTiles)
        	{
		        for (int i = 0; i < exstingTiles.size(); i++) {
		        	if(exstingTiles.get(i).isStructureBlock && exstingTiles.get(i).isLoaded())
		        		exstingTiles.get(i).structure.removeTile(exstingTiles.get(i));
		        	removeLittleTile(exstingTiles.get(i));
				}
		        for (int i = 0; i < tilesToAdd.size(); i++) {
		        	
					addLittleTile(tilesToAdd.get(i));
					if(tilesToAdd.get(i).isStructureBlock)
		        		tilesToAdd.get(i).checkForStructure();
				}
        	}
        }
        
        updateTiles();
        
        super.onDataPacket(net, pkt);
    }
    
    public RayTraceResult getMoving(EntityPlayer player)
    {
    	RayTraceResult hit = null;
		
		Vec3d pos = player.getPositionEyes(TickUtils.getPartialTickTime());
		double d0 = player.capabilities.isCreativeMode ? 5.0F : 4.5F;
		Vec3d look = player.getLook(TickUtils.getPartialTickTime());
		Vec3d vec32 = pos.addVector(look.x * d0, look.y * d0, look.z * d0);
		return getMoving(pos, vec32);
    }
    
    public RayTraceResult getMoving(Vec3d pos, Vec3d look)
    {
    	RayTraceResult hit = null;
    	for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
    		for (int j = 0; j < tile.boundingBoxes.size(); j++) {
    			RayTraceResult Temphit = tile.boundingBoxes.get(j).getBox().offset(getPos()).calculateIntercept(pos, look);
    			if(Temphit != null)
    			{
    				if(hit == null || hit.hitVec.distanceTo(pos) > Temphit.hitVec.distanceTo(pos))
    				{
    					hit = Temphit;
    				}
    			}
			}
		}
		return hit;
    }
	
	public LittleTile getFocusedTile(EntityPlayer player)
	{
		if(!isClientSide())
			return null;
		Vec3d pos = player.getPositionEyes(TickUtils.getPartialTickTime());
		double d0 = player.capabilities.isCreativeMode ? 5.0F : 4.5F;
		Vec3d look = player.getLook(TickUtils.getPartialTickTime());
		Vec3d vec32 = pos.addVector(look.x * d0, look.y * d0, look.z * d0);
		return getFocusedTile(pos, vec32);
	}
	
	public LittleTile getFocusedTile(Vec3d pos, Vec3d look)
	{	
		LittleTile tileFocus = null;
		RayTraceResult hit = null;
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
    		for (int j = 0; j < tile.boundingBoxes.size(); j++) {
    			RayTraceResult Temphit = tile.boundingBoxes.get(j).getBox().offset(getPos()).calculateIntercept(pos, look);
    			if(Temphit != null)
    			{
    				if(hit == null || hit.hitVec.distanceTo(pos) > Temphit.hitVec.distanceTo(pos))
    				{
    					hit = Temphit;
    					tileFocus = tile;
    				}
    			}
			}
		}
		return tileFocus;
	}
	
	@Override
	public void onLoad()
    {
		setLoaded();
		
		customTilesUpdate();
    }
	
	public boolean ticking = true;
	
	@Override
	public void update()
	{
		/*if(isClientSide())
		{
			if(forceChunkRenderUpdate)
			{
				updateRender();
				forceChunkRenderUpdate = false;
			}
		}*/
		
		if(updateTiles.isEmpty())
		{
			System.out.println("Ticking tileentity which shouldn't " + pos);
			return ;
		}
		
		for (Iterator iterator = updateTiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			tile.updateEntity();
		}
		
		/*if(!world.isRemote && tiles.size() == 0)
			world.setBlockToAir(getPos());*/
	}
	
	public void combineTiles(LittleStructure structure) {
		if(!structure.hasLoaded())
			return ;
		
		int size = 0;
		boolean isMainTile = false;
		while(size != tiles.size())
		{
			size = tiles.size();
			int i = 0;
			while(i < tiles.size()){
				if(tiles.get(i).structure != structure)
				{
					i++;
					continue;
				}
				
				int j = 0;
				
				while(j < tiles.size()) {
					if(tiles.get(j).structure != structure)
					{
						j++;
						continue;
					}
					
					if(i != j && tiles.get(i).boundingBoxes.size() == 1 && tiles.get(j).boundingBoxes.size() == 1 && tiles.get(i).canBeCombined(tiles.get(j)) && tiles.get(j).canBeCombined(tiles.get(i)))
					{
						if(tiles.get(i).isMainBlock || tiles.get(j).isMainBlock)
							isMainTile = true;
						LittleTileBox box = tiles.get(i).boundingBoxes.get(0).combineBoxes(tiles.get(j).boundingBoxes.get(0));
						if(box != null)
						{
							tiles.get(i).boundingBoxes.set(0, box);
							tiles.get(i).combineTiles(tiles.get(j));
							tiles.get(i).updateCorner();
							tiles.remove(j);
							if(i > j)
								i--;
							continue;
						}
					}
					j++;
				}
				i++;
			}
		}
		if(isMainTile)
			structure.selectMainTile();
		updateTiles();
	}
	
	public void updateCollisionCache()
	{
		collisionChecks = 0;
		for (Iterator<LittleTile> iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = iterator.next();
			if(tile.shouldCheckForCollision())
				collisionChecks++;
		}
	}
	
	public void combineTiles() {
		combineTilesList(tiles);
		
		updateBlock();
		updateCollisionCache();
	}

	public static void combineTilesList(List<LittleTile> tiles) {
		int size = 0;
		while(size != tiles.size())
		{
			size = tiles.size();
			int i = 0;
			while(i < tiles.size()){
				int j = 0;
				while(j < tiles.size()) {
					if(i != j && tiles.get(i).boundingBoxes.size() == 1 && tiles.get(j).boundingBoxes.size() == 1 && tiles.get(i).canBeCombined(tiles.get(j)) && tiles.get(j).canBeCombined(tiles.get(i)))
					{
						LittleTileBox box = tiles.get(i).boundingBoxes.get(0).combineBoxes(tiles.get(j).boundingBoxes.get(0));
						if(box != null)
						{
							tiles.get(i).boundingBoxes.set(0, box);
							tiles.get(i).combineTiles(tiles.get(j));
							tiles.get(i).updateCorner();
							tiles.remove(j);
							if(i > j)
								i--;
							continue;
						}
					}
					j++;
				}
				i++;
			}
		}
		
	}

	public boolean shouldTick() {
		return !updateTiles.isEmpty();
	}
	
	/*public List<LittleTile> removeBoxFromTiles(LittleTileBox box) {
		preventUpdate = true;
		List<LittleTile> removed = new ArrayList<>();
		for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
			LittleTile tile = (LittleTile) iterator.next();
			if(!tile.isStructureBlock && tile.canBeSplitted())
				removeBoxFromTile(tile, box, removed);
			else{
				tile.destroy();
				removed.add(tile);
			}
		}
		preventUpdate = false;
		combineTiles();
		return removed;
	}

	public void removeBoxFromTile(LittleTile loaded, LittleTileBox box, List<LittleTile> removed) {
		ArrayList<LittleTileBox> boxes = new ArrayList<>(loaded.boundingBoxes);
		ArrayList<LittleTile> newTiles = new ArrayList<>();
		ArrayList<LittleTileBox> removedBoxes = new ArrayList<>();
		boolean isIntersecting = false;
		for (int i = 0; i < boxes.size(); i++) {
			if(box.intersectsWith(boxes.get(i)))
			{
				isIntersecting = true;
				LittleTileBox oldBox = boxes.get(i);
				for (int littleX = oldBox.minX; littleX < oldBox.maxX; littleX++) {
					for (int littleY = oldBox.minY; littleY < oldBox.maxY; littleY++) {
						for (int littleZ = oldBox.minZ; littleZ < oldBox.maxZ; littleZ++) {
							LittleTileVec vec = new LittleTileVec(littleX, littleY, littleZ);
							if(!box.isVecInsideBox(vec)){
								LittleTile newTile = loaded.copy();
								newTile.boundingBoxes.clear();
								newTile.boundingBoxes.add(new LittleTileBox(vec));
								newTiles.add(newTile);
							}else
								removedBoxes.add(new LittleTileBox(vec));
						}
					}
				}
			}
		}
		
		if(isIntersecting)
		{
			LittleTileBox.combineBoxes(removedBoxes);
			LittleTile removedPart = loaded.copy();
			removedPart.boundingBoxes.clear();
			removedPart.boundingBoxes.addAll(removedBoxes);
			removed.add(removedPart);
			
			loaded.destroy();
			
			TileEntityLittleTiles.combineTilesList(newTiles);
			for (int i = 0; i < newTiles.size(); i++) {
				addTile(newTiles.get(i));
			}
		}
	}*/

	
}
