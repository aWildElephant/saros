package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.util.ThrowableComputable;
import de.fu_berlin.inf.dpp.filesystem.IFile;
import de.fu_berlin.inf.dpp.filesystem.IFolder;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;

import java.io.File;
import java.io.IOException;

/**
 * IntelliJ implementation of the IProject interface.
 */
public class IntelliJProjectImpl extends IntelliJContainerImpl
        implements IProject {

    public IntelliJProjectImpl(IntelliJWorkspaceImpl workspace, File path) {
        super(workspace, path);
    }

    /**
     * Create this project.
     * <p/>
     * This method will fail if the project already exists.
     *
     */
    public void create() throws IOException {
        writeInUIThread(new ThrowableComputable<Void, IOException>() {

            @Override
            public Void compute() throws IOException {
                getParent().getVirtualFile()
                    .createChildDirectory(this, getName());
                return null;
            }
        });
    }

    @Override
    public IProject getProject() {
        return this;
    }

    @Override
    public IPath getLocation() {
        return location;
    }

    @Override
    public IResource findMember(IPath path) {
        if (path.segmentCount() == 0) {
            return this;
        }

        File file = getLocation().append(path).toFile();

        if (file.isFile()) {
            return getFile(path);
        } else if (file.isDirectory()) {
            return getFolder(path);
        } else {
            return null;
        }
    }

    @Override
    public IFile getFile(String path) {
        return new IntelliJFileImpl(workspace, toFile(path));
    }

    @Override
    public IFile getFile(IPath path) {
        return getFile(path.toPortableString());
    }

    @Override
    public IFolder getFolder(String path) {
        return new IntelliJFolderImpl(workspace, toFile(path));
    }

    @Override
    public IFolder getFolder(IPath path) {
        return getFolder(path.toPortableString());
    }

    @Override
    public int getType() {
        return PROJECT;
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        if (clazz.isInstance(this)) {
            return this;
        }

        return null;
    }

    private File toFile(String path) {
        return new File(getFullPath().append(path).toOSString());
    }
}
