#version 120

#define TEX_SIZE 256

uniform sampler2D perlin1;
uniform sampler2D biomes;

varying vec2 texCoord;

// preview settings
uniform mat4 previewTransform;
uniform vec2 biomeCoordScaleAndOffset;

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

float perlinRaw(vec2 pos, int octaves) {
    vec2 p = pos*8*(1.0/TEX_SIZE);
    float valFactor = 1;
    float ret = 0;
    float maxVal = 2 - pow(0.5, octaves - 1);
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
float perlin(vec2 texCoord, vec2 freq, int octaves) {
    float v00 = perlinRaw(floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)*freq, octaves);
    float v10 = perlinRaw((floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)+vec2(4, 0))*freq, octaves);
    float v01 = perlinRaw((floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)+vec2(0, 8))*freq, octaves);
    float v11 = perlinRaw((floor(texCoord*vec2(0.25, 0.125))*vec2(4, 8)+vec2(4, 8))*freq, octaves);

    vec2 interpA = fract(texCoord*vec2(0.25, 0.125));//*vec2(4, 8);

    float vX0 = mix(v00, v10, interpA.x);
    float vX1 = mix(v01, v11, interpA.x);

    float v = mix(vX0, vX1, interpA.y);
    return v;
}
vec2 getBiomeData(float at) {
    return texture2D(biomes, vec2(at*0.25, at*0.25)).rg;
}
void main() {
    vec2 blockCoord1 = floor((previewTransform*vec4(texCoord, 0, 1)).xy);
    vec2 blockCoord2 = blockCoord1+vec2(100, 100);
    vec2 blockCoord3 = blockCoord2+vec2(200, 200);

    vec2 previewBiomeData = getBiomeData(blockCoord1.x*biomeCoordScaleAndOffset.x + biomeCoordScaleAndOffset.y);
    float previewHeight = previewBiomeData.x;
    float previewHeightVariation = previewBiomeData.y;

    float low = perlin(blockCoord1, lowFreq.xy, lowOctaves)*lowFactor + lowOffset;
    float high = perlin(blockCoord2, highFreq.xy, highOctaves)*highFactor + highOffset;
    float sel = perlin(blockCoord3, selectorFreq.xy, selectorOctaves)*selectorFactor + selectorOffset;
    sel = clamp(sel, 0, 1);

    float special = 1;
    if (blockCoord1.y < previewHeight*heightFactor+heightOffset) {
        special = heightVariationSpecial;
    }
    // biomeData: x=height, y=heightVariation
    float v = mix(low, high, sel)*(previewHeightVariation*heightVariationFactor*special+heightVariationOffset)
                + (previewHeight*heightFactor+heightOffset) - blockCoord1.y;
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

