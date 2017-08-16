/*
 * Copyright (C) 2009 The Guava Authors
 *
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

package com.google.common.collect;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Even though {@link ImmutableSortedMap} cannot be instantiated, we still need a custom field
 * serializer. TODO(cpovirk): why? Does it help if ensure that the GWT and non-GWT classes have the
 * same fields? Is that worth the trouble?
 *
 * @author Chris Povirk
 */
public final class ImmutableSortedMap_CustomFieldSerializer {
  public static void deserialize(
      SerializationStreamReader reader, ImmutableSortedMap<?, ?> instance) {}

  public static ImmutableSortedMap<?, ?> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    return ImmutableSortedMap_CustomFieldSerializerBase.instantiate(reader);
  }

  public static void serialize(SerializationStreamWriter writer, ImmutableSortedMap<?, ?> instance)
      throws SerializationException {
    ImmutableSortedMap_CustomFieldSerializerBase.serialize(writer, instance);
  }
}
