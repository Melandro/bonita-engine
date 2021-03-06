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
package org.bonitasoft.engine.search.impl;

import java.io.Serializable;

import org.bonitasoft.engine.exception.IncorrectParameterException;
import org.bonitasoft.engine.search.SearchFilterOperation;

/**
 * @author Emmanuel Duchastenier
 */
public class SearchFilter implements Serializable {

    private static final long serialVersionUID = 2476946810762051485L;

    private String field;

    private SearchFilterOperation operation;

    private Serializable value;

    private Serializable from;

    private Serializable to;

    /**
     * @param field
     *            the field to filter on
     * @param operation
     *            the operation to filter on
     * @param value
     *            the value of the field to filter on
     * @see SearchFilterOperation
     */
    public SearchFilter(final String field, final SearchFilterOperation operation, final Serializable value) {
        this.field = field;
        this.operation = operation;
        this.value = value;
    }

    public SearchFilter(final String field, final Serializable from, final Serializable to) {
        this.field = field;
        operation = SearchFilterOperation.BETWEEN;
        this.from = from;
        this.to = to;
    }

    public SearchFilter(final SearchFilterOperation operation) throws IncorrectParameterException {
        this.operation = operation;
        if (!isUndefinedFieldNameAuthorized()) {
            throw new IncorrectParameterException("search operator can only be AND or OR on the one-parameter SearchFilter constructor");
        }
    }

    public boolean isUndefinedFieldNameAuthorized() {
        return operation == SearchFilterOperation.AND || operation == SearchFilterOperation.OR;
    }

    /**
     * @return the field name
     */
    public String getField() {
        return field;
    }

    /**
     * @param field
     *            the field name to set
     */
    public void setField(final String field) {
        this.field = field;
    }

    /**
     * @return the operation
     */
    public SearchFilterOperation getOperation() {
        return operation;
    }

    /**
     * @param operation
     *            the operation to set
     */
    public void setOperation(final SearchFilterOperation operation) {
        this.operation = operation;
    }

    /**
     * @return the value
     */
    public Serializable getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(final Serializable value) {
        this.value = value;
    }

    /**
     * @return the from
     */
    public Serializable getFrom() {
        return from;
    }

    /**
     * @return the to
     */
    public Serializable getTo() {
        return to;
    }
}
