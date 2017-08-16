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

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

class JdtExecutableType extends JdtTypeMirror implements ExecutableType {

  private TypeMirror superOuterParamType = null;

  JdtExecutableType(IMethodBinding binding) {
    super(binding);
  }

  @Override
  public TypeKind getKind() {
    return TypeKind.EXECUTABLE;
  }

  @Override
  public <R, P> R accept(TypeVisitor<R, P> v, P p) {
    return v.visitExecutable(this, p);
  }

  @Override
  public List<? extends TypeVariable> getTypeVariables() {
    List<TypeVariable> typeVars = new ArrayList<TypeVariable>();
    for (ITypeBinding typeVar : ((IMethodBinding) binding).getTypeParameters()) {
      typeVars.add((TypeVariable) BindingConverter.getType(typeVar));
    }
    return typeVars;
  }

  @Override
  public TypeMirror getReturnType() {
    return BindingConverter.getType(((IMethodBinding) binding).getReturnType());
  }

  void setSuperOuterParamType(TypeMirror superOuterParamType) {
    this.superOuterParamType = superOuterParamType;
  }

  @Override
  public List<? extends TypeMirror> getParameterTypes() {
    List<TypeMirror> params = new ArrayList<TypeMirror>();
    if (superOuterParamType != null) {
      params.add(superOuterParamType);
    }
    for (ITypeBinding param : ((IMethodBinding) binding).getParameterTypes()) {
      params.add(BindingConverter.getType(param));
    }
    return params;
  }

  @Override
  public TypeMirror getReceiverType() {
    return null;
  }

  @Override
  public List<? extends TypeMirror> getThrownTypes() {
    List<TypeMirror> exceptions = new ArrayList<TypeMirror>();
    for (ITypeBinding exception : ((IMethodBinding) binding).getExceptionTypes()) {
      exceptions.add((TypeVariable) BindingConverter.getType(exception));
    }
    return exceptions;
  }

}
