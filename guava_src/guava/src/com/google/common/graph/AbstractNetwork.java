/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.graph.GraphConstants.MULTIPLE_EDGES_CONNECTING;
import static java.util.Collections.unmodifiableSet;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link Network}. It is recommended to extend
 * this class rather than implement {@link Network} directly.
 *
 * <p>The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractNetwork<N, E> implements Network<N, E> {

  @Override
  public Graph<N> asGraph() {
    return new AbstractGraph<N>() {
      @Override
      public Set<N> nodes() {
        return AbstractNetwork.this.nodes();
      }

      @Override
      public Set<EndpointPair<N>> edges() {
        if (allowsParallelEdges()) {
          return super.edges(); // Defer to AbstractGraph implementation.
        }

        // Optimized implementation assumes no parallel edges (1:1 edge to EndpointPair mapping).
        return new AbstractSet<EndpointPair<N>>() {
          @Override
          public Iterator<EndpointPair<N>> iterator() {
            return Iterators.transform(
                AbstractNetwork.this.edges().iterator(),
                new Function<E, EndpointPair<N>>() {
                  @Override
                  public EndpointPair<N> apply(E edge) {
                    return incidentNodes(edge);
                  }
                });
          }

          @Override
          public int size() {
            return AbstractNetwork.this.edges().size();
          }

          // Mostly safe: We check contains(u) before calling successors(u), so we perform unsafe
          // operations only in weird cases like checking for an EndpointPair<ArrayList> in a
          // Network<LinkedList>.
          @SuppressWarnings("unchecked")
          @Override
          public boolean contains(@Nullable Object obj) {
            if (!(obj instanceof EndpointPair)) {
              return false;
            }
            EndpointPair<?> endpointPair = (EndpointPair<?>) obj;
            return isDirected() == endpointPair.isOrdered()
                && nodes().contains(endpointPair.nodeU())
                && successors((N) endpointPair.nodeU()).contains(endpointPair.nodeV());
          }
        };
      }

      @Override
      public ElementOrder<N> nodeOrder() {
        return AbstractNetwork.this.nodeOrder();
      }

      @Override
      public boolean isDirected() {
        return AbstractNetwork.this.isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return AbstractNetwork.this.allowsSelfLoops();
      }

      @Override
      public Set<N> adjacentNodes(N node) {
        return AbstractNetwork.this.adjacentNodes(node);
      }

      @Override
      public Set<N> predecessors(N node) {
        return AbstractNetwork.this.predecessors(node);
      }

      @Override
      public Set<N> successors(N node) {
        return AbstractNetwork.this.successors(node);
      }

      // DO NOT override the AbstractGraph *degree() implementations.
    };
  }

  @Override
  public int degree(N node) {
    if (isDirected()) {
      return IntMath.saturatedAdd(inEdges(node).size(), outEdges(node).size());
    } else {
      return IntMath.saturatedAdd(incidentEdges(node).size(), edgesConnecting(node, node).size());
    }
  }

  @Override
  public int inDegree(N node) {
    return isDirected() ? inEdges(node).size() : degree(node);
  }

  @Override
  public int outDegree(N node) {
    return isDirected() ? outEdges(node).size() : degree(node);
  }

  @Override
  public Set<E> adjacentEdges(E edge) {
    EndpointPair<N> endpointPair = incidentNodes(edge); // Verifies that edge is in this network.
    Set<E> endpointPairIncidentEdges =
        Sets.union(incidentEdges(endpointPair.nodeU()), incidentEdges(endpointPair.nodeV()));
    return Sets.difference(endpointPairIncidentEdges, ImmutableSet.of(edge));
  }

  @Override
  public Set<E> edgesConnecting(N nodeU, N nodeV) {
    Set<E> outEdgesU = outEdges(nodeU);
    Set<E> inEdgesV = inEdges(nodeV);
    return outEdgesU.size() <= inEdgesV.size()
        ? unmodifiableSet(Sets.filter(outEdgesU, connectedPredicate(nodeU, nodeV)))
        : unmodifiableSet(Sets.filter(inEdgesV, connectedPredicate(nodeV, nodeU)));
  }

  private Predicate<E> connectedPredicate(final N nodePresent, final N nodeToCheck) {
    return new Predicate<E>() {
      @Override
      public boolean apply(E edge) {
        return incidentNodes(edge).adjacentNode(nodePresent).equals(nodeToCheck);
      }
    };
  }

  @Override
  public Optional<E> edgeConnecting(N nodeU, N nodeV) {
    Set<E> edgesConnecting = edgesConnecting(nodeU, nodeV);
    switch (edgesConnecting.size()) {
      case 0:
        return Optional.empty();
      case 1:
        return Optional.of(edgesConnecting.iterator().next());
      default:
        throw new IllegalArgumentException(String.format(MULTIPLE_EDGES_CONNECTING, nodeU, nodeV));
    }
  }

  @Override
  @Nullable
  public E edgeConnectingOrNull(N nodeU, N nodeV) {
    return edgeConnecting(nodeU, nodeV).orElse(null);
  }

  @Override
  public boolean hasEdgeConnecting(N nodeU, N nodeV) {
    return !edgesConnecting(nodeU, nodeV).isEmpty();
  }

  @Override
  public final boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Network)) {
      return false;
    }
    Network<?, ?> other = (Network<?, ?>) obj;

    return isDirected() == other.isDirected()
        && nodes().equals(other.nodes())
        && edgeIncidentNodesMap(this).equals(edgeIncidentNodesMap(other));
  }

  @Override
  public final int hashCode() {
    return edgeIncidentNodesMap(this).hashCode();
  }

  /** Returns a string representation of this network. */
  @Override
  public String toString() {
    return "isDirected: "
        + isDirected()
        + ", allowsParallelEdges: "
        + allowsParallelEdges()
        + ", allowsSelfLoops: "
        + allowsSelfLoops()
        + ", nodes: "
        + nodes()
        + ", edges: "
        + edgeIncidentNodesMap(this);
  }

  private static <N, E> Map<E, EndpointPair<N>> edgeIncidentNodesMap(final Network<N, E> network) {
    Function<E, EndpointPair<N>> edgeToIncidentNodesFn =
        new Function<E, EndpointPair<N>>() {
          @Override
          public EndpointPair<N> apply(E edge) {
            return network.incidentNodes(edge);
          }
        };
    return Maps.asMap(network.edges(), edgeToIncidentNodesFn);
  }
}
