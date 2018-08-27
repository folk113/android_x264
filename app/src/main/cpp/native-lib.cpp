#include <jni.h>
#include <string>

#include "example.c"


extern "C" JNIEXPORT jint
JNICALL
Java_com_phubber_x264player_MainActivity_x264_1encoder_1init(JNIEnv *env,
                                                               jobject /* this */,jint width,jint height)
{
   return  x264_init(width,height);
}

extern "C" JNIEXPORT jint
JNICALL
Java_com_phubber_x264player_MainActivity_x264_1encoder_1encode(JNIEnv *env,
jobject /* this */,jbyteArray dataIn,jbyteArray dataOut)
{
    jbyte* _dataIn = env->GetByteArrayElements(dataIn,NULL);
    jbyte* _dataOut = env->GetByteArrayElements(dataOut,NULL);
    int size = x264_encode((uint8_t*)_dataIn,(uint8_t*) _dataOut);
    env->ReleaseByteArrayElements(dataIn,_dataIn,JNI_FALSE);
    env->ReleaseByteArrayElements(dataOut,_dataOut,JNI_FALSE);
    return size;
}

extern "C" JNIEXPORT void
JNICALL
Java_com_phubber_x264player_MainActivity_x264_1encoder_1release(JNIEnv *env,
                                                               jobject /* this */)
{
    x264_release();
}