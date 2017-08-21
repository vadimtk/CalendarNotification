//
//   Calendar Notifications Plus
//   Copyright (C) 2017 Sergey Parshin (s.parshin.sc@gmail.com)
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

package com.github.quarck.calnotify.calendareditor

import android.content.Context
import com.github.quarck.calnotify.Consts
import com.github.quarck.calnotify.calendar.CalendarProviderInterface
import com.github.quarck.calnotify.calendar.EventAlertRecord
import com.github.quarck.calnotify.calendareditor.storage.*
import com.github.quarck.calnotify.logs.DevLog
import com.github.quarck.calnotify.permissions.PermissionsManager

class CalendarChangeManager(val provider: CalendarProviderInterface): CalendarChangeManagerInterface {

    override fun createEvent(context: Context, event: CalendarChangeRequest): Boolean {

        DevLog.info(context, LOG_TAG, "Request to create an event")

        if (!PermissionsManager.hasAllPermissions(context)) {
            DevLog.error(context, LOG_TAG, "createEvent: no permissions");
            return false;
        }

        val ret =
            CalendarChangeRequestsStorage(context).use {
                db ->

                db.add(event)

                val eventId = provider.createEvent(context, event)

                if (eventId != -1L) {
                    DevLog.info(context, LOG_TAG, "Created new event, id $eventId")

                    event.eventId = eventId
                    db.update(event)
                    true
                }
                else {
                    DevLog.info(context, LOG_TAG, "Failed to create a new event, will retry later")

                    false
                }
            }

        return ret
    }

    override fun moveEvent(context: Context, event: EventAlertRecord, addTimeMillis: Long): Boolean {

        var ret = false

        if (!PermissionsManager.hasAllPermissions(context)) {
            DevLog.error(context, LOG_TAG, "moveEvent: no permissions");
            return false;
        }

        CalendarChangeRequestsStorage(context).use {
            db ->

            db.deleteForEventId(event.eventId)

            val oldStartTime = event.startTime
            val oldEndTime = event.endTime

            val newStartTime: Long
            val newEndTime: Long

            val currentTime = System.currentTimeMillis()

            val numSecondsInThePast = currentTime + Consts.ALARM_THRESHOLD - event.startTime

            if (numSecondsInThePast > 0) {
                val addUnits = numSecondsInThePast / addTimeMillis + 1

                newStartTime = event.startTime + addTimeMillis * addUnits
                newEndTime = event.endTime + addTimeMillis * addUnits

                if (addUnits != 1L)
                    DevLog.error(context, LOG_TAG, "Requested time is already in the past, total added time: ${addTimeMillis * addUnits}")

                ret = provider.moveEvent(context, event.eventId, newStartTime, newEndTime)
                event.startTime = newStartTime
                event.endTime = newEndTime
            }
            else {
                newStartTime = event.startTime + addTimeMillis
                newEndTime = event.endTime + addTimeMillis

                ret = provider.moveEvent(context, event.eventId, newStartTime, newEndTime)
                event.startTime = newStartTime
                event.endTime = newEndTime
            }

            DevLog.info(context, LOG_TAG, "Provider move event for ${event.eventId} result: $ret")

            db.add(
                    CalendarChangeRequest(
                            id = -1L,
                            type = EventChangeRequestType.MoveExistingEvent,
                            eventId = event.eventId,
                            calendarId = event.calendarId,
                            status = EventChangeStatus.Dirty,
                            details = CalendarEventDetails(
                                    title = event.title,
                                    desc = "", // TBD
                                    startTime = newStartTime,
                                    endTime = newEndTime,

                                    colour = event.color,
                                    location = event.location,
                                    timezone = "",
                                    isAllDay = event.isAllDay,
                                    reminders = listOf<EventCreationRequestReminder>()
                            ),
                            oldDetails = CalendarEventDetails(
                                    title = event.title,
                                    desc = "", // TBD
                                    startTime = oldStartTime,
                                    endTime = oldEndTime,

                                    colour = event.color,
                                    location = event.location,
                                    timezone = "",
                                    isAllDay = event.isAllDay,
                                    reminders = listOf<EventCreationRequestReminder>()
                            )
                    )
            )
        }

        return ret
    }

    companion object {
        const val LOG_TAG = "CalendarChangeMonitor"
    }
}