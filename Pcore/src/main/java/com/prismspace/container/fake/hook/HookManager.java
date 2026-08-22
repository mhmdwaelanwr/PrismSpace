package com.prismspace.container.fake.hook;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.prismspace.container.PrismSpaceCore;
import com.prismspace.container.core.EngineCapabilities;
import com.prismspace.container.fake.delegate.AppInstrumentation;

import com.prismspace.container.fake.service.HCallbackProxy;
import com.prismspace.container.fake.service.IAccessibilityManagerProxy;
import com.prismspace.container.fake.service.IAccountManagerProxy;
import com.prismspace.container.fake.service.IActivityClientProxy;
import com.prismspace.container.fake.service.IActivityManagerProxy;
import com.prismspace.container.fake.service.IActivityTaskManagerProxy;
import com.prismspace.container.fake.service.IAlarmManagerProxy;
import com.prismspace.container.fake.service.IAppOpsManagerProxy;
import com.prismspace.container.fake.service.IAppWidgetManagerProxy;
import com.prismspace.container.fake.service.IAttributionSourceProxy;
import com.prismspace.container.fake.service.IAutofillManagerProxy;
import com.prismspace.container.fake.service.ISensitiveContentProtectionManagerProxy;
import com.prismspace.container.fake.service.ISettingsSystemProxy;
import com.prismspace.container.fake.service.IConnectivityManagerProxy;
import com.prismspace.container.fake.service.ISystemSensorManagerProxy;
import com.prismspace.container.fake.service.IContentProviderProxy;
import com.prismspace.container.fake.service.IXiaomiAttributionSourceProxy;
import com.prismspace.container.fake.service.IXiaomiSettingsProxy;
import com.prismspace.container.fake.service.IXiaomiMiuiServicesProxy;
import com.prismspace.container.fake.service.IDnsResolverProxy;
import com.prismspace.container.fake.service.IContextHubServiceProxy;
import com.prismspace.container.fake.service.IDeviceIdentifiersPolicyProxy;
import com.prismspace.container.fake.service.IDevicePolicyManagerProxy;
import com.prismspace.container.fake.service.IDisplayManagerProxy;
import com.prismspace.container.fake.service.IFingerprintManagerProxy;
import com.prismspace.container.fake.service.IGraphicsStatsProxy;
import com.prismspace.container.fake.service.IJobServiceProxy;
import com.prismspace.container.fake.service.ILauncherAppsProxy;
import com.prismspace.container.fake.service.ILocationManagerProxy;
import com.prismspace.container.fake.service.IMediaRouterServiceProxy;
import com.prismspace.container.fake.service.IMediaSessionManagerProxy;
import com.prismspace.container.fake.service.IAudioServiceProxy;
import com.prismspace.container.fake.service.ISensorPrivacyManagerProxy;
import com.prismspace.container.fake.service.ContentResolverProxy;
import com.prismspace.container.fake.service.IWebViewUpdateServiceProxy;
import com.prismspace.container.fake.service.IMiuiSecurityManagerProxy;
import com.prismspace.container.fake.service.SystemLibraryProxy;
import com.prismspace.container.fake.service.ReLinkerProxy;
import com.prismspace.container.fake.service.WebViewProxy;
import com.prismspace.container.fake.service.WebViewFactoryProxy;
import com.prismspace.container.fake.service.MediaRecorderProxy;
import com.prismspace.container.fake.service.AudioRecordProxy;
import com.prismspace.container.fake.service.MediaRecorderClassProxy;
import com.prismspace.container.fake.service.SQLiteDatabaseProxy;
import com.prismspace.container.fake.service.ClassLoaderProxy;
import com.prismspace.container.fake.service.FileSystemProxy;
import com.prismspace.container.fake.service.GmsProxy;
import com.prismspace.container.fake.service.LevelDbProxy;
import com.prismspace.container.fake.service.DeviceIdProxy;
import com.prismspace.container.fake.service.GoogleAccountManagerProxy;
import com.prismspace.container.fake.service.AuthenticationProxy;
import com.prismspace.container.fake.service.AndroidIdProxy;
import com.prismspace.container.fake.service.AudioPermissionProxy;

