/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.Suppressibility;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/** A serialization-friendly POJO of the information in a {@link BugPattern}. */
public final class BugPatternInstance {

  private static final Function<AnnotationValue, String> TO_STRING =
      new Function<AnnotationValue, String>() {
        @Override
        public String apply(AnnotationValue input) {
          return input.toString();
        }
      };

  public String className;
  public String name;
  public String summary;
  public String explanation;
  public String[] altNames;
  public String category;
  public SeverityLevel severity;
  public Suppressibility suppressibility;
  public String[] customSuppressionAnnotations;
  public boolean documentSuppression = true;
  public boolean generateExamplesFromTestCases = true;

  public static BugPatternInstance fromElement(Element element) {
    BugPatternInstance instance = new BugPatternInstance();
    instance.className = element.toString();

    BugPattern annotation = element.getAnnotation(BugPattern.class);
    instance.name = annotation.name();
    instance.altNames = annotation.altNames();
    instance.severity = annotation.severity();
    instance.suppressibility = annotation.suppressibility();
    instance.summary = annotation.summary();
    instance.explanation = annotation.explanation();
    instance.documentSuppression = annotation.documentSuppression();

    Map<String, Object> keyValues = getAnnotation(element, BugPattern.class.getName());
    instance.category = verifyNotNull(keyValues.get("category")).toString();
    Object result = keyValues.get("customSuppressionAnnotations");
    if (result == null) {
      instance.customSuppressionAnnotations = new String[0];
    } else {
      Preconditions.checkState(result instanceof List);
      // The doc for AnnotationValue says that if the value is an array, then
      // AnnotationValue#getValue() will return a List<? extends AnnotationValue>.
      @SuppressWarnings("unchecked")
      List<? extends AnnotationValue> resultList = (List<? extends AnnotationValue>) result;
      instance.customSuppressionAnnotations =
          FluentIterable.from(resultList).transform(TO_STRING).toArray(String.class);
    }
    instance.generateExamplesFromTestCases =
        !keyValues.containsKey("generateExamplesFromTestCases")
            || (boolean) keyValues.get("generateExamplesFromTestCases");

    return instance;
  }

  private static Map<String, Object> getAnnotation(Element element, String name) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(name)) {
        return annotationKeyValues(mirror);
      }
    }
    throw new IllegalArgumentException(String.format("%s has no annotation %s", element, name));
  }

  private static Map<String, Object> annotationKeyValues(AnnotationMirror mirror) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (ExecutableElement key : mirror.getElementValues().keySet()) {
      result.put(key.getSimpleName().toString(), mirror.getElementValues().get(key).getValue());
    }
    return result;
  }
}
