# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Builds a J2ObjC translated Guava library.
#
# Author: Keith Stanger

J2OBJC_ROOT = ..

include ../make/common.mk
include ../make/j2objc_deps.mk
include ../java_deps/jars.mk

SRC_JAR = $(JAVA_DEPS_JAR_DIR)/$(GUAVA_SOURCE_JAR)
JAR = $(JAVA_DEPS_JAR_DIR)/$(GUAVA_JAR)
OBJS_DIR = $(BUILD_DIR)/objs
