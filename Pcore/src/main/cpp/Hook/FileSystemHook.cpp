



#include "FileSystemHook.h"
#include "Log.h"
#include "xdl.h"
#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>
#include <cstring>
#include <errno.h>


static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_open64)(const char *pathname, int flags, ...) = nullptr;


int new_open(const char *pathname, int flags, ...) {
    
    if (pathname != nullptr) {
        if (strstr(pathname, "resource-cache") || 
            strstr(pathname, "@idmap") || 
            strstr(pathname, ".frro") ||
            strstr(pathname, "systemui") ||
            strstr(pathname, "data@resource-cache@")) {
            PRISM_LOGD("FileSystemHook: Blocking problematic file access: %s", pathname);
            errno = ENOENT;
            return -1;
        }
    }
    
    
    va_list args;
    va_start(args, flags);
    mode_t mode = va_arg(args, mode_t);
    va_end(args);
    
    return orig_open(pathname, flags, mode);
}


int new_open64(const char *pathname, int flags, ...) {
    
    if (pathname != nullptr) {
        if (strstr(pathname, "resource-cache") || 
            strstr(pathname, "@idmap") || 
            strstr(pathname, ".frro") ||
            strstr(pathname, "systemui") ||
            strstr(pathname, "data@resource-cache@")) {
            PRISM_LOGD("FileSystemHook: Blocking problematic file access (64): %s", pathname);
            errno = ENOENT;
            return -1;
        }
    }
    
    
    va_list args;
    va_start(args, flags);
    mode_t mode = va_arg(args, mode_t);
    va_end(args);
    
    return orig_open64(pathname, flags, mode);
}

void FileSystemHook::init() {
    PRISM_LOGD("FileSystemHook: Initializing file system hooks");

    
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    if (!handle) {
        PRISM_LOGE("FileSystemHook: Failed to open libc.so");
        return;
    }
    
    
    orig_open = (int (*)(const char*, int, ...))xdl_sym(handle, "open", nullptr);
    if (orig_open) {
        
        
        PRISM_LOGD("FileSystemHook: Found open function at %p", orig_open);
    } else {
        PRISM_LOGE("FileSystemHook: Failed to find open function");
    }
    
    
    orig_open64 = (int (*)(const char*, int, ...))xdl_sym(handle, "open64", nullptr);
    if (orig_open64) {
        PRISM_LOGD("FileSystemHook: Found open64 function at %p", orig_open64);
    } else {
        PRISM_LOGE("FileSystemHook: Failed to find open64 function");
    }
    
    xdl_close(handle);
}
