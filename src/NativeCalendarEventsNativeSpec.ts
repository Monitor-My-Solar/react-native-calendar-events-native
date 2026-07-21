import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  debugModuleMethods(): Promise<string>;
  requestPermissions(writeOnly: boolean): Promise<string>;
  checkPermissions(writeOnly: boolean): Promise<string>;
  fetchAllCalendars(): Promise<Array<{
    id: string;
    title: string;
    type: string;
    source: string;
    isPrimary?: boolean;
    allowsModifications?: boolean;
    color?: string;
    allowedAvailabilities?: Array<string>;
  }>>;
  findOrCreateCalendar(
    title: string,
    color?: string,
    entityType?: string,
    source?: string
  ): Promise<{
    id: string;
    title: string;
    type: string;
    source: string;
    isPrimary?: boolean;
    allowsModifications?: boolean;
    color?: string;
    allowedAvailabilities?: Array<string>;
  }>;
  removeCalendar(calendarId: string): Promise<boolean>;
  fetchAllEvents(
    startDate: string,
    endDate: string,
    calendarIds: Array<string>
  ): Promise<Array<{
    id?: string;
    title: string;
    startDate: string;
    endDate: string;
    location?: string;
    notes?: string;
    url?: string;
    alarms?: Array<{
      date?: string;
      structuredLocation?: {
        title?: string;
        proximity?: string;
        radius?: number;
        coords?: {
          latitude: number;
          longitude: number;
        };
      };
      minutes?: number;
    }>;
    recurrence?: {
      frequency: string;
      interval?: number;
      endDate?: string;
      occurrence?: number;
      daysOfWeek?: Array<{
        dayOfWeek: number;
        weekNumber?: number;
      }>;
      daysOfMonth?: Array<number>;
      monthsOfYear?: Array<number>;
      daysOfYear?: Array<number>;
    };
    availability?: string;
    allDay?: boolean;
    calendar?: string;
  }>>;
  findEventById(eventId: string): Promise<{
    id?: string;
    title: string;
    startDate: string;
    endDate: string;
    location?: string;
    notes?: string;
    url?: string;
    alarms?: Array<{
      date?: string;
      structuredLocation?: {
        title?: string;
        proximity?: string;
        radius?: number;
        coords?: {
          latitude: number;
          longitude: number;
        };
      };
      minutes?: number;
    }>;
    recurrence?: {
      frequency: string;
      interval?: number;
      endDate?: string;
      occurrence?: number;
      daysOfWeek?: Array<{
        dayOfWeek: number;
        weekNumber?: number;
      }>;
      daysOfMonth?: Array<number>;
      monthsOfYear?: Array<number>;
      daysOfYear?: Array<number>;
    };
    availability?: string;
    allDay?: boolean;
    calendar?: string;
  } | null>;
  // `details` is a full event object (title, startDate, endDate, location, notes,
  // url, allDay, calendar, availability, alarms, recurrence, timeZone). Typed as
  // `Object` so codegen maps it to NSDictionary/ReadableMap (a structured type
  // would emit a C++ struct that conflicts with the native NSDictionary impl).
  saveEvent(details: Object): Promise<string>;
  updateEvent(eventId: string, details: Object): Promise<string>;
  removeEvent(eventId: string): Promise<boolean>;
  openEventInCalendar?(eventId: string): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RNCalendarEventsNativeSpec');
