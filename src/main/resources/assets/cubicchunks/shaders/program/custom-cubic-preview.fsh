#version 120

#define TEX_SIZE 256
#define SAMPLES_PER_UNIT 8
// should be 16 but there is no visible difference > 12, and if there is - it's swamped by shader inaccuracies
#define MAX_OCTAVES 12
// pre-generated values
#define BIOME_WEIGHT_0 50.37709878023948
#define BIOME_WEIGHT_1 31.383286567439356
#define BIOME_WEIGHT_2 20.63438386838542
// inverse of 2*weight1+2*weight2+weight0
#define BIOME_WEIGHTS_INV 0.005430203690844081

uniform sampler2D perlin;
uniform sampler2D biomes;

varying vec2 texCoord;

// preview settings
uniform mat4 previewTransform;
uniform vec2 biomeCoordScaleAndOffset;

// TODO: make it #define
uniform float biomeCount;

// simple wgen options
uniform float waterLevel;

// advanced wgen settings
uniform float heightVariationFactor;
uniform float heightVariationSpecial;
uniform float heightVariationOffset;
uniform float heightFactor;
uniform float heightOffset;

uniform float depthFactor;
uniform float depthOffset;
uniform float depthFreq;
uniform int depthOctaves;

uniform float selectorFactor;
uniform float selectorOffset;
uniform vec2 selectorFreq;
uniform int selectorOctaves;

uniform float lowFactor;
uniform float lowOffset;
uniform vec2 lowFreq;
uniform int lowOctaves;

uniform float highFactor;
uniform float highOffset;
uniform vec2 highFreq;
uniform int highOctaves;

// a helpful macro to avoid repeating code
// can't use line continuation before glsl 1.3 :(
#define NOISE(pos, var) valFactor = 1.0; for (int i = 0; i < MAX_OCTAVES; i++) { float val = texture2D(perlin, pos).var * 2 - 1; if (i < octaves.var) { ret.var += val * valFactor; } pos *= 2.0; valFactor *= 0.5; }

// pos vector: block position
// freq: frequencies, x=selector, y=low, z=high, w=depth
// octaves: octaves, same as for frequencies
// return: values for selector, low, high and depth
vec4 getNoise(vec2 pos, vec2 freqSel, vec2 freqLow, vec2 freqHigh, vec2 freqDepth, ivec4 octaves) {
    vec2 posBase = pos*SAMPLES_PER_UNIT*(1.0/TEX_SIZE);
    vec2 pSel = posBase * freqSel;
    vec2 pLow = posBase * freqLow;
    vec2 pHigh = posBase * freqHigh;
    vec2 pDepth = posBase * freqDepth;

    vec4 ret = vec4(0.0);
    vec4 maxVal = vec4(2.0) - pow(vec4(0.5), vec4(octaves) - vec4(1.0));

    float valFactor;

    NOISE(pSel, x);
    NOISE(pLow, y);
    NOISE(pHigh, z);
    NOISE(pDepth, w);

    return ret/maxVal;
}
float intFractToFloat(vec2 intFract) {
    return (intFract.x*255-128)+intFract.y;
}
vec2 rawBiomeData(vec2 pos) {
    vec4 data = texture2D(biomes, pos);
    return vec2(intFractToFloat(data.xy), intFractToFloat(data.zw));
}
vec2 biomeData(float pos) {
    float p = pos*biomeCoordScaleAndOffset.x + biomeCoordScaleAndOffset.y;
    return rawBiomeData(vec2(floor(p*biomeCount)/biomeCount + 0.5/biomeCount, 0));
}
vec2 interpBiomeData(float blockPos) {
    vec2 v_2 = biomeData(blockPos - 8);
    vec2 v_1 = biomeData(blockPos - 4);
    vec2 v0 = biomeData(blockPos);
    vec2 v1 = biomeData(blockPos + 4);
    vec2 v2 = biomeData(blockPos + 8);
    return (BIOME_WEIGHT_2*(v_2+v2) + BIOME_WEIGHT_1*(v_1+v1) + BIOME_WEIGHT_0*v0)*BIOME_WEIGHTS_INV;
}
float depthTransform(float d) {
    d *= (d < 0.0) ? -0.9 : 3.0;
    d -= 2.0;
    d *= (d < 0.0) ? 5/28.0 : 0.125;
    return clamp(d, -5/14.0, 0.125) * (0.2 * 17.0 / 64.0);
}
float densityRaw(vec2 pos) {
    vec2 previewBiomeData = interpBiomeData(pos.x);
    float previewHeight = previewBiomeData.x;
    float previewHeightVariation = previewBiomeData.y;

    vec4 noiseFactors = vec4(selectorFactor, lowFactor, highFactor, depthFactor);
    vec4 noiseOffsets = vec4(selectorOffset, lowOffset, highOffset, depthOffset);
    ivec4 octaves = ivec4(selectorOctaves, lowOctaves, highOctaves, depthOctaves);

    vec4 noise = getNoise(pos, selectorFreq, lowFreq, highFreq, vec2(depthFreq, 0), octaves) * noiseFactors + noiseOffsets;
    noise.x = clamp(noise.x, 0, 1);

    float special = 1;
    if (pos.y < previewHeight*heightFactor+heightOffset) {
        special = heightVariationSpecial;
    }

    float depth = depthTransform(noise.w);

    // mix(low, high, selector)
    float rawDensity = mix(noise.y, noise.z, noise.x) + depth;
    float heightVariation = previewHeightVariation*heightVariationFactor*special+heightVariationOffset;
    float heightOffset = previewHeight*heightFactor+heightOffset;

    return rawDensity*heightVariation + heightOffset - pos.y*sign(heightVariation);
}
float densityInterp(vec2 texCoord) {
    vec2 posBase = floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8);
    float v00 = densityRaw(posBase);
    float v10 = densityRaw(posBase+vec2(4, 0));
    float v01 = densityRaw(posBase+vec2(0, 8));
    float v11 = densityRaw(posBase+vec2(4, 8));

    vec2 interpA = fract(texCoord*vec2(0.25, 0.125));

    float vX0 = mix(v00, v10, interpA.x);
    float vX1 = mix(v01, v11, interpA.x);

    float v = mix(vX0, vX1, interpA.y);
    return v;
}
void main() {

    vec2 blockCoord1 = floor((previewTransform*vec4(texCoord, 0, 1)).xy);

    float v = densityInterp(blockCoord1);
    vec4 col;
    if (v > 0) {
        col = vec4(0.5, 0.5, 0.5, 1);
    } else {
        if (blockCoord1.y <= waterLevel) {
            col = vec4(0.25, 0.25, 1, 1);
        } else {
            col = vec4(0, 0, 0, 0);
        }
    }
    gl_FragColor = col;
}