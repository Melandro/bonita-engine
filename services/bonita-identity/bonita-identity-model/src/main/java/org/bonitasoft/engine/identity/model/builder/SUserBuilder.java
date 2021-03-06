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
package org.bonitasoft.engine.identity.model.builder;

import org.bonitasoft.engine.identity.model.SUser;

/**
 * @author Baptiste Mesta
 * @author Yanyan Liu
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public interface SUserBuilder {

    SUserBuilder createNewInstance();

    SUserBuilder createNewInstance(final SUser user);

    SUserBuilder setUserName(final String userName);

    SUserBuilder setPassword(final String password);

    SUserBuilder setFirstName(final String firstName);

    SUserBuilder setLastName(final String lastName);

    SUserBuilder setTitle(final String title);

    SUserBuilder setJobTitle(final String jobTitle);

    SUserBuilder setIconName(final String iconName);

    SUserBuilder setIconPath(final String iconPath);

    SUserBuilder setLastUpdate(final long lastUpdate);

    SUserBuilder setLastConnection(final Long lastConnecton);

    SUserBuilder setCreatedBy(final long createdBy);

    SUserBuilder setCreationDate(final long creationDate);

    SUserBuilder setManagerUserId(final long managerUserId);

    SUserBuilder setDelegeeUserName(final String delegeeUserName);

    SUserBuilder setEnabled(final boolean enabled);

    SUser done();

    String getIdKey();

    String getUserNameKey();

    String getPasswordKey();

    String getFirstNameKey();

    String getLastNameKey();

    String getTitleKey();

    String getJobTitleKey();

    String getIconNameKey();

    String getIconPathKey();

    String getLastUpdateKey();

    String getLastConnectionKey();

    String getCreatedByKey();

    String getCreationDateKey();

    String getManagerUserIdKey();

    String getDelegeeUserNameKey();

    String getEnabledKey();

}
