# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_VERBOSE_MAKEFILE ON CACHE BOOL "ON")
set(CMAKE_CXX_STANDARD 11)

# OpenCV

set(OPENCV_SDK_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../OpenCV-android-sdk)

include_directories(${OPENCV_SDK_PATH}/sdk/native/jni/include)
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv
            PROPERTIES
            IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

# for native build
set(OpenCV_DIR ${OPENCV_SDK_PATH}/sdk/native/jni)
find_package(OpenCV 3.4 REQUIRED java core imgproc imgcodecs highgui ml)
include_directories(${OpenCV_INCLUDE_DIRS})

# Tesseract
set(TESS_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../tess)
set(TESS_UPSTREAM_SOURCE ${TESS_PATH}/../tess/tess-two/jni/com_googlecode_tesseract_android/src)
set(TESS_LIB_PATH ${TESS_PATH}/../tess/tess-two/libs/${ANDROID_ABI})

add_library(lib_tesseract SHARED IMPORTED)
set_target_properties(lib_tesseract PROPERTIES IMPORTED_LOCATION ${TESS_LIB_PATH}/libtess.so)

include_directories(${TESS_PATH}/tess-two/jni/com_googlecode_leptonica_android/src/src)
include_directories(${TESS_UPSTREAM_SOURCE}/api)
# look in tess/tess-two/jni/com_googlecode_tesseract_android/src/api/Makefile.am
include_directories(${TESS_UPSTREAM_SOURCE}/ccutil)
include_directories(${TESS_UPSTREAM_SOURCE}/ccstruct)
include_directories(${TESS_UPSTREAM_SOURCE}/cube)
include_directories(${TESS_UPSTREAM_SOURCE}/viewer)
include_directories(${TESS_UPSTREAM_SOURCE}/textord)
include_directories(${TESS_UPSTREAM_SOURCE}/dict)
include_directories(${TESS_UPSTREAM_SOURCE}/classify)
include_directories(${TESS_UPSTREAM_SOURCE}/ccmain)
include_directories(${TESS_UPSTREAM_SOURCE}/wordrec)
include_directories(${TESS_UPSTREAM_SOURCE}/cutil)
include_directories(${TESS_UPSTREAM_SOURCE}/opencl)

# tesseract deps
add_library(lib_lept SHARED IMPORTED)
set_target_properties(lib_lept PROPERTIES IMPORTED_LOCATION ${TESS_LIB_PATH}/liblept.so)
add_library(lib_pngt SHARED IMPORTED)
set_target_properties(lib_pngt PROPERTIES IMPORTED_LOCATION ${TESS_LIB_PATH}/libpngt.so)
add_library(lib_jpgt SHARED IMPORTED)
set_target_properties(lib_jpgt PROPERTIES IMPORTED_LOCATION ${TESS_LIB_PATH}/libjpgt.so)

# My code: 'Koala'

set(KOALA_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../koala)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DREFERENCE_ANDROID")

include_directories(${KOALA_PATH})

file(GLOB KOALA_SOURCES
    ${KOALA_PATH}/*.cpp
    ${KOALA_PATH}/*.h
)
# remove main.cpp
list(REMOVE_ITEM KOALA_SOURCES ${KOALA_PATH}/main.cpp)

add_library(koala
            SHARED
            ${KOALA_SOURCES})

# Sample

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       lib_opencv
                       koala
                       log
                       )

target_link_libraries(  koala
                        ${OpenCV_LIBRARIES}
                        lib_tesseract
                        lib_lept
                        lib_pngt
                        lib_jpgt
                       )