package org.ultramine.server.chunk;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;

public class ChunkIOExecutor
{
	static final int BASE_THREADS = 1;
	static final int PLAYERS_PER_THREAD = 50;

	private static final AsynchronousExecutor<QueuedChunk, Chunk, IChunkLoadCallback, RuntimeException> instance =
			new AsynchronousExecutor<QueuedChunk, Chunk, IChunkLoadCallback, RuntimeException>(new ChunkIOProvider(), BASE_THREADS);

	// public static void waitForChunkLoad(World world, int x, int z) {
	// instance.get(new QueuedChunk(ChunkHash.chunkToKey(x, z), null, world,
	// null));
	// }

	public static void queueChunkLoad(World world, AnvilChunkLoader loader, ChunkProviderServer provider, int x, int z, IChunkLoadCallback runnable)
	{
		instance.add(new QueuedChunk(ChunkHash.chunkToKey(x, z), loader, world, provider), runnable);
	}

	public static void dropQueuedChunkLoad(net.minecraft.world.World world, int x, int z, IChunkLoadCallback runnable)
	{
		instance.drop(new QueuedChunk(ChunkHash.chunkToKey(x, z), null, world, null), runnable);
	}

	public static void adjustPoolSize(int players)
	{
		// int size = Math.max(BASE_THREADS, (int) Math.ceil(players /
		// PLAYERS_PER_THREAD));
		// instance.setActiveThreads(size);
	}

	public static void tick()
	{
		instance.finishActive();
	}
}
