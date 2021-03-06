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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.archive.ArchiveInsertRecord;
import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.archive.SDefinitiveArchiveNotFound;
import org.bonitasoft.engine.bpm.process.ProcessInstanceState;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SObjectModificationException;
import org.bonitasoft.engine.commons.exceptions.SObjectReadException;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.connector.exception.SConnectorInstanceDeletionException;
import org.bonitasoft.engine.core.process.comment.api.SCommentService;
import org.bonitasoft.engine.core.process.comment.model.archive.builder.SACommentBuilder;
import org.bonitasoft.engine.core.process.comment.model.builder.SCommentBuilders;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.model.SActivityDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeType;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.document.api.ProcessDocumentService;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.TokenService;
import org.bonitasoft.engine.core.process.instance.api.TransitionService;
import org.bonitasoft.engine.core.process.instance.api.event.EventInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityModificationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeDeletionException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeReadException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceCreationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceHierarchicalDeletionException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceModificationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceReadException;
import org.bonitasoft.engine.core.process.instance.model.SActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.SConnectorInstance;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.SStateCategory;
import org.bonitasoft.engine.core.process.instance.model.archive.SAActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.builder.SAProcessInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.core.process.instance.model.builder.ProcessInstanceLogBuilder;
import org.bonitasoft.engine.core.process.instance.model.builder.SProcessInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.model.event.SEventInstance;
import org.bonitasoft.engine.core.process.instance.recorder.SelectDescriptorBuilder;
import org.bonitasoft.engine.data.instance.api.DataInstanceContainer;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.data.instance.exception.SDataInstanceException;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SDeleteEvent;
import org.bonitasoft.engine.events.model.SInsertEvent;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilder;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.FilterOption;
import org.bonitasoft.engine.persistence.OrderByOption;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.queriablelogger.model.builder.SPersistenceLogBuilder;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;

