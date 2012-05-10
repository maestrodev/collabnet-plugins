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

import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.cemain.ProjectSoapDO;
import com.collabnet.ce.soap60.webservices.filestorage.IFileStorageAppSoap;
import com.collabnet.ce.soap60.webservices.frs.IFrsAppSoap;
import com.maestrodev.plugins.collabnet.frs.FrsSession;
import com.maestrodev.plugins.collabnet.log.Log;
import com.maestrodev.plugins.collabnet.log.Slf4jLog;

import java.rmi.RemoteException;

/**
 * Captures a session interacting with CollabNet TeamForge via the SOAP API. Logs in using the username and password
 * supplied for the given TeamForge URL.
 *
 * This object can be used to obtain projects, and sessions on other subsystems such as the FRS.
 *
 * When finished with the session, ensure that you call the {@linkplain #logoff()} method.
 */
public class CollabNetSession {
    private final ICollabNetSoap collabNetSoap;

    private final String sessionId;

    private final String teamForgeUrl;

    private final String teamForgeUsername;

    private final Log log;

    /**
     * Create a new session and log in. Log messages will be directed to SLF4J.
     *
     * @param teamForgeUrl      the URL of the TeamForge instance to log in to
     * @param teamForgeUsername the username to log in with
     * @param teamForgePassword the password to log in with (not stored)
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    @SuppressWarnings("UnusedDeclaration")
    public CollabNetSession(String teamForgeUrl, String teamForgeUsername, String teamForgePassword) throws RemoteException {
        this(teamForgeUrl, teamForgeUsername, teamForgePassword, new Slf4jLog());
    }

    /**
     * Create a new session and log in, directing log messages to the given {@linkplain Log} implementation.
     *
     * @param teamForgeUrl      the URL of the TeamForge instance to log in to
     * @param teamForgeUsername the username to log in with
     * @param teamForgePassword the password to log in with (not stored)
     * @param log               the log implementation to use
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public CollabNetSession(String teamForgeUrl, String teamForgeUsername, String teamForgePassword, Log log) throws RemoteException {
        collabNetSoap =
                (ICollabNetSoap) ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, teamForgeUrl);
        this.teamForgeUrl = teamForgeUrl;
        this.teamForgeUsername = teamForgeUsername;
        this.log = log;

        log.debug("Logging in to TeamForge for user '" + teamForgeUsername + "' at " + teamForgeUrl);
        sessionId = collabNetSoap.login(teamForgeUsername, teamForgePassword);
        log.debug("Session created: '" + sessionId + "'");
    }

    /**
     * Find a project by it's path in CollabNet. The project would be found at the URL
     * <code><i>teamForgeUrl</i>/sf/projects/<i>project</i>/</code>.
     *
     * @param project the project name (<code>projects.</code> will be automatically prepended)
     * @return the project ID
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public String findProject(String project) throws RemoteException {
        ProjectSoapDO p = collabNetSoap.getProjectByPath(sessionId, "projects." + project);
        return p.getId();
    }

    /**
     * Start a session to interact with the File Releases (FRS) subsystem, for a given project.
     *
     * @param projectId the project ID that will be used for the session
     * @return the FRS session
     */
    public FrsSession createFrsSession(String projectId) {
        IFrsAppSoap frsAppSoap = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, teamForgeUrl);
        IFileStorageAppSoap fileStorageAppSoap = (IFileStorageAppSoap) ClientSoapStubFactory.getSoapStub(
                IFileStorageAppSoap.class, teamForgeUrl);

        return new FrsSession(frsAppSoap, fileStorageAppSoap, sessionId, projectId, log);
    }

    /**
     * Log out of the CollabNet session.
     *
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public void logoff() throws RemoteException {
        log.debug("Logging out of TeamForge session '" + sessionId + "' for user '" + teamForgeUsername + "'");
        collabNetSoap.logoff(teamForgeUsername, sessionId);
    }
}
