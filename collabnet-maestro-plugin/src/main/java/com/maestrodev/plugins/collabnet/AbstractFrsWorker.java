package com.maestrodev.plugins.collabnet;

/*
 * Copyright 2012 MaestroDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.btr.proxy.search.ProxySearch;
import com.maestrodev.MaestroWorker;
import com.maestrodev.plugins.collabnet.frs.FrsSession;
import com.maestrodev.plugins.collabnet.frs.Release;
import com.maestrodev.plugins.collabnet.log.Log;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ProxySelector;
import java.rmi.RemoteException;
import java.text.MessageFormat;

public abstract class AbstractFrsWorker extends MaestroWorker {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * The package (product) to deploy the files to. If it can not be found, it will be created, unless
     * <code>{@linkplain #createRelease}</code> is <code>false</code>.
     *
     * TODO: default to the name of the current project in Maestro
     */
    protected String pkg;
    /**
     * The name of the release to deploy the files to.  If it can not be found it will be created, unless
     * <code>{@linkplain #createRelease}</code> is <code>false</code>.
     */
    protected String release;
    /**
     * The name of the CollabNet TeamForge project to deploy the files to.
     */
    protected String project;
    /**
     * The URL of the TeamForge instance to deploy to.
     */
    protected String teamForgeUrl;
    /**
     * The username to login to TeamForge with.
     */
    protected String teamForgeUsername;
    /**
     * The password to login to TeamForge with.
     */
    protected String teamForgePassword;
    /**
     * Whether to create the release on CollabNet if it does not already exist.
     */
    protected boolean createRelease;
    /**
     * The description to assign to the package, if it is created.
     *
     * TODO: default to the description of the current project in Maestro
     */
    protected String packageDescription;
    /**
     * The description to assign to the release, if it is created.
     */
    protected String releaseDescription;
    /**
     * The status to assign to the release, if it is created. Can be <code>active</code> or <code>pending</code>.
     *
     * TODO: create an enumerated type in Maestro plugin
     */
    protected String releaseStatus;
    /**
     * The maturity to assign to the release, if it is created.
     */
    protected String releaseMaturity;
    /**
     * Whether to overwrite the file if it already exists.
     */
    protected boolean overwrite;

    @Override
    public void setWorkitem(JSONObject workitem) {
        super.setWorkitem(workitem);

        this.pkg = getField("package");
        this.release = getField("release");
        this.project = getField("project");
        this.teamForgeUrl = getField("teamForgeUrl");
        this.teamForgeUsername = getField("teamForgeUsername");
        this.teamForgePassword = getField("teamForgePassword");
        this.createRelease = Boolean.valueOf(getField("createRelease"));
        this.packageDescription = getField("packageDescription");
        this.releaseDescription = getField("releaseDescription");
        this.releaseStatus = getField("releaseStatus");
        this.releaseMaturity = getField("releaseMaturity");
        if (this.releaseMaturity == null) {
            this.releaseMaturity = "";
        }

        this.overwrite = Boolean.valueOf(getField("overwrite"));
    }

    protected void verifyConfiguration() throws IllegalArgumentException {
        if (teamForgeUsername == null) {
            throw new IllegalArgumentException("TeamForge username must be specified");
        }
        if (teamForgePassword == null) {
            throw new IllegalArgumentException("TeamForge password must be specified");
        }

        if (!"active".equals(releaseStatus) && !"pending".equals(releaseStatus)) {
            throw new IllegalArgumentException("Release status must be 'active' or 'pending', but is: '" + releaseStatus + "'");
        }
    }

    protected com.maestrodev.plugins.collabnet.frs.Package createPackageTemplate() {
        com.maestrodev.plugins.collabnet.frs.Package template = new com.maestrodev.plugins.collabnet.frs.Package();
        template.setTitle(pkg);
        template.setDescription(packageDescription);
        return template;
    }

    protected Release createReleaseTemplate() {
        Release template = new Release();
        template.setTitle(release);
        template.setDescription(releaseDescription);
        template.setMaturity(releaseMaturity);
        template.setStatus(releaseStatus);
        return template;
    }

    protected void logoff(CollabNetSession session) {
        try {
            session.logoff();
        } catch (RemoteException e) {
            logger.error("Error logging off from CollabNet TeamForge (ignoring): " + e.getLocalizedMessage(), e);
        }
    }

    protected String preparePackage(FrsSession frsSession) throws RemoteException, ResourceNotFoundException {
        String packageId;
        if (createRelease) {
            packageId = frsSession.findOrCreatePackage(createPackageTemplate());
        } else {
            packageId = frsSession.findPackage(pkg);
        }
        setField("packageId", packageId);
        return packageId;
    }

    protected String prepareRelease(FrsSession frsSession, String packageId) throws RemoteException, ResourceNotFoundException {
        String releaseId;
        if (createRelease) {
            releaseId = frsSession.findOrCreateRelease(createReleaseTemplate(), packageId);
        } else {
            releaseId = frsSession.findRelease(release, packageId);
        }
        setField("releaseId", releaseId);
        return releaseId;
    }

    protected void setupProxy() {
        ProxySearch proxySearch = new ProxySearch();

        // put JAVA after the others, as it will apply even if it's not set...
        proxySearch.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
        proxySearch.addStrategy(ProxySearch.Strategy.ENV_VAR);
        proxySearch.addStrategy(ProxySearch.Strategy.JAVA);
        com.btr.proxy.util.Logger.setBackend(new com.btr.proxy.util.Logger.LogBackEnd() {

            public void log(Class<?> clazz, com.btr.proxy.util.Logger.LogLevel loglevel, String msg,
                            Object... params) {
                logger.debug(MessageFormat.format(msg, params));
            }

            public boolean isLogginEnabled(com.btr.proxy.util.Logger.LogLevel logLevel) {
                return logger.isDebugEnabled();
            }
        });
        ProxySelector myProxySelector = proxySearch.getProxySelector();

        ProxySelector.setDefault(myProxySelector);
    }

    protected class MaestroPluginLog implements Log {
        public void debug(String msg) {
            logger.debug(msg);

            // don't write to output
        }

        public void info(String msg) {
            // debug only to console log
            logger.debug(msg);

            writeOutput(msg + "\n");
        }
    }
}
