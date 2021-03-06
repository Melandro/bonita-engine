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
package org.bonitasoft.engine.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.BPMRemoteTests;
import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.data.DataNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.CallActivityDefinition;
import org.bonitasoft.engine.bpm.flownode.CallActivityInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceCriterion;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.impl.CallActivityBuilder;
import org.bonitasoft.engine.bpm.process.impl.EndEventDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.connectors.TestConnectorWithOutput;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ProcessInstanceHierarchicalDeletionException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionConstants;
import org.bonitasoft.engine.expression.ExpressionType;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.io.IOUtil;
import org.bonitasoft.engine.operation.LeftOperandBuilder;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.operation.OperatorType;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.test.TestStates;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.bonitasoft.engine.test.check.CheckNbOfProcessInstances;
import org.bonitasoft.engine.test.wait.WaitForActivity;
import org.bonitasoft.engine.test.wait.WaitForFinalArchivedActivity;
import org.bonitasoft.engine.test.wait.WaitForStep;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Elias Ricken de Medeiros
 * @author Celine Souchet
 */
@SuppressWarnings("javadoc")
public class CallActivityTest extends CommonAPITest {

    private User cebolinha;

    private User cascao;

    @Before
    public void beforeTest() throws BonitaException {
        login();
        cebolinha = createUser("cebolinha", "bpm");
        cascao = createUser("cascao", "bpm");
    }

    @After
    public void afterTest() throws BonitaException {
        deleteUser(cebolinha.getId());
        deleteUser(cascao.getId());
        logout();
    }

    private ProcessDefinition getSimpleProcess(final String ACTOR_NAME, final String processName, final String processVersion, final boolean terminateEnd)
            throws BonitaException {
        final Expression clientNumberExpr = new ExpressionBuilder().createConstantIntegerExpression(10);
        final Expression protocolNumberExpr = new ExpressionBuilder().createConstantIntegerExpression(305020);

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance(processName, processVersion);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addShortTextData("firstName", null);
        processDefBuilder.addShortTextData("lastName", null);
        processDefBuilder.addShortTextData("calledProcessData", null);
        processDefBuilder.addIntegerData("clientNumber", clientNumberExpr);
        processDefBuilder.addIntegerData("protocolNumber", protocolNumberExpr);
        processDefBuilder.addStartEvent("tStart");
        processDefBuilder.addUserTask("tStep1", ACTOR_NAME);
        final EndEventDefinitionBuilder endEvent = processDefBuilder.addEndEvent("tEnd");
        if (terminateEnd) {
            endEvent.addTerminateEventTrigger();
        }
        processDefBuilder.addTransition("tStart", "tStep1");
        processDefBuilder.addTransition("tStep1", "tEnd");

        final ProcessDefinition targetProcessDefinition = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cebolinha);

