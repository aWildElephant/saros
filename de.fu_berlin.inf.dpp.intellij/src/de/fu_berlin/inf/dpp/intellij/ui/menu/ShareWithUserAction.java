package de.fu_berlin.inf.dpp.intellij.ui.menu;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.core.ui.util.CollaborationUtils;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJProjectImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJWorkspaceImpl;
import de.fu_berlin.inf.dpp.intellij.ui.util.IconManager;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An Action that starts a session when triggered.
 * <p/>
 * Calls {@link CollaborationUtils#startSession(List, List)}  with the
 * selected module as parameter.
 * <p/>
 * This class assumes that the project is allowed to be shared (at the moment only
 * completely shared projects are implemented) and that the call to
 * {@link SarosFileShareGroup#getProjectFromVirtFile(VirtualFile, Project)}
 * is supported for this IDE type.
 */
public class ShareWithUserAction extends AnAction {

    private static final Logger LOG = Logger
        .getLogger(ShareWithUserAction.class);

    private final JID userJID;
    private final String title;

    ShareWithUserAction(JID user) {
        super(user.getName(), null, IconManager.CONTACT_ONLINE_ICON);
        userJID = user;
        title = user.getName();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            return;
        }

        IProject project = new IntelliJWorkspaceImpl(e.getData(CommonDataKeys.PROJECT)).getProject(
            virtualFile.getName());

        List<IResource> resources = new ArrayList<IResource>();
        //We allow only completely shared projects, so no need to check
        //for partially shared ones.
        resources.add(project);

        List<JID> contacts = Collections.singletonList(userJID);

        CollaborationUtils.startSession(resources, contacts);
    }

    @Override
    public String toString() {
        return super.toString() + " " + title;
    }
}
