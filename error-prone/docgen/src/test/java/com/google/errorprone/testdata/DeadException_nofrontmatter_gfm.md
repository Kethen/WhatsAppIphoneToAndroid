<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
</table></div>

# DeadException
__Exception created but not thrown__

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with new, but is not thrown, and the reference is lost.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DeadException")` annotation to the enclosing element.

----------

### Positive examples
__DeadExceptionPositiveCase.java__

```java
here is an example
```

