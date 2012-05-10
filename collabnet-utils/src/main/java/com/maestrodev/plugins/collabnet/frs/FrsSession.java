package com.maestrodev.plugins.collabnet.frs;

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

import com.collabnet.ce.soap60.webservices.filestorage.IFileStorageAppSoap;
import com.collabnet.ce.soap60.webservices.frs.FrsFileSoapRow;
import com.collabnet.ce.soap60.webservices.frs.IFrsAppSoap;
import com.collabnet.ce.soap60.webservices.frs.PackageSoapDO;
import com.collabnet.ce.soap60.webservices.frs.PackageSoapList;
import com.collabnet.ce.soap60.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap60.webservices.frs.ReleaseSoapDO;
import com.collabnet.ce.soap60.webservices.frs.ReleaseSoapList;
import com.collabnet.ce.soap60.webservices.frs.ReleaseSoapRow;
import com.maestrodev.plugins.collabnet.ResourceNotFoundException;
import com.maestrodev.plugins.collabnet.log.Log;

import javax.activation.DataHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Create a session interacting with File Releases within a particular
 * {@linkplain com.maestrodev.plugins.collabnet.CollabNetSession}.
 */
public class FrsSession {
    private final IFrsAppSoap frsAppSoap;

    private final IFileStorageAppSoap fileStorageAppSoap;

    private final String sessionId;

    private final String projectId;

    private final Log log;

    public FrsSession(IFrsAppSoap frsAppSoap, IFileStorageAppSoap fileStorageAppSoap, String sessionId, String projectId, Log log) {
        this.frsAppSoap = frsAppSoap;
        this.fileStorageAppSoap = fileStorageAppSoap;
        this.sessionId = sessionId;
        this.projectId = projectId;
        this.log = log;
    }

    /**
     * Find a particular release within the given package, or create it if it could not be found. Searches based on the
     * title in the supplied template.
     *
     * @param release   the release parameters to create if not found
     * @param packageId the package to find or create the release within
     * @return the release ID
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public String findOrCreateRelease(Release release, String packageId) throws RemoteException {
        String releaseId;
        try {
            releaseId = findRelease(release.getTitle(), packageId);
        } catch (ResourceNotFoundException e) {
            releaseId = createRelease(release, packageId);
        }
        return releaseId;
    }

    /**
     * Find a particular release within the given package.
     *
     * @param title     the title of the release to search for
     * @param packageId the package to find the release within
     * @return the release ID
     * @throws RemoteException           if there was a problem communicating with TeamForge
     * @throws ResourceNotFoundException if a release with that title does not exist. The exception message will list the available releases.
     */
    public String findRelease(String title, String packageId) throws RemoteException, ResourceNotFoundException {
        String releaseId = null;

        ReleaseSoapList releaseList = frsAppSoap.getReleaseList(sessionId, packageId);
        ReleaseSoapRow[] releaseRows = releaseList.getDataRows();

        for (ReleaseSoapRow row : releaseRows) {
            if (row.getTitle().equals(title)) {
                releaseId = row.getId();
                log.debug("Found release '" + releaseId + "'");
                break;
            }
        }

        if (releaseId == null) {
            List<String> releases = new ArrayList<String>();
            for (ReleaseSoapRow row : releaseRows) {
                releases.add(row.getTitle());
            }
            throw new ResourceNotFoundException("Unable to find release '" + title + "' in available releases: " + releases);
        }

        return releaseId;
    }

