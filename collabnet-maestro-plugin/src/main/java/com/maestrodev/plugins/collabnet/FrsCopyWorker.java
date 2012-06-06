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
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.json.simple.JSONObject;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.rmi.RemoteException;

public class FrsCopyWorker extends AbstractFrsWorker {

    /**
     * The URL of the repository to copy the artifact from.
     */
    private String repositoryUrl;

    /**
     * The username to authenticate to the repository if necessary.
     */
    private String repositoryUsername;

    /**
     * The password to authenticate to the repository if necessary.
     */
    private String repositoryPassword;

    /**
     * The group ID of the artifact to copy.
     */
    private String artifactGroupId;

    /**
     * The artifact ID of the artifact to copy.
     */
    private String artifactId;

    /**
     * The version of the artifact to copy.
     */
    private String artifactVersion;

    /**
     * The type of the artifact to copy.
     */
    private String artifactType;

    /**
     * The (optional) classifier of the artifact to copy.
     */
    private String artifactClassifier;

    @Override
    public void setWorkitem(JSONObject workitem) {
        super.setWorkitem(workitem);

        this.repositoryUrl = getField("repositoryUrl");
        this.repositoryUsername = getField("repositoryUsername");
        this.repositoryPassword = getField("repositoryPassword");
        this.artifactGroupId = getField("artifactGroupId");
        this.artifactId = getField("artifactId");
        this.artifactVersion = getField("artifactVersion");
        this.artifactType = getField("artifactType");
        this.artifactClassifier = getField("artifactClassifier");

        if (StringUtils.isBlank(this.release)) {
            this.release = this.artifactVersion;
        }
    }

    public void frsCopy() {
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
            String packageId = preparePackage(frsSession);
            String releaseId = prepareRelease(frsSession, packageId);

            String fileId = copyArtifact(frsSession, releaseId);
            setField("fileId", fileId);
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

    private String copyArtifact(FrsSession frsSession, String releaseId) throws MalformedURLException, RemoteException {
        Artifact artifact = new DefaultArtifact(artifactGroupId, artifactId,
                VersionRange.createFromVersion(artifactVersion), null, artifactType, artifactClassifier,
                new DefaultArtifactHandler(artifactType));
        String path = new DefaultRepositoryLayout().pathOf(artifact);
        String url = repositoryUrl + "/" + path;

        String msg = "Uploading '" + url + "' to release '" + releaseId + "'";
        logger.debug(msg);
        writeOutput(msg + "\n");

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(repositoryUsername, repositoryPassword.toCharArray());
            }
        });

        return frsSession.uploadFileFromUrl(releaseId, new URL(url), path.substring(path.lastIndexOf('/') + 1), overwrite);
    }
}
