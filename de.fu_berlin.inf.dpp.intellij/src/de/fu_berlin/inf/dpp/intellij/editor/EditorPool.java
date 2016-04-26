package de.fu_berlin.inf.dpp.intellij.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ArrayListSet;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJPathImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJWorkspaceImpl;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * IntelliJ editor pool.
 * <p/>
 * This class provides a link between IntelliJ's <code>Editor</code> and
 * <code>Document</code> instances and files in the current workspace.
 *
 * @see EditorFactory
 * @see FileDocumentManager
 */
public class EditorPool {

    private static final Logger LOG = Logger.getLogger(EditorPool.class);

    private final IWorkspace workspace;
    private final Project project;

    public EditorPool(IWorkspace workspace, Project project) {
        this.workspace = workspace;
        this.project = project;
    }

    /**
     * Set all shared documents opened in an editor as read-only.
     * <p/>
     * FIXME: all documents not opened in an editor are left as-is.
     */
    public void unlockAllDocuments() {
        for (Editor ed : getEditors()) {
            ed.getDocument().setReadOnly(false);
        }
    }

    /**
     * Set all shared documents opened in an editor as read-write.
     * <p/>
     * FIXME: calling this method after unlockAllDocuments will not restore the previous status of affected documents.
     */
    public void lockAllDocuments() {
        for (Editor ed : getEditors()) {
            ed.getDocument().setReadOnly(true);
        }
    }

    /**
     * Retrieve the cached document instance for a given file.
     *
     * @param file a file in the current workspace
     * @return null if the path cannot be found, is a directory, a binary without associated decompiler or is too large.
     */
    @Nullable
    public Document getDocument(
        @NotNull
        SPath file) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByIoFile(file.getProject().getLocation().append(file.getProjectRelativePath()).toFile());

        if (virtualFile == null) {
            LOG.debug(
                "Could not get document instance for " + file + ": not found");
            return null;
        }

        return ApplicationManager.getApplication().runReadAction(
            new Computable<Document>() {

                @Override
                public Document compute() {
                    return FileDocumentManager.getInstance()
                        .getDocument(virtualFile);
                }
            });
    }

    /**
     * Return the editor in which the given file is opened.
     *
     * @param file a file in the current workspace.
     * @return null if the file cannot be found or if it is not opened in an editor.
     */
    @Nullable
    public Editor getEditor(
        @NotNull
        SPath file) {
        Document doc = getDocument(file);

        if (doc == null) {
            return null;
        }

        Editor[] editors = EditorFactory.getInstance()
            .getEditors(doc, project);

        if (editors.length == 0) {
            LOG.debug("File " + file + " is not opened in an editor");
            return null;
        }

        if (editors.length > 1) {
            LOG.warn(editors.length + " editors linked to file " + file);
        }

        return editors[0]; // FIXME: we should return all editors and adapt uses of this method to handle them all
    }

    /**
     * Returns the file in the workspace corresponding to the document instance.
     *
     * @param doc a cached document instance
     * @return null if the document was not created from the filesystem or if the corresponding file does not belong to the current workspace.
     */
    @Nullable
    public SPath getFile(
        @NotNull
        Document doc) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(doc);

        if (file == null) {
            LOG.debug(
                "Document " + doc + " was not created from a virtual file");
            return null;
        }

        String fullPath = file.getPath();
        IProject fileProject = ((IntelliJWorkspaceImpl) workspace)
            .getProjectForPath(fullPath);

        if (fileProject == null) {
            LOG.debug("Document " + doc
                + " does not belong to the current workspace");
            return null;
        }

        IPath projectRelativePath = IntelliJPathImpl.fromString(fullPath)
            .removeFirstSegments(fileProject.getLocation().segmentCount());

        return new SPath(fileProject, projectRelativePath);
    }

    /**
     * Returns all opened editors for the current workspace.
     */
    public Collection<Editor> getEditors() {
        Editor[] allEditors = EditorFactory.getInstance().getAllEditors();

        Collection<Editor> editors = new ArrayList<Editor>();

        for (Editor ed : allEditors) {
            if (getFile(ed.getDocument()) != null) {
                editors.add(ed);
            }
        }

        return editors;
    }

    /**
     * Returns all opened files in the current workspace.
     */
    public Set<SPath> getFiles() {
        Editor[] allEditors = EditorFactory.getInstance().getAllEditors();

        Set<SPath> files = new ArrayListSet<SPath>();

        for (Editor ed : allEditors) {
            SPath openedFile = getFile(ed.getDocument());

            if (openedFile != null) {
                files.add(openedFile);
            }
        }

        return files;
    }
}
