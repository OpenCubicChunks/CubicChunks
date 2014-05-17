package cuchaz.cubicChunks.gen.builder;

import cuchaz.cubicChunks.gen.lib.exception.ExceptionInvalidParam;
import cuchaz.cubicChunks.gen.lib.module.*;

//The following is a list of module groups and subgroups that build the
//terrain:
//
//Group (continent definition)
//Subgroup (base continent definition)
//Subgroup (continent definition)
//Group (terrain type definition)
//Subgroup (terrain type definition)
//Group (mountainous terrain)
//Subgroup (mountain base definition)
//Subgroup (high mountainous terrain)
//Subgroup (low mountainous terrain)
//Subgroup (mountainous terrain)
//Group (hilly terrain)
//Subgroup (hilly terrain)
//Group (plains terrain)
//Subgroup (plains terrain)
//Group (badlands terrain)
//Subgroup (badlands sand)
//Subgroup (badlands cliffs)
//Subgroup (badlands terrain)
//Group (river positions)
//Subgroup (river positions)
//Group (scaled mountainous terrain)
//Subgroup (scaled mountainous terrain)
//Group (scaled hilly terrain)
//Subgroup (scaled hilly terrain)
//Group (scaled plains terrain)
//Subgroup (scaled plains terrain)
//Group (scaled badlands terrain)
//Subgroup (scaled badlands terrain)
//Group (final planet)
//Subgroup (continental shelf)
//Subgroup (base continent elevation)
//Subgroup (continents with plains)
//Subgroup (continents with hills)
//Subgroup (continents with mountains)
//Subgroup (continents with badlands)
//Subgroup (continents with rivers)
//Subgroup (unscaled final planet)
//Subgroup (final planet)
//
//A description of each group and subgroup can be found above the source code
//for that group and subgroup.

public class ComplexWorldBuilder implements IBuilder
{	
	ModuleBase finalModule;
	
	/** CONSTANTS
	 * 
	 * Modify these finalants to change the terrain of the world
	 * and to change the boundaries and size of the elevation grid.
	 * 
	 *  Note: "Planetary Elevation Units" range from -1.0 (for the lowest underwater trenches)
	 *  to +1.0 (for the highest mountain peaks).
	 */

	// Width of elevation grid, in points
	final int GRID_WIDTH = 1000;

	// Height of elevation grid, in points
	final int GRID_HEIGHT = 1000;

	// Planet seed.  Change this to generate a different planet.
	int CUR_SEED = 0;

	// Minimum elevation, in meters.  This value is approximate.
	double MIN_ELEV = -8192.0;

	// Maximum elevation, in meters.  This value is approximate.
	double MAX_ELEV = 8192.0;

	// Frequency of the continents. Higher frequency produce smaller,
	// more numerous continents. This value is measured in radians.
	final double CONTINENT_FREQUENCY = 1.0;

	// Lacunarity of the continents. Changing this value produces 
	// slightly different continents. For the best results, this value should
	// be random, but close to 2.0.
	final double CONTINENT_LACUNARITY = 2.208984375;

	// Lacunarity of the planet's mountains.  Changing this value produces
	// slightly different mountains.  For the best results, this value should
	// be random, but close to 2.0.
	final double MOUNTAIN_LACUNARITY = 2.142578125;

	// Lacunarity of the planet's hills.  Changing this value produces slightly
	// different hills.  For the best results, this value should be random, but
	// close to 2.0.
	final double HILLS_LACUNARITY = 2.162109375;

	// Lacunarity of the planet's plains.  Changing this value produces slightly
	// different plains.  For the best results, this value should be random, but
	// close to 2.0.
	final double PLAINS_LACUNARITY = 2.314453125;

	// Lacunarity of the planet's badlands.  Changing this value produces
	// slightly different badlands.  For the best results, this value should be
	// random, but close to 2.0.
	final double BADLANDS_LACUNARITY = 2.212890625;

