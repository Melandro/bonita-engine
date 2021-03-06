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
package org.bonitasoft.engine.core.process.instance.model.archive.builder.impl;

import org.bonitasoft.engine.core.process.instance.model.archive.builder.SAFlowElementInstanceBuilder;

/**
 * @author Emmanuel Duchastenier
 */
public abstract class SAFlowElementInstanceBuilderImpl extends SANamedElementBuilderImpl implements SAFlowElementInstanceBuilder {

    private static final String DESCRIPTION_KEY = "description";

    private static final String ROOT_CONTAINER_ID_KEY = "rootContainerId";

    private static final String PARENT_CONTAINER_ID_KEY = "parentContainerId";

    private static final String LOGICAL_GROUP1_KEY = "logicalGroup1";

    private static final String LOGICAL_GROUP2_KEY = "logicalGroup2";

    private static final String LOGICAL_GROUP3_KEY = "logicalGroup3";

    private static final String LOGICAL_GROUP4_KEY = "logicalGroup4";

    private static final String KIND_KEY = "kind";

    protected final int PROCESS_DEFINITION_INDEX = 0;

    protected final int ROOT_PROCESS_INSTANCE_INDEX = 1;

    protected final int PARENT_ACTIVITY_INSTANCE_INDEX = 2;

    protected final int PARENT_PROCESS_INSTANCE_INDEX = 3;

    @Override
    public String getRootContainerIdKey() {
        return ROOT_CONTAINER_ID_KEY;
    }

    @Override
    public String getParentContainerIdKey() {
        return PARENT_CONTAINER_ID_KEY;
    }

    @Override
    public String getProcessDefinitionKey() {
        return LOGICAL_GROUP1_KEY;
    }

    @Override
    public String getRootProcessInstanceKey() {
        return LOGICAL_GROUP2_KEY;
    }

    @Override
    public String getParentProcessInstanceKey() {
        return LOGICAL_GROUP4_KEY;
    }

    @Override
    public String getParentActivityInstanceKey() {
        return LOGICAL_GROUP3_KEY;
    }

    @Override
    public int getProcessDefinitionIndex() {
        return PROCESS_DEFINITION_INDEX;
    }

    @Override
    public int getRootProcessInstanceIndex() {
        return ROOT_PROCESS_INSTANCE_INDEX;
    }

    @Override
    public int getParentProcessInstanceIndex() {
        return PARENT_PROCESS_INSTANCE_INDEX;
    }

    @Override
    public int getParentActivityInstanceIndex() {
        return PARENT_ACTIVITY_INSTANCE_INDEX;
    }

    @Override
    public String getDescriptionKey() {
        return DESCRIPTION_KEY;
    }

    @Override
    public String getKindKey() {
        return KIND_KEY;
    }

}
