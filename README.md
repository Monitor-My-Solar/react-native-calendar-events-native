# react-native-calendar-events-native

A React Native module for native calendar event creation on iOS and Android, fully compatible with React Native's New Architecture (Fabric/TurboModules).

## Features

- ✅ **New Architecture Compatible** - Built with TurboModules for optimal performance
- ✅ **TypeScript Support** - Full TypeScript definitions included
- ✅ **iOS & Android** - Native implementations for both platforms
- ✅ **Permission Management** - Built-in permission handling
- ✅ **Calendar Management** - Create, find, and remove calendars
- ✅ **Event Management** - Full CRUD operations for calendar events
- ✅ **Recurring Events** - Support for complex recurrence rules
- ✅ **Reminders/Alarms** - Add multiple alarms to events
- ✅ **Event Availability** - Set busy/free/tentative status

## Installation

```sh
npm install react-native-calendar-events-native
# or
yarn add react-native-calendar-events-native
```

### iOS Setup

1. Add required permissions to `Info.plist`:

```xml
<key>NSCalendarsUsageDescription</key>
<string>This app needs access to your calendar to create and manage events</string>
<key>NSCalendarsFullAccessUsageDescription</key>
<string>This app needs full access to your calendar to create and manage events</string>
```

2. Install pods:

```sh
cd ios && pod install
```

### Android Setup

The required permissions are automatically added by the module. No additional setup needed.

## Usage

```typescript
import CalendarEvents from 'react-native-calendar-events-native';

// Request permissions
const status = await CalendarEvents.requestPermissions();
if (status === 'authorized' || status === 'granted') {
  // Permission granted
}

// Create or find a custom calendar
const calendar = await CalendarEvents.findOrCreateCalendar({
  title: 'My App Events',
  color: '#2196F3',
});

// Create an event
const eventId = await CalendarEvents.saveEvent({
  title: 'Meeting',
  startDate: new Date('2024-01-20 10:00'),
  endDate: new Date('2024-01-20 11:00'),
  location: 'Office',
  notes: 'Important meeting',
  alarms: [{ minutes: 15 }], // Reminder 15 minutes before
});

// Fetch events
const events = await CalendarEvents.fetchAllEvents(
  new Date('2024-01-01'),
  new Date('2024-12-31')
);

// Update an event
await CalendarEvents.updateEvent(eventId, {
  title: 'Updated Meeting',
  location: 'Conference Room',
});

// Delete an event
await CalendarEvents.removeEvent(eventId);
```

## API Reference

### Permission Methods

#### `requestPermissions(writeOnly?: boolean): Promise<AuthorizationStatus>`

Request calendar permissions from the user.

#### `checkPermissions(writeOnly?: boolean): Promise<AuthorizationStatus>`

Check current calendar permission status.

### Calendar Methods

#### `fetchAllCalendars(): Promise<Calendar[]>`

Fetch all available calendars on the device.

#### `findOrCreateCalendar(calendar: Partial<Calendar>): Promise<Calendar>`

Find an existing calendar by title or create a new one if it doesn't exist.

```typescript
const calendar = await CalendarEvents.findOrCreateCalendar({
  title: 'My Custom Calendar',
  color: '#FF5722', // Optional: hex color code
});
```

#### `removeCalendar(calendarId: string): Promise<boolean>`

Remove a calendar by its ID. Returns `true` if successful.

```typescript
const success = await CalendarEvents.removeCalendar(calendarId);
```

### Event Methods

#### `fetchAllEvents(startDate: Date | string, endDate: Date | string, calendarIds?: string[]): Promise<CalendarEvent[]>`

Fetch all events within a date range.

#### `findEventById(eventId: string): Promise<CalendarEvent | null>`

Find a specific event by ID.

#### `saveEvent(event: CalendarEvent): Promise<string>`

Create a new calendar event.

#### `updateEvent(eventId: string, event: Partial<CalendarEvent>): Promise<string>`

Update an existing event.

#### `removeEvent(eventId: string): Promise<boolean>`

Remove an event from the calendar.

#### `openEventInCalendar(eventId: string): Promise<void>` (iOS only)

Open an event in the native calendar app.

## Types

### CalendarEvent

```typescript
interface CalendarEvent {
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
```

### CalendarAlarm

```typescript
interface CalendarAlarm {
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
```

### RecurrenceRule

```typescript
interface RecurrenceRule {
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
```

### Calendar

```typescript
interface Calendar {
  id: string;
  title: string;
  type: string;
  source: string;
  isPrimary?: boolean;
  allowsModifications?: boolean;
  color?: string;
  allowedAvailabilities?: string[];
}
```

## Examples

### Creating a Recurring Event

```typescript
const eventId = await CalendarEvents.saveEvent({
  title: 'Weekly Team Meeting',
  startDate: new Date('2024-01-20 10:00'),
  endDate: new Date('2024-01-20 11:00'),
  recurrence: {
    frequency: 'weekly',
    interval: 1,
    endDate: new Date('2024-12-31'),
  },
  alarms: [
    { minutes: 15 },
    { minutes: 60 },
  ],
});
```

### Creating an All-Day Event

```typescript
const eventId = await CalendarEvents.saveEvent({
  title: 'Birthday',
  startDate: new Date('2024-03-15'),
  endDate: new Date('2024-03-16'),
  allDay: true,
});
```

## Platform Differences

- **iOS**: Supports opening events in the native calendar app via `openEventInCalendar()`
- **Android**: Calendar creation requires specifying account details
- **iOS 17+**: Uses new full access calendar permissions

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with ❤️ for the React Native community