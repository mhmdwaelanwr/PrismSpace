#pragma once

#include <android/log.h>

#ifndef PRISM_LOG_TAG
#define PRISM_LOG_TAG "prismspace"
#endif

#define PRISM_LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, PRISM_LOG_TAG, __VA_ARGS__)
#define PRISM_LOGI(...) __android_log_print(ANDROID_LOG_INFO,  PRISM_LOG_TAG, __VA_ARGS__)
#define PRISM_LOGW(...) __android_log_print(ANDROID_LOG_WARN,  PRISM_LOG_TAG, __VA_ARGS__)
#define PRISM_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, PRISM_LOG_TAG, __VA_ARGS__)

// Legacy fallbacks for older C++ util files
#ifndef ALOGD
#define ALOGD(...) PRISM_LOGD(__VA_ARGS__)
#endif
#ifndef ALOGI
#define ALOGI(...) PRISM_LOGE(__VA_ARGS__)
#endif
#ifndef ALOGW
#define ALOGW(...) PRISM_LOGW(__VA_ARGS__)
#endif
#ifndef ALOGE
#define ALOGE(...) PRISM_LOGE(__VA_ARGS__)
#endif

