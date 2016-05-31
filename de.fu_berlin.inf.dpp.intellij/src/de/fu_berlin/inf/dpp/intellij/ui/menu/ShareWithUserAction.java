package de.fu_berlin.inf.dpp.intellij.ui.menu;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.core.ui.util.CollaborationUtils;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJProjectImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJWorkspaceImpl;
import de.fu_berlin.inf.dpp.intellij.ui.util.IconManager;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import org.apache.log4j.Logger;
import org.picocontainer.annotations.Inject;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * An action that starts a session when triggered.
 * <p/>
 * Calls {@link CollaborationUtils#startSession(List, List)}  with the
 * selected module as parameter.
 */
public class ShareWithUserAction extends AnAction {

    private static final Logger LOG = Logger
        .getLogger(ShareWithUserAction.class);

    private final JID userJID;
    private final String title;
    private final IntelliJWorkspaceImpl workspace;

    ShareWithUserAction(JID user, IntelliJWorkspaceImpl workspace) {
        super(user.getName(), null, IconManager.CONTACT_ONLINE_ICON);
        userJID = user;
        title = user.getName();
        this.workspace = workspace;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            LOG.debug("Cannot retrieve selected file");
            return;
        }

        Project ideaProject = e.getProject();
        if (ideaProject == null) {
            LOG.debug("Cannot retrieve current IntelliJ project");
            return;
        }

        // Here we need to create an IProject that more or less represent the virtualFile.
        // The project may not be located in the workspace directory.
        IResource project = new IntelliJProjectImpl(workspace,
            new File(virtualFile.getPath()));

        List<IResource> resources = Collections.singletonList(project);

        List<JID> contacts = Collections.singletonList(userJID);

        CollaborationUtils.startSession(resources, contacts);
    }

    @Override
    public String toString() {
        return super.toString() + " " + title;
    }
}
