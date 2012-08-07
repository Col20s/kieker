/***************************************************************************
 * Copyright 2012 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
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

package kieker.tools.traceAnalysis.filter.visualization.dependencyGraph;

import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.RepositoryPort;
import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.Constants;
import kieker.tools.traceAnalysis.filter.AbstractGraphProducingFilter;
import kieker.tools.traceAnalysis.filter.AbstractMessageTraceProcessingFilter;
import kieker.tools.traceAnalysis.filter.AbstractTraceAnalysisFilter;
import kieker.tools.traceAnalysis.systemModel.AbstractMessage;
import kieker.tools.traceAnalysis.systemModel.AssemblyComponent;
import kieker.tools.traceAnalysis.systemModel.MessageTrace;
import kieker.tools.traceAnalysis.systemModel.SynchronousReplyMessage;
import kieker.tools.traceAnalysis.systemModel.repository.AssemblyRepository;
import kieker.tools.traceAnalysis.systemModel.repository.SystemModelRepository;

/**
 * Refactored copy from LogAnalysis-legacy tool<br>
 * 
 * This class has exactly one input port named "in". The created graph is emitted through
 * the output port.
 * 
 * @author Andre van Hoorn, Lena St&ouml;ver, Matthias Rohr,
 */
@Plugin(repositoryPorts = @RepositoryPort(name = AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, repositoryType = SystemModelRepository.class),
		outputPorts = @OutputPort(name = AbstractGraphProducingFilter.OUTPUT_PORT_NAME_GRAPH))
public class ComponentDependencyGraphAssemblyFilter extends AbstractDependencyGraphFilter<AssemblyComponent> {

	private static final String CONFIGURATION_NAME = Constants.PLOTASSEMBLYCOMPONENTDEPGRAPH_COMPONENT_NAME;

	public ComponentDependencyGraphAssemblyFilter(final Configuration configuration) {
		super(configuration, new ComponentAssemblyDependencyGraph(AssemblyRepository.ROOT_ASSEMBLY_COMPONENT));
	}

	@Override
	@InputPort(
			name = AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES,
			description = "Receives message traces to be processed",
			eventTypes = { MessageTrace.class })
	public void inputMessageTraces(final MessageTrace t) {
		for (final AbstractMessage m : t.getSequenceAsVector()) {
			if (m instanceof SynchronousReplyMessage) {
				continue;
			}
			final AssemblyComponent senderComponent = m.getSendingExecution().getAllocationComponent().getAssemblyComponent();
			final AssemblyComponent receiverComponent = m.getReceivingExecution().getAllocationComponent().getAssemblyComponent();
			DependencyGraphNode<AssemblyComponent> senderNode = this.getGraph().getNode(senderComponent.getId());
			DependencyGraphNode<AssemblyComponent> receiverNode = this.getGraph().getNode(receiverComponent.getId());
			if (senderNode == null) {
				senderNode = new DependencyGraphNode<AssemblyComponent>(senderComponent.getId(), senderComponent, t);

				if (m.getSendingExecution().isAssumed()) {
					senderNode.setAssumed();
				}

				this.getGraph().addNode(senderNode.getId(), senderNode);
			}
			else {
				senderNode.addOrigin(t);
			}

			if (receiverNode == null) {
				receiverNode = new DependencyGraphNode<AssemblyComponent>(receiverComponent.getId(), receiverComponent, t);

				if (m.getReceivingExecution().isAssumed()) {
					receiverNode.setAssumed();
				}

				this.getGraph().addNode(receiverNode.getId(), receiverNode);
			}
			else {
				receiverNode.addOrigin(t);
			}

			final boolean assumed = this.isDependencyAssumed(senderNode, receiverNode);

			senderNode.addOutgoingDependency(receiverNode, assumed, t);
			receiverNode.addIncomingDependency(senderNode, assumed, t);

			this.invokeDecorators(m, senderNode, receiverNode);
		}

		this.reportSuccess(t.getTraceId());
	}

	@Override
	public String getConfigurationName() {
		return CONFIGURATION_NAME;
	}
}
