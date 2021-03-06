package de.fu_berlin.inf.dpp.server.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import de.fu_berlin.inf.dpp.filesystem.IContainer;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.filesystem.IResourceAttributes;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;

/**
 * Server implementation of the {@link IResource} interface. It represents each
 * resource directly as a folder or file in the physical file system.
 */
public abstract class ServerResourceImpl implements IResource {

    private IWorkspace workspace;
    private IPath path;

    /**
     * Creates a ServerResourceImpl.
     * 
     * @param workspace
     *            the containing workspace
     * @param path
     *            the resource's path relative to the workspace's root
     */
    public ServerResourceImpl(IWorkspace workspace, IPath path) {
        this.path = path;
        this.workspace = workspace;
    }

    /**
     * Returns the workspace the resource belongs to.
     * 
     * @return the containing workspace
     */
    public IWorkspace getWorkspace() {
        return workspace;
    }

    @Override
    public IPath getFullPath() {
        return path;
    }

    @Override
    public IPath getProjectRelativePath() {
        return getFullPath().removeFirstSegments(1);
    }

    @Override
    public String getName() {
        return getFullPath().lastSegment();
    }

    @Override
    public IPath getLocation() {
        return workspace.getLocation().append(path);
    }

    @Override
    public URI getLocationURI() {
        return toNioPath().toUri();
    }

    @Override
    public IContainer getParent() {
        IPath parentPath = getProjectRelativePath().removeLastSegments(1);
        IProject project = getProject();
        return parentPath.segmentCount() == 0 ? project : project
            .getFolder(parentPath);
    }

    @Override
    public IProject getProject() {
        String projectName = getFullPath().segment(0);
        return workspace.getProject(projectName);
    }

    @Override
    public boolean exists() {
        return Files.exists(toNioPath());
    }

    @Override
    public boolean isAccessible() {
        return exists();
    }

    @Override
    public boolean isDerived() {
        return false;
    }

    @Override
    public boolean isDerived(boolean checkAncestors) {
        return false;
    }

    @Override
    public void refreshLocal() throws IOException {
        // Nothing to do
    }

    @Override
    public IResourceAttributes getResourceAttributes() {
        File file = getLocation().toFile();
        if (!file.exists()) {
            return null;
        }

        IResourceAttributes attributes = new ServerResourceAttributesImpl();
        attributes.setReadOnly(!file.canWrite());
        return attributes;
    }

    @Override
    public void setResourceAttributes(IResourceAttributes attributes)
        throws IOException {

        File file = getLocation().toFile();
        if (!file.exists()) {
            throw new FileNotFoundException(getLocation().toOSString());
        }

        file.setWritable(!attributes.isReadOnly());
    }

    @Override
    public Object getAdapter(Class<? extends IResource> clazz) {
        return clazz.isInstance(this) ? this : null;
    }

    /**
     * Returns the resource's location as a {@link java.nio.files.Path}. This is
     * for internal use in conjunction with the utility methods of the
     * {@link java.nio.file.Files} class.
     * 
     * @return location as {@link java.nio.files.Path}
     */
    Path toNioPath() {
        return ((ServerPathImpl) getLocation()).getDelegate();
    }
}
