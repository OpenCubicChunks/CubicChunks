package cuchaz.cubicChunks.gen;

import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import cuchaz.cubicChunks.world.Cube;

public interface ICubeGenerator
{
	/**
     * Checks to see if a cube exists at x, y, z
     */
    boolean cubeExists(int cubeX, int cubeY, int cubeZ);

    /**
     * Will return back a cube, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified cube from the map seed and cube seed
     */
    Cube provideCube(int cubeX, int cubeY, int cubeZ);

    /**
     * loads or generates the cube at the cube location specified
     */
    Cube loadCube(int cubeX, int cubeY, int cubeZ);

    /**
     * Populates cube with ores etc etc
     */
    void populate(ICubeGenerator icubeprovider, int cubeX, int cubeY, int cubeZ);

    /**
     * Two modes of operation: if passed true, save all cubes in one go.  If passed false, save up to two cubes.
     * Return true if all cubes have been saved.
     */
    boolean saveCubes(boolean flag, IProgressUpdate iprogressupdate);

    /**
     * Unloads cubes that are marked to be unloaded. This is not guaranteed to unload every such cube.
     */
    boolean unloadQueuedCubes();

    /**
     * Returns if the CubeProvider supports saving.
     */
    boolean canSave();

    /**
     * Converts the instance data to a readable string.
     */
    String makeString();

    /**
     * Returns a list of creatures of the specified type that can spawn at the given location.
     */
    List getPossibleCreatures(EnumCreatureType enumcreaturetype, int cubeX, int cubeY, int cubeZ);

    /**
     * Returns the location of the closest structure of the specified type. If not found returns null.
     */
//    CubicChunkPosition findClosestStructure(World world, String s, int cubeX, int cubeY, int cubeZ);

    int getLoadedCubeCount();

    void recreateStructures(int cubeX, int cubeY, int cubeZ);
}
