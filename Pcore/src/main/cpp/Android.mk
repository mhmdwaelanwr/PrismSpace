LOCAL_PATH := $(call my-dir)

# Defaults stay conservative, but CI/local diagnostic builds can override these on the
# ndk-build command line (for example PRISM_DIAGNOSTIC_DEP_STAGE=A1).
PRISM_DIAGNOSTIC_LAYERED_BRINGUP ?= true
PRISM_DIAGNOSTIC_DEP_STAGE ?= A0
PRISM_DIAGNOSTIC_SRC_STAGE ?= B9

ifeq ($(PRISM_DIAGNOSTIC_LAYERED_BRINGUP),true)

ifneq ($(filter A1 A2 A3 A4,$(PRISM_DIAGNOSTIC_DEP_STAGE)),)
include $(CLEAR_VARS)
LOCAL_MODULE := xdl
LOCAL_CXXFLAGS := -std=c++11 -fno-exceptions -fno-rtti
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SRC_FILES := xdl/xdl.c \
    xdl/xdl_iterate.c \
    xdl/xdl_linker.c \
    xdl/xdl_lzma.c \
    xdl/xdl_util.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_STATIC_LIBRARY)
endif

ifneq ($(filter A2 A3 A4,$(PRISM_DIAGNOSTIC_DEP_STAGE)),)
include $(CLEAR_VARS)
LOCAL_MODULE := libdobby
LOCAL_SRC_FILES := Dobby/$(TARGET_ARCH_ABI)/libdobby.a
include $(PREBUILT_STATIC_LIBRARY)
endif

include $(CLEAR_VARS)
LOCAL_MODULE := prismspace
LOCAL_SRC_FILES :=

ifneq ($(filter B1 B2 B3 B4 B5 B6 B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += PolicyEngine.cpp IO.cpp
endif
ifneq ($(filter B2 B3 B4 B5 B6 B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += hidden_api.cpp
endif
ifneq ($(filter B3 B4 B5 B6 B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += JniHook/JniHook.cpp
endif
ifneq ($(filter B4 B5 B6 B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += Hook/UnixFileSystemHook.cpp
endif
ifneq ($(filter B5 B6 B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += Hook/RuntimeHook.cpp
endif
ifneq ($(filter B6 B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += Hook/DexFileHook.cpp
endif
ifneq ($(filter B7 B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += Hook/VMClassLoaderHook.cpp
endif
ifneq ($(filter B8 B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += Hook/BinderHook.cpp
endif
ifneq ($(filter B4 B5 B6 B7 B8,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += DiagnosticCoreStubs.cpp
endif
ifneq ($(filter B9,$(PRISM_DIAGNOSTIC_SRC_STAGE)),)
LOCAL_SRC_FILES += PrismCore.cpp
endif

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_CFLAGS += -std=c++17
LOCAL_CPPFLAGS += -std=c++17
LOCAL_LDLIBS := -llog -landroid -ldl -lz
# Android 15+ devices may use 16 KB pages. Apply alignment to the actual diagnostic
# build path too; previously it existed only in the legacy/non-diagnostic branch.
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384

ifneq ($(filter A1 A2 A3 A4,$(PRISM_DIAGNOSTIC_DEP_STAGE)),)
LOCAL_STATIC_LIBRARIES += xdl
endif
ifneq ($(filter A2 A3 A4,$(PRISM_DIAGNOSTIC_DEP_STAGE)),)
LOCAL_STATIC_LIBRARIES += libdobby
endif

include $(BUILD_SHARED_LIBRARY)

else

include $(CLEAR_VARS)
LOCAL_MODULE := libdobby
LOCAL_SRC_FILES := Dobby/$(TARGET_ARCH_ABI)/libdobby.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := xdl
LOCAL_CXXFLAGS := -std=c++11 -fno-exceptions -fno-rtti
LOCAL_EXPORT_C_INCLUDES:=$(LOCAL_PATH)
LOCAL_SRC_FILES := xdl/xdl.c \
    xdl/xdl_iterate.c \
    xdl/xdl_linker.c \
    xdl/xdl_lzma.c \
    xdl/xdl_util.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := prismspace
LOCAL_SRC_FILES := PrismCore.cpp \
PolicyEngine.cpp \
hidden_api.cpp \
IO.cpp \
Utils/elf_util.cpp \
Hook/DexFileHook.cpp \
Hook/RuntimeHook.cpp \
Utils/VirtualSpoof.cpp \
Utils/HexDump.cpp \
Utils/AntiDetection.cpp \
Hook/VMClassLoaderHook.cpp \
Hook/UnixFileSystemHook.cpp \
Hook/BinderHook.cpp \
JniHook/JniHook.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_CFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -std=c++17
LOCAL_CPPFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -Werror -fms-extensions
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all,-z,max-page-size=16384
LOCAL_ARM_MODE := arm

LOCAL_CPP_FEATURES := exceptions
LOCAL_STATIC_LIBRARIES := libdobby xdl
LOCAL_LDLIBS := -llog -landroid -lz -ldl
include $(BUILD_SHARED_LIBRARY)

endif
