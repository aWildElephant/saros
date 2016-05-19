package de.fu_berlin.inf.dpp.intellij.project;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import de.fu_berlin.inf.dpp.activities.FileActivity;
import de.fu_berlin.inf.dpp.activities.FolderCreatedActivity;
import de.fu_berlin.inf.dpp.activities.FolderDeletedActivity;
import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.core.util.FileUtils;
import de.fu_berlin.inf.dpp.filesystem.IFile;
import de.fu_berlin.inf.dpp.filesystem.IFolder;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.intellij.editor.AbstractStoppableListener;
import de.fu_berlin.inf.dpp.intellij.editor.EditorManager;
import de.fu_berlin.inf.dpp.intellij.editor.StoppableDocumentListener;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJFileImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJPathImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJProjectImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJWorkspaceImpl;
import de.fu_berlin.inf.dpp.session.User;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Virtual file system listener. It receives events for all files in all projects
 * opened by the user.
 * <p/>
 * It filters for files that are shared and calls the corresponding methods for
 * {@link IActivity}-creation on the {@link SharedResourcesManager}.
 *
 * @see StoppableDocumentListener
 */
public class FileSystemChangeListener extends AbstractStoppableListener
    implements VirtualFileListener {

    private static final Logger LOG = Logger
        .getLogger(FileSystemChangeListener.class);
    private final SharedResourcesManager resourceManager;
    private IntelliJWorkspaceImpl intelliJWorkspaceImpl;

    //HACK: This list is used to filter events for files that were created from
    //remote, because we can not disable the listener for them
    private final List<File> incomingFilesToFilterFor = new ArrayList<File>();

    private final Set<VirtualFile> newFiles = new HashSet<VirtualFile>();

    public FileSystemChangeListener(SharedResourcesManager resourceManager,
        EditorManager editorManager) {
        super(editorManager);
        this.resourceManager = resourceManager;
    }

    private void generateFolderMove(SPath oldSPath, SPath newSPath,
        boolean before) {
        User user = resourceManager.getSession().getLocalUser();
        IntelliJProjectImpl project = (IntelliJProjectImpl) oldSPath
            .getProject();
        IActivity createActivity = new FolderCreatedActivity(user, newSPath);
        resourceManager.internalFireActivity(createActivity);

        IFolder folder = before ? oldSPath.getFolder() : newSPath.getFolder();

        IResource[] members = new IResource[0];
        try {
            members = folder.members();
        } catch (IOException e) {
            LOG.error("error reading folder: " + folder, e);
        }

        for (IResource resource : members) {
            SPath oldChildSPath = new IntelliJFileImpl(project, new File(
                oldSPath.getFullPath().toOSString() + File.separator + resource
                    .getName())).getSPath();
            SPath newChildSPath = new IntelliJFileImpl(
                (IntelliJProjectImpl) newSPath.getProject(), new File(
                newSPath.getFullPath().toOSString() + File.separator + resource
                    .getName())).getSPath();
            if (resource.getType() == IResource.FOLDER) {
                generateFolderMove(oldChildSPath, newChildSPath, before);
            } else {
                generateFileMove(oldChildSPath, newChildSPath, before);
            }
        }

        IActivity removeActivity = new FolderDeletedActivity(user, oldSPath);
        resourceManager.internalFireActivity(removeActivity);

        project.addFile(newSPath.getFile().getLocation().toFile());
        project.removeFile(oldSPath.getFile().getLocation().toFile());
    }

    private void generateFileMove(SPath oldSPath, SPath newSPath,
        boolean before) {
        User user = resourceManager.getSession().getLocalUser();
        IntelliJProjectImpl project = (IntelliJProjectImpl) newSPath
            .getProject();
        IntelliJProjectImpl oldProject = (IntelliJProjectImpl) oldSPath
            .getProject();

        IFile file;

        if (before) {
            file = oldProject.getFile(oldSPath.getFullPath());
            editorManager.saveFile(oldSPath);
        } else {
            file = project.getFile(newSPath.getFullPath());
            editorManager.saveFile(newSPath);
        }

        if (file == null) {
            return;
        }

        byte[] bytes = FileUtils.getLocalFileContent(file);

        String charset = getEncoding(file);

        IActivity activity = new FileActivity(user, FileActivity.Type.MOVED,
            newSPath, oldSPath, bytes, charset, FileActivity.Purpose.ACTIVITY);
        editorManager.replaceAllEditorsForPath(oldSPath, newSPath);

        project.addFile(newSPath.getFile().getLocation().toFile());
        oldProject.removeFile(oldSPath.getFile().getLocation().toFile());
        resourceManager.internalFireActivity(activity);
    }

    /**
     * This is called after the file was modified on disk.
     * <p/>
     * We use the DocumentListener interface to capture modifications of the files' contents.
     *
     * @see StoppableDocumentListener#documentChanged(DocumentEvent)
     */
    @Override
    public void contentsChanged(
        @NotNull
        VirtualFileEvent virtualFileEvent) {

        if (virtualFileEvent.isFromSave()) {
            return;
        }

        LOG.warn(
            "Ignored contentsChanged event that does not come from the save of a document: "
                + virtualFileEvent);
    }

    /**
     * Handle a creation of a file or folder.
     * <p/>
     * Check if the resource should belong to the current project. If this is
     * the case, fire an activity and add the resource to the project.
     *
     * @see FileActivity#created(User, SPath, byte[], String, FileActivity.Purpose)
     * @see FolderCreatedActivity
     */
    @Override
    public void fileCreated(
        @NotNull
        VirtualFileEvent virtualFileEvent) {
        if (!enabled) {
            return;
        }

        File file = convertVirtualFileEventToFile(virtualFileEvent);
        IPath path = IntelliJPathImpl.fromString(file.getPath());
        IntelliJProjectImpl project = intelliJWorkspaceImpl
            .getProjectForPath(file.getPath());

        if (!isValidProject(project)) {
            return;
        }

        //This is true, when a new folder for an incoming project was created.
        //If this is the case, we do not want to send an FolderActivity back.

        if (path.equals(project.getLocation())) {
            return;
        }

        if (incomingFilesToFilterFor.remove(file)) {
            project.addFile(file);
            return;
        }

        if (path.equals(project.getFullPath())) {
            if (!isCompletelyShared(project)) {
                return;
            }
        }

        path = makeAbsolutePathProjectRelative(path, project);

        SPath spath = new SPath(project, path);
        User user = resourceManager.getSession().getLocalUser();
        IActivity activity;

        if (file.isFile()) {
            byte[] bytes = new byte[0];
            String charset;
            charset = virtualFileEvent.getFile().getCharset().name();
            activity = FileActivity.created(user, spath, bytes, charset,
                FileActivity.Purpose.ACTIVITY);
        } else {
            activity = new FolderCreatedActivity(user, spath);
        }

        project.addFile(file);

        resourceManager.internalFireActivity(activity);
    }

    /**
     * Handle the deletion of a file.
     */
    @Override
    public void fileDeleted(
        @NotNull
        VirtualFileEvent virtualFileEvent) {
        if (!enabled) {
            return;
        }

        File file = convertVirtualFileEventToFile(virtualFileEvent);
        if (incomingFilesToFilterFor.remove(file)) {
            return;
        }

        IPath path = IntelliJPathImpl.fromString(file.getPath());
        IntelliJProjectImpl project = intelliJWorkspaceImpl
            .getProjectForPath(file.getPath());

        if (!isValidProject(project) || !isCompletelyShared(project)) {
            return;
        }

        path = makeAbsolutePathProjectRelative(path, project);

        SPath spath = new SPath(project, path);
        User user = resourceManager.getSession().getLocalUser();

        IActivity activity;
        if (virtualFileEvent.getFile().isDirectory()) {
            activity = new FolderDeletedActivity(user, spath);
        } else {
            activity = FileActivity
                .removed(user, spath, FileActivity.Purpose.ACTIVITY);
        }

        project.removeFile(file);
        editorManager.removeAllEditorsForPath(spath);

        resourceManager.internalFireActivity(activity);
    }

    @Override
    public void fileMoved(
        @NotNull
        VirtualFileMoveEvent virtualFileMoveEvent) {
        if (!enabled) {
            return;
        }

        File newFile = convertVirtualFileEventToFile(virtualFileMoveEvent);
        if (incomingFilesToFilterFor.remove(newFile)) {
            return;
        }

        IPath path = IntelliJPathImpl.fromString(newFile.getPath());
        IntelliJProjectImpl project = intelliJWorkspaceImpl
            .getProjectForPath(newFile.getPath());

        if (!isValidProject(project) || !isCompletelyShared(project)) {
            return;
        }

        path = makeAbsolutePathProjectRelative(path, project);

        SPath newSPath = new SPath(project, path);

        IPath oldParent = IntelliJPathImpl
            .fromString(virtualFileMoveEvent.getOldParent().getPath());
        IPath oldPath = oldParent.append(virtualFileMoveEvent.getFileName());
        IProject oldProject = intelliJWorkspaceImpl
            .getProjectForPath(oldPath.toPortableString());

        oldPath = makeAbsolutePathProjectRelative(oldPath, project);
        SPath oldSPath = new SPath(oldProject, oldPath);

        //FIXME: Handle cases where files are moved from outside the shared project
        //into the shared project
        if (oldProject == null) {
            LOG.error(
                " can not move files from unshared project to shared project");
            return;
        }
        if (project.equals(oldProject)) {
            if (newFile.isFile()) {
                generateFileMove(oldSPath, newSPath, false);
            } else {
                generateFolderMove(oldSPath, newSPath, false);
            }
        }
    }

    @Override
    public void propertyChanged(
        @NotNull
        VirtualFilePropertyEvent filePropertyEvent) {
        if (!enabled) {
            return;
        }

        File oldFile = new File(
            filePropertyEvent.getFile().getParent().getPath() + File.separator
                + filePropertyEvent.getOldValue());
        File newFile = convertVirtualFileEventToFile(filePropertyEvent);

        if (incomingFilesToFilterFor.remove(newFile)) {
            return;
        }

        IPath oldPath = IntelliJPathImpl.fromString(oldFile.getPath());
        IntelliJProjectImpl project = intelliJWorkspaceImpl
            .getProjectForPath(newFile.getPath());

        if (!isValidProject(project)) {
            return;
        }

        oldPath = makeAbsolutePathProjectRelative(oldPath, project);
        SPath oldSPath = new SPath(project, oldPath);

        IPath newPath = IntelliJPathImpl.fromString(newFile.getPath());
        newPath = makeAbsolutePathProjectRelative(newPath, project);

        SPath newSPath = new SPath(project, newPath);
        //we handle this as a move activity
        if (newFile.isFile()) {
            generateFileMove(oldSPath, newSPath, false);
        } else {
            generateFolderMove(oldSPath, newSPath, false);
        }
    }

    @Override
    public void fileCopied(
        @NotNull
        VirtualFileCopyEvent virtualFileCopyEvent) {
        if (!enabled) {
            return;
        }

        VirtualFile virtualFile = virtualFileCopyEvent.getFile();
        File newFile = new File(virtualFile.getPath());

        if (incomingFilesToFilterFor.remove(newFile)) {
            return;
        }

        IPath path = IntelliJPathImpl.fromString(newFile.getPath());
        IntelliJProjectImpl project = intelliJWorkspaceImpl
            .getProjectForPath(newFile.getPath());

        if (!isValidProject(project) || !isCompletelyShared(project)) {
            return;
        }

        path = makeAbsolutePathProjectRelative(path, project);

        SPath spath = new SPath(project, path);

        User user = resourceManager.getSession().getLocalUser();
        IActivity activity;

        byte[] bytes = new byte[0];
        try {
            bytes = virtualFileCopyEvent.getOriginalFile()
                .contentsToByteArray();
        } catch (IOException e) {
            LOG.error("could not read content of original file "
                + virtualFileCopyEvent.getOriginalFile(), e);
            return;
        }

        activity = FileActivity
            .created(user, spath, bytes, virtualFile.getCharset().name(),
                FileActivity.Purpose.ACTIVITY);

        project.addFile(newFile);

        resourceManager.internalFireActivity(activity);
    }

    @Override
    public void beforePropertyChange(
        @NotNull
        VirtualFilePropertyEvent filePropertyEvent) {
        // Not interested
    }

    /**
     * This method is called for files that already exist and that are modified,
     * but before the file is modified on disk.
     *
     * @param virtualFileEvent
     */
    @Override
    public void beforeContentsChange(
        @NotNull
        VirtualFileEvent virtualFileEvent) {
        //Do nothing
    }

    @Override
    public void beforeFileDeletion(
        @NotNull
        VirtualFileEvent virtualFileEvent) {
        //Do nothing
    }

    @Override
    public void beforeFileMovement(
        @NotNull
        VirtualFileMoveEvent virtualFileMoveEvent) {
        //Do nothing
    }

    public void setWorkspace(IntelliJWorkspaceImpl intelliJWorkspaceImpl) {
        this.intelliJWorkspaceImpl = intelliJWorkspaceImpl;
    }

    /**
     * Adds a file to the filter list for incoming files (for which no activities
     * should be generated).
     *
     * @param file
     */
    public void addIncomingFileToFilterFor(File file) {
        incomingFilesToFilterFor.add(file);
    }

    private IPath makeAbsolutePathProjectRelative(IPath path,
        IProject project) {
        return path.removeFirstSegments(project.getLocation().segmentCount());
    }

    private File convertVirtualFileEventToFile(
        VirtualFileEvent virtualFileEvent) {
        return new File(virtualFileEvent.getFile().getPath());
    }

    private String getEncoding(IFile file) {
        String charset = null;

        try {
            charset = file.getCharset();
        } catch (IOException e) {
            LOG.warn("could not determine encoding for file: " + file, e);
        }
        if (charset == null)
            return EncodingProjectManager.getInstance().getDefaultCharset()
                .name();

        return charset;
    }

    private boolean isCompletelyShared(IntelliJProjectImpl project) {
        return resourceManager.getSession().isCompletelyShared(project);
    }

    private boolean isValidProject(IntelliJProjectImpl project) {
        return project != null && project.exists();
    }
}
