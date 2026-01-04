// native_audio.cpp
#include <jni.h>
#include <android/log.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#define LOG_TAG "EternalAudio"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global engine objects
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;
static SLObjectItf outputMixObject = NULL;
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;

// Infinite playback callback
void callback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    // This creates an infinite loop by re-queuing buffers
    static short buffer[48000]; // 1 second of audio
    (*bq)->Enqueue(bq, buffer, sizeof(buffer));
}

extern "C" JNIEXPORT void JNICALL
Java_com_shadow_eternalsiren_NativeAudio_startEternalPlayback(JNIEnv *env, jobject thiz) {
    // Create OpenSL ES engine
    slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    
    // Create output mix
    (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    
    // Configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
        SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {
        SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1,
        SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
        SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN};
    
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};
    
    // Configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};
    
    // Create audio player
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    
    (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk,
                                       1, ids, req);
    (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
    
    // Set callback for infinite playback
    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                    &bqPlayerBufferQueue);
    (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, callback, NULL);
    
    // Start playback
    (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    callback(bqPlayerBufferQueue, NULL); // Initial buffer
}
