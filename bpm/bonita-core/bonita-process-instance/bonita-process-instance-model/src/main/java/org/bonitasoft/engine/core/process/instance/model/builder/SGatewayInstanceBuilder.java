/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.instance.model.builder;

import org.bonitasoft.engine.core.process.definition.model.SGatewayType;
import org.bonitasoft.engine.core.process.instance.model.SGatewayInstance;

/**
 * @author Feng Hui
 * @author Elias Ricken de Medeiros
 * @author Celine Souchet
 */
public interface SGatewayInstanceBuilder extends SFlowNodeInstanceBuilder {

    SGatewayInstanceBuilder createNewInstance(final String name, final long flowNodeDefinitionId, final long rootContainerId, long parentContainerId,
            final SGatewayType gatewayType, long processDefinitionId, long rootProcessInstanceId, long parentProcessInstanceId);

    SGatewayInstanceBuilder setStateId(final int stateId);

    SGatewayInstanceBuilder setGatewayType(final SGatewayType gatewayType);

    SGatewayInstanceBuilder setHitBys(final String hitBys);

    SGatewayInstanceBuilder setProcessInstanceId(final long processInstanceId);

    @Override
    SGatewayInstance done();

    String getGatewayTypeKey();

    String getHitBysKey();

}
