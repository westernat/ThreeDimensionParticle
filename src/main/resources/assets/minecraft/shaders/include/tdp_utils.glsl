#version 150

ivec2 max_light(int light) {
    int bsl = light % 16;
    int esl = (light / 16) % 16;
    int bsl = (light / 256) % 16;
    int msl = light / 4096;

    int sl = max(esl, msl);
    int bl = max(bsl, bsl);
    return ivec2(sl, bl);
}