	// Specifies the "twistiness" of the mountains.
	final double MOUNTAINS_TWIST = 1.0;

	// Specifies the "twistiness" of the hills.
	final double HILLS_TWIST = 1.0;

	// Specifies the "twistiness" of the badlands.
	final double BADLANDS_TWIST = 1.0;

	// Specifies the sea level.  This value must be between -1.0
	// (minimum elevation) and +1.0 (maximum planet elevation.)
	double SEA_LEVEL = 0.0;

	// Specifies the level on the planet in which continental shelves appear.
	// This value must be between -1.0 (minimum planet elevation) and +1.0
	// (maximum planet elevation), and must be less than SEA_LEVEL.
	final double SHELF_LEVEL = -0.375;

	// Determines the amount of mountainous terrain that appears on the
	// planet.  Values range from 0.0 (no mountains) to 1.0 (all terrain is
	// covered in mountains).  Mountainous terrain will overlap hilly terrain.
	// Because the badlands terrain may overlap parts of the mountainous
	// terrain, setting MOUNTAINS_AMOUNT to 1.0 may not completely cover the
	// terrain in mountains.
	final double MOUNTAINS_AMOUNT = 0.5;

	// Determines the amount of hilly terrain that appears on the planet.
	// Values range from 0.0 (no hills) to 1.0 (all terrain is covered in
	// hills).  This value must be less than MOUNTAINS_AMOUNT.  Because the
	// mountainous terrain will overlap parts of the hilly terrain, and
	// the badlands terrain may overlap parts of the hilly terrain, setting
	// HILLS_AMOUNT to 1.0 may not completely cover the terrain in hills.
	final double HILLS_AMOUNT = (1.0 + MOUNTAINS_AMOUNT) / 2.0;

	// Determines the amount of badlands terrain that covers the planet.
	// Values range from 0.0 (no badlands) to 1.0 (all terrain is covered in
	// badlands.)  Badlands terrain will overlap any other type of terrain.
	final double BADLANDS_AMOUNT = 0.03125;

	// Offset to apply to the terrain type definition.  Low values (< 1.0) cause
	// the rough areas to appear only at high elevations.  High values (> 2.0)
	// cause the rough areas to appear at any elevation.  The percentage of
	// rough areas on the planet are independent of this value.
	final double TERRAIN_OFFSET = 1.0;

	// Specifies the amount of "glaciation" on the mountains.  This value
	// should be close to 1.0 and greater than 1.0.
	final double MOUNTAIN_GLACIATION = 1.375;

	// Scaling to apply to the base continent elevations, in planetary elevation
	// units.
	final double CONTINENT_HEIGHT_SCALE = (1.0 - SEA_LEVEL) / 4.0;

	// Maximum depth of the rivers, in planetary elevation units.
	final double RIVER_DEPTH = 0.0234375;
	
	@Override
	public void setSeed(int seed)
	{
		this.CUR_SEED = seed;	
	}
	
	public void setMinElev(double minElev)
	{
		this.MIN_ELEV = minElev;	
	}
	
	public void setMaxElev(double maxElev)
	{
		this.MAX_ELEV = maxElev;	
	}
	/**
	 * Sets the sea level for the world. This must be between -1.0 (minimum
	 * elevation) and 1.0 (maximum elevation). I recommend dividing the desired 
	 * seaLevel by the desired build height and feeding the result into this method.
	 */
	public void setSeaLevel(double seaLevel)
	{
		this.SEA_LEVEL = seaLevel;	
	}

