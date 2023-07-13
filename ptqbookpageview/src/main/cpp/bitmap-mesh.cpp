#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <cmath>
#include <string>

#define LOG_TAG "ptq_bitmap_synthesizer"
#define logd(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define loge(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


static int *lZWEdge;
static int *lSTEdge;
static int width;
static int height;

/**
 * rgb565转argb8888
 * rgb565：16位，RRRRR GGGGGG BBBBB
 * argb8888：32位，AAAAAAAA BBBBBBBB GGGGGGGG RRRRRRRR
 * @param pixel
 * @return
 */
static uint32_t rgb565ToArgb8888(uint16_t pixel) {
    uint8_t r = ((pixel >> 11) & 0x1F) * 0xff / 0x1f;
    uint8_t g = ((pixel >> 5) & 0x3F) * 0xff / 0x3f;
    uint8_t b = (pixel & 0x1F) * 0xff / 0x1f;
    return 0xff << 24 | (b & 0xff) << 16 | (g & 0xff) << 8 | (r & 0xff);
}

/**
 * 将底图和扭曲图合成
 * @since v1.1.0
 * @param synthesized 合成图像素
 * @param lower 底图像素
 * @param WZS W、Z、S点的坐标（不垂直的时候才会用到S，否则只用WZ）
 */
static void synthesize(
        uint32_t *synthesized,
        const uint16_t *lower,
        const float *WZS,
        const bool upsideDown) {
    if (width == 0 || height == 0 || lZWEdge == nullptr || lSTEdge == nullptr) return;

    //对每个y，计算合成区段
    float Wx, Wy, Zx, Zy, Sx = 0;
    Wx = WZS[0];
    Wy = WZS[1];
    Zx = WZS[2];
    Zy = WZS[3];
    Sx = WZS[4];

    float k_, bWZ;

    k_ = (Zx - Wx) / (Zy - Wy); //k的倒数
    bWZ = Zy - Zx / k_;
    float WS = Sx - Wx;
    for (int i = 0; i < height; ++i) {
        float res = ((float) i - bWZ) * k_;
        int resInt = (int) res - 1; //-1为了不留白，多往左侧画一个
        resInt = (resInt < 0) ? 0 : resInt;
        resInt = (resInt >= width) ? width - 1 : resInt;
        if (!upsideDown) {
            lZWEdge[i] = resInt;
        } else {
            lZWEdge[height - 1 - i] = resInt;
        }

        int resIntST = (int) (res + WS) - 1;
        resIntST = (resIntST < 0) ? 0 : resIntST;
        resIntST = (resIntST >= width) ? width - 1 : resIntST;
        if (!upsideDown) {
            lSTEdge[i] = resIntST;
        } else {
            lSTEdge[height - 1 - i] = resIntST;
        }
    }

    //合成
    for (int i = 0; i < height; ++i) {
        //synthesize
        for (int j = lZWEdge[i]; j <= lSTEdge[i]; ++j) {
            int index = width * i + j;
            if (synthesized[index] != 0) {
                continue;
            }

            synthesized[index] = rgb565ToArgb8888(lower[index]);
        }

        //lower
        for (int j = lSTEdge[i] + 1; j < width; ++j) {
            int index = width * i + j;

            if (synthesized[index] != 0) {
                continue;
            }

            synthesized[index] = rgb565ToArgb8888(lower[index]);
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_ptq_mpga_ptqbookpageview_widget_BitmapSynthesizer_nSynthesize(
        JNIEnv *env,
        jobject,
        jobject lower,
        jobject synthesized,
        jfloatArray WZS,
        jboolean upsideDown) {
    int ret;
    void *synthesizedPixels;
    void *lowPixels;

    if ((ret = AndroidBitmap_lockPixels(env, synthesized, &synthesizedPixels)) < 0) {
        loge("PTQBookPageView lockPixels(synthesized) failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, lower, &lowPixels)) < 0) {
        loge("PTQBookPageView lockPixels(lower) failed ! error=%d", ret);
        return;
    }

    float *mWZS = env->GetFloatArrayElements(WZS, nullptr);

    synthesize(
            (uint32_t *) synthesizedPixels,
            (uint16_t *) lowPixels,
            mWZS,
            upsideDown);

    AndroidBitmap_unlockPixels(env, synthesized);
    AndroidBitmap_unlockPixels(env, lower);
    env->ReleaseFloatArrayElements(WZS, mWZS, 0);
}

/**
 * 清空Synthesized
 * @param env
 * @param synthesized
 */
static void clearSynthesized(JNIEnv *env, jobject synthesized) {
    int ret;
    void *synthesizedPixels;
    if ((ret = AndroidBitmap_lockPixels(env, synthesized, &synthesizedPixels)) < 0) {
        loge("PTQBookPageView clearSynthesized lockPixels(low) failed ! error=%d", ret);
        return;
    }

    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, synthesized, &info);

    uint32_t size = info.height * info.width;
    std::fill_n((uint32_t *) synthesizedPixels, size, 0);
    AndroidBitmap_unlockPixels(env, synthesized);
}

extern "C"
JNIEXPORT void JNICALL
Java_ptq_mpga_ptqbookpageview_widget_BitmapSynthesizer_nResize(
        JNIEnv *env,
        jobject,
        jint width_,
        jint height_,
        jobject synthesized) {
    width = width_;
    height = height_;
    lZWEdge = (int *) realloc(lZWEdge, height_ * sizeof(int));
    lSTEdge = (int *) realloc(lSTEdge, height_ * sizeof(int));

    clearSynthesized(env, synthesized);
}

extern "C"
JNIEXPORT void JNICALL
Java_ptq_mpga_ptqbookpageview_widget_BitmapSynthesizer_nDestroy(JNIEnv *, jobject) {
    free(lZWEdge);
    free(lSTEdge);
    lZWEdge = nullptr;
    lSTEdge = nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_ptq_mpga_ptqbookpageview_widget_BitmapSynthesizer_nClearSynthesizedCache(JNIEnv *env, jobject, jobject synthesized) {
    clearSynthesized(env, synthesized);
}