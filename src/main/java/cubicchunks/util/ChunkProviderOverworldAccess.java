package cubicchunks.util;

import com.google.common.base.Throwables;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkProviderOverworld;
import net.minecraft.world.gen.ChunkProviderSettings;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.*;

import java.lang.invoke.MethodHandle;
import java.util.Random;

import static cubicchunks.util.ReflectionUtil.*;

public class ChunkProviderOverworldAccess {
	private static MethodHandle getRand = fieldGetter("field_185990_i");
	private static MethodHandle setBlocksInChunk = method("func_185976_a");
	private static MethodHandle setBiomesForGeneration = fieldSetter("field_185981_C");
	private static MethodHandle getBiomesForGeneration = fieldGetter("field_185981_C");
	private static MethodHandle replaceBiomeBlocks = method("func_185977_a");
	private static MethodHandle getSettings = fieldGetter("field_186000_s");
	private static MethodHandle getCaveGenerator = fieldGetter("field_186003_v");
	private static MethodHandle getRavineGenerator = fieldGetter("field_185979_A");
	private static MethodHandle getMapFeaturesEnabled = fieldGetter("field_185996_o");
	private static MethodHandle getMineshaftGenerator = fieldGetter("field_186006_y");
	private static MethodHandle getVillageGenerator = fieldGetter("field_186005_x");
	private static MethodHandle getStrongholdGenerator = fieldGetter("field_186004_w");
	private static MethodHandle getScatteredFeatureGenerator = fieldGetter("field_186007_z");
	private static MethodHandle getOceanMonumentGenerator = fieldGetter("field_185980_B");

	private static MethodHandle fieldGetter(String srgName) {
		return getFieldGetterHandle(ChunkProviderOverworld.class, srgName);
	}

	private static MethodHandle fieldSetter(String srgName) {
		return getFieldSetterHandle(ChunkProviderOverworld.class, srgName);
	}

	private static MethodHandle method(String srgName) {
		return getMethodHandle(ChunkProviderOverworld.class, srgName);
	}

	public static final Random getRand(ChunkProviderOverworld prov) {
		try {
			return (Random) getRand.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final void setBlocksInChunk(ChunkProviderOverworld prov, int x, int z, ChunkPrimer primer) {
		try {
			setBlocksInChunk.invoke(prov, x, z, primer);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final void setBiomesForGeneration(ChunkProviderOverworld prov, BiomeGenBase[] arr) {
		try {
			setBiomesForGeneration.invoke(prov, arr);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final BiomeGenBase[] getBiomesForGeneration(ChunkProviderOverworld prov) {
		try {
			return (BiomeGenBase[]) getBiomesForGeneration.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final void replaceBiomeBlocks(ChunkProviderOverworld prov, int x, int z, ChunkPrimer primer, BiomeGenBase[] biomes) {
		try {
			replaceBiomeBlocks.invoke(prov, x, z, primer, biomes);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final ChunkProviderSettings getSettings(ChunkProviderOverworld prov) {
		try {
			return (ChunkProviderSettings) getSettings.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final MapGenBase getCaveGenerator(ChunkProviderOverworld prov) {
		try {
			return (MapGenBase) getCaveGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final MapGenBase getRavineGenerator(ChunkProviderOverworld prov) {
		try {
			return (MapGenBase) getRavineGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final boolean getMapFeaturesEnabled(ChunkProviderOverworld prov) {
		try {
			return (Boolean) getMapFeaturesEnabled.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final MapGenMineshaft getMineshaftGenerator(ChunkProviderOverworld prov) {
		try {
			return (MapGenMineshaft) getMineshaftGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final MapGenVillage getVillageGenerator(ChunkProviderOverworld prov) {
		try {
			return (MapGenVillage) getVillageGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final MapGenStronghold getStrongholdGenerator(ChunkProviderOverworld prov) {
		try {
			return (MapGenStronghold) getStrongholdGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final MapGenScatteredFeature getScatteredFeatureGenerator(ChunkProviderOverworld prov) {
		try {
			return (MapGenScatteredFeature) getScatteredFeatureGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}

	public static final StructureOceanMonument getOceanMonumentGenerator(ChunkProviderOverworld prov) {
		try {
			return (StructureOceanMonument) getOceanMonumentGenerator.invoke(prov);
		} catch (Throwable throwable) {
			throw Throwables.propagate(throwable);
		}
	}
}
