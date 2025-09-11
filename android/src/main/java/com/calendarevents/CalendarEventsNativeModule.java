package com.calendarevents;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@ReactModule(name = CalendarEventsNativeModule.NAME)
public class CalendarEventsNativeModule extends ReactContextBaseJavaModule {
    public static final String NAME = "CalendarEventsNative";
    private static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    
    static {
        ISO_8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public CalendarEventsNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    // Permission methods
    @ReactMethod
    public void requestPermissions(boolean writeOnly, Promise promise) {
        // Permission handling is done in JavaScript side using PermissionsAndroid
        // This is just a placeholder for consistency with iOS
        checkPermissions(writeOnly, promise);
    }

    @ReactMethod
    public void checkPermissions(boolean writeOnly, Promise promise) {
        Context context = getReactApplicationContext();
        boolean hasReadPermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        boolean hasWritePermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        
        if (writeOnly) {
            promise.resolve(hasWritePermission ? "granted" : "denied");
        } else {
            promise.resolve(hasReadPermission && hasWritePermission ? "granted" : "denied");
        }
    }

    // Calendar methods
    @ReactMethod
    public void fetchAllCalendars(Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        
        String[] projection = new String[] {
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.IS_PRIMARY,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_ACCESS_LEVEL
        };
        
        Cursor cursor = cr.query(Calendars.CONTENT_URI, projection, null, null, null);
        WritableArray calendars = Arguments.createArray();
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                WritableMap calendar = Arguments.createMap();
                calendar.putString("id", cursor.getString(0));
                calendar.putString("title", cursor.getString(1));
                calendar.putString("source", cursor.getString(2));
                calendar.putString("type", cursor.getString(3));
                calendar.putBoolean("isPrimary", cursor.getInt(4) == 1);
                calendar.putString("color", String.format("#%06X", (0xFFFFFF & cursor.getInt(5))));
                calendar.putBoolean("allowsModifications", 
                    cursor.getInt(6) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR);
                
                WritableArray availabilities = Arguments.createArray();
                availabilities.pushString("busy");
                availabilities.pushString("free");
                calendar.putArray("allowedAvailabilities", availabilities);
                
                calendars.pushMap(calendar);
            }
            cursor.close();
        }
        