        return targetProcessDefinition;
    }

    private ProcessDefinition getProcessWithCallActivity(final String ACTOR_NAME, final boolean addInputOperations, final boolean addOutputOperations,
            final String processName, final String targetProcessName, final int loopNb, final String strTargetVersion) throws BonitaException {

        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression(targetProcessName);
        Expression targetProcessVersionExpr = null;
        if (strTargetVersion != null) {
            targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(strTargetVersion);
        }
        final Expression expressionTrue = new ExpressionBuilder().createConstantBooleanExpression(true);

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance(processName, PROCESS_VERSION);
        processDefBuilder.addShortTextData("fName", null);
        processDefBuilder.addShortTextData("lName", null);
        processDefBuilder.addIntegerData("cNumber", null);
        processDefBuilder.addIntegerData("pNumber", null);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        final CallActivityBuilder callActivityBuilder = processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr);
        addDataInputOperationsIfNeed(addInputOperations, callActivityBuilder);
        callActivityBuilder.addShortTextData("callActivityData", new ExpressionBuilder().createConstantStringExpression("defaultValue"));
        if (loopNb > 0) {
            callActivityBuilder.addLoop(false, expressionTrue, new ExpressionBuilder().createConstantIntegerExpression(loopNb));
        }
        addDataOutputOperationIfNeed(addOutputOperations, callActivityBuilder);
        processDefBuilder.addUserTask("step1", ACTOR_NAME);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "step1");
        processDefBuilder.addTransition("step1", "end");

        final ProcessDefinition processDefinition = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);

        return processDefinition;
    }

    private void addDataOutputOperationIfNeed(final boolean addOutputOperations, final CallActivityBuilder callActivityBuilder)
            throws InvalidExpressionException {
        if (addOutputOperations) {
            final Operation setClientNumber = buildAssignOperation("cNumber", "clientNumber", ExpressionType.TYPE_VARIABLE, Integer.class.getName());
            final Operation setProtocalNumber = buildAssignOperation("pNumber", "protocolNumber", ExpressionType.TYPE_VARIABLE, Integer.class.getName());
            callActivityBuilder.addDataOutputOperation(setClientNumber);
            callActivityBuilder.addDataOutputOperation(setProtocalNumber);
        }
    }

    private void addDataInputOperationsIfNeed(final boolean addInputOperations, final CallActivityBuilder callActivityBuilder)
            throws InvalidExpressionException {
        if (addInputOperations) {
            final Operation setFirstName = buildAssignOperation("firstName", "fName", ExpressionType.TYPE_VARIABLE, String.class.getName());
            final Operation setLastName = buildAssignOperation("lastName", "lName", ExpressionType.TYPE_VARIABLE, String.class.getName());
            final Operation mapFromCallActivity = buildAssignOperation("calledProcessData", "callActivityData", ExpressionType.TYPE_VARIABLE,
                    String.class.getName());
            callActivityBuilder.addDataInputOperation(setFirstName);
            callActivityBuilder.addDataInputOperation(setLastName);
            callActivityBuilder.addDataInputOperation(mapFromCallActivity);
        }
    }

    /*
     * Most simple case :
     * No Inputs or Outputs for the callActivity
     * See executeCallAtivityUntilEndOfProcess for details.
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void callActivity() throws Exception {
        executeCallAtivityUntilEndOfProcess(false, false, PROCESS_VERSION, false);

    }

    /*
     * Only Inputs for the callActivity
     * See executeCallAtivityUntilEndOfProcess for details.
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.ACTIVITIES, keywords = { "Call Activity" })
    @Test
    public void callActivityWithDataInputOperations() throws Exception {
        executeCallAtivityUntilEndOfProcess(true, false, PROCESS_VERSION, false);
    }

    /*
     * Only Outputs for the callActivity
     * See executeCallAtivityUntilEndOfProcess for details.
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void callActivityWithDataOutputOperations() throws Exception {
        executeCallAtivityUntilEndOfProcess(false, true, PROCESS_VERSION, false);
    }

    /*
     * Only Outputs for the callActivity
     * See executeCallAtivityUntilEndOfProcess for details.
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void callActivityWithDataOutputOperationsAndTerminateEnd() throws Exception {
        executeCallAtivityUntilEndOfProcess(false, true, PROCESS_VERSION, true);
    }

    /*
     * Only Outputs for the callActivity
     * See executeCallAtivityUntilEndOfProcess for details.
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void callActivityWithDataInputAndOutputOperationsAndVersion2() throws Exception {
        executeCallAtivityUntilEndOfProcess(true, true, "2.0", false);
    }

    /*
     * Create a call activity which map some data between child and parent.
     * Execute an operation using one of this data.
     * -> operation must be executed after the data mapping is executed
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, jira = "ENGINE-878", keywords = { "Data mapping with operations execution order" })
    @Test
    public void callActivityWithDataOutputAndOperationAreExecutedInTheGoodOrder() throws Exception {

        final ProcessDefinition targetProcessDef = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);
        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression("targetProcess");
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION);
        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("processWithCallActivity", PROCESS_VERSION);
        processDefBuilder.addIntegerData("cNumber", new ExpressionBuilder().createConstantIntegerExpression(125));
        processDefBuilder.addIntegerData("pNumber", null);
        processDefBuilder.addIntegerData("dataInitWithCNumber", new ExpressionBuilder().createConstantIntegerExpression(12));
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        final CallActivityBuilder callActivityBuilder = processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr);
        addDataOutputOperationIfNeed(true, callActivityBuilder);
        callActivityBuilder.addOperation(new OperationBuilder().createSetDataOperation("dataInitWithCNumber",
                new ExpressionBuilder().createDataExpression("cNumber", Integer.class.getName())));
        processDefBuilder.addUserTask("step1", ACTOR_NAME);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "step1");
        processDefBuilder.addTransition("step1", "end");
        final ProcessDefinition callingProcessDef = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId());

        // execute process until step1
        waitForUserTaskAndExecuteIt("tStep1", callingProcessInstance, cebolinha.getId());
        waitForUserTask("step1", callingProcessInstance);

        /*
         * check the data have the value of the clientNumber of the called process
         * the set of data should be like this:
         * targetProcess:clientNumber(10) -> callingProcess:cNumber(was 125)
         * callingProcess:cNumber(10) -> callingProcess:dataInitWithCNumber(was 12)
         */
        assertEquals(Integer.valueOf(10), getProcessAPI().getProcessDataInstance("dataInitWithCNumber", callingProcessInstance.getId()).getValue());

        disableAndDeleteProcess(callingProcessDef);
        disableAndDeleteProcess(targetProcessDef);
    }

    /*
     * 1 parent process : callingProcess : startEvent(start) -> callActivity -> userTask(step1) -> endEvent(end).
     * 1 called process : targetProcess : startEvent(tStart) -> userTask(tStep1) -> endEvent(tEnd).
     * checks : No instances of a process exist, 2 process instances (callingProcess starts and set off targetProcess of call activity),
     * callingProcess stopped on the call activity, targetProcess is executing, callingProcess is the root process of targetProcess,
     * call activity called the callProcess, callingProcess continues and reaches user task, targetProcess is finished, the user task is
     * the one from the callingProcess, calling Process is finished.
     */
    private void executeCallAtivityUntilEndOfProcess(final boolean addInputOperations, final boolean addOutputOperations, final String strTargetVersion,
            final boolean terminateEnd) throws Exception {
        final ProcessDefinition targetProcessDef1 = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, terminateEnd);
        final ProcessDefinition targetProcessDef3 = getSimpleProcess(ACTOR_NAME, "targetProcess", "3.0", terminateEnd);
        final ProcessDefinition targetProcessDef2 = getSimpleProcess(ACTOR_NAME, "targetProcess", "2.0", terminateEnd);

        final ProcessDefinition callingProcessDef = getProcessWithCallActivity(ACTOR_NAME, addInputOperations, addOutputOperations, "callingProcess",
                "targetProcess", 0, strTargetVersion);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());

        final List<Operation> operations = getStartOperations();
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId(), operations, null);

        if (strTargetVersion != null && strTargetVersion.contentEquals("4.0")) {
            checkNbOfProcessInstances(2, ProcessInstanceCriterion.NAME_DESC);
            final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_DESC);
            assertEquals(1, processInstances.size());
            final WaitForActivity waitForActivity = waitForActivity("callActivity", callingProcessInstance);
            final ActivityInstance callActivityInstance = waitForActivity.getResult();
            assertEquals(TestStates.getFailedState(), callActivityInstance.getState());

            return;
        }

        checkNbOfProcessInstances(2, ProcessInstanceCriterion.NAME_DESC);
        final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_DESC);
        assertEquals(2, processInstances.size()); // two instances are expected calling and target process
        final ProcessInstance targetPI = processInstances.get(0);

        // System.out.println("Version of the definition of targetProcess : "
        // + getProcessAPI().getProcessDefinition(targetPI.getProcessDefinitionId()).getVersion());

        if (strTargetVersion == null || strTargetVersion.contentEquals("2.0")) {
            assertEquals(targetProcessDef2.getId(), targetPI.getProcessDefinitionId());
        } else if (strTargetVersion.contentEquals(PROCESS_VERSION)) {
            assertEquals(targetProcessDef1.getId(), targetPI.getProcessDefinitionId());
        }

        final WaitForActivity waitForActivity = waitForActivity("callActivity", callingProcessInstance);
        final ActivityInstance callActivityInstance = waitForActivity.getResult();
        assertEquals(TestStates.getExecutingState(), callActivityInstance.getState());
        assertEquals(callingProcessInstance.getId(), targetPI.getRootProcessInstanceId());
        assertEquals(callActivityInstance.getId(), targetPI.getCallerId());

        ActivityInstance activityInstance = waitForUserTask("tStep1", callingProcessInstance);
        assertEquals(targetPI.getId(), activityInstance.getParentProcessInstanceId());
        assertEquals(callingProcessInstance.getId(), activityInstance.getRootContainerId());
        checkDataInputOperations(addInputOperations, targetPI);

        // execute step in the target process
        assignAndExecuteStep(activityInstance, cebolinha.getId());

        activityInstance = waitForUserTask("step1", callingProcessInstance);
        checkOutputOperations(addOutputOperations, callingProcessInstance);
        assertTrue("target process was not archived", waitProcessToFinishAndBeArchived(targetPI));
        assertEquals(callingProcessInstance.getId(), activityInstance.getParentProcessInstanceId());

        assignAndExecuteStep(activityInstance, cascao.getId());

        waitForProcessToFinish(callingProcessInstance);
        assertTrue("parent process was not archived", waitProcessToFinishAndBeArchived(callingProcessInstance));

        disableAndDeleteProcess(callingProcessDef);
        disableAndDeleteProcess(targetProcessDef1);
        disableAndDeleteProcess(targetProcessDef2);
        disableAndDeleteProcess(targetProcessDef3);
    }

    private List<Operation> getStartOperations() throws InvalidExpressionException {
        final ArrayList<Operation> operations = new ArrayList<Operation>(2);
        operations.add(buildAssignOperation("fName", "Fulano", ExpressionType.TYPE_CONSTANT, String.class.getName()));
        operations.add(buildAssignOperation("lName", "de Tal", ExpressionType.TYPE_CONSTANT, String.class.getName()));
        return operations;
    }

    private void checkOutputOperations(final boolean addOutputOperations, final ProcessInstance callingProcessInstance) throws Exception {
        if (addOutputOperations) {
            final DataInstance clientNumberData = getProcessAPI().getProcessDataInstance("cNumber", callingProcessInstance.getId());
            final DataInstance protocolNumberData = getProcessAPI().getProcessDataInstance("pNumber", callingProcessInstance.getId());
            assertEquals(10, clientNumberData.getValue());
            assertEquals(305020, protocolNumberData.getValue());
        }
    }

    private void checkDataInputOperations(final boolean addInputOperations, final ProcessInstance targetPI) throws DataNotFoundException {
        if (addInputOperations) {
            final DataInstance firstNameData = getProcessAPI().getProcessDataInstance("firstName", targetPI.getId());
            final DataInstance lastNameData = getProcessAPI().getProcessDataInstance("lastName", targetPI.getId());
            final DataInstance calledProcessData = getProcessAPI().getProcessDataInstance("calledProcessData", targetPI.getId());
            assertEquals("Fulano", firstNameData.getValue());
            assertEquals("de Tal", lastNameData.getValue());
            assertEquals("defaultValue", calledProcessData.getValue());
        }
    }

    private void variableMultiLevelCallActivity(final int nbLevel) throws Exception {

        final ProcessDefinition[] processDefLevels = new ProcessDefinition[nbLevel];

        for (int i = 0; i < nbLevel; i++) {
            if (i != nbLevel - 1) {
                processDefLevels[i] = getProcessWithCallActivity(ACTOR_NAME, false, false, "processLevel" + i, "processLevel" + (i + 1), 0, PROCESS_VERSION);
            } else {
                processDefLevels[i] = getSimpleProcess(ACTOR_NAME, "processLevel" + i, PROCESS_VERSION, false);
            }

        }

        final List<Operation> operations = getStartOperations();

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());
        final ProcessInstance[] procInstLevels = new ProcessInstance[nbLevel];
        procInstLevels[0] = getProcessAPI().startProcess(processDefLevels[0].getId(), operations, null);
        checkNbOfProcessInstances(50, 5000 * nbLevel, nbLevel, ProcessInstanceCriterion.NAME_DESC);
        final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, nbLevel, ProcessInstanceCriterion.NAME_ASC); // if nbLevel>10 use
                                                                                                                                           // CREATION_DATE_ASC
        assertEquals(nbLevel, processInstances.size());

        // for (int i = 0; i < nbLevel; i++) {
        // System.out.println("processInstance of level : " + i + "\nContent : " + processInstances.get(i));
        // }

        for (int i = 0; i < nbLevel; i++) {
            procInstLevels[i] = processInstances.get(i);
            assertEquals("Level of process : " + i, processDefLevels[i].getId(), procInstLevels[i].getProcessDefinitionId());
        }

        for (int i = nbLevel - 1; i >= 0; i--) {
            if (i == nbLevel - 1) {
                waitForStepAndExecuteIt(procInstLevels[0], "tStep1", cebolinha, procInstLevels[i]);
            } else {
                waitForStepAndExecuteIt(procInstLevels[0], "step1", cascao, procInstLevels[i], procInstLevels[i + 1]);
                assertTrue("process of level " + i + " was not archived", waitProcessToFinishAndBeArchived(procInstLevels[i + 1]));
            }
        }

        assertTrue("root process was not archived", waitProcessToFinishAndBeArchived(procInstLevels[0]));

        for (int i = 0; i < nbLevel; i++) {
            disableAndDeleteProcess(processDefLevels[i]);
        }
    }

    /*
     * Tested until 200, works !
     */
    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void multiLevelCallActivity() throws Exception {
        variableMultiLevelCallActivity(10);
    }

    private void waitForStepAndExecuteIt(final ProcessInstance rootProcessInstance, final String userTaskName, final User user,
            final ProcessInstance actualProcessInstance, final ProcessInstance... childProcessInstances) throws Exception {
        final WaitForStep waitForStep = waitForStep(50, 5000, userTaskName, rootProcessInstance, TestStates.getReadyState());
        if (childProcessInstances != null) {
            for (final ProcessInstance childProcessInstance : childProcessInstances) {
                assertTrue("target process was not archived: " + childProcessInstance.getName(), waitProcessToFinishAndBeArchived(childProcessInstance));
            }
        }

        final ActivityInstance activityInstance = waitForStep.getResult();
        assertEquals(rootProcessInstance.getId(), activityInstance.getRootContainerId());
        assertEquals(actualProcessInstance.getId(), activityInstance.getParentProcessInstanceId());
        // execute step in the target process
        assignAndExecuteStep(activityInstance, user.getId());
    }

    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void callUndeployedProcess() throws Exception {

        final ProcessDefinition processDef = getProcessWithCallActivity(ACTOR_NAME, false, false, "callingProcess", "unDeployedProcess", 0, PROCESS_VERSION);
        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDef.getId());
        final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_DESC);
        assertEquals(1, processInstances.size());

        final ActivityInstance waitForTaskToFail = waitForTaskToFail(processInstance);
        assertEquals(TestStates.getFailedState(), waitForTaskToFail.getState());
        disableAndDeleteProcess(processDef);
    }

    private void callActivityInALoop(final int nbLoop) throws Exception {

        final ProcessDefinition targetProcessDef = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);
        final ProcessDefinition callingProcessDef = getProcessWithCallActivity(ACTOR_NAME, false, false, "callingProcess", "targetProcess", nbLoop,
                PROCESS_VERSION);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId(), null, null);

        assertNotNull(waitForFlowNode(callingProcessInstance.getId(), TestStates.getExecutingState(), "callActivity", true, 10000));

        final ProcessInstance[] targetPILoopExecs = new ProcessInstance[nbLoop];

        for (int i = 0; i < nbLoop; i++) {
            targetPILoopExecs[i] = getTargetProcessInstance(targetProcessDef);
            if (i != 0) {
                assertTrue(targetPILoopExecs[i - 1].getId() != targetPILoopExecs[i].getId());
            }
            waitForStepAndExecuteIt(callingProcessInstance, "tStep1", cebolinha, targetPILoopExecs[i]); // i-th loop execution
            assertTrue(waitProcessToFinishAndBeArchived(50, 3000, targetPILoopExecs[i]));
        }

        waitForStepAndExecuteIt(callingProcessInstance, "step1", cascao, callingProcessInstance, targetPILoopExecs[0]);
        assertTrue("parent process was not archived", waitProcessToFinishAndBeArchived(callingProcessInstance));

        disableAndDeleteProcess(callingProcessDef);
        disableAndDeleteProcess(targetProcessDef);
    }

    /*
     * Tested until 200, works !
     * Don't use 0 as argument though.
     */
    @Cover(classes = CallActivityDefinition.class, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Loop" })
    @Test
    public void callActivityInALoop() throws Exception {
        callActivityInALoop(10);
    }

    private ProcessInstance getTargetProcessInstance(final ProcessDefinition targetProcessDef) throws Exception {
        final CheckNbOfProcessInstances checkNbOfProcInst = checkNbOfProcessInstances(2, ProcessInstanceCriterion.NAME_DESC);
        final List<ProcessInstance> processInstances = checkNbOfProcInst.getResult();
        assertEquals(2, processInstances.size()); // two instances are expected calling and target process
        final ProcessInstance targetPI = processInstances.get(0);
        assertEquals(targetProcessDef.getId(), targetPI.getProcessDefinitionId());
        return targetPI;
    }

    @Cover(classes = CallActivityInstance.class, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Archiving" })
    @Test
    public void getArchivedCallActivityInstance() throws Exception {

        final ProcessDefinition targetProcessDef = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);
        final ProcessDefinition callingProcessDef = getProcessWithCallActivity(ACTOR_NAME, false, false, "callingProcess", "targetProcess", 0, PROCESS_VERSION);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId());
        final ProcessInstance targetPI = getTargetProcessInstance(targetProcessDef);

        final WaitForActivity waitForActivity = waitForActivity(50, 3000, "callActivity", callingProcessInstance);
        final ActivityInstance callActivityInstance = waitForActivity.getResult();
        assertEquals(TestStates.getExecutingState(), callActivityInstance.getState());

        waitForStepAndExecuteIt(callingProcessInstance, "tStep1", cebolinha, targetPI); // first loop execution
        waitForProcessToFinish(targetPI);
        assertTrue(waitProcessToFinishAndBeArchived(targetPI));

        final WaitForFinalArchivedActivity waitForFinalArchivedActivity = waitForFinalArchivedActivity("callActivity", callingProcessInstance);
        assertEquals(FlowNodeType.CALL_ACTIVITY, waitForFinalArchivedActivity.getResult().getType());
        assertTrue(waitProcessToFinishAndBeArchived(targetPI));
        final List<ArchivedProcessInstance> archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(targetPI.getId(), 0, 20);

        final ArchivedProcessInstance firstProcessInstanceArchive = archivedProcessInstanceList.get(0);
        assertEquals(callingProcessInstance.getId(), firstProcessInstanceArchive.getRootProcessInstanceId());
        assertEquals(callActivityInstance.getId(), firstProcessInstanceArchive.getCallerId());

        disableAndDeleteProcess(callingProcessDef);
        disableAndDeleteProcess(targetProcessDef);
    }

    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Process Version" })
    @Test
    public void callActivityUsingLatestVersion() throws Exception {
        executeCallAtivityUntilEndOfProcess(false, false, null, false);
    }

    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Process Version" })
    @Test
    public void callActivityUsingInexistingVersion() throws Exception {
        final ProcessDefinition callingProcessDef = getProcessWithCallActivity("delivery", false, false, "callingProcess", "targetProcess", 0,
                "unexisting_version_4.0");

        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId());

        final ActivityInstance failedTask = waitForTaskToFail(callingProcessInstance);
        assertEquals("callActivity", failedTask.getName());

        disableAndDeleteProcess(callingProcessDef);

    }

    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Delete" }, jira = "ENGINE-1132")
    @Test
    public void deleteProcessInstanceThatIsCalledByCallActivity() throws Exception {

        final ProcessDefinition targetProcessDef1 = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);

        final ProcessDefinition callingProcessDef = getProcessWithCallActivity(ACTOR_NAME, false, false, "callingProcess", "targetProcess", 0, PROCESS_VERSION);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());

        final List<Operation> operations = getStartOperations();
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId(), operations, null);
        final ActivityInstance waitForUserTask = waitForUserTask("tStep1", callingProcessInstance.getId());

        try {
            getProcessAPI().deleteProcessInstance(waitForUserTask.getParentProcessInstanceId());
            fail("Should not be able to delete process instance that is called by an other process");
        } catch (final ProcessInstanceHierarchicalDeletionException e) {
            getProcessAPI().deleteProcessInstance(e.getProcessInstanceId());
            // should work now
            try {
                getProcessAPI().getProcessInstance(waitForUserTask.getParentProcessInstanceId());
                fail("process should be deleted");
            } catch (final ProcessInstanceNotFoundException e1) {
                // ok
            }
        } finally {
            disableAndDeleteProcess(callingProcessDef);
            disableAndDeleteProcess(targetProcessDef1);
        }

    }

    @Cover(classes = { CallActivityDefinition.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Delete" }, jira = "ENGINE-1132")
    @Test(expected = DeletionException.class)
    public void deleteProcessDefinitionWithProcessInstanceThatIsCalledByCallActivity() throws Exception {
        final ProcessDefinition targetProcessDef1 = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);
        final ProcessDefinition callingProcessDef = getProcessWithCallActivity(ACTOR_NAME, false, false, "callingProcess", "targetProcess", 0, PROCESS_VERSION);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());

        final List<Operation> operations = getStartOperations();
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId(), operations, null);
        waitForUserTask("tStep1", callingProcessInstance.getId());

        getProcessAPI().disableProcess(targetProcessDef1.getId());
        try {
            deleteProcess(targetProcessDef1);
            fail("Should not be able to delete process instance that is called by an other process");
        } finally {
            disableAndDeleteProcess(callingProcessDef);
            deleteProcess(targetProcessDef1);
        }

    }

    @Cover(classes = CallActivityDefinition.class, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Dependencies" })
    @Test
    public void callActivityWithDependencies() throws Exception {
        final ProcessDefinition targetProcessDef = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);

        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression("targetProcess");

        final Expression vExpression = new ExpressionBuilder().createDataExpression("v", String.class.getName());
        final List<Expression> dependencies = new ArrayList<Expression>(1);
        dependencies.add(vExpression);
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createGroovyScriptExpression("version", "v", String.class.getName(), dependencies);

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("callingProcess", PROCESS_VERSION);
        processDefBuilder.addShortTextData("v", new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION));
        processDefBuilder.addShortTextData("lName", null);
        processDefBuilder.addIntegerData("cNumber", null);
        processDefBuilder.addIntegerData("pNumber", null);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr);
        processDefBuilder.addUserTask("step1", ACTOR_NAME);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "step1");
        processDefBuilder.addTransition("step1", "end");

        final ProcessDefinition callingProcessDef = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId(), null, null);

        checkNbOfProcessInstances(2, ProcessInstanceCriterion.NAME_DESC);
        final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_DESC);
        assertEquals(2, processInstances.size()); // two instances are expected calling and target process
        final ProcessInstance targetPI = processInstances.get(0);
        assertEquals(targetPI.getProcessDefinitionId(), targetProcessDef.getId());

        assertEquals(PROCESS_VERSION, targetProcessDef.getVersion());

        final WaitForActivity waitForActivity = waitForActivity("callActivity", callingProcessInstance);
        final ActivityInstance callActivityInstance = waitForActivity.getResult();
        assertEquals(TestStates.getExecutingState(), callActivityInstance.getState());
        assertEquals(callingProcessInstance.getId(), targetPI.getRootProcessInstanceId());
        assertEquals(callActivityInstance.getId(), targetPI.getCallerId());

        WaitForStep waitForStep = waitForStep(50, 2000, "tStep1", callingProcessInstance, TestStates.getReadyState());
        ActivityInstance activityInstance = waitForStep.getResult();
        assertEquals(targetPI.getId(), activityInstance.getParentProcessInstanceId());
        assertEquals(callingProcessInstance.getId(), activityInstance.getRootContainerId());

        // execute step in the target process
        assignAndExecuteStep(activityInstance, cebolinha.getId());

        waitForStep = waitForStep(50, 5000, "step1", callingProcessInstance, TestStates.getReadyState());
        assertTrue("target process was not archived", waitProcessToFinishAndBeArchived(targetPI));
        activityInstance = waitForStep.getResult();
        assertEquals(callingProcessInstance.getId(), activityInstance.getParentProcessInstanceId());

        assignAndExecuteStep(activityInstance, cascao.getId());

        assertTrue("parent process was not archived", waitProcessToFinishAndBeArchived(callingProcessInstance));

        disableAndDeleteProcess(callingProcessDef);
        disableAndDeleteProcess(targetProcessDef);
    }

    @Cover(classes = { CallActivityInstance.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity" })
    @Test
    public void callActivityCheckAttributes() throws Exception {

        final ProcessDefinition targetProcessDef = getSimpleProcess(ACTOR_NAME, "targetProcess", PROCESS_VERSION, false);

        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression("targetProcess");
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION);

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("callingProcess", PROCESS_VERSION);
        processDefBuilder.addShortTextData("lName", null);
        processDefBuilder.addIntegerData("cNumber", null);
        processDefBuilder.addIntegerData("pNumber", null);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr)
                .addDisplayName(new ExpressionBuilder().createConstantStringExpression("callActivityDisplayName")).addDescription("callActivityDescription")
                .addDisplayDescription(new ExpressionBuilder().createConstantStringExpression("callActivityDisplayDescription"));
        processDefBuilder.addUserTask("step1", ACTOR_NAME);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "step1");
        processDefBuilder.addTransition("step1", "end");

        final ProcessDefinition callingProcessDef = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);

        assertEquals(0, getProcessAPI().getNumberOfProcessInstances());
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDef.getId(), null, null);

        checkNbOfProcessInstances(2, ProcessInstanceCriterion.NAME_DESC);
        final List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_DESC);
        assertEquals(2, processInstances.size()); // two instances are expected calling and target process
        final ProcessInstance targetPI = processInstances.get(0);

        final WaitForActivity waitForActivity = waitForActivity("callActivity", callingProcessInstance);
        final ActivityInstance callActivityInstance = waitForActivity.getResult();
        assertEquals(TestStates.getExecutingState(), callActivityInstance.getState());
        assertEquals(callingProcessInstance.getId(), targetPI.getRootProcessInstanceId());
        assertEquals(callActivityInstance.getId(), targetPI.getCallerId());
        assertEquals("callActivityDisplayName", callActivityInstance.getDisplayName());
        assertEquals("callActivityDescription", callActivityInstance.getDescription());
        assertEquals("callActivityDisplayDescription", callActivityInstance.getDisplayDescription());

        WaitForStep waitForStep = waitForStep(50, 2000, "tStep1", callingProcessInstance, TestStates.getReadyState());
        ActivityInstance activityInstance = waitForStep.getResult();
        assertEquals(targetPI.getId(), activityInstance.getParentProcessInstanceId());
        assertEquals(callingProcessInstance.getId(), activityInstance.getRootContainerId());

        // execute step in the target process
        assignAndExecuteStep(activityInstance, cebolinha.getId());

        waitForStep = waitForStep(50, 5000, "step1", callingProcessInstance, TestStates.getReadyState());
        assertTrue("target process was not archived", waitProcessToFinishAndBeArchived(targetPI));
        activityInstance = waitForStep.getResult();
        assertEquals(callingProcessInstance.getId(), activityInstance.getParentProcessInstanceId());

        assignAndExecuteStep(activityInstance, cascao.getId());

        assertTrue("parent process was not archived", waitProcessToFinishAndBeArchived(callingProcessInstance));

        disableAndDeleteProcess(callingProcessDef);
        disableAndDeleteProcess(targetProcessDef);
    }

    @Cover(classes = { ProcessDefinitionBuilder.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Expression" })
    @Test(expected = InvalidProcessDefinitionException.class)
    public void callActivityTargetProcessExprIsNull() throws Exception {
        final Expression targetProcessNameExpr = null;
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION);

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("callingProcess", PROCESS_VERSION);
        processDefBuilder.addShortTextData("lName", null);
        processDefBuilder.addIntegerData("cNumber", null);
        processDefBuilder.addIntegerData("pNumber", null);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr);
        processDefBuilder.addUserTask("step1", ACTOR_NAME);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "step1");
        processDefBuilder.addTransition("step1", "end");

        final ProcessDefinition callingProcessDef = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);

        disableAndDeleteProcess(callingProcessDef);
    }

    @Cover(classes = { CallActivityInstance.class, HumanTaskInstance.class, ArchivedProcessInstance.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = {
            "Call Activity", "Human Task", "Search", "Archived Process Instance" }, jira = "ENGINE-922")
    @Test
    public void callActivityTargetProcessWithJustHumanTask() throws Exception {

        // Build target process
        final ProcessDefinitionBuilder targetProcessDefBuilder = new ProcessDefinitionBuilder().createNewInstance("targetProcess", PROCESS_VERSION);
        targetProcessDefBuilder.addActor(ACTOR_NAME);
        targetProcessDefBuilder.addStartEvent("tStart");
        targetProcessDefBuilder.addUserTask("tStep1", ACTOR_NAME);
        targetProcessDefBuilder.addEndEvent("tEnd");
        targetProcessDefBuilder.addTransition("tStart", "tStep1");
        targetProcessDefBuilder.addTransition("tStep1", "tEnd");
        final ProcessDefinition targetProcessDefinition = deployAndEnableWithActor(targetProcessDefBuilder.done(), ACTOR_NAME, cebolinha);

        // Build and start calling process
        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression("targetProcess");
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION);
        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("callingProcess", PROCESS_VERSION);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr)
                .addDisplayName(new ExpressionBuilder().createConstantStringExpression("callActivityDisplayName")).addDescription("callActivityDescription")
                .addDisplayDescription(new ExpressionBuilder().createConstantStringExpression("callActivityDisplayDescription"));
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "end");
        final ProcessDefinition callingProcessDefinition = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDefinition.getId());

        // Execute step in the target process
        final WaitForStep waitForStep = waitForStep(50, 2000, "tStep1", callingProcessInstance, TestStates.getReadyState());
        final ActivityInstance activityInstance = waitForStep.getResult();
        assignAndExecuteStep(activityInstance, cebolinha.getId());

        waitForProcessToFinish(callingProcessInstance);

        // Search archived process
        assertEquals(1, getProcessAPI().getNumberOfArchivedProcessInstances());

        final SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0, 10);
        searchOptions.sort(ArchivedProcessInstancesSearchDescriptor.NAME, Order.ASC);
        final List<ArchivedProcessInstance> archivedProcessInstances = getProcessAPI().searchArchivedProcessInstances(searchOptions.done()).getResult();
        assertEquals(1, archivedProcessInstances.size());
        assertEquals(callingProcessInstance.getId(), archivedProcessInstances.get(0).getSourceObjectId());

        disableAndDeleteProcess(callingProcessDefinition);
        disableAndDeleteProcess(targetProcessDefinition);
    }

    @Cover(classes = { CallActivityInstance.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Engine constant" }, jira = "ENGINE-1009")
    @Test
    public void callActivityWithTaskUsingEngineExpressions() throws Exception {

        // Build target process
        final ProcessDefinitionBuilder targetProcessDefBuilder = new ProcessDefinitionBuilder().createNewInstance("targetProcess", PROCESS_VERSION);
        targetProcessDefBuilder.addActor(ACTOR_NAME);
        targetProcessDefBuilder.addStartEvent("tStart");
        targetProcessDefBuilder.addUserTask("tStep1", ACTOR_NAME).addData("rootProcId", Long.class.getName(),
                new ExpressionBuilder().createEngineConstant(ExpressionConstants.ROOT_PROCESS_INSTANCE_ID));
        targetProcessDefBuilder.addEndEvent("tEnd");
        targetProcessDefBuilder.addTransition("tStart", "tStep1");
        targetProcessDefBuilder.addTransition("tStep1", "tEnd");
        final ProcessDefinition targetProcessDefinition = deployAndEnableWithActor(targetProcessDefBuilder.done(), ACTOR_NAME, cebolinha);

        // Build and start calling process
        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression("targetProcess");
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION);
        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("callingProcess", PROCESS_VERSION);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addStartEvent("start");
        processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", "callActivity");
        processDefBuilder.addTransition("callActivity", "end");
        final ProcessDefinition callingProcessDefinition = deployAndEnableWithActor(processDefBuilder.done(), ACTOR_NAME, cascao);
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDefinition.getId());

        final ActivityInstance activityInstance = waitForUserTask("tStep1", callingProcessInstance.getId());
        assertEquals(callingProcessInstance.getId(), getProcessAPI().getActivityDataInstance("rootProcId", activityInstance.getId()).getValue());

        disableAndDeleteProcess(callingProcessDefinition);
        disableAndDeleteProcess(targetProcessDefinition);
    }

    @Cover(classes = { CallActivityInstance.class }, concept = BPMNConcept.CALL_ACTIVITY, keywords = { "Call Activity", "Connector", "Data mapping" }, jira = "ENGINE-1243")
    @Test
    public void callActivityWithDataMappingAndConnectors() throws Exception {
        // Build target process
        final ProcessDefinitionBuilder targetProcessDefBuilder = new ProcessDefinitionBuilder().createNewInstance("targetProcess", PROCESS_VERSION);
        targetProcessDefBuilder.addActor(ACTOR_NAME);
        targetProcessDefBuilder.addData("subProcessData", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("subDefault"));
        targetProcessDefBuilder.addAutomaticTask("tStep1").addOperation(
                new OperationBuilder().createSetDataOperation("subProcessData", new ExpressionBuilder().createConstantStringExpression("subModified")));
        final ProcessDefinition targetProcessDefinition = deployAndEnableWithActor(targetProcessDefBuilder.done(), ACTOR_NAME, cebolinha);

        // Build and start calling process
        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression("targetProcess");
        final Expression targetProcessVersionExpr = new ExpressionBuilder().createConstantStringExpression(PROCESS_VERSION);
        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("callingProcess", PROCESS_VERSION);
        processDefBuilder.addActor(ACTOR_NAME);
        processDefBuilder.addData("parentProcessData", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("parentDefault"));
        processDefBuilder.addData("valueOnCallOnEnter", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("none"));
        processDefBuilder.addData("valueOnCallOnFinish", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("none"));
        final CallActivityBuilder callActivityBuilder = processDefBuilder.addCallActivity("callActivity", targetProcessNameExpr, targetProcessVersionExpr);

        callActivityBuilder
                .addConnector("onEnterConnector", "org.bonitasoft.connector.testConnectorWithOutput", PROCESS_VERSION, ConnectorEvent.ON_ENTER)
                .addInput("input1", new ExpressionBuilder().createDataExpression("parentProcessData", String.class.getName()))
                .addOutput(new LeftOperandBuilder().createNewInstance().setName("valueOnCallOnEnter").done(), OperatorType.ASSIGNMENT, "=", "",
                        new ExpressionBuilder().createInputExpression(CONNECTOR_OUTPUT_NAME, String.class.getName()));
        callActivityBuilder
                .addConnector("onFinishConnector", "org.bonitasoft.connector.testConnectorWithOutput", PROCESS_VERSION, ConnectorEvent.ON_FINISH)
                .addInput("input1", new ExpressionBuilder().createDataExpression("parentProcessData", String.class.getName()))
                .addOutput(new LeftOperandBuilder().createNewInstance().setName("valueOnCallOnFinish").done(), OperatorType.ASSIGNMENT, "=", "",
                        new ExpressionBuilder().createInputExpression(CONNECTOR_OUTPUT_NAME, String.class.getName()));
        callActivityBuilder.addDataOutputOperation(new OperationBuilder().createSetDataOperation("parentProcessData",
                new ExpressionBuilder().createDataExpression("subProcessData", String.class.getName())));
        processDefBuilder.addUserTask("end", ACTOR_NAME);
        processDefBuilder.addTransition("callActivity", "end");
        final BusinessArchiveBuilder bizArchive = new BusinessArchiveBuilder();
        bizArchive.createNewBusinessArchive();
        bizArchive.setProcessDefinition(processDefBuilder.done());
        bizArchive.addConnectorImplementation(new BarResource("TestConnectorWithOutput.impl", IOUtils.toByteArray(BPMRemoteTests.class
                .getResourceAsStream("/org/bonitasoft/engine/connectors/TestConnectorWithOutput.impl"))));
        bizArchive.addClasspathResource(new BarResource("TestConnectorWithOutput.jar", IOUtil.generateJar(TestConnectorWithOutput.class)));

        final ProcessDefinition callingProcessDefinition = deployAndEnableWithActor(bizArchive.done(), ACTOR_NAME, cascao);
        final ProcessInstance callingProcessInstance = getProcessAPI().startProcess(callingProcessDefinition.getId());

        final ActivityInstance activityInstance = waitForUserTask("end", callingProcessInstance.getId());

        assertEquals("parentDefault", getProcessAPI().getActivityDataInstance("valueOnCallOnEnter", activityInstance.getId()).getValue());
        assertEquals("subModified", getProcessAPI().getActivityDataInstance("valueOnCallOnFinish", activityInstance.getId()).getValue());

        disableAndDeleteProcess(callingProcessDefinition);
        disableAndDeleteProcess(targetProcessDefinition);
    }

    private static final String CONNECTOR_OUTPUT_NAME = "output1";
}
