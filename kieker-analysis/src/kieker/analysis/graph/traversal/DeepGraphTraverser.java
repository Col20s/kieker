/***************************************************************************
 * Copyright 2022 Kieker Project (http://kieker-monitoring.net)
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
 ***************************************************************************/

package kieker.analysis.graph.traversal;

import java.util.List;

import com.google.common.graph.MutableNetwork;

import kieker.analysis.graph.IEdge;
import kieker.analysis.graph.INode;

/**
 * @author Sören Henning
 *
 * @since 1.14
 */
public class DeepGraphTraverser extends AbstractGraphTraverser {

	public DeepGraphTraverser() {
		super();
	}

	public DeepGraphTraverser(final List<IVertexVisitor> vertexVisitors, final List<IEdgeVisitor> edgeVisitors) {
		super(vertexVisitors, edgeVisitors);
	}

	public DeepGraphTraverser(final IVertexVisitor vertexVisitor, final IEdgeVisitor edgeVisitor) {
		super(vertexVisitor, edgeVisitor);
	}

	@Override
	public void traverse(final MutableNetwork<INode, IEdge> graph) {
		for (final INode vertex : graph.nodes()) {
			for (final IVertexVisitor visitor : this.vertexVisitors) {
				visitor.visitVertex(vertex);
			}
			if (vertex.getChildGraph() != null) {
				this.traverse(vertex.getChildGraph().getGraph());
			}
		}

		for (final IEdge edge : graph.edges()) {
			for (final IEdgeVisitor visitor : this.edgeVisitors) {
				visitor.visitEdge(edge);
			}
		}
	}

}
