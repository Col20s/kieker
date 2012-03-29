/***************************************************************************
 * Copyright 2011 by
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

package kieker.test.tools.junit.traceAnalysis.filter.traceWriter;

import java.io.IOException;
import java.util.List;

import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.filter.AbstractExecutionTraceProcessingFilter;
import kieker.tools.traceAnalysis.filter.traceWriter.ExecutionTraceWriterFilter;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;

/**
 * 
 * @author Andre van Hoorn
 * 
 */
public class BasicExecutionTraceWriterFilterTest extends AbstractTraceWriterFilterTest {

	@Override
	protected AbstractExecutionTraceProcessingFilter provideWriterFilter(final String filename) throws IOException {
		final Configuration filterConfiguration = new Configuration();
		filterConfiguration.setProperty(ExecutionTraceWriterFilter.CONFIG_PROPERTY_NAME_OUTPUT_FN, filename);

		return new ExecutionTraceWriterFilter(filterConfiguration);
	}

	@Override
	protected String provideFilterInputName() {
		return ExecutionTraceWriterFilter.INPUT_PORT_NAME_EXECUTION_TRACES;
	}

	@Override
	protected String provideExpectedFileContent(final List<Object> loggedEvents) {
		final StringBuilder strB = new StringBuilder();
		for (final Object o : loggedEvents) {
			if (o instanceof ExecutionTrace) {
				strB.append(o.toString()).append("\n");
			}
		}
		return strB.toString();
	}

}
