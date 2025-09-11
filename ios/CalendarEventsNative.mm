#import "CalendarEventsNative.h"
#import <EventKit/EventKit.h>
#import <EventKitUI/EventKitUI.h>
#import <React/RCTConvert.h>

#ifdef RCT_NEW_ARCH_ENABLED
// JSI headers are included automatically by the framework
#endif

@interface CalendarEventsNative () <EKEventEditViewDelegate>
@property (nonatomic, strong) EKEventStore *eventStore;
@property (nonatomic, copy) RCTPromiseResolveBlock editEventResolver;
@property (nonatomic, copy) RCTPromiseRejectBlock editEventRejecter;
@end


@implementation CalendarEventsNative

RCT_EXPORT_MODULE()

- (instancetype)init {
    if (self = [super init]) {
        self.eventStore = [[EKEventStore alloc] init];
        
        // Log available methods on module initialization
        NSLog(@"🚀 CalendarEventsNative: Module initialized successfully!");
        NSLog(@"📋 Exported methods:");
        NSLog(@"   • debugModuleMethods");
        NSLog(@"   • requestPermissions");
        NSLog(@"   • checkPermissions");
        NSLog(@"   • fetchAllCalendars");
        NSLog(@"   • findOrCreateCalendar");
        NSLog(@"   • removeCalendar");
        NSLog(@"   • fetchAllEvents");
        NSLog(@"   • findEventById");
        NSLog(@"   • saveEvent");
        NSLog(@"   • updateEvent");
        NSLog(@"   • removeEvent");
        NSLog(@"   • openEventInCalendar");
        NSLog(@"✅ All methods exported with RCT_EXPORT_METHOD");
    }
    return self;
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

RCT_EXPORT_METHOD(debugModuleMethods:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    NSLog(@"🔍 CalendarEventsNative: Module methods available!");
    NSLog(@"🔍 Available methods: requestPermissions, checkPermissions, fetchAllCalendars, findOrCreateCalendar, removeCalendar, fetchAllEvents, findEventById, saveEvent, updateEvent, removeEvent, openEventInCalendar");
    resolve(@"Methods logged to console");
}

#pragma mark - Permission Methods

RCT_EXPORT_METHOD(requestPermissions:(BOOL)writeOnly
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    if (@available(iOS 17.0, *)) {
        [self.eventStore requestFullAccessToEventsWithCompletion:^(BOOL granted, NSError *error) {
            if (error) {
                reject(@"permission_error", error.localizedDescription, error);
            } else {
                resolve(granted ? @"authorized" : @"denied");
            }
        }];
    } else {
        [self.eventStore requestAccessToEntityType:EKEntityTypeEvent completion:^(BOOL granted, NSError *error) {
            if (error) {
                reject(@"permission_error", error.localizedDescription, error);
            } else {
                resolve(granted ? @"authorized" : @"denied");
            }
        }];
    }
}

RCT_EXPORT_METHOD(checkPermissions:(BOOL)writeOnly
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    EKAuthorizationStatus status = [EKEventStore authorizationStatusForEntityType:EKEntityTypeEvent];
    
    NSString *statusString;
    switch (status) {
        case EKAuthorizationStatusAuthorized:
            statusString = @"authorized";
            break;
        case EKAuthorizationStatusDenied:
            statusString = @"denied";
            break;
        case EKAuthorizationStatusRestricted:
            statusString = @"restricted";
            break;
        case EKAuthorizationStatusNotDetermined:
            statusString = @"undetermined";
            break;
        default:
            statusString = @"undetermined";
            break;
    }
    
    resolve(statusString);
}

#pragma mark - Calendar Methods

RCT_EXPORT_METHOD(fetchAllCalendars:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    NSArray<EKCalendar *> *calendars = [self.eventStore calendarsForEntityType:EKEntityTypeEvent];
    NSMutableArray *calendarData = [NSMutableArray array];
    
    for (EKCalendar *calendar in calendars) {
        NSMutableDictionary *calDict = [NSMutableDictionary dictionary];
        calDict[@"id"] = calendar.calendarIdentifier;
        calDict[@"title"] = calendar.title;
        calDict[@"type"] = @(calendar.type);
        calDict[@"source"] = calendar.source.title ?: @"";
        calDict[@"isPrimary"] = @(calendar.type == EKCalendarTypeLocal);
        calDict[@"allowsModifications"] = @(calendar.allowsContentModifications);
        calDict[@"color"] = [self hexStringFromColor:calendar.CGColor];
        
        NSMutableArray *availabilities = [NSMutableArray array];
        if (calendar.supportedEventAvailabilities & EKCalendarEventAvailabilityBusy) {
            [availabilities addObject:@"busy"];
        }
        if (calendar.supportedEventAvailabilities & EKCalendarEventAvailabilityFree) {
            [availabilities addObject:@"free"];
        }
        if (calendar.supportedEventAvailabilities & EKCalendarEventAvailabilityTentative) {
            [availabilities addObject:@"tentative"];
        }
        if (calendar.supportedEventAvailabilities & EKCalendarEventAvailabilityUnavailable) {
            [availabilities addObject:@"unavailable"];
        }
        calDict[@"allowedAvailabilities"] = availabilities;
        
        [calendarData addObject:calDict];
    }
    
    resolve(calendarData);
}

RCT_EXPORT_METHOD(findOrCreateCalendar:(NSDictionary *)calendarDict
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    NSString *title = calendarDict[@"title"] ?: @"Calendar";
    
    // First, try to find existing calendar
    NSArray<EKCalendar *> *calendars = [self.eventStore calendarsForEntityType:EKEntityTypeEvent];
    for (EKCalendar *calendar in calendars) {
        if ([calendar.title isEqualToString:title]) {
            resolve(calendar.calendarIdentifier);
            return;
        }
    }
    
    // Create new calendar
    EKCalendar *calendar = [EKCalendar calendarForEntityType:EKEntityTypeEvent eventStore:self.eventStore];
    calendar.title = title;
    
    // Find the default source
    EKSource *localSource = nil;
    EKSource *iCloudSource = nil;
    
    for (EKSource *source in self.eventStore.sources) {
        if (source.sourceType == EKSourceTypeLocal) {
            localSource = source;
        } else if (source.sourceType == EKSourceTypeCalDAV && 
                   [source.title containsString:@"iCloud"]) {
            iCloudSource = source;
        }
    }
    
    calendar.source = iCloudSource ?: localSource ?: self.eventStore.defaultCalendarForNewEvents.source;
    
    // Set color if provided
    NSString *colorHex = calendarDict[@"color"];
    if (colorHex) {
        calendar.CGColor = [self colorFromHexString:colorHex];
    }
    
    NSError *error;
    BOOL success = [self.eventStore saveCalendar:calendar commit:YES error:&error];
    
    if (success) {
        resolve(calendar.calendarIdentifier);
    } else {
        reject(@"calendar_creation_failed", error.localizedDescription, error);
    }
}

RCT_EXPORT_METHOD(removeCalendar:(NSString *)calendarId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    EKCalendar *calendar = [self.eventStore calendarWithIdentifier:calendarId];
    
    if (!calendar) {
        resolve(@NO);
        return;
    }
    
    NSError *error;
    BOOL success = [self.eventStore removeCalendar:calendar commit:YES error:&error];
    
    if (success) {
        resolve(@YES);
    } else {
        reject(@"calendar_removal_failed", error.localizedDescription, error);
    }
}

#pragma mark - Event Methods

RCT_EXPORT_METHOD(fetchAllEvents:(NSString *)startDate
                  endDate:(NSString *)endDate
                  calendarIds:(NSArray<NSString *> *)calendarIds
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    NSDate *start = [self dateFromISO8601String:startDate];
    NSDate *end = [self dateFromISO8601String:endDate];
    
    NSPredicate *predicate;
    if (calendarIds.count > 0) {
        NSMutableArray<EKCalendar *> *calendars = [NSMutableArray array];
        for (NSString *calendarId in calendarIds) {
            EKCalendar *calendar = [self.eventStore calendarWithIdentifier:calendarId];
            if (calendar) {
                [calendars addObject:calendar];
            }
        }
        predicate = [self.eventStore predicateForEventsWithStartDate:start endDate:end calendars:calendars];
    } else {
        predicate = [self.eventStore predicateForEventsWithStartDate:start endDate:end calendars:nil];
    }
    
    NSArray<EKEvent *> *events = [self.eventStore eventsMatchingPredicate:predicate];
    NSMutableArray *eventData = [NSMutableArray array];
    
    for (EKEvent *event in events) {
        [eventData addObject:[self serializeEvent:event]];
    }
    
    resolve(eventData);
}

RCT_EXPORT_METHOD(findEventById:(NSString *)eventId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    EKEvent *event = [self.eventStore eventWithIdentifier:eventId];
    
    if (event) {
        resolve([self serializeEvent:event]);
    } else {
        resolve([NSNull null]);
    }
}

RCT_EXPORT_METHOD(saveEvent:(NSDictionary *)eventDict
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    EKEvent *event = [EKEvent eventWithEventStore:self.eventStore];
    
    [self applyEventProperties:eventDict toEvent:event];
    
    NSError *error;
    BOOL success = [self.eventStore saveEvent:event span:EKSpanThisEvent commit:YES error:&error];
    
    if (success) {
        resolve(event.eventIdentifier);
    } else {
        reject(@"event_save_failed", error.localizedDescription, error);
    }
}

RCT_EXPORT_METHOD(updateEvent:(NSString *)eventId
                  event:(NSDictionary *)eventDict
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    EKEvent *event = [self.eventStore eventWithIdentifier:eventId];
    
    if (!event) {
        reject(@"event_not_found", @"Event not found", nil);
        return;
    }
    
    [self applyEventProperties:eventDict toEvent:event];
    
    NSError *error;
    BOOL success = [self.eventStore saveEvent:event span:EKSpanThisEvent commit:YES error:&error];
    
    if (success) {
        resolve(event.eventIdentifier);
    } else {
        reject(@"event_update_failed", error.localizedDescription, error);
    }
}

RCT_EXPORT_METHOD(removeEvent:(NSString *)eventId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    EKEvent *event = [self.eventStore eventWithIdentifier:eventId];
    
    if (!event) {
        resolve(@NO);
        return;
    }
    
    NSError *error;
    BOOL success = [self.eventStore removeEvent:event span:EKSpanThisEvent commit:YES error:&error];
    
    if (success) {
        resolve(@YES);
    } else {
        reject(@"event_removal_failed", error.localizedDescription, error);
    }
}

RCT_EXPORT_METHOD(openEventInCalendar:(NSString *)eventId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        EKEvent *event = [self.eventStore eventWithIdentifier:eventId];
        
        if (!event) {
            reject(@"event_not_found", @"Event not found", nil);
            return;
        }
        
        EKEventEditViewController *controller = [[EKEventEditViewController alloc] init];
        controller.event = event;
        controller.eventStore = self.eventStore;
        controller.editViewDelegate = self;
        
        self.editEventResolver = resolve;
        self.editEventRejecter = reject;
        
        UIViewController *rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
        [rootViewController presentViewController:controller animated:YES completion:nil];
    });
}

