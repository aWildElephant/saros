package de.fu_berlin.inf.dpp.stf.test.stf.view.sarosview;

import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.ALICE;
import static de.fu_berlin.inf.dpp.stf.server.STFMessage.log;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.VIEW_SAROS;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.VIEW_SAROS_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.StfTestCase;
import de.fu_berlin.inf.dpp.stf.client.util.Constants;

public class SarosViewTest extends StfTestCase {

    @BeforeClass
    public static void runBeforeClass() throws RemoteException {
        initTesters(ALICE);
        setUpWorkbench();
        setUpSaros();
    }

    @Test
    public void testSarosView() throws RemoteException {
        ALICE.remoteBot().view(VIEW_SAROS).close();
        assertEquals(false, ALICE.remoteBot().isViewOpen(VIEW_SAROS));
        ALICE.remoteBot().openViewById(VIEW_SAROS_ID);
        ALICE.remoteBot().captureScreenshot(
            (ALICE.remoteBot().getPathToScreenShot() + "/session_view.png"));
        assertEquals(true, ALICE.remoteBot().isViewOpen(VIEW_SAROS));
    }

    @Test
    public void connect() throws RemoteException {
        log.trace("xmppConnect");
        ALICE.superBot().views().sarosView()
            .connectWith(ALICE.getJID(), ALICE.getPassword());
        log.trace("captureScreenshot");
        ALICE.remoteBot().captureScreenshot(
            (ALICE.remoteBot().getPathToScreenShot() + "/xmpp_connected.png"));
        assertEquals(true, ALICE.superBot().views().sarosView().isConnected());
    }

    @Test
    public void connectWith() throws RemoteException {
        ALICE.superBot().views().sarosView()
            .connectWith(Constants.TEST_JID, Constants.PASSWORD);
        assertTrue(ALICE.superBot().menuBar().saros().preferences()
            .isAccountActive(Constants.TEST_JID));
    }

    @Test
    public void connectWithActiveAccount() throws RemoteException {
        ALICE.superBot().views().sarosView().connectWithActiveAccount();
        assertTrue(ALICE.superBot().views().sarosView().isConnected());
    }

    @Test
    public void disconnect() throws RemoteException {
        ALICE.superBot().views().sarosView()
            .connectWith(ALICE.getJID(), ALICE.getPassword());
        ALICE.superBot().views().sarosView().disconnect();
        ALICE
            .remoteBot()
            .captureScreenshot(
                (ALICE.remoteBot().getPathToScreenShot() + "/xmpp_disconnected.png"));
        assertEquals(false, ALICE.superBot().views().sarosView().isConnected());
    }

    @Test
    public void addExistedBuddy() throws RemoteException {
        assertTrue(ALICE.superBot().views().sarosView()
            .hasBuddy(Constants.TEST_JID));
        ALICE.superBot().views().sarosView().addANewBuddy(Constants.TEST_JID);
        assertTrue(ALICE.superBot().views().sarosView()
            .hasBuddy(Constants.TEST_JID));
    }
}