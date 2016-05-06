package de.fu_berlin.inf.dpp.intellij.editor;

import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import de.fu_berlin.inf.dpp.activities.SPath;

/**
 * IntelliJ editor selection listener.
 */
public class StoppableSelectionListener extends AbstractStoppableListener
    implements SelectionListener {

    public StoppableSelectionListener(EditorManager manager) {
        super(manager);
    }

    /**
     * Forwards change of the current selection to the {@link EditorManager}
     * <p/>
     * This method checks that text range of the selection actually changed,
     * in order not to flood the network layer.
     *
     * @see EditorManager#generateSelection(SPath, SelectionEvent)
     */
    @Override
    public void selectionChanged(SelectionEvent event) {
        if (!enabled) {
            return;
        }

        if (!selectionRangeChanged(event)) {
            return;
        }

        SPath path = editorManager.getEditorPool()
            .getFile(event.getEditor().getDocument());
        if (path != null) {
            editorManager.generateSelection(path, event);
        }
    }

    private boolean selectionRangeChanged(SelectionEvent event) {
        return !event.getNewRange().equals(event.getOldRange());
    }
}
