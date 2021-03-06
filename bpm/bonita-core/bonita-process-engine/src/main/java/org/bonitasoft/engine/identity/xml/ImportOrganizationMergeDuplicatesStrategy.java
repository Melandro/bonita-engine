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
package org.bonitasoft.engine.identity.xml;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.engine.identity.ExportedUser;
import org.bonitasoft.engine.identity.GroupCreator;
import org.bonitasoft.engine.identity.GroupCreator.GroupField;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.RoleCreator;
import org.bonitasoft.engine.identity.RoleCreator.RoleField;
import org.bonitasoft.engine.identity.SIdentityException;
import org.bonitasoft.engine.identity.model.SContactInfo;
import org.bonitasoft.engine.identity.model.SGroup;
import org.bonitasoft.engine.identity.model.SRole;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.identity.model.SUserMembership;
import org.bonitasoft.engine.identity.model.builder.GroupUpdateBuilder;
import org.bonitasoft.engine.identity.model.builder.IdentityModelBuilder;
import org.bonitasoft.engine.identity.model.builder.RoleUpdateBuilder;
import org.bonitasoft.engine.identity.model.builder.SContactInfoUpdateBuilder;
import org.bonitasoft.engine.identity.model.builder.UserUpdateBuilder;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 */
public class ImportOrganizationMergeDuplicatesStrategy implements ImportOrganizationStrategy {

    private final IdentityService identityService;

    private final IdentityModelBuilder identityModelBuilder;

    public ImportOrganizationMergeDuplicatesStrategy(final IdentityService identityService, final IdentityModelBuilder identityModelBuilder) {
        this.identityService = identityService;
        this.identityModelBuilder = identityModelBuilder;
    }

    @Override
    public void foundExistingGroup(final SGroup existingGroup, final GroupCreator newGroup) throws SIdentityException {
        final EntityUpdateDescriptor descriptor = getGroupDescriptor(existingGroup, newGroup);
        if (!descriptor.getFields().isEmpty()) {
            identityService.updateGroup(existingGroup, descriptor);
        }
    }

    @Override
    public void foundExistingUser(final SUser existingUser, final ExportedUser user) throws SIdentityException {
        final long userId = existingUser.getId();
        final EntityUpdateDescriptor descriptor = getUserDescriptor(user);
        identityService.updateUser(existingUser, descriptor, user.isPasswordEncrypted());

        SContactInfo persoContactInfo = identityService.getUserContactInfo(userId, true);
        if (persoContactInfo == null) {
            persoContactInfo = identityModelBuilder.getUserContactInfoBuilder().createNewInstance(userId, true).done();
            identityService.createUserContactInfo(persoContactInfo);
        }
        final EntityUpdateDescriptor personalDataDesc = getUserContactInfoDescriptor(user, true);
        identityService.updateUserContactInfo(persoContactInfo, personalDataDesc);

        SContactInfo professContactInfo = identityService.getUserContactInfo(userId, false);
        if (professContactInfo == null) {
            professContactInfo = identityModelBuilder.getUserContactInfoBuilder().createNewInstance(userId, false).done();
            identityService.createUserContactInfo(professContactInfo);
        }
        final EntityUpdateDescriptor professionalDataDesc = getUserContactInfoDescriptor(user, false);
        identityService.updateUserContactInfo(professContactInfo, professionalDataDesc);
    }

    @Override
    public void foundExistingRole(final SRole existingRole, final RoleCreator newRole) throws SIdentityException {
        final EntityUpdateDescriptor descriptor = getRoleDescriptor(existingRole, newRole);
        if (!descriptor.getFields().isEmpty()) {
            identityService.updateRole(existingRole, descriptor);
        }
    }

    @Override
    public void foundExistingMembership(final SUserMembership existingMembership) {

    }

    protected EntityUpdateDescriptor getRoleDescriptor(final SRole existingRole, final RoleCreator roleCreator) {
        final RoleUpdateBuilder roleUpdateBuilder = identityModelBuilder.getRoleUpdateBuilder();
        final Map<RoleField, Serializable> fields = roleCreator.getFields();
        final String name = (String) fields.get(RoleField.NAME);
        if (name != null && !name.equals(existingRole.getName())) {
            roleUpdateBuilder.updateName(name);
        }
        final String description = (String) fields.get(RoleField.DESCRIPTION);
        if (description != null && !description.equals(existingRole.getDescription())) {
            roleUpdateBuilder.updateDescription(description);
        }
        final String displayName = (String) fields.get(RoleField.DISPLAY_NAME);
        if (displayName != null && !displayName.equals(existingRole.getDisplayName())) {
            roleUpdateBuilder.updateDisplayName(displayName);
        }
        final String iconName = (String) fields.get(RoleField.ICON_NAME);
        if (iconName != null && !iconName.equals(existingRole.getIconName())) {
            roleUpdateBuilder.updateIconName(iconName);
        }
        final String iconPath = (String) fields.get(RoleField.ICON_PATH);
        if (iconPath != null && !iconPath.equals(existingRole.getIconPath())) {
            roleUpdateBuilder.updateIconPath(iconPath);
        }
        return roleUpdateBuilder.done();
    }

