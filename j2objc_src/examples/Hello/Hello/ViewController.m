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
//  ViewController.m
//  Hello
//

#import "ViewController.h"

#import "org/j2objc/Status.h"

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];

#ifdef NO_J2OBJC
  NSString *message = @"Hello";
#else
  NSString *message = [OrgJ2objcStatus hello];
#endif
  UIAlertController *alert =
      [UIAlertController alertControllerWithTitle:@"J2ObjC"
                                          message:message
                                   preferredStyle:UIAlertControllerStyleAlert];
  UIAlertAction *actionOk = [UIAlertAction actionWithTitle:@"OK"
                                                     style:UIAlertActionStyleDefault
                                                   handler:nil];
  [alert addAction:actionOk];
  [self presentViewController:alert animated:YES completion:nil];
}

@end
