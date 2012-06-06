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

import com.maestrodev.plugins.collabnet.frs.FrsSession;
import org.json.simple.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class FrsDeployWorker extends AbstractFrsWorker {

    /**
     * The files to deploy.
     */
    private List<File> files;

    @Override
    public void setWorkitem(JSONObject workitem) {
        super.setWorkitem(workitem);

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

        setupProxy();

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
            String packageId = preparePackage(frsSession);
            String releaseId = prepareRelease(frsSession, packageId);

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

}
