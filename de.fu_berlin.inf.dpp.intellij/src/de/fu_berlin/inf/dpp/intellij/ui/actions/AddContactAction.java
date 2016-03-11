package de.fu_berlin.inf.dpp.intellij.ui.actions;

import de.fu_berlin.inf.dpp.intellij.ui.util.SafeDialogUtils;
import de.fu_berlin.inf.dpp.net.util.XMPPUtils;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.net.xmpp.XMPPConnectionService;
import org.jivesoftware.smack.XMPPException;
import org.picocontainer.annotations.Inject;

public class AddContactAction extends AbstractSarosAction {

    private static String NAME = "addContact";

    @Inject
    private XMPPConnectionService connectionService;

    @Override
    public String getActionName() {
        return NAME;
    }

    @Override
    public void execute() {
        String userID = SafeDialogUtils
            .showInputDialog("Their User-ID", "", "Add a contact");

        if (!userID.isEmpty()) {
            validateUserID(userID);
        }
    }

    private void validateUserID(String userID) {
        JID jid = new JID(userID);

        if (XMPPUtils.validateJID(jid)) {
            if (isJIDOnServer(jid)) {
                sendSubscriptionRequest(jid);
            } else {
                confirmAddJIDNotOnServer(jid);
            }
        } else {
            SafeDialogUtils.showError("Invalid User-ID", "Error");
        }
    }

    private boolean isJIDOnServer(JID jid) {
        try {
            return XMPPUtils
                .isJIDonServer(connectionService.getConnection(), jid,
                    null); // Eclipse version uses SarosConstants.RESOURCE as hint, but it is deprecated
        } catch (XMPPException e) {
            return false; // The discovery manager raise a XMPPException with a 404 error if the user is unknown to the server
        }
    }

    private void confirmAddJIDNotOnServer(JID jid) {
        boolean choice = SafeDialogUtils.showQuestionMessageDialog(
            "You entered a valid XMPP server.\n\n"
                + "Unfortunately your entered JID is unknown to the server.\n"
                + "Please make sure you spelled the JID correctly.\n\n"
                + "Do you want to add the contact anyway?", "Contact unknown");

        if (choice) {
            sendSubscriptionRequest(jid);
        }
    }

    private void sendSubscriptionRequest(JID jid) {
        try {
            connectionService.getRoster()
                .createEntry(jid.getBase(), null, null);
        } catch (XMPPException e) {
            SafeDialogUtils.showWarning(
                "An unexpected error prevented this action to complete:\n" + e
                    .getMessage(), "Error");
        }
    }
}
