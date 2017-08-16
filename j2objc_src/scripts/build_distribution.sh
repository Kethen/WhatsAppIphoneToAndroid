#!/bin/bash
#
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
#
# Build j2objc distribution bundle.
#
# Usage:
#   build_distribution.sh <version-number>

if [ $(basename $(pwd)) != "j2objc" ]; then
  echo "This script must be run in the top-level j2objc directory."
  exit 1
fi

if [ $# -ne 2 ]; then
  echo "usage: build_distribution.sh <version-number> <protobuf-install-dir>"
  exit 1
fi
DISTRIBUTION_NAME=j2objc-$1

# Set j2objc flags used for public builds.
TRANSLATE_GLOBAL_FLAGS="--doc-comments;--generate-deprecated;--swift-friendly"

ENV_CMD="env -i PATH=$PATH HOME=$HOME J2OBJC_VERSION=${1%/} PROTOBUF_ROOT_DIR=${2%/}"
ENV_CMD="${ENV_CMD} TRANSLATE_GLOBAL_FLAGS=${TRANSLATE_GLOBAL_FLAGS}"

echo "make clean"
$ENV_CMD make clean
ERR=$?
if [ ${ERR} -ne 0 ]; then
  exit ${ERR}
fi

# Remove any previous distribution artifacts.
rm -rf ${DISTRIBUTION_NAME} ${DISTRIBUTION_NAME}.zip

echo "make all_dist"
$ENV_CMD make -j8 all_dist
ERR=$?
if [ ${ERR} -ne 0 ]; then
  exit ${ERR}
fi

echo "make test_all"
$ENV_CMD make -j8 test_all
ERR=$?
if [ ${ERR} -ne 0 ]; then
  exit ${ERR}
fi

mv dist ${DISTRIBUTION_NAME}
zip -ry ${DISTRIBUTION_NAME}.zip ${DISTRIBUTION_NAME}
