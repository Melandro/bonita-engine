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
package org.bonitasoft.engine.work;

import java.util.Collection;

import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

/**
 * @author Charles Souillard
 * @author Baptiste Mesta
 */
public class SequenceRunnableExecutor extends NotifyingRunnable {

    private final Collection<AbstractBonitaWork> works;

    private boolean cancelled = false;

    private final TechnicalLoggerService loggerService;

    public SequenceRunnableExecutor(final Collection<AbstractBonitaWork> works, final RunnableListener runnableListener, final long tenantId,
            final TechnicalLoggerService loggerService) {
        super(runnableListener, tenantId);
        this.works = works;
        this.loggerService = loggerService;
    }

    @Override
    public void innerRun() {
        for (final AbstractBonitaWork work : works) {
            if (!cancelled) {
                try {
                    work.run();
                } catch (Throwable t) {
                    loggerService.log(getClass(), TechnicalLogSeverity.ERROR, "Error while executing one work in the list of works: " + work.getDescription(),
                            t);
                }
            }
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

}
