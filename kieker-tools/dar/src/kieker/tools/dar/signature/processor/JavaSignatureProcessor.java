/***************************************************************************
 * Copyright (C) 2023 Kieker Project (https://kieker-monitoring.net)
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
package kieker.tools.dar.signature.processor;

import kieker.analysis.architecture.recovery.signature.AbstractSignatureProcessor;

/**
 * Processes Java signatures.
 *
 * @author Reiner Jung
 * @since 2.0.0
 */
public class JavaSignatureProcessor extends AbstractSignatureProcessor {

	public JavaSignatureProcessor(final boolean caseInsensitive) {
		super(caseInsensitive);
	}

	@Override
	public void processSignatures(final String componentSignature, final String operationSignature) {
		this.componentSignature = componentSignature;
		this.operationSignature = operationSignature.replaceAll("[0-9A-Za-z$_.]*\\.", "");
	}

}
