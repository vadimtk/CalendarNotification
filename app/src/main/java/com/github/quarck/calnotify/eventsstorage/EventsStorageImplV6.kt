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

package com.github.quarck.calnotify.eventsstorage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.github.quarck.calnotify.Consts
import com.github.quarck.calnotify.logs.Logger
import java.io.Closeable
import java.util.*

class EventsStorageImplV6(val context: Context, val helper: SQLiteOpenHelper)
: EventsStorageImplInterface {

    override fun createDb(db: SQLiteDatabase) {
        val CREATE_PKG_TABLE =
            "CREATE " +
                "TABLE ${TABLE_NAME} " +
                "( " +
                "${KEY_EVENTID} INTEGER PRIMARY KEY, " +
                "${KEY_NOTIFICATIONID} INTEGER, " +
                "${KEY_TITLE} TEXT, " +
                "${KEY_DESC} TEXT, " +
                "${KEY_START} INTEGER, " +
                "${KEY_END} INTEGER, " +
                "${KEY_LOCATION} LOCATION, " +
                "${KEY_SNOOZED_UNTIL} INTEGER, " +
                "${KEY_LAST_EVENT_FIRE} INTEGER, " +
                "${KEY_IS_DISPLAYED} INTEGER, " +
                "${KEY_COLOR} INTEGER, " +
                "${KEY_ALERT_TIME} INTEGER, " +
                "${KEY_RESERVED_STR1} TEXT, " +
                "${KEY_RESERVED_STR2} TEXT, " +
                "${KEY_RESERVED_STR3} TEXT, " +
                "${KEY_CALENDAR_ID} INTEGER, " +
                "${KEY_INSTANCE_START} INTEGER, " +
                "${KEY_INSTANCE_END} INTEGER" +
                " )"

        logger.debug("Creating DB TABLE using query: " + CREATE_PKG_TABLE)

        db.execSQL(CREATE_PKG_TABLE)

        val CREATE_INDEX = "CREATE UNIQUE INDEX ${INDEX_NAME} ON ${TABLE_NAME} (${KEY_EVENTID})"

        logger.debug("Creating DB INDEX using query: " + CREATE_INDEX)

        db.execSQL(CREATE_INDEX)
    }

    override fun dropAll(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP INDEX IF EXISTS " + INDEX_NAME);
    }

    override fun addEventImpl(event: EventInstanceRecord) {
        logger.debug("addEvent " + event.eventId)

        val db = helper.writableDatabase

        if (event.notificationId == 0)
            event.notificationId = nextNotificationId(db);

        val values = eventRecordToContentValues(event, true)

        try {
            db.insertOrThrow(TABLE_NAME, // table
                null, // nullColumnHack
                values) // key/value -> keys = column names/ values = column
            // values
            db.close()
        } catch (ex: SQLiteConstraintException) {
            // Close Db before attempting to open it again from another method
            db.close()

            logger.debug("This entry (${event.eventId}) is already in the DB, updating!")

            // persist original notification id in this case
            event.notificationId = getEventImpl(event.eventId, event.instanceStartTime)?.notificationId ?: event.notificationId;

            updateEventImpl(event)
        }
    }

    private fun nextNotificationId(db: SQLiteDatabase): Int {

        var ret = 0;

        val query = "SELECT MAX(${KEY_NOTIFICATIONID}) FROM " + TABLE_NAME

        val cursor = db.rawQuery(query, null)

        if (cursor != null && cursor.moveToFirst()) {
            try {
                ret = cursor.getString(0).toInt() + 1
            } catch (ex: Exception) {
                ret = 0;
            }
        }

        cursor?.close()

        if (ret == 0)
            ret = Consts.NOTIFICATION_ID_DYNAMIC_FROM;

        logger.debug("nextNotificationId, returning $ret")

        return ret
    }

    override fun updateEventImpl(event: EventInstanceRecord) {
        val db = helper.writableDatabase

        val values = eventRecordToContentValues(event)

        logger.debug("Updating event, eventId=${event.eventId}");

        db.update(TABLE_NAME, // table
            values, // column/value
            KEY_EVENTID + " = ?", // selections
            arrayOf<String>(event.eventId.toString())) // selection args

        db.close()
    }

    override fun updateEventsImpl(events: List<EventInstanceRecord>) {
        val db = helper.writableDatabase

        logger.debug("Updating ${events.size} events");

        for (event in events) {
            val values = eventRecordToContentValues(event)

            db.update(TABLE_NAME, // table
                values, // column/value
                KEY_EVENTID + " = ?", // selections
                arrayOf<String>(event.eventId.toString())) // selection args
        }

        db.close()
    }

    override fun getEventImpl(eventId: Long, instanceStartTime: Long): EventInstanceRecord? {
        val db = helper.readableDatabase

        val selection =
            if (instanceStartTime != 0L)
                " $KEY_EVENTID = ? AND $KEY_INSTANCE_START = ?"
            else
                " $KEY_EVENTID = ?"

        val selectionArgs =
            if (instanceStartTime != 0L)
                arrayOf(eventId.toString(), instanceStartTime.toString())
            else
                arrayOf(eventId.toString())

        val cursor = db.query(TABLE_NAME, // a. table
            SELECT_COLUMNS, // b. column names
            selection, // c. selections
            selectionArgs, // d. selections args
            null, // e. group by
            null, // f. having
            null, // g. order by
            null) // h. limit

        var event: EventInstanceRecord? = null

        if (cursor != null) {
            if (cursor.moveToFirst())
                event = cursorToEventRecord(cursor)

            cursor.close()
        }

        return event
    }

    override val eventsImpl: List<EventInstanceRecord>
        get() {
            val ret = LinkedList<EventInstanceRecord>()

            val db = helper.readableDatabase

            val cursor = db.query(TABLE_NAME, // a. table
                SELECT_COLUMNS, // b. column names
                null, // c. selections
                null,
                null, // e. group by
                null, // f. h aving
                null, // g. order by
                null) // h. limit


            if (cursor.moveToFirst()) {
                do {
                    ret.add(cursorToEventRecord(cursor))

                } while (cursor.moveToNext())
            }
            cursor.close()

            logger.debug("eventsImpl, returnint ${ret.size} events")

            return ret
        }

    override fun getActiveEventsImpl(currentTime: Long, threshold: Long): List<EventInstanceRecord> {

        val ret = LinkedList<EventInstanceRecord>()

        val timePlusThr = currentTime + threshold

        val db = helper.readableDatabase

        val cursor = db.query(TABLE_NAME, // a. table
            SELECT_COLUMNS, // b. column names
            " ($KEY_SNOOZED_UNTIL = 0) OR ($KEY_SNOOZED_UNTIL < ?) ", // c. selections
            arrayOf<String>(timePlusThr.toString()), // d. selections args
            "$KEY_LAST_EVENT_FIRE", // e. group by
            null, // f. h aving
            null, // g. order by
            null) // h. limit

        if (cursor.moveToFirst()) {
            do {
                ret.add(cursorToEventRecord(cursor))
            } while (cursor.moveToNext())

        }
        cursor.close()

        logger.debug("getActiveEventsImpl, returning ${ret.size} events")

        return ret
    }

    override fun deleteEventImpl(eventId: Long) {
        val db = helper.writableDatabase

        db.delete(TABLE_NAME, // table name
            KEY_EVENTID + " = ?", // selections
            arrayOf(eventId.toString())) // selections args

        db.close()

        logger.debug("deleteNotification ${eventId}")
    }

    private fun eventRecordToContentValues(event: EventInstanceRecord, includeId: Boolean = false): ContentValues {
        val values = ContentValues();

        if (includeId)
            values.put(KEY_EVENTID, event.eventId);

        values.put(KEY_CALENDAR_ID, event.calendarId)
        values.put(KEY_NOTIFICATIONID, event.notificationId);
        values.put(KEY_TITLE, event.title);
        values.put(KEY_DESC, ""); // we have no description anymore
        values.put(KEY_START, event.startTime);
        values.put(KEY_END, event.endTime);
        values.put(KEY_INSTANCE_START, event.instanceStartTime);
        values.put(KEY_INSTANCE_END, event.instanceEndTime);
        values.put(KEY_LOCATION, event.location);
        values.put(KEY_SNOOZED_UNTIL, event.snoozedUntil);
        values.put(KEY_LAST_EVENT_FIRE, event.lastEventVisibility);
        values.put(KEY_IS_DISPLAYED, event.displayStatus.code);
        values.put(KEY_COLOR, event.color)
        values.put(KEY_ALERT_TIME, event.alertTime)

        return values;
    }

    private fun cursorToEventRecord(cursor: Cursor): EventInstanceRecord {

        return EventInstanceRecord(
            calendarId = (cursor.getLong(0) as Long?) ?: -1L,
            eventId = cursor.getLong(1),
            notificationId = cursor.getInt(2),
            title = cursor.getString(3),
            startTime = cursor.getLong(4),
            endTime = cursor.getLong(5),
            instanceStartTime = cursor.getLong(6),
            instanceEndTime =  cursor.getLong(7),
            location = cursor.getString(8),
            snoozedUntil = cursor.getLong(9),
            lastEventVisibility = cursor.getLong(10),
            displayStatus = EventDisplayStatus.fromInt(cursor.getInt(11)),
            color = cursor.getInt(12),
            alertTime = cursor.getLong(13)
        )
    }

    companion object {
        private val logger = Logger("EventsStorageImplV6")

        private const val TABLE_NAME = "events"
        private const val INDEX_NAME = "eventsIdx"

        private const val KEY_CALENDAR_ID = "i1"
        private const val KEY_EVENTID = "eventId"
        private const val KEY_NOTIFICATIONID = "notificationId"
        private const val KEY_TITLE = "title"
        private const val KEY_DESC = "description"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
        private const val KEY_INSTANCE_START = "i2"
        private const val KEY_INSTANCE_END = "i3"
        private const val KEY_LOCATION = "location"
        private const val KEY_SNOOZED_UNTIL = "snoozeUntil"
        private const val KEY_IS_DISPLAYED = "displayed"
        private const val KEY_LAST_EVENT_FIRE = "lastFire"
        private const val KEY_COLOR = "color"
        private const val KEY_ALERT_TIME = "alertTime"

        private const val KEY_RESERVED_STR1 = "s1"
        private const val KEY_RESERVED_STR2 = "s2"
        private const val KEY_RESERVED_STR3 = "s3"

        private val SELECT_COLUMNS = arrayOf<String>(
            KEY_CALENDAR_ID,
            KEY_EVENTID,
            KEY_NOTIFICATIONID,
            KEY_TITLE,
            KEY_START,
            KEY_END,
            KEY_INSTANCE_START,
            KEY_INSTANCE_END,
            KEY_LOCATION,
            KEY_SNOOZED_UNTIL,
            KEY_LAST_EVENT_FIRE,
            KEY_IS_DISPLAYED,
            KEY_COLOR,
            KEY_ALERT_TIME
        )
    }
}
