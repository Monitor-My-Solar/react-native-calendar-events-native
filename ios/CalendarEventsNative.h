#ifdef RCT_NEW_ARCH_ENABLED
#import <RNCalendarEventsNativeSpec/RNCalendarEventsNativeSpec.h>
@interface CalendarEventsNative : NSObject <NativeCalendarEventsNativeSpec>
#else
#import <React/RCTBridgeModule.h>
@interface CalendarEventsNative : NSObject <RCTBridgeModule>
#endif

@end