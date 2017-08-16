/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.ast;

import com.google.devtools.j2objc.util.TypeUtil;
import javax.lang.model.type.TypeMirror;

/**
 * Either "true" or "false".
 */
public class BooleanLiteral extends Expression {

  private final TypeMirror typeMirror;

  public BooleanLiteral(BooleanLiteral other) {
    super(other);
    this.typeMirror = other.getTypeMirror();
  }

  public BooleanLiteral(boolean booleanValue, TypeUtil typeUtil) {
    this(booleanValue, typeUtil.getBoolean());
  }

  public BooleanLiteral(boolean booleanValue, TypeMirror typeMirror) {
    this.constantValue = booleanValue;
    this.typeMirror = typeMirror;
  }

  @Override
  public Kind getKind() {
    return Kind.BOOLEAN_LITERAL;
  }

  @Override
  public TypeMirror getTypeMirror() {
    return typeMirror;
  }

  public boolean booleanValue() {
    return (Boolean) constantValue;
  }

  public void setBooleanValue(boolean newBooleanValue) {
    constantValue = newBooleanValue;
  }

  @Override
  public BooleanLiteral setConstantValue(Object value) {
    assert value == null || value instanceof Boolean;
    return (BooleanLiteral) super.setConstantValue(value);
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    visitor.visit(this);
    visitor.endVisit(this);
  }

  @Override
  public BooleanLiteral copy() {
    return new BooleanLiteral(this);
  }
}
