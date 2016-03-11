package de.fu_berlin.inf.dpp.intellij.ui.views.buttons;

import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.intellij.ui.actions.AddContactAction;
import de.fu_berlin.inf.dpp.net.ConnectionState;
import de.fu_berlin.inf.dpp.net.xmpp.IConnectionListener;
import de.fu_berlin.inf.dpp.net.xmpp.XMPPConnectionService;
import org.jivesoftware.smack.Connection;
import org.picocontainer.annotations.Inject;

public class AddContactButton extends SimpleButton {

    public static final String ADD_CONTACT_ICON_PATH = "/icons/famfamfam/contact_add_tsk.png";

    @Inject
    XMPPConnectionService connectionService;

    private IConnectionListener connectionListener = new IConnectionListener() {
        @Override
        public void connectionStateChanged(Connection connection,
            ConnectionState state) {
            setEnabledFromUIThread(connectionService.isConnected());
        }
    };

    public AddContactButton() {
        super(new AddContactAction(), "Add a contact", ADD_CONTACT_ICON_PATH, "addContact");

        SarosPluginContext.initComponent(this);
        setEnabledFromUIThread(connectionService.isConnected());
        connectionService.addListener(connectionListener);
    }
}
