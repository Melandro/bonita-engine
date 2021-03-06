/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.home;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.io.PropertiesManager;

/**
 * @author Baptiste Mesta
 * @author Frederic Bouquet
 * @author Matthieu Chaffotte
 */
public final class BonitaHomeServer extends BonitaHome {

    private static final String CONF = "conf";

    private static final String REPORTS = "reports";

    private static final String BONITA_HOME_SERVER = "server";

    private static final String BONITA_HOME_PLATFORM = "platform";

    private static final String BONITA_HOME_TENANTS = "tenants";

    private static final String BONITA_HOME_PROCESSES = "processes";

    private static final String BONITA_HOME_WORK = "work";

    private static final String BONITA_HOME_TENANT_TEMPLATE = "tenant-template";

    private String tenantsPath;

    private String platformPath;

    private String serverPath;

    private Properties platformProperties;

    public static final BonitaHomeServer INSTANCE = new BonitaHomeServer();

    private BonitaHomeServer() {
        platformProperties = null;
    }

    public static BonitaHomeServer getInstance() {
        return INSTANCE;
    }

    public String getBonitaHomeServerFolder() throws BonitaHomeNotSetException {
        if (serverPath == null) {
            final StringBuilder path = new StringBuilder(getBonitaHomeFolder());
            path.append(File.separatorChar);
            path.append(BONITA_HOME_SERVER);
            serverPath = path.toString();
        }
        return serverPath;
    }

    public String getPlatformFolder() throws BonitaHomeNotSetException {
        if (platformPath == null) {
            final StringBuilder path = new StringBuilder(getBonitaHomeServerFolder());
            path.append(File.separatorChar);
            path.append(BONITA_HOME_PLATFORM);
            platformPath = path.toString();
        }
        return platformPath;
    }

    public String getPlatformConfFolder() throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getPlatformFolder());
        path.append(File.separatorChar);
        path.append(CONF);
        return path.toString();
    }

    public File getPlaformFile() throws BonitaHomeNotSetException {
        final String platformFolder = getPlatformConfFolder();
        final StringBuilder builder = new StringBuilder(platformFolder);
        builder.append(File.separatorChar).append("bonita-platform.properties");
        return new File(builder.toString());
    }

    public String getTenantsFolder() throws BonitaHomeNotSetException {
        if (tenantsPath == null) {
            final StringBuilder path = new StringBuilder(getBonitaHomeServerFolder());
            path.append(File.separatorChar);
            path.append(BONITA_HOME_TENANTS);
            tenantsPath = path.toString();
        }
        return tenantsPath;
    }

    public String getProcessesFolder(final long tenantId) throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getTenantFolder(tenantId));
        path.append(File.separatorChar);
        path.append(BONITA_HOME_WORK);
        path.append(File.separatorChar);
        path.append(BONITA_HOME_PROCESSES);
        return path.toString();
    }

    public String getProcessFolder(final long tenantId, final long processDefinitionId) throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getTenantFolder(tenantId));
        path.append(File.separatorChar);
        path.append(BONITA_HOME_WORK);
        path.append(File.separatorChar);
        path.append(BONITA_HOME_PROCESSES);
        path.append(File.separatorChar);
        path.append(processDefinitionId);
        return path.toString();
    }

    public String getTenantFolder(final long tenantId) throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getTenantsFolder());
        path.append(File.separatorChar);
        path.append(tenantId);
        return path.toString();
    }

    public String getTenantConfFolder(final long tenantId) throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getTenantsFolder());
        path.append(File.separatorChar);
        path.append(tenantId);
        path.append(File.separatorChar);
        path.append(CONF);
        return path.toString();
    }

    /**
     * Returns the absolute path to the reports folder inside a specific tenant folder.
     * 
     * @param tenantId
     *            the ID of the tenant to search for.
     * @return the absolute path to the reports folder for the tenant.
     * @throws BonitaHomeNotSetException
     *             if BonitaHome is not set.
     */
    public String getTenantReportFolder(final long tenantId) throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getTenantsFolder());
        path.append(File.separatorChar);
        path.append(tenantId);
        path.append(File.separatorChar);
        path.append(REPORTS);
        return path.toString();
    }

    public String getTenantTemplateFolder() throws BonitaHomeNotSetException {
        final StringBuilder path = new StringBuilder(getPlatformFolder());
        path.append(File.separatorChar);
        path.append(BONITA_HOME_TENANT_TEMPLATE);
        return path.toString();
    }

    @Override
    protected void refresh() {
        platformPath = null;
        serverPath = null;
        tenantsPath = null;
        platformProperties = null;
    }

    public Properties getPlatformProperties() throws BonitaHomeNotSetException, IOException {
        if (platformProperties == null) {
            platformProperties = PropertiesManager.getProperties(getPlaformFile());
        }
        return platformProperties;
    }

}
