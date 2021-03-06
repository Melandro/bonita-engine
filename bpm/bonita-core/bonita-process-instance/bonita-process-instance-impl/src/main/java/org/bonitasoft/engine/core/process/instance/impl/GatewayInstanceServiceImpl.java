/**
 * Copyright (C) 2011-2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.instance.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SObjectReadException;
import org.bonitasoft.engine.core.process.definition.model.SFlowElementContainerDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SGatewayDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.STransitionDefinition;
import org.bonitasoft.engine.core.process.instance.api.GatewayInstanceService;
import org.bonitasoft.engine.core.process.instance.api.TokenService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SGatewayCreationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SGatewayModificationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SGatewayNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SGatewayReadException;
import org.bonitasoft.engine.core.process.instance.model.SGatewayInstance;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.core.process.instance.model.builder.GatewayInstanceLogBuilder;
import org.bonitasoft.engine.core.process.instance.model.builder.SGatewayInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.recorder.SelectDescriptorBuilder;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SInsertEvent;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.queriablelogger.model.builder.SPersistenceLogBuilder;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;

/**
 * @author Feng Hui
 * @author Zhao Na
 * @author Matthieu Chaffotte
 */
public class GatewayInstanceServiceImpl implements GatewayInstanceService {

    private final Recorder recorder;

    private final EventService eventService;

    private final ReadPersistenceService persistenceRead;

    private final SGatewayInstanceBuilder sGatewayInstanceBuilder;

    private final BPMInstanceBuilders instanceBuilders;

    private final QueriableLoggerService queriableLoggerService;

    private final TokenService tokenService;

    private final TechnicalLoggerService logger;

    public GatewayInstanceServiceImpl(final Recorder recorder, final EventService eventService, final ReadPersistenceService persistenceRead,
            final BPMInstanceBuilders instanceBuilders, final QueriableLoggerService queriableLoggerService, final TechnicalLoggerService logger,
            final TokenService tokenService) {
        this.recorder = recorder;
        this.eventService = eventService;
        this.persistenceRead = persistenceRead;
        this.instanceBuilders = instanceBuilders;
        this.queriableLoggerService = queriableLoggerService;
        this.logger = logger;
        this.tokenService = tokenService;
        sGatewayInstanceBuilder = instanceBuilders.getSGatewayInstanceBuilder();
    }

    @Override
    public void createGatewayInstance(final SGatewayInstance gatewayInstance) throws SGatewayCreationException {
        final GatewayInstanceLogBuilder logBuilder = getQueriableLog(ActionType.CREATED, "Creating a new Gateway Instance", gatewayInstance);
        final InsertRecord insertRecord = new InsertRecord(gatewayInstance);
        SInsertEvent insertEvent = null;
        if (eventService.hasHandlers(GATEWAYINSTANCE, EventActionType.CREATED)) {
            insertEvent = (SInsertEvent) eventService.getEventBuilder().createInsertEvent(GATEWAYINSTANCE).setObject(gatewayInstance).done();
        }
        try {
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(gatewayInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "createGatewayInstance");
        } catch (final SRecorderException e) {
            initiateLogBuilder(gatewayInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "createGatewayInstance");

            throw new SGatewayCreationException(e);
        }

        logger.log(this.getClass(), TechnicalLogSeverity.INFO,
                "Created gateway instance <" + gatewayInstance.getName() + "> with id <" + gatewayInstance.getId() + ">");
    }

    @Override
    public SGatewayInstance getGatewayInstance(final long gatewayInstanceId) throws SGatewayNotFoundException, SGatewayReadException {
        SGatewayInstance selectOne;
        try {
            selectOne = persistenceRead.selectById(SelectDescriptorBuilder.getElementById(SGatewayInstance.class, "SGatewayInstance", gatewayInstanceId));
        } catch (final SBonitaReadException e) {
            throw new SGatewayReadException(e);
        }
        if (selectOne == null) {
            throw new SGatewayNotFoundException(gatewayInstanceId);
        }
        return selectOne;
    }

    @Override
    public boolean checkMergingCondition(final SProcessDefinition sDefinition, final SGatewayInstance gatewayInstance) throws SBonitaException {
        switch (gatewayInstance.getGatewayType()) {
            case EXCLUSIVE:
                return true;
            case INCLUSIVE:
                return inclusiveBehavior(sDefinition, gatewayInstance);
            case PARALLEL:
                return parallelBehavior(sDefinition, gatewayInstance);
            default:
                return false;
        }
    }

    private boolean inclusiveBehavior(final SProcessDefinition sDefinition, final SGatewayInstance gatewayInstance) throws SObjectReadException {
        final SFlowNodeDefinition flowNode = sDefinition.getProcessContainer().getFlowNode(gatewayInstance.getFlowNodeDefinitionId());
        if (flowNode.getIncomingTransitions().size() == 1) {
            return true;
        }
        // get the token refId that hit the gate
        final Long tokenRefId = gatewayInstance.getTokenRefId();
        // if there is NO more token than the number of transitions that hit the gate merge is ok
        final int size = getHitByTransitionList(gatewayInstance).size();
        return tokenService.getNumberOfToken(gatewayInstance.getParentContainerId(), tokenRefId) <= size;
    }

    private boolean parallelBehavior(final SProcessDefinition sDefinition, final SGatewayInstance gatewayInstance) {
        final List<String> hitsBy = getHitByTransitionList(gatewayInstance);
        final List<STransitionDefinition> trans = getTransitionDefinitions(gatewayInstance, sDefinition);
        boolean go = true;
        int i = 1;
        while (go && i <= trans.size()) {
            go = hitsBy.contains(String.valueOf(i));
            i++;
        }
        return go;
    }

