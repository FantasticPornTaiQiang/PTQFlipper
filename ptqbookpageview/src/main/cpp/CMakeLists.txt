cmake_minimum_required(VERSION 3.18.1)

project("bitmap-mesh")

add_library(bitmap-mesh SHARED
        bitmap-mesh.cpp)

# Include libraries needed for hello-jni lib
target_link_libraries(bitmap-mesh
                      android
                      jnigraphics
                      log)
