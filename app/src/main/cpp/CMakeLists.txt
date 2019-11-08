cmake_minimum_required(VERSION 3.4.1)

# Standard Android dependencies
find_library(android-lib android)
find_library(log-lib log)

include_directories(.)

# General Sources
file(GLOB general_srcs "*.cc")
# Sensors Sources
file(GLOB sensors_srcs "sensors/*.cc")
file(GLOB sensors_android_srcs "sensors/android/*.cc")
# Util Sources
file(GLOB util_srcs "util/*.cc")
# Output binary
add_library(sensor_fusion_jni SHARED
    ${general_srcs}
    ${sensors_srcs}
    ${sensors_android_srcs}
    ${util_srcs})
# Build
target_link_libraries(sensor_fusion_jni
    ${android-lib}
    ${log-lib})
