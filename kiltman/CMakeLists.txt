cmake_minimum_required(VERSION 3.10)

project(kiltman)

set(CMAKE_FIND_LIBRARY_SUFFIXES ".a")
set(BUILD_SHARED_LIBS OFF)
set(CMAKE_EXE_LINKER_FLAGS "-static")

add_executable(kiltman flat_node_trie.cpp kiltman.cpp runes.cpp trie.cpp)
add_subdirectory(poco)
target_link_libraries(kiltman PUBLIC Poco::Net Poco::Util)