#pragma mark - Helper Methods

- (void)applyEventProperties:(NSDictionary *)eventDict toEvent:(EKEvent *)event {
    event.title = eventDict[@"title"];
    event.startDate = [self dateFromISO8601String:eventDict[@"startDate"]];
    event.endDate = [self dateFromISO8601String:eventDict[@"endDate"]];
    event.location = eventDict[@"location"];
    event.notes = eventDict[@"notes"];
    event.URL = eventDict[@"url"] ? [NSURL URLWithString:eventDict[@"url"]] : nil;
    event.allDay = [eventDict[@"allDay"] boolValue];
    
    // Set calendar
    if (eventDict[@"calendar"]) {
        EKCalendar *calendar = [self.eventStore calendarWithIdentifier:eventDict[@"calendar"]];
        if (calendar) {
            event.calendar = calendar;
        }
    } else {
        event.calendar = self.eventStore.defaultCalendarForNewEvents;
    }
    
    // Set availability
    NSString *availability = eventDict[@"availability"];
    if ([availability isEqualToString:@"busy"]) {
        event.availability = EKEventAvailabilityBusy;
    } else if ([availability isEqualToString:@"free"]) {
        event.availability = EKEventAvailabilityFree;
    } else if ([availability isEqualToString:@"tentative"]) {
        event.availability = EKEventAvailabilityTentative;
    } else if ([availability isEqualToString:@"unavailable"]) {
        event.availability = EKEventAvailabilityUnavailable;
    }
    
    // Set alarms
    NSArray *alarms = eventDict[@"alarms"];
    if (alarms && alarms.count > 0) {
        NSMutableArray<EKAlarm *> *ekAlarms = [NSMutableArray array];
        for (NSDictionary *alarmDict in alarms) {
            EKAlarm *alarm;
            if (alarmDict[@"date"]) {
                NSDate *alarmDate = [self dateFromISO8601String:alarmDict[@"date"]];
                alarm = [EKAlarm alarmWithAbsoluteDate:alarmDate];
            } else if (alarmDict[@"minutes"]) {
                NSTimeInterval offset = -[alarmDict[@"minutes"] doubleValue] * 60;
                alarm = [EKAlarm alarmWithRelativeOffset:offset];
            }
            if (alarm) {
                [ekAlarms addObject:alarm];
            }
        }
        event.alarms = ekAlarms;
    }
    
    // Set recurrence
    NSDictionary *recurrence = eventDict[@"recurrence"];
    if (recurrence) {
        EKRecurrenceFrequency frequency;
        NSString *freq = recurrence[@"frequency"];
        if ([freq isEqualToString:@"daily"]) {
            frequency = EKRecurrenceFrequencyDaily;
        } else if ([freq isEqualToString:@"weekly"]) {
            frequency = EKRecurrenceFrequencyWeekly;
        } else if ([freq isEqualToString:@"monthly"]) {
            frequency = EKRecurrenceFrequencyMonthly;
        } else if ([freq isEqualToString:@"yearly"]) {
            frequency = EKRecurrenceFrequencyYearly;
        } else {
            frequency = EKRecurrenceFrequencyDaily;
        }
        
        NSInteger interval = [recurrence[@"interval"] integerValue] ?: 1;
        
        EKRecurrenceEnd *recurrenceEnd = nil;
        if (recurrence[@"endDate"]) {
            NSDate *endDate = [self dateFromISO8601String:recurrence[@"endDate"]];
            recurrenceEnd = [EKRecurrenceEnd recurrenceEndWithEndDate:endDate];
        } else if (recurrence[@"occurrence"]) {
            NSInteger occurrenceCount = [recurrence[@"occurrence"] integerValue];
            recurrenceEnd = [EKRecurrenceEnd recurrenceEndWithOccurrenceCount:occurrenceCount];
        }
        
        EKRecurrenceRule *rule = [[EKRecurrenceRule alloc] initRecurrenceWithFrequency:frequency
                                                                               interval:interval
                                                                                    end:recurrenceEnd];
        event.recurrenceRules = @[rule];
    }
}