/**
 * @author Elias Ricken de Medeiros
 * @author Matthieu Chaffotte
 * @author Yanyan Liu
 * @author Frederic Bouquet
 * @author Celine Souchet
 */
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    private static final String MANAGER_USER_ID = "managerUserId";

    private static final String USER_ID = "userId";

    private static final String SUPERVISED_BY = "SupervisedBy";

    private static final String INVOLVING_USER = "InvolvingUser";

    private static final String MANAGED_BY = "ManagedBy";

    private static final int BATCH_SIZE = 100;

    private final Recorder recorder;

    private final ReadPersistenceService persistenceRead;

    private final EventService eventService;

    private final ActivityInstanceService activityService;

    private final SProcessInstanceBuilder processInstanceKeyProvider;

    private final BPMInstanceBuilders bpmInstanceBuilders;

    private final EventInstanceService bpmEventInstanceService;

    private final DataInstanceService dataInstanceService;

    private final ArchiveService archiveService;

    private final TechnicalLoggerService logger;

    private final QueriableLoggerService queriableLoggerService;

    private final TransitionService transitionService;

    private final ProcessDefinitionService processDefinitionService;

    private final ConnectorInstanceService connectorInstanceService;

    private final ClassLoaderService classLoaderService;

    private final ProcessDocumentService processDocumentService;

    private final SCommentService commentService;

    private final SCommentBuilders commentBuilders;

    private final TokenService tokenService;

    public ProcessInstanceServiceImpl(final Recorder recorder, final ReadPersistenceService persistenceRead, final EventService eventService,
            final ActivityInstanceService activityService, final TechnicalLoggerService logger, final BPMInstanceBuilders bpmInstanceBuilders,
            final EventInstanceService bpmEventInstanceService, final DataInstanceService dataInstanceService, final ArchiveService archiveService,
            final QueriableLoggerService queriableLoggerService, final TransitionService transitionService,
            final ProcessDefinitionService processDefinitionService, final ConnectorInstanceService connectorInstanceService,
            final ClassLoaderService classLoaderService, final ProcessDocumentService processDocumentService, final SCommentService commentService,
            final SCommentBuilders commentBuilders, final TokenService tokenService) {
        this.recorder = recorder;
        this.persistenceRead = persistenceRead;
        this.eventService = eventService;
        this.activityService = activityService;
        this.bpmInstanceBuilders = bpmInstanceBuilders;
        this.transitionService = transitionService;
        this.processDefinitionService = processDefinitionService;
        this.connectorInstanceService = connectorInstanceService;
        this.classLoaderService = classLoaderService;
        this.processDocumentService = processDocumentService;
        this.commentService = commentService;
        this.commentBuilders = commentBuilders;
        this.tokenService = tokenService;
        processInstanceKeyProvider = bpmInstanceBuilders.getSProcessInstanceBuilder();
        this.bpmEventInstanceService = bpmEventInstanceService;
        this.dataInstanceService = dataInstanceService;
        this.archiveService = archiveService;
        this.logger = logger;
        this.queriableLoggerService = queriableLoggerService;
    }

    private ProcessInstanceLogBuilder getQueriableLog(final ActionType actionType, final String message) {
        final ProcessInstanceLogBuilder logBuilder = bpmInstanceBuilders.getProcessInstanceLogBuilder();
        initializeLogBuilder(logBuilder, message);
        updateLog(actionType, logBuilder);
        return logBuilder;
    }

    @Override
    public void createProcessInstance(final SProcessInstance processInstance) throws SProcessInstanceCreationException {
        final ProcessInstanceLogBuilder logBuilder = getQueriableLog(ActionType.CREATED, "Creating a new Process Instance");
        final InsertRecord insertRecord = new InsertRecord(processInstance);
        SInsertEvent insertEvent = null;
        if (eventService.hasHandlers(PROCESSINSTANCE, EventActionType.CREATED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            insertEvent = (SInsertEvent) eventBuilder.createInsertEvent(PROCESSINSTANCE).setObject(processInstance).done();
        }
        try {
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(processInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "createProcessInstance");
            setProcessState(processInstance, ProcessInstanceState.INITIALIZING);
        } catch (final SRecorderException sre) {
            initiateLogBuilder(processInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "createProcessInstance");
            throw new SProcessInstanceCreationException(sre);
        } catch (final SProcessInstanceModificationException spicme) {
            throw new SProcessInstanceCreationException(spicme);
        }
    }

    @Override
    public SProcessInstance getProcessInstance(final long processInstanceId) throws SProcessInstanceReadException, SProcessInstanceNotFoundException {
        SProcessInstance instance;
        try {
            instance = persistenceRead.selectById(SelectDescriptorBuilder.getElementById(SProcessInstance.class, "ProcessInstance", processInstanceId));
        } catch (final SBonitaReadException sbre) {
            throw new SProcessInstanceReadException(sbre);
        }
        if (instance == null) {
            throw new SProcessInstanceNotFoundException(processInstanceId);
        }
        return instance;
    }

    @Override
    public void deleteProcessInstance(final long processInstanceId) throws SProcessInstanceModificationException, SProcessInstanceReadException,
            SProcessInstanceNotFoundException {
        final SProcessInstance processInstance = getProcessInstance(processInstanceId);
        deleteProcessInstance(processInstance);
    }

    @Override
    public long deleteParentProcessInstanceAndElements(final List<SProcessInstance> sProcessInstances) {
        long nbDeleted = 0;
        for (final SProcessInstance sProcessInstance : sProcessInstances) {
            try {
                deleteParentProcessInstanceAndElements(sProcessInstance);
                nbDeleted = +1;
            } catch (SBonitaException e) {
                if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                    logger.log(this.getClass(), TechnicalLogSeverity.WARNING, e.getMessage() + ". It has probably completed.");
                }
            }
        }
        return nbDeleted;
    }

    @Override
    public void deleteParentProcessInstanceAndElements(final long processInstanceId) throws SProcessInstanceReadException, SProcessInstanceNotFoundException,
            SFlowNodeReadException, SProcessInstanceHierarchicalDeletionException, SProcessInstanceModificationException {
        final SProcessInstance sProcessInstance = getProcessInstance(processInstanceId);
        deleteParentProcessInstanceAndElements(sProcessInstance);
    }

    protected void deleteParentProcessInstanceAndElements(final SProcessInstance sProcessInstance) throws SFlowNodeReadException,
            SProcessInstanceHierarchicalDeletionException, SProcessInstanceModificationException {
        checkIfCallerIsNotActive(sProcessInstance.getCallerId());
        deleteProcessInstance(sProcessInstance);
    }

    @Override
    public long deleteParentArchivedProcessInstancesAndElements(final List<SAProcessInstance> saProcessInstances) {
        long nbDeleted = 0;
        for (final SAProcessInstance saProcessInstance : saProcessInstances) {
            try {
                deleteParentArchivedProcessInstanceAndElements(saProcessInstance);
                nbDeleted = +1;
            } catch (SBonitaException e) {
                if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                    logger.log(this.getClass(), TechnicalLogSeverity.WARNING, e.getMessage());
                }
            }
        }
        return nbDeleted;
    }

    private void deleteParentArchivedProcessInstanceAndElements(final SAProcessInstance saProcessInstance) throws SFlowNodeReadException,
            SProcessInstanceHierarchicalDeletionException, SProcessInstanceModificationException {
        checkIfCallerIsNotActive(saProcessInstance.getCallerId());
        deleteArchivedProcessInstanceElements(saProcessInstance.getSourceObjectId(), saProcessInstance.getProcessDefinitionId());
        deleteArchivedProcessInstance(saProcessInstance);
    }

    @Override
    public void deleteArchivedProcessInstance(final SAProcessInstance archivedProcessInstance) throws SProcessInstanceModificationException {
        final DeleteRecord deleteRecord = new DeleteRecord(archivedProcessInstance);
        final ProcessInstanceLogBuilder logBuilder = getQueriableLog(ActionType.DELETED, "deleting process instance");
        SDeleteEvent deleteEvent = null;
        if (eventService.hasHandlers(PROCESSINSTANCE, EventActionType.DELETED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(PROCESSINSTANCE).setObject(archivedProcessInstance).done();
        }
        try {
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(archivedProcessInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "deleteArchivedProcessInstance");
        } catch (final SRecorderException e) {
            initiateLogBuilder(archivedProcessInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "deleteArchivedProcessInstance");
            throw new SProcessInstanceModificationException(e);
        }
    }

    @Override
    public void deleteArchivedProcessInstanceElements(final long processInstanceId, final long processDefinitionId)
            throws SProcessInstanceModificationException {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ClassLoader localClassLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);
            Thread.currentThread().setContextClassLoader(localClassLoader);
            deleteArchivedFlowNodeInstances(processInstanceId);
            dataInstanceService.deleteLocalArchivedDataInstances(processInstanceId, DataInstanceContainer.PROCESS_INSTANCE.toString());
            processDocumentService.deleteArchivedDocuments(processInstanceId);
            connectorInstanceService.deleteArchivedConnectorInstances(processInstanceId, SConnectorInstance.PROCESS_TYPE);
            transitionService.deleteArchivedTransitionsOfProcessInstance(processInstanceId);
            commentService.deleteArchivedComments(processInstanceId);
            deleteArchivedChidrenProcessInstanceElements(processInstanceId, processDefinitionId);
        } catch (final SBonitaException e) {
            throw new SProcessInstanceModificationException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private void deleteArchivedChidrenProcessInstanceElements(final long processInstanceId, final long processDefinitionId) throws SBonitaException {
        List<Long> childrenProcessInstanceIds = null;
        do {
            // from index always will be zero because elements will be deleted
            childrenProcessInstanceIds = getArchivedChildrenSourceObjectIdsFromRootProcessInstance(processInstanceId, 0, BATCH_SIZE, OrderByType.ASC);
            for (final Long childProcessInstanceId : childrenProcessInstanceIds) {
                deleteArchivedProcessInstanceElements(childProcessInstanceId, processDefinitionId);
                deleteArchivedProcessInstancesOfProcessInstance(childProcessInstanceId);
            }
        } while (!childrenProcessInstanceIds.isEmpty());
    }

    private void deleteArchivedFlowNodeInstances(final long processInstanceId) throws SFlowNodeReadException, SBonitaSearchException,
            SConnectorInstanceDeletionException, SFlowNodeDeletionException, SDataInstanceException {
        List<SAFlowNodeInstance> activityInstances;
        do {
            activityInstances = activityService.getArchivedFlowNodeInstances(processInstanceId, 0, BATCH_SIZE);
            final HashSet<Long> orgActivityIds = new HashSet<Long>();
            final ArrayList<SAFlowNodeInstance> orgActivities = new ArrayList<SAFlowNodeInstance>();
            for (final SAFlowNodeInstance activityInstance : activityInstances) {
                if (!orgActivityIds.contains(activityInstance.getSourceObjectId())) {
                    orgActivityIds.add(activityInstance.getSourceObjectId());
                    orgActivities.add(activityInstance);
                }
                activityService.deleteArchivedFlowNodeInstance(activityInstance);
            }
            for (final SAFlowNodeInstance orgActivity : orgActivities) {
                deleteArchivedFlowNodeInstanceElements(orgActivity);
            }
        } while (!activityInstances.isEmpty());
    }

    private void deleteArchivedFlowNodeInstanceElements(final SAFlowNodeInstance activityInstance) throws SFlowNodeReadException,
            SBonitaSearchException, SConnectorInstanceDeletionException, SDataInstanceException {
        if (activityInstance instanceof SAActivityInstance) {
            dataInstanceService.deleteLocalArchivedDataInstances(activityInstance.getSourceObjectId(), DataInstanceContainer.ACTIVITY_INSTANCE.toString());
            connectorInstanceService.deleteArchivedConnectorInstances(activityInstance.getSourceObjectId(), SConnectorInstance.FLOWNODE_TYPE);
            if (SFlowNodeType.USER_TASK.equals(activityInstance.getType()) || SFlowNodeType.MANUAL_TASK.equals(activityInstance.getType())) {
                try {
                    activityService.deleteArchivedPendingMappings(activityInstance.getSourceObjectId());
                } catch (final SActivityModificationException e) {
                    throw new SFlowNodeReadException(e);
                }
            }
        }
    }

    @Override
    public List<Long> getSourceProcesInstanceIdsOfArchProcessInstancesFromDefinition(final long processDefinitionId, final int fromIndex, final int maxResults,
            final OrderByType sortingOrder) throws SProcessInstanceReadException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final SACommentBuilder archCommentKeyProvider = commentBuilders.getSACommentBuilder();
        final QueryOptions queryOptions = new QueryOptions(fromIndex, maxResults, SAProcessInstance.class, archCommentKeyProvider.getSourceObjectId(),
                sortingOrder);
        try {
            return persistenceService.selectList(SelectDescriptorBuilder.getSourceProcesInstanceIdsOfArchProcessInstancesFromDefinition(processDefinitionId,
                    queryOptions));
        } catch (final SBonitaReadException e) {
            throw new SProcessInstanceReadException(e);
        }
    }

    @Override
    public void deleteProcessInstance(final SProcessInstance sProcessInstance) throws SProcessInstanceModificationException {
        final ProcessInstanceLogBuilder logBuilder = getQueriableLog(ActionType.DELETED, "deleting process instance");
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long processDefinitionId = sProcessInstance.getProcessDefinitionId();
            final ClassLoader localClassLoader = classLoaderService.getLocalClassLoader("process", processDefinitionId);
            Thread.currentThread().setContextClassLoader(localClassLoader);
            deleteProcessInstanceElements(sProcessInstance);
            final DeleteRecord deleteRecord = new DeleteRecord(sProcessInstance);
            SDeleteEvent deleteEvent = null;
            if (eventService.hasHandlers(PROCESSINSTANCE, EventActionType.DELETED)) {
                final SEventBuilder eventBuilder = eventService.getEventBuilder();
                deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(PROCESSINSTANCE).setObject(sProcessInstance).done();
            }
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(sProcessInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "deleteProcessInstance");
        } catch (final SBonitaException e) {
            initiateLogBuilder(sProcessInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "deleteProcessInstance");
            throw new SProcessInstanceModificationException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private void checkIfCallerIsNotActive(final long callerId) throws SFlowNodeReadException, SProcessInstanceHierarchicalDeletionException {
        if (callerId > 0) {
            try {
                final SFlowNodeInstance flowNodeInstance = activityService.getFlowNodeInstance(callerId);
                throw new SProcessInstanceHierarchicalDeletionException("Unable to delete the process instance because the parent is still active: activity "
                        + flowNodeInstance.getName() + " with id " + flowNodeInstance.getId(), flowNodeInstance.getRootProcessInstanceId());
            } catch (final SFlowNodeNotFoundException e) {
                // ok the activity that called this process do not exists anymore
            }
        }
    }

    private void deleteProcessInstanceElements(final SProcessInstance processInstance) throws SBonitaException {
        SProcessDefinition processDefinition = null;
        try {
            processDefinition = processDefinitionService.getProcessDefinition(processInstance.getProcessDefinitionId());
        } catch (final SProcessDefinitionNotFoundException e) {
            // delete anyway
        }
        try {
            tokenService.deleteTokens(processInstance.getId());
        } catch (final SObjectReadException e) {
            throw new SProcessInstanceModificationException(e);
        } catch (final SObjectModificationException e) {
            throw new SProcessInstanceModificationException(e);
        }
        deleteFlowNodeInstances(processInstance.getId(), processDefinition);
        deleteDataInstancesIfNecessary(processInstance, processDefinition);
        processDocumentService.deleteDocumentsFromProcessInstance(processInstance.getId());
        deleteConnectorInstancesIfNecessary(processInstance, processDefinition);
        commentService.deleteComments(processInstance.getId());
    }

    private void deleteConnectorInstancesIfNecessary(final SProcessInstance processInstance, final SProcessDefinition processDefinition)
            throws SBonitaSearchException, SConnectorInstanceDeletionException {
        if (processDefinition != null && processDefinition.hasConnectors()) {
            connectorInstanceService.deleteConnectors(processInstance.getId(), SConnectorInstance.PROCESS_TYPE);
        }
    }

    private void deleteConnectorInstancesIfNecessary(final SFlowNodeInstance flowNodeInstance, final SProcessDefinition processDefinition)
            throws SBonitaSearchException, SConnectorInstanceDeletionException {
        if (hasConnectors(flowNodeInstance, processDefinition)) {
            connectorInstanceService.deleteConnectors(flowNodeInstance.getId(), SConnectorInstance.FLOWNODE_TYPE);
        }
    }

    private boolean hasConnectors(final SFlowNodeInstance flowNodeInstance, final SProcessDefinition processDefinition) {
        boolean hasConnectors = false;
        if (processDefinition != null) {
            final SActivityDefinition activityDefinition = (SActivityDefinition) processDefinition.getProcessContainer().getFlowNode(
                    flowNodeInstance.getFlowNodeDefinitionId());
            if (activityDefinition != null) {
                hasConnectors = activityDefinition.getConnectors().size() > 0;
            }
        }
        return hasConnectors;
    }

    private void deleteDataInstancesIfNecessary(final SProcessInstance processInstance, final SProcessDefinition processDefinition)
            throws SDataInstanceException {
        boolean dataPresent = true;
        if (processDefinition != null) {
            dataPresent = processDefinition.getProcessContainer().getDataDefinitions().size() > 0;
        }
        dataInstanceService.deleteLocalDataInstances(processInstance.getId(), DataInstanceContainer.PROCESS_INSTANCE.toString(), dataPresent);
    }

    private void deleteFlowNodeInstances(final long processInstanceId, final SProcessDefinition processDefinition) throws SFlowNodeReadException,
            SProcessInstanceModificationException {
        List<SFlowNodeInstance> activityInstances;
        do {
            activityInstances = activityService.getFlowNodeInstances(processInstanceId, 0, BATCH_SIZE);
            for (final SFlowNodeInstance activityInstance : activityInstances) {
                deleteFlowNodeInstance(activityInstance, processDefinition);
            }
        } while (!activityInstances.isEmpty());
    }

    @Override
    public void deleteArchivedProcessInstancesOfProcessInstance(final long processInstanceId) throws SBonitaException {
        final int fromIndex = 0;
        final int maxResults = 100;
        final SAProcessInstanceBuilder archProcInstKeyProvider = bpmInstanceBuilders.getSAProcessInstanceBuilder();

        List<SAProcessInstance> archProcInstances = null;
        do {
            // fromIndex variable is not updated because the elements will be deleted, so we always need to start from zero;
            final SAProcessInstanceBuilder processInstanceBuilder = bpmInstanceBuilders.getSAProcessInstanceBuilder();
            final FilterOption filterOption = new FilterOption(SAProcessInstance.class, processInstanceBuilder.getSourceObjectIdKey(), processInstanceId);
            final OrderByOption orderBy = new OrderByOption(SAProcessInstance.class, archProcInstKeyProvider.getIdKey(), OrderByType.ASC);
            final QueryOptions queryOptions = new QueryOptions(fromIndex, maxResults, Collections.singletonList(orderBy),
                    Collections.singletonList(filterOption), null);
            archProcInstances = searchArchivedProcessInstances(queryOptions);
            for (final SAProcessInstance archProcInstance : archProcInstances) {
                deleteArchivedProcessInstance(archProcInstance);
            }
        } while (!archProcInstances.isEmpty()); // never will be null as the persistence service sends an empty list if there are no results
    }

    @Override
    public void deleteFlowNodeInstance(final SFlowNodeInstance flowNodeInstance, final SProcessDefinition processDefinition)
            throws SProcessInstanceModificationException {
        try {
            deleteFlowNodeInstanceElements(flowNodeInstance, processDefinition);
            activityService.deleteFlowNodeInstance(flowNodeInstance);
        } catch (final SBonitaException e) {
            throw new SProcessInstanceModificationException(e);
        }
    }

    private void deleteFlowNodeInstanceElements(final SFlowNodeInstance flowNodeInstance, final SProcessDefinition processDefinition) throws SBonitaException {
        if (flowNodeInstance.getType().equals(SFlowNodeType.INTERMEDIATE_CATCH_EVENT)) {
            bpmEventInstanceService.deleteWaitingEvents(flowNodeInstance);
        }
        if (flowNodeInstance instanceof SEventInstance) {
            bpmEventInstanceService.deleteEventTriggerInstances(flowNodeInstance.getId());
        } else if (flowNodeInstance instanceof SActivityInstance) {
            deleteDataInstancesIfNecessary(flowNodeInstance, processDefinition);
            deleteConnectorInstancesIfNecessary(flowNodeInstance, processDefinition);
            if (SFlowNodeType.USER_TASK.equals(flowNodeInstance.getType()) || SFlowNodeType.MANUAL_TASK.equals(flowNodeInstance.getType())) {
                activityService.deleteHiddenTasksForActivity(flowNodeInstance.getId());
                try {
                    activityService.deletePendingMappings(flowNodeInstance.getId());
                } catch (final SActivityModificationException e) {
                    throw new SFlowNodeReadException(e);
                }
            } else if (SFlowNodeType.CALL_ACTIVITY.equals(flowNodeInstance.getType()) || SFlowNodeType.SUB_PROCESS.equals(flowNodeInstance.getType())) {
                // in the case of a call activity or subprocess activity delete the child process instance
                try {
                    deleteProcessInstance(getChildOfActivity(flowNodeInstance.getId()));
                } catch (final SProcessInstanceNotFoundException e) {
                    final StringBuilder stb = new StringBuilder();
                    stb.append("Can't find the process instance called by the activity [id: ");
                    stb.append(flowNodeInstance.getId());
                    stb.append(", name: ");
                    stb.append(flowNodeInstance.getName());
                    stb.append("]. This process may be already finished");
                    // if the child process is not found, it's because it has already finished and archived or it was not created
                    logger.log(getClass(), TechnicalLogSeverity.DEBUG, stb.toString());
                    logger.log(getClass(), TechnicalLogSeverity.DEBUG, e);
                }
            }
        }
    }

    private void deleteDataInstancesIfNecessary(final SFlowNodeInstance flowNodeInstance, final SProcessDefinition processDefinition)
            throws SDataInstanceException {
        boolean dataPresent = true;
        if (processDefinition != null) {
            final SActivityDefinition activityDefinition = (SActivityDefinition) processDefinition.getProcessContainer().getFlowNode(
                    flowNodeInstance.getFlowNodeDefinitionId());
            if (activityDefinition != null) {
                dataPresent = activityDefinition.getSDataDefinitions().size() > 0;
            }
        }
        dataInstanceService.deleteLocalDataInstances(flowNodeInstance.getId(), DataInstanceContainer.ACTIVITY_INSTANCE.toString(), dataPresent);
    }

    @Override
    public void setState(final SProcessInstance processInstance, final ProcessInstanceState state) throws SProcessInstanceModificationException {
        // Let's archive the process instance before changing the state (to keep a track of state change):
        archiveProcessInstance(processInstance);
        setProcessState(processInstance, state);
        if (logger.isLoggable(getClass(), TechnicalLogSeverity.DEBUG)) {
            logger.log(getClass(), TechnicalLogSeverity.DEBUG, MessageFormat.format("[{0} with id {1}]{2}->{3}(new={4})", processInstance.getClass()
                    .getSimpleName(), processInstance.getId(), processInstance.getStateId(), state.getId(), state.getClass().getSimpleName()));
        }
    }

    @Override
    public void setMigrationPlanId(final SProcessInstance processInstance, final long migrationPlanId) throws SProcessInstanceModificationException {
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(processInstanceKeyProvider.getMigrationPlanIdKey(), migrationPlanId);
        final long now = System.currentTimeMillis();
        descriptor.addField(processInstanceKeyProvider.getLastUpdateKey(), now);
        updateProcessInstance(processInstance, "set migration plan", descriptor, MIGRATION_PLAN);
    }

    private void setProcessState(final SProcessInstance processInstance, final ProcessInstanceState state) throws SProcessInstanceModificationException {
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(processInstanceKeyProvider.getStateIdKey(), state.getId());
        final long now = System.currentTimeMillis();
        switch (state) {
            case COMPLETED:
                descriptor.addField(processInstanceKeyProvider.getEndDateKey(), now);
                break;
            case ABORTED:
                descriptor.addField(processInstanceKeyProvider.getEndDateKey(), now);
                break;
            case CANCELLED:
                descriptor.addField(processInstanceKeyProvider.getEndDateKey(), now);
                break;
            case STARTED:
                descriptor.addField(processInstanceKeyProvider.getStartDateKey(), now);
                break;
            default:
                break;
        }
        descriptor.addField(processInstanceKeyProvider.getLastUpdateKey(), now);
        updateProcessInstance(processInstance, "updating process instance state", descriptor, PROCESSINSTANCE_STATE);
    }

    private void updateProcessInstance(final SProcessInstance processInstance, final String message, final EntityUpdateDescriptor descriptor,
            final String eventType) throws SProcessInstanceModificationException {
        final ProcessInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, message);
        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(processInstance, descriptor);
        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(eventType, EventActionType.UPDATED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            updateEvent = (SUpdateEvent) eventBuilder.createUpdateEvent(eventType).setObject(processInstance).done();
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(processInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateProcessInstance");
        } catch (final SRecorderException e) {
            initiateLogBuilder(processInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateProcessInstance");
            throw new SProcessInstanceModificationException(e);
        }
    }

    private void archiveProcessInstance(final SProcessInstance processInstance) throws SProcessInstanceModificationException {
        final SAProcessInstance saProcessInstance = bpmInstanceBuilders.getSAProcessInstanceBuilder().createNewInstance(processInstance).done();
        final ArchiveInsertRecord insertRecord = new ArchiveInsertRecord(saProcessInstance);
        try {
            archiveService.recordInsert(System.currentTimeMillis(), insertRecord, getQueriableLog(ActionType.CREATED, "archive the process instance").done());
        } catch (final SRecorderException e) {
            throw new SProcessInstanceModificationException(e);
        } catch (final SDefinitiveArchiveNotFound e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                logger.log(this.getClass(), TechnicalLogSeverity.WARNING, "The process instance was not archived. Id:" + processInstance.getId(), e);
            }
        }
    }

    @Override
    public void setStateCategory(final SProcessInstance processInstance, final SStateCategory stateCatetory) throws SProcessInstanceModificationException {
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(processInstanceKeyProvider.getStateCategoryKey(), stateCatetory);
        updateProcessInstance(processInstance, "update process instance state category", descriptor, PROCESS_INSTANCE_CATEGORY_STATE);
    }

    private <T extends SLogBuilder> void initializeLogBuilder(final T logBuilder, final String message) {
        logBuilder.createNewInstance().actionStatus(SQueriableLog.STATUS_FAIL).severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    private <T extends HasCRUDEAction> void updateLog(final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    @Override
    public List<Long> getChildInstanceIdsOfProcessInstance(final long processInstanceId, final int fromIndex, final int maxResults, final String sortingField,
            final OrderByType sortingOrder) throws SProcessInstanceReadException {
        try {
            final QueryOptions queryOptions = new QueryOptions(fromIndex, maxResults, SProcessInstance.class, sortingField, sortingOrder);
            final SelectListDescriptor<Long> elements = SelectDescriptorBuilder.getChildInstanceIdsOfProcessInstance(SProcessInstance.class, processInstanceId,
                    queryOptions);
            return persistenceRead.selectList(elements);
        } catch (final SBonitaReadException e) {
            throw new SProcessInstanceReadException(e);
        }
    }

    @Override
    public SProcessInstance getChildOfActivity(final long activityInstId) throws SProcessInstanceNotFoundException, SBonitaSearchException {
        try {
            final SProcessInstanceBuilder processInstanceBuilder = bpmInstanceBuilders.getSProcessInstanceBuilder();
            final FilterOption filterOption = new FilterOption(SProcessInstance.class, processInstanceBuilder.getCallerIdKey(), activityInstId);
            final QueryOptions queryOptions = new QueryOptions(0, QueryOptions.UNLIMITED_NUMBER_OF_RESULTS, null, Collections.singletonList(filterOption), null);
            return searchProcessInstances(queryOptions).get(0);
        } catch (final IndexOutOfBoundsException e) {
            throw new SProcessInstanceNotFoundException("No process instance was found as child of activity: " + activityInstId);
        }
    }

    @Override
    public long getNumberOfChildInstancesOfProcessInstance(final long processInstanceId) throws SProcessInstanceReadException {
        try {
            return persistenceRead.selectOne(SelectDescriptorBuilder.getNumberOfChildInstancesOfProcessInstance(processInstanceId));
        } catch (final SBonitaReadException e) {
            throw new SProcessInstanceReadException(e);
        }
    }

    @Override
    public long getNumberOfProcessInstances(final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            return persistenceRead.getNumberOfEntities(SProcessInstance.class, queryOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SProcessInstance> searchProcessInstances(final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            return persistenceRead.searchEntity(SProcessInstance.class, queryOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public long getNumberOfOpenProcessInstancesSupervisedBy(final long userId, final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap(USER_ID, (Object) userId);
            return persistenceRead.getNumberOfEntities(SProcessInstance.class, SUPERVISED_BY, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SProcessInstance> searchOpenProcessInstancesSupervisedBy(final long userId, final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap(USER_ID, (Object) userId);
            return persistenceRead.searchEntity(SProcessInstance.class, SUPERVISED_BY, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public long getNumberOfOpenProcessInstancesInvolvingUser(final long userId, final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(1);
            parameters.put(USER_ID, userId);
            return persistenceRead.getNumberOfEntities(SProcessInstance.class, INVOLVING_USER, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SProcessInstance> searchOpenProcessInstancesInvolvingUser(final long userId, final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(1);
            parameters.put(USER_ID, userId);
            return persistenceRead.searchEntity(SProcessInstance.class, INVOLVING_USER, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public long getNumberOfOpenProcessInstancesInvolvingUsersManagedBy(final long managerUserId, final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(1);
            parameters.put(MANAGER_USER_ID, managerUserId);
            return persistenceRead.getNumberOfEntities(SProcessInstance.class, INVOLVING_USER + "s" + MANAGED_BY, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SProcessInstance> searchOpenProcessInstancesInvolvingUsersManagedBy(final long managerUserId, final QueryOptions queryOptions)
            throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(1);
            parameters.put(MANAGER_USER_ID, managerUserId);
            return persistenceRead.searchEntity(SProcessInstance.class, INVOLVING_USER + "s" + MANAGED_BY, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public long getNumberOfArchivedProcessInstancesWithoutSubProcess(final QueryOptions queryOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            return persistenceService.getNumberOfEntities(SAProcessInstance.class, "WithoutSubProcess", queryOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SAProcessInstance> searchArchivedProcessInstancesWithoutSubProcess(final QueryOptions queryOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            return persistenceService.searchEntity(SAProcessInstance.class, "WithoutSubProcess", queryOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SAProcessInstance> searchArchivedProcessInstancesSupervisedBy(final long userId, final QueryOptions queryOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final Map<String, Object> parameters = Collections.singletonMap(USER_ID, (Object) userId);
        try {
            return persistenceService.searchEntity(SAProcessInstance.class, SUPERVISED_BY, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public long getNumberOfArchivedProcessInstancesSupervisedBy(final long userId, final QueryOptions countOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final Map<String, Object> parameters = Collections.singletonMap(USER_ID, (Object) userId);
        try {
            return persistenceService.getNumberOfEntities(SAProcessInstance.class, SUPERVISED_BY, countOptions, parameters);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfArchivedProcessInstancesInvolvingUser(final long userId, final QueryOptions countOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(2);
            parameters.put(USER_ID, userId);
            return persistenceService.getNumberOfEntities(SAProcessInstance.class, INVOLVING_USER, countOptions, parameters);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfArchivedProcessInstances(final QueryOptions queryOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            return persistenceService.getNumberOfEntities(SAProcessInstance.class, queryOptions, null);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SAProcessInstance> searchArchivedProcessInstances(final QueryOptions queryOptions) throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            return persistenceService.searchEntity(SAProcessInstance.class, queryOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SAProcessInstance> searchArchivedProcessInstancesInvolvingUser(final long userId, final QueryOptions queryOptions)
            throws SBonitaSearchException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(1);
            parameters.put(USER_ID, userId);
            return persistenceService.searchEntity(SAProcessInstance.class, INVOLVING_USER, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public void updateProcess(final SProcessInstance processInstance, final EntityUpdateDescriptor descriptor) throws SProcessInstanceModificationException {
        final ProcessInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "process instance is updated");
        try {
            final UpdateRecord updateRecord = UpdateRecord.buildSetFields(processInstance, descriptor);
            SUpdateEvent updateEvent = null;
            if (eventService.hasHandlers(PROCESSINSTANCE, EventActionType.UPDATED)) {
                updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(PROCESSINSTANCE).setObject(processInstance).done();
            }
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(processInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateProcess");
        } catch (final SRecorderException e) {
            initiateLogBuilder(processInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateProcess");
            throw new SProcessInstanceModificationException(e);
        }
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
    public SAProcessInstance getArchivedProcessInstance(final long archivedProcessInstanceId) throws SProcessInstanceReadException,
            SProcessInstanceNotFoundException {
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        try {
            final Map<String, Object> parameters = Collections.singletonMap("id", (Object) archivedProcessInstanceId);
            final SAProcessInstance saProcessInstance = persistenceService.selectOne(new SelectOneDescriptor<SAProcessInstance>("getArchivedProcessInstance",
                    parameters, SAProcessInstance.class));
            if (saProcessInstance == null) {
                throw new SProcessInstanceNotFoundException(archivedProcessInstanceId);
            }
            return saProcessInstance;
        } catch (final SBonitaReadException e) {
            throw new SProcessInstanceReadException(e);
        }
    }

    @Override
    public List<SProcessInstance> getProcessInstancesInState(final QueryOptions queryOptions, final ProcessInstanceState state)
            throws SProcessInstanceReadException {
        final Map<String, Object> inputParameters = new HashMap<String, Object>(1);
        inputParameters.put("state", state.getId());
        final SelectListDescriptor<SProcessInstance> selectListDescriptor = new SelectListDescriptor<SProcessInstance>("getProcessInstancesInState",
                inputParameters, SProcessInstance.class, queryOptions);
        try {
            return persistenceRead.selectList(selectListDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessInstanceReadException(e);
        }
    }

    @Override
    public List<Long> getArchivedChildrenSourceObjectIdsFromRootProcessInstance(final long rootProcessIntanceId, final int fromIndex, final int maxResults,
            final OrderByType sortingOrder) throws SBonitaReadException {
        final Map<String, Object> inputParameters = new HashMap<String, Object>(1);
        inputParameters.put("rootProcessInstanceId", rootProcessIntanceId);
        final QueryOptions queryOptions = new QueryOptions(fromIndex, maxResults, SAProcessInstance.class, "sourceObjectId", sortingOrder);
        final SelectListDescriptor<Long> selectListDescriptor = new SelectListDescriptor<Long>("getChildrenSourceProcessInstanceIdsFromRootProcessInstance",
                inputParameters, SAProcessInstance.class, queryOptions);
        return persistenceRead.selectList(selectListDescriptor);
    }

}
