/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.invitation.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import de.fu_berlin.inf.dpp.FileList;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.editor.internal.EditorAPI;
import de.fu_berlin.inf.dpp.invitation.IIncomingInvitationProcess;
import de.fu_berlin.inf.dpp.net.ITransmitter;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.DataTransferManager;
import de.fu_berlin.inf.dpp.project.ISharedProject;
import de.fu_berlin.inf.dpp.project.SessionManager;
import de.fu_berlin.inf.dpp.ui.ErrorMessageDialog;
import de.fu_berlin.inf.dpp.util.Util;

/**
 * An incoming invitation process.
 * 
 * @author rdjemili
 */
public class IncomingInvitationProcess extends InvitationProcess implements
    IIncomingInvitationProcess {

    private static Logger log = Logger
        .getLogger(IncomingInvitationProcess.class);

    protected FileList remoteFileList;

    protected IProject localProject;

    protected int filesLeftToSynchronize;

    /** size of current transfered part of archive file. */
    protected int transferedFileSize = 0;

    protected IProgressMonitor progressMonitor;

    protected String projectName;

    protected SessionManager sessionManager;

    protected DataTransferManager dataTransferManager;

    public IncomingInvitationProcess(SessionManager sessionManager,
        ITransmitter transmitter, DataTransferManager dataTransferManager,
        JID from, String projectName, String description, int colorID) {

        super(transmitter, from, description, colorID);

        this.sessionManager = sessionManager;
        this.projectName = projectName;
        this.dataTransferManager = dataTransferManager;

        setState(State.INVITATION_SENT);

    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.IInvitationProcess
     */
    public void fileListReceived(JID from, FileList fileList) {

        log.debug("Received file list from " + from.getBase());
        assertState(State.HOST_FILELIST_REQUESTED);

        if (fileList == null) {
            cancel("Failed to receive remote file list.", false);
        } else {
            setState(State.HOST_FILELIST_SENT);
            this.remoteFileList = fileList;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.IIncomingInvitationProcess
     */
    public void requestRemoteFileList(IProgressMonitor monitor)
        throws InterruptedException {
        assertState(State.INVITATION_SENT);

        monitor.beginTask("Requesting remote file list",
            IProgressMonitor.UNKNOWN);

        try {
            monitor.subTask("Initializing Jingle");

            this.transmitter.awaitJingleManager(this.peer);

            if (monitor.isCanceled()) {
                cancel(null, false);
                throw new InterruptedException();
            }
            if (this.state == State.CANCELED) {
                return;
            }

            monitor.subTask("Sending request for remote file list");

            setState(State.HOST_FILELIST_REQUESTED);

            this.transmitter.sendRequestForFileListMessage(this.peer);

            monitor.subTask("Waiting for remote file list");

            while (this.remoteFileList == null) {

                if (monitor.isCanceled()) {
                    cancel(null, false);
                    throw new InterruptedException();
                }
                if (this.state == State.CANCELED) {
                    return;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // Only warn, because we can reasonable deal with IE
                    log.warn("Code not intended to be interrupted", e);
                    cancel(null, false);
                    throw e;
                }
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.IIncomingInvitationProcess
     */
    public void accept(final IProject baseProject, final String newProjectName,
        IProgressMonitor monitor) {

        if ((newProjectName == null) && (baseProject == null)) {
            throw new IllegalArgumentException(
                "At least newProjectName or baseProject have to be not null.");
        }

        try {
            if (acceptUnsafe(baseProject, newProjectName, monitor)) {
                done();
            } else {
                cancel(null, false);
            }
        } catch (CoreException e) {
            ErrorMessageDialog.showErrorMessage(new Exception(
                "Exception during create project.", e));
            failed(e);
        } catch (IOException e) {
            ErrorMessageDialog.showErrorMessage(new Exception(
                "Exception during create project.", e));
            failed(e);
        } catch (RuntimeException e) {
            ErrorMessageDialog.showErrorMessage(new Exception(
                "Exception during create project.", e));
            failed(e);
        } finally {
            monitor.done();
        }
    }

    private boolean acceptUnsafe(final IProject baseProject,
        final String newProjectName, IProgressMonitor monitor)
        throws CoreException, IOException {
        assertState(State.HOST_FILELIST_SENT);

        // If a base project is given, save it
        if (baseProject != null) {
            if (!EditorAPI.saveProject(baseProject)) {
                // User canceled saving the source project
                return false;
            }
        }

        if (newProjectName != null) {

            try {
                this.localProject = Util.runSWTSync(new Callable<IProject>() {
                    public IProject call() throws CoreException,
                        InterruptedException {
                        return createNewProject(newProjectName, baseProject);

                    }
                });
            } catch (CoreException e) {
                // Eclipse reported an error
                throw e;
            } catch (InterruptedException e) {
                // @InterrupteExceptionOK - Method uses IE to signal cancelation
                return false;
            } catch (Exception e) {
                // We are probably at fault!
                throw new RuntimeException(e);
            }

        } else {
            this.localProject = baseProject;
        }

        this.filesLeftToSynchronize = handleDiff(this.localProject,
            this.remoteFileList);

        this.progressMonitor = monitor;

        if (dataTransferManager.getIncomingTransferMode(getPeer()).isP2P()) {
            this.progressMonitor.beginTask("Synchronizing",
                this.filesLeftToSynchronize);
        } else {
            this.progressMonitor.beginTask("Synchronizing",
                100 + this.filesLeftToSynchronize);
            this.progressMonitor.subTask("Receiving Archive...");
        }
        setState(State.SYNCHRONIZING);

        // Disable Autobuilding while we receive files
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc = ws.getDescription();
        boolean wasAutobuilding = desc.isAutoBuilding();
        if (wasAutobuilding) {
            desc.setAutoBuilding(false);
            ws.setDescription(desc);
        }

        try {
            this.transmitter.sendFileList(this.peer, new FileList(
                this.localProject), this);

            return blockUntilAllFilesSynchronized(monitor);
        } finally {
            // Reenable Autobuilding...
            if (wasAutobuilding) {
                desc.setAutoBuilding(true);
                ws.setDescription(desc);
            }
        }
    }

    public void invitationAccepted(JID from) {
        failState();
    }

    public void joinReceived(JID from) {
        failState();
    }

    public void resourceReceived(JID from, IPath path, InputStream in) {
        IncomingInvitationProcess.log.debug("new file received: " + path);
        if (this.localProject == null || this.progressMonitor.isCanceled()) {
            return; // we do not have started the new project yet, so received
            // resources are not welcomed
        }

        try {
            IFile file = this.localProject.getFile(path);
            if (file.exists()) {
                ResourceAttributes attributes = new ResourceAttributes();
                attributes.setReadOnly(false);
                file.setResourceAttributes(attributes);
                file
                    .setContents(in, IResource.FORCE, new NullProgressMonitor());

                // TODO Set ReadOnly again?
            } else {
                file.create(in, true, new NullProgressMonitor());
                IncomingInvitationProcess.log.debug("New File created: "
                    + file.getName());
            }

        } catch (Exception e) {
            failed(e);
        }

        // Could have been canceled in the meantime
        if (this.progressMonitor.isCanceled())
            return;

        this.progressMonitor.worked(1);
        this.progressMonitor.subTask("Files left: "
            + this.filesLeftToSynchronize);

        this.filesLeftToSynchronize--;
        IncomingInvitationProcess.log.debug("file counter: "
            + this.filesLeftToSynchronize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.IIncomingInvitationProcess
     */
    public FileList getRemoteFileList() {
        return this.remoteFileList;
    }

    /**
     * Blocks until all files have been synchronized or cancel has been
     * selected.
     * 
     * @return <code>true</code> if all files were synchronized.
     *         <code>false</code> if operation was canceled by user.
     */
    private boolean blockUntilAllFilesSynchronized(IProgressMonitor monitor) {

        // Make sure that cancelation by the user is at least checked once
        do {
            // Operation canceled by the local user
            if (monitor.isCanceled()) {
                return false;
            }

            // Operation canceled by state change
            if (getState() == State.CANCELED) {
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.warn("Code not designed to handle InterruptedException", e);
                Thread.currentThread().interrupt();
                return false;
            }

        } while (this.filesLeftToSynchronize > 0);

        return true;
    }

    /**
     * Creates a new project.
     * 
     * @param newProjectName
     *            the project name of the new project.
     * @param baseProject
     *            if not <code>null</code> all files of the baseProject will be
     *            copied into the new project after having created it.
     * @return the new project.
     * @throws CoreException
     *             if something goes wrong while creating the new project.
     * @throws InterruptedException
     *             If this operation was canceled by the user.
     * 
     * @swt Needs to be run from the SWT UI Thread
     */
    private static IProject createNewProject(String newProjectName,
        final IProject baseProject) throws CoreException, InterruptedException {

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        final IProject project = workspaceRoot.getProject(newProjectName);

        // TODO Why do some string magic here?
        final File projectDir = new File(workspaceRoot.getLocation().toString()
            + File.separator + newProjectName);

        if (projectDir.exists()) {
            throw new CoreException(new Status(IStatus.ERROR, Saros.SAROS,
                "Project " + newProjectName + " already exists!"));
        }

        ProgressMonitorDialog dialog = new ProgressMonitorDialog(EditorAPI
            .getAWorkbenchWindow().getShell());

        try {
            dialog.run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                    try {

                        SubMonitor subMonitor = SubMonitor.convert(monitor,
                            "Copy local resources ... ", 200);

                        project.clearHistory(null);
                        project.refreshLocal(IResource.DEPTH_INFINITE, null);

                        if (baseProject == null) {
                            project.create(subMonitor.newChild(100));
                            project.open(subMonitor.newChild(100));
                        } else {
                            baseProject.copy(project.getFullPath(), true,
                                subMonitor.newChild(200));
                        }
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }

            });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof CoreException) {
                throw (CoreException) e.getCause();
            } else {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        return project;
    }

    /**
     * Prepares for receiving the missing resources.
     * 
     * @param localProject
     *            the project that is used for the base of the replication.
     * @param remoteFileList
     *            the file list of the remote project.
     * @return the number of files that we need to receive to end the
     *         synchronization.
     * @throws CoreException
     *             is thrown when getting all files of the local project.
     */
    private int handleDiff(IProject localProject, FileList remoteFileList)
        throws CoreException {

        FileList diff = new FileList(localProject).diff(remoteFileList);

        removeUnneededResources(localProject, diff);
        int addedPaths = addAllFolders(localProject, diff);

        return diff.getAddedPaths().size() - addedPaths
            + diff.getAlteredPaths().size();
    }

    /**
     * Removes all local resources that aren't part of the shared project we're
     * currently joining. This includes files and folders.
     * 
     * @param localProject
     *            the local project were the shared project will be replicated.
     * @param diff
     *            the fileList which contains the diff information.
     * @throws CoreException
     */
    private void removeUnneededResources(IProject localProject, FileList diff)
        throws CoreException {

        // TODO don't throw CoreException
        // TODO check if this triggers the resource listener
        for (IPath path : diff.getRemovedPaths()) {
            if (path.hasTrailingSeparator()) {
                IFolder folder = localProject.getFolder(path);

                if (folder.exists()) {
                    folder.delete(true, new NullProgressMonitor());
                }

            } else {
                IFile file = localProject.getFile(path);

                // check if file exists because it might have already been
                // deleted when deleting its folder
                if (file.exists()) {
                    file.delete(true, new NullProgressMonitor());
                }
            }
        }
    }

    private int addAllFolders(IProject localProject, FileList diff)
        throws CoreException {

        int addedFolders = 0;

        for (IPath path : diff.getAddedPaths()) {
            if (path.hasTrailingSeparator()) {
                IFolder folder = localProject.getFolder(path);
                if (!folder.exists()) {
                    folder.create(true, true, new NullProgressMonitor());
                }

                addedFolders++;
            }
        }

        return addedFolders;
    }

    /**
     * Ends the incoming invitation process.
     */
    private void done() {
        JID host = this.peer;

        ISharedProject sharedProject = sessionManager.joinSession(
            this.localProject, host, colorID);

        // TODO joining the session will already send events, which will be
        // rejected by our peers, because they don't know us yet (JoinMessage is
        // send only later)

        // TODO Will block 1000 ms to ensure something...
        this.transmitter.sendJoinMessage(sharedProject);
        this.transmitter.removeInvitationProcess(this);

        sharedProject.setProjectReadonly(!sharedProject.isDriver());

        // Starting the project here is a HACK to workaround the first todo
        // above
        sharedProject.start();

        setState(State.DONE);
    }

    public String getProjectName() {
        return this.projectName;
    }

    public void fileSent(IPath path) {
        // do nothing
    }

    public void fileTransferFailed(IPath path, Exception e) {
        failed(e);
    }

    public void transferProgress(int transfered) {
        this.progressMonitor.worked(transfered - this.transferedFileSize);
        this.transferedFileSize = transfered;
    }

    @Override
    public void cancel(String errorMsg, boolean replicated) {
        super.cancel(errorMsg, replicated);

        sessionManager.cancelIncomingInvitation();
    }
}
