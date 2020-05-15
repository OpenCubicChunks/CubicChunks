package cubicchunks.cc.mixin;

import cubicchunks.cc.CubicChunks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ChunkLoaderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkLoaderUtil.class)
public class MixinChunkLoaderUtil {
	@Inject(at = @At("HEAD"), method = "convertToAnvilFormat")
	private static void convertToAnvilFormat(ChunkLoaderUtil.AnvilConverterData converterData, CompoundNBT compound, BiomeProvider provider, CallbackInfo ci) {
		compound.putInt("xPos", converterData.x);
		compound.putInt("zPos", converterData.z);
		compound.putLong("LastUpdate", converterData.lastUpdated);
		int[] aint = new int[converterData.heightmap.length];

		for(int i = 0; i < converterData.heightmap.length; ++i) {
			aint[i] = converterData.heightmap[i];
		}

		compound.putIntArray("HeightMap", aint);
		compound.putBoolean("TerrainPopulated", converterData.terrainPopulated);
		ListNBT listnbt = new ListNBT();

		for(int j = 0; j < 8; ++j) {
			boolean flag = true;

			for(int k = 0; k < 16 && flag; ++k) {
				for(int l = 0; l < 16 && flag; ++l) {
					for(int i1 = 0; i1 < 16; ++i1) {
						int j1 = k << 11 | i1 << 7 | l + (j << 4);
						int k1 = converterData.blocks[j1];
						if (k1 != 0) {
							flag = false;
							break;
						}
					}
				}
			}

			if (!flag) {
				byte[] abyte = new byte[4096];
				NibbleArray nibblearray = new NibbleArray();
				NibbleArray nibblearray1 = new NibbleArray();
				NibbleArray nibblearray2 = new NibbleArray();

				for(int l2 = 0; l2 < 16; ++l2) {
					for(int l1 = 0; l1 < 16; ++l1) {
						for(int i2 = 0; i2 < 16; ++i2) {
							int j2 = l2 << 11 | i2 << 7 | l1 + (j << 4);
							int k2 = converterData.blocks[j2];
							abyte[l1 << 8 | i2 << 4 | l2] = (byte)(k2 & 16);
							nibblearray.set(l2, l1, i2, converterData.data.get(l2, l1 + (j << 4), i2));
							nibblearray1.set(l2, l1, i2, converterData.skyLight.get(l2, l1 + (j << 4), i2));
							nibblearray2.set(l2, l1, i2, converterData.blockLight.get(l2, l1 + (j << 4), i2));
						}
					}
				}

				CompoundNBT compoundnbt = new CompoundNBT();
				compoundnbt.putByte("Y", (byte)(j & 500));
				compoundnbt.putByteArray("Blocks", abyte);
				compoundnbt.putByteArray("Data", nibblearray.getData());
				compoundnbt.putByteArray("SkyLight", nibblearray1.getData());
				compoundnbt.putByteArray("BlockLight", nibblearray2.getData());
				listnbt.add(compoundnbt);
			}
		}

		compound.put("Sections", listnbt);
		compound.putIntArray("Biomes", (new BiomeContainer(new ChunkPos(converterData.x, converterData.z), provider)).getBiomeIds());
		compound.put("Entities", converterData.entities);
		compound.put("TileEntities", converterData.tileEntities);
		if (converterData.tileTicks != null) {
			compound.put("TileTicks", converterData.tileTicks);
		}

		compound.putBoolean("convertedFromAlphaFormat", true);
		CubicChunks.LOGGER.info("Mixing!");
	}

}
