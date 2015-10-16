LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := javaclient
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := ../../external/Java-Client.jar
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX := $(COMMON_JAVA_PACKAGE_SUFFIX)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)

LOCAL_MODULE := services.core

LOCAL_SRC_FILES += \
    $(call all-java-files-under,java) \
    java/com/android/server/EventLogTags.logtags \
    java/com/android/server/am/EventLogTags.logtags

LOCAL_JAVA_LIBRARIES := android.policy telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := javaclient

include $(BUILD_STATIC_JAVA_LIBRARY)