- (NSDictionary *)serializeEvent:(EKEvent *)event {
    NSMutableDictionary *eventDict = [NSMutableDictionary dictionary];
    
    eventDict[@"id"] = event.eventIdentifier ?: @"";
    eventDict[@"title"] = event.title ?: @"";
    eventDict[@"startDate"] = [self ISO8601StringFromDate:event.startDate];
    eventDict[@"endDate"] = [self ISO8601StringFromDate:event.endDate];
    eventDict[@"location"] = event.location ?: @"";
    eventDict[@"notes"] = event.notes ?: @"";
    eventDict[@"url"] = event.URL.absoluteString ?: @"";
    eventDict[@"allDay"] = @(event.allDay);
    eventDict[@"calendar"] = event.calendar.calendarIdentifier ?: @"";
    
    // Serialize availability
    switch (event.availability) {
        case EKEventAvailabilityBusy:
            eventDict[@"availability"] = @"busy";
            break;
        case EKEventAvailabilityFree:
            eventDict[@"availability"] = @"free";
            break;
        case EKEventAvailabilityTentative:
            eventDict[@"availability"] = @"tentative";
            break;
        case EKEventAvailabilityUnavailable:
            eventDict[@"availability"] = @"unavailable";
            break;
        default:
            eventDict[@"availability"] = @"busy";
            break;
    }
    
    // Serialize alarms
    if (event.alarms && event.alarms.count > 0) {
        NSMutableArray *alarms = [NSMutableArray array];
        for (EKAlarm *alarm in event.alarms) {
            NSMutableDictionary *alarmDict = [NSMutableDictionary dictionary];
            if (alarm.absoluteDate) {
                alarmDict[@"date"] = [self ISO8601StringFromDate:alarm.absoluteDate];
            } else {
                alarmDict[@"minutes"] = @(-alarm.relativeOffset / 60);
            }
            [alarms addObject:alarmDict];
        }
        eventDict[@"alarms"] = alarms;
    }
    
    // Serialize recurrence
    if (event.recurrenceRules && event.recurrenceRules.count > 0) {
        EKRecurrenceRule *rule = event.recurrenceRules.firstObject;
        NSMutableDictionary *recurrence = [NSMutableDictionary dictionary];
        
        switch (rule.frequency) {
            case EKRecurrenceFrequencyDaily:
                recurrence[@"frequency"] = @"daily";
                break;
            case EKRecurrenceFrequencyWeekly:
                recurrence[@"frequency"] = @"weekly";
                break;
            case EKRecurrenceFrequencyMonthly:
                recurrence[@"frequency"] = @"monthly";
                break;
            case EKRecurrenceFrequencyYearly:
                recurrence[@"frequency"] = @"yearly";
                break;
        }
        
        recurrence[@"interval"] = @(rule.interval);
        
        if (rule.recurrenceEnd) {
            if (rule.recurrenceEnd.endDate) {
                recurrence[@"endDate"] = [self ISO8601StringFromDate:rule.recurrenceEnd.endDate];
            } else if (rule.recurrenceEnd.occurrenceCount > 0) {
                recurrence[@"occurrence"] = @(rule.recurrenceEnd.occurrenceCount);
            }
        }
        
        eventDict[@"recurrence"] = recurrence;
    }
    
    return eventDict;
}

