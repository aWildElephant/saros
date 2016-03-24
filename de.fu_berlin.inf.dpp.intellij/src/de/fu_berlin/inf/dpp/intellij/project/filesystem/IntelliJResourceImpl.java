/*
 *
 *  DPP - Serious Distributed Pair Programming
 *  (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2010
 *  (c) NFQ (www.nfq.com) - 2014
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 1, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * /
 */

package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.filesystem.IResourceAttributes;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public abstract class IntelliJResourceImpl implements IResource {

    //TODO resolve charset issue by reading real data
    public static final String DEFAULT_CHARSET = "utf8";

    protected IntelliJProjectImpl project;
    protected IPath projectRelativePath;
    private IResourceAttributes attributes;
    private String defaultCharset = DEFAULT_CHARSET;

    protected IntelliJResourceImpl(IntelliJProjectImpl project, File file) {
        this.project = project;

        if (file.isAbsolute()) {
            if (!project.getLocation()
                .isPrefixOf(IntelliJPathImpl.fromString(file.getPath()))) {
                throw new IllegalArgumentException(
                    "File " + file.getPath() + " does not belong in project "
                        + project.getLocation());
            }
            this.projectRelativePath = IntelliJPathImpl
                .fromString(file.getPath())
                .removeFirstSegments(project.getLocation().segmentCount());
        } else {
            this.projectRelativePath = IntelliJPathImpl
                .fromString(file.getPath());
        }
        this.attributes = new IntelliJFileResourceAttributesImpl(file);
    }

    public String getDefaultCharset() {
        return defaultCharset;
    }

    public void setDefaultCharset(String defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    @Override
    public boolean exists() {
        return toFile().exists();
    }

    @Override
    public IPath getFullPath() {
        return getLocation();
    }

    @Override
    public IPath getLocation() {
        return project.getLocation().append(projectRelativePath);
    }

    @Override
    public URI getLocationURI() {
        return getLocation().toFile().toURI();
    }

    @Override
    public String getName() {
        return projectRelativePath.lastSegment();
    }

    @Override
    public IntelliJFolderImpl getParent() {
        return new IntelliJFolderImpl(project,
            getLocation().toFile().getParentFile());
    }

    @Override
    public IntelliJProjectImpl getProject() {
        return project;
    }

    public void setProject(IntelliJProjectImpl project) {
        this.project = project;
    }

    @Override
    public IPath getProjectRelativePath() {
        return projectRelativePath;
    }

    public SPath getSPath() {
        return new SPath(project, getProjectRelativePath());
    }

    @Override
    public int getType() {
        return NONE;
    }

    @Override
    public boolean isAccessible() {
        return getFullPath().toFile().canRead();
    }

    @Override
    public boolean isDerived(boolean checkAncestors) {
        return isDerived(); //todo.
    }

    @Override
    public boolean isDerived() {
        return false;
    }

    public File toFile() {
        return getLocation().toFile();
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
    public void delete(int updateFlags) throws IOException {
        writeInUIThread(new ThrowableComputable<Void, IOException>() {
            @Override
            public Void compute() throws IOException {
                getVirtualFile().delete(this);
                return null;
            }
        });
    }

    @Override
    public void move(IPath destination, boolean force) throws IOException {

        final IPath absoluteDestination = destination.makeAbsolute();

        if (!project.getLocation().isPrefixOf(absoluteDestination)) {
            throw new IOException(
                "Destination does not belong in project " + project);
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

        projectRelativePath = absoluteDestination
            .removeFirstSegments(project.getLocation().segmentCount());
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
        return Objects.hash(getLocation(), getProject());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntelliJResourceImpl)) {
            return false;
        }

        IntelliJResourceImpl other = (IntelliJResourceImpl) obj;

        return getLocation().equals(other.getLocation()) && getProject()
            .equals(other.getProject());
    }
}
