/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.execution;

import java.util.List;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.instance.api.FlowNodeInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SFlowElementsContainerType;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.SStateCategory;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.core.process.instance.model.builder.SFlowNodeInstanceBuilder;
import org.bonitasoft.engine.lock.LockService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaSearchException;

/**
 * @author Elias Ricken de Medeiros
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 */
public class TransactionContainedProcessInstanceInterruptor extends AbstractProcessInstanceInterruptor {

    private final ProcessInstanceService processInstanceService;

    private final FlowNodeInstanceService flowNodeInstanceService;

    private final ContainerRegistry containerRegistry;

    public TransactionContainedProcessInstanceInterruptor(final BPMInstanceBuilders bpmInstanceBuilders, final ProcessInstanceService processInstanceService,
            final FlowNodeInstanceService flowNodeInstanceService, final ContainerRegistry containerRegistry, final LockService lockService,
            final TechnicalLoggerService logger) {
        super(bpmInstanceBuilders, lockService, logger);
        this.processInstanceService = processInstanceService;
        this.flowNodeInstanceService = flowNodeInstanceService;
        this.containerRegistry = containerRegistry;
    }

    @Override
    protected void setProcessStateCategory(final long processInstanceId, final SStateCategory stateCategory) throws SBonitaException {
        final SProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        processInstanceService.setStateCategory(processInstance, stateCategory);
    }

    @Override
    protected void resumeStableChildExecution(final long childId, final long processInstanceId, final long userId) throws SBonitaException {
        final SFlowNodeInstance flowNodeInstance = flowNodeInstanceService.getFlowNodeInstance(childId);
        final SFlowNodeInstanceBuilder flowNodeKeyProvider = getBpmInstanceBuilders().getSUserTaskInstanceBuilder();

        String containerType = SFlowElementsContainerType.PROCESS.name();
        final long parentActivity = flowNodeInstance.getLogicalGroup(flowNodeKeyProvider.getParentActivityInstanceIndex());
        if (parentActivity > 0) {
            containerType = SFlowElementsContainerType.FLOWNODE.name();
        }

        containerRegistry.executeFlowNode(flowNodeInstance.getId(), null, null, containerType,
                flowNodeInstance.getLogicalGroup(flowNodeKeyProvider.getParentProcessInstanceIndex()));
    }

    @Override
    protected List<SFlowNodeInstance> getChildren(final long processInstanceId) throws SBonitaException {
        final List<SFlowNodeInstance> flowNodeInstances = flowNodeInstanceService.searchFlowNodeInstances(SFlowNodeInstance.class,
                getQueryOptions(processInstanceId));
        return flowNodeInstances;
    }

    @Override
    protected List<SFlowNodeInstance> getChildrenExcept(final long processInstanceId, final long childExceptionId) throws SBonitaException {
        final List<SFlowNodeInstance> flowNodeInstances = flowNodeInstanceService.searchFlowNodeInstances(SFlowNodeInstance.class,
                getQueryOptions(processInstanceId, childExceptionId));
        return flowNodeInstances;
    }

    @Override
    protected long getNumberOfChildren(final long processInstanceId) throws SBonitaSearchException {
        final QueryOptions countOptions = new QueryOptions(0, 1, null, getFilterOptions(processInstanceId, getBpmInstanceBuilders()
                .getSUserTaskInstanceBuilder()), null);
        return flowNodeInstanceService.getNumberOfFlowNodeInstances(SFlowNodeInstance.class, countOptions);
    }

    @Override
    protected long getNumberOfChildrenExcept(final long processInstanceId, final long childExceptionId) throws SBonitaSearchException {
        final QueryOptions countOptions = new QueryOptions(0, 1, null, getFilterOptions(processInstanceId, childExceptionId, getBpmInstanceBuilders()
                .getSUserTaskInstanceBuilder()), null);
        return flowNodeInstanceService.getNumberOfFlowNodeInstances(SFlowNodeInstance.class, countOptions);
    }

    @Override
    protected void setChildStateCategory(final long flowNodeInstanceId, final SStateCategory stateCategory) throws SBonitaException {
        final SFlowNodeInstance flowNodeInstance = flowNodeInstanceService.getFlowNodeInstance(flowNodeInstanceId);
        flowNodeInstanceService.setStateCategory(flowNodeInstance, stateCategory);
    }

}
