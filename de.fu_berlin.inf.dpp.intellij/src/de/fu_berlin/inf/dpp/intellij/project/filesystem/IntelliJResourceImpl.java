package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.filesystem.IResourceAttributes;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * IntelliJ's implementation of the IResource interface.
 */
public abstract class IntelliJResourceImpl implements IResource {

    //TODO resolve charset issue by reading real data
    public static final String DEFAULT_CHARSET = "utf8";
    private static final Logger LOG = Logger
            .getLogger(IntelliJResourceImpl.class);

    protected IntelliJWorkspaceImpl workspace;
    protected IPath location;
    private IResourceAttributes attributes;

    protected IntelliJResourceImpl(
        @NotNull
        IntelliJWorkspaceImpl workspace,
        @NotNull
        File file) {

        if (file.isAbsolute()) {
            this.location = IntelliJPathImpl.fromString(file.getPath());
        } else {
            this.location = workspace.getLocation().append(file.getPath());
        }

        this.workspace = workspace;
        this.attributes = new IntelliJFileResourceAttributesImpl(file);
    }

    public String getDefaultCharset() {
        return DEFAULT_CHARSET;
    }

    public IntelliJWorkspaceImpl getWorkspace() {
        return workspace;
    }

    @Override
    public boolean exists() {
        return toFile().exists();
    }

    @Override
    public IPath getFullPath() {
        return ((IntelliJPathImpl) workspace.getLocation()).relativize(getLocation());
    }

    @Override
    public String getName() {
        return location.lastSegment();
    }

    @Override
    public IntelliJFolderImpl getParent() {
        return new IntelliJFolderImpl(workspace,
            getLocation().toFile().getParentFile());
    }

    @Override
    public IProject getProject() {
        return workspace.getProject(getFullPath().toPortableString());
    }

    @Override
    public IPath getProjectRelativePath() {
        IntelliJProjectImpl project;
        try {
            project = workspace.getProjectForPath(getVirtualFile().getPath());
        } catch (IOException e) {
            return null;
        }

        return ((IntelliJPathImpl) project.getLocation()).relativize(getLocation());
    }

    public SPath getSPath() {
        return new SPath(getProject(), getProjectRelativePath());
    }

    @Override
    public int getType() {
        return NONE;
    }

    @Override
    public boolean isAccessible() {
        return getLocation().toFile().canRead();
    }

    @Override
    public boolean isDerived(boolean checkAncestors) {
        return isDerived();
    }

    @Override
    public boolean isDerived() {
        //TODO: Query ModuleRootManager.getExcludedRoots whether this is ignored
        return false;
    }

    @Override
    public IResourceAttributes getResourceAttributes() {
        return attributes;
    }

    @Override
    public void setResourceAttributes(IResourceAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public IPath getLocation() {
        return location;
    }

    @Override
    public URI getLocationURI() {
        return getLocation().toFile().toURI();
    }

    public File toFile() {
        return getLocation().toFile();
    }

    @Override
    public void move(IPath destination, boolean force) throws IOException {

        final IPath absoluteDestination = destination.makeAbsolute();

        if (!workspace.getLocation().isPrefixOf(absoluteDestination)) {
            throw new IOException(
                "Destination does not belong in current workspace "
                    + workspace);
        }

        if (getLocation().isPrefixOf(absoluteDestination)) {
            throw new IOException(
                "Current location of the resource must no be a prefix of the destination");
        }

        writeInUIThread(new ThrowableComputable<Void, IOException>() {
            @Override
            public Void compute() throws IOException {
                VirtualFile file = getVirtualFile();
                String newName = absoluteDestination.lastSegment();
                VirtualFile newParent = file.getFileSystem().findFileByPath(
                    absoluteDestination.removeLastSegments(1)
                        .toPortableString());

                if (newParent == null) {
                    throw new IOException("Destination folder not found");
                }

                file.copy(this, newParent, newName);
                file.delete(this);
                return null;
            }
        });

        location = absoluteDestination;
    }

    @Override
    public void delete(int updateFlags) throws IOException {
        writeInUIThread(new ThrowableComputable<Void, IOException>() {
            @Override
            public Void compute() throws IOException {
                getVirtualFile().delete(this);
                return null;
            }
        });
    }

    /**
     * Returns the virtual file corresponding to this resource.
     * <p/>
     * The this method should be called only if the resource exists in the filesystem.
     *
     * @throws FileNotFoundException if the file does not exist.
     */
    @NotNull
    protected VirtualFile getVirtualFile() throws IOException {
        VirtualFile virtualFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByIoFile(toFile());

        if (virtualFile == null) {
            throw new FileNotFoundException(toString());
        }

        return virtualFile;
    }

    protected void writeInUIThread(
        final ThrowableComputable<Void, IOException> runnable)
        throws IOException {
        final AtomicReference<IOException> thrownInUIThread = new AtomicReference<IOException>(
            null);

        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    ApplicationManager.getApplication()
                        .runWriteAction(runnable);
                } catch (IOException e) {
                    thrownInUIThread.set(e);
                }
            }
        }, ModalityState.defaultModalityState());

        IOException e;
        if ((e = thrownInUIThread.get()) != null) {
            throw e;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getLocation());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntelliJResourceImpl)) {
            return false;
        }

        IntelliJResourceImpl other = (IntelliJResourceImpl) obj;

        return getType() == other.getType() && getWorkspace()
            .equals(other.getWorkspace()) && getFullPath()
            .equals(other.getFullPath());
    }
}
