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
package org.bonitasoft.engine.execution.work;

import org.bonitasoft.engine.execution.ContainerRegistry;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.work.TxBonitaWork;

/**
 * @author Baptiste Mesta
 * @author Celine Souchet
 */
public class NotifyChildFinishedWork extends TxBonitaWork {

    private static final long serialVersionUID = -8987586943379865375L;

    private final long processDefinitionId;

    private final long flowNodeInstanceId;

    private final String parentType;

    private final int stateId;

    private final long parentId;

    public NotifyChildFinishedWork(final long processDefinitionId, final long flowNodeInstanceId, final long parentId, final String parentType,
            final int stateId) {
        super();
        this.processDefinitionId = processDefinitionId;
        this.flowNodeInstanceId = flowNodeInstanceId;
        this.parentId = parentId;
        this.parentType = parentType;
        this.stateId = stateId;
    }

    @Override
    protected void work() throws Exception {
        final ContainerRegistry containerRegistry = getTenantAccessor().getContainerRegistry();
        containerRegistry.nodeReachedState(processDefinitionId, flowNodeInstanceId, stateId, parentId, parentType);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + ": processInstanceId:" + parentId + ", flowNodeInstanceId: " + flowNodeInstanceId;
    }

    protected TenantServiceAccessor getTenantAccessor() {
        try {
            return TenantServiceSingleton.getInstance(getTenantId());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