    /**
     * @return
     *         the list of transition indexes that hit the gateway
     */
    private List<String> getHitByTransitionList(final SGatewayInstance gatewayInstance) {
        return Arrays.asList(gatewayInstance.getHitBys().split(","));
    }

    protected List<STransitionDefinition> getTransitionDefinitions(final SGatewayInstance gatewayInstance, final SProcessDefinition processDefinition) {
        final SFlowElementContainerDefinition processContainer = processDefinition.getProcessContainer();
        final SGatewayDefinition gatewayDefinition = processContainer.getGateway(gatewayInstance.getName());
        return gatewayDefinition.getIncomingTransitions();
    }

    @Override
    public void setState(final SGatewayInstance gatewayInstance, final int stateId) throws SGatewayModificationException {
        updateOneColum(gatewayInstance, sGatewayInstanceBuilder.getStateIdKey(), stateId, GATEWAYINSTANCE_STATE);
    }

    @Override
    public void hitTransition(final SGatewayInstance gatewayInstance, final long transitionIndex) throws SGatewayModificationException,
            SGatewayCreationException {
        final String hitBys = gatewayInstance.getHitBys();
        String columnValue;
        if (hitBys == null || hitBys.isEmpty()) {
            columnValue = String.valueOf(transitionIndex);
        } else {
            columnValue = hitBys + "," + String.valueOf(transitionIndex);
        }
        updateOneColum(gatewayInstance, sGatewayInstanceBuilder.getHitBysKey(), columnValue, GATEWAYINSTANCE_HITBYS);
    }

    private void updateOneColum(final SGatewayInstance gatewayInstance, final String columnName, final Serializable columnValue, final String event)
            throws SGatewayModificationException {
        final GatewayInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Updating gateway instance " + columnName, gatewayInstance);
        final EntityUpdateDescriptor entityUpdateDescriptor = new EntityUpdateDescriptor();
        entityUpdateDescriptor.addField(columnName, columnValue);

        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(gatewayInstance, entityUpdateDescriptor);

        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(event, EventActionType.UPDATED)) {
            updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(event).setObject(gatewayInstance).done();
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(gatewayInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateOneColum");
        } catch (final SRecorderException e) {
            initiateLogBuilder(gatewayInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateOneColum");
            throw new SGatewayModificationException(e);
        }
    }

    private <T extends SLogBuilder> void initializeLogBuilder(final T logBuilder, final String message) {
        logBuilder.createNewInstance().actionStatus(SQueriableLog.STATUS_FAIL).severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    private <T extends HasCRUDEAction> void updateLog(final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    private GatewayInstanceLogBuilder getQueriableLog(final ActionType actionType, final String message, final SGatewayInstance gatewayInstance) {
        final GatewayInstanceLogBuilder logBuilder = instanceBuilders.getGatewayInstanceLogBuilder();
        this.initializeLogBuilder(logBuilder, message);
        this.updateLog(actionType, logBuilder);
        logBuilder.processInstanceId(gatewayInstance.getRootContainerId());
        return logBuilder;
    }

    @Override
    public SGatewayInstance getActiveGatewayInstanceOfTheProcess(final long parentProcessInstanceId, final String name) throws SGatewayNotFoundException,
            SGatewayReadException {
        SGatewayInstance selectOne;
        try {
            selectOne = persistenceRead.selectOne(SelectDescriptorBuilder.getActiveGatewayInstanceOfProcess(parentProcessInstanceId, name));// FIXME select more
            // one and get the oldest
        } catch (final SBonitaReadException e) {
            throw new SGatewayReadException(e);
        }
        if (selectOne == null) {
            throw new SGatewayNotFoundException(parentProcessInstanceId, name);
        }
        return selectOne;
    }

    private void initiateLogBuilder(final long objectId, final int sQueriableLogStatus, final SPersistenceLogBuilder logBuilder, final String callerMethodName) {
        logBuilder.actionScope(String.valueOf(objectId));
        logBuilder.actionStatus(sQueriableLogStatus);
        logBuilder.objectId(objectId);
        final SQueriableLog log = logBuilder.done();
        if (queriableLoggerService.isLoggable(log.getActionType(), log.getSeverity())) {
            queriableLoggerService.log(this.getClass().getName(), callerMethodName, log);
        }
    }

    @Override
    public void setFinish(final SGatewayInstance gatewayInstance) throws SGatewayModificationException {
        final String columnValue = FINISH + gatewayInstance.getHitBys().split(",").length;
        updateOneColum(gatewayInstance, sGatewayInstanceBuilder.getHitBysKey(), columnValue, GATEWAYINSTANCE_HITBYS);
    }

    @Override
    public SGatewayInstance getGatewayMergingToken(final long processInstanceId, final Long tokenRefId) throws SGatewayReadException {
        final HashMap<String, Object> hashMap = new HashMap<String, Object>(2);
        hashMap.put("processInstanceId", processInstanceId);
        hashMap.put("tokenRefId", tokenRefId);
        try {
            return persistenceRead.selectOne(new SelectOneDescriptor<SGatewayInstance>("getGatewayMergingToken", hashMap, SGatewayInstance.class));
        } catch (final SBonitaReadException e) {
            throw new SGatewayReadException(e);
        }
    }

}
