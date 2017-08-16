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

package com.google.devtools.j2objc.jdt;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import org.eclipse.jdt.core.dom.ITypeBinding;

class JdtWildcardType extends JdtTypeMirror implements WildcardType {

  JdtWildcardType(ITypeBinding binding) {
    super(binding);
    assert binding.isWildcardType();
  }

  @Override
  public TypeKind getKind() {
    return TypeKind.WILDCARD;
  }

  @Override
  public <R, P> R accept(TypeVisitor<R, P> v, P p) {
    return v.visitWildcard(this, p);
  }

  @Override
  public TypeMirror getExtendsBound() {
    ITypeBinding wildcard = (ITypeBinding) binding;
    ITypeBinding bound = wildcard.getBound();
    if (bound != null && wildcard.isUpperbound()) {
      return BindingConverter.getType(bound);
    }
    return null;
  }

  @Override
  public TypeMirror getSuperBound() {
    ITypeBinding wildcard = (ITypeBinding) binding;
    ITypeBinding bound = wildcard.getBound();
    if (bound != null && !wildcard.isUpperbound()) {
      return BindingConverter.getType(bound);
    }
    return null;
  }
}
