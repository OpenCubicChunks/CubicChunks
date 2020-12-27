# Modded Overworld Chunk Generators & Cubic Chunks 1.17

This documentation will go over the required changes(relatively minimal) to have your mod's chunk generator be a plug and play experience w/ Cubic Chunks. All changes will be based off changes that had to be done to the Snapshot 20w49a(expected changes are minimal) Vanilla Chunk Generator. We'll be  using Vanilla methods from LevelHeightAccessor.

## fillFromNoise 

**Collecting Structures For Noise**

In the method fillFromNoise, collect the feature starts w/ the section pos of:
```featureManager.startsForFeature(SectionPos.of(chunkPos, chunk.getMinSection());```

When filtering JigsawJunctions by x/z coordinates, check against the Y of the structure also like so:
```for (JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
    int jigsawJunctionSourceX = jigsawJunction.getSourceX();
    int jigsawJunctionSourceY = jigsawJunction.getSourceGroundY();
    int jigsawJunctionSourceZ = jigsawJunction.getSourceZ();
    int minY = chunk.getMinBuildHeight();
    int maxY = chunk.getMaxBuildHeight() - 1;

    boolean isInYBounds = jigsawJunctionSourceY > minY - 12 && jigsawJunctionSourceY < maxY + 15 + 12;
    if (jigsawJunctionSourceX > ySectionCoordX - 12 && jigsawJunctionSourceZ > ySectionCoordZ - 12 && jigsawJunctionSourceX < ySectionCoordX + 15 + 12
        && jigsawJunctionSourceZ < ySectionCoordZ + 15 + 12 && isInYBounds) {
        jigsawJunctionsList.add(jigsawJunction);
    }
}
```

**Placing Blocks**
Blocks should be placed with LevelChunkSection(see how NoiseBasedChunkGenerator accomplishes this) using LocalY(yCoord & 15).

**Height**
Total height should be from: `chunk.getHeight();`

Min height should be from: `chunk.getMinBuildHeight();`

Max height should be from: `chunk.getMaxBuildHeight();`

**Sampling Noise**
Debatable, but remove(through an if check for Cubic Chunks) all instances of code where you transform your generator to fit to vanilla Y bounds/assume chunks are 2D. An instance from NoiseBasedChunkGenerator that does such transforming that CC had to disable is: https://github.com/OpenCubicChunks/CubicChunks/blob/MC_1.17_ChunkGenerator/src/main/java/io/github/opencubicchunks/cubicchunks/chunk/CCNoiseBasedChunkGenerator.java#L277-L287


## getBaseHeight 
Leave as is.


## getNoiseColumn
If you're using settings, use the minY, otherwise use 0(Basically leave it vanilla)


## buildSurfaceAndBedrock
Pretty self explanatory, run as an if else statement checking for CC and if CC is present DO NOT place bedrock(best in a separate method like NoiseBasedChunkGenerator does).