        promise.resolve(calendars);
    }

    @ReactMethod
    public void findOrCreateCalendar(ReadableMap calendarMap, Promise promise) {
        String title = calendarMap.hasKey("title") ? calendarMap.getString("title") : "Calendar";
        
        // First, try to find existing calendar
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        String[] projection = new String[] { Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME };
        Cursor cursor = cr.query(Calendars.CONTENT_URI, projection, 
            Calendars.CALENDAR_DISPLAY_NAME + " = ?", new String[] { title }, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String calendarId = cursor.getString(0);
            cursor.close();
            promise.resolve(calendarId);
            return;
        }
        
        if (cursor != null) {
            cursor.close();
        }
        
        // Create new calendar
        ContentValues values = new ContentValues();
        values.put(Calendars.ACCOUNT_NAME, "CalendarEventsNative");
        values.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(Calendars.NAME, title);
        values.put(Calendars.CALENDAR_DISPLAY_NAME, title);
        values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        values.put(Calendars.OWNER_ACCOUNT, "CalendarEventsNative");
        values.put(Calendars.VISIBLE, 1);
        values.put(Calendars.SYNC_EVENTS, 1);
        
        if (calendarMap.hasKey("color")) {
            String colorHex = calendarMap.getString("color");
            int color = (int) Long.parseLong(colorHex.replace("#", ""), 16);
            values.put(Calendars.CALENDAR_COLOR, color);
        }
        
        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");
        builder.appendQueryParameter(Calendars.ACCOUNT_NAME, "CalendarEventsNative");
        builder.appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        
        Uri uri = cr.insert(builder.build(), values);
        if (uri != null) {
            promise.resolve(uri.getLastPathSegment());
        } else {
            promise.reject("CALENDAR_CREATION_FAILED", "Failed to create calendar");
        }
    }

    @ReactMethod
    public void removeCalendar(String calendarId, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, Long.parseLong(calendarId));
        int rows = cr.delete(uri, null, null);
        promise.resolve(rows > 0);
    }

    // Event methods
    @ReactMethod
    public void fetchAllEvents(String startDate, String endDate, ReadableArray calendarIds, Promise promise) {
        long startMillis = parseDate(startDate);
        long endMillis = parseDate(endDate);
        
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        
        String selection = Events.DTSTART + " >= ? AND " + Events.DTSTART + " <= ?";
        String[] selectionArgs = new String[] { String.valueOf(startMillis), String.valueOf(endMillis) };
        
        if (calendarIds != null && calendarIds.size() > 0) {
            StringBuilder calendarSelection = new StringBuilder(" AND " + Events.CALENDAR_ID + " IN (");
            for (int i = 0; i < calendarIds.size(); i++) {
                if (i > 0) calendarSelection.append(",");
                calendarSelection.append(calendarIds.getString(i));
            }
            calendarSelection.append(")");
            selection += calendarSelection.toString();
        }
        
        String[] projection = new String[] {
            Events._ID,
            Events.TITLE,
            Events.DESCRIPTION,
            Events.DTSTART,
            Events.DTEND,
            Events.ALL_DAY,
            Events.EVENT_LOCATION,
            Events.CALENDAR_ID,
            Events.AVAILABILITY,
            Events.RRULE,
            Events.CUSTOM_APP_URI
        };
        
        Cursor cursor = cr.query(Events.CONTENT_URI, projection, selection, selectionArgs, null);
        WritableArray events = Arguments.createArray();
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                WritableMap event = serializeEvent(cursor);
                events.pushMap(event);
            }
            cursor.close();
        }
        
        promise.resolve(events);
    }

    @ReactMethod
    public void findEventById(String eventId, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, Long.parseLong(eventId));
        
        String[] projection = new String[] {
            Events._ID,
            Events.TITLE,
            Events.DESCRIPTION,
            Events.DTSTART,
            Events.DTEND,
            Events.ALL_DAY,
            Events.EVENT_LOCATION,
            Events.CALENDAR_ID,
            Events.AVAILABILITY,
            Events.RRULE,
            Events.CUSTOM_APP_URI
        };
        
        Cursor cursor = cr.query(uri, projection, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            WritableMap event = serializeEvent(cursor);
            cursor.close();
            promise.resolve(event);
        } else {
            promise.resolve(null);
        }
    }

    @ReactMethod
    public void saveEvent(ReadableMap eventMap, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        ContentValues values = new ContentValues();
        
        applyEventProperties(eventMap, values);
        
        // Set default calendar if not specified
        if (!values.containsKey(Events.CALENDAR_ID)) {
            values.put(Events.CALENDAR_ID, getDefaultCalendarId());
        }
        
        Uri uri = cr.insert(Events.CONTENT_URI, values);
        if (uri != null) {
            String eventId = uri.getLastPathSegment();
            
            // Add alarms if specified
            if (eventMap.hasKey("alarms")) {
                ReadableArray alarms = eventMap.getArray("alarms");
                for (int i = 0; i < alarms.size(); i++) {
                    ReadableMap alarm = alarms.getMap(i);
                    addReminder(eventId, alarm);
                }
            }
            
            promise.resolve(eventId);
        } else {
            promise.reject("EVENT_SAVE_FAILED", "Failed to save event");
        }
    }

    @ReactMethod
    public void updateEvent(String eventId, ReadableMap eventMap, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        ContentValues values = new ContentValues();
        
        applyEventProperties(eventMap, values);
        
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, Long.parseLong(eventId));
        int rows = cr.update(uri, values, null, null);
        
        if (rows > 0) {
            // Update alarms if specified
            if (eventMap.hasKey("alarms")) {
                // Remove existing reminders
                cr.delete(Reminders.CONTENT_URI, Reminders.EVENT_ID + " = ?", new String[] { eventId });
                
                // Add new reminders
                ReadableArray alarms = eventMap.getArray("alarms");
                for (int i = 0; i < alarms.size(); i++) {
                    ReadableMap alarm = alarms.getMap(i);
                    addReminder(eventId, alarm);
                }
            }
            
            promise.resolve(eventId);
        } else {
            promise.reject("EVENT_UPDATE_FAILED", "Failed to update event");
        }
    }

    @ReactMethod
    public void removeEvent(String eventId, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, Long.parseLong(eventId));
        int rows = cr.delete(uri, null, null);
        promise.resolve(rows > 0);
    }

    @ReactMethod
    public void openEventInCalendar(String eventId, Promise promise) {
        // Android doesn't support opening events directly in the calendar app
        // We can only open the calendar app
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("content://com.android.calendar/time"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            getReactApplicationContext().startActivity(intent);
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("OPEN_CALENDAR_FAILED", "Failed to open calendar app", e);
        }
    }

    // Helper methods
    private void applyEventProperties(ReadableMap eventMap, ContentValues values) {
        if (eventMap.hasKey("title")) {
            values.put(Events.TITLE, eventMap.getString("title"));
        }
        
        if (eventMap.hasKey("startDate")) {
            values.put(Events.DTSTART, parseDate(eventMap.getString("startDate")));
        }
        
        if (eventMap.hasKey("endDate")) {
            values.put(Events.DTEND, parseDate(eventMap.getString("endDate")));
        }
        
        if (eventMap.hasKey("location")) {
            values.put(Events.EVENT_LOCATION, eventMap.getString("location"));
        }
        
        if (eventMap.hasKey("notes")) {
            values.put(Events.DESCRIPTION, eventMap.getString("notes"));
        }
        
        if (eventMap.hasKey("url")) {
            values.put(Events.CUSTOM_APP_URI, eventMap.getString("url"));
        }
        
        if (eventMap.hasKey("allDay")) {
            values.put(Events.ALL_DAY, eventMap.getBoolean("allDay") ? 1 : 0);
        }
        
        if (eventMap.hasKey("calendar")) {
            values.put(Events.CALENDAR_ID, Long.parseLong(eventMap.getString("calendar")));
        }
        
        if (eventMap.hasKey("availability")) {
            String availability = eventMap.getString("availability");
            int availabilityValue = Events.AVAILABILITY_BUSY;
            if ("free".equals(availability)) {
                availabilityValue = Events.AVAILABILITY_FREE;
            } else if ("tentative".equals(availability)) {
                availabilityValue = Events.AVAILABILITY_TENTATIVE;
            }
            values.put(Events.AVAILABILITY, availabilityValue);
        }
        
        if (eventMap.hasKey("recurrence")) {
            ReadableMap recurrence = eventMap.getMap("recurrence");
            String rrule = buildRRule(recurrence);
            if (!TextUtils.isEmpty(rrule)) {
                values.put(Events.RRULE, rrule);
                values.put(Events.DURATION, "P3600S"); // Default 1 hour duration for recurring events
            }
        }
        
        values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
    }

    private WritableMap serializeEvent(Cursor cursor) {
        WritableMap event = Arguments.createMap();
        
        event.putString("id", cursor.getString(0));
        event.putString("title", cursor.getString(1));
        event.putString("notes", cursor.getString(2));
        event.putString("startDate", formatDate(cursor.getLong(3)));
        event.putString("endDate", formatDate(cursor.getLong(4)));
        event.putBoolean("allDay", cursor.getInt(5) == 1);
        event.putString("location", cursor.getString(6));
        event.putString("calendar", cursor.getString(7));
        
        int availability = cursor.getInt(8);
        String availabilityStr = "busy";
        if (availability == Events.AVAILABILITY_FREE) {
            availabilityStr = "free";
        } else if (availability == Events.AVAILABILITY_TENTATIVE) {
            availabilityStr = "tentative";
        }
        event.putString("availability", availabilityStr);
        
        String rrule = cursor.getString(9);
        if (!TextUtils.isEmpty(rrule)) {
            event.putMap("recurrence", parseRRule(rrule));
        }
        
        event.putString("url", cursor.getString(10));
        
        // Get alarms
        WritableArray alarms = getEventReminders(cursor.getString(0));
        if (alarms.size() > 0) {
            event.putArray("alarms", alarms);
        }
        
        return event;
    }

    private WritableArray getEventReminders(String eventId) {
        WritableArray alarms = Arguments.createArray();
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        
        Cursor cursor = cr.query(Reminders.CONTENT_URI,
            new String[] { Reminders.MINUTES },
            Reminders.EVENT_ID + " = ?",
            new String[] { eventId },
            null);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                WritableMap alarm = Arguments.createMap();
                alarm.putInt("minutes", cursor.getInt(0));
                alarms.pushMap(alarm);
            }
            cursor.close();
        }
        
        return alarms;
    }

    private void addReminder(String eventId, ReadableMap alarm) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        ContentValues values = new ContentValues();
        
        values.put(Reminders.EVENT_ID, Long.parseLong(eventId));
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        
        if (alarm.hasKey("minutes")) {
            values.put(Reminders.MINUTES, alarm.getInt("minutes"));
        } else {
            values.put(Reminders.MINUTES, 15); // Default 15 minutes
        }
        
        cr.insert(Reminders.CONTENT_URI, values);
    }

    private String buildRRule(ReadableMap recurrence) {
        StringBuilder rrule = new StringBuilder();
        
        if (recurrence.hasKey("frequency")) {
            String frequency = recurrence.getString("frequency");
            String freq = "DAILY";
            if ("weekly".equals(frequency)) {
                freq = "WEEKLY";
            } else if ("monthly".equals(frequency)) {
                freq = "MONTHLY";
            } else if ("yearly".equals(frequency)) {
                freq = "YEARLY";
            }
            rrule.append("FREQ=").append(freq);
        }
        
        if (recurrence.hasKey("interval")) {
            rrule.append(";INTERVAL=").append(recurrence.getInt("interval"));
        }
        
        if (recurrence.hasKey("endDate")) {
            long endMillis = parseDate(recurrence.getString("endDate"));
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            rrule.append(";UNTIL=").append(format.format(new Date(endMillis)));
        } else if (recurrence.hasKey("occurrence")) {
            rrule.append(";COUNT=").append(recurrence.getInt("occurrence"));
        }
        
        return rrule.toString();
    }

    private WritableMap parseRRule(String rrule) {
        WritableMap recurrence = Arguments.createMap();
        
        String[] parts = rrule.split(";");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                
                if ("FREQ".equals(key)) {
                    String frequency = "daily";
                    if ("WEEKLY".equals(value)) {
                        frequency = "weekly";
                    } else if ("MONTHLY".equals(value)) {
                        frequency = "monthly";
                    } else if ("YEARLY".equals(value)) {
                        frequency = "yearly";
                    }
                    recurrence.putString("frequency", frequency);
                } else if ("INTERVAL".equals(key)) {
                    recurrence.putInt("interval", Integer.parseInt(value));
                } else if ("COUNT".equals(key)) {
                    recurrence.putInt("occurrence", Integer.parseInt(value));
                }
            }
        }
        
        return recurrence;
    }

    private long parseDate(String dateString) {
        try {
            return ISO_8601_FORMAT.parse(dateString).getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    private String formatDate(long millis) {
        return ISO_8601_FORMAT.format(new Date(millis));
    }

    private long getDefaultCalendarId() {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        Cursor cursor = cr.query(Calendars.CONTENT_URI,
            new String[] { Calendars._ID },
            Calendars.IS_PRIMARY + " = 1",
            null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return id;
        }
        
        // If no primary calendar, get the first one
        cursor = cr.query(Calendars.CONTENT_URI,
            new String[] { Calendars._ID },
            null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return id;
        }
        
        return 1; // Default fallback
    }

    @ReactMethod
    public void saveEvent(String title, String startDate, String endDate, 
                         String location, String notes, String calendarId, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        
        ContentValues values = new ContentValues();
        values.put(Events.TITLE, title);
        values.put(Events.DESCRIPTION, notes);
        values.put(Events.EVENT_LOCATION, location);
        
        try {
            long startMillis = ISO_8601_FORMAT.parse(startDate).getTime();
            long endMillis = ISO_8601_FORMAT.parse(endDate).getTime();
            
            values.put(Events.DTSTART, startMillis);
            values.put(Events.DTEND, endMillis);
            values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            
            // Use provided calendar or default
            long calId = TextUtils.isEmpty(calendarId) ? getDefaultCalendarId() : Long.parseLong(calendarId);
            values.put(Events.CALENDAR_ID, calId);
            
            Uri uri = cr.insert(Events.CONTENT_URI, values);
            if (uri != null) {
                String eventId = uri.getLastPathSegment();
                promise.resolve(eventId);
            } else {
                promise.reject("event_save_failed", "Failed to save event");
            }
        } catch (ParseException e) {
            promise.reject("date_parse_error", "Invalid date format", e);
        } catch (Exception e) {
            promise.reject("event_save_failed", "Failed to save event", e);
        }
    }

    @ReactMethod
    public void updateEvent(String eventId, String title, String startDate, String endDate,
                           String location, String notes, String calendarId, Promise promise) {
        ContentResolver cr = getReactApplicationContext().getContentResolver();
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, Long.parseLong(eventId));
        
        ContentValues values = new ContentValues();
        if (!TextUtils.isEmpty(title)) values.put(Events.TITLE, title);
        if (!TextUtils.isEmpty(notes)) values.put(Events.DESCRIPTION, notes);
        if (!TextUtils.isEmpty(location)) values.put(Events.EVENT_LOCATION, location);
        
        try {
            if (!TextUtils.isEmpty(startDate)) {
                long startMillis = ISO_8601_FORMAT.parse(startDate).getTime();
                values.put(Events.DTSTART, startMillis);
            }
            if (!TextUtils.isEmpty(endDate)) {
                long endMillis = ISO_8601_FORMAT.parse(endDate).getTime();
                values.put(Events.DTEND, endMillis);
            }
            if (!TextUtils.isEmpty(calendarId)) {
                values.put(Events.CALENDAR_ID, Long.parseLong(calendarId));
            }
            
            int rowsUpdated = cr.update(uri, values, null, null);
            if (rowsUpdated > 0) {
                promise.resolve(eventId);
            } else {
                promise.reject("event_update_failed", "No rows updated");
            }
        } catch (Exception e) {
            promise.reject("event_update_failed", "Failed to update event", e);
        }
    }
}