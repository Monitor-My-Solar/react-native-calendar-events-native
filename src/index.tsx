import { PermissionsAndroid, Platform } from 'react-native';
import CalendarEventsNative from './NativeCalendarEventsNativeSpec';

export interface CalendarEvent {
  id?: string;
  title: string;
  startDate: string | Date;
  endDate: string | Date;
  location?: string;
  notes?: string;
  url?: string;
  alarms?: CalendarAlarm[];
  recurrence?: RecurrenceRule;
  availability?: 'busy' | 'free' | 'tentative' | 'unavailable';
  allDay?: boolean;
  calendar?: string;
}

export interface CalendarAlarm {
  date?: string | Date;
  structuredLocation?: {
    title?: string;
    proximity?: 'enter' | 'leave' | 'none';
    radius?: number;
    coords?: {
      latitude: number;
      longitude: number;
    };
  };
  minutes?: number;
}

export interface RecurrenceRule {
  frequency: 'daily' | 'weekly' | 'monthly' | 'yearly';
  interval?: number;
  endDate?: string | Date;
  occurrence?: number;
  daysOfWeek?: Array<{
    dayOfWeek: number;
    weekNumber?: number;
  }>;
  daysOfMonth?: number[];
  monthsOfYear?: number[];
  daysOfYear?: number[];
}

export interface Calendar {
  id: string;
  title: string;
  type: string;
  source: string;
  isPrimary?: boolean;
  allowsModifications?: boolean;
  color?: string;
  allowedAvailabilities?: string[];
}

export type AuthorizationStatus = 
  | 'authorized'
  | 'denied'
  | 'restricted'
  | 'undetermined';

export type PermissionStatus = 
  | 'granted'
  | 'denied'
  | 'never_ask_again'
  | 'unavailable';

export type AllPermissionStatus = AuthorizationStatus | PermissionStatus;

class CalendarEvents {
  constructor() {
    // Log what's actually available to JavaScript
    console.log('🚀 CalendarEvents: JavaScript wrapper initialized!');
    console.log('🔍 CalendarEventsNative object:', CalendarEventsNative);
    console.log('📋 Actually available methods:');
    
    const methods = ['requestPermissions', 'checkPermissions', 'fetchAllCalendars', 'findOrCreateCalendar', 'removeCalendar', 'fetchAllEvents', 'findEventById', 'saveEvent', 'updateEvent', 'removeEvent', 'openEventInCalendar', 'debugModuleMethods'];
    
    methods.forEach(method => {
      const isAvailable = typeof (CalendarEventsNative as any)[method] === 'function';
      console.log(`   ${isAvailable ? '✅' : '❌'} ${method}: ${typeof (CalendarEventsNative as any)[method]}`);
    });
    
    console.log('🔍 All CalendarEventsNative properties:', Object.getOwnPropertyNames(CalendarEventsNative));
  }

  /**
   * Debug method to check available methods
   */
  async debugModuleMethods(): Promise<string> {
    return CalendarEventsNative.debugModuleMethods();
  }

  /**
   * Request calendar permissions
   */
  async requestPermissions(writeOnly: boolean = false): Promise<AllPermissionStatus> {
    if (Platform.OS === 'ios') {
      return CalendarEventsNative.requestPermissions(writeOnly) as Promise<AuthorizationStatus>;
    } else if (Platform.OS === 'android') {
      const permissions = writeOnly 
        ? [PermissionsAndroid.PERMISSIONS.WRITE_CALENDAR]
        : [
            PermissionsAndroid.PERMISSIONS.READ_CALENDAR,
            PermissionsAndroid.PERMISSIONS.WRITE_CALENDAR
          ];
      
      const validPermissions = permissions.filter(p => p !== undefined);
      const results = await PermissionsAndroid.requestMultiple(validPermissions as any);
      
      if (validPermissions.every(p => p && (results as any)[p as string] === PermissionsAndroid.RESULTS.GRANTED)) {
        return 'granted';
      } else if (validPermissions.some(p => p && (results as any)[p as string] === PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN)) {
        return 'never_ask_again';
      } else {
        return 'denied';
      }
    }
    return 'unavailable';
  }

