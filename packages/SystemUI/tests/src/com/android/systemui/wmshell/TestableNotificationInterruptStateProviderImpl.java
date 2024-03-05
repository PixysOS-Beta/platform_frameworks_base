/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.wmshell;

<<<<<<< HEAD
import android.content.ContentResolver;
import android.content.Context;
=======
>>>>>>> 378466bed3dc5d28851ae521d6bc3c78a8136f26
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Handler;
import android.os.PowerManager;

import com.android.internal.logging.UiEventLogger;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.notification.NotifPipelineFlags;
import com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProvider;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptLogger;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.EventLog;
import com.android.systemui.util.settings.GlobalSettings;
import com.android.systemui.util.time.SystemClock;

public class TestableNotificationInterruptStateProviderImpl
        extends NotificationInterruptStateProviderImpl {

    TestableNotificationInterruptStateProviderImpl(
<<<<<<< HEAD
            Context context,
            ContentResolver contentResolver,
=======
>>>>>>> 378466bed3dc5d28851ae521d6bc3c78a8136f26
            PowerManager powerManager,
            AmbientDisplayConfiguration ambientDisplayConfiguration,
            StatusBarStateController statusBarStateController,
            KeyguardStateController keyguardStateController,
            BatteryController batteryController,
            HeadsUpManager headsUpManager,
            NotificationInterruptLogger logger,
            Handler mainHandler,
            NotifPipelineFlags flags,
            KeyguardNotificationVisibilityProvider keyguardNotificationVisibilityProvider,
            UiEventLogger uiEventLogger,
<<<<<<< HEAD
            UserTracker userTracker) {
        super(context,
                contentResolver,
=======
            UserTracker userTracker,
            DeviceProvisionedController deviceProvisionedController,
            SystemClock systemClock,
            GlobalSettings globalSettings,
            EventLog eventLog) {
        super(
>>>>>>> 378466bed3dc5d28851ae521d6bc3c78a8136f26
                powerManager,
                ambientDisplayConfiguration,
                batteryController,
                statusBarStateController,
                keyguardStateController,
                headsUpManager,
                logger,
                mainHandler,
                flags,
                keyguardNotificationVisibilityProvider,
                uiEventLogger,
                userTracker,
                deviceProvisionedController,
                systemClock,
                globalSettings,
                eventLog);
        mUseHeadsUp = true;
    }
}
