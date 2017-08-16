[`SimpleDateFormat`][] is not thread-safe. The documentation recommendeds
creating separate format instances for each thread. If multiple threads access a
format concurrently, it must be synchronized externally.

The [Google Java Style Guide §5.2.4][style] requires `CONSTANT_CASE` to only be
used for static final fields whose contents are deeply immutable and whose
methods have no detectable side effects, so fields of type `SimpleDateFormat`
should not use `CONSTANT_CASE`.

TIP: Consider using the `java.time` API added in Java8, in particular
[`DateTimeFormatter`][]. One its many advantages over `SimpleDateFormat` is that
it is immutable and thread-safe.

If the date formatter is accessed by multiple threads, consider using
[`ThreadLocal`][]:

```java
private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm"));

```

If the field is never accessed by multiple threads, rename it to use
`lowerCamelCase`.

```java
@NotThreadSafe
private static final SimpleDateFormat dateFormat =
    new SimpleDateFormat("yyyy-MM-dd HH:mm");

```

[`SimpleDateFormat`]: http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
[`ThreadLocal`]: http://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html
[`DateTimeFormatter`]: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html

[style]: https://google.github.io/styleguide/javaguide.html#s5.2.4-constant-names
