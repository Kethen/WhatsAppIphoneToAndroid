// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//
//  JRETestRunListener.m
//  JreEmulation
//

#import "JRETestRunListener.h"

#import "IOSClass.h"
#import "org/junit/runner/Description.h"
#import <mach/mach.h>

@interface JRETestRunListener ()

@property CFTimeInterval startTime;

@end

@implementation JRETestRunListener

static BOOL printStats = NO;

+ (void)initialize {
  printStats = getenv("JUNIT_PRINT_STATS");
}

vm_size_t usedMemory(void) {
  struct task_basic_info info;
  mach_msg_type_number_t size = sizeof(info);
  kern_return_t kerr = task_info(mach_task_self(), TASK_BASIC_INFO, (task_info_t)&info, &size);
  return (kerr == KERN_SUCCESS) ? info.resident_size : 0; // size in bytes
}

vm_size_t freeMemory(void) {
  mach_port_t host_port = mach_host_self();
  mach_msg_type_number_t host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
  vm_size_t pagesize;
  vm_statistics_data_t vm_stat;

  host_page_size(host_port, &pagesize);
  (void) host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size);
  return vm_stat.free_count * pagesize;
}

NSString *memUsage() {
  // compute memory usage and log if different by >= 100k
  static long prevMemUsage = 0;
  long curMemUsage = usedMemory();
  long memUsageDiff = curMemUsage - prevMemUsage;

  if (memUsageDiff > 100000 || memUsageDiff < -100000) {
    prevMemUsage = curMemUsage;
    return [NSString stringWithFormat:@", memory used %7.1f (%+5.0f), free %7.1f kb",
            curMemUsage/1000.0f, memUsageDiff/1000.0f, freeMemory()/1000.0f];
  }
  return @"";
}

- (void)testRunStartedWithOrgJunitRunnerDescription:(OrgJunitRunnerDescription *)description_ {
  if (printStats) {
    NSLog(@"test run started");
  }
}

- (void)testRunFinishedWithOrgJunitRunnerResult:(OrgJunitRunnerResult *)result {
  if (printStats) {
    NSLog(@"test run finished  %@", memUsage());
  }
}

- (void)testStartedWithOrgJunitRunnerDescription:(OrgJunitRunnerDescription *)description_ {
  if (printStats) {
    NSLog(@"%@.%@", [[description_ getTestClass] getSimpleName], [description_ getMethodName]);
    self.startTime = CACurrentMediaTime();
  }
}

- (void)testFinishedWithOrgJunitRunnerDescription:(OrgJunitRunnerDescription *)description_{
  if (printStats) {
    CFTimeInterval elapsedTime = CACurrentMediaTime() - self.startTime;
    NSLog(@"    %.3f secs%@", elapsedTime, memUsage());
  }
}

@end
