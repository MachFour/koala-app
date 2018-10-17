#include <jni.h>
#include <string>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>


#include <baseapi.h>

#include <table.h>
#include <reference.h>
#include <ocrutils.h>

using namespace std;
using namespace cv;

extern "C" {

JNIEXPORT
jstring JNICALL Java_com_machfour_koalaApp_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


JNIEXPORT
void JNICALL Java_com_machfour_koalaApp_OpenCVActivity_salt(JNIEnv *env, jobject instance, jlong matAddrGray, jint nbrElem) {
    Mat &mGr = *(Mat *) matAddrGray;
    for (int k = 0; k < nbrElem; k++) {
        int i = rand() % mGr.cols;
        int j = rand() % mGr.rows;
        mGr.at<uchar>(j, i) = 255;
    }
}

JNIEXPORT
jstring JNICALL Java_com_machfour_koalaApp_ProcessImageActivity_doExtractTable(
        JNIEnv *env, jobject instance, jlong matAddr, jstring tessdataPath, jstring tessConfigFile) {
    tesseract::TessBaseAPI tesseractApi;
    const char *nativeTessdataPath = env->GetStringUTFChars(tessdataPath, JNI_FALSE);
    const char *nativeTessConfigFile = env->GetStringUTFChars(tessConfigFile, JNI_FALSE);
    if (tesseractInit(tesseractApi, nativeTessdataPath, nativeTessConfigFile) == -1) {
        //fprintf(stderr, "Could not initialise tesseract API");
        __android_log_print(ANDROID_LOG_WARN, "doExtractTable()", "Could not initialise tesseract");
        return env->NewStringUTF("");
    }

    Table outTable = tableExtract(*(Mat *) matAddr, tesseractApi);

    tesseractApi.End();
    env->ReleaseStringUTFChars(tessdataPath, nativeTessdataPath);
    env->ReleaseStringUTFChars(tessConfigFile, nativeTessConfigFile);

    std::string tableString = outTable.parseableString();
    return env->NewStringUTF(tableString.data());
    /*
#ifdef REFERENCE_ANDROID
    __android_log_print(ANDROID_LOG_INFO, "KTable", "%s", tableString.data());
    //printf("%s", s.data());
#else
    printf("%s", s.data());
#endif
     */
}



}