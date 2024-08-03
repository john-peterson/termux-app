LOCAL_PATH:= $(call my-dir)
#$(error $(LOCAL_PATH))

build_path = $(shell pwd)
#$(error $(build_path))

include $(CLEAR_VARS)
LOCAL_MODULE:= libtermux
LOCAL_SRC_FILES:= termux.c oom.c
LOCAL_SRC_FILES+= $(wildcard $(build_path)/../earlyoom/*.c)
#$(error $(LOCAL_SRC_FILES))
LOCAL_C_INCLUDES := $(build_path)/../earlyoom
LOCAL_LDLIBS:=-llog
include $(BUILD_SHARED_LIBRARY)