    protected EntityUpdateDescriptor getGroupDescriptor(final SGroup existingGroup, final GroupCreator groupCreator) {
        final GroupUpdateBuilder groupUpdateBuilder = identityModelBuilder.getGroupUpdateBuilder();
        final Map<GroupField, Serializable> fields = groupCreator.getFields();
        final String name = (String) fields.get(GroupField.NAME);
        if (name != null && !name.equals(existingGroup.getName())) {
            groupUpdateBuilder.updateName(name);
        }
        final String parentPath = (String) fields.get(GroupField.PARENT_PATH);
        if (parentPath != null && !parentPath.equals(existingGroup.getParentPath())) {
            groupUpdateBuilder.updateName(parentPath);
        }
        final String description = (String) fields.get(GroupField.DESCRIPTION);
        if (description != null && !description.equals(existingGroup.getDescription())) {
            groupUpdateBuilder.updateDescription(description);
        }
        final String displayName = (String) fields.get(GroupField.DISPLAY_NAME);
        if (displayName != null && !displayName.equals(existingGroup.getDisplayName())) {
            groupUpdateBuilder.updateDisplayName(displayName);
        }
        final String iconName = (String) fields.get(GroupField.ICON_NAME);
        if (iconName != null && !iconName.equals(existingGroup.getIconName())) {
            groupUpdateBuilder.updateIconName(iconName);
        }
        final String iconPath = (String) fields.get(GroupField.ICON_PATH);
        if (iconPath != null && !iconPath.equals(existingGroup.getIconPath())) {
            groupUpdateBuilder.updateIconPath(iconPath);
        }
        return groupUpdateBuilder.done();
    }

    protected EntityUpdateDescriptor getUserContactInfoDescriptor(final ExportedUser user, final boolean isPersonal) {
        final SContactInfoUpdateBuilder updateBuilder = identityModelBuilder.getUserContactInfoUpdateBuilder();
        if (isPersonal) {
            updateBuilder.updateAddress(user.getPersonalAddress());
            updateBuilder.updateBuilding(user.getPersonalBuilding());
            updateBuilder.updateCity(user.getPersonalCity());
            updateBuilder.updateCountry(user.getPersonalCountry());
            updateBuilder.updateEmail(user.getPersonalEmail());
            updateBuilder.updateFaxNumber(user.getPersonalFaxNumber());
            updateBuilder.updateMobileNumber(user.getPersonalMobileNumber());
            updateBuilder.updatePhoneNumber(user.getPersonalPhoneNumber());
            updateBuilder.updateRoom(user.getPersonalRoom());
            updateBuilder.updateState(user.getPersonalState());
            updateBuilder.updateWebsite(user.getPersonalWebsite());
            updateBuilder.updateZipCode(user.getPersonalZipCode());
        } else {
            updateBuilder.updateAddress(user.getProfessionalAddress());
            updateBuilder.updateBuilding(user.getProfessionalBuilding());
            updateBuilder.updateCity(user.getProfessionalCity());
            updateBuilder.updateCountry(user.getProfessionalCountry());
            updateBuilder.updateEmail(user.getProfessionalEmail());
            updateBuilder.updateFaxNumber(user.getProfessionalFaxNumber());
            updateBuilder.updateMobileNumber(user.getProfessionalMobileNumber());
            updateBuilder.updatePhoneNumber(user.getProfessionalPhoneNumber());
            updateBuilder.updateRoom(user.getProfessionalRoom());
            updateBuilder.updateState(user.getProfessionalState());
            updateBuilder.updateWebsite(user.getProfessionalWebsite());
            updateBuilder.updateZipCode(user.getProfessionalZipCode());
        }
        return updateBuilder.done();
    }

    protected EntityUpdateDescriptor getUserDescriptor(final ExportedUser user) {
        final UserUpdateBuilder userUpdateBuilder = identityModelBuilder.getUserUpdateBuilder();
        userUpdateBuilder.updateFirstName(user.getFirstName());
        userUpdateBuilder.updateIconName(user.getIconName());
        userUpdateBuilder.updateIconPath(user.getIconPath());
        userUpdateBuilder.updateJobTitle(user.getJobTitle());
        userUpdateBuilder.updateLastName(user.getLastName());
        userUpdateBuilder.updateManagerUserId(user.getManagerUserId());
        userUpdateBuilder.updatePassword(user.getPassword());
        userUpdateBuilder.updateTitle(user.getTitle());
        userUpdateBuilder.updateUserName(user.getUserName());
        userUpdateBuilder.updateEnabled(user.isEnabled());
        return userUpdateBuilder.done();
    }

}
