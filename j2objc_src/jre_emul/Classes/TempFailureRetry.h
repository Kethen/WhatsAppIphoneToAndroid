/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /*
  * Macro definition from Android's JNIHelp.h.
  */
#ifndef TEMP_FAILURE_RETRY
/* Used to retry syscalls that can return EINTR. */
# define TEMP_FAILURE_RETRY(expression)           \
  (__extension__                                  \
    ({ int __result;                              \
      do __result = (int) (expression);           \
      while (__result == -1 && errno == EINTR);   \
      __result; }))
#endif
