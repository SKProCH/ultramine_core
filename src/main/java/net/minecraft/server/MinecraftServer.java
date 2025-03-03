package net.minecraft.server;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.StartupQuery;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Bootstrap;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ultramine.permission.IPermissionManager;
import org.ultramine.scheduler.Scheduler;
import org.ultramine.server.BackupManager;
import org.ultramine.server.ConfigurationHandler;
import org.ultramine.server.internal.WatchdogThread;
import org.ultramine.server.internal.ChatComponentLogMessage;
import org.ultramine.server.world.MultiWorld;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

public abstract class MinecraftServer implements ICommandSender, Runnable, IPlayerUsage
{
	private static final Logger logger = LogManager.getLogger();
	public static File field_152367_a = new File("usercache.json");
	private static MinecraftServer mcServer;
	private final ISaveFormat anvilConverterForAnvilFile;
	private final PlayerUsageSnooper usageSnooper = new PlayerUsageSnooper("server", this, getSystemTimeMillis());
	private final File anvilFile;
	private final List tickables = new ArrayList();
	private final ICommandManager commandManager;
	public final Profiler theProfiler = new Profiler();
	private final NetworkSystem field_147144_o;
	private final ServerStatusResponse field_147147_p = new ServerStatusResponse();
	private final Random field_147146_q = new Random();
	@SideOnly(Side.SERVER)
	private String hostname;
	private int serverPort = -1;
	public WorldServer[] worldServers = new WorldServer[0];
	private ServerConfigurationManager serverConfigManager;
	private boolean serverRunning = true;
	private boolean serverStopped;
	private int tickCounter;
	protected final Proxy serverProxy;
	public String currentTask;
	public int percentDone;
	private boolean onlineMode;
	private boolean canSpawnAnimals;
	private boolean canSpawnNPCs;
	private boolean pvpEnabled;
	private boolean allowFlight;
	private String motd;
	private int buildLimit;
	private int field_143008_E = 0;
	public final long[] tickTimeArray = new long[100];
	//public long[][] timeOfLastDimensionTick;
	public Hashtable<Integer, long[]> worldTickTimes = new Hashtable<Integer, long[]>();
	private KeyPair serverKeyPair;
	private String serverOwner;
	private String folderName;
	@SideOnly(Side.CLIENT)
	private String worldName;
	private boolean isDemo;
	private boolean enableBonusChest;
	private boolean worldIsBeingDeleted;
	private String field_147141_M = "";
	private boolean serverIsRunning;
	private long timeOfLastWarning;
	private String userMessage;
	private boolean startProfiling;
	private boolean isGamemodeForced;
	private final YggdrasilAuthenticationService field_152364_T;
	private final MinecraftSessionService field_147143_S;
	private long field_147142_T = 0L;
	private final GameProfileRepository field_152365_W;
	protected PlayerProfileCache field_152366_X;
	private static final String __OBFID = "CL_00001462";

	public MinecraftServer(File p_i45281_1_, Proxy p_i45281_2_)
	{
		mcServer = this;
		this.serverProxy = p_i45281_2_;
		this.anvilFile = p_i45281_1_;
		this.field_147144_o = new NetworkSystem(this);
		this.commandManager = new ServerCommandManager();
		this.anvilConverterForAnvilFile = new AnvilSaveConverter(p_i45281_1_);
		this.field_152364_T = new YggdrasilAuthenticationService(p_i45281_2_, UUID.randomUUID().toString());
		this.field_147143_S = this.field_152364_T.createMinecraftSessionService();
		this.field_152365_W = this.field_152364_T.createProfileRepository();
	}

	protected abstract boolean startServer() throws IOException;

	protected void convertMapIfNeeded(String p_71237_1_)
	{
		if (this.getActiveAnvilConverter().isOldMapFormat(p_71237_1_))
		{
			logger.info("Converting map!");
			this.setUserMessage("menu.convertingLevel");
			this.getActiveAnvilConverter().convertMapFormat(p_71237_1_, new IProgressUpdate()
			{
				private long field_96245_b = System.currentTimeMillis();
				private static final String __OBFID = "CL_00001417";
				public void displayProgressMessage(String p_73720_1_) {}
				public void setLoadingProgress(int p_73718_1_)
				{
					if (System.currentTimeMillis() - this.field_96245_b >= 1000L)
					{
						this.field_96245_b = System.currentTimeMillis();
						MinecraftServer.logger.info("Converting... " + p_73718_1_ + "%");
					}
				}
				@SideOnly(Side.CLIENT)
				public void resetProgressAndMessage(String p_73721_1_) {}
				@SideOnly(Side.CLIENT)
				public void func_146586_a() {}
				public void resetProgresAndWorkingMessage(String p_73719_1_) {}
			});
		}
	}

	protected synchronized void setUserMessage(String p_71192_1_)
	{
		this.userMessage = p_71192_1_;
	}

	@SideOnly(Side.CLIENT)

	public synchronized String getUserMessage()
	{
		return this.userMessage;
	}

