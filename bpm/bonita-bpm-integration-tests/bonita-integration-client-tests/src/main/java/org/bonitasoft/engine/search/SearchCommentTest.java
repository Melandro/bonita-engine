package org.bonitasoft.engine.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.comment.ArchivedComment;
import org.bonitasoft.engine.bpm.comment.ArchivedCommentsSearchDescriptor;
import org.bonitasoft.engine.bpm.comment.Comment;
import org.bonitasoft.engine.bpm.comment.SearchCommentsDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.test.APITestUtil;
import org.bonitasoft.engine.test.WaitUntil;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.bonitasoft.engine.test.wait.WaitForStep;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Celine Souchet
 */
public class SearchCommentTest extends CommonAPITest {

    @After
    public void afterTest() throws BonitaException {
        logout();
    }

    @Before
    public void beforeTest() throws BonitaException {
        login();
    }

    @Test
    public void searchArchiveComment() throws Exception {
        // create an user
        final User user = createUser(USERNAME, PASSWORD);

        // create a ProcessDefinition
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(PROCESS_NAME, PROCESS_VERSION);
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final DesignProcessDefinition designProcessDefinition = processBuilder.addUserTask("userTask1", ACTOR_NAME).getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, ACTOR_NAME, user);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        final WaitForStep waitForStep = waitForStep(50, 3000, "userTask1", processInstance);
        final String commentContent1 = "commentContent1";
        getProcessAPI().addComment(processInstance.getId(), commentContent1);
        final ActivityInstance activityInstance = waitForStep.getResult();
        assignAndExecuteStep(activityInstance, user.getId());
        waitForProcessToFinish(processInstance);
        final SearchOptionsBuilder builder2 = new SearchOptionsBuilder(0, 10);
        builder2.filter(ArchivedCommentsSearchDescriptor.PROCESS_INSTANCE_ID, processInstance.getId());
        final SearchResult<ArchivedComment> archivedCommentsResult = getProcessAPI().searchArchivedComments(builder2.done());
        final List<ArchivedComment> archivedComments = archivedCommentsResult.getResult();
        final ArchivedComment archivedComment = archivedComments.get(0);
        assertEquals(archivedComment, getProcessAPI().getArchivedComment(archivedComment.getId()));

