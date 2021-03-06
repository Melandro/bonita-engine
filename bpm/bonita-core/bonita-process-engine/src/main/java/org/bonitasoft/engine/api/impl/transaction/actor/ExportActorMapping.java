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
package org.bonitasoft.engine.api.impl.transaction.actor;

import java.util.List;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.actor.mapping.model.SActor;
import org.bonitasoft.engine.actor.mapping.model.SActorMember;
import org.bonitasoft.engine.actor.xml.Actor;
import org.bonitasoft.engine.actor.xml.ActorMapping;
import org.bonitasoft.engine.actor.xml.ActorMappingNodeBuilder;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.model.SGroup;
import org.bonitasoft.engine.identity.model.SRole;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.xml.XMLNode;
import org.bonitasoft.engine.xml.XMLWriter;

/**
 * @author Matthieu Chaffotte
 */
public class ExportActorMapping implements TransactionContentWithResult<String> {

    private final ActorMappingService actorMappingService;

    private final IdentityService identityService;

    private final XMLWriter writer;

    private final long processDefinitionId;

    private String xmlContent;

    public ExportActorMapping(final ActorMappingService actorMappingService, final IdentityService identityService, final XMLWriter writer,
            final long processDefinitionId) {
        super();
        this.actorMappingService = actorMappingService;
        this.identityService = identityService;
        this.writer = writer;
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public void execute() throws SBonitaException {
        final ActorMapping mapping = getActorMapping();
        final XMLNode node = getNode(mapping);
        final byte[] bytes = writer.write(node);
        xmlContent = new String(bytes);
    }

    private ActorMapping getActorMapping() throws SBonitaException {
        final ActorMapping actorMapping = new ActorMapping();
        final List<SActor> actors = actorMappingService.getActors(processDefinitionId);
        for (final SActor sActor : actors) {
            final Actor actor = new Actor(sActor.getName());
            final List<SActorMember> actorMembers = actorMappingService.getActorMembers(sActor.getId(), 0, Integer.MAX_VALUE);
            for (final SActorMember sActorMember : actorMembers) {
                if (sActorMember.getUserId() > 0) {
                    final SUser user = identityService.getUser(sActorMember.getUserId());
                    actor.addUser(user.getUserName());
                }
                if (sActorMember.getGroupId() > 0 && sActorMember.getRoleId() <= 0) {
                    final SGroup group = identityService.getGroup(sActorMember.getGroupId());
                    actor.addGroup(group.getPath());
                }
                if (sActorMember.getRoleId() > 0 && sActorMember.getGroupId() <= 0) {
                    final SRole role = identityService.getRole(sActorMember.getRoleId());
                    actor.addRole(role.getName());
                }
                if (sActorMember.getRoleId() > 0 && sActorMember.getGroupId() > 0) {
                    final SRole role = identityService.getRole(sActorMember.getRoleId());
                    final SGroup group = identityService.getGroup(sActorMember.getGroupId());
                    actor.addMembership(group.getPath(), role.getName());
                }
            }
            actorMapping.addActor(actor);
        }
        return actorMapping;
    }

    private XMLNode getNode(final ActorMapping mapping) {
        return ActorMappingNodeBuilder.getDocument(mapping);
    }

    @Override
    public String getResult() {
        return xmlContent;
    }

}