	@Override
	public void build() throws ExceptionInvalidParam
	{
		////////////////////////////////////////////////////////////////////////////
		// Module group: continent definition
		////////////////////////////////////////////////////////////////////////////
		
		////////////////////////////////////////////////////////////////////////////
		// Module subgroup: base continent definition (7 noise modules)
		//
		// This subgroup roughly defines the positions and base elevations of the
		// planet's continents.
		//
		// The "base elevation" is the elevation of the terrain before any terrain
		// features (mountains, hills, etc.) are placed on that terrain.
		//
		// -1.0 represents the lowest elevations and +1.0 represents the highest
		// elevations.
		//
		
		// 1: [Continent module]: This Simplex-noise module generates the continents.
		//    This noise module has a high number of octaves so that detail is
		//    visible at high zoom levels.
		Simplex baseContinentDef_pe0 = new Simplex();
		baseContinentDef_pe0.setSeed(2);
		baseContinentDef_pe0.setFrequency(CONTINENT_FREQUENCY);
		baseContinentDef_pe0.setPersistence(0.5);
		baseContinentDef_pe0.setLacunarity(CONTINENT_LACUNARITY);
		baseContinentDef_pe0.setOctaveCount(14);
		
		// 2: [Continent-with-ranges module]: Next, a curve module modifies the
		//    output value from the continent module so that very high values appear
		//    near sea level.  This defines the positions of the mountain ranges.
		Curve baseContinentDef_cu = new Curve(baseContinentDef_pe0);
		baseContinentDef_cu.addControlPoint(-2.0000 + SEA_LEVEL,-1.625 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(-1.0000 + SEA_LEVEL,-1.375 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 0.0000 + SEA_LEVEL,-0.375 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 0.0625 + SEA_LEVEL, 0.125 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 0.1250 + SEA_LEVEL, 0.250 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 0.2500 + SEA_LEVEL, 1.000 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 0.5000 + SEA_LEVEL, 0.250 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 0.7500 + SEA_LEVEL, 0.250 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 1.0000 + SEA_LEVEL, 0.500 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint( 2.0000 + SEA_LEVEL, 0.500 + SEA_LEVEL);
		
		// 3: [Carver module]: This higher-frequency Simplex-noise module will be
		//    used by subsequent noise modules to carve out chunks from the mountain
		//    ranges within the continent-with-ranges module so that the mountain
		//    ranges will not be completely impassible.
		Simplex baseContinentDef_pe1 = new Simplex();
		baseContinentDef_pe1.setSeed (CUR_SEED + 1);
		baseContinentDef_pe1.setFrequency (CONTINENT_FREQUENCY * 4.34375);
		baseContinentDef_pe1.setPersistence (0.5);
		baseContinentDef_pe1.setLacunarity (CONTINENT_LACUNARITY);
		baseContinentDef_pe1.setOctaveCount (11);
		
		// 4: [Scaled-carver module]: This scale/bias module scales the output
		//    value from the carver module such that it is usually near 1.0.  This
		//    is required for step 5.
		ScaleBias baseContinentDef_sb = new ScaleBias(baseContinentDef_pe1);
		baseContinentDef_sb.setScale (0.375);
		baseContinentDef_sb.setBias (0.625);
		
		// 5: [Carved-continent module]: This minimum-value module carves out chunks
		//    from the continent-with-ranges module.  It does this by ensuring that
		//    only the minimum of the output values from the scaled-carver module
		//    and the continent-with-ranges module contributes to the output value
		//    of this subgroup.  Most of the time, the minimum-value module will
		//    select the output value from the continents-with-ranges module since
		//    the output value from the scaled-carver module is usually near 1.0.
		//    Occasionally, the output value from the scaled-carver module will be
		//    less than the output value from the continent-with-ranges module, so
		//    in this case, the output value from the scaled-carver module is
		//    selected.
		Min baseContinentDef_mi = new Min(baseContinentDef_sb, baseContinentDef_cu);
		
		// 6: [Clamped-continent module]: Finally, a clamp module modifies the
		//    carved-continent module to ensure that the output value of this
		//    subgroup is between -1.0 and 1.0.
		Clamp baseContinentDef_cl = new Clamp(baseContinentDef_mi);
		baseContinentDef_cl.setBounds (-1.0, 1.0);
		
		// 7: [Base-continent-definition subgroup]: Caches the output value from the
		//    clamped-continent module.
		Cache baseContinentDef = new Cache(baseContinentDef_cl);
		
		////////////////////////////////////////////////////////////////////////////
		// Module subgroup: continent definition (5 noise modules)
		//
		// This subgroup warps the output value from the the base-continent-
		// definition subgroup, producing more realistic terrain.
		//
		// Warping the base continent definition produces lumpier terrain with
		// cliffs and rifts.
		//
		// -1.0 represents the lowest elevations and +1.0 represents the highest
		// elevations.
		//
		
		// 1: [Coarse-turbulence module]: This turbulence module warps the output
		//    value from the base-continent-definition subgroup, adding some coarse
		//    detail to it.
		Turbulence continentDef_tu0 = new Turbulence(baseContinentDef);
		continentDef_tu0.setSeed (CUR_SEED + 10);
		continentDef_tu0.setFrequency (CONTINENT_FREQUENCY * 15.25);
		continentDef_tu0.setPower (CONTINENT_FREQUENCY / 113.75);
		continentDef_tu0.setRoughness (13);
		
		// 2: [Intermediate-turbulence module]: This turbulence module warps the
		//    output value from the coarse-turbulence module.  This turbulence has
		//    a higher frequency, but lower power, than the coarse-turbulence
		//    module, adding some intermediate detail to it.
		Turbulence continentDef_tu1 = new Turbulence(continentDef_tu0);
		continentDef_tu1.setSourceModule (0, continentDef_tu0);
		continentDef_tu1.setSeed (CUR_SEED + 11);
		continentDef_tu1.setFrequency (CONTINENT_FREQUENCY * 47.25);
		continentDef_tu1.setPower (CONTINENT_FREQUENCY / 433.75);
		continentDef_tu1.setRoughness (12);
		
		// 3: [Warped-base-continent-definition module]: This turbulence module
		//    warps the output value from the intermediate-turbulence module.  This
		//    turbulence has a higher frequency, but lower power, than the
		//    intermediate-turbulence module, adding some fine detail to it.
		Turbulence continentDef_tu2 = new Turbulence(continentDef_tu1);
		continentDef_tu2.setSourceModule (0, continentDef_tu1);
		continentDef_tu2.setSeed (CUR_SEED + 12);
		continentDef_tu2.setFrequency (CONTINENT_FREQUENCY * 95.25);
		continentDef_tu2.setPower (CONTINENT_FREQUENCY / 1019.75);
		continentDef_tu2.setRoughness (11);
		
		// 4: [Select-turbulence module]: At this stage, the turbulence is applied
		//    to the entire base-continent-definition subgroup, producing some very
		//    rugged, unrealistic coastlines.  This selector module selects the
		//    output values from the (unwarped) base-continent-definition subgroup
		//    and the warped-base-continent-definition module, based on the output
		//    value from the (unwarped) base-continent-definition subgroup.  The
		//    selection boundary is near sea level and has a relatively smooth
		//    transition.  In effect, only the higher areas of the base-continent-
		//    definition subgroup become warped; the underwater and coastal areas
		//    remain unaffected.
		Select continentDef_se = new Select(baseContinentDef, continentDef_tu2, baseContinentDef);
		continentDef_se.setSourceModule (0, baseContinentDef);
		continentDef_se.setSourceModule (1, continentDef_tu2);
		continentDef_se.setControlModule (baseContinentDef);
		continentDef_se.setBounds (SEA_LEVEL - 0.0375, SEA_LEVEL + 1000.0375);
		continentDef_se.setEdgeFalloff (0.0625);
		
		// 7: [Continent-definition group]: Caches the output value from the
		//    clamped-continent module.  This is the output value for the entire
		//    continent-definition group.
		Cache continentDef = new Cache(continentDef_se);
		continentDef.setSourceModule(0, continentDef_se);
		
		
		finalModule = continentDef;
	}

	@Override
	public double getValue(double x, double y, double z)
	{
		return finalModule.getValue(x, y, z);
	}

}
