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
package org.bonitasoft.engine.core.process.instance.model.archive.builder.event.impl;

import org.bonitasoft.engine.core.process.instance.model.archive.builder.event.SAStartEventInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.model.archive.builder.impl.SAFlowNodeInstanceBuilderImpl;
import org.bonitasoft.engine.core.process.instance.model.archive.event.SAStartEventInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.event.impl.SAStartEventInstanceImpl;
import org.bonitasoft.engine.core.process.instance.model.event.SStartEventInstance;

/**
 * @author Elias Ricken de Medeiros
 */
public class SAStartEventInstanceBuilderImpl extends SAFlowNodeInstanceBuilderImpl implements SAStartEventInstanceBuilder {

    private SAStartEventInstanceImpl entity;

    @Override
    public SAStartEventInstanceBuilder createNewArchivedStartEventInstance(final SStartEventInstance startEventInstance) {
        this.entity = new SAStartEventInstanceImpl(startEventInstance);
        return this;
    }

    @Override
    public SAStartEventInstance done() {
        return this.entity;
    }

}
