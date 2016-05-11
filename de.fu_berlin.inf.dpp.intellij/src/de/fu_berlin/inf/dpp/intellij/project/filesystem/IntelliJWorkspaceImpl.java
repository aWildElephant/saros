package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileSystem;
import de.fu_berlin.inf.dpp.exceptions.OperationCanceledException;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.filesystem.IWorkspaceRoot;
import de.fu_berlin.inf.dpp.filesystem.IWorkspaceRunnable;
import de.fu_berlin.inf.dpp.intellij.project.FileSystemChangeListener;
import de.fu_berlin.inf.dpp.monitoring.NullProgressMonitor;
import org.apache.log4j.Logger;

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
    private VirtualFileSystem fileSystem;

    public IntelliJWorkspaceImpl(Project project) {
        if (project.getBaseDir() == null) {
            throw new IllegalArgumentException(
                "Cannot create workspace for default project"); // TODO: this breaks some tests.
        }
        this.project = project;
        this.fileSystem = project.getBaseDir().getFileSystem();
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

    @Override
    public IProject getProject(String projectPath) {
        return new IntelliJProjectImpl(this, new File(projectPath));
    }

    /**
     * Returns a handle to the project for the given path.
     */
    public IntelliJProjectImpl getProjectForPath(String path) { // FIXME: fix it
        IPath filePath = IntelliJPathImpl.fromString(path);
        IPath projectPath = IntelliJPathImpl.fromString(project.getBasePath());

        if (!projectPath.isPrefixOf(filePath)) {
            return null;
        }

        IPath relativePath = filePath
            .removeFirstSegments(projectPath.segmentCount());

        return new IntelliJProjectImpl(this, new File(relativePath.segment(0)));
    }

    @Override
    public IPath getLocation() {
        return IntelliJPathImpl.fromString(project.getBasePath());
    }

    public void addResourceListener(FileSystemChangeListener listener) {
        listener.setWorkspace(this);

        fileSystem.addVirtualFileListener(listener);
    }

    public void removeResourceListener(FileSystemChangeListener listener) {
        listener.setWorkspace(this);

        fileSystem.removeVirtualFileListener(listener);
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

        return getLocation() == other.getLocation();
    }
}