        // clean all data for test
        disableAndDeleteProcess(processDefinition);
        deleteUser(user.getId());
    }

    @Test
    public void searchArchivedComments() throws Exception {
        searchArchivedComments("commentContent", new SearchOptionsBuilder(0, 10));
    }

    @Cover(classes = { SearchOptionsBuilder.class, ProcessAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "SearchArchivedComments", "Apostrophe" }, jira = "ENGINE-366")
    @Test
    public void searchArchivedCommentsWithApostrophe() throws Exception {
        searchArchivedComments("comment'Content", new SearchOptionsBuilder(0, 10).searchTerm("comment'"));
    }

    private void searchArchivedComments(final String commentContent, final SearchOptionsBuilder builder) throws Exception {
        // create an user
        final User user = createUser(USERNAME, PASSWORD);

        logout();
        loginWith(USERNAME, PASSWORD);

        // create a ProcessDefinition
        final String activityName = "userTask1";
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(PROCESS_NAME, PROCESS_VERSION);
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final DesignProcessDefinition designProcessDefinition = processBuilder.addUserTask(activityName, ACTOR_NAME).getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, ACTOR_NAME, user);

        // create a ProcessInstance
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForStep(50, 5000, activityName, processInstance);

        // add an comment to ProcessInstance
        getProcessAPI().addComment(processInstance.getId(), commentContent);

        // test the comment is added to ProcessInstance
        final SearchOptionsBuilder builder0 = new SearchOptionsBuilder(0, 5);
        builder0.filter(SearchCommentsDescriptor.PROCESS_INSTANCE_ID, processInstance.getId());
        final SearchResult<Comment> searchResult0 = getProcessAPI().searchComments(builder0.done());
        final List<Comment> commentList0 = searchResult0.getResult();
        assertEquals(1, commentList0.size());

        // get a ActivityInstance
        final List<ActivityInstance> activityInstances = getProcessAPI().getActivities(processInstance.getId(), 0, 10);

        // test before archive comment
        final SearchOptionsBuilder builder3 = new SearchOptionsBuilder(0, 10);
        builder3.filter(ArchivedCommentsSearchDescriptor.PROCESS_INSTANCE_ID, processInstance.getId());
        SearchResult<ArchivedComment> archivedCommentsResult = getProcessAPI().searchArchivedComments(builder3.done());
        assertEquals(0, archivedCommentsResult.getCount());

        // Archive a ProcessInstance
        for (final ActivityInstance activityInstance : activityInstances) {
            final long activityInstanceId = activityInstance.getId();
            getProcessAPI().setActivityStateById(activityInstanceId, 12);
        }

        // make sure archiving of the process instance is finished
        final SearchOptionsBuilder searchOptions = buildSearchOptions(processDefinition.getId(), 0, 1, ArchivedProcessInstancesSearchDescriptor.ID, Order.ASC);
        // search and check result ASC
        assertTrue("Expected 1 ARCHIVED process instances not found", new WaitUntil(500, 10000) {

            @Override
            protected boolean check() throws Exception {
                return getProcessAPI().searchArchivedProcessInstances(searchOptions.done()).getCount() == 1;
            }
        }.waitUntil());

        // test comment is archived
        builder.filter(ArchivedCommentsSearchDescriptor.PROCESS_INSTANCE_ID, processInstance.getId());
        archivedCommentsResult = getProcessAPI().searchArchivedComments(builder.done());
        assertEquals(1, archivedCommentsResult.getCount());
        final List<ArchivedComment> archivedComments = archivedCommentsResult.getResult();
        final ArchivedComment archivedComment = archivedComments.get(0);
        assertEquals(commentContent, archivedComment.getContent());

        // clean all data for test
        disableAndDeleteProcess(processDefinition);
        deleteUser(user.getId());
    }

    @Cover(classes = { SearchOptionsBuilder.class, ProcessAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "SearchComments" }, jira = "ENGINE-366")
    @Test
    public void searchComments() throws Exception {
        final User user = createUser(USERNAME, PASSWORD);
        loginWith(USERNAME, PASSWORD);
        DesignProcessDefinition designProcessDefinition;
        designProcessDefinition = APITestUtil.createProcessDefinitionWithHumanAndAutomaticSteps(Arrays.asList("step1", "step2"), Arrays.asList(true, true));
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, ACTOR_NAME, user);
        final ProcessInstance pi0 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance pi1 = getProcessAPI().startProcess(processDefinition.getId());

        final String commentContent1 = "commentContent1";
        final String commentContent2 = "commentContent2";
        final String commentContent3 = "commentContent3";
        final String commentContent4 = "content4";
        final String commentContent5 = "content'5";
        getProcessAPI().addComment(pi1.getId(), commentContent1);
        getProcessAPI().addComment(pi1.getId(), commentContent2);
        getProcessAPI().addComment(pi1.getId(), commentContent3);
        getProcessAPI().addComment(pi0.getId(), commentContent4);
        getProcessAPI().addComment(pi0.getId(), commentContent5);

        final SearchOptionsBuilder builder0 = new SearchOptionsBuilder(0, 5);
        builder0.filter(SearchCommentsDescriptor.PROCESS_INSTANCE_ID, pi0.getId());
        builder0.sort(SearchCommentsDescriptor.POSTDATE, Order.ASC);

        final SearchResult<Comment> searchResult0 = getProcessAPI().searchComments(builder0.done());
        final List<Comment> commentList0 = searchResult0.getResult();
        assertEquals(2, commentList0.size());

        final SearchOptionsBuilder builder1 = new SearchOptionsBuilder(0, 5);
        builder1.filter(SearchCommentsDescriptor.PROCESS_INSTANCE_ID, pi1.getId());
        builder1.sort(SearchCommentsDescriptor.POSTDATE, Order.ASC);
        final SearchResult<Comment> searchResult1 = getProcessAPI().searchComments(builder1.done());
        final List<Comment> commentList1 = searchResult1.getResult();
        assertEquals(3, commentList1.size());
        assertEquals(commentContent1, commentList1.get(0).getContent());
        assertEquals(commentContent2, commentList1.get(1).getContent());
        assertEquals(commentContent3, commentList1.get(2).getContent());

        final SearchOptionsBuilder builder2 = new SearchOptionsBuilder(0, 5);
        builder2.sort(SearchCommentsDescriptor.POSTDATE, Order.ASC);
        builder2.searchTerm("comment");
        final SearchResult<Comment> searchResult2 = getProcessAPI().searchComments(builder2.done());
        final List<Comment> commentList2 = searchResult2.getResult();
        assertEquals(3, commentList2.size());

        // Search with a apostrophe
        final SearchOptionsBuilder builder4 = new SearchOptionsBuilder(0, 5);
        builder4.sort(SearchCommentsDescriptor.POSTDATE, Order.ASC);
        builder4.searchTerm("content'");
        final SearchResult<Comment> searchResult4 = getProcessAPI().searchComments(builder4.done());
        final List<Comment> commentList4 = searchResult4.getResult();
        assertEquals(1, commentList4.size());

        final SearchOptionsBuilder builder3 = new SearchOptionsBuilder(0, 5);
        builder3.filter(SearchCommentsDescriptor.USER_NAME, USERNAME);
        builder3.sort(SearchCommentsDescriptor.POSTDATE, Order.ASC);
        final SearchResult<Comment> searchResult3 = getProcessAPI().searchComments(builder3.done());
        final List<Comment> commentList3 = searchResult3.getResult();
        assertEquals(5, commentList3.size());

        disableAndDeleteProcess(processDefinition);
        deleteUser(user);
    }

    @Test
    public void searchCommentsForPartArchivedCases() throws Exception {
        // create an user
        final User user = createUser(USERNAME, PASSWORD);

        // create a ProcessDefinition
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(PROCESS_NAME, PROCESS_VERSION);
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final DesignProcessDefinition designProcessDefinition = processBuilder.addUserTask("userTask1", ACTOR_NAME).getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, ACTOR_NAME, user);

        // create a ProcessInstance
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance processInstance2 = getProcessAPI().startProcess(processDefinition.getId());
        waitForStep(50, 5000, "userTask1", processInstance);
        waitForStep(50, 5000, "userTask1", processInstance2);

        // add an comment to ProcessInstance
        final String commentContent1 = "commentContent1";
        final String commentContent2 = "commentContent2";
        getProcessAPI().addComment(processInstance.getId(), commentContent1);
        getProcessAPI().addComment(processInstance2.getId(), commentContent2);

        // get a ActivityInstance
        final List<ActivityInstance> activityInstances = getProcessAPI().getActivities(processInstance.getId(), 0, 10);

        // test before archive comment
        final SearchOptionsBuilder builder3 = new SearchOptionsBuilder(0, 10);
        SearchResult<Comment> commentsResult = getProcessAPI().searchComments(builder3.done());
        assertEquals(2, commentsResult.getCount());

        // Archive a ProcessInstance
        for (final ActivityInstance activityInstance : activityInstances) {
            final long activityInstanceId = activityInstance.getId();
            getProcessAPI().setActivityStateById(activityInstanceId, 12);
        }

        // make sure archiving of the process instance is finished
        final SearchOptionsBuilder searchOptions = buildSearchOptions(processDefinition.getId(), 0, 10, ArchivedProcessInstancesSearchDescriptor.ID, Order.ASC);
        // search and check result ASC
        assertTrue("Expected 1 ARCHIVED process instances not found", new WaitUntil(500, 10000) {

            @Override
            protected boolean check() throws Exception {
                return getProcessAPI().searchArchivedProcessInstances(searchOptions.done()).getCount() == 1;
            }
        }.waitUntil());

        // test comment is archived
        final SearchOptionsBuilder builder2 = new SearchOptionsBuilder(0, 10);
        commentsResult = getProcessAPI().searchComments(builder2.done());
        assertEquals(1, commentsResult.getCount());
        final List<Comment> comments = commentsResult.getResult();
        final Comment comment = comments.get(0);
        assertEquals(commentContent2, comment.getContent());

        // clean all data for test
        disableAndDeleteProcess(processDefinition);
        deleteUser(user.getId());
    }

    @Test
    public void searchCommentsInvolvingUser() throws Exception {
        final String jackUserName = "jack";
        final String johnUserName = "john";

        final User jack = createUser(jackUserName, PASSWORD);
        final User john = createUser(johnUserName, PASSWORD);

        final SearchOptionsBuilder builderInitJack = new SearchOptionsBuilder(0, 10);
        final SearchResult<Comment> resultinitjack = getProcessAPI().searchCommentsInvolvingUser(jack.getId(), builderInitJack.done());
        final long jackInitComments = resultinitjack.getCount();

        final SearchOptionsBuilder builderInitJohn = new SearchOptionsBuilder(0, 10);
        final SearchResult<Comment> resultInitJohn = getProcessAPI().searchCommentsInvolvingUser(john.getId(), builderInitJohn.done());
        final long johnInitComments = resultInitJohn.getCount();

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(PROCESS_NAME, PROCESS_VERSION);
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final DesignProcessDefinition designProcessDefinition = processBuilder.addUserTask("userTask1", ACTOR_NAME).addUserTask("userTask2", ACTOR_NAME)
                .getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, ACTOR_NAME, jack);

        final ProcessInstance instance1 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance instance2 = getProcessAPI().startProcess(john.getId(), processDefinition.getId());

        final WaitForStep waitForStep1 = waitForStep(50, 500, "userTask1", instance1);
        final WaitForStep waitForStep2 = waitForStep(50, 500, "userTask2", instance1);
        final long stepId1 = waitForStep1.getStepId();
        final long stepId2 = waitForStep2.getStepId();
        getProcessAPI().assignUserTask(stepId1, jack.getId());
        getProcessAPI().assignUserTask(stepId2, jack.getId());
        logout();
        loginWith(jackUserName, PASSWORD);

        final String commentContent1 = "jack's comment Content1";
        final String commentContent2 = "jack's comment Content3";
        final String commentContent3 = "jack's comment Content3";
        getProcessAPI().addComment(instance1.getId(), commentContent1);
        getProcessAPI().addComment(instance1.getId(), commentContent2);
        getProcessAPI().addComment(instance1.getId(), commentContent3);

        logout();
        loginWith(johnUserName, PASSWORD);
        final String commentContent4 = "john's comment Content3";
        getProcessAPI().addComment(instance2.getId(), commentContent4);

        final SearchOptionsBuilder builder1 = new SearchOptionsBuilder(0, 10);
        final SearchResult<Comment> result1 = getProcessAPI().searchCommentsInvolvingUser(jack.getId(), builder1.done());
        assertEquals(jackInitComments + 3, result1.getCount());
        final List<Comment> commentList1 = result1.getResult();
        assertNotNull(commentList1);
        assertEquals(jackInitComments + 3, commentList1.size());

        final SearchOptionsBuilder builder2 = new SearchOptionsBuilder(0, 10);
        final SearchResult<Comment> result2 = getProcessAPI().searchCommentsInvolvingUser(john.getId(), builder2.done());
        assertEquals(johnInitComments + 1, result2.getCount());
        final List<Comment> commentList2 = result2.getResult();
        assertEquals(commentContent4, commentList2.get(0).getContent());

        disableAndDeleteProcess(processDefinition);
        deleteUser(jack.getId());
        deleteUser(john.getId());
    }

    @Test
    public void searchCommentsManagedBy() throws Exception {
        // Create two new users John Jack and Steven, and set John managed by Steven, login with John.
        final String johnUserName = "john";
        final String jimUserName = "jim";
        final String jackUserName = "jack";
        final String stevenUserName = "steven";

        final User steven = createUser(stevenUserName, PASSWORD);
        final User jack = createUser(jackUserName, PASSWORD);
        final User john = createUser(johnUserName, PASSWORD, steven.getId());
        final User jim = createUser(jimUserName, PASSWORD, steven.getId());

        logout();
        loginWith(johnUserName, PASSWORD);

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(PROCESS_NAME, PROCESS_VERSION);
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final DesignProcessDefinition designProcessDefinition = processBuilder.addUserTask("userTask1", ACTOR_NAME).addUserTask("userTask2", ACTOR_NAME)
                .getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, ACTOR_NAME, john);

        final ProcessInstance pi1 = getProcessAPI().startProcess(steven.getId(), processDefinition.getId());

        final WaitForStep waitForStep1 = waitForStep(50, 500, "userTask1", pi1);
        final WaitForStep waitForStep2 = waitForStep(50, 500, "userTask2", pi1);
        final long stepId1 = waitForStep1.getStepId();
        final long stepId2 = waitForStep2.getStepId();
        getProcessAPI().assignUserTask(stepId1, john.getId());
        getProcessAPI().assignUserTask(stepId2, john.getId());

        final String commentContent1 = "John's comment Content1";
        final String commentContent2 = "John's comment Content2";
        final String commentContent3 = "John's comment Content3";
        getProcessAPI().addComment(pi1.getId(), commentContent1);
        getProcessAPI().addComment(pi1.getId(), commentContent2);
        getProcessAPI().addComment(pi1.getId(), commentContent3);

        logout();
        loginWith(jackUserName, PASSWORD);

        final ProcessInstance pi2 = getProcessAPI().startProcess(jim.getId(), processDefinition.getId());
        final String commentContent4 = "Jack's comment Content4";
        getProcessAPI().addComment(pi2.getId(), commentContent4);

        logout();
        loginWith(stevenUserName, PASSWORD);

        final ProcessInstance pi3 = getProcessAPI().startProcess(steven.getId(), processDefinition.getId());
        final WaitForStep waitForStep3 = waitForStep(50, 500, "userTask1", pi3);
        final long stepId3 = waitForStep3.getStepId();
        getProcessAPI().assignUserTask(stepId3, jim.getId());
        final String commentContent5 = "Steven's comment Content5";
        getProcessAPI().addComment(pi3.getId(), commentContent5);

        final ProcessInstance pi4 = getProcessAPI().startProcess(steven.getId(), processDefinition.getId());
        final String commentContent6 = "Steven's comment Content6";
        getProcessAPI().addComment(pi4.getId(), commentContent6);

        logout();
        login();

        final SearchOptionsBuilder builder3 = new SearchOptionsBuilder(0, 10);
        final SearchResult<Comment> searchResult3 = getProcessAPI().searchCommentsManagedBy(jack.getId(), builder3.done());
        final List<Comment> commentList3 = searchResult3.getResult();
        assertEquals(0, commentList3.size());

        final SearchOptionsBuilder builder2 = new SearchOptionsBuilder(0, 10);
        builder2.sort(SearchCommentsDescriptor.POSTED_BY_ID, Order.ASC);
        final SearchResult<Comment> searchResult2 = getProcessAPI().searchCommentsManagedBy(steven.getId(), builder2.done());
        final List<Comment> commentList2 = searchResult2.getResult();
        assertEquals(5, searchResult2.getCount());
        // Order by POSTED_BY_ID uses creation order:
        assertEquals(Long.valueOf(steven.getId()), commentList2.get(0).getUserId());
        assertEquals(Long.valueOf(jack.getId()), commentList2.get(1).getUserId());
        assertEquals(Long.valueOf(john.getId()), commentList2.get(2).getUserId());
        assertEquals(Long.valueOf(john.getId()), commentList2.get(3).getUserId());
        assertEquals(Long.valueOf(john.getId()), commentList2.get(4).getUserId());
        assertTrue(commentList2.get(0).getContent().contains("Content5"));

        disableAndDeleteProcess(processDefinition);
        deleteUser(john);
        deleteUser(jack);
        deleteUser(jim);
        deleteUser(steven);
    }

}
