#include <jni.h>
#include "ezpwd/rs"
#include "findBorder"
#include <android/log.h>

#define  LOG_TAG    "your-log-tag"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

std::vector<int> findBorder(jbyte *pixels, int threshold, int imgWidth, int imgHeight);
JNIEXPORT jintArray JNICALL Java_cn_edu_nju_cs_screencamera_AndroidJni_RSEncode(JNIEnv *env,
                                                                                jobject instance,
                                                                                jintArray array_,
                                                                                jint length) {
    jint *array = env->GetIntArrayElements(array_, nullptr);
    std::vector<uint16_t> dataVec;
    dataVec.insert(dataVec.end(), &array[0], &array[length]);
    env->ReleaseIntArrayElements(array_, array, 0);
    ezpwd::RS<1023, 983> rs;
    rs.encode(dataVec);

    int newLength = dataVec.size();
    int arr[newLength];
    std::copy(dataVec.begin(), dataVec.end(), arr);
    jintArray result = env->NewIntArray(newLength);
    env->SetIntArrayRegion(result, 0, newLength, arr);
    return result;
}
JNIEXPORT jintArray JNICALL Java_cn_edu_nju_cs_screencamera_AndroidJni_RSDecode(JNIEnv *env,
                                                                                jclass type,
                                                                                jintArray array_,
                                                                                jint length) {
    jint *array = env->GetIntArrayElements(array_, nullptr);
    std::vector<uint16_t> dataVec;
    dataVec.insert(dataVec.end(), &array[0], &array[length]);
    env->ReleaseIntArrayElements(array_, array, 0);
    ezpwd::RS<1023, 983> rs;
    int fixed = rs.decode(dataVec);
    if (fixed == -1) {
        return nullptr;
    }
    int newLength = dataVec.size();
    int arr[newLength];
    std::copy(dataVec.begin(), dataVec.end(), arr);
    jintArray result = env->NewIntArray(newLength);
    env->SetIntArrayRegion(result, 0, newLength, arr);
    return result;
}

JNIEXPORT jintArray JNICALL Java_cn_edu_nju_cs_screencamera_AndroidJni_findBorder(JNIEnv *env,
                                                                                  jclass type,
                                                                                  jbyteArray array_,
                                                                                  jint imgColorType,
                                                                                  jint threshold,
                                                                                  jint imgWidth,
                                                                                  jint imgHeight,
                                                                                  jintArray initBorder) {
    jboolean b;
    jbyte *array = env->GetByteArrayElements(array_, &b);
    jint *init = env->GetIntArrayElements(initBorder, nullptr);
    Find_border find_border(array, imgColorType, imgWidth, imgHeight, threshold);
    std::vector<int> borders = find_border.findBorder(init);
    env->ReleaseByteArrayElements(array_, array, 0);
    env->ReleaseIntArrayElements(initBorder, init, 0);

    jintArray result = env->NewIntArray(borders.size());
    env->SetIntArrayRegion(result, 0, borders.size(), &borders[0]);
    return result;

}
}