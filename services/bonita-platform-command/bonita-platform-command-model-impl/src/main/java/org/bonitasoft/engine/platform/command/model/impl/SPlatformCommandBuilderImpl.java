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
package org.bonitasoft.engine.platform.command.model.impl;

import org.bonitasoft.engine.platform.command.model.SPlatformCommand;
import org.bonitasoft.engine.platform.command.model.SPlatformCommandBuilder;

/**
 * @author Zhang Bole
 */
public class SPlatformCommandBuilderImpl implements SPlatformCommandBuilder {

    private static final String ID = "id";

    static final String NAME = "name";

    static final String DESCRIPTION = "description";

    static final String IMPLEMENTATION = "implementation";

    private SPlatformCommandImpl command;

    @Override
    public SPlatformCommandBuilder createNewInstance(final SPlatformCommand command) {
        this.command = new SPlatformCommandImpl(command);
        return this;
    }

    @Override
    public SPlatformCommandBuilder createNewInstance(final String name, final String description, final String implementationClass) {
        this.command = new SPlatformCommandImpl(name, description, implementationClass);
        return this;
    }

    @Override
    public SPlatformCommand done() {
        return command;
    }

    @Override
    public String getIdKey() {
        return ID;
    }

    @Override
    public String getNameKey() {
        return NAME;
    }

    @Override
    public String getDescriptionKey() {
        return DESCRIPTION;
    }

    @Override
    public String getImplementationClassKey() {
        return IMPLEMENTATION;
    }

}
