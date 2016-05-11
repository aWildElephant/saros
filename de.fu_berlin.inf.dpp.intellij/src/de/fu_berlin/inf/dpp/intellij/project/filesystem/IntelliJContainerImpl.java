package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.filesystem.IContainer;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class IntelliJContainerImpl extends IntelliJResourceImpl
    implements IContainer {

    protected IntelliJContainerImpl(
        @NotNull
        IntelliJWorkspaceImpl workspace,
        @NotNull
        File path) {
        super(workspace, path);
    }

    public boolean exists(
        @NotNull
        IPath path) {
        return getLocation().append(path).toFile().exists();
    }

    @Override
    public IResource[] members() {
        return members(NONE);
    }

    @Override
    public IResource[] members(int memberFlags) {
        VirtualFile virtualFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByIoFile(toFile());

        if (virtualFile == null) {
            return new IResource[0];
        }

        VirtualFile[] files = virtualFile.getChildren();

        if (files == null) {
            return new IResource[0];
        }

        List<IResource> list = new ArrayList<IResource>();

        for (VirtualFile file : files) {
            if (file.isDirectory() && (memberFlags == FOLDER
                || memberFlags == NONE)) {
                list.add(
                    new IntelliJFolderImpl(workspace, new File(file.getPath())));
            }

            if (!file.isDirectory() && (memberFlags == FILE
                || memberFlags == NONE)) {
                list.add(new IntelliJFileImpl(workspace,
                    new File(file.getPath())));
            }
        }

        return list.toArray(new IResource[list.size()]);
    }

    @Override
    public void refreshLocal() throws IOException {
        getVirtualFile().refresh(false, true);
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        if (clazz.isInstance(this)) {
            return this;
        }

        return null;
    }
}