import com.prismspace.container.fake.service.INetworkManagementServiceProxy;
import com.prismspace.container.fake.service.INotificationManagerProxy;
import com.prismspace.container.fake.service.IPackageManagerProxy;
import com.prismspace.container.fake.service.IPermissionManagerProxy;
import com.prismspace.container.fake.service.IPersistentDataBlockServiceProxy;
import com.prismspace.container.fake.service.IPhoneSubInfoProxy;
import com.prismspace.container.fake.service.IPowerManagerProxy;
import com.prismspace.container.fake.service.ApkAssetsProxy;
import com.prismspace.container.fake.service.ResourcesManagerProxy;
import com.prismspace.container.fake.service.IShortcutManagerProxy;
import com.prismspace.container.fake.service.IStorageManagerProxy;
import com.prismspace.container.fake.service.IStorageStatsManagerProxy;
import com.prismspace.container.fake.service.ISystemUpdateProxy;
import com.prismspace.container.fake.service.ITelephonyManagerProxy;
import com.prismspace.container.fake.service.ITelephonyRegistryProxy;
import com.prismspace.container.fake.service.IUserManagerProxy;
import com.prismspace.container.fake.service.IVibratorServiceProxy;
import com.prismspace.container.fake.service.IVpnManagerProxy;
import com.prismspace.container.fake.service.IWifiManagerProxy;
import com.prismspace.container.fake.service.IWifiScannerProxy;
import com.prismspace.container.fake.service.IWindowManagerProxy;
import com.prismspace.container.fake.service.context.ContentServiceStub;
import com.prismspace.container.fake.service.context.RestrictionsManagerStub;
import com.prismspace.container.fake.service.libcore.OsStub;
import com.prismspace.container.utils.Slog;
import com.prismspace.container.utils.compat.PuildCompat;
import com.prismspace.container.fake.service.ISettingsProviderProxy;
import com.prismspace.container.fake.service.FeatureFlagUtilsProxy;
import com.prismspace.container.fake.service.WorkManagerProxy;

public class HookManager {
    public static final String TAG = "HookManager";

    private static final HookManager sHookManager = new HookManager();

    private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

    public static HookManager get() {
        return sHookManager;
    }