	protected void loadAllWorlds(String p_71247_1_, String p_71247_2_, long p_71247_3_, WorldType p_71247_5_, String p_71247_6_)
	{
		this.convertMapIfNeeded(p_71247_1_);
		this.setUserMessage("menu.loadingLevel");
		ISaveHandler isavehandler = this.anvilConverterForAnvilFile.getSaveLoader(p_71247_1_, true);
		WorldInfo worldinfo = isavehandler.loadWorldInfo();
		WorldSettings worldsettings;

		if (worldinfo == null)
		{
			worldsettings = new WorldSettings(p_71247_3_, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), p_71247_5_);
			worldsettings.func_82750_a(p_71247_6_);
		}
		else
		{
			worldsettings = new WorldSettings(worldinfo);
		}

		if (this.enableBonusChest)
		{
			worldsettings.enableBonusChest();
		}

		WorldServer overWorld = (isDemo() ? new DemoWorldServer(this, isavehandler, p_71247_2_, 0, theProfiler) : new WorldServer(this, isavehandler, p_71247_2_, 0, worldsettings, theProfiler));
		for (int dim : DimensionManager.getStaticDimensionIDs())
		{
			WorldServer world = (dim == 0 ? overWorld : new WorldServerMulti(this, isavehandler, p_71247_2_, dim, worldsettings, overWorld, theProfiler));
			world.addWorldAccess(new WorldManager(this, world));

			if (!this.isSinglePlayer())
			{
				world.getWorldInfo().setGameType(this.getGameType());
			}

			MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
		}

