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

import com.maestrodev.MaestroWorker;
import com.maestrodev.plugins.collabnet.frs.FrsSession;
import com.maestrodev.plugins.collabnet.frs.Package;
import com.maestrodev.plugins.collabnet.frs.Release;
import com.maestrodev.plugins.collabnet.log.Log;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class FrsDeployWorker extends MaestroWorker {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The package (product) to deploy the files to. If it can not be found, it will be created, unless
     * <code>{@linkplain #createRelease}</code> is <code>false</code>.
     *
     * TODO: default to the name of the current project in Maestro
     */
    private String pkg;

    /**
     * The name of the release to deploy the files to.  If it can not be found it will be created, unless
     * <code>{@linkplain #createRelease}</code> is <code>false</code>.
     */
    private String release;

    /**
     * The name of the CollabNet TeamForge project to deploy the files to.
     */
    private String project;

    /**
     * The URL of the TeamForge instance to deploy to.
     */
    private String teamForgeUrl;

    /**
     * The username to login to TeamForge with.
     */
    private String teamForgeUsername;

    /**
     * The password to login to TeamForge with.
     */
    private String teamForgePassword;

    /**
     * Whether to create the release on CollabNet if it does not already exist.
     */
    private boolean createRelease;

    /**
     * The description to assign to the package, if it is created.
     *
     * TODO: default to the description of the current project in Maestro
     */
    private String packageDescription;

    /**
     * The description to assign to the release, if it is created.
     */
    private String releaseDescription;

    /**
     * The status to assign to the release, if it is created. Can be <code>active</code> or <code>pending</code>.
     *
     * TODO: create an enumerated type in Maestro plugin
     */
    private String releaseStatus;

    /**
     * The maturity to assign to the release, if it is created.
     */
    private String releaseMaturity;

    /**
     * Whether to overwrite the file if it already exists.
     */
    private boolean overwrite;

    /**
     * The files to deploy.
     */
    private List<File> files;

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

        List<File> files = new ArrayList<File>();
        for (String file : getArrayField(String.class, "files")) {
            files.add(new File(file));
        }
        this.files = files;
    }

    public void frsDeploy() {
        try {
            verifyConfiguration();
        } catch (IllegalArgumentException e) {
            logger.info(e.getLocalizedMessage());
            setError(e.getLocalizedMessage() + "\n");
            return;
        }

        CollabNetSession session;
        try {
            session = new CollabNetSession(teamForgeUrl, teamForgeUsername, teamForgePassword, new MaestroPluginLog());
        } catch (RemoteException e) {
            String msg = "Failed to login to TeamForge: " + e.getLocalizedMessage();
            logger.error(msg, e);
            setError(msg + "\n");
            return;
        }

        String projectId;
        try {
            projectId = session.findProject(project);
        } catch (RemoteException e) {
            logger.error("Exception retrieving TeamForge project: " + e.getLocalizedMessage(), e);
            setError("Failed to retrieve TeamForge project '" + project + "': " + e.getLocalizedMessage() + "\n");
            return;
        }
        logger.debug("Found CollabNet project '" + projectId + "'");
        setField("projectId", projectId);

        try {
            FrsSession frsSession = session.createFrsSession(projectId);
            String packageId;
            String releaseId;
            if (createRelease) {
                packageId = frsSession.findOrCreatePackage(createPackageTemplate());
                releaseId = frsSession.findOrCreateRelease(createReleaseTemplate(), packageId);
            } else {
                packageId = frsSession.findPackage(pkg);
                releaseId = frsSession.findRelease(release, packageId);
            }

            setField("packageId", packageId);
            setField("releaseId", releaseId);

            List<String> fileIds = uploadArtifacts(files, releaseId, frsSession);
            setField("fileIds", fileIds);
        } catch (RemoteException e) {
            String msg = e.getLocalizedMessage();
            logger.error(msg, e);
            setError(msg + "\n");
        } catch (MalformedURLException e) {
            String msg = e.getLocalizedMessage();
            logger.error(msg, e);
            setError(msg + "\n");
        } catch (ResourceNotFoundException e) {
            String msg = e.getLocalizedMessage();
            logger.error(msg, e);
            setError(msg + "\n");
        } finally {
            logoff(session);
        }
    }

    private void verifyConfiguration() throws IllegalArgumentException {
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

    private com.maestrodev.plugins.collabnet.frs.Package createPackageTemplate() {
        com.maestrodev.plugins.collabnet.frs.Package template = new Package();
        template.setTitle(pkg);
        template.setDescription(packageDescription);
        return template;
    }

    private Release createReleaseTemplate() {
        Release template = new Release();
        template.setTitle(release);
        template.setDescription(releaseDescription);
        template.setMaturity(releaseMaturity);
        template.setStatus(releaseStatus);
        return template;
    }

    private void logoff(CollabNetSession session) {
        try {
            session.logoff();
        } catch (RemoteException e) {
            logger.error("Error logging off from CollabNet TeamForge (ignoring): " + e.getLocalizedMessage(), e);
        }
    }

    private List<String> uploadArtifacts(List<File> files, String releaseId, FrsSession frsSession) throws MalformedURLException, RemoteException {
        List<String> fileIds = new ArrayList<String>(files.size());
        for (File file : files) {
            String msg = "Uploading '" + file + "' to release '" + releaseId + "'";
            logger.debug(msg);
            writeOutput(msg + "\n");
            String fileId = frsSession.uploadFile(releaseId, file, overwrite);
            fileIds.add(fileId);
        }
        return fileIds;
    }

    private class MaestroPluginLog implements Log {
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
