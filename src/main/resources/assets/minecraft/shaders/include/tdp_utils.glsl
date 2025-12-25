#version 150

ivec2 max_light(int light) {
    int ebl = light % 16;
    int esl = (light / 16) % 16;
    int mbl = (light / 256) % 16;
    int msl = light / 4096;

    int sl = max(msl, esl);
    int bl = max(mbl, ebl);
    return ivec2(sl, bl);
}
