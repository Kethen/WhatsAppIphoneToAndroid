https://github.com/google/guava

# Guava: Google Core Libraries for Java

[![Build Status](https://travis-ci.org/google/guava.svg?branch=master)](https://travis-ci.org/google/guava)

Guava is a set of core libraries that includes new collection types (such as
multimap and multiset), immutable collections, a graph library, functional
types, an in-memory cache, and APIs/utilities for concurrency, I/O, hashing,
primitives, reflection, string processing, and much more!

Guava comes in two flavors.

*   The main flavor requires JDK 1.8 or higher.
*   If you need support for JDK 1.7 or Android, use the Android flavor. You can
    find the Android Guava source in the [`android` directory].

[`android` directory]: https://github.com/google/guava/tree/master/android

## Latest release

The most recent release is [Guava 23.0][current release], released August 4, 2017.

- 23.0 API Docs: [guava][guava-release-api-docs], [guava-testlib][testlib-release-api-docs]
- 23.0 API Diffs from 22.0: [guava][guava-release-api-diffs]

The Maven group ID is `com.google.guava`, and the artifact ID is `guava`. Use
version `23.0` for the main flavor, or `23.0-android` for the Android flavor.

To add a dependency on Guava using Maven, use the following:

```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>23.0</version>
  <!-- or, for Android: -->
  <version>23.0-android</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'com.google.guava:guava:23.0'
  // or, for Android:
  compile 'com.google.guava:guava:23.0-android'
}
```

## Snapshots

Snapshots of Guava built from the `master` branch are available through Maven
using version `24.0-SNAPSHOT`, or `24.0-android-SNAPSHOT` for the Android
flavor.

- Snapshot API Docs: [guava][guava-snapshot-api-docs]
- Snapshot API Diffs: [guava][guava-snapshot-api-diffs]

## Learn about Guava

- Our users' guide, [Guava Explained][]
- [A nice collection](http://www.tfnico.com/presentations/google-guava) of other helpful links

## Links

- [GitHub project](https://github.com/google/guava)
- [Issue tracker: Report a defect or feature request](https://github.com/google/guava/issues/new)
- [StackOverflow: Ask "how-to" and "why-didn't-it-work" questions](https://stackoverflow.com/questions/ask?tags=guava+java)
- [guava-discuss: For open-ended questions and discussion](http://groups.google.com/group/guava-discuss)

## IMPORTANT WARNINGS

1. APIs marked with the `@Beta` annotation at the class or method level
are subject to change. They can be modified in any way, or even
removed, at any time. If your code is a library itself (i.e. it is
used on the CLASSPATH of users outside your own control), you should
not use beta APIs, unless you repackage them (e.g. using ProGuard).

2. Deprecated non-beta APIs will be removed two years after the
release in which they are first deprecated. You must fix your
references before this time. If you don't, any manner of breakage
could result (you are not guaranteed a compilation error).

3. Serialized forms of ALL objects are subject to change unless noted
otherwise. Do not persist these and assume they can be read by a
future version of the library.

4. Our classes are not designed to protect against a malicious caller.
You should not use them for communication between trusted and
untrusted code.

5. For the mainline flavor, we unit-test the libraries using only OpenJDK 1.8 on
Linux. Some features, especially in `com.google.common.io`, may not work
correctly in other environments.

  For the Android flavor, our unit tests run on API level 10 (Gingerbread).

[current release]: https://github.com/google/guava/wiki/Release23
[guava-release-api-docs]: http://google.github.io/guava/releases/23.0/api/docs/
[testlib-release-api-docs]: http://www.javadoc.io/doc/com.google.guava/guava-testlib/23.0
[guava-release-api-diffs]: http://google.github.io/guava/releases/23.0/api/diffs/
[guava-snapshot-api-docs]: http://google.github.io/guava/releases/snapshot/api/docs/
[guava-snapshot-api-diffs]: http://google.github.io/guava/releases/snapshot/api/diffs/
[Guava Explained]: https://github.com/google/guava/wiki/Home
