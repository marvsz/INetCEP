# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.10

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:


#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:


# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list


# Suppress display of executed commands.
$(VERBOSE).SILENT:


# A target that is always out of date.
cmake_force:

.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/bin/cmake

# The command to remove a file.
RM = /usr/bin/cmake -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel

# Utility rule file for ccnl-lxkernel.

# Include the progress variables for this target.
include CMakeFiles/ccnl-lxkernel.dir/progress.make

CMakeFiles/ccnl-lxkernel: ccnl-lxkernel/ccnl-lxkernel.ko


ccnl-lxkernel/ccnl-lxkernel.ko: ccn-lite-lnxkernel.c
ccnl-lxkernel/ccnl-lxkernel.ko: Kbuild.in
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --blue --bold --progress-dir=/home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Generating ccnl-lxkernel/ccnl-lxkernel.ko"
	cd /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel/ccnl-lxkernel && /usr/bin/make -C /lib/modules/5.3.0-26-generic/build M=/home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel/ccnl-lxkernel modules

ccnl-lxkernel: CMakeFiles/ccnl-lxkernel
ccnl-lxkernel: ccnl-lxkernel/ccnl-lxkernel.ko
ccnl-lxkernel: CMakeFiles/ccnl-lxkernel.dir/build.make

.PHONY : ccnl-lxkernel

# Rule to build all files generated by this target.
CMakeFiles/ccnl-lxkernel.dir/build: ccnl-lxkernel

.PHONY : CMakeFiles/ccnl-lxkernel.dir/build

CMakeFiles/ccnl-lxkernel.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/ccnl-lxkernel.dir/cmake_clean.cmake
.PHONY : CMakeFiles/ccnl-lxkernel.dir/clean

CMakeFiles/ccnl-lxkernel.dir/depend:
	cd /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel /home/johannes/INetCEP/ccn-lite/src/ccnl-lnxkernel/CMakeFiles/ccnl-lxkernel.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/ccnl-lxkernel.dir/depend
