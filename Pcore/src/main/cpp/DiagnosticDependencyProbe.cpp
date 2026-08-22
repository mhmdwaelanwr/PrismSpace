#include <cstddef>

#if defined(PRISM_PROBE_XDL)
#include "xdl.h"
#endif

#if defined(PRISM_PROBE_DOBBY)
#include "Dobby/dobby.h"
#endif

// Link-only diagnostic entry point. It is never called by the runtime; its purpose is to make
// staged bring-up builds resolve real symbols from xDL/Dobby instead of reporting a false pass
// because an unused static archive was silently omitted by the linker.
extern "C" __attribute__((used, visibility("default"))) int prism_dependency_link_probe() {
    int linked = 0;

#if defined(PRISM_PROBE_XDL)
    // Keep references to both open/close so the xDL archive must contribute real objects.
    void* (*open_fn)(const char*, int) = &xdl_open;
    void* (*close_fn)(void*) = &xdl_close;
    linked += (open_fn != nullptr && close_fn != nullptr) ? 1 : 0;
#endif

#if defined(PRISM_PROBE_DOBBY)
    // DobbyBuildVersion is side-effect free and forces a real Dobby symbol into the final link.
    const char* (*version_fn)() = &DobbyBuildVersion;
    linked += version_fn != nullptr ? 1 : 0;
#endif

    return linked;
}
