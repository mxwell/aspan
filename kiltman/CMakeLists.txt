cmake_minimum_required(VERSION 3.10)

project(kiltman)

add_executable(kiltman flat_node_trie.cpp kiltman.cpp runes.cpp trie.cpp)
add_subdirectory(poco)
target_link_libraries(kiltman PUBLIC Poco::Net Poco::Util)