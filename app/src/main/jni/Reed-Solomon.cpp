#include <jni.h>

extern "C" {
JNIEXPORT jintArray JNICALL Java_cn_edu_nju_cs_screencamera_ReedSolomonEncode_encode(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jintArray array_,
                                                                                     jint length) {
    jint *array = env->GetIntArrayElements(array_, nullptr);

    // TODO

    env->ReleaseIntArrayElements(array_, array, 0);
}
JNIEXPORT jintArray JNICALL Java_cn_edu_nju_cs_screencamera_ReedSolomonEncode_decode(JNIEnv *env, jclass type, jintArray array_,
                                                         jint length) {
    jint *array = env->GetIntArrayElements(array_, nullptr);

    // TODO

    env->ReleaseIntArrayElements(array_, array, 0);
}
}