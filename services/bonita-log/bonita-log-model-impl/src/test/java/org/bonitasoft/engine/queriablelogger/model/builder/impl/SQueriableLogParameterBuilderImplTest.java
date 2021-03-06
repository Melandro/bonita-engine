/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.engine.queriablelogger.model.builder.impl;

import static org.junit.Assert.assertEquals;

import org.bonitasoft.engine.queriablelogger.model.SQueriableLogParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Elias Ricken de Medeiros
 */
public class SQueriableLogParameterBuilderImplTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(SQueriableLogParameterBuilderImplTest.class);

    private final SQueriableLogParameterBuilderImpl builder;

    public SQueriableLogParameterBuilderImplTest() {
        builder = new SQueriableLogParameterBuilderImpl();
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        LOGGER.info("Testing : {}", name.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        LOGGER.info("Tested: {}", name.getMethodName());
    }

    @Test
    public void testCriateNewInstance() {
        final SQueriableLogParameter parameter = builder.createNewInstance("returnValue", "String").done();
        assertEquals("returnValue", parameter.getName());
        assertEquals("String", parameter.getValueType());
    }

    @Test
    public void testSetStringValue() {
        final SQueriableLogParameter parameter = builder.createNewInstance("returnValue", "String").stringValue("38").done();
        assertEquals("38", parameter.getStringValue());
    }

    @Test
    public void testSetBlobValue() {
        final SQueriableLogParameter parameter = builder.createNewInstance("returnValue", "String").blobValue("blobValue").done();
        assertEquals("blobValue", parameter.getBlobValue().getValue());
    }

    @Test(expected = MissingMandatoryFieldsException.class)
    public void testFailsIfNameIsNull() {
        builder.createNewInstance(null, "Integer").done();
    }

    @Test(expected = MissingMandatoryFieldsException.class)
    public void testFailsIfValueType() {
        builder.createNewInstance("name", null).done();
    }

}
