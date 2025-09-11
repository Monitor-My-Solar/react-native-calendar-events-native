package com.calendarevents;

import androidx.annotation.Nullable;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CalendarEventsNativePackage extends TurboReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(String name, ReactApplicationContext reactContext) {
        if (name.equals(CalendarEventsNativeModule.NAME)) {
            return new CalendarEventsNativeModule(reactContext);
        } else {
            return null;
        }
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
            boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
            moduleInfos.put(
                    CalendarEventsNativeModule.NAME,
                    new ReactModuleInfo(
                            CalendarEventsNativeModule.NAME,
                            CalendarEventsNativeModule.NAME,
                            false, // canOverrideExistingModule
                            false, // needsEagerInit
                            true, // hasConstants
                            false, // isCxxModule
                            isTurboModule // isTurboModule
                    )
            );
            return moduleInfos;
        };
    }
}