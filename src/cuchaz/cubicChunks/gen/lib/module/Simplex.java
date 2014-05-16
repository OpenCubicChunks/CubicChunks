/*****************************************************************************
 * Copyright (c) Nick Whitney.                      
 *
 * This source is licensed under the GNU LGPL v3
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package cuchaz.cubicChunks.gen.lib.module;

import java.util.Random;

import cuchaz.cubicChunks.gen.lib.*;

public class Simplex extends ModuleBase
{
	/** Noise module that outputs 3-dimensional Simplex noise.
	 *
	 * Simplex noise is the sum of several coherent-noise functions of
	 * ever-increasing frequencies and ever-decreasing amplitudes.
	 *
	 * An important property of Simplex noise is that a small change in the
	 * input value will produce a small change in the output value, while a
	 * large change in the input value will produce a random change in the
	 * output value.
	 *
	 * This noise module outputs Simplex-noise values that usually range from
	 * -1.0 to +1.0, but there are no guarantees that all output values will
	 * exist within that range.
	 *
	 * For a better description of Simplex noise, see the links in the
	 * <i>References and Acknowledgments</i> section.
	 *
	 * This noise module does not require any source modules.
	 *
	 * <b>Octaves</b>
	 *
	 * The number of octaves control the <i>amount of detail</i> of the
	 * Simplex noise.  Adding more octaves increases the detail of the Simplex
	 * noise, but with the drawback of increasing the calculation time.
	 *
	 * An octave is one of the coherent-noise functions in a series of
	 * coherent-noise functions that are added together to form Simplex
	 * noise.
	 *
	 * An application may specify the frequency of the first octave by
	 * calling the setFrequency() method.
	 *
	 * An application may specify the number of octaves that generate Simplex
	 * noise by calling the setOctaveCount() method.
	 *
	 * These coherent-noise functions are called octaves because each octave
	 * has, by default, double the frequency of the previous octave.  Musical
	 * tones have this property as well; a musical C tone that is one octave
	 * higher than the previous C tone has double its frequency.
	 *
	 * <b>Frequency</b>
	 *
	 * An application may specify the frequency of the first octave by
	 * calling the setFrequency() method.
	 *
	 * <b>Persistence</b>
	 *
	 * The persistence value controls the <i>roughness</i> of the Simplex
	 * noise.  Larger values produce rougher noise.
	 *
	 * The persistence value determines how quickly the amplitudes diminish
	 * for successive octaves.  The amplitude of the first octave is 1.0.
	 * The amplitude of each subsequent octave is equal to the product of the
	 * previous octave's amplitude and the persistence value.  So a
	 * persistence value of 0.5 sets the amplitude of the first octave to
	 * 1.0; the second, 0.5; the third, 0.25; etc.
	 *
	 * An application may specify the persistence value by calling the
	 * setPersistence() method.
	 *
	 * <b>Lacunarity</b>
	 *
	 * The lacunarity specifies the frequency multipler between successive
	 * octaves.
	 *
	 * The effect of modifying the lacunarity is subtle; you may need to play
	 * with the lacunarity value to determine the effects.  For best results,
	 * set the lacunarity to a number between 1.5 and 3.5.
	 *
	 * <b>References &amp; acknowledgments</b>
	 *
	 * <a href=http://www.noisemachine.com/talk1/>The Noise Machine</a> -
	 * From the master, Ken Simplex himself.  This page contains a
	 * presentation that describes Simplex noise and some of its variants.
	 * He won an Oscar for creating the Simplex noise algorithm!
	 *
	 * <a
	 * href=http://freespace.virgin.net/hugo.elias/models/m_perlin.htm>
	 * Simplex Noise</a> - Hugo Elias's webpage contains a very good
	 * description of Simplex noise and describes its many applications.  This
	 * page gave me the inspiration to create libnoise in the first place.
	 * Now that I know how to generate Simplex noise, I will never again use
	 * cheesy subdivision algorithms to create terrain (unless I absolutely
	 * need the speed.)
	 *
	 * <a
	 * href=http://www.robo-murito.net/code/perlin-noise-math-faq.html>The
	 * Simplex noise math FAQ</a> - A good page that describes Simplex noise in
	 * plain English with only a minor amount of math.  During development of
	 * libnoise, I noticed that my coherent-noise function generated terrain
	 * with some "regularity" to the terrain features.  This page describes a
	 * better coherent-noise function called <i>gradient noise</i>.  This
	 * version of the Simplex module uses gradient coherent noise to
	 * generate Simplex noise.
	 */


	// Default frequency for the noise::module::Simplex noise module.
	static final double DEFAULT_SIMPLEX_FREQUENCY = 1.0;

	// Default lacunarity for the noise::module::Simplex noise module.
	static final double DEFAULT_SIMPLEX_LACUNARITY = 2.0;

	// Default number of octaves for the noise::module::Simplex noise module.
	static final int DEFAULT_SIMPLEX_OCTAVE_COUNT = 6;

	// Default persistence value for the noise::module::Simplex noise module.
	static final double DEFAULT_SIMPLEX_PERSISTENCE = 0.5;

	// Default noise quality for the noise::module::Simplex noise module.
	static final NoiseQuality DEFAULT_SIMPLEX_QUALITY = NoiseQuality.QUALITY_STD;

	// Default noise seed for the noise::module::Simplex noise module.
	static final int DEFAULT_SIMPLEX_SEED = 0;

	// Maximum number of octaves for the noise::module::Simplex noise module.
	static final int SIMPLEX_MAX_OCTAVE = 30;


	// Frequency of the first octave.
	double frequency;

	// Frequency multiplier between successive octaves.
	double lacunarity;

	// Quality of the Simplex noise.
	NoiseQuality noiseQuality;

	// Total number of octaves that generate the Simplex noise.
	int octaveCount;

	// Persistence of the Simplex noise.
	double persistence;

	// Seed value used by the Simplex-noise function.
	int seed;

	private SimplexBasis[] source;
	double[] frequencies;
	double[] amplitudes;

	public Simplex()
	{
		super(0);
		frequency = DEFAULT_SIMPLEX_FREQUENCY;
		lacunarity = DEFAULT_SIMPLEX_LACUNARITY;
		noiseQuality = DEFAULT_SIMPLEX_QUALITY;
		octaveCount = DEFAULT_SIMPLEX_OCTAVE_COUNT;
		persistence = DEFAULT_SIMPLEX_PERSISTENCE;
		seed = DEFAULT_SIMPLEX_SEED;
	}
	
	public void setUp()
	{
		source = new SimplexBasis[octaveCount];
		frequencies = new double[octaveCount];
		amplitudes = new double[octaveCount];

		for(int i = 0; i < octaveCount; i++)
		{
			if(seed != 0)
			{
				Random rnd = new Random(seed);
				seed = rnd.nextInt();
			}

			source[i] = new SimplexBasis();
			source[i].setSeed(seed);

			frequencies[i] = Math.pow(lacunarity, i);
			amplitudes[i] = Math.pow(persistence, source.length - i);
		}
	}

	public double getValue(double x, double y, double z)
	{
		double value = 0;
		double nx, ny, nz;
		double signal = 0;
		
		nx = Misc.MakeInt32Range (x);
		ny = Misc.MakeInt32Range (y);
		nz = Misc.MakeInt32Range (z);

		for(int i = 0; i < source.length; i++)
		{
			signal = source[i].getValue(nx/frequencies[i],  ny/frequencies[i], nz/frequencies[i]);
			value += signal * amplitudes[i];
		}

		return value;
	}

	/** Returns the frequency of the first octave.
	 *
	 * @returns The frequency of the first octave.
	 */
	public double getFrequency ()
	{
		return frequency;
	}

	/** Returns the lacunarity of the Simplex noise.
	 *
	 * @returns The lacunarity of the Simplex noise.
	 * 
	 * The lacunarity is the frequency multiplier between successive
	 * octaves.
	 */
	public double getLacunarity ()
	{
		return lacunarity;
	}

	/** Returns the quality of the Simplex noise.
	 * 
	 * See NoiseQuality for definitions of the various
	 * coherent-noise qualities.
	 * 
	 * @returns The quality of the Simplex noise.
	 *
	 * 
	 */
	public NoiseQuality getNoiseQuality ()
	{
		return noiseQuality;
	}

	/** Returns the number of octaves that generate the Simplex noise.
	 *
	 * @returns The number of octaves that generate the Simplex noise.
	 *
	 * The number of octaves controls the amount of detail in the Simplex
	 * noise.
	 */
	public int getOctaveCount ()
	{
		return octaveCount;
	}

	/** Returns the persistence value of the Simplex noise.
	 *
	 * @returns The persistence value of the Simplex noise.
	 *
	 * The persistence value controls the roughness of the Simplex noise.
	 */
	public double getPersistence ()
	{
		return persistence;
	}

	/** Returns the seed value used by the Simplex-noise function.
	 *
	 * @returns The seed value.
	 */
	public int getSeed ()
	{
		return seed;
	}

	/** Sets the frequency of the first octave.
	 *
	 * @param frequency The frequency of the first octave.
	 */
	public void setFrequency (double frequency)
	{
		this.frequency = frequency;
	}

	/** Sets the lacunarity of the Simplex noise.
	 *
	 * @param lacunarity The lacunarity of the Simplex noise.
	 * 
	 * The lacunarity is the frequency multiplier between successive
	 * octaves.
	 *
	 * For best results, set the lacunarity to a number between 1.5 and
	 * 3.5.
	 */
	public void setLacunarity (double lacunarity)
	{
		this.lacunarity = lacunarity;
	}

	/** Sets the quality of the Simplex noise.
	 *
	 * @param noiseQuality The quality of the Simplex noise.
	 *
	 * See NoiseQuality for definitions of the various
	 * coherent-noise qualities.
	 */
	public void setNoiseQuality (NoiseQuality noiseQuality)
	{
		this.noiseQuality = noiseQuality;
	}

	/** Sets the number of octaves that generate the Simplex noise.
	 *
	 * @param octaveCount The number of octaves that generate the Simplex
	 * noise.
	 *
	 * @pre The number of octaves ranges from 1 to SIMPLEX_MAX_OCTAVE.
	 *
	 * @throw noise::ExceptionInvalidParam An invalid parameter was
	 * specified; see the preconditions for more information.
	 *
	 * The number of octaves controls the amount of detail in the Simplex
	 * noise.
	 *
	 * The larger the number of octaves, the more time required to
	 * calculate the Simplex-noise value.
	 */
	public void setOctaveCount (int octaveCount)
	{
		if (octaveCount < 1)
		{
			octaveCount = 1;
		}
		else if (octaveCount > SIMPLEX_MAX_OCTAVE)
		{
			octaveCount = SIMPLEX_MAX_OCTAVE;
		}

		this.octaveCount = octaveCount;
	}

	/** Sets the persistence value of the Simplex noise.
	 *
	 * @param persistence The persistence value of the Simplex noise.
	 *
	 * The persistence value controls the roughness of the Simplex noise.
	 *
	 * For best results, set the persistence to a number between 0.0 and
	 * 1.0.
	 */
	public void setPersistence (double persistence)
	{
		this.persistence = persistence;
	}

	/** Sets the seed value used by the Simplex-noise function.
	 *
	 * @param seed The seed value.
	 */
	public void setSeed (int seed)
	{
		this.seed = seed;
	}
}
