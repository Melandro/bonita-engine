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
package org.bonitasoft.engine.expression;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Celine Souchet
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpressionBuilderTest {

    @InjectMocks
    private ExpressionBuilder expressionBuilder;

    @Test
    public final void createNewInstance() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createConstantStringExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createConstantBooleanExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createConstantLongExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createConstantIntegerExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createDataExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createDocumentReferenceExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createPatternExpressionString() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createInputExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createAPIAccessorExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createEngineConstant() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void done() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setContent() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setExpressionTypeString() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setExpressionTypeExpressionType() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setReturnType() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setInterpreter() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setDependencies() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void setName() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createExpressionStringStringStringExpressionType() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createConstantDoubleExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createConstantFloatExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createListExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createListOfListExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createGroovyScriptExpressionStringStringString() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createGroovyScriptExpressionStringStringStringExpressionArray() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createPatternExpressionStringStringExpressionArray() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createParameterExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createComparisonExpressionStringExpressionComparisonOperatorExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createComparisonExpressionStringExpressionStringExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createLogicalComplementExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createXPathExpressionWithDataAsContent() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createXPathExpression() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void getXPathReturnType() throws Exception {

        // TODO : Not yet implemented
    }

    @Test
    public final void createJavaMethodCallExpression() throws Exception {

        // TODO : Not yet implemented
    }

    // FIXME : Split the following tests in unit tests

    private Expression longExpr;

    private Expression intExpr;

    private Expression string;

    @Before
    public void initializeExpressions() throws Exception {
        longExpr = new ExpressionBuilder().createConstantLongExpression(1);
        intExpr = new ExpressionBuilder().createConstantIntegerExpression(1);
        string = new ExpressionBuilder().createConstantStringExpression("string");
    }

    @Test(expected = InvalidExpressionException.class)
    public void testInvalidOperator() throws Exception {
        new ExpressionBuilder().createComparisonExpression("comp1", intExpr, "||", intExpr);
    }

    @Test
    public void testValidReturnTypesForBinaryOperator() throws Exception {
        new ExpressionBuilder().createComparisonExpression("comp1", intExpr, ComparisonOperator.GREATER_THAN, longExpr);
    }

    @Test(expected = InvalidExpressionException.class)
    public void testInvalidReturnTypesForBinaryOperator() throws Exception {
        new ExpressionBuilder().createComparisonExpression("comp1", intExpr, ComparisonOperator.GREATER_THAN, string);
    }

    @Test(expected = InvalidExpressionException.class)
    public void testInvalidReturnTypeForUnaryOperator() throws Exception {
        new ExpressionBuilder().createLogicalComplementExpression("complement", intExpr);
    }

}
