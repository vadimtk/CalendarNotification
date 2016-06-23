//
//   Calendar Notifications Plus  
//   Copyright (C) 2016 Sergey Parshin (s.parshin.sc@gmail.com)
//
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation; either version 3 of the License, or
//   (at your option) any later version.
// 
//   This program is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with this program; if not, write to the Free Software Foundation,
//   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
//

package com.github.quarck.calnotify

object Consts {

    // global log prefix for all the messages generated by this app
    const val LOG_PREFIX = "CalNotify_"

    const val REMINDER_WAKE_LOCK_NAME = "com.github.quarck.calnotify.reminderwakelock"
    const val SCREEN_WAKE_LOCK_NAME = "com.github.quarck.calnotify.screen.wake.notification"

    const val MIN_REMINDER_INTERVAL = 60 * 1000

    const val COMPACT_VIEW_DEFAULT_SINCE_VER = 1008

    const val DAY_IN_MILLISECONDS = 24L * 3600L * 1000L
    const val DAY_IN_SECONDS: Long = 3600L * 24
    const val HOUR_IN_SECONDS: Long = 3600L
    const val MINUTE_IN_SECONDS: Long = 60L;

    const val NOTIFICATION_ID_COLLAPSED = 0;
    const val NOTIFICATION_ID_DYNAMIC_FROM = 1;

    //
    const val INTENT_NOTIFICATION_ID_KEY = "notificationId";
    const val INTENT_EVENT_ID_KEY = "eventId";
    const val INTENT_INSTANCE_START_TIME_KEY = "instanceStartTime"
    const val INTENT_SNOOZE_ALL_IS_CHANGE = "snooze_all_is_change"
    const val INTENT_SNOOZE_FROM_MAIN_ACTIVITY = "snooze_by_main_activity"

    const val INTENT_IS_USER_ACTION = "causedByUser"

    // max number of notifications displayed on the screen at all the times
    const val MAX_NOTIFICATIONS = 8;

    //
    private const val VIBRATION_DURATION: Long = 1200;
    val VIBRATION_PATTERNS by lazy {
        arrayOf(
            longArrayOf(0, VIBRATION_DURATION), // Single long
            longArrayOf(0, 2*VIBRATION_DURATION),
            longArrayOf(0, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5),
            longArrayOf(0, VIBRATION_DURATION, VIBRATION_DURATION/3, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5),
            longArrayOf(0, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/5, VIBRATION_DURATION/3, VIBRATION_DURATION)
        )
    }

    const val WAKE_SCREEN_DURATION = 100L

    const val LED_DURATION_ON = 300;
    const val LED_DURATION_OFF = 2000;
    const val DEFAULT_LED_PATTERN = "$LED_DURATION_ON,$LED_DURATION_OFF"
    const val LED_MIN_DURATION = 100
    const val LED_MAX_DURATION = 2500

    const val DEFAULT_LED_COLOR = 0x7f0000ff;

    const val ALARM_THRESHOULD = 15 * 1000L;

    val DEFAULT_SNOOZE_PRESETS = longArrayOf(15 * 60 * 1000, 60 * 60 * 1000, 4 * 60 * 60 * 1000, 24 * 60 * 60 * 1000);

    const val MAX_SUPPORTED_PRESETS = 6

    const val DEFAULT_CALENDAR_EVENT_COLOR = 0xff0000ff.toInt()

    // Only auto-dismisss event notification if we can confirm that event was moved into the future
    // by at least 1hr
    const val EVENT_MOVED_THRESHOLD = 60*60*1000L

    const val SAMSUNG_KEYWORD = "samsung"
}
