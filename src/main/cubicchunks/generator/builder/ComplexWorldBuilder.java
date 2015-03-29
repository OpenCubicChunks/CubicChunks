/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.generator.builder;

import com.sun.org.apache.bcel.internal.generic.Select;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

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

public class ComplexWorldBuilder implements IBuilder {
	
	ModuleBase finalModule;
	
	/**
	 * CONSTANTS
	 * 
	 * Modify these finalants to change the terrain of the world and to change the boundaries and size of the elevation grid.
	 * 
	 * Note: "Planetary Elevation Units" range from -1.0 (for the lowest underwater trenches) to +1.0 (for the highest mountain peaks).
	 */
	
	// Planet seed. Change this to generate a different planet.
	int CUR_SEED = 0;
	
	// Minimum elevation, in meters. This value is approximate.
	double MIN_ELEV = -8192.0;
	
	// Maximum elevation, in meters. This value is approximate.
	double MAX_ELEV = 8192.0;
	
	// Frequency of the continents. Higher frequency produce smaller,
	// more numerous continents. This value is measured in radians.
	final double CONTINENT_FREQUENCY = 1.0;
	
	// Lacunarity of the continents. Changing this value produces
	// slightly different continents. For the best results, this value should
	// be random, but close to 2.0.
	final double CONTINENT_LACUNARITY = 2.208984375;
	
	// Lacunarity of the planet's mountains. Changing this value produces
	// slightly different mountains. For the best results, this value should
	// be random, but close to 2.0.
	final double MOUNTAIN_LACUNARITY = 2.142578125;
	
	// Lacunarity of the planet's hills. Changing this value produces slightly
	// different hills. For the best results, this value should be random, but
	// close to 2.0.
	final double HILLS_LACUNARITY = 2.162109375;
	
	// Lacunarity of the planet's plains. Changing this value produces slightly
	// different plains. For the best results, this value should be random, but
	// close to 2.0.
	final double PLAINS_LACUNARITY = 2.314453125;
	
	// Lacunarity of the planet's badlands. Changing this value produces
	// slightly different badlands. For the best results, this value should be
	// random, but close to 2.0.
	final double BADLANDS_LACUNARITY = 2.212890625;
	
	// Specifies the "twistiness" of the mountains.
	final double MOUNTAINS_TWIST = 1.0;
	
	// Specifies the "twistiness" of the hills.
	final double HILLS_TWIST = 1.0;
	
	// Specifies the "twistiness" of the badlands.
	final double BADLANDS_TWIST = 1.0;
	
	// Specifies the sea level. This value must be between -1.0
	// (minimum elevation) and +1.0 (maximum planet elevation.)
	double SEA_LEVEL = 0.0;
	
	// Specifies the level on the planet in which continental shelves appear.
	// This value must be between -1.0 (minimum planet elevation) and +1.0
	// (maximum planet elevation), and must be less than SEA_LEVEL.
	final double SHELF_LEVEL = -0.375;
	
	// Determines the amount of mountainous terrain that appears on the
	// planet. Values range from 0.0 (no mountains) to 1.0 (all terrain is
	// covered in mountains). Mountainous terrain will overlap hilly terrain.
	// Because the badlands terrain may overlap parts of the mountainous
	// terrain, setting MOUNTAINS_AMOUNT to 1.0 may not completely cover the
	// terrain in mountains.
	final double MOUNTAINS_AMOUNT = 0.5;
	
	// Determines the amount of hilly terrain that appears on the planet.
	// Values range from 0.0 (no hills) to 1.0 (all terrain is covered in
	// hills). This value must be less than MOUNTAINS_AMOUNT. Because the
	// mountainous terrain will overlap parts of the hilly terrain, and
	// the badlands terrain may overlap parts of the hilly terrain, setting
	// HILLS_AMOUNT to 1.0 may not completely cover the terrain in hills.
	final double HILLS_AMOUNT = (1.0 + MOUNTAINS_AMOUNT) / 2.0;
	
	// Determines the amount of badlands terrain that covers the planet.
	// Values range from 0.0 (no badlands) to 1.0 (all terrain is covered in
	// badlands.) Badlands terrain will overlap any other type of terrain.
	final double BADLANDS_AMOUNT = 0.03125;
	