#pragma mark - EKEventEditViewDelegate

- (void)eventEditViewController:(EKEventEditViewController *)controller
          didCompleteWithAction:(EKEventEditViewAction)action {
    [controller dismissViewControllerAnimated:YES completion:^{
        if (self.editEventResolver) {
            self.editEventResolver(nil);
            self.editEventResolver = nil;
            self.editEventRejecter = nil;
        }
    }];
}

#pragma mark - Date Helpers

- (NSDate *)dateFromISO8601String:(NSString *)dateString {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    formatter.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
    formatter.timeZone = [NSTimeZone localTimeZone];
    return [formatter dateFromString:dateString];
}

- (NSString *)ISO8601StringFromDate:(NSDate *)date {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    formatter.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
    formatter.timeZone = [NSTimeZone localTimeZone];
    return [formatter stringFromDate:date];
}

#pragma mark - Color Helpers

- (NSString *)hexStringFromColor:(CGColorRef)color {
    const CGFloat *components = CGColorGetComponents(color);
    CGFloat r = components[0];
    CGFloat g = components[1];
    CGFloat b = components[2];
    return [NSString stringWithFormat:@"#%02lX%02lX%02lX",
            lroundf(r * 255),
            lroundf(g * 255),
            lroundf(b * 255)];
}

- (CGColorRef)colorFromHexString:(NSString *)hexString {
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:hexString];
    [scanner setScanLocation:1]; // bypass '#' character
    [scanner scanHexInt:&rgbValue];
    
    CGFloat red = ((rgbValue & 0xFF0000) >> 16) / 255.0;
    CGFloat green = ((rgbValue & 0x00FF00) >> 8) / 255.0;
    CGFloat blue = (rgbValue & 0x0000FF) / 255.0;
    
    return CGColorCreateGenericRGB(red, green, blue, 1.0);
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::ObjCTurboModule>(params);
}
#endif

@end