		this.serverConfigManager.setPlayerManager(new WorldServer[]{ overWorld });
		this.func_147139_a(this.func_147135_j());
		this.initialWorldChunkLoad();
	}

	protected void initialWorldChunkLoad()
	{
		/*
		boolean flag = true;
		boolean flag1 = true;
		boolean flag2 = true;
		boolean flag3 = true;
		int i = 0;
		this.setUserMessage("menu.generatingTerrain");
		byte b0 = 0;
		logger.info("Preparing start region for level " + b0);
		WorldServer worldserver = this.worldServers[b0];
		ChunkCoordinates chunkcoordinates = worldserver.getSpawnPoint();
		long j = getSystemTimeMillis();

		for (int k = -192; k <= 192 && this.isServerRunning(); k += 16)
		{
			for (int l = -192; l <= 192 && this.isServerRunning(); l += 16)
			{
				long i1 = getSystemTimeMillis();

				if (i1 - j > 1000L)
				{
					this.outputPercentRemaining("Preparing spawn area", i * 100 / 625);
					j = i1;
				}

				++i;
				worldserver.theChunkProviderServer.loadChunk(chunkcoordinates.posX + k >> 4, chunkcoordinates.posZ + l >> 4);
			}
		}
		*/
		this.clearCurrentTask();
	}

	public abstract boolean canStructuresSpawn();

	public abstract WorldSettings.GameType getGameType();

	public abstract EnumDifficulty func_147135_j();

	public abstract boolean isHardcore();

	public abstract int getOpPermissionLevel();

	public abstract boolean func_152363_m();

	protected void outputPercentRemaining(String p_71216_1_, int p_71216_2_)
	{
		this.currentTask = p_71216_1_;
		this.percentDone = p_71216_2_;
		logger.info(p_71216_1_ + ": " + p_71216_2_ + "%");
	}

	protected void clearCurrentTask()
	{
		this.currentTask = null;
		this.percentDone = 0;
	}

	protected void saveAllWorlds(boolean p_71267_1_)
	{
		if (!this.worldIsBeingDeleted)
		{
			WorldServer[] aworldserver = this.worldServers;
			if (aworldserver == null) return; //Forge: Just in case, NPE protection as it has been encountered.
			int i = aworldserver.length;

			for (int j = 0; j < i; ++j)
			{
				WorldServer worldserver = aworldserver[j];

				if (worldserver != null)
				{
					if (!p_71267_1_)
					{
						logger.info("Saving chunks for level \'" + worldserver.getWorldInfo().getWorldName() + "\'/" + worldserver.provider.getDimensionName());
					}

					try
					{
						worldserver.saveAllChunks(true, (IProgressUpdate)null);
					}
					catch (MinecraftException minecraftexception)
					{
						logger.warn(minecraftexception.getMessage());
					}
				}
			}
		}
	}

	public void stopServer()
	{
		if (!this.worldIsBeingDeleted && Loader.instance().hasReachedState(LoaderState.SERVER_STARTED) && !serverStopped) // make sure the save is valid and we don't save twice
		{
			logger.info("Stopping server");

			if (this.func_147137_ag() != null)
			{
				this.func_147137_ag().terminateEndpoints();
			}

			if (this.serverConfigManager != null)
			{
				logger.info("Saving players");
				this.serverConfigManager.saveAllPlayerData();
				this.serverConfigManager.removeAllPlayers();
			}

			if (this.worldServers != null)
			{
				logger.info("Saving worlds");
				this.saveAllWorlds(false);

				for (int i = 0; i < this.worldServers.length; ++i)
				{
					WorldServer worldserver = this.worldServers[i];
					MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
					worldserver.flush();
				}

				WorldServer[] tmp = worldServers;
				for (WorldServer world : tmp)
				{
					DimensionManager.setWorld(world.provider.dimensionId, null);
				}
			}

			if (this.usageSnooper.isSnooperRunning())
			{
				this.usageSnooper.stopSnooper();
			}
		}
	}

	public boolean isServerRunning()
	{
		return this.serverRunning;
	}

	public void initiateShutdown()
	{
		this.serverRunning = false;
	}

	public void run()
	{
		boolean normalStarted = false;
		try
		{
			if (normalStarted = startServer())
			{
				FMLCommonHandler.instance().handleServerStarted();
				logger.info("\u00a7eServer loading totally finished");
				long i = getSystemTimeMillis();
				long l = 0L;
				this.field_147147_p.func_151315_a(new ChatComponentText(this.motd));
				this.field_147147_p.func_151321_a(new ServerStatusResponse.MinecraftProtocolVersionIdentifier("1.7.10", 5));
				this.func_147138_a(this.field_147147_p);
				
				if(!isSinglePlayer()) WatchdogThread.doStart();
				
				long curWait = 0L;
				long curPeakWait = 0L;
				for (long lastTick = System.nanoTime() - TICK_TIME; this.serverRunning; this.serverIsRunning = true)
				{
					long curTime = System.nanoTime();
					long wait = TICK_TIME - (curTime - lastTick);
					if(curWait == 0) curWait = wait;
					wait -= catchupTime;

					if (wait > 100000)
					{
						utilizeCPU(wait);
						catchupTime = 0;
						continue;
					}
					else
					{
						catchupTime = Math.min(TICK_TIME * TPS, -wait);
					}

					currentTPS = (currentTPS * 0.95) + (1E9 / (curTime - lastTick) * 0.05);
					currentWait = (long)(currentWait * 0.95 + curWait * 0.05);
					if(curWait < curPeakWait)
						curPeakWait = curWait;
					if(tickCounter % 20 == 0)
					{
						peakWait = curPeakWait;
						curPeakWait = TICK_TIME;
					}
					curWait = 0;
					
					lastTick = curTime;
					this.tick();
					if(!isSinglePlayer()) WatchdogThread.tick();
				}
				
				FMLCommonHandler.instance().handleServerStopping();
				FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
			}
			else
			{
				FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
				//this.finalTick((CrashReport)null);
			}
		}
		catch (StartupQuery.AbortedException e)
		{
			// ignore silently
			FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
		}
		catch (Throwable throwable1)
		{
			logger.error("Encountered an unexpected exception", throwable1);
			CrashReport crashreport = null;

			if (throwable1 instanceof ReportedException)
			{
				crashreport = this.addServerInfoToCrashReport(((ReportedException)throwable1).getCrashReport());
			}
			else
			{
				crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
			}

			File file1 = new File(new File(this.getHomeDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

			if (crashreport.saveToFile(file1))
			{
				logger.error("This crash report has been saved to: " + file1.getAbsolutePath());
			}
			else
			{
				logger.error("We were unable to save this crash report to disk.");
			}

			FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
			//this.finalTick(crashreport);
		}
		finally
		{
			try
			{
				this.stopServer();
				this.serverStopped = true;
			}
			catch (Throwable throwable)
			{
				logger.error("Exception stopping the server", throwable);
			}
			finally
			{
				try
				{
					if(normalStarted) FMLCommonHandler.instance().handleServerStopped();
				}
				finally
				{
					this.serverStopped = true;
					this.systemExitNow();
				}
			}
		}
	}

	private void func_147138_a(ServerStatusResponse p_147138_1_)
	{
		File file1 = this.getFile("server-icon.png");

		if (file1.isFile())
		{
			ByteBuf bytebuf = Unpooled.buffer();

			try
			{
				BufferedImage bufferedimage = ImageIO.read(file1);
				Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
				Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
				ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
				ByteBuf bytebuf1 = Base64.encode(bytebuf);
				p_147138_1_.func_151320_a("data:image/png;base64," + bytebuf1.toString(Charsets.UTF_8));
			}
			catch (Exception exception)
			{
				logger.error("Couldn\'t load server icon", exception);
			}
			finally
			{
				bytebuf.release();
			}
		}
	}

	protected File getDataDirectory()
	{
		return new File(".");
	}

	protected void finalTick(CrashReport p_71228_1_) {}

	protected void systemExitNow() {}

	public void tick()
	{
		long i = System.nanoTime();

		if (this.startProfiling)
		{
			this.startProfiling = false;
			this.theProfiler.profilingEnabled = true;
			this.theProfiler.clearProfiling();
		}

		this.theProfiler.startSection("root");
		FMLCommonHandler.instance().onPreServerTick();
		++this.tickCounter;
		this.updateTimeLightAndEntities();

		if (i - this.field_147142_T >= 5000000000L)
		{
			this.field_147142_T = i;
			this.field_147147_p.func_151319_a(new ServerStatusResponse.PlayerCountData(this.getMaxPlayers(), this.getCurrentPlayerCount()));
			GameProfile[] agameprofile = new GameProfile[Math.min(this.getCurrentPlayerCount(), 12)];
			int j = MathHelper.getRandomIntegerInRange(this.field_147146_q, 0, this.getCurrentPlayerCount() - agameprofile.length);

			for (int k = 0; k < agameprofile.length; ++k)
			{
				agameprofile[k] = ((EntityPlayerMP)this.serverConfigManager.playerEntityList.get(j + k)).getGameProfile();
			}

			Collections.shuffle(Arrays.asList(agameprofile));
			this.field_147147_p.func_151318_b().func_151330_a(agameprofile);
		}

//		if (this.tickCounter % 900 == 0)
//		{
			this.theProfiler.startSection("save");
			this.theProfiler.startSection("players");
			this.serverConfigManager.saveOnePlayerData(tickCounter);
			this.theProfiler.endStartSection("chunks");
			for(WorldServer world : worldServers)
				world.theChunkProviderServer.saveOneChunk(tickCounter);
			this.theProfiler.endSection();
			if(tickCounter % 2401 == 0)
			{
				this.theProfiler.startSection("other");
				for(WorldServer world : worldServers)
					world.saveOtherData();
				this.theProfiler.endSection();
			}
			this.theProfiler.endSection();
//		}

		this.theProfiler.startSection("tallying");
		this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - i;
		this.theProfiler.endSection();
		this.theProfiler.startSection("snooper");

		if (!this.usageSnooper.isSnooperRunning() && this.tickCounter > 100)
		{
			this.usageSnooper.startSnooper();
		}

		if (this.tickCounter % 6000 == 0)
		{
			this.usageSnooper.addMemoryStatsToSnooper();
		}

		this.theProfiler.endSection();
		FMLCommonHandler.instance().onPostServerTick();
		this.theProfiler.endSection();
	}

	public void updateTimeLightAndEntities()
	{
		theProfiler.startSection("ChunkIOExecutor");
		net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
		theProfiler.endSection();
		
		this.theProfiler.startSection("levels");
		int i;

		Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);
		for (int x = 0; x < ids.length; x++)
		{
			int id = ids[x];
			long j = System.nanoTime();

			if (id == 0 || this.getAllowNether())
			{
				WorldServer worldserver = DimensionManager.getWorld(id);
				this.theProfiler.startSection(worldserver.getWorldInfo().getWorldName());
				this.theProfiler.startSection("pools");
				this.theProfiler.endSection();

				if (this.tickCounter % 20 == 0)
				{
					this.theProfiler.startSection("timeSync");
					this.serverConfigManager.sendPacketToAllPlayersInDimension(new S03PacketTimeUpdate(worldserver.getTotalWorldTime(), worldserver.getWorldTime(), worldserver.getGameRules().getGameRuleBooleanValue("doDaylightCycle")), worldserver.provider.dimensionId);
					this.theProfiler.endSection();
				}

				this.theProfiler.startSection("tick");
				FMLCommonHandler.instance().onPreWorldTick(worldserver);
				CrashReport crashreport;

				try
				{
					worldserver.tick();
				}
				catch (Throwable throwable1)
				{
					crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
					worldserver.addWorldInfoToCrashReport(crashreport);
					throw new ReportedException(crashreport);
				}

				try
				{
					worldserver.updateEntities();
				}
				catch (Throwable throwable)
				{
					crashreport = CrashReport.makeCrashReport(throwable, "Exception ticking world entities");
					worldserver.addWorldInfoToCrashReport(crashreport);
					throw new ReportedException(crashreport);
				}

				FMLCommonHandler.instance().onPostWorldTick(worldserver);
				this.theProfiler.endSection();
				this.theProfiler.startSection("tracker");
				worldserver.getEntityTracker().updateTrackedEntities();
				this.theProfiler.endSection();
				this.theProfiler.endSection();
			}

			worldTickTimes.get(id)[this.tickCounter % 100] = System.nanoTime() - j;
		}

		this.theProfiler.endStartSection("dim_unloading");
		DimensionManager.unloadWorlds(worldTickTimes);
		this.theProfiler.endStartSection("connection");
		this.func_147137_ag().networkTick();
		this.theProfiler.endStartSection("players");
		this.serverConfigManager.sendPlayerInfoToAllPlayers();
		this.theProfiler.endStartSection("tickables");

		for (i = 0; i < this.tickables.size(); ++i)
		{
			((IUpdatePlayerListBox)this.tickables.get(i)).update();
		}

		this.theProfiler.endSection();
	}

	public boolean getAllowNether()
	{
		return true;
	}

	public void startServerThread()
	{
		StartupQuery.reset();
		(serverThread = new Thread("Server thread")
		{
			private static final String __OBFID = "CL_00001418";
			public void run()
			{
				MinecraftServer.this.run();
			}
		}).start();
	}

	public File getFile(String p_71209_1_)
	{
		return new File(this.getDataDirectory(), p_71209_1_);
	}

	public void logWarning(String p_71236_1_)
	{
		logger.warn(p_71236_1_);
	}

	public WorldServer worldServerForDimension(int p_71218_1_)
	{
		WorldServer ret = DimensionManager.getWorld(p_71218_1_);
		if (ret == null)
		{
			DimensionManager.initDimension(p_71218_1_);
			ret = DimensionManager.getWorld(p_71218_1_);
		}
		return ret;
	}

	public String getMinecraftVersion()
	{
		return "1.7.10";
	}

	public int getCurrentPlayerCount()
	{
		return this.serverConfigManager.getCurrentPlayerCount();
	}

	public int getMaxPlayers()
	{
		return this.serverConfigManager.getMaxPlayers();
	}

	public String[] getAllUsernames()
	{
		return this.serverConfigManager.getAllUsernames();
	}

	public GameProfile[] func_152357_F()
	{
		return this.serverConfigManager.func_152600_g();
	}

	public String getServerModName()
	{
		return FMLCommonHandler.instance().getModName();
	}

	public CrashReport addServerInfoToCrashReport(CrashReport p_71230_1_)
	{
		p_71230_1_.getCategory().addCrashSectionCallable("Profiler Position", new Callable()
		{
			private static final String __OBFID = "CL_00001419";
			public String call()
			{
				return MinecraftServer.this.theProfiler.profilingEnabled ? MinecraftServer.this.theProfiler.getNameOfLastSection() : "N/A (disabled)";
			}
		});

		if (this.worldServers != null && this.worldServers.length > 0 && this.worldServers[0] != null)
		{
			p_71230_1_.getCategory().addCrashSectionCallable("Vec3 Pool Size", new Callable()
			{
				private static final String __OBFID = "CL_00001420";
				public String call()
				{
					byte b0 = 0;
					int i = 56 * b0;
					int j = i / 1024 / 1024;
					byte b1 = 0;
					int k = 56 * b1;
					int l = k / 1024 / 1024;
					return b0 + " (" + i + " bytes; " + j + " MB) allocated, " + b1 + " (" + k + " bytes; " + l + " MB) used";
				}
			});
		}

		if (this.serverConfigManager != null)
		{
			p_71230_1_.getCategory().addCrashSectionCallable("Player Count", new Callable()
			{
				private static final String __OBFID = "CL_00001780";
				public String call()
				{
					return MinecraftServer.this.serverConfigManager.getCurrentPlayerCount() + " / " + MinecraftServer.this.serverConfigManager.getMaxPlayers() + "; " + MinecraftServer.this.serverConfigManager.playerEntityList;
				}
			});
		}

		return p_71230_1_;
	}

	public List getPossibleCompletions(ICommandSender p_71248_1_, String p_71248_2_)
	{
		ArrayList arraylist = new ArrayList();

		if (p_71248_2_.startsWith("/"))
		{
			p_71248_2_ = p_71248_2_.substring(1);
			boolean flag = !p_71248_2_.contains(" ");
			List list = this.commandManager.getPossibleCommands(p_71248_1_, p_71248_2_);

			if (list != null)
			{
				Iterator iterator = list.iterator();

				while (iterator.hasNext())
				{
					String s3 = (String)iterator.next();

					if (flag)
					{
						arraylist.add("/" + s3);
					}
					else
					{
						arraylist.add(s3);
					}
				}
			}

			return arraylist;
		}
		else
		{
			String[] astring = p_71248_2_.split(" ", -1);
			String s1 = astring[astring.length - 1];
			String[] astring1 = this.serverConfigManager.getAllUsernames();
			int i = astring1.length;

			for (int j = 0; j < i; ++j)
			{
				String s2 = astring1[j];

				if (CommandBase.doesStringStartWith(s1, s2))
				{
					arraylist.add(s2);
				}
			}

			return arraylist;
		}
	}

	public static MinecraftServer getServer()
	{
		return mcServer;
	}

	public String getCommandSenderName()
	{
		return "Server";
	}

	public void addChatMessage(IChatComponent p_145747_1_)
	{
		logger.info(new ChatComponentLogMessage(p_145747_1_));
	}

	public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_)
	{
		return true;
	}

	public ICommandManager getCommandManager()
	{
		return this.commandManager;
	}

	public KeyPair getKeyPair()
	{
		return this.serverKeyPair;
	}

	public String getServerOwner()
	{
		return this.serverOwner;
	}

	public void setServerOwner(String p_71224_1_)
	{
		this.serverOwner = p_71224_1_;
	}

	public boolean isSinglePlayer()
	{
		return this.serverOwner != null;
	}

	public String getFolderName()
	{
		return this.folderName;
	}

	public void setFolderName(String p_71261_1_)
	{
		this.folderName = p_71261_1_;
	}

	@SideOnly(Side.CLIENT)
	public void setWorldName(String p_71246_1_)
	{
		this.worldName = p_71246_1_;
	}

	@SideOnly(Side.CLIENT)
	public String getWorldName()
	{
		return this.worldName;
	}

	public void setKeyPair(KeyPair p_71253_1_)
	{
		this.serverKeyPair = p_71253_1_;
	}

	public void func_147139_a(EnumDifficulty p_147139_1_)
	{
		if(!isSinglePlayer()) return;
		
		for (int i = 0; i < this.worldServers.length; ++i)
		{
			WorldServer worldserver = this.worldServers[i];

			if (worldserver != null)
			{
				if (worldserver.getWorldInfo().isHardcoreModeEnabled())
				{
					worldserver.difficultySetting = EnumDifficulty.HARD;
					worldserver.setAllowedSpawnTypes(true, true);
					worldserver.getConfig().mobSpawn.spawnMonsters = true;
				}
				else if (this.isSinglePlayer())
				{
					worldserver.difficultySetting = p_147139_1_;
					worldserver.setAllowedSpawnTypes(worldserver.difficultySetting != EnumDifficulty.PEACEFUL, true);
					worldserver.getConfig().mobSpawn.spawnMonsters = worldserver.difficultySetting != EnumDifficulty.PEACEFUL;
				}
				else
				{
					worldserver.difficultySetting = p_147139_1_;
					worldserver.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.canSpawnAnimals);
				}
			}
		}
	}

	protected boolean allowSpawnMonsters()
	{
		return true;
	}

	public boolean isDemo()
	{
		return this.isDemo;
	}

	public void setDemo(boolean p_71204_1_)
	{
		this.isDemo = p_71204_1_;
	}

	public void canCreateBonusChest(boolean p_71194_1_)
	{
		this.enableBonusChest = p_71194_1_;
	}

	public ISaveFormat getActiveAnvilConverter()
	{
		return this.anvilConverterForAnvilFile;
	}

	public void deleteWorldAndStopServer()
	{
		this.worldIsBeingDeleted = true;
		this.getActiveAnvilConverter().flushCache();

		for (int i = 0; i < this.worldServers.length; ++i)
		{
			WorldServer worldserver = this.worldServers[i];

			if (worldserver != null)
			{
				MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
				worldserver.flush();
			}
		}

		this.getActiveAnvilConverter().deleteWorldDirectory(this.worldServers[0].getSaveHandler().getWorldDirectoryName());
		this.initiateShutdown();
	}

	public String getTexturePack()
	{
		return this.field_147141_M;
	}

	public void addServerStatsToSnooper(PlayerUsageSnooper p_70000_1_)
	{
		p_70000_1_.func_152768_a("whitelist_enabled", Boolean.valueOf(false));
		p_70000_1_.func_152768_a("whitelist_count", Integer.valueOf(0));
		p_70000_1_.func_152768_a("players_current", Integer.valueOf(this.getCurrentPlayerCount()));
		p_70000_1_.func_152768_a("players_max", Integer.valueOf(this.getMaxPlayers()));
		p_70000_1_.func_152768_a("players_seen", Integer.valueOf(this.serverConfigManager.getAvailablePlayerDat().length));
		p_70000_1_.func_152768_a("uses_auth", Boolean.valueOf(this.onlineMode));
		p_70000_1_.func_152768_a("gui_state", this.getGuiEnabled() ? "enabled" : "disabled");
		p_70000_1_.func_152768_a("run_time", Long.valueOf((getSystemTimeMillis() - p_70000_1_.getMinecraftStartTimeMillis()) / 60L * 1000L));
		p_70000_1_.func_152768_a("avg_tick_ms", Integer.valueOf((int)(MathHelper.average(this.tickTimeArray) * 1.0E-6D)));
		int i = 0;

		for (int j = 0; j < this.worldServers.length; ++j)
		{
			if (this.worldServers[j] != null)
			{
				WorldServer worldserver = this.worldServers[j];
				WorldInfo worldinfo = worldserver.getWorldInfo();
				p_70000_1_.func_152768_a("world[" + i + "][dimension]", Integer.valueOf(worldserver.provider.dimensionId));
				p_70000_1_.func_152768_a("world[" + i + "][mode]", worldinfo.getGameType());
				p_70000_1_.func_152768_a("world[" + i + "][difficulty]", worldserver.difficultySetting);
				p_70000_1_.func_152768_a("world[" + i + "][hardcore]", Boolean.valueOf(worldinfo.isHardcoreModeEnabled()));
				p_70000_1_.func_152768_a("world[" + i + "][generator_name]", worldinfo.getTerrainType().getWorldTypeName());
				p_70000_1_.func_152768_a("world[" + i + "][generator_version]", Integer.valueOf(worldinfo.getTerrainType().getGeneratorVersion()));
				p_70000_1_.func_152768_a("world[" + i + "][height]", Integer.valueOf(this.buildLimit));
				p_70000_1_.func_152768_a("world[" + i + "][chunks_loaded]", Integer.valueOf(worldserver.getChunkProvider().getLoadedChunkCount()));
				++i;
			}
		}

		p_70000_1_.func_152768_a("worlds", Integer.valueOf(i));
	}

	public void addServerTypeToSnooper(PlayerUsageSnooper p_70001_1_)
	{
		p_70001_1_.func_152767_b("singleplayer", Boolean.valueOf(this.isSinglePlayer()));
		p_70001_1_.func_152767_b("server_brand", this.getServerModName());
		p_70001_1_.func_152767_b("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
		p_70001_1_.func_152767_b("dedicated", Boolean.valueOf(this.isDedicatedServer()));
	}

	public boolean isSnooperEnabled()
	{
		return true;
	}

	public abstract boolean isDedicatedServer();

	public boolean isServerInOnlineMode()
	{
		return this.onlineMode;
	}

	public void setOnlineMode(boolean p_71229_1_)
	{
		this.onlineMode = p_71229_1_;
	}

	public boolean getCanSpawnAnimals()
	{
		return this.canSpawnAnimals;
	}

	public void setCanSpawnAnimals(boolean p_71251_1_)
	{
		this.canSpawnAnimals = p_71251_1_;
	}

	public boolean getCanSpawnNPCs()
	{
		return this.canSpawnNPCs;
	}

	public void setCanSpawnNPCs(boolean p_71257_1_)
	{
		this.canSpawnNPCs = p_71257_1_;
	}

	public boolean isPVPEnabled()
	{
		return this.pvpEnabled;
	}

	public void setAllowPvp(boolean p_71188_1_)
	{
		this.pvpEnabled = p_71188_1_;
	}

	public boolean isFlightAllowed()
	{
		return this.allowFlight;
	}

	public void setAllowFlight(boolean p_71245_1_)
	{
		this.allowFlight = p_71245_1_;
	}

	public abstract boolean isCommandBlockEnabled();

	public String getMOTD()
	{
		return this.motd;
	}

	public void setMOTD(String p_71205_1_)
	{
		this.motd = p_71205_1_;
	}

	public int getBuildLimit()
	{
		return this.buildLimit;
	}

	public void setBuildLimit(int p_71191_1_)
	{
		this.buildLimit = p_71191_1_;
	}

	public ServerConfigurationManager getConfigurationManager()
	{
		return this.serverConfigManager;
	}

	public void func_152361_a(ServerConfigurationManager p_152361_1_)
	{
		this.serverConfigManager = p_152361_1_;
	}

	public void setGameType(WorldSettings.GameType p_71235_1_)
	{
		for (int i = 0; i < this.worldServers.length; ++i)
		{
			getServer().worldServers[i].getWorldInfo().setGameType(p_71235_1_);
		}
	}

	public NetworkSystem func_147137_ag()
	{
		return this.field_147144_o;
	}

	@SideOnly(Side.CLIENT)
	public boolean serverIsInRunLoop()
	{
		return this.serverIsRunning;
	}

	public boolean getGuiEnabled()
	{
		return false;
	}

	public abstract String shareToLAN(WorldSettings.GameType p_71206_1_, boolean p_71206_2_);

	public int getTickCounter()
	{
		return this.tickCounter;
	}

	public void enableProfiling()
	{
		this.startProfiling = true;
	}

	@SideOnly(Side.CLIENT)
	public PlayerUsageSnooper getPlayerUsageSnooper()
	{
		return this.usageSnooper;
	}

	public ChunkCoordinates getPlayerCoordinates()
	{
		return new ChunkCoordinates(0, 0, 0);
	}

	public World getEntityWorld()
	{
		return this.worldServers[0];
	}

	public int getSpawnProtectionSize()
	{
		return 16;
	}

	public boolean isBlockProtected(World p_96290_1_, int p_96290_2_, int p_96290_3_, int p_96290_4_, EntityPlayer p_96290_5_)
	{
		return false;
	}

	public boolean getForceGamemode()
	{
		return this.isGamemodeForced;
	}

	public Proxy getServerProxy()
	{
		return this.serverProxy;
	}

	public static long getSystemTimeMillis()
	{
		return System.currentTimeMillis();
	}

	public int func_143007_ar()
	{
		return this.field_143008_E;
	}

	public void func_143006_e(int p_143006_1_)
	{
		this.field_143008_E = p_143006_1_;
	}

	public IChatComponent func_145748_c_()
	{
		return new ChatComponentText(this.getCommandSenderName());
	}

	public boolean func_147136_ar()
	{
		return true;
	}

	public MinecraftSessionService func_147130_as()
	{
		return this.field_147143_S;
	}

	public GameProfileRepository func_152359_aw()
	{
		return this.field_152365_W;
	}

	public PlayerProfileCache func_152358_ax()
	{
		return this.field_152366_X;
	}

	public ServerStatusResponse func_147134_at()
	{
		return this.field_147147_p;
	}

	public void func_147132_au()
	{
		this.field_147142_T = 0L;
	}

	@SideOnly(Side.SERVER)
	public String getServerHostname()
	{
		return this.hostname;
	}

	@SideOnly(Side.SERVER)
	public void setHostname(String p_71189_1_)
	{
		this.hostname = p_71189_1_;
	}

	@SideOnly(Side.SERVER)
	public void func_82010_a(IUpdatePlayerListBox p_82010_1_)
	{
		this.tickables.add(p_82010_1_);
	}

	@SideOnly(Side.SERVER)
	public static void main(String[] p_main_0_)
	{
		Bootstrap.func_151354_b();

		try
		{
			boolean flag = true;
			String s = null;
			String s1 = ".";
			String s2 = null;
			boolean flag1 = false;
			boolean flag2 = false;
			int i = -1;

			for (int j = 0; j < p_main_0_.length; ++j)
			{
				String s3 = p_main_0_[j];
				String s4 = j == p_main_0_.length - 1 ? null : p_main_0_[j + 1];
				boolean flag3 = false;

				if (!s3.equals("nogui") && !s3.equals("--nogui"))
				{
					if (s3.equals("--port") && s4 != null)
					{
						flag3 = true;

						try
						{
							i = Integer.parseInt(s4);
						}
						catch (NumberFormatException numberformatexception)
						{
							;
						}
					}
					else if (s3.equals("--singleplayer") && s4 != null)
					{
						flag3 = true;
						s = s4;
					}
					else if (s3.equals("--universe") && s4 != null)
					{
						flag3 = true;
						s1 = s4;
					}
					else if (s3.equals("--world") && s4 != null)
					{
						flag3 = true;
						s2 = s4;
					}
					else if (s3.equals("--demo"))
					{
						flag1 = true;
					}
					else if (s3.equals("--bonusChest"))
					{
						flag2 = true;
					}
				}
				else
				{
					flag = false;
				}

				if (flag3)
				{
					++j;
				}
			}

			final DedicatedServer dedicatedserver = new DedicatedServer(ConfigurationHandler.getWorldsDir());

			if (s != null)
			{
				dedicatedserver.setServerOwner(s);
			}

			if (s2 != null)
			{
				dedicatedserver.setFolderName(s2);
			}

			if (i >= 0)
			{
				dedicatedserver.setServerPort(i);
			}

			if (flag1)
			{
				dedicatedserver.setDemo(true);
			}

			if (flag2)
			{
				dedicatedserver.canCreateBonusChest(true);
			}

			dedicatedserver.startServerThread();
//			Runtime.getRuntime().addShutdownHook(new Thread("Server Shutdown Thread")
//			{
//				private static final String __OBFID = "CL_00001806";
//				public void run()
//				{
//					dedicatedserver.stopServer();
//				}
//			});
		}
		catch (Exception exception)
		{
			logger.fatal("Failed to start the minecraft server", exception);
		}
	}

	@SideOnly(Side.SERVER)
	public void logInfo(String p_71244_1_)
	{
		logger.info(p_71244_1_);
	}

	@SideOnly(Side.SERVER)
	public String getHostname()
	{
		return this.hostname;
	}

	@SideOnly(Side.SERVER)
	public int getPort()
	{
		return this.serverPort;
	}

	@SideOnly(Side.SERVER)
	public String getMotd()
	{
		return this.motd;
	}

	@SideOnly(Side.SERVER)
	public String getPlugins()
	{
		return "";
	}

	@SideOnly(Side.SERVER)
	public String handleRConCommand(String p_71252_1_)
	{
		RConConsoleSource.instance.resetLog();
		this.commandManager.executeCommand(RConConsoleSource.instance, p_71252_1_);
		return RConConsoleSource.instance.getLogContents();
	}

	@SideOnly(Side.SERVER)
	public boolean isDebuggingEnabled()
	{
		return false;
	}

	@SideOnly(Side.SERVER)
	public void logSevere(String p_71201_1_)
	{
		logger.error(p_71201_1_);
	}

	@SideOnly(Side.SERVER)
	public void logDebug(String p_71198_1_)
	{
		if (this.isDebuggingEnabled())
		{
			logger.info(p_71198_1_);
		}
	}

	@SideOnly(Side.SERVER)
	public int getServerPort()
	{
		return this.serverPort;
	}

	@SideOnly(Side.SERVER)
	public void setServerPort(int p_71208_1_)
	{
		this.serverPort = p_71208_1_;
	}

	@SideOnly(Side.SERVER)
	public void func_155759_m(String p_155759_1_)
	{
		this.field_147141_M = p_155759_1_;
	}

	public boolean isServerStopped()
	{
		return this.serverStopped;
	}

	@SideOnly(Side.SERVER)
	public void setForceGamemode(boolean p_104055_1_)
	{
		this.isGamemodeForced = p_104055_1_;
	}

	/* ========================================= ULTRAMINE START ======================================*/

	private static final int TPS = 20;
	private static final int TICK_TIME = 1000000000 / TPS;
	public double currentTPS = 20;
	private long catchupTime = 0;
	public long currentWait = TICK_TIME;
	public long peakWait = TICK_TIME;
	public final long startTime = System.currentTimeMillis();
	private Thread serverThread;
	private final MultiWorld multiworld = new MultiWorld(this);
	private IPermissionManager permissionManager;
	private final Scheduler scheduler = new Scheduler();
	
	public Thread getServerThread()
	{
		return serverThread;
	}
	
	public MultiWorld getMultiWorld()
	{
		return multiworld;
	}
	
	public BackupManager getBackupManager()
	{
		return null;
	}

	public IPermissionManager getPermissionManager()
	{
		return permissionManager;
	}

	protected void setPermissionManager(IPermissionManager permissionManager)
	{
		this.permissionManager = permissionManager;
	}
	
	public Scheduler getScheduler()
	{
		return scheduler;
	}
	
	public File getWorldsDir()
	{
		return anvilFile;
	}
	
	public File getHomeDirectory()
	{
		return getDataDirectory();
	}

	public File getVanillaFile(String name)
	{
		return getFile(name);
	}

	public File getStorageFile(String name)
	{
		return getFile(name);
	}
	
	public File getBackupDir()
	{
		File file = new File(getHomeDirectory(), "backup");
		file.mkdir();
		return file;
	}

	protected void utilizeCPU(long nanos) throws InterruptedException
	{
		Thread.sleep(nanos / 1000000);
	}
}
