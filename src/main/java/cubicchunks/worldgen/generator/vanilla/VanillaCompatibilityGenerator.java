package cubicchunks.worldgen.generator.vanilla;

import java.util.List;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.IColumnGenerator;
import cubicchunks.worldgen.generator.ICubeGenerator;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class VanillaCompatibilityGenerator implements ICubeGenerator, IColumnGenerator {

	private static final Vec3i[] NO_POPULATION = new Vec3i[]{
			new Vec3i(0, 0, 0),
			new Vec3i(0, 0, 0)
	};

	private IChunkGenerator vanilla;
	private ICubicWorld world;

	private Chunk lastChunk;
	//private boolean optimizationHack;

	private Biome[] biomes;

	private boolean stripBadrock;
	private IBlockState badrock = Blocks.BEDROCK.getDefaultState();
	private IBlockState underBlock = Blocks.STONE.getDefaultState();

	public VanillaCompatibilityGenerator(IChunkGenerator vanilla, ICubicWorld world) {
		this.vanilla = vanilla;
		this.world = world;
		
		// heuristics TODO: add a config that overrides this
		lastChunk = vanilla.provideChunk(0, 0); // lets scan the chunk at 0, 0

		IBlockState topstate = null;
		int         topcount = 0;
		{   // find the type of block that is most common on the bottom layer
			IBlockState laststate = null;
			for(int at = 0;at < 16 * 16;at++){
				IBlockState state = lastChunk.getBlockState(at | 0x0F, 0, at >> 4);
				if(state != laststate){
					
					int count = 1;
					for(int i = at + 1;i < 16 * 16;i++){
						if(lastChunk.getBlockState(i | 0x0F, 0, i >> 4) == state){
							count++;
						}
					}
					if(count > topcount){
						topcount = count;
						topstate = state;
					}
				}
				laststate = state;
			}
		}

		if(topstate.getBlock() != Blocks.BEDROCK){
			underBlock = topstate;
		}else{
			stripBadrock = true;
			underBlock = world.getProvider() instanceof WorldProviderHell
					? Blocks.NETHERRACK.getDefaultState()
					: Blocks.STONE.getDefaultState(); //TODO: maybe scan for stone type?
		}
	}

	@Override
	public Column generateColumn(Column column) {
		
		this.biomes = this.world.getBiomeProvider()
				.getBiomes(this.biomes, 
						Coords.cubeToMinBlock(column.getX()),
						Coords.cubeToMinBlock(column.getZ()),
						16, 16);
		
		byte[] abyte = column.getBiomeArray();
        for (int i = 0; i < abyte.length; ++i)
        {
            abyte[i] = (byte)Biome.getIdForBiome(this.biomes[i]);
        }
		
		return column;
	}

	@Override
	public void recreateStructures(Column column) {
		vanilla.recreateStructures(column, column.getX(), column.getZ());
	}

	@Override
	public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
		CubePrimer primer = new CubePrimer();
		
		if(cubeY < 0){
			for(int x = 0;x < Coords.CUBE_MAX_X;x++){
				for(int y = 0;y < Coords.CUBE_MAX_Y;y++){
					for(int z = 0;z < Coords.CUBE_MAX_Z;z++){
						primer.setBlockState(x, y, z, underBlock);
					}
				}
			}
		}else if(cubeY > 15){
			// over block?
		}else{
			if(lastChunk.xPosition != cubeX || lastChunk.zPosition != cubeZ){
				lastChunk = vanilla.provideChunk(cubeX, cubeZ);
			}
			
			//generate 16 cubes at once! (also helps get the heightmap ready for population)
			/*if(!optimizationHack){
				optimizationHack = true;
				for(int y = 15; y >= 0; y--) {
					world.getCubeFromCubeCoords(cubeX, y, cubeZ);
				}
				optimizationHack = false;
			}*/

			ExtendedBlockStorage storage = lastChunk.getBlockStorageArray()[cubeY];
			if(storage != null && !storage.isEmpty()){
				for(int x = 0;x < Coords.CUBE_MAX_X;x++){
					for(int y = 0;y < Coords.CUBE_MAX_Y;y++){
						for(int z = 0;z < Coords.CUBE_MAX_Z;z++){
							IBlockState state = storage.get(x, y, z);
							primer.setBlockState(x, y, z, 
									stripBadrock && state == badrock ? underBlock : state);
						}
					}
				}
			}
		}

		return primer;
	}

	@Override
	public void populate(Cube cube) {
		if(cube.getY() >= 0 && cube.getY() <= 15){
			for(int x = 0;x < 2;x++){
				for(int z = 0;z < 2;z++){
					for(int y = 15; y >= 0;y--) {
						// Vanilla populators break the rules! They need to find the ground!
						world.getCubeFromCubeCoords(cube.getX() + x, y, cube.getZ() + z);
					}
				}
			}
			for(int y = 15; y >= 0;y--) {
				// normal populators would not do this... but we are populating more than one cube!
				world.getCubeFromCubeCoords(cube.getX(), y, cube.getZ()).setPopulated(true);
			}
			
			boolean flag = false;
			for(int x = Coords.cubeToMinBlock(cube.getX()) + 31;x >= Coords.cubeToMinBlock(cube.getX());x--){
				for(int z = Coords.cubeToMinBlock(cube.getZ()) + 31;z >= Coords.cubeToMinBlock(cube.getZ());z--){
					if(world.getEffectiveHeight(x, z) < 0){
						flag = true;
					}
				}
			}

			if(flag){
				System.err.println("HEIGHT MAP INCONSISTANCY IN VANILLA at: " + Coords.cubeToMinBlock(cube.getX()) + ", " + Coords.cubeToMinBlock(cube.getZ()));
				for(int x = Coords.cubeToMinBlock(cube.getX()) + 31;x >= Coords.cubeToMinBlock(cube.getX());x--){
					for(int z = Coords.cubeToMinBlock(cube.getZ()) + 31;z >= Coords.cubeToMinBlock(cube.getZ());z--){
						if(world.getEffectiveHeight(x, z) >= 0){
							world.setBlockState(new BlockPos(x, world.getEffectiveHeight(x, z), z),
									Blocks.GLASS.getDefaultState(), 2);
						}else{
							int y = 140;
							for(;y > 0;y--){
								if(world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.AIR){
									y++;
									break;
								}
							}
							world.setBlockState(new BlockPos(x, world.getEffectiveHeight(x, z), z),
									Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.RED), 2);
						}
					}
				}
			}else{
				vanilla.populate(cube.getX(), cube.getZ()); // ez! >:D
			}
		}
	}

	@Override
	public Vec3i[] getPopRequirment(Cube cube) {
		if(cube.getY() >= 0 && cube.getY() <= 15){
			return new Vec3i[]{
					new Vec3i(-1,  0 -cube.getY(), -1),
					new Vec3i( 0, 15 -cube.getY(),  0)
			};
		}
		return NO_POPULATION;
	}

	@Override
	public void recreateStructures(Cube cube) {}

	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		return vanilla.getPossibleCreatures(creatureType, pos);
	}

	@Override
	public BlockPos getClosestStructure(String name, BlockPos pos) {
		return vanilla.getStrongholdGen((World)world, name, pos);
	}

}
