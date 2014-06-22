package net.minecraft.world;

import org.ultramine.server.chunk.ChunkHash;

public class ChunkCoordIntPair
{
	public final int chunkXPos;
	public final int chunkZPos;
	private static final String __OBFID = "CL_00000133";

	public ChunkCoordIntPair(int par1, int par2)
	{
		this.chunkXPos = par1;
		this.chunkZPos = par2;
	}

	public static long chunkXZ2Int(int par0, int par1)
	{
		return (long)par0 & 4294967295L | ((long)par1 & 4294967295L) << 32;
	}

	public int hashCode()
	{
		return ChunkHash.chunkToKey(chunkXPos, chunkZPos);
	}

	public boolean equals(Object par1Obj)
	{
		ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair)par1Obj;
		return chunkcoordintpair.chunkXPos == this.chunkXPos && chunkcoordintpair.chunkZPos == this.chunkZPos;
	}

	public int getCenterXPos()
	{
		return (this.chunkXPos << 4) + 8;
	}

	public int getCenterZPosition()
	{
		return (this.chunkZPos << 4) + 8;
	}

	public ChunkPosition func_151349_a(int p_151349_1_)
	{
		return new ChunkPosition(this.getCenterXPos(), p_151349_1_, this.getCenterZPosition());
	}

	public String toString()
	{
		return "[" + this.chunkXPos + ", " + this.chunkZPos + "]";
	}
}