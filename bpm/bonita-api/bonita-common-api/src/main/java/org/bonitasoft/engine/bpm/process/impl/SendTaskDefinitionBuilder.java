/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.bpm.process.impl;

import org.bonitasoft.engine.bpm.flownode.impl.FlowElementContainerDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.SendTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.ThrowMessageEventTriggerDefinitionImpl;
import org.bonitasoft.engine.expression.Expression;

/**
 * @author Matthieu Chaffotte
 */
public class SendTaskDefinitionBuilder extends ActivityDefinitionBuilder {

    public SendTaskDefinitionBuilder(final ProcessDefinitionBuilder processDefinitionBuilder, final FlowElementContainerDefinitionImpl process,
            final String taskName, final String messageName, final Expression targetProcess) {
        super(process, processDefinitionBuilder, new SendTaskDefinitionImpl(taskName, messageName, targetProcess));
        if (messageName == null || messageName.isEmpty()) {
            processDefinitionBuilder.addError("Message is not set on the send task " + taskName);
        }
    }

    public SendTaskDefinitionBuilder setTargetFlowNode(final Expression targetFlowNode) {
        getActivity().setTargetFlowNode(targetFlowNode);
        return this;
    }

    public DataDefinitionBuilder addMessageContentExpression(final Expression displayName, final Expression messageContent) {
        final String dataName = displayName.getContent(); // FIXME evaluate the expression
        final String className = messageContent.getReturnType();
        return new DataDefinitionBuilder(getProcessBuilder(), getContainer(), (ThrowMessageEventTriggerDefinitionImpl) getActivity().getMessageTrigger(),
                dataName, className, messageContent);
    }

    public SendTaskDefinitionBuilder addCorrelation(final Expression correlationKey, final Expression value) {
        final SendTaskDefinitionImpl sendTask = getActivity();
        sendTask.addCorrelation(correlationKey, value);
        if (sendTask.getMessageTrigger().getCorrelations().size() > 5) {
            getProcessBuilder().addError("The limit of correlation keys are 5 on send task: " + sendTask.getName());
        }
        return this;
    }

    @Override
    SendTaskDefinitionImpl getActivity() {
        return (SendTaskDefinitionImpl) super.getActivity();
    }

}
