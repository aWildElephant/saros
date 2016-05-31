package de.fu_berlin.inf.dpp.intellij.ui.menu;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.filesystem.IFolder;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJFolderImpl;
import de.fu_berlin.inf.dpp.intellij.project.filesystem.IntelliJWorkspaceImpl;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.net.xmpp.XMPPConnectionService;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.picocontainer.annotations.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Saros action group for the pop-up menu when right-clicking on a module.
 */
public class SarosFileShareGroup extends ActionGroup {

    @Inject
    private ISarosSessionManager sessionManager;

    @Inject
    private XMPPConnectionService connectionService;

    @Inject
    private IntelliJWorkspaceImpl workspace;

    @Override
    public void actionPerformed(AnActionEvent e) {
        //do nothing when menu pops-up
    }

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

        if (e == null || sessionManager.getSarosSession() != null) {
            return new AnAction[0];
        }

        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Project ideaProject = e.getData(CommonDataKeys.PROJECT);
        if (virtualFile == null || ideaProject == null) {
            return new AnAction[0];
        }

        if (!virtualFile.isDirectory()) {
            return new AnAction[0];
        }

        Roster roster = connectionService.getRoster();
        if (roster == null)
            return new AnAction[0];

        IProject project = new IntelliJWorkspaceImpl(
            e.getData(CommonDataKeys.PROJECT))
            .getProjectForPath(virtualFile.getPath());

        IntelliJFolderImpl resFolder = new IntelliJFolderImpl(workspace,
            new File(virtualFile.getPath()));

        //Holger: This disables partial sharing for the moment, until the need arises
        if (!isCompleteProject(project, resFolder)) {
            return new AnAction[0];
        }

        List<AnAction> list = new ArrayList<AnAction>();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            Presence presence = roster.getPresence(rosterEntry.getUser());

            if (presence.getType() == Presence.Type.available) {
                list.add(
                    new ShareWithUserAction(new JID(rosterEntry.getUser())));
            }
        }

        return list.toArray(new AnAction[list.size()]);
    }

    /**
     * Checks whether a given folder is the project (module) root folder, to allow
     * only complete modules to be shared.
     *
     * @param project
     * @param resFolder
     * @return
     */
    private boolean isCompleteProject(IProject project, IFolder resFolder) {
        return resFolder.getLocation().equals(project.getLocation());
    }
}