	// Offset to apply to the terrain type definition. Low values (< 1.0) cause
	// the rough areas to appear only at high elevations. High values (> 2.0)
	// cause the rough areas to appear at any elevation. The percentage of
	// rough areas on the planet are independent of this value.
	final double TERRAIN_OFFSET = 1.0;
	
	// Specifies the amount of "glaciation" on the mountains. This value
	// should be close to 1.0 and greater than 1.0.
	final double MOUNTAIN_GLACIATION = 1.375;
	
	// Scaling to apply to the base continent elevations, in planetary elevation
	// units.
	final double CONTINENT_HEIGHT_SCALE = (1.0 - SEA_LEVEL) / 4.0;
	
	// Maximum depth of the rivers, in planetary elevation units.
	final double RIVER_DEPTH = 0.0234375;
	
	static int ModOctaves = 0;
	
	@Override
	public void setSeed(int seed) {
		this.CUR_SEED = seed;
	}
	
	public void setMinElev(double minElev) {
		this.MIN_ELEV = minElev;
	}
	
	public void setMaxElev(double maxElev) {
		this.MAX_ELEV = maxElev;
	}
	
	/**
	 * Sets the sea level for the world. This must be between -1.0 (minimum elevation) and 1.0 (maximum elevation). I recommend dividing the desired seaLevel by the desired build height and feeding the result into this method.
	 */
	public void setSeaLevel(double seaLevel) {
		this.SEA_LEVEL = seaLevel;
	}
	
	public void setModOctaves(int value) {
		this.ModOctaves = value;
	}
	