    /**
     * Find a particular release within the project, or create it if it could not be found. Searches based on the
     * title in the supplied template.
     *
     * @param pkg the package parameters to create if not found
     * @return the package ID
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public String findOrCreatePackage(Package pkg) throws RemoteException {
        String packageId;
        try {
            packageId = findPackage(pkg.getTitle());
        } catch (ResourceNotFoundException e) {
            packageId = createPackage(pkg);
        }
        return packageId;
    }

    /**
     * Find a particular package within the project.
     *
     * @param title the title of the package to search for
     * @return the package ID
     * @throws RemoteException           if there was a problem communicating with TeamForge
     * @throws ResourceNotFoundException if a package with that title does not exist. The exception message will list the available packages.
     */
    public String findPackage(String title) throws ResourceNotFoundException, RemoteException {
        String packageId = null;

        PackageSoapList packageList = frsAppSoap.getPackageList(sessionId, projectId);
        PackageSoapRow[] packageRows = packageList.getDataRows();

        for (PackageSoapRow row : packageRows) {
            if (row.getTitle().equals(title)) {
                packageId = row.getId();
                log.debug("Found package '" + packageId + "'");
                break;
            }
        }

        if (packageId == null) {
            List<String> packages = new ArrayList<String>();
            for (PackageSoapRow row : packageRows) {
                packages.add(row.getTitle());
            }
            throw new ResourceNotFoundException("Unable to find package '" + title + "' in available packages: " + packages);
        }

        return packageId;
    }

    /**
     * Create a new release.
     *
     * @param release the release parameters to create it from
     * @return the release ID
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public String createRelease(Release release, String packageId) throws RemoteException {
        ReleaseSoapDO r = frsAppSoap.createRelease(sessionId, packageId, release.getTitle(), release.getDescription(),
                release.getStatus(), release.getMaturity());
        String releaseId = r.getId();
        log.info("Created release '" + release.getTitle() + "' (id: " + releaseId + ")");
        return releaseId;
    }

    /**
     * Create a new package.
     *
     * @param pkg the package parameters to create it from
     * @return the package ID
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public String createPackage(Package pkg) throws RemoteException {
        PackageSoapDO p = frsAppSoap.createPackage(sessionId, projectId, pkg.getTitle(), pkg.getDescription(), true);
        String packageId = p.getId();
        log.info("Created package '" + pkg.getTitle() + "' (id: " + packageId + ")");
        return packageId;
    }

    /**
     * Upload a file to a release. Note that overwriting is not atomic - the release will first be deleted if it exists,
     * and if there is a subsequent problem uploading then the previous release can not be restored.
     *
     * @param releaseId the ID of the release to upload the file to
     * @param file      the file to upload
     * @param overwrite whether to overwrite the file if it already exists. If <code>false</code>, a
     *                  {@link RemoteException} will be thrown with an error message if it already exists.
     * @return the ID of the file
     * @throws RemoteException       if there was a problem communicating with TeamForge
     * @throws MalformedURLException if the file could not be converted into an URL
     */
    public String uploadFile(String releaseId, File file, boolean overwrite) throws RemoteException, MalformedURLException {
        return uploadFileFromUrl(releaseId, file.toURI().toURL(), file.getName(), overwrite);
    }

    /**
     * Upload a file to a release from an URL. Note that overwriting is not atomic - the release will first be deleted
     * if it exists, and if there is a subsequent problem uploading then the previous release can not be restored.
     *
     * @param releaseId the ID of the release to upload the file to
     * @param url       the URL to upload the file from
     * @param overwrite whether to overwrite the file if it already exists. If <code>false</code>, a
     *                  {@link RemoteException} will be thrown with an error message if it already exists.
     * @return the ID of the file
     * @throws RemoteException if there was a problem communicating with TeamForge
     */
    public String uploadFileFromUrl(String releaseId, URL url, String name, boolean overwrite) throws RemoteException {
        DataHandler dh = new DataHandler(url);
        String id = fileStorageAppSoap.uploadFile(sessionId, dh);

        if (overwrite) {
            // check if the file exists first and delete it - SOAP API doesn't appear to have a way to replace
            // or update a file like the web UI does
            for (FrsFileSoapRow row : frsAppSoap.getFrsFileList(sessionId, releaseId).getDataRows()) {
                if (row.getFilename().equals(name)) {
                    log.debug("Removing existing file '" + row.getId() + "'");
                    frsAppSoap.deleteFrsFile(sessionId, row.getId());
                    break;
                }
            }
        }
        // if overwrite is not set, the attempt to create a file will fail

        log.debug("Associating file '" + name + "' with release '" + releaseId + "'");
        frsAppSoap.createFrsFile(sessionId, releaseId, name, dh.getContentType(), id);

        return id;
    }
}
