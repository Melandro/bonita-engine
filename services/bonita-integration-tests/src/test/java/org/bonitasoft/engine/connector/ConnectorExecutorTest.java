/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.connector.impl.ConnectorExecutorImpl;
import org.bonitasoft.engine.log.technical.TechnicalLoggerSLF4JImpl;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.xml.ElementBinding;
import org.bonitasoft.engine.xml.Parser;
import org.bonitasoft.engine.xml.parse.SAXParser;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Feng Hui
 */
public class ConnectorExecutorTest {

    private Parser parser;

    private Map<String, Object> inputParameters; // TODO parse it from XML file

    private final String INPUT_KEY = "input1";

    @Before
    public void before() {
        parser = new SAXParser(null, null);
        final List<Class<? extends ElementBinding>> bindings = new ArrayList<Class<? extends ElementBinding>>();
        // bindings.add(ConnectorImplementationBinding.class);
        // bindings.add(JarDependenciesBinding.class);
        parser.setBindings(bindings);
        setXMLPath();
    }

    private void setXMLPath() {
        String path = this.getClass().getClassLoader().getResource("Mockup_1.0.xml").getPath();
        final int index = path.lastIndexOf("/");
        path = path.substring(0, index + 1);
        System.setProperty("implementationXMLPath", path);
    }

    private ConnectorExecutor getConnectorExecutor() {
        return new ConnectorExecutorImpl(10, 5, new TechnicalLoggerSLF4JImpl(), 100, 100, new SessionAccessor() {

            @Override
            public long getTenantId() {
                return 0;
            }

            @Override
            public long getSessionId() {
                return 0;
            }

            @Override
            public void setSessionInfo(long sessionId, long tenantId) {

            }

            @Override
            public void deleteSessionId() {
            }
        });
    }

    @Test
    public void testExecuteConnector() {
        getConnectorExecutor();
        inputParameters = getInputParameters();
        // Map<String, Object> resultMap = connectorExecutor.execute(null, inputParameters);
        // assertNotNull(resultMap);
        // assertEquals("Hello Bonita!", resultMap.get(OUTPUT_KEY));
    }

    /**
     * @return
     */
    private Map<String, Object> getInputParameters() {
        inputParameters = new HashMap<String, Object>();
        inputParameters.put(INPUT_KEY, "Hello Bonita!");
        return inputParameters;
    }

}
