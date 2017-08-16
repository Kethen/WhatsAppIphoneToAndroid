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
 * Synchronized statement node type.
 */
public class SynchronizedStatement extends Statement {

  private ChildLink<Expression> expression = ChildLink.create(Expression.class, this);
  private ChildLink<Block> body = ChildLink.create(Block.class, this);

  public SynchronizedStatement() {}

  public SynchronizedStatement(SynchronizedStatement other) {
    super(other);
    expression.copyFrom(other.getExpression());
    body.copyFrom(other.getBody());
  }

  @Override
  public Kind getKind() {
    return Kind.SYNCHRONIZED_STATEMENT;
  }

  public SynchronizedStatement(Expression expression) {
    this.expression.set(expression);
  }

  public Expression getExpression() {
    return expression.get();
  }

  public SynchronizedStatement setExpression(Expression newExpression) {
    expression.set(newExpression);
    return this;
  }

  public Block getBody() {
    return body.get();
  }

  public SynchronizedStatement setBody(Block newBody) {
    body.set(newBody);
    return this;
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      expression.accept(visitor);
      body.accept(visitor);
    }
    visitor.endVisit(this);
  }

  @Override
  public SynchronizedStatement copy() {
    return new SynchronizedStatement(this);
  }
}
