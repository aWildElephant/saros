package de.fu_berlin.inf.dpp.intellij.ui.menu;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.filesystem.IFolder;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJFolderImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJWorkspaceImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJProjectImpl;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.net.xmpp.XMPPConnectionService;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.picocontainer.annotations.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Saros action group for the pop-up menu when right-clicking in the project view.
 * <p/>
 * Computes the list of actions to share the selected element with each available
 * contacts.
 */
public class SarosFileShareGroup extends ActionGroup {

    private static final Logger LOG = Logger
        .getLogger(SarosFileShareGroup.class);

    @Inject
    private ISarosSessionManager sessionManager;

    @Inject
    private XMPPConnectionService connectionService;

    @Inject
    private IntelliJWorkspaceImpl workspace;

    @NotNull
    @Override
    public AnAction[] getChildren(
        @Nullable
        AnActionEvent e) {
        // This has to be initialized here, because doing it in the
        // constructor would be too early. The lifecycle is not
        // running yet when this class is instantiated.
        // To make the dependency injection work,
        // SarosPluginContext.initComponent has to be called here.
        if (sessionManager == null && connectionService == null) {
            SarosPluginContext.initComponent(this);
        }

        // The second expression seems to be here to prevent adding a project to a session.
        if (e == null || sessionManager.getSarosSession() != null) {
            return EMPTY_ARRAY;
        }

        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            LOG.debug("Cannot retrieve selected file");
            return EMPTY_ARRAY;
        }

        Project ideaProject = e.getData(CommonDataKeys.PROJECT);
        if (ideaProject == null) {
            LOG.debug("Cannot retrieve current IntelliJ project");
            return EMPTY_ARRAY;
        }

        if (!virtualFile.isDirectory()) {
            return EMPTY_ARRAY;
        }

        VirtualFile root = ProjectFileIndex.SERVICE.getInstance(ideaProject)
            .getContentRootForFile(virtualFile);
        // This disables partial sharing
        if (!virtualFile.equals(root)) {
            LOG.debug("Selected file is not a content root: " + virtualFile);
            return EMPTY_ARRAY;
        }

        // User selected has chosen a correct input. We now create the list of possible pair
        // and will let ShareWithUserAction properly create the resources.

        Roster roster = connectionService.getRoster();
        if (roster == null) {
            return EMPTY_ARRAY;
        }

        List<AnAction> list = new ArrayList<AnAction>();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            Presence presence = roster.getPresence(rosterEntry.getUser());

            if (presence.getType() == Presence.Type.available) {
                list.add(
                    new ShareWithUserAction(new JID(rosterEntry.getUser()), workspace));
            }
        }

        return list.toArray(new AnAction[list.size()]);
    }
}