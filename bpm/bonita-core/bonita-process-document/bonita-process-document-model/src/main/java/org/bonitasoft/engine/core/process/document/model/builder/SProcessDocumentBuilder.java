/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.document.model.builder;

import org.bonitasoft.engine.core.process.document.model.SProcessDocument;

/**
 * @author Nicolas Chabanoles
 */
public interface SProcessDocumentBuilder {

    SProcessDocumentBuilder createNewInstance();

    SProcessDocumentBuilder setId(long id);

    SProcessDocumentBuilder setProcessInstanceId(final long processInstanceId);

    SProcessDocumentBuilder setName(String name);

    SProcessDocumentBuilder setAuthor(long authorId);

    SProcessDocumentBuilder setCreationDate(long creationDate);

    SProcessDocumentBuilder setHasContent(boolean hasContent);

    SProcessDocumentBuilder setFileName(String fileName);

    SProcessDocumentBuilder setContentMimeType(String contentMimeType);

    SProcessDocumentBuilder setContentStorageId(String contentStorageId);

    SProcessDocumentBuilder setURL(String url);

    SProcessDocument done();

}