    public void init() {
        if (PrismSpaceCore.get().isBlackProcess() || PrismSpaceCore.get().isServerProcess()) {
            addInjector(new IDisplayManagerProxy());
            addInjector(new OsStub());
            addInjector(new IActivityManagerProxy());
            addInjector(new IPackageManagerProxy());
            addInjector(new ITelephonyManagerProxy());
            addInjector(new HCallbackProxy());
            addInjector(new IAppOpsManagerProxy());
            addInjector(new INotificationManagerProxy());
            addInjector(new IAlarmManagerProxy());
            addInjector(new IAppWidgetManagerProxy());
            addInjector(new ContentServiceStub());
            addInjector(new IWindowManagerProxy());
            addInjector(new IUserManagerProxy());
            addInjector(new RestrictionsManagerStub());
            addInjector(new IMediaSessionManagerProxy());
            addInjector(new IAudioServiceProxy());
            addInjector(new ISensorPrivacyManagerProxy());
            addInjector(new ContentResolverProxy());
            addInjector(new IWebViewUpdateServiceProxy());
            addInjector(new SystemLibraryProxy());
            addInjector(new ReLinkerProxy());
            addInjector(new WebViewProxy());
            addInjector(new WebViewFactoryProxy());
            addInjector(new WorkManagerProxy());
            addInjector(new MediaRecorderProxy());
            addInjector(new AudioRecordProxy());
            addInjector(new IMiuiSecurityManagerProxy());
            addInjector(new ISettingsProviderProxy());
            addInjector(new FeatureFlagUtilsProxy());
            addInjector(new MediaRecorderClassProxy());
            addInjector(new SQLiteDatabaseProxy());
            addInjector(new ClassLoaderProxy());
            addInjector(new FileSystemProxy());
            addInjector(new GmsProxy());
            addInjector(new LevelDbProxy());
            addInjector(new DeviceIdProxy());
            addInjector(new GoogleAccountManagerProxy());
            addInjector(new AuthenticationProxy());
            addInjector(new AndroidIdProxy());
            addInjector(new AudioPermissionProxy());
            addInjector(new ILocationManagerProxy());
            addInjector(new IStorageManagerProxy());
            addInjector(new ILauncherAppsProxy());
            addInjector(new IJobServiceProxy());
            addInjector(new IAccessibilityManagerProxy());
            addInjector(new ITelephonyRegistryProxy());
            addInjector(new IDevicePolicyManagerProxy());
            addInjector(new IAccountManagerProxy());
            addInjector(new IConnectivityManagerProxy());
            addInjector(new IDnsResolverProxy());
            addInjector(new IAttributionSourceProxy());
            addInjector(new IContentProviderProxy());
            addInjector(new ISettingsSystemProxy());
            addInjector(new ISystemSensorManagerProxy());
            addInjector(new IXiaomiAttributionSourceProxy());
            addInjector(new IXiaomiSettingsProxy());
            addInjector(new IXiaomiMiuiServicesProxy());
            addInjector(new IPhoneSubInfoProxy());
            addInjector(new IMediaRouterServiceProxy());
            addInjector(new IPowerManagerProxy());
            addInjector(new IContextHubServiceProxy());
            addInjector(new IVibratorServiceProxy());
            addInjector(new IPersistentDataBlockServiceProxy());
            addInjector(AppInstrumentation.get());
            addInjector(new IWifiManagerProxy());
            addInjector(new IWifiScannerProxy());
            addInjector(new ApkAssetsProxy());
            addInjector(new ResourcesManagerProxy());

            if (PuildCompat.isS()) {
                addInjector(new IActivityClientProxy(null));
                addInjector(new IVpnManagerProxy());
            }

            if (PuildCompat.isS()) {
                addInjector(new ISensitiveContentProtectionManagerProxy());
            }

            if (PuildCompat.isR()) {
                addInjector(new IPermissionManagerProxy());
            }

            if (PuildCompat.isQ()) {
                addInjector(new IActivityTaskManagerProxy());
            }

            if (PuildCompat.isPie()) {
                addInjector(new ISystemUpdateProxy());
            }

            if (PuildCompat.isOreo()) {
                addInjector(new IAutofillManagerProxy());
                addInjector(new IDeviceIdentifiersPolicyProxy());
                addInjector(new IStorageStatsManagerProxy());
            }

            if (PuildCompat.isN_MR1()) {
                addInjector(new IShortcutManagerProxy());
            }

            if (PuildCompat.isN()) {
                addInjector(new INetworkManagementServiceProxy());
            }

            if (PuildCompat.isM()) {
                addInjector(new IFingerprintManagerProxy());
                addInjector(new IGraphicsStatsProxy());
            }

            if (PuildCompat.isL()) {
                addInjector(new IJobServiceProxy());
            }
        }
        injectAll();
    }

    public void checkEnv(Class<?> clazz) {
        IInjectHook iInjectHook = mInjectors.get(clazz);
        if (iInjectHook != null && iInjectHook.isBadEnv()) {
            Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
            iInjectHook.injectHook();
            publishHookCapability(iInjectHook, isHookHealthy(iInjectHook), "environment recheck");
        }
    }

    public void checkAll() {
        for (Class<?> aClass : mInjectors.keySet()) {
            IInjectHook iInjectHook = mInjectors.get(aClass);
            if (iInjectHook != null && iInjectHook.isBadEnv()) {
                Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
                iInjectHook.injectHook();
            }
            if (iInjectHook != null) {
                publishHookCapability(iInjectHook, isHookHealthy(iInjectHook), "environment check");
            }
        }
    }

