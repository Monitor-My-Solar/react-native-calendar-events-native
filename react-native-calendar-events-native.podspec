require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1'

Pod::Spec.new do |s|
  s.name         = "react-native-calendar-events-native"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/yourusername/react-native-calendar-events-native.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  
  if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
    s.source_files = "ios/**/*.{h,m,mm,swift}"
    s.public_header_files = "ios/**/*.h"
  end

  s.frameworks = "EventKit", "EventKitUI"

  if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
    s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
    s.pod_target_xcconfig    = {
        "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
        "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -DFOLLY_CFG_NO_COROUTINES=1",
        "CLANG_CXX_LANGUAGE_STANDARD" => "c++20"
    }
    s.dependency "React-Codegen"
    s.dependency "RCT-Folly"
    s.dependency "RCTRequired"
    s.dependency "RCTTypeSafety"
    s.dependency "React-Core"
    s.dependency "React-RCTFabric"
    s.dependency "ReactCommon/turbomodule/core"
  else
    s.dependency "React-Core"
  end
end