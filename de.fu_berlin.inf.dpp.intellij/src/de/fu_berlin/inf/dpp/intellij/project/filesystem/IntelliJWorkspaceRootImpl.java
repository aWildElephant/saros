package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.filesystem.IWorkspaceRoot;

import java.io.File;

/**
 * IntelliJ implementation of {@link IWorkspaceRoot}.
 * <p/>
 * TODO: implement the findFilesForLocation method here and remove getFileForPath from IntelliJWorkspaceImpl
 */
public class IntelliJWorkspaceRootImpl extends IntelliJContainerImpl
    implements IWorkspaceRoot {

    public IntelliJWorkspaceRootImpl(IntelliJWorkspaceImpl workspace) {
        super(workspace, new File("."));
    }

    @Override
    public IProject[] getProjects() {
        return (IProject[]) members(PROJECT); // Dirty dirty
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        if (clazz.isInstance(this)) {
            return this;
        }

        return null;
    }
}
