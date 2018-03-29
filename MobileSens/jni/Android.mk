LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := macAddr

LOCAL_SRC_FILES := macAddr.cpp

LOCAL_C_INCLUDES := macAddr.h

LOCAL_LDLIBS := \
	-llog

include $(BUILD_SHARED_LIBRARY)

