package de.fu_berlin.inf.dpp.stf.test.multidriver;

import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.ALICE;
import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.BOB;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.TB_INCONSISTENCY_DETECTED;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.VIEW_SAROS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.StfTestCase;
import de.fu_berlin.inf.dpp.stf.client.util.Constants;
import de.fu_berlin.inf.dpp.stf.client.util.Util;
import de.fu_berlin.inf.dpp.stf.shared.Constants.TypeOfCreateProject;

public class ConcurrentEditingTest extends StfTestCase {
    @BeforeClass
    public static void beforeClass() throws Exception {
        initTesters(ALICE, BOB);
        setUpWorkbench();
        setUpSaros();
    }

    @Before
    public void beforeEachMethod() throws RemoteException {
        deleteAllProjectsByActiveTesters();
    }

    static final String FILE = "file.txt";

    /**
     * Test to reproduce bug
     * "Inconsistency when concurrently writing at same position"
     * 
     * @throws RemoteException
     * @throws InterruptedException
     * 
     * @see <a
     *      href="https://sourceforge.net/tracker/?func=detail&aid=3098992&group_id=167540&atid=843359">Bug
     *      tracker entry 3098992</a>
     */
    @Test
    public void testBugInconsistencyConcurrentEditing() throws RemoteException,
        InterruptedException {
        ALICE.superBot().views().packageExplorerView().tree().newC()
            .project(Constants.PROJECT1);
        // cool trick, no need to always use PROJECT1, PKG1, CLS1 as arguments

        ALICE.superBot().views().packageExplorerView()
            .selectProject(Constants.PROJECT1).newC().file(FILE);
        ALICE.remoteBot().waitUntilEditorOpen(FILE);
        ALICE.remoteBot().editor(FILE)
            .setTexWithSave("test/resources/lorem.txt");
        ALICE.remoteBot().editor(FILE).navigateTo(0, 6);

        Util.buildSessionSequentially(Constants.PROJECT1,
            TypeOfCreateProject.NEW_PROJECT, ALICE, BOB);
        BOB.superBot().views().packageExplorerView().selectFile(Constants.PATH)
            .open();

        BOB.remoteBot().waitUntilEditorOpen(FILE);
        BOB.remoteBot().editor(FILE).navigateTo(0, 30);

        BOB.remoteBot().sleep(1000);

        // Alice goes to 0,6 and hits Delete
        ALICE.remoteBot().activateWorkbench();
        int waitActivate = 100;
        ALICE.remoteBot().sleep(waitActivate);
        ALICE.remoteBot().editor(FILE).show();

        ALICE.remoteBot().editor(FILE).waitUntilIsActive();
        ALICE.remoteBot().editor(FILE).pressShortcut("DELETE");
        // at the same time, Bob enters L at 0,30
        BOB.remoteBot().activateWorkbench();
        BOB.remoteBot().sleep(waitActivate);
        BOB.remoteBot().editor(FILE).show();
        BOB.remoteBot().editor(FILE).waitUntilIsActive();
        BOB.remoteBot().editor(FILE).typeText("L");
        // both sleep for less than 1000ms
        ALICE.remoteBot().sleep(300);

        // Alice hits Delete again
        ALICE.remoteBot().activateWorkbench();
        ALICE.remoteBot().sleep(waitActivate);
        ALICE.remoteBot().editor(FILE).show();
        ALICE.remoteBot().editor(FILE).waitUntilIsActive();
        ALICE.remoteBot().editor(FILE).pressShortcut("DELETE");
        // Bob enters o
        BOB.remoteBot().activateWorkbench();
        BOB.remoteBot().sleep(waitActivate);
        BOB.remoteBot().editor(FILE).show();
        BOB.remoteBot().editor(FILE).waitUntilIsActive();
        BOB.remoteBot().editor(FILE).typeText("o");

        ALICE.remoteBot().sleep(5000);
        String ALICEText = ALICE.remoteBot().editor(FILE).getText();
        String BOBText = BOB.remoteBot().editor(FILE).getText();
        assertEquals(ALICEText, BOBText);
    }

    @Test(expected = AssertionError.class)
    public void AliceAndBobeditInSameLine() throws RemoteException,
        InterruptedException {

        ALICE
            .superBot()
            .views()
            .packageExplorerView()
            .tree()
            .newC()
            .javaProjectWithClasses(Constants.PROJECT1, Constants.PKG1,
                Constants.CLS1);
        Util.buildSessionConcurrently(Constants.PROJECT1,
            TypeOfCreateProject.NEW_PROJECT, ALICE, BOB);
        BOB.superBot().views().packageExplorerView()
            .selectClass(Constants.PROJECT1, Constants.PKG1, Constants.CLS1)
            .open();
        BOB.remoteBot().editor(Constants.CLS1_SUFFIX).waitUntilIsActive();

        ALICE.remoteBot().editor(Constants.CLS1_SUFFIX).navigateTo(3, 0);
        BOB.remoteBot().editor(Constants.CLS1_SUFFIX).navigateTo(3, 0);
        char[] content = "Merry Christmas and Happy New Year!".toCharArray();
        for (int i = 0; i < content.length; i++) {
            ALICE.remoteBot().editor(Constants.CLS1_SUFFIX)
                .typeText(content[i] + "");
            Thread.sleep(100);
            if (i != 0 && i % 2 == 0) {
                BOB.remoteBot().editor(Constants.CLS1_SUFFIX).navigateTo(3, i);
                BOB.remoteBot()
                    .editor(Constants.CLS1_SUFFIX)
                    .pressShortcut(IKeyLookup.DELETE_NAME,
                        IKeyLookup.DELETE_NAME);
            }
        }

        String ALICEText = ALICE.remoteBot().editor(Constants.CLS1_SUFFIX)
            .getText();
        String BOBText = BOB.remoteBot().editor(Constants.CLS1_SUFFIX)
            .getText();
        System.out.println(ALICEText);
        System.out.println(BOBText);
        assertEquals(ALICEText, BOBText);
        BOB.remoteBot().sleep(5000);
        assertTrue(BOB.remoteBot().view(VIEW_SAROS)
            .existsToolbarButton(TB_INCONSISTENCY_DETECTED));

    }
}