	@Override
	public void build() {
		// //////////////////////////////////////////////////////////////////////////
		// Module group: continent definition
		// //////////////////////////////////////////////////////////////////////////
		
		// //////////////////////////////////////////////////////////////////////////
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
		// This noise module has a high number of octaves so that detail is
		// visible at high zoom levels.
		Simplex baseContinentDef_pe0 = new Simplex();
		baseContinentDef_pe0.setSeed(2);
		baseContinentDef_pe0.setFrequency(CONTINENT_FREQUENCY);
		baseContinentDef_pe0.setPersistence(0.5);
		baseContinentDef_pe0.setLacunarity(CONTINENT_LACUNARITY);
		baseContinentDef_pe0.setOctaveCount(14 + ModOctaves);
		baseContinentDef_pe0.build();
		
		// 2: [Continent-with-ranges module]: Next, a curve module modifies the
		// output value from the continent module so that very high values appear
		// near sea level. This defines the positions of the mountain ranges.
		Curve baseContinentDef_cu = new Curve(baseContinentDef_pe0);
		baseContinentDef_cu.addControlPoint(-2.0000 + SEA_LEVEL, -1.625 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(-1.0000 + SEA_LEVEL, -1.375 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(0.0000 + SEA_LEVEL, -0.375 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(0.0625 + SEA_LEVEL, 0.125 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(0.1250 + SEA_LEVEL, 0.250 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(0.2500 + SEA_LEVEL, 1.000 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(0.5000 + SEA_LEVEL, 0.250 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(0.7500 + SEA_LEVEL, 0.250 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(1.0000 + SEA_LEVEL, 0.500 + SEA_LEVEL);
		baseContinentDef_cu.addControlPoint(2.0000 + SEA_LEVEL, 0.500 + SEA_LEVEL);
		
		// 3: [Carver module]: This higher-frequency Simplex-noise module will be
		// used by subsequent noise modules to carve out chunks from the mountain
		// ranges within the continent-with-ranges module so that the mountain
		// ranges will not be completely impassible.
		Simplex baseContinentDef_pe1 = new Simplex();
		baseContinentDef_pe1.setSeed(CUR_SEED + 1);
		baseContinentDef_pe1.setFrequency(CONTINENT_FREQUENCY * 4.34375);
		baseContinentDef_pe1.setPersistence(0.5);
		baseContinentDef_pe1.setLacunarity(CONTINENT_LACUNARITY);
		baseContinentDef_pe1.setOctaveCount(11 + ModOctaves);
		baseContinentDef_pe1.build();
		
		// 4: [Scaled-carver module]: This scale/bias module scales the output
		// value from the carver module such that it is usually near 1.0. This
		// is required for step 5.
		ScaleBias baseContinentDef_sb = new ScaleBias(baseContinentDef_pe1);
		baseContinentDef_sb.setScale(0.375);
		baseContinentDef_sb.setBias(0.625);
		
		// 5: [Carved-continent module]: This minimum-value module carves out chunks
		// from the continent-with-ranges module. It does this by ensuring that
		// only the minimum of the output values from the scaled-carver module
		// and the continent-with-ranges module contributes to the output value
		// of this subgroup. Most of the time, the minimum-value module will
		// select the output value from the continents-with-ranges module since
		// the output value from the scaled-carver module is usually near 1.0.
		// Occasionally, the output value from the scaled-carver module will be
		// less than the output value from the continent-with-ranges module, so
		// in this case, the output value from the scaled-carver module is
		// selected.
		Min baseContinentDef_mi = new Min(baseContinentDef_sb, baseContinentDef_cu);
		
		// 6: [Clamped-continent module]: Finally, a clamp module modifies the
		// carved-continent module to ensure that the output value of this
		// subgroup is between -1.0 and 1.0.
		Clamp baseContinentDef_cl = new Clamp(baseContinentDef_mi);
		baseContinentDef_cl.setBounds(-1.0, 1.0);
		
		// 7: [Base-continent-definition subgroup]: Caches the output value from the
		// clamped-continent module.
		Cached baseContinentDef = new Cached(baseContinentDef_cl);
		
		// //////////////////////////////////////////////////////////////////////////
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
		// value from the base-continent-definition subgroup, adding some coarse
		// detail to it.
		Turbulence continentDef_tu0 = new Turbulence(baseContinentDef);
		continentDef_tu0.setSeed(CUR_SEED + 10);
		continentDef_tu0.setFrequency(CONTINENT_FREQUENCY * 15.25);
		continentDef_tu0.setPower(CONTINENT_FREQUENCY / 113.75);
		continentDef_tu0.setRoughness(13 + ModOctaves);
		continentDef_tu0.build();
		
		// 2: [Intermediate-turbulence module]: This turbulence module warps the
		// output value from the coarse-turbulence module. This turbulence has
		// a higher frequency, but lower power, than the coarse-turbulence
		// module, adding some intermediate detail to it.
		Turbulence continentDef_tu1 = new Turbulence(continentDef_tu0);
		continentDef_tu1.setSeed(CUR_SEED + 11);
		continentDef_tu1.setFrequency(CONTINENT_FREQUENCY * 47.25);
		continentDef_tu1.setPower(CONTINENT_FREQUENCY / 433.75);
		continentDef_tu1.setRoughness(12 + ModOctaves);
		continentDef_tu1.build();
		
		// 3: [Warped-base-continent-definition module]: This turbulence module
		// warps the output value from the intermediate-turbulence module. This
		// turbulence has a higher frequency, but lower power, than the
		// intermediate-turbulence module, adding some fine detail to it.
		Turbulence continentDef_tu2 = new Turbulence(continentDef_tu1);
		continentDef_tu2.setSeed(CUR_SEED + 12);
		continentDef_tu2.setFrequency(CONTINENT_FREQUENCY * 95.25);
		continentDef_tu2.setPower(CONTINENT_FREQUENCY / 1019.75);
		continentDef_tu2.setRoughness(12 + ModOctaves);
		continentDef_tu2.build();
		
		// 4: [Select-turbulence module]: At this stage, the turbulence is applied
		// to the entire base-continent-definition subgroup, producing some very
		// rugged, unrealistic coastlines. This selector module selects the
		// output values from the (unwarped) base-continent-definition subgroup
		// and the warped-base-continent-definition module, based on the output
		// value from the (unwarped) base-continent-definition subgroup. The
		// selection boundary is near sea level and has a relatively smooth
		// transition. In effect, only the higher areas of the base-continent-
		// definition subgroup become warped; the underwater and coastal areas
		// remain unaffected.
		Select continentDef_se = new Select(baseContinentDef, continentDef_tu2, baseContinentDef);
		continentDef_se.setBounds(SEA_LEVEL - 0.0375, SEA_LEVEL + 1000.0375);
		continentDef_se.setEdgeFalloff(0.0625);
		
		// 7: [Continent-definition group]: Caches the output value from the
		// clamped-continent module. This is the output value for the entire
		// continent-definition group.
		Cached continentDef = new Cached(continentDef_se);
		
		// //////////////////////////////////////////////////////////////////////////
		// Module group: terrain type definition
		// //////////////////////////////////////////////////////////////////////////
		
		// //////////////////////////////////////////////////////////////////////////
		// Module subgroup: terrain type definition (3 noise modules)
		//
		// This subgroup defines the positions of the terrain types on the planet.
		//
		// Terrain types include, in order of increasing roughness, plains, hills,
		// and mountains.
		//
		// This subgroup's output value is based on the output value from the
		// continent-definition group. Rougher terrain mainly appears at higher
		// elevations.
		//
		// -1.0 represents the smoothest terrain types (plains and underwater) and
		// +1.0 represents the roughest terrain types (mountains).
		//
		
		// 1: [Warped-continent module]: This turbulence module slightly warps the
		// output value from the continent-definition group. This prevents the
		// rougher terrain from appearing exclusively at higher elevations.
		// Rough areas may now appear in the the ocean, creating rocky islands
		// and fjords.
		Turbulence terrainTypeDef_tu = new Turbulence(continentDef);
		terrainTypeDef_tu.setSourceModule(0, continentDef);
		terrainTypeDef_tu.setSeed(CUR_SEED + 20);
		terrainTypeDef_tu.setFrequency(CONTINENT_FREQUENCY * 18.125);
		terrainTypeDef_tu.setPower(CONTINENT_FREQUENCY / 20.59375 * TERRAIN_OFFSET);
		terrainTypeDef_tu.setRoughness(3);
		terrainTypeDef_tu.build();
		
		// 2: [Roughness-probability-shift module]: This terracing module sharpens
		// the edges of the warped-continent module near sea level and lowers
		// the slope towards the higher-elevation areas. This shrinks the areas
		// in which the rough terrain appears, increasing the "rarity" of rough
		// terrain.
		Terrace terrainTypeDef_te = new Terrace(terrainTypeDef_tu);
		terrainTypeDef_te.addControlPoint(-1.00);
		terrainTypeDef_te.addControlPoint(SHELF_LEVEL + SEA_LEVEL / 2.0);
		terrainTypeDef_te.addControlPoint(1.00);
		
		// 3: [Terrain-type-definition group]: Caches the output value from the
		// roughness-probability-shift module. This is the output value for
		// the entire terrain-type-definition group.
		Cached terrainTypeDef = new Cached(terrainTypeDef_te);
		
		// //////////////////////////////////////////////////////////////////////////
		// Module group: mountainous terrain
		// //////////////////////////////////////////////////////////////////////////
		
		// //////////////////////////////////////////////////////////////////////////
		// Module subgroup: mountain base definition (9 noise modules)
		//
		// This subgroup generates the base-mountain elevations. Other subgroups
		// will add the ridges and low areas to the base elevations.
		//
		// -1.0 represents low mountainous terrain and +1.0 represents high
		// mountainous terrain.
		//
		
		// 1: [Mountain-ridge module]: This ridged-multifractal-noise module
		// generates the mountain ridges.
		RidgedMulti mountainBaseDef_rm0 = new RidgedMulti();
		mountainBaseDef_rm0.setSeed(CUR_SEED + 30);
		mountainBaseDef_rm0.setFrequency(1723.0);
		mountainBaseDef_rm0.setLacunarity(MOUNTAIN_LACUNARITY);
		mountainBaseDef_rm0.setOctaveCount(4);
		mountainBaseDef_rm0.build();
		
		// 2: [Scaled-mountain-ridge module]: Next, a scale/bias module scales the
		// output value from the mountain-ridge module so that its ridges are not
		// too high. The reason for this is that another subgroup adds actual
		// mountainous terrain to these ridges.
		ScaleBias mountainBaseDef_sb0 = new ScaleBias(mountainBaseDef_rm0);
		mountainBaseDef_sb0.setScale(0.5);
		mountainBaseDef_sb0.setBias(0.375);
		
		// 3: [River-valley module]: This ridged-multifractal-noise module generates
		// the river valleys. It has a much lower frequency than the mountain-
		// ridge module so that more mountain ridges will appear outside of the
		// valleys. Note that this noise module generates ridged-multifractal
		// noise using only one octave; this information will be important in the
		// next step.
		RidgedMulti mountainBaseDef_rm1 = new RidgedMulti();
		mountainBaseDef_rm1.setSeed(CUR_SEED + 31);
		mountainBaseDef_rm1.setFrequency(367.0);
		mountainBaseDef_rm1.setLacunarity(MOUNTAIN_LACUNARITY);
		mountainBaseDef_rm1.setOctaveCount(1);
		mountainBaseDef_rm1.build();
		
		// 4: [Scaled-river-valley module]: Next, a scale/bias module applies a
		// scaling factor of -2.0 to the output value from the river-valley
		// module. This stretches the possible elevation values because one-
		// octave ridged-multifractal noise has a lower range of output values
		// than multiple-octave ridged-multifractal noise. The negative scaling
		// factor inverts the range of the output value, turning the ridges from
		// the river-valley module into valleys.
		ScaleBias mountainBaseDef_sb1 = new ScaleBias(mountainBaseDef_rm1);
		mountainBaseDef_sb1.setScale(-2.0);
		mountainBaseDef_sb1.setBias(-0.5);
		
		// 5: [Low-flat module]: This low constant value is used by step 6.
		Const mountainBaseDef_co = new Const();
		mountainBaseDef_co.setConstValue(-1.0);
		
		// 6: [Mountains-and-valleys module]: This blender module merges the
		// scaled-mountain-ridge module and the scaled-river-valley module
		// together. It causes the low-lying areas of the terrain to become
		// smooth, and causes the high-lying areas of the terrain to contain
		// ridges. To do this, it uses the scaled-river-valley module as the
		// control module, causing the low-flat module to appear in the lower
		// areas and causing the scaled-mountain-ridge module to appear in the
		// higher areas.
		Blend mountainBaseDef_bl = new Blend(mountainBaseDef_co, mountainBaseDef_sb0, mountainBaseDef_sb1);
		
		// 7: [Coarse-turbulence module]: This turbulence module warps the output
		// value from the mountain-and-valleys module, adding some coarse detail
		// to it.
		Turbulence mountainBaseDef_tu0 = new Turbulence(mountainBaseDef_bl);
		mountainBaseDef_tu0.setSeed(CUR_SEED + 32);
		mountainBaseDef_tu0.setFrequency(1337.0);
		mountainBaseDef_tu0.setPower(1.0 / 6730.0 * MOUNTAINS_TWIST);
		mountainBaseDef_tu0.setRoughness(4);
		mountainBaseDef_tu0.build();
		
		// 8: [Warped-mountains-and-valleys module]: This turbulence module warps
		// the output value from the coarse-turbulence module. This turbulence
		// has a higher frequency, but lower power, than the coarse-turbulence
		// module, adding some fine detail to it.
		Turbulence mountainBaseDef_tu1 = new Turbulence(mountainBaseDef_tu0);
		mountainBaseDef_tu1.setSeed(CUR_SEED + 33);
		mountainBaseDef_tu1.setFrequency(21221.0);
		mountainBaseDef_tu1.setPower(1.0 / 120157.0 * MOUNTAINS_TWIST);
		mountainBaseDef_tu1.setRoughness(6);
		mountainBaseDef_tu1.build();
		
		// 9: [Mountain-base-definition subgroup]: Caches the output value from the
		// warped-mountains-and-valleys module.
		Cached mountainBaseDef = new Cached(mountainBaseDef_tu1);
		
		// //////////////////////////////////////////////////////////////////////////
		// Module subgroup: high mountainous terrain (5 noise modules)
		//
		// This subgroup generates the mountainous terrain that appears at high
		// elevations within the mountain ridges.
		//
		// -1.0 represents the lowest elevations and +1.0 represents the highest
		// elevations.
		//
		
		// 1: [Mountain-basis-0 module]: This ridged-multifractal-noise module,
		// along with the mountain-basis-1 module, generates the individual
		// mountains.
		RidgedMulti mountainousHigh_rm0 = new RidgedMulti();
		mountainousHigh_rm0.setSeed(CUR_SEED + 40);
		mountainousHigh_rm0.setFrequency(2371.0);
		mountainousHigh_rm0.setLacunarity(MOUNTAIN_LACUNARITY);
		mountainousHigh_rm0.setOctaveCount(3);
		mountainousHigh_rm0.build();
		
		// 2: [Mountain-basis-1 module]: This ridged-multifractal-noise module,
		// along with the mountain-basis-0 module, generates the individual
		// mountains.
		RidgedMulti mountainousHigh_rm1 = new RidgedMulti();
		mountainousHigh_rm1.setSeed(CUR_SEED + 41);
		mountainousHigh_rm1.setFrequency(2341.0);
		mountainousHigh_rm1.setLacunarity(MOUNTAIN_LACUNARITY);
		mountainousHigh_rm1.setOctaveCount(3);
		mountainousHigh_rm1.build();
		
		// 3: [High-mountains module]: Next, a maximum-value module causes more
		// mountains to appear at the expense of valleys. It does this by
		// ensuring that only the maximum of the output values from the two
		// ridged-multifractal-noise modules contribute to the output value of
		// this subgroup.
		Max mountainousHigh_ma = new Max(mountainousHigh_rm0, mountainousHigh_rm1);
		
		// 4: [Warped-high-mountains module]: This turbulence module warps the
		// output value from the high-mountains module, adding some detail to it.
		Turbulence mountainousHigh_tu = new Turbulence(mountainousHigh_ma);
		mountainousHigh_tu.setSeed(CUR_SEED + 42);
		mountainousHigh_tu.setFrequency(31511.0);
		mountainousHigh_tu.setPower(1.0 / 180371.0 * MOUNTAINS_TWIST);
		mountainousHigh_tu.setRoughness(4);
		mountainousHigh_tu.build();
		
		// 5: [High-mountainous-terrain subgroup]: Caches the output value from the
		// warped-high-mountains module.
		Cached mountainousHigh = new Cached(mountainousHigh_tu);
		
		// //////////////////////////////////////////////////////////////////////////
		// Module subgroup: low mountainous terrain (4 noise modules)
		//
		// This subgroup generates the mountainous terrain that appears at low
		// elevations within the river valleys.
		//
		// -1.0 represents the lowest elevations and +1.0 represents the highest
		// elevations.
		//
		
		// 1: [Lowland-basis-0 module]: This ridged-multifractal-noise module,
		// along with the lowland-basis-1 module, produces the low mountainous
		// terrain.
		RidgedMulti mountainousLow_rm0 = new RidgedMulti();
		mountainousLow_rm0.setSeed(CUR_SEED + 50);
		mountainousLow_rm0.setFrequency(1381.0);
		mountainousLow_rm0.setLacunarity(MOUNTAIN_LACUNARITY);
		mountainousLow_rm0.setOctaveCount(8);
		mountainousLow_rm0.build();
		
		// 1: [Lowland-basis-1 module]: This ridged-multifractal-noise module,
		// along with the lowland-basis-0 module, produces the low mountainous
		// terrain.
		RidgedMulti mountainousLow_rm1 = new RidgedMulti();
		mountainousLow_rm1.setSeed(CUR_SEED + 51);
		mountainousLow_rm1.setFrequency(1427.0);
		mountainousLow_rm1.setLacunarity(MOUNTAIN_LACUNARITY);
		mountainousLow_rm1.setOctaveCount(8);
		mountainousLow_rm1.build();
		
		// 3: [Low-mountainous-terrain module]: This multiplication module combines
		// the output values from the two ridged-multifractal-noise modules.
		// This causes the following to appear in the resulting terrain:
		// - Cracks appear when two negative output values are multiplied
		// together.
		// - Flat areas appear when a positive and a negative output value are
		// multiplied together.
		// - Ridges appear when two positive output values are multiplied
		// together.
		Multiply mountainousLow_mu = new Multiply(mountainousLow_rm0, mountainousLow_rm1);
		
		// 4: [Low-mountainous-terrain subgroup]: Caches the output value from the
		// low-moutainous-terrain module.
		Cached mountainousLow = new Cached(mountainousLow_mu);
		
		// //////////////////////////////////////////////////////////////////////////
		// Module subgroup: mountainous terrain (7 noise modules)
		//
		// This subgroup generates the final mountainous terrain by combining the
		// high-mountainous-terrain subgroup with the low-mountainous-terrain
		// subgroup.
		//
		// -1.0 represents the lowest elevations and +1.0 represents the highest
		// elevations.
		//
		
		// 1: [Scaled-low-mountainous-terrain module]: First, this scale/bias module
		// scales the output value from the low-mountainous-terrain subgroup to a
		// very low value and biases it towards -1.0. This results in the low
		// mountainous areas becoming more-or-less flat with little variation.
		// This will also result in the low mountainous areas appearing at the
		// lowest elevations in this subgroup.
		ScaleBias mountainousTerrain_sb0 = new ScaleBias(mountainousLow);
		mountainousTerrain_sb0.setScale(0.03125);
		mountainousTerrain_sb0.setBias(-0.96875);
		
		// 2: [Scaled-high-mountainous-terrain module]: Next, this scale/bias module
		// scales the output value from the high-mountainous-terrain subgroup to
		// 1/4 of its initial value and biases it so that its output value is
		// usually positive.
		ScaleBias mountainousTerrain_sb1 = new ScaleBias(mountainousHigh);
		mountainousTerrain_sb1.setScale(0.25);
		mountainousTerrain_sb1.setBias(0.25);
		
		// 3: [Added-high-mountainous-terrain module]: This addition module adds the
		// output value from the scaled-high-mountainous-terrain module to the
		// output value from the mountain-base-definition subgroup. Mountains
		// now appear all over the terrain.
		Add mountainousTerrain_ad = new Add(mountainousTerrain_sb1, mountainBaseDef);
		mountainousTerrain_ad.setSourceModule(0, mountainousTerrain_sb1);
		mountainousTerrain_ad.setSourceModule(1, mountainBaseDef);
		
		// 4: [Combined-mountainous-terrain module]: Note that at this point, the
		// entire terrain is covered in high mountainous terrain, even at the low
		// elevations. To make sure the mountains only appear at the higher
		// elevations, this selector module causes low mountainous terrain to
		// appear at the low elevations (within the valleys) and the high
		// mountainous terrain to appear at the high elevations (within the
		// ridges.) To do this, this noise module selects the output value from
		// the added-high-mountainous-terrain module if the output value from the
		// mountain-base-definition subgroup is higher than a set amount.
		// Otherwise, this noise module selects the output value from the scaled-
		// low-mountainous-terrain module.
		Select mountainousTerrain_se = new Select(mountainousTerrain_sb0, mountainousTerrain_ad, mountainBaseDef);
		mountainousTerrain_se.setBounds(-0.5, 999.5);
		mountainousTerrain_se.setEdgeFalloff(0.5);
		
		// 5: [Scaled-mountainous-terrain-module]: This scale/bias module slightly
		// reduces the range of the output value from the combined-mountainous-
		// terrain module, decreasing the heights of the mountain peaks.
		ScaleBias mountainousTerrain_sb2 = new ScaleBias(mountainousTerrain_se);
		mountainousTerrain_sb2.setScale(0.8);
		mountainousTerrain_sb2.setBias(0.0);
		
		// 6: [Glaciated-mountainous-terrain-module]: This exponential-curve module
		// applies an exponential curve to the output value from the scaled-
		// mountainous-terrain module. This causes the slope of the mountains to
		// smoothly increase towards higher elevations, as if a glacier grinded
		// out those mountains. This exponential-curve module expects the output
		// value to range from -1.0 to +1.0.
		Exponent mountainousTerrain_ex = new Exponent(mountainousTerrain_sb2);
		mountainousTerrain_ex.setExponent(MOUNTAIN_GLACIATION);
		
		// 7: [Mountainous-terrain group]: Caches the output value from the
		// glaciated-mountainous-terrain module. This is the output value for
		// the entire mountainous-terrain group.
		Cached mountainousTerrain = new Cached(mountainousTerrain_ex);
		
		ScaleBias scaleBias = new ScaleBias(mountainousTerrain);
		scaleBias.setScale(MAX_ELEV);
		scaleBias.setBias(SEA_LEVEL);
		
		finalModule = continentDef;
	}
	
	@Override
	public double getValue(double x, double y, double z) {
		return finalModule.getValue(x, y, z);
	}
	
}
