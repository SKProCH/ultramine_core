package org.ultramine.server;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ultramine.server.WorldsConfig.WorldConfig;
import org.ultramine.server.WorldsConfig.WorldConfig.MobSpawn.MobSpawnEngine;
import org.ultramine.server.WorldsConfig.WorldConfig.Settings.WorldTime;
import org.ultramine.server.util.BasicTypeParser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.network.ForgeMessage;
import net.minecraftforge.event.world.WorldEvent;

public class MultiWorld
{
	private static final Logger log = LogManager.getLogger();
	private final MinecraftServer server;
	private final TIntObjectMap<String> dimToNameMap = new TIntObjectHashMap<String>();
	private final TIntObjectMap<WorldServer> dimToWorldMap = new TIntObjectHashMap<WorldServer>();
	private final Map<String, WorldServer> nameToWorldMap = new HashMap<String, WorldServer>();
	private final TIntObjectMap<WorldConfig> dimToConfigMap = new TIntObjectHashMap<WorldConfig>();
	private final Set<String> backupDirs = new HashSet<String>();
	private TIntSet isolatedDataDims;

	public MultiWorld(MinecraftServer server)
	{
		this.server = server;
	}
	
	@SubscribeEvent
	public void onPlayerLoggedIn(FMLNetworkEvent.ServerConnectionFromClientEvent event)
	{
		FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel("FORGE", Side.SERVER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DISPATCHER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(event.manager.channel().attr(NetworkDispatcher.FML_DISPATCHER).get());
		for (int dim : DimensionManager.getStaticDimensionIDs())
		{
			int pid = DimensionManager.getProviderType(dim);
			channel.writeAndFlush(new ForgeMessage.DimensionRegisterMessage(dim, pid == -10 ? 0 : pid));
		}
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		if(!event.world.isRemote)
		{
			dimToWorldMap.remove(event.world.provider.dimensionId);
			nameToWorldMap.remove(event.world.getWorldInfo().getWorldName());
		}
	}
	
	@SideOnly(Side.SERVER)
	public void handleServerWorldsInit()
	{
		DimensionManager.registerProviderType(-10, org.ultramine.server.wempty.WorldProviderEmpty.class, false);
		DimensionManager.unregisterDimension(-1);
		DimensionManager.unregisterDimension(0);
		DimensionManager.unregisterDimension(1);
		
		Map<String, WorldConfig> worlds = ConfigurationHandler.getWorldsConfig().worlds;
		TIntSet isolatedDataDimsSet = new TIntHashSet();
		
		for(Map.Entry<String, WorldConfig> ent : worlds.entrySet())
		{
			WorldConfig conf = ent.getValue();
			DimensionManager.registerDimension(conf.dimension, conf.generation.providerID);
			if(conf.settings.useIsolatedPlayerData)
				isolatedDataDimsSet.add(conf.dimension);
			
			dimToNameMap.put(conf.dimension, ent.getKey());
			dimToConfigMap.put(conf.dimension, conf);
		}
		
		isolatedDataDims = TCollections.unmodifiableSet(isolatedDataDimsSet);
		
		String mainWorldName = dimToNameMap.get(0);
		if(mainWorldName == null)
			mainWorldName = "world";
		server.setFolderName("worlds" + File.separator + mainWorldName);
		
		WorldConfig mainConf = worlds.get(mainWorldName);
		if(mainConf == null)
			mainConf = ConfigurationHandler.getWorldsConfig().global;
		
		if(mainConf.settings.useIsolatedPlayerData)
		{
			log.warn("Can't use isolated player data in main world! Ignoring it");
			mainConf.settings.useIsolatedPlayerData = false;
		}
		
		boolean usingPlayerDir = server.getConfigurationManager().getDataLoader().getDataProvider().isUsingWorldPlayerDir();
		
		ISaveFormat format = server.getActiveAnvilConverter();
		ISaveHandler mainSaveHandler = format.getSaveLoader(mainWorldName, true);
		WorldInfo mainWorldInfo = mainSaveHandler.loadWorldInfo();
		WorldSettings mainSettings = makeSettings(mainWorldInfo, mainConf);
		
		WorldServer mainWorld = new WorldServer(server, mainSaveHandler, mainWorldName, mainConf.dimension, mainSettings, server.theProfiler);
		
		initWorld(mainWorld, mainConf);
		
		for (int dim : DimensionManager.getStaticDimensionIDs())
		{
			if(dim == mainConf.dimension) continue;
			
			String name = dimToNameMap.get(dim);
			WorldConfig conf;
			
			if(name == null)
			{
				log.warn("World with dimension id:{} was loaded bypass worlds configuration. Using global config", dim);
				name = "world_unnamed" + dim;
				dimToNameMap.put(dim, name);
				conf = ConfigurationHandler.getWorldsConfig().global;
			}
			else
			{
				conf = worlds.get(name);
			}
			
			WorldServer world;
			if(ConfigurationHandler.getServerConfig().settings.other.splitWorldDirs)
			{
				ISaveHandler save = format.getSaveLoader(name, usingPlayerDir && conf.settings.useIsolatedPlayerData);
				((AnvilSaveHandler)save).setSingleStorage();
				world = new WorldServer(server, save, name, dim, makeSettings(save.loadWorldInfo(), conf), server.theProfiler);
			}
			else
			{
				world = new WorldServerMulti(server, mainSaveHandler, mainWorldName, dim, makeSettings(mainWorldInfo, conf), mainWorld, server.theProfiler);
			}
			
			initWorld(world, conf);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void handleClientWorldsInit()
	{
		WorldConfig conf = new WorldConfig();
		conf.mobSpawn = new WorldConfig.MobSpawn();
		conf.settings = new WorldConfig.Settings();
		conf.chunkLoading = new WorldConfig.ChunkLoading();
		for(WorldServer world : server.worldServers)
		{
			world.setConfig(conf);
			String name = world.getWorldInfo().getWorldName();
			dimToWorldMap.put(world.provider.dimensionId, world);
			if(nameToWorldMap.containsKey(name))
				nameToWorldMap.put(name + world.provider.dimensionId, world);
			else
				nameToWorldMap.put(name, world);
		}
	}
	
	@SideOnly(Side.SERVER)
	public void initDimension(int dim)
	{
		ISaveFormat format = server.getActiveAnvilConverter();
		
		String name = dimToNameMap.get(dim);
		WorldConfig conf = name == null ? null : ConfigurationHandler.getWorldsConfig().worlds.get(name);
		
		if(name == null)
		{
			log.warn("World with dimension id:{} was loaded bypass worlds configuration. Using global config", dim);
			name = "world_unnamed" + dim;
			dimToNameMap.put(dim, name);
			conf = ConfigurationHandler.getWorldsConfig().global;
		}
		else
		{
			conf = ConfigurationHandler.getWorldsConfig().worlds.get(name);
		}
		
		WorldServer world;
		if(ConfigurationHandler.getServerConfig().settings.other.splitWorldDirs)
		{
			ISaveHandler save = format.getSaveLoader(name, false);
			((AnvilSaveHandler)save).setSingleStorage();
			world = new WorldServer(server, save, name, dim, makeSettings(save.loadWorldInfo(), conf), server.theProfiler);
		}
		else
		{
			WorldServer mainWorld = getWorldByID(0);
			ISaveHandler mainSaveHandler = mainWorld.getSaveHandler();
			WorldInfo mainWorldInfo = mainWorld.getWorldInfo();
			world = new WorldServerMulti(server, mainSaveHandler, mainWorldInfo.getWorldName(), dim, makeSettings(mainWorldInfo, conf), mainWorld, server.theProfiler);
		}
		
		initWorld(world, conf);
	}
	
	@SideOnly(Side.CLIENT)
	public void onClientInitDimension(WorldServer world)
	{
		WorldConfig conf = new WorldConfig();
		conf.mobSpawn = new WorldConfig.MobSpawn();
		conf.settings = new WorldConfig.Settings();
		conf.chunkLoading = new WorldConfig.ChunkLoading();
		
		world.setConfig(conf);
		String name = world.getWorldInfo().getWorldName();
		dimToWorldMap.put(world.provider.dimensionId, world);
		if(nameToWorldMap.containsKey(name))
			nameToWorldMap.put(name + world.provider.dimensionId, world);
		else
			nameToWorldMap.put(name, world);
	}
	
	@SideOnly(Side.SERVER)
	private WorldSettings makeSettings(WorldInfo wi, WorldConfig conf)
	{
		WorldSettings mainSettings;

		if (wi == null)
		{
			mainSettings = new WorldSettings(toSeed(conf.generation.seed), server.getGameType(), conf.generation.generateStructures,
					server.isHardcore(), WorldType.parseWorldType(conf.generation.levelType));
			mainSettings.func_82750_a(conf.generation.generatorSettings);
		}
		else
		{
			mainSettings = new WorldSettings(wi);
		}
		
		return mainSettings;
	}
	
	private static long toSeed(String seedstr)
	{
		try
		{
			return Long.parseLong(seedstr);
		}
		catch (NumberFormatException e)
		{
			return seedstr.hashCode();
		}
	}
	
	@SideOnly(Side.SERVER)
	private void initWorld(WorldServer world, WorldConfig conf)
	{
		world.addWorldAccess(new WorldManager(server, world));

		if (!server.isSinglePlayer())
			world.getWorldInfo().setGameType(server.getGameType());
		
		world.difficultySetting = BasicTypeParser.parseDifficulty(ConfigurationHandler.getWorldsConfig().global.settings.difficulty);
		world.setAllowedSpawnTypes(conf.mobSpawn.spawnMonsters, conf.mobSpawn.spawnAnimals);
		world.getGameRules().setOrCreateGameRule("doDaylightCycle", Boolean.toString(conf.settings.time != WorldTime.FIXED));
		world.getGameRules().setOrCreateGameRule("doMobSpawning", Boolean.toString(conf.mobSpawn.spawnEngine != MobSpawnEngine.NONE));
		world.setConfig(conf);

		MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
		
		String name = world.getWorldInfo().getWorldName();
		dimToWorldMap.put(world.provider.dimensionId, world);
		if(nameToWorldMap.containsKey(name))
			nameToWorldMap.put(name + world.provider.dimensionId, world);
		else
			nameToWorldMap.put(name, world);
		
		backupDirs.add(name);
	}
	
	public WorldServer getWorldByID(int dim)
	{
		return dimToWorldMap.get(dim);
	}
	
	public WorldServer getOrLoadWorldByID(int dim)
	{
		WorldServer world = dimToWorldMap.get(dim);
		if(world != null)
			return world;
		DimensionManager.initDimension(dim);
		return dimToWorldMap.get(dim);
	}
	
	public WorldServer getWorldByName(String name)
	{
		return nameToWorldMap.get(name);
	}
	
	public WorldServer getWorldByNameOrID(String id)
	{
		return BasicTypeParser.isInt(id) ? dimToWorldMap.get(Integer.parseInt(id)) : nameToWorldMap.get(id);
	}
	
	public Set<String> getAllNames()
	{
		return nameToWorldMap.keySet();
	}
	
	public Collection<WorldServer> getLoadedWorlds()
	{
		return dimToWorldMap.valueCollection();
	}
	
	public String getNameByID(int id)
	{
		return dimToNameMap.get(id);
	}
	
	@SideOnly(Side.SERVER)
	public WorldConfig getConfigByID(int dim)
	{
		WorldConfig cfg = dimToConfigMap.get(dim);
		if(cfg == null)
			return ConfigurationHandler.getWorldsConfig().global;
		return cfg;
	}
	
	public Set<String> getDirsForBackup()
	{
		return backupDirs;
	}
	
	public TIntSet getIsolatedDataDims()
	{
		return isolatedDataDims;
	}
	
	public void register()
	{
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public void unregister()
	{
		FMLCommonHandler.instance().bus().unregister(this);
		MinecraftForge.EVENT_BUS.unregister(this);
		dimToWorldMap.clear();
		nameToWorldMap.clear();
	}
}
