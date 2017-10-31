#version 120

#define TEX_SIZE 256
#define SAMPLES_PER_UNIT 8
// pre-generated values
#define BIOME_WEIGHT_0 50.37709878023948
#define BIOME_WEIGHT_1 31.383286567439356
#define BIOME_WEIGHT_2 20.63438386838542
// inverse of 2*weight1+2*weight2+weight0
#define BIOME_WEIGHTS_INV 0.005430203690844081

uniform sampler2D perlin1;
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
uniform vec2 depthFreq;
uniform int depthOctaves;

uniform float selectorFactor;
uniform float selectorOffset;
uniform vec3 selectorFreq;
uniform int selectorOctaves;

uniform float lowFactor;
uniform float lowOffset;
uniform vec3 lowFreq;
uniform int lowOctaves;

uniform float highFactor;
uniform float highOffset;
uniform vec3 highFreq;
uniform int highOctaves;

float perlin(vec2 pos, vec2 freq, int octaves) {
    vec2 p = pos*SAMPLES_PER_UNIT*(1.0/TEX_SIZE)*freq;
    float valFactor = 1;
    float ret = 0;
    float maxVal = 2 - pow(0.5, octaves - 1);
    // TODO: reduce max to 8?
    for (int i = 0; i < 16; i++) { // constant amount vecause glsl may not like dynamic loops witn sampler access inside, assume max 16
        float val = texture2D(perlin1, p).r;
        if (i < octaves) {
            ret += val*valFactor;
            p *= 2;
            valFactor *= 0.5;
        }
    }
    return ret/maxVal;
}
vec2 rawBiomeData(float pos) {
    float p = pos*biomeCoordScaleAndOffset.x + biomeCoordScaleAndOffset.y;
    return texture2D(biomes, vec2(0, floor(p*biomeCount)/biomeCount + 0.5/biomeCount)).rg;
}
vec2 interpBiomeData(float blockPos) {
    vec2 v_2 = rawBiomeData(blockPos - 8);
    vec2 v_1 = rawBiomeData(blockPos - 4);
    vec2 v0 = rawBiomeData(blockPos);
    vec2 v1 = rawBiomeData(blockPos + 4);
    vec2 v2 = rawBiomeData(blockPos + 8);
    return (BIOME_WEIGHT_2*(v_2+v2) + BIOME_WEIGHT_1*(v_1+v1) + BIOME_WEIGHT_0*v0)*BIOME_WEIGHTS_INV;
}
float densityRaw(vec2 pos) {
    // 2 other positions so that the noise values used aren't exactly the same
    vec2 pos2 = pos + vec2(1234.5678, 8765.4321);
    vec2 pos3 = pos + vec2(4321.5678, 5678.4321);

    vec2 previewBiomeData = interpBiomeData(pos.x);
    float previewHeight = previewBiomeData.x;
    float previewHeightVariation = previewBiomeData.y;

    float low = perlin(pos, lowFreq.xy, lowOctaves)*lowFactor + lowOffset;
    float high = perlin(pos2, highFreq.xy, highOctaves)*highFactor + highOffset;
    float sel = perlin(pos3, selectorFreq.xy, selectorOctaves)*selectorFactor + selectorOffset;
    sel = clamp(sel, 0, 1);

    float special = 1;
    if (pos.y < previewHeight*heightFactor+heightOffset) {
        special = heightVariationSpecial;
    }
    // biomeData: x=height, y=heightVariation
    return mix(low, high, sel)*(previewHeightVariation*heightVariationFactor*special+heightVariationOffset)
                + (previewHeight*heightFactor+heightOffset) - pos.y;
}
float densityInterp(vec2 texCoord) {
    float v00 = densityRaw(floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8));
    float v10 = densityRaw(floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)+vec2(4, 0));
    float v01 = densityRaw(floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)+vec2(0, 8));
    float v11 = densityRaw(floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)+vec2(4, 8));

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