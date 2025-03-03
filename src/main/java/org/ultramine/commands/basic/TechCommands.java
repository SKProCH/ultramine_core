package org.ultramine.commands.basic;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import static net.minecraft.util.EnumChatFormatting.*;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ultramine.commands.Command;
import org.ultramine.commands.CommandContext;
import org.ultramine.server.BackupManager;
import org.ultramine.server.ConfigurationHandler;
import org.ultramine.server.Restarter;
import org.ultramine.server.Teleporter;
import org.ultramine.server.UltramineServerConfig;
import org.ultramine.server.UltramineServerModContainer;
import org.ultramine.server.BackupManager.BackupDescriptor;
import org.ultramine.server.WorldsConfig.WorldConfig.Border;
import org.ultramine.server.chunk.ChunkProfiler;
import org.ultramine.server.chunk.IChunkLoadCallback;
import org.ultramine.server.util.BasicTypeParser;
import org.ultramine.server.world.MultiWorld;
import org.ultramine.server.world.WorldDescriptor;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.functions.GenericIterableFactory;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TechCommands
{
	private static final Logger log = LogManager.getLogger();
	
	@Command(
			name = "id",
			group = "technical",
			permissions = {"command.technical.id"},
			syntax = {"<%id>"}
	)
	public static void id(CommandContext ctx)
	{
		Item item = ctx.get("id").asItem();
		ctx.sendMessage("ID: %s", Item.itemRegistry.getNameForObject(item));
		ctx.sendMessage("Internal ID: %s", Item.itemRegistry.getIDForObject(item));
		ctx.sendMessage("Unlocalized name: %s", new ChatComponentText(item.getUnlocalizedName()));
		ctx.sendMessage("Localized name: %s", new ChatComponentTranslation(item.getUnlocalizedName()+".name"));
		ctx.sendMessage("Is block: %s", item instanceof ItemBlock);
		ctx.sendMessage("Class name: %s", item.getClass().getName());
	}
	
	@Command(
			name = "uptime",
			aliases = {"ticks", "lagometer"},
			group = "technical",
			permissions = {"command.technical.uptime"}
	)
	public static void uptime(CommandContext ctx)
	{
		double tps = Math.round(ctx.getServer().currentTPS*10)/10d;
		double downtime = ctx.getServer().currentWait/1000/1000d;
		double pickdowntime = ctx.getServer().peakWait/1000/1000d;
		int load = (int)Math.round((50-downtime)/50*100);
		int pickload = (int)Math.round((50-pickdowntime)/50*100);
		int uptime = (int)((System.currentTimeMillis() - ctx.getServer().startTime)/1000);
		ChatComponentText pickloadcomp = new ChatComponentText(Integer.toString(pickload).concat("%"));
		pickloadcomp.getChatStyle().setColor(pickload >= 200 ? RED : DARK_GREEN);
		ctx.sendMessage(DARK_GREEN, "command.uptime.msg.up", String.format("%dd %dh %dm %ds", uptime/(60*60*24), uptime/(60*60)%24, uptime/60%60, uptime%60));
		ctx.sendMessage(load > 100 ? RED : DARK_GREEN, "command.uptime.msg.load", Integer.toString(load).concat("%"), pickloadcomp);
		ctx.sendMessage(tps < 15 ? RED : DARK_GREEN, "command.uptime.msg.tps",  tps, Integer.toString((int)(tps/20*100)).concat("%"));
	}
	
	@Command(
			name = "debuginfo",
			group = "technical",
			permissions = {"command.technical.debuginfo"},
			syntax = {
					"",
					"[chunk]",
					"<world>",
					"<player>"
			}
	)
	public static void debuginfo(CommandContext ctx)
	{
		if(ctx.getAction().equals("chunk"))
		{
			EntityPlayerMP player = ctx.getSenderAsPlayer();
			Chunk chunk = player.worldObj.getChunkFromChunkCoords(player.chunkCoordX, player.chunkCoordZ);
			ctx.sendMessage("Chunk: %s %s", chunk.xPosition, chunk.zPosition);
			ctx.sendMessage("TileEntity: %s", chunk.chunkTileEntityMap.size());
			ctx.sendMessage("EntityLiving: %s", chunk.getEntityCount());
			ctx.sendMessage("EntityMonster: %s", chunk.getEntityCountByType(EnumCreatureType.monster));
			ctx.sendMessage("EntityAnimal: %s", chunk.getEntityCountByType(EnumCreatureType.creature));
			ctx.sendMessage("EntityAmbient: %s", chunk.getEntityCountByType(EnumCreatureType.ambient));
			ctx.sendMessage("EntityWater: %s", chunk.getEntityCountByType(EnumCreatureType.waterCreature));
		}
		else if(ctx.contains("world") || ctx.getArgs().length == 0)
		{
			WorldServer world = ctx.contains("world") ? ctx.get("world").asWorld() : ctx.getSenderAsPlayer().getServerForPlayer();
			ctx.sendMessage("World: %s, Dimension: %s", world.getWorldInfo().getWorldName(), world.provider.dimensionId);
			ctx.sendMessage("Chunks loaded:  %s", world.theChunkProviderServer.getLoadedChunkCount());
			ctx.sendMessage("Chunks active:  %s", world.getActiveChunkSetSize());
			ctx.sendMessage("Chunks for unload:  %s", world.theChunkProviderServer.chunksToUnload.size());
			ctx.sendMessage("Players: %s", world.playerEntities.size());
			ctx.sendMessage("Entities:  %s", world.loadedEntityList.size());
			ctx.sendMessage("TileEntities:  %s", world.loadedTileEntityList.size());
		}
		else
		{
			EntityPlayerMP player = ctx.get("player").asPlayer();
			ctx.sendMessage("Username: %s", player.getCommandSenderName());
			ctx.sendMessage("UUID: %s", player.getGameProfile().getId());
			ctx.sendMessage("Location: [%s](%s, %s, %s)", player.dimension,
					MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ));
			ctx.sendMessage("Chunk send rate: %s", player.getChunkMgr().getRate());
			ctx.sendMessage("View distance: %s", player.getRenderDistance());
		}
	}
	
	@Command(
			name = "memstat",
			group = "technical",
			permissions = {"command.technical.memstat"}
	)
	public static void memstat(CommandContext ctx)
	{
		ctx.sendMessage("Heap max: %sm", Runtime.getRuntime().maxMemory() >> 20);
		ctx.sendMessage("Heap total: %sm", Runtime.getRuntime().totalMemory() >> 20);
		ctx.sendMessage("Heap free: %sm", Runtime.getRuntime().freeMemory() >> 20);
	}
	
	@Command(
			name = "multiworld",
			aliases = {"mw", "mv"},
			group = "technical",
			permissions = {"command.technical.multiworld"},
			syntax = {
					"[list]",
					"[load unload hold goto destroy delete] <%world>" //No world validation
			}
	)
	public static void multiworld(CommandContext ctx)
	{
		MultiWorld mw = ctx.getServer().getMultiWorld();
		
		if(ctx.getAction().equals("list"))
		{
			ctx.sendMessage("command.multiworld.list.head");
			for(WorldDescriptor desc : mw.getAllDescs())
			{
				ctx.sendMessage(GOLD, "    - [%s](%s) - %s", desc.getDimension(), desc.getName(), desc.getState());
			}
			return;
		}
		
		WorldDescriptor desc = mw.getDescByNameOrID(ctx.get("world").asString());

		if(desc == null)
			ctx.failure("command.multiworld.notregistered");
		
		if(ctx.getAction().equals("load"))
		{
			if(desc.getState().isLoaded())
				ctx.failure("command.multiworld.alreadyloaded");
			
			desc.forceLoadNow();
			ctx.sendMessage("command.multiworld.load.success");
		}
		else if(ctx.getAction().equals("unload"))
		{
			if(!desc.getState().isLoaded())
				ctx.failure("command.multiworld.notloaded");
			
			desc.unloadNow(true);
			ctx.sendMessage("command.multiworld.unload.success");
		}
		else if(ctx.getAction().equals("hold"))
		{
			desc.holdNow(true);
			ctx.sendMessage("command.multiworld.hold.success");
		}
		else if(ctx.getAction().equals("goto"))
		{
			if(!desc.getState().isLoaded())
				ctx.failure("command.multiworld.notloaded");
			
			WorldServer world = desc.getWorld();
			Teleporter.tpNow(ctx.getSenderAsPlayer(), desc.getDimension(), world.getWorldInfo().getSpawnX(), world.getWorldInfo().getSpawnY(), world.getWorldInfo().getSpawnZ());
		}
		else if(ctx.getAction().equals("destroy"))
		{
			if(!desc.getState().isLoaded())
				ctx.failure("command.multiworld.notloaded");
			
			desc.destroyWorld(true);
			desc.holdNow(true);
			ctx.sendMessage("command.multiworld.destroy.success");
			
		}
		else if(ctx.getAction().equals("delete"))
		{
			desc.deleteLater();
			desc.holdLater(true);
			ctx.sendMessage("command.multiworld.delete.success");
		}
	}

	@Command(
			name = "countentity",
			group = "technical",
			permissions = {"command.technical.countentity"},
			syntax = {"<int%radius>"}
	)
	public static void countentity(CommandContext ctx)
	{
		int radius = ctx.get("radius").asInt();
		EntityPlayerMP player = ctx.getSenderAsPlayer();
		
		double minX = player.posX - radius;
		double minY = player.posY - radius;
		double minZ = player.posZ - radius;
		double maxX = player.posX + radius;
		double maxY = player.posY + radius;
		double maxZ = player.posZ + radius;
		
		int count = 0;
		int itemcount = 0;
		int mobcount = 0;
		
		int monsters = 0;
		int animals = 0;
		int ambient = 0;
		int water = 0;
		
		for(Entity ent : GenericIterableFactory.newCastingIterable(player.worldObj.loadedEntityList, Entity.class))
		{
			if(!ent.isDead && (ent.posX > minX && ent.posX < maxX) && (ent.posY > minY && ent.posY < maxY) && (ent.posZ > minZ && ent.posZ < maxZ))
			{
				count ++;
				if(ent.isEntityLiving() && !ent.isEntityPlayer())
				{
					mobcount++;
					if(ent.isEntityMonster())
						monsters++;
					else if(ent.isEntityAnimal())
						animals++;
					else if(ent.isEntityAmbient())
						ambient++;
					else if(ent.isEntityWater())
						water++;
					
				}
				else if(ent instanceof EntityItem)
				{
					itemcount++;
				}
			}
		}
		ctx.sendMessage("command.countentity.result1", count, mobcount, itemcount);
		ctx.sendMessage("command.countentity.result2", monsters, animals, water, ambient);
	}

	@Command(
			name = "clearentity",
			group = "technical",
			permissions = {"command.technical.clearentity"},
			syntax = {
					"<int%radius>",
					"[all mobs items] <int%radius>"
			}
	)
	public static void clearentity(CommandContext ctx)
	{
		int radius = ctx.get("radius").asInt();
		EntityPlayerMP player = ctx.getSenderAsPlayer();

		boolean items = true;
		boolean mobs = true;
		if(ctx.getAction().equals("mobs"))
			items = false;
		if(ctx.getAction().equals("items"))
			mobs = false;
		
		double minX = player.posX - radius;
		double minY = player.posY - radius;
		double minZ = player.posZ - radius;
		double maxX = player.posX + radius;
		double maxY = player.posY + radius;
		double maxZ = player.posZ + radius;
		
		int count = 0;
		int itemcount = 0;
		int mobcount = 0;
		
		for(Entity ent : GenericIterableFactory.newCastingIterable(player.worldObj.loadedEntityList, Entity.class))
		{
			if(!ent.isDead && (ent.posX > minX && ent.posX < maxX) && (ent.posY > minY && ent.posY < maxY) && (ent.posZ > minZ && ent.posZ < maxZ))
			{
				if(mobs && ent.isEntityLiving() && !ent.isEntityPlayer() && !(ent instanceof EntityVillager))
				{
					count ++;
					mobcount++;
					ent.setDead();
				}
				else if(items && ent instanceof EntityItem)
				{
					count ++;
					itemcount++;
					ent.setDead();
				}
			}
		}
		
		ctx.sendMessage("command.clearentity.success", count, mobcount, itemcount);
	}
	
	private static LagGenerator laghandler;
	
	@Command(
			name = "startlags",
			group = "technical",
			permissions = {"command.technical.startlags"},
			syntax = {
					"<int%percent>",
					"[stop]"
			}
	)
	public static void startlags(CommandContext ctx)
	{
		if(laghandler != null)
		{
			FMLCommonHandler.instance().bus().unregister(laghandler);
			laghandler = null;
		}
		
		if(ctx.getAction().equals("stop"))
		{
			ctx.sendMessage("command.startlags.stop");
			return;
		}
		
		int percent = ctx.get("percent").asInt(1);
		FMLCommonHandler.instance().bus().register(laghandler = new LagGenerator(percent));
		ctx.sendMessage("command.startlags.start", percent);
		
	}
	
	public static class LagGenerator
	{
		private final int percent;
		private int counter;
		
		public LagGenerator(int percent){this.percent = percent;}
		
		@SubscribeEvent
		public void inTick(TickEvent.ServerTickEvent e)
		{
			if(e.phase == TickEvent.Phase.START)
			{
				if(++counter%600 == 0)
					log.warn("Startlags command enabled! It loads server by {}%", percent);
				
				try
				{
					Thread.sleep(percent/2); //100% = 50ms (1 tick)
				} catch(InterruptedException ignored){}
			}
		}
	}
	
	@Command(
			name = "restart",
			group = "technical",
			permissions = {"command.technical.restart"},
			syntax = {
					"[abort]",
					"<time>"
			}
	)
	public static void restart(CommandContext ctx)
	{
		if(ctx.getAction().equals("abort"))
		{
			if(Restarter.abort())
				ctx.broadcast("command.restart.abort.success");
			else
				ctx.sendMessage("command.restart.abort.fail");
		}
		else
		{
			Restarter.restart((int)(ctx.get("time").asTimeMills()/1000));
			ctx.sendMessage("command.restart.success");
		}
	}
	
	@Command(
			name = "javagc",
			group = "technical",
			permissions = {"command.technical.javagc"},
			syntax = {""}
	)
	public static void javagc(CommandContext ctx)
	{
		System.gc();
		ctx.sendMessage("command.javagc.success");
	}
	
	@Command(
			name = "chunkgc",
			group = "technical",
			permissions = {"command.technical.chunkgc"},
			syntax = {""}
	)
	@SideOnly(Side.SERVER)
	public static void chunkgc(CommandContext ctx)
	{
		for(WorldServer world : ctx.getServer().getMultiWorld().getLoadedWorlds())
			world.theChunkProviderServer.getChunkGC().forceCollect();
		ctx.sendMessage("command.chunkgc.success");
	}
	
	private static WorldGenerator worldgen;
	
	@Command(
			name = "genworld",
			group = "technical",
			permissions = {"command.genworld"},
			syntax = {
					"",
					"[stop]",
					"<cpt>",
					"[radius] <radius>",
					"[radius] <radius> <cpt>"
			}
	)
	public static void genworld(CommandContext ctx)
	{
		if(ctx.getAction().equals("stop"))
		{
			FMLCommonHandler.instance().bus().unregister(worldgen);
			ctx.sendMessage("command.genworld.stop", worldgen.genCurrent, worldgen.genTotal);
			worldgen = null;
			return;
		}
		
		if(worldgen != null)
			ctx.failure("command.genworld.already");
		
		WorldServer world = ctx.getSenderAsPlayer().getServerForPlayer();
		int dim = world.provider.dimensionId;
		int radius = ctx.contains("radius") ? ctx.get("radius").asInt(1) : -1;
		int cpt = ctx.contains("cpt") ? ctx.get("cpt").asInt(1) : 20;
		
		int x = MathHelper.floor_double(ctx.getSenderAsPlayer().posX);
		int z = MathHelper.floor_double(ctx.getSenderAsPlayer().posZ);
		
		if(radius == -1 && world.getConfig().borders.length == 0)
			ctx.failure("command.genworld.noborder");
		
		worldgen = radius == -1 ? new WorldGenerator(dim, cpt) : new WorldGenerator(dim, cpt, x, z, radius);
		FMLCommonHandler.instance().bus().register(worldgen);
		ctx.sendMessage("command.genworld.start");
	}
	
	public static class WorldGenerator
	{
		private final int dim;
		private final int chunksPerTick;
		private final boolean isBorder;
		
		private int borderInd = 0;
		private int minX = Integer.MIN_VALUE;
		private int minZ;
		private int maxX;
		private int maxZ;
		
		private int x;
		private int z;
		
		private int genCurrent;
		private int genTotal;
		
		public WorldGenerator(int dim, int chunksPerTick)
		{
			this.isBorder = true;
			this.dim = dim;
			this.chunksPerTick = chunksPerTick;
		}
		
		public WorldGenerator(int dim, int chunksPerTick, int centX, int centZ, int radius)
		{
			this.isBorder = false;
			this.dim = dim;
			this.chunksPerTick = chunksPerTick;
			
			minX = (centX - radius) >> 4;
			minZ = (centZ - radius) >> 4;
			maxX = (centX + radius) >> 4;
			maxZ = (centZ + radius) >> 4;
			
			x = minX;
			z = minZ;
			
			genTotal = (Math.abs(maxX - minX) + 8)*(Math.abs(maxZ - minZ) + 8);
		}
		
		@SubscribeEvent
		public void onTick(TickEvent.ServerTickEvent e)
		{
			if(e.phase == TickEvent.Phase.START)
			{
				if(MinecraftServer.getServer().getTickCounter() % 600 == 0)
					MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("command.genworld.process", genCurrent, genTotal));
				
				if(MinecraftServer.getServer().getTickCounter() % Math.max(1,169/chunksPerTick) != 0)
					return;
				WorldServer world = MinecraftServer.getServer().getMultiWorld().getWorldByID(dim);
				if(world == null)
					return;
				Border[] borders = world.getConfig().borders;
				int counter = 0;
				l1:
				while(borderInd < (isBorder ? borders.length : 1))
				{
					if(minX == Integer.MIN_VALUE)
					{
						Border border = borders[borderInd];
						
						minX = (border.x - border.radius) >> 4;
						minZ = (border.z - border.radius) >> 4;
						maxX = (border.x + border.radius) >> 4;
						maxZ = (border.z + border.radius) >> 4;
						
						x = minX;
						z = minZ;
						
						genTotal = (Math.abs(maxX - minX) + 8)*(Math.abs(maxZ - minZ) + 8);
					}
					
					while(x <= maxX)
					{
						while(z <= maxZ)
						{
							if(world.getBorder().isChunkInsideBorder(x, z))
							{
								if(++counter > Math.max(1, chunksPerTick/169)) break l1;
								
								world.theChunkProviderServer.loadAsyncWithRadius(x, z, 6, IChunkLoadCallback.EMPTY);
							}
							
							z += 8;
						}
						
						x += 8;
						if(x <= maxX) z = minZ;
					}
					
					if(x > maxX && z > maxZ)
					{
						borderInd++;
						minX = Integer.MIN_VALUE;
					}
				}
				
				genCurrent += (counter-1)*81;
				
				if(borderInd >= (isBorder ? borders.length : 1))
				{
					FMLCommonHandler.instance().bus().unregister(worldgen);
					worldgen = null;
					MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("command.genworld.complete", genCurrent, genTotal));
				}
			}
		}
	}
	
	@Command(
			name = "chunkdebug",
			group = "technical",
			permissions = {"command.technical.chunkdebug"},
			syntax = {
					"",
					"[start stop]",
					"[top]",
					"[top] <list average peak>",
					"[top] <list average peak> <%count>"
			}
	)
	public static void chunkdebug(CommandContext ctx)
	{
		if(ctx.getAction().equals("start"))
		{
			ChunkProfiler.instance().setEnabled(true);
			ctx.sendMessage("command.chunkdebug.start");
		}
		else if(ctx.getAction().equals("stop"))
		{
			ChunkProfiler.instance().setEnabled(false);
			ctx.sendMessage("command.chunkdebug.stop");
		}
		else
		{
			String act2 = ctx.contains("list") ? ctx.get("list").asString() : "average";
			int count = ctx.contains("count") ? ctx.get("count").asInt(1) : 9;
			
			if(!ChunkProfiler.instance().isEnabled())
			{
				ctx.sendMessage("command.chunkdebug.notstart");
				ChunkProfiler.instance().setEnabled(true);
				FMLCommonHandler.instance().bus().register(new ChunkDebugDelayedDisplay(ctx, act2.startsWith("p"), count));
			}
			else
			{
				printChunkDebugResults(ctx, act2.startsWith("p"), count);
			}
		}
	}
	
	private static void printChunkDebugResults(CommandContext ctx, boolean peak, int count)
	{
		ChunkProfiler.ChunkData[] results;
		if(!peak)
			results = ChunkProfiler.instance().getAverageTop();
		else
			results = ChunkProfiler.instance().getPeakTop();
		
		ctx.sendMessage("command.chunkdebug.top.head");
		for(int i = 0; i < Math.min(count, results.length); i++)
		{
			ChunkProfiler.ChunkData chunk = results[i];
			ctx.sendMessage(GOLD, "    - [%s](%s, %s) -> %s%% (%s%%)",
					chunk.getDimension(), chunk.getChunkX() << 4, chunk.getChunkZ() << 4, (chunk.getAverage()/5000)/100d, (chunk.getPeak()/5000)/100d);
		}
	}
	
	public static class ChunkDebugDelayedDisplay
	{
		private final CommandContext ctx;
		private final boolean peak;
		private final int count;
		
		private int tickCounter;
		
		public ChunkDebugDelayedDisplay(CommandContext ctx, boolean peak, int count)
		{
			this.ctx = ctx;
			this.peak = peak;
			this.count = count;
		}
		
		@SubscribeEvent
		public void inTick(TickEvent.ServerTickEvent e)
		{
			if(e.phase == TickEvent.Phase.START && ++tickCounter == 20)
			{
				printChunkDebugResults(ctx, peak, count);
				ChunkProfiler.instance().setEnabled(false);
				FMLCommonHandler.instance().bus().unregister(this);
			}
		}
	}
	
	@SideOnly(Side.SERVER)
	@Command(
			name = "backup",
			group = "technical",
			permissions = {"command.technical.backup"},
			syntax = {
					"[make now]",
					"[make now] <%world>...",
					"[list]",
					"[apply] <number>",
					"[apply] <number> <flags>..."
			}
	)
	public static void backup(CommandContext ctx)
	{
		BackupManager mgr = ctx.getServer().getBackupManager();
		MultiWorld mw = ctx.getServer().getMultiWorld();
		if(ctx.getAction().equals("make") || ctx.getAction().equals("now"))
		{
			Collection<String> worlds = ctx.contains("world") ? mw.resolveSaveDirs(Arrays.asList(ctx.get("world").asStringArray())) : mw.getDirsForBackup();
			ctx.check(worlds.size() != 0, "command.backup.make.fail");
			ctx.sendMessage("command.backup.make.started", worlds);
			mgr.backup(worlds);
		}
		else if(ctx.getAction().equals("list"))
		{
			List<BackupDescriptor> list = mgr.getBackupList();
			ctx.sendMessage("command.backup.list.head");
			for(int i = 0, s = list.size(); i < s; i++)
			{
				ctx.sendMessage("    - #%s %s", s-i, list.get(i).getName());
			}
		}
		else if(ctx.getAction().equals("apply"))
		{
			String path = ctx.get("number").asString();
			Map<String, List<String>> flags = ctx.contains("flags") ? ctx.get("flags").asFlags("worlds", "noplayers", "temp", "restart") : new HashMap<String, List<String>>();
			if(BasicTypeParser.isInt(path))
			{
				int num = ctx.get("number").asInt(1);
				List<BackupDescriptor> list = mgr.getBackupList();
				ctx.check(num <= list.size(), "command.backup.apply.fail.none");
				path = list.get(list.size()-num).getName();
			}
			
			if(flags.containsKey("restart"))
				for(EntityPlayerMP player : GenericIterableFactory.newCastingIterable(ctx.getServer().getConfigurationManager().playerEntityList, EntityPlayerMP.class))
					player.playerNetServerHandler.kickPlayerFromServer("\u00a75Выполняется бэкап мира\n\u00a7dВы сможете войти через несколько минут");
			
			mgr.applyBackup(path, ctx, flags.get("worlds"), !flags.containsKey("noplayers"), flags.containsKey("temp"));
			
			if(flags.containsKey("restart"))
				ctx.getServer().initiateShutdown();
		}
	}
	
	@Command(
			name = "recipecache",
			group = "technical",
			permissions = {"command.technical.recipecache"},
			syntax = {"[clear]"}
	)
	public static void recipecache(CommandContext ctx)
	{
		UltramineServerModContainer.getInstance().getRecipeCache().clearCache();
		ctx.sendMessage("command.recipecache.clear.success");
	}
	
	@SideOnly(Side.SERVER)
	@Command(
			name = "reloadcfg",
			group = "technical",
			permissions = {"command.technical.reloadcfg"},
			syntax = {""}
	)
	public static void reloadcfg(CommandContext ctx)
	{
		ConfigurationHandler.load();
		UltramineServerConfig cfg = ConfigurationHandler.getServerConfig();
		MinecraftServer server = ctx.getServer();
		server.func_143006_e(cfg.settings.player.playerIdleTimeout);
		server.getConfigurationManager().maxPlayers = cfg.settings.player.maxPlayers;
		
		server.getMultiWorld().reloadServerWorlds();
		UltramineServerModContainer.getInstance().reloadToolsCfg();
		
		ctx.sendMessage("command.reloadcfg.success");
	}
	
	@Command(
			name = "entitylist",
			group = "technical",
			permissions = {"command.technical.entitylist"},
			syntax = {""}
	)
	public static void entitylist(CommandContext ctx)
	{
		for(WorldServer world : ctx.getServer().getMultiWorld().getLoadedWorlds())
		{
			for(Entity ent : GenericIterableFactory.newCastingIterable(world.loadedEntityList, Entity.class))
			{
				ctx.sendMessage("[%s](%s, %s, %s) ID: %s, Class: %s, Entity: %s", world.provider.dimensionId, ent.posX, ent.posY, ent.posZ, ent.getEntityId(), ent.getClass(), ent);
			}
		}
	}
	
	@Command(
			name = "killentity",
			group = "technical",
			permissions = {"command.technical.killentity"},
			syntax = {"<%id>"}
	)
	public static void killentity(CommandContext ctx)
	{
		int id = ctx.get("id").asInt();
		for(WorldServer world : ctx.getServer().getMultiWorld().getLoadedWorlds())
		{
			for(Entity ent : GenericIterableFactory.newCastingIterable(world.loadedEntityList, Entity.class))
			{
				if(ent.getEntityId() == id)
				{
					ent.attackEntityFrom(DamageSource.outOfWorld, 10000f);
					ent.setDead();
				}
			}
		}
	}
}
