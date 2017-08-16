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

/**
 * Node type for a parameterized type.
 */
public class ParameterizedType extends Type {

  private ChildLink<Type> type = ChildLink.create(Type.class, this);

  public ParameterizedType() {}

  public ParameterizedType(ParameterizedType other) {
    super(other);
    type.copyFrom(other.getType());
  }

  @Override
  public Kind getKind() {
    return Kind.PARAMETERIZED_TYPE;
  }

  public Type getType() {
    return type.get();
  }

  public ParameterizedType setType(Type newType) {
    type.set(newType);
    return this;
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      type.accept(visitor);
    }
    visitor.endVisit(this);
  }

  @Override
  public ParameterizedType copy() {
    return new ParameterizedType(this);
  }
}
