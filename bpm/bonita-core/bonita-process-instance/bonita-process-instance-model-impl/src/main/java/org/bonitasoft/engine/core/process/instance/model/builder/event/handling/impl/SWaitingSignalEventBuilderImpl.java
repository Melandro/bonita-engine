/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.instance.model.builder.event.handling.impl;

import org.bonitasoft.engine.core.process.instance.model.builder.event.handling.SWaitingSignalEventBuilder;
import org.bonitasoft.engine.core.process.instance.model.event.handling.SBPMEventType;
import org.bonitasoft.engine.core.process.instance.model.event.handling.SWaitingSignalEvent;
import org.bonitasoft.engine.core.process.instance.model.event.handling.impl.SWaitingSignalEventImpl;

/**
 * @author Zhao Na
 * @author Matthieu Chaffotte
 * @author Elias Ricken de Medeiros
 * @author Celine Souchet
 */
public class SWaitingSignalEventBuilderImpl extends SWaitingEventKeyProviderImpl implements SWaitingSignalEventBuilder {

    private SWaitingSignalEventImpl entity;

    @Override
    public SWaitingSignalEventBuilder createNewWaitingSignalStartEventInstance(final long processdefinitionId, final String signalName,
            final String processName, final long flowNodeDefinitionId, final String flowNodeName) {
        entity = new SWaitingSignalEventImpl(SBPMEventType.START_EVENT, processdefinitionId, processName, flowNodeDefinitionId, flowNodeName, signalName);
        return this;
    }

    @Override
    public SWaitingSignalEventBuilder createNewWaitingSignalEventSubProcInstance(final long processdefinitionId, final long parentProcessInstanceId,
            final long rootProcessInstanceId, final String signalName, final String processName, final long flowNodeDefinitionId, final String flowNodeName,
            final long subProcessId) {
        entity = new SWaitingSignalEventImpl(SBPMEventType.EVENT_SUB_PROCESS, processdefinitionId, processName, flowNodeDefinitionId, flowNodeName, signalName);
        entity.setParentProcessInstanceId(parentProcessInstanceId);
        entity.setRootProcessInstanceId(rootProcessInstanceId);
        entity.setSubProcessId(subProcessId);
        return this;
    }

    @Override
    public SWaitingSignalEventBuilder createNewWaitingSignalIntermediateEventInstance(final long processdefinitionId, final long processInstanceId,
            final long flowNodeInstanceId, final String signalName, final String processName, final long flowNodeDefinitionId, final String flowNodeName) {
        createNonStartEvent(processdefinitionId, processInstanceId, flowNodeInstanceId, signalName, processName, flowNodeDefinitionId, flowNodeName,
                SBPMEventType.INTERMEDIATE_CATCH_EVENT);
        return this;
    }

    protected void createNonStartEvent(final long processdefinitionId, final long processInstanceId, final long flowNodeInstanceId, final String signalName,
            final String processName, final long flowNodeDefinitionId, final String flowNodeName, final SBPMEventType eventType) {
        entity = new SWaitingSignalEventImpl(eventType, processdefinitionId, processName, flowNodeDefinitionId, flowNodeName, signalName);
        entity.setFlowNodeInstanceId(flowNodeInstanceId);
        entity.setRootProcessInstanceId(processInstanceId);
    }

    @Override
    public SWaitingSignalEvent done() {
        return entity;
    }

    @Override
    public SWaitingSignalEventBuilder createNewWaitingSignalBoundaryEventInstance(final long processdefinitionId, final long processInstanceId,
            final long flowNodeInstanceId, final String signalName, final String processName, final long flowNodeDefinitionId, final String flowNodeName) {
        createNonStartEvent(processdefinitionId, processInstanceId, flowNodeInstanceId, signalName, processName, flowNodeDefinitionId, flowNodeName,
                SBPMEventType.BOUNDARY_EVENT);
        return this;
    }

}
