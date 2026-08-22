cmake_minimum_required(VERSION 3.22.1)
project(prismspace_native)

set(CMAKE_C_STANDARD 17)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(PRISM_DIAGNOSTIC_LAYERED_BRINGUP ON)
set(PRISM_DIAGNOSTIC_DEP_STAGE "A2")
set(PRISM_DIAGNOSTIC_SRC_STAGE "B9")

if(PRISM_DIAGNOSTIC_LAYERED_BRINGUP)
    set(PRISM_DEP_STAGES A0 A1 A2 A3 A4)
    set(PRISM_SRC_STAGES B0 B1 B2 B3 B4 B5 B6 B7 B8 B9)

    list(FIND PRISM_DEP_STAGES "${PRISM_DIAGNOSTIC_DEP_STAGE}" PRISM_DEP_STAGE_INDEX)
    list(FIND PRISM_SRC_STAGES "${PRISM_DIAGNOSTIC_SRC_STAGE}" PRISM_SRC_STAGE_INDEX)

    if(PRISM_DEP_STAGE_INDEX EQUAL -1)
        message(FATAL_ERROR "Invalid PRISM_DIAGNOSTIC_DEP_STAGE: ${PRISM_DIAGNOSTIC_DEP_STAGE}")
    endif()
    if(PRISM_SRC_STAGE_INDEX EQUAL -1)
        message(FATAL_ERROR "Invalid PRISM_DIAGNOSTIC_SRC_STAGE: ${PRISM_DIAGNOSTIC_SRC_STAGE}")
    endif()

    add_library(prismspace SHARED)

    set(PRISM_STAGE_SOURCES
        NativeLoadProbe.cpp
    )

    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 1)
        list(APPEND PRISM_STAGE_SOURCES IO.cpp PolicyEngine.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 2)
        list(APPEND PRISM_STAGE_SOURCES hidden_api.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 3)
        list(APPEND PRISM_STAGE_SOURCES JniHook/JniHook.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 4)
        list(APPEND PRISM_STAGE_SOURCES Hook/UnixFileSystemHook.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 5)
        list(APPEND PRISM_STAGE_SOURCES Hook/RuntimeHook.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 6)
        list(APPEND PRISM_STAGE_SOURCES Hook/DexFileHook.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 7)
        list(APPEND PRISM_STAGE_SOURCES Hook/VMClassLoaderHook.cpp)
    endif()
    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 8)
        list(APPEND PRISM_STAGE_SOURCES Hook/BinderHook.cpp)
    endif()

    if((PRISM_SRC_STAGE_INDEX GREATER_EQUAL 4) AND (PRISM_SRC_STAGE_INDEX LESS_EQUAL 8))
        list(APPEND PRISM_STAGE_SOURCES DiagnosticCoreStubs.cpp)
    endif()

    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 9)
        list(APPEND PRISM_STAGE_SOURCES PrismCore.cpp)
    endif()

    target_sources(prismspace PRIVATE ${PRISM_STAGE_SOURCES})

    target_include_directories(prismspace PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
    target_compile_features(prismspace PRIVATE cxx_std_20)
    target_compile_options(prismspace PRIVATE
        -Wall
        -Wextra
        -Werror=return-type
        -Werror=format-security
        -fvisibility=hidden
        -ffunction-sections
        -fdata-sections
        -fms-extensions
        $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:-mbranch-protection=standard>
    )
    target_link_options(prismspace PRIVATE
        -Wl,--gc-sections
        -Wl,--exclude-libs,ALL
        -Wl,--strip-all
        -Wl,-z,max-page-size=16384
        $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:-mbranch-protection=standard>
    )

    if(PRISM_SRC_STAGE_INDEX GREATER_EQUAL 9)
        target_compile_definitions(prismspace PRIVATE PRISM_USE_EXTERNAL_JNI_BOOTSTRAP=1)
    endif()

    find_library(log-lib log)
    find_library(android-lib android)
    find_library(dl-lib dl)
    find_library(z-lib z)

    set(PRISM_STAGE_LINK_LIBS
        ${log-lib}
        ${android-lib}
        ${dl-lib}
    )

    if(PRISM_DEP_STAGE_INDEX GREATER_EQUAL 1)
        add_library(xdl STATIC
            xdl/xdl.c
            xdl/xdl_iterate.c
            xdl/xdl_linker.c
            xdl/xdl_lzma.c
            xdl/xdl_util.c
        )
        list(APPEND PRISM_STAGE_LINK_LIBS xdl ${z-lib})
    endif()

    if(PRISM_DEP_STAGE_INDEX GREATER_EQUAL 2)
        add_library(dobby STATIC IMPORTED)
        set_target_properties(dobby PROPERTIES
            IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/Dobby/${ANDROID_ABI}/libdobby.a
        )
        list(APPEND PRISM_STAGE_LINK_LIBS dobby)
    endif()

    target_link_libraries(prismspace ${PRISM_STAGE_LINK_LIBS})

else()
    add_library(xdl STATIC
        xdl/xdl.c
        xdl/xdl_iterate.c
        xdl/xdl_linker.c
        xdl/xdl_lzma.c
        xdl/xdl_util.c
    )

    add_library(dobby STATIC IMPORTED)
    set_target_properties(dobby PROPERTIES
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/Dobby/${ANDROID_ABI}/libdobby.a
    )

    add_library(prismspace SHARED
        PrismCore.cpp
        hidden_api.cpp
        IO.cpp
        PolicyEngine.cpp
        Utils/elf_util.cpp
        Hook/DexFileHook.cpp
        Hook/RuntimeHook.cpp
        Utils/VirtualSpoof.cpp
        Utils/HexDump.cpp
        Utils/AntiDetection.cpp
        Hook/VMClassLoaderHook.cpp
        Hook/UnixFileSystemHook.cpp
        Hook/BinderHook.cpp
        JniHook/JniHook.cpp
    )

    target_include_directories(prismspace PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
    target_compile_features(prismspace PRIVATE cxx_std_20)
    target_compile_options(prismspace PRIVATE
        -Wall
        -Wextra
        -Werror=return-type
        -Werror=format-security
        -fvisibility=hidden
        -ffunction-sections
        -fdata-sections
        -fms-extensions
        $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:-mbranch-protection=standard>
    )
    target_link_options(prismspace PRIVATE
        -Wl,--gc-sections
        -Wl,--exclude-libs,ALL
        -Wl,--strip-all
        -Wl,-z,max-page-size=16384
        $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:-mbranch-protection=standard>
    )

    find_library(log-lib log)
    find_library(android-lib android)
    find_library(z-lib z)
    find_library(dl-lib dl)

    target_link_libraries(prismspace
        dobby
        xdl
        ${log-lib}
        ${android-lib}
        ${z-lib}
        ${dl-lib}
    )
endif()
