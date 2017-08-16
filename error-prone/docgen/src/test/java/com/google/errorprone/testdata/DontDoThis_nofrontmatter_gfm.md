<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>ONE_OFF</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
</table></div>

# DontDoThis
__Don&#39;t do this; do List&lt;Foo&gt; instead__

## The problem
This is a bad idea, you want `List<Foo>` instead

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DontDoThis")` annotation to the enclosing element.
