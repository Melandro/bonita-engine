package org.bonitasoft.engine.bpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.bpm.process.ProcessInstanceState;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceCreationException;
import org.bonitasoft.engine.core.process.instance.model.SActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.builder.SProcessInstanceBuilder;
import org.bonitasoft.engine.data.definition.model.builder.SDataDefinitionBuilder;
import org.bonitasoft.engine.data.instance.api.DataInstanceContainer;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.data.instance.exception.SDataInstanceNotFoundException;
import org.bonitasoft.engine.data.instance.model.SDataInstance;
import org.bonitasoft.engine.data.instance.model.builder.SDataInstanceBuilder;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.transaction.STransactionCommitException;
import org.bonitasoft.engine.transaction.STransactionCreationException;
import org.bonitasoft.engine.transaction.STransactionRollbackException;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Duchastenier
 * @author Yanyan Liu
 */
public class ProcessInstanceServiceIntegrationTest extends CommonBPMServicesTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(CommonBPMServicesTest.class);

    private static BPMServicesBuilder bpmServicesBuilder;

    private static ProcessInstanceService processInstanceService;

    private static TransactionService transactionService;

    private static DataInstanceService dataInstanceService;

    static {
        bpmServicesBuilder = new BPMServicesBuilder();
        processInstanceService = getProcessInstanceService();
        transactionService = bpmServicesBuilder.getTransactionService();
        dataInstanceService = bpmServicesBuilder.getDataInstanceService();
    }

    static ProcessInstanceService getProcessInstanceService() {
        return bpmServicesBuilder.getProcessInstanceService();
    }

    /**
     * Clean up of all existing process instances
     */
    private void cleanUpAllProcessInstances() {
        try {
            transactionService.begin();
            List<SProcessInstance> processInstances = get100FirstProcessInstances();
            transactionService.complete();

            while (processInstances.size() > 0) {
                transactionService.begin();
                for (final SProcessInstance sProcessInstance : processInstances) {
                    processInstanceService.deleteProcessInstance(sProcessInstance.getId());
                }
                transactionService.complete();

                transactionService.begin();
                // get the next 100:
                processInstances = get100FirstProcessInstances();
                transactionService.complete();
            }

        } catch (final Exception e) {
            LOGGER.error("Error during clean-up. Ignoring...");
        }
    }

    private List<SProcessInstance> get100FirstProcessInstances() throws SBonitaSearchException {
        return getFirstProcessInstances(100);
    }

    @Test
    public void getNumberOfProcessInstances() throws STransactionCreationException, STransactionCommitException, SProcessInstanceCreationException,
            STransactionRollbackException, SBonitaSearchException {
        transactionService.begin();
        final SProcessInstanceBuilder sProcessInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder();
        final long processDefinitionId = 123L;
        SProcessInstance sProcessInstance = sProcessInstanceBuilder.createNewInstance("an instance name", processDefinitionId).done();
        processInstanceService.createProcessInstance(sProcessInstance);
        final long processInstanceNumber = processInstanceService.getNumberOfProcessInstances(null);
        transactionService.complete();

        // first test with one process:
        assertEquals(1, processInstanceNumber);

        transactionService.begin();

        // second test with 100 processes:
        for (int i = 0; i < 100; i++) {
            sProcessInstance = sProcessInstanceBuilder.createNewInstance("process instance " + i, processDefinitionId).done();
            processInstanceService.createProcessInstance(sProcessInstance);
        }
        final long numberOfProcessInstances = processInstanceService.getNumberOfProcessInstances(null);
        transactionService.complete();
        assertEquals(101, numberOfProcessInstances);

        // clean up:
        cleanUpAllProcessInstances();
    }

    @Test
    public void getCorrectProcessInstancesOrder() throws Exception {
        // Creation of the process instances we want to retrieve:
        transactionService.begin();
        final SProcessInstanceBuilder sProcessInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder();
        final long processDefinitionId = 123L;
        final SProcessInstance sProcessInstance0 = sProcessInstanceBuilder.createNewInstance("instance name 0", processDefinitionId).done();
        final SProcessInstance sProcessInstance1 = sProcessInstanceBuilder.createNewInstance("instance name 1", processDefinitionId).done();
        final SProcessInstance sProcessInstance2 = sProcessInstanceBuilder.createNewInstance("instance name 2", processDefinitionId).done();
        processInstanceService.createProcessInstance(sProcessInstance0);
        processInstanceService.setState(sProcessInstance0, ProcessInstanceState.STARTED);
        // to ensure the date is not exactly the same as the previous one:
        Thread.sleep(5);
        processInstanceService.createProcessInstance(sProcessInstance1);
        processInstanceService.setState(sProcessInstance1, ProcessInstanceState.STARTED);
        // to ensure the date is not exactly the same as the previous one:
        Thread.sleep(5);
        processInstanceService.createProcessInstance(sProcessInstance2);
        processInstanceService.setState(sProcessInstance2, ProcessInstanceState.STARTED);
        transactionService.complete();

        // Retrieval of the previously created process instances:
        transactionService.begin();
        final List<SProcessInstance> processInstances = getFirstProcessInstances(20);
        transactionService.complete();

        // Verification of the number of process instances retrieved:
        assertEquals(3, processInstances.size());
        // Verification of the order:
        assertEquals(sProcessInstance0.getId(), processInstances.get(2).getId());
        assertEquals(sProcessInstance1.getId(), processInstances.get(1).getId());
        assertEquals(sProcessInstance2.getId(), processInstances.get(0).getId());

        // clean up:
        cleanUpAllProcessInstances();
    }

    @Test
    public void testSetState() {
        // TODO: not yet implemented
    }

    @Test
    public void testDeleteProcessInstance() throws SBonitaException {
        // Creation of a process instance:
        transactionService.begin();
        final SProcessInstanceBuilder sProcessInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder();
        final long processDefinitionId = 123L;
        final SProcessInstance sProcessInstance = sProcessInstanceBuilder.createNewInstance("an instance name", processDefinitionId).done();
        processInstanceService.createProcessInstance(sProcessInstance);
        transactionService.complete();

        // clean up:
        cleanUpAllProcessInstances();

        // retrieve the number of process instances:
        transactionService.begin();
        final long processInstanceNumber = processInstanceService.getNumberOfProcessInstances(null);
        transactionService.complete();

        // Check that this number is 0:
        assertEquals(0, processInstanceNumber);
    }

    @Test
    public void testGetChildInstanceIdsOfProcessInstance() throws Exception {
        // first create parent process instance
        transactionService.begin();
        final SProcessInstanceBuilder sProcessInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder();
        final long processDefinitionId = 123L;
        final SProcessInstance parentProcessInstance = sProcessInstanceBuilder.createNewInstance("an instance name", processDefinitionId).done();
        processInstanceService.createProcessInstance(parentProcessInstance);
        transactionService.complete();

        transactionService.begin();
        // second create 10 child processes:
        final List<Long> childInstanceIds = new ArrayList<Long>();
        SProcessInstance childProcessInstance;
        for (int i = 0; i < 10; i++) {
            childProcessInstance = sProcessInstanceBuilder.createNewInstance("child process instance " + i, processDefinitionId)
                    .setContainerId(parentProcessInstance.getId()).done();
            processInstanceService.createProcessInstance(childProcessInstance);
            childInstanceIds.add(childProcessInstance.getId());
        }
        transactionService.complete();
        // test get child by paging, order by name ASC
        final String nameField = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder().getNameKey();
        transactionService.begin();
        final List<Long> childInstanceIdList1 = processInstanceService.getChildInstanceIdsOfProcessInstance(parentProcessInstance.getId(), 0, 4, nameField,
                OrderByType.ASC);
        assertEquals(4, childInstanceIdList1.size());
        assertEquals(childInstanceIds.get(0), childInstanceIdList1.get(0));
        assertEquals(childInstanceIds.get(1), childInstanceIdList1.get(1));
        assertEquals(childInstanceIds.get(2), childInstanceIdList1.get(2));
        assertEquals(childInstanceIds.get(3), childInstanceIdList1.get(3));

        final List<Long> childInstanceIdList2 = processInstanceService.getChildInstanceIdsOfProcessInstance(parentProcessInstance.getId(), 4, 4, nameField,
                OrderByType.ASC);
        assertEquals(4, childInstanceIdList2.size());
        assertEquals(childInstanceIds.get(4), childInstanceIdList2.get(0));
        assertEquals(childInstanceIds.get(5), childInstanceIdList2.get(1));
        assertEquals(childInstanceIds.get(6), childInstanceIdList2.get(2));
        assertEquals(childInstanceIds.get(7), childInstanceIdList2.get(3));

        final List<Long> childInstanceIdList3 = processInstanceService.getChildInstanceIdsOfProcessInstance(parentProcessInstance.getId(), 8, 4, nameField,
                OrderByType.ASC);
        assertEquals(2, childInstanceIdList3.size());
        assertEquals(childInstanceIds.get(8), childInstanceIdList3.get(0));
        assertEquals(childInstanceIds.get(9), childInstanceIdList3.get(1));

        // test DESC
        final List<Long> childInstanceIdList4 = processInstanceService.getChildInstanceIdsOfProcessInstance(parentProcessInstance.getId(), 0, 4, nameField,
                OrderByType.DESC);
        assertEquals(4, childInstanceIdList4.size());
        assertEquals(childInstanceIds.get(9), childInstanceIdList4.get(0));
        assertEquals(childInstanceIds.get(8), childInstanceIdList4.get(1));
        assertEquals(childInstanceIds.get(7), childInstanceIdList4.get(2));
        assertEquals(childInstanceIds.get(6), childInstanceIdList4.get(3));

        transactionService.complete();

        // clean up:
        cleanUpAllProcessInstances();
    }

    @Test
    public void testGetNumberOfChildInstancesOfProcessInstance() throws Exception {
        // first create parent process instance and test
        transactionService.begin();
        final SProcessInstanceBuilder sProcessInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder();
        final long processDefinitionId = 123L;
        final SProcessInstance parentProcessInstance = sProcessInstanceBuilder.createNewInstance("an instance name", processDefinitionId).done();
        processInstanceService.createProcessInstance(parentProcessInstance);
        long numberOfChild = processInstanceService.getNumberOfChildInstancesOfProcessInstance(parentProcessInstance.getId());
        transactionService.complete();
        assertEquals(0, numberOfChild);

        transactionService.begin();
        // second create 10 child processes:
        final List<Long> childInstanceIds = new ArrayList<Long>();
        SProcessInstance childProcessInstance;
        for (int i = 0; i < 10; i++) {
            childProcessInstance = sProcessInstanceBuilder.createNewInstance("child process instance " + i, processDefinitionId)
                    .setContainerId(parentProcessInstance.getId()).done();
            processInstanceService.createProcessInstance(childProcessInstance);
            childInstanceIds.add(childProcessInstance.getId());
        }
        numberOfChild = processInstanceService.getNumberOfChildInstancesOfProcessInstance(parentProcessInstance.getId());
        transactionService.complete();
        assertEquals(childInstanceIds.size(), numberOfChild);

        // clean up:
        cleanUpAllProcessInstances();
    }

    @Test
    public void testDeletProcessInstanceAlsoDeleteDataInstances() throws Exception {
        final long processDefinitionId = 123123123L;
        final String processName = "myProcInst";

        // create a process instance
        final SProcessInstance processInstance = createProcessInstanceInTransaction(processDefinitionId, processName);

        // create a data instance having the process instance as container
        final SDataInstance globalDataInstance = createDataInTransaction("myData", String.class.getName(), processInstance.getId(),
                DataInstanceContainer.PROCESS_INSTANCE);

        // create a automatic task
        final SActivityInstance taskInstance = createSAutomaticTaskInstance(bpmServicesBuilder.getBPMInstanceBuilders().getSAutomaticTaskInstanceBuilder(),
                "auto", 1234L, processInstance.getId(), processDefinitionId, processInstance.getId());
        final SDataInstance localDataInstance = createDataInTransaction("myLocalData", String.class.getName(), taskInstance.getId(),
                DataInstanceContainer.ACTIVITY_INSTANCE);

        // delete the process instance: the data instance is supposed to be deleted at same time
        deleteSProcessInstance(processInstance);

        // check that no more data is available for the deleted process instance and flow node instance
        checkDataDoesNotExist(globalDataInstance);
        checkDataDoesNotExist(localDataInstance);
        checkFlowNodeDoesNotExist(taskInstance);

    }

    private void checkDataDoesNotExist(final SDataInstance dataInstance) throws SBonitaException {
        try {
            getDataInstanceInTransaction(dataInstance.getId());
            fail("the data instance was not deleted");
        } catch (final SDataInstanceNotFoundException e) {
            // ok
        }
    }

    private void checkFlowNodeDoesNotExist(final SFlowNodeInstance flowNodeInstance) throws SBonitaException {
        try {
            getFlowNodeInstance(flowNodeInstance.getId());
            fail("the flowNode instance was not deleted");
        } catch (final SFlowNodeNotFoundException e) {
            // ok
        }
    }

    private SProcessInstance createProcessInstanceInTransaction(final long process_definition_id, final String processName)
            throws STransactionCreationException, SProcessInstanceCreationException, STransactionCommitException, STransactionRollbackException {
        getTransactionService().begin();
        final SProcessInstanceBuilder sProcessInstanceBuilder = bpmServicesBuilder.getBPMInstanceBuilders().getSProcessInstanceBuilder();

        // Creation of a process instance:
        final SProcessInstance processInstance = sProcessInstanceBuilder.createNewInstance(processName, process_definition_id).done();
        processInstanceService.createProcessInstance(processInstance);
        getTransactionService().complete();
        return processInstance;
    }

    private SDataInstance createDataInTransaction(final String dataName, final String dataType, final long containerId,
            final DataInstanceContainer containerType) throws SBonitaException {
        getTransactionService().begin();
        final SDataInstanceBuilder dataInstanceBuilder = bpmServicesBuilder.getSDataInstanceBuilders().getDataInstanceBuilder();
        final SDataDefinitionBuilder dataDefBuilder = bpmServicesBuilder.getSDataDefinitionBuilders().getDataDefinitionBuilder();
        dataDefBuilder.createNewInstance(dataName, dataType);

        dataInstanceBuilder.createNewInstance(dataDefBuilder.done());
        dataInstanceBuilder.setContainerId(containerId);
        dataInstanceBuilder.setContainerType(containerType.name());

        final SDataInstance dataInstance = dataInstanceBuilder.done();
        dataInstanceService.createDataInstance(dataInstance);

        getTransactionService().complete();
        return dataInstance;
    }

    private SDataInstance getDataInstanceInTransaction(final long dataInstanceId) throws SBonitaException {
        SDataInstance dataInstance = null;
        getTransactionService().begin();
        try {
            dataInstance = dataInstanceService.getDataInstance(dataInstanceId);
        } finally {
            getTransactionService().complete();
        }
        return dataInstance;
    }

}