    void addInjector(IInjectHook injectHook) {
        mInjectors.put(injectHook.getClass(), injectHook);
    }

    void injectAll() {
        for (IInjectHook value : mInjectors.values()) {
            try {
                Slog.d(TAG, "hook: " + value);
                value.injectHook();
                boolean healthy = isHookHealthy(value);
                publishHookCapability(value, healthy, healthy ? "injected" : "inject returned but environment is bad");
            } catch (Exception e) {
                Slog.d(TAG, "hook error: " + value);
                publishHookCapability(value, false, e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()));
                handleHookError(value, e);
            }
        }
    }

    private void handleHookError(IInjectHook hook, Exception e) {
        String hookName = hook.getClass().getSimpleName();
        Slog.e(TAG, "Hook failed: " + hookName + " - " + e.getMessage(), e);

        if (hookName.contains("ActivityManager") ||
            hookName.contains("PackageManager") ||
            hookName.contains("WebView") ||
            hookName.contains("ContentProvider")) {

            Slog.w(TAG, "Critical hook failed: " + hookName + ", attempting recovery");

            try {
                if (hook.isBadEnv()) {
                    Slog.d(TAG, "Attempting to recover hook: " + hookName);
                    hook.injectHook();
                }
                publishHookCapability(hook, isHookHealthy(hook), "recovery attempted");
            } catch (Exception recoveryException) {
                publishHookCapability(hook, false,
                        "recovery failed: " + recoveryException.getClass().getSimpleName());
                Slog.e(TAG, "Hook recovery failed: " + hookName, recoveryException);
            }
        }
    }

    private boolean isHookHealthy(IInjectHook hook) {
        try {
            return !hook.isBadEnv();
        } catch (Throwable t) {
            Slog.w(TAG, "Unable to verify hook health: " + hook.getClass().getSimpleName(), t);
            return false;
        }
    }

    private void publishHookCapability(IInjectHook hook, boolean healthy, String detail) {
        EngineCapabilities.Component component = capabilityFor(hook);
        if (component == null) return;
        EngineCapabilities.get().mark(
                component,
                healthy ? EngineCapabilities.State.ACTIVE : EngineCapabilities.State.FAILED,
                hook.getClass().getSimpleName() + ": " + detail);
    }

    private EngineCapabilities.Component capabilityFor(IInjectHook hook) {
        if (hook instanceof IActivityManagerProxy) {
            return EngineCapabilities.Component.ACTIVITY_MANAGER;
        }
        if (hook instanceof IPackageManagerProxy) {
            return EngineCapabilities.Component.PACKAGE_MANAGER;
        }
        if (hook instanceof IActivityTaskManagerProxy) {
            return EngineCapabilities.Component.ACTIVITY_TASK_MANAGER;
        }
        if (hook instanceof HCallbackProxy) {
            return EngineCapabilities.Component.H_CALLBACK;
        }
        return null;
    }

    public boolean areCriticalHooksInstalled() {
        String[] criticalHooks = {
            "IActivityManagerProxy",
            "IPackageManagerProxy",
            "HCallbackProxy"
        };

        for (String hookName : criticalHooks) {
            IInjectHook matched = null;
            for (IInjectHook hook : mInjectors.values()) {
                if (hook.getClass().getSimpleName().equals(hookName)) {
                    matched = hook;
                    break;
                }
            }
            if (matched == null) {
                Slog.w(TAG, "Critical hook missing: " + hookName);
                return false;
            }
            if (!isHookHealthy(matched)) {
                Slog.w(TAG, "Critical hook unhealthy: " + hookName);
                return false;
            }
        }

        Slog.d(TAG, "All critical hooks are installed and healthy");
        return true;
    }

    public void reinitializeHooks() {
        Slog.d(TAG, "Reinitializing all hooks");
        mInjectors.clear();
        init();
        Slog.d(TAG, "Hook reinitialization completed");
    }
}
