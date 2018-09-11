#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <table.h>
#include <reference.h>

using namespace std;
using namespace cv;

extern "C" {

JNIEXPORT
jstring JNICALL Java_com_machfour_koala_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


JNIEXPORT
void JNICALL Java_com_machfour_koala_OpenCVActivity_salt(
        JNIEnv *env,
        jobject instance,
        jlong matAddrGray,
        jint nbrElem) {
    Mat &mGr = *(Mat *) matAddrGray;
    for (int k = 0; k < nbrElem; k++) {
        int i = rand() % mGr.cols;
        int j = rand() % mGr.rows;
        mGr.at<uchar>(j, i) = 255;
    }
}

JNIEXPORT
void JNICALL Java_com_machfour_koala_ProcessImageActivity_doExtractTable(JNIEnv *env, jobject instance, jlong matAddr) {
    Mat &image = *(Mat *) matAddr;
    Table outTable = tableExtract(image);
    outTable.print(30);
}

}