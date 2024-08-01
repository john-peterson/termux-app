LOCAL_PATH:= $(call my-dir)
#$(error $(LOCAL_PATH))

#include ../lmk/Android.mk

include $(CLEAR_VARS)
LOCAL_MODULE:= libtermux
LOCAL_SRC_FILES:= termux.c
LOCAL_C_INCLUDES := ../../../core/libcutils/include ../lmkd/include ../core/libprocessgroup/include ../libbase/include ../core/libprocessgroup/cgrouprc/include ../core/libprocessgroup /home/a/termux-monet/lmkd/include
LOCAL_STATIC_LIBRARIES:=liblmkd_utils
include $(BUILD_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := liblmkd_utils
#LOCAL_SRC_FILES := ../../../../build/local/x86_64/liblmkd_utils.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libcutils
LOCAL_SRC_FILES := ../../../../build-tools/linux-x86/lib64/libcutils.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
#LOCAL_EXPORT_STATIC_LIBRARIES:=abc
LOCAL_MODULE:= liblmkd_utils
LOCAL_SRC_FILES:= ../../../../lmkd/liblmkd_utils.cpp
LOCAL_C_INCLUDES := ../core/libcutils/include ../lmkd/include ../core/libprocessgroup/include ../libbase/include ../core/libprocessgroup/cgrouprc/include ../core/libprocessgroup
LOCAL_SHARED_LIBRARIES:=libcutils
include $(BUILD_STATIC_LIBRARY)