  /**
   * Check current calendar permissions
   */
  async checkPermissions(writeOnly: boolean = false): Promise<AllPermissionStatus> {
    if (Platform.OS === 'ios') {
      return CalendarEventsNative.checkPermissions(writeOnly) as Promise<AuthorizationStatus>;
    } else if (Platform.OS === 'android') {
      const permissions = writeOnly 
        ? [PermissionsAndroid.PERMISSIONS.WRITE_CALENDAR]
        : [
            PermissionsAndroid.PERMISSIONS.READ_CALENDAR,
            PermissionsAndroid.PERMISSIONS.WRITE_CALENDAR
          ];
      
      const validPermissions = permissions.filter(p => p !== undefined);
      const results = await Promise.all(
        validPermissions.map(p => PermissionsAndroid.check(p as any))
      );
      
      if (results.every(r => r === true)) {
        return 'granted';
      } else {
        return 'denied';
      }
    }
    return 'unavailable';
  }

  /**
   * Fetch all calendars
   */
  async fetchAllCalendars(): Promise<Calendar[]> {
    return CalendarEventsNative.fetchAllCalendars();
  }

  /**
   * Find or create a calendar
   */
  async findOrCreateCalendar(calendar: Partial<Calendar>): Promise<Calendar> {
    return CalendarEventsNative.findOrCreateCalendar(
      calendar.title || 'Calendar',
      calendar.color,
      undefined, // entityType
      calendar.source
    );
  }

  /**
   * Remove a calendar
   */
  async removeCalendar(calendarId: string): Promise<boolean> {
    return CalendarEventsNative.removeCalendar(calendarId);
  }

  /**
   * Fetch all events
   */
  async fetchAllEvents(
    startDate: string | Date,
    endDate: string | Date,
    calendarIds?: string[]
  ): Promise<CalendarEvent[]> {
    const start = typeof startDate === 'string' ? startDate : startDate.toISOString();
    const end = typeof endDate === 'string' ? endDate : endDate.toISOString();
    const events = await CalendarEventsNative.fetchAllEvents(start, end, calendarIds || []);
    return events as CalendarEvent[];
  }

  /**
   * Find event by ID
   */
  async findEventById(id: string): Promise<CalendarEvent | null> {
    const event = await CalendarEventsNative.findEventById(id);
    return event as CalendarEvent | null;
  }

  /**
   * Convert a (partial) event into the plain object the native module expects.
   * Date instances become ISO-8601 strings, and only the keys that are present
   * are forwarded so that updateEvent() behaves as a partial update.
   */
  private toNativeDetails(event: Partial<CalendarEvent>): Object {
    const details: { [key: string]: any } = { ...event };

    if (event.startDate !== undefined) {
      details.startDate =
        typeof event.startDate === 'string'
          ? event.startDate
          : event.startDate.toISOString();
    }
    if (event.endDate !== undefined) {
      details.endDate =
        typeof event.endDate === 'string'
          ? event.endDate
          : event.endDate.toISOString();
    }
    if (event.alarms) {
      details.alarms = event.alarms.map((alarm) => ({
        ...alarm,
        date:
          alarm.date instanceof Date ? alarm.date.toISOString() : alarm.date,
      }));
    }
    if (event.recurrence?.endDate) {
      details.recurrence = {
        ...event.recurrence,
        endDate:
          event.recurrence.endDate instanceof Date
            ? event.recurrence.endDate.toISOString()
            : event.recurrence.endDate,
      };
    }

    return details;
  }

  /**
   * Save an event. Honors title, start/end dates, location, notes, url, allDay,
   * calendar, availability, alarms, recurrence and timeZone.
   */
  async saveEvent(event: CalendarEvent): Promise<string> {
    return CalendarEventsNative.saveEvent(this.toNativeDetails(event));
  }

  /**
   * Update an existing event. Only the fields provided are changed.
   */
  async updateEvent(
    eventId: string,
    event: Partial<CalendarEvent>
  ): Promise<string> {
    return CalendarEventsNative.updateEvent(eventId, this.toNativeDetails(event));
  }

  /**
   * Remove an event
   */
  async removeEvent(eventId: string): Promise<boolean> {
    return CalendarEventsNative.removeEvent(eventId);
  }

  /**
   * Open event in calendar app
   */
  async openEventInCalendar(eventId: string): Promise<void> {
    if (Platform.OS === 'ios' && CalendarEventsNative.openEventInCalendar) {
      return CalendarEventsNative.openEventInCalendar(eventId);
    } else {
      throw new Error('Opening events in calendar app is only supported on iOS');
    }
  }
}

export default new CalendarEvents();