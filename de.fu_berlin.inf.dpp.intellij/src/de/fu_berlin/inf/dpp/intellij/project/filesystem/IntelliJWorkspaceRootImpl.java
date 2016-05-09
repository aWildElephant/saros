package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.filesystem.IWorkspaceRoot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * IntelliJ implementation of {@link IWorkspaceRoot}.
 * <p/>
 * TODO: implement the findFilesForLocation method here and remove getFileForPath from IntelliJWorkspaceImpl
 */
public class IntelliJWorkspaceRootImpl extends IntelliJContainerImpl
    implements IWorkspaceRoot {

    private IWorkspace workspace;

    public IntelliJWorkspaceRootImpl(IWorkspace workspace) {
        super(workspace.getProject("."), new File("."));

        this.workspace = workspace;
    }

    @Override
    public IProject[] getProjects() {
        Collection<IProject> projects = new ArrayList<IProject>();

        for (IResource subFolder : members(IResource.FOLDER)) {
            IProject newProject = workspace.getProject(subFolder.getName());

            projects.add(newProject);
        }

        return projects.toArray(new IProject[projects.size()]);
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        if (clazz.isInstance(this)) {
            return this;
        }

        return null;
    }
}
