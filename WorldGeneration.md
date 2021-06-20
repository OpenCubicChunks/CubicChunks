# 1.17 Cubic Chunks World Generation

Hi and welcome to a document that will *hopefully* layout the way world generation functions in Cubic Chunks in 1.17+.

Cubic Chunks world generation is composed of cubes that are pushed through the numerous generation stages/chunk statuses. (Vanilla System but 3D)

ProtoCube (aka CubePrimer at the time of writing) is what's pushed through all stages of world generation to eventually upgrade to a BigCube.

ProtoCube is the CC equivalent of a ProtoChunk(extends ProtoChunk)
BigCube is the CC equivalent of a LevelChunk(does not extend LevelChunk)

## Chunk statuses and how they function

### Structure Starts

Structure starts are ran per cube and NOT PER 16x16 SECTION.

### Structure References

### Biomes

Biomes are ran per 16x16 chunk, and the biome container for the cube is filled respectively with 4 `ChunkBiomeContainers` to create a `CubeBiomeContainer`. The fed LevelHeightAccessor needs
to be the currently fed cube(`ChunkAccess`), this will make the dimensions of the container fit the cube's height.

### Noise

Noise is ran for each 16x16 chunk. So noise is a rather interesting step where it actually requires us to exceed our cube's current height and simulate the generation of the cube above to
determine whether the chunk generator will place blocks above the current cube. We then use this simulated cube above to pass over to the surface builder, more into that in the Surface.

### Surface

Surface is ran for each 16x16 chunk. As mentioned in Noise, the simulated cube above is required to assist the current cube generating the surface builder. Without the simulated cube above
the surface builder would see any blocks in above the current cube as air(no info), this would then tell surface builders 2 major details:

* Heightmap(main cube's local heightmap) is at the "surface".
* The block above the heightmap is now air, and the surface builder that needs air above to place a block may now place.

This is because the cube above is not there when checking, remember vanilla's surface only gives you the chunk its operating on, none of its neighbors, so in CC we're only given the cube
being operated on. Therefore, with this in mind this gives us the sufficient context to complete the surface stage of world generation.

### Carvers & Liquid Carvers