package org.bonitasoft.engine.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.InvalidSessionException;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.test.APITestUtil;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformTest.class);

    private static PlatformAPI platformAPI;

    private static PlatformSession session;

    @BeforeClass
    public static void beforeClass() throws BonitaException {
        session = APITestUtil.loginPlatform();
        platformAPI = PlatformAPIAccessor.getPlatformAPI(session);
        APITestUtil.initializeAndStartPlatformWithDefaultTenant(platformAPI, false);
    }

    @AfterClass
    public static void afterClass() throws BonitaException {
        APITestUtil.stopAndCleanPlatformAndTenant(platformAPI, false);
        APITestUtil.logoutPlatform(session);
    }

    @Before
    public void before() throws BonitaException {
        if (!platformAPI.isPlatformCreated()) {
            platformAPI.createAndInitializePlatform();
            platformAPI.startNode();
        }
    }

    @Rule
    public TestRule testWatcher = new TestWatcher() {

        @Override
        public void starting(final Description d) {
            LOGGER.info("Starting test: " + getClass().getName() + "." + d.getMethodName());
        }

        @Override
        public void failed(final Throwable cause, final Description d) {
            LOGGER.info("Failed test: " + getClass().getName() + "." + d.getMethodName());
            LOGGER.info("-------------------------------------------------------------------------------------");
        }

        @Override
        public void succeeded(final Description d) {
            LOGGER.info("Succeeded test: " + getClass().getName() + "." + d.getMethodName());
            LOGGER.info("-------------------------------------------------------------------------------------");
        }

    };

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform" }, story = "Test if platform is created.")
    @Test
    public void isPlatformCreated() throws BonitaException {
        assertTrue(platformAPI.isPlatformCreated());
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform" }, story = "Get exception when creating platform.")
    @Test(expected = CreationException.class)
    public void createPlatformException() throws BonitaException {
        assertTrue(platformAPI.isPlatformCreated());
        platformAPI.createAndInitializePlatform();
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform" }, story = "Get platform.")
    @Test
    public void getPlatform() throws BonitaException {
        final Platform platform = platformAPI.getPlatform();

        final String platformVersionToTest = "6.1.0";

        assertNotNull("can't find the platform", platform);
        assertEquals("platformAdmin", platform.getCreatedBy());
        final String version = platform.getVersion();
        assertTrue(
                version,
                version.equals(platformVersionToTest + "-SNAPSHOT") || version.equals(platformVersionToTest + "")
                        || version.startsWith(platformVersionToTest + "-rc"));

        final String initialVersion = platform.getInitialVersion();
        assertTrue(
                version,
                initialVersion.equals(platformVersionToTest + "-SNAPSHOT") || initialVersion.equals(platformVersionToTest + "")
                        || initialVersion.startsWith(platformVersionToTest + "-rc"));
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform" }, story = "Delete platform.")
    @Test
    public void deletePlatform() throws BonitaException {
        platformAPI.stopNode();
        platformAPI.cleanAndDeletePlaftorm();
        assertFalse(platformAPI.isPlatformCreated());
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform", "State" }, story = "Try to get platfom state.")
    @Test
    public void getPlatformState() throws Exception {
        // test started state
        PlatformState state = platformAPI.getPlatformState();
        assertEquals(PlatformState.STARTED, state);
        // test stopped state
        platformAPI.stopNode();
        state = platformAPI.getPlatformState();
        assertEquals(PlatformState.STOPPED, state);
        // test exception:PlatformNotFoundException
        platformAPI.cleanAndDeletePlaftorm();
        try {
            platformAPI.getPlatformState();
            fail();
        } catch (final PlatformNotFoundException e) {
            // ok
        }
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform", "Node" }, story = "Get exception when starting node.", jira = "ENGINE-621")
    @Test(expected = StartNodeException.class)
    public void unableToStartANodeIfTheNodeIsNotCreated() throws Exception {
        platformAPI.stopNode();
        platformAPI.cleanAndDeletePlaftorm();
        assertFalse(platformAPI.isPlatformCreated());
        platformAPI.startNode();
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform", "Node" }, story = "Get exception when stopping node.", jira = "ENGINE-621")
    @Test
    public void callStopNodeTwice() throws Exception {
        platformAPI.stopNode();
        platformAPI.stopNode();
        platformAPI.cleanAndDeletePlaftorm();
    }

    @Cover(classes = PlatformAPI.class, concept = BPMNConcept.NONE, keywords = { "Platform", "Node" }, story = "stop node then start it with same session.")
    @Test
    public void stopNodeAndStartNode() throws Exception {
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        APISession tenantSession = loginAPI.login("install", "install");
        IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(tenantSession);
        identityAPI.getNumberOfUsers();
        platformAPI.stopNode();
        platformAPI.startNode();
        try {
            identityAPI.getNumberOfUsers();
            fail("session should not work");
        } catch (InvalidSessionException e) {
            // ok
            e.printStackTrace();
        }
    }

}
