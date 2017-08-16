Reference types that declare an `equals()` method, or that inherit `equals()`
from a type other than `Object`, should not be compared for reference equality
with `==` or `!=`. Instead, always compare for value equality with `.equals()`.

## FAQs

### How about comparing interned objects?

It's dangerous to rely on your instances being interned. We have no tooling to
check or enforce that, and it's easy to get wrong.

### But what about `Boolean` values? We _know_ there's just `TRUE` and `FALSE` (and `null`). Surely _they're_ okay!

Well, no, because some tricky client can always generate a new instance with
`new Boolean(true)`. Comparing with `equals` always works; comparing with `==`
doesn't.

### But `enum` values are always unique, so can't I compare _them_ with `==`?

Yes, _but_ that might confuse the reader, who must understand that your type has
special properties because it's an `enum`. Using `equals` everywhere can work
the same everywhere; special-casing for enums isn't worth it.

### How about a reference equality comparison before a more expensive content equality comparison?

We _do_ exempt methods that override `Object#equals()` from this check. In other
cases, calling `Type#equals()` should be just as fast, because that method will
likely be inlined, and the first thing it will likely do is that same instance
comparison.

Alternatively, if you're okay with accepting null, you could call
`java.util.Objects.equals()`, which first does a reference equality comparison
and then falls back to content equality for non-null arguments.

### How about asserting in a test that two different references point to the same object (or not)?

Both Truth and JUnit provide clearer ways to assert this.

Truth:

```
assertThat(a).isSameAs(b);
assertThat(a).isNotSameAs(b);
```

JUnit:

```
assertSame(b, a);
assertNotSame(b, a);
```

### How about comparing against a special marker instance?

Classes override `equals` to express when two instances should be treated as
interchangeable with each other. Predominant Java libraries and practices are
built on that assumption. Defining a "magic instance" for such a type goes
against this whole practice, leaving you vulnerable to unexpected bugs.

Consider choosing a sentinel value within the domain of the type (the moral
equivalent of `-1` for indexOf function calls) that you could compare against
using the normal `equals` method.

### So how can I put a special "nothing" value in my map?

Use `Optional<V>` as the value type of your map instead.

