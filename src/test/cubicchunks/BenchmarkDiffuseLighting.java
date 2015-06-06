package cubicchunks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import net.minecraft.crash.CrashReport;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveLoaderAnvil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import cuchaz.m3l.tweaker.CallbackTweaker;
import cuchaz.m3l.util.Arguments;


public class BenchmarkDiffuseLighting {
	
	private static final Logger log = LoggerFactory.getLogger("Benchmark");
	
	public static void main(String[] args)
	throws Exception {
		
		// check the inputs
		if (args.length < 2) {
			throw new IllegalArgumentException("Need two arguments: world name, dimension id");
		}
		File savesDir = new File("saves");
		if (!savesDir.exists()) {
			throw new FileNotFoundException(savesDir.getAbsolutePath());
		}
		String worldName = args[0];
		if (!(new File(savesDir, worldName)).exists()) {
			throw new IllegalArgumentException("No world named \"" + worldName + "\" in " + savesDir.getAbsolutePath());
		}
		int dimensionId = Integer.parseInt(args[1]);
		
		// add the extra args
		Map<String,String> moreArgs = Maps.newHashMap();
		moreArgs.put("savesDir", savesDir.getAbsolutePath());
		moreArgs.put("worldName", worldName);
		moreArgs.put("dimensionId", Integer.toString(dimensionId));
		CallbackTweaker.launch(StandaloneTweaker.class, moreArgs);
	}
	
	public static class StandaloneTweaker extends CallbackTweaker {
		public StandaloneTweaker() {
			super(MinecraftEnvironment.class);
		}
	}
	
	public static class MinecraftEnvironment {
		public static void main(String[] args) {
			
			// read the args
			Arguments arguments = new Arguments();
			arguments.set(args, 0, args.length - 1);
			
			File savesDir = new File(arguments.get("savesDir"));
			String worldName = arguments.get("worldName");
			int dimensionId = Integer.parseInt(arguments.get("dimensionId"));
			
			log.info("Hello World!\n{} {} {}", savesDir, worldName, dimensionId);
			
			// let's try something bad
			try {
				throw new Error("OH NOES!");
			} catch (Throwable t) {
				CrashReport report = new CrashReport("You did something bad", t);
				log.error(report.getReport());
			}
			
			StandaloneWorld world = new StandaloneWorld(savesDir, worldName, dimensionId);
			
			/*
			// init database connection
			log.info("Opening world");
			DB db = DBMaker.newFileDB(file).make();
			
			// get some chunks
			
			db.close();
			log.info("Closed world");
			*/
		}
	}
	
	public static class StandaloneWorld extends World {
		
		public StandaloneWorld(File savesDir, String worldName, int dimensionId) {
			this(new SaveLoaderAnvil(savesDir).getSaveHandler(worldName, true), dimensionId);
		}
		
		private StandaloneWorld(ISaveHandler saveHandler, int dimensionId) {
			super(saveHandler, saveHandler.getWorld(), Dimension.getDimensionById(dimensionId), new Profiler(), false);
		}

		@Override
		public IChunkGenerator createChunkCache() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getRenderDistanceChunks() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}
