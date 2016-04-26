package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.exceptions.OperationCanceledException;
import de.fu_berlin.inf.dpp.filesystem.*;
import de.fu_berlin.inf.dpp.intellij.project.FileSystemChangeListener;
import de.fu_berlin.inf.dpp.monitoring.NullProgressMonitor;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * IntelliJ's implementation of the IWorkspace interface.
 * <p/>
 * This class wraps and represents an IntelliJ {@link Project} instance.
 */
public class IntelliJWorkspaceImpl implements IWorkspace {
    public static final Logger LOG = Logger
        .getLogger(IntelliJWorkspaceImpl.class);

    private Project project;
    private ISarosSessionManager sessionManager;

    public IntelliJWorkspaceImpl(@NotNull Project project, @NotNull ISarosSessionManager sessionManager) {
        this.project = project;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run(IWorkspaceRunnable procedure)
        throws IOException, OperationCanceledException {
        procedure.run(new NullProgressMonitor());
    }

    @Override
    public void run(IWorkspaceRunnable runnable, IResource[] resources)
        throws IOException, OperationCanceledException {
        run(runnable);
    }

    /**
     * Return an IProject instance containing the given path.
     *
     * @param projectPath a workspace-relative path.
     * @return an IProject containing the given path, or <code>null</code> if it doesn't exist.
     */
    @Override
    public IProject getProject(String projectPath) {
        return getProjectForPath(getLocation().append(projectPath).toPortableString());
    }

    /**
     * Returns a handle to the project for the given path.
     */
    public IntelliJProjectImpl getProjectForPath(String path) {
        // If in a session, we prioritize shared projects
        ISarosSession session = sessionManager.getSarosSession();
        if (session != null) {
            IPath resourcePath = IntelliJPathImpl.fromString(path);
            for (IProject sharedProject : session.getProjects()) {
                if (sharedProject.getLocation().isPrefixOf(resourcePath)) {
                    return (IntelliJProjectImpl) sharedProject;
                }
            }
            if (!session.isHost() && getLocation().isPrefixOf(resourcePath)) { // Dirty fix, better to implement the out-of-project-basedir scheme
                // Create an IProject at the base of the IntelliJ project, with a name matching the given path
                return new IntelliJProjectImpl(this, new File(resourcePath.removeFirstSegments(getLocation().segmentCount()).segment(0)));
            }
        }

        /* If there is no active session or the given path do not belong to any shared project,
         * create a IProject for the content root containing the path. */
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file == null) {
            return null;
        }

        VirtualFile root = ProjectFileIndex.SERVICE.getInstance(project).getContentRootForFile(file);
        if (root == null) {
            return null;
        }

        return new IntelliJProjectImpl(this, new File(root.getPath()));
    }

    @Override
    public IPath getLocation() {
        return IntelliJPathImpl.fromString(project.getBasePath());
    }

    public void addResourceListener(FileSystemChangeListener listener) {
        listener.setWorkspace(this);

        LocalFileSystem.getInstance().addVirtualFileListener(listener);
    }

    public void removeResourceListener(FileSystemChangeListener listener) {
        listener.setWorkspace(this);

        LocalFileSystem.getInstance().removeVirtualFileListener(listener);
    }

    public IWorkspaceRoot getRoot() {
        return new IntelliJWorkspaceRootImpl(this);
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntelliJWorkspaceImpl)) {
            return false;
        }

        IntelliJWorkspaceImpl other = (IntelliJWorkspaceImpl) o;

        return getLocation().equals(other.getLocation());
    }
}
