#ifdef RCT_NEW_ARCH_ENABLED
#import <RNCalendarEventsNativeSpec/RNCalendarEventsNativeSpec.h>
@interface CalendarEventsNative : NativeCalendarEventsNativeSpecSpecBase <NativeCalendarEventsNativeSpecSpec>
#else
#import <React/RCTBridgeModule.h>
@interface CalendarEventsNative : NSObject <RCTBridgeModule>
#endif

@end