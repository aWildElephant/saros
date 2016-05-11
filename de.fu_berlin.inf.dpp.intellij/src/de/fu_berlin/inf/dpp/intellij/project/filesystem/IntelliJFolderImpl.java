package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.util.ThrowableComputable;
import de.fu_berlin.inf.dpp.filesystem.IFolder;
import de.fu_berlin.inf.dpp.filesystem.IResource;

import java.io.File;
import java.io.IOException;

public class IntelliJFolderImpl extends IntelliJContainerImpl
    implements IFolder {

    public IntelliJFolderImpl(IntelliJWorkspaceImpl workspace, File file) {
        super(workspace, file);
    }

    @Override
    public void create(int updateFlags, boolean local) throws IOException {
        create(false, local);
    }

    @Override
    public void create(boolean force, boolean local) throws IOException {
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
    public int getType() {
        return FOLDER;
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        if (clazz.isInstance(this)) {
            return this;
        }

        return null;
    }
}
