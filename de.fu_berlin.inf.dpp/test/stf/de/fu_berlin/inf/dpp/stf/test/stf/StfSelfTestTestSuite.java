package de.fu_berlin.inf.dpp.stf.test.stf;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    de.fu_berlin.inf.dpp.stf.test.stf.basicwidget.TestSuite.class,
    de.fu_berlin.inf.dpp.stf.test.stf.contextmenu.TestSuite.class,
    de.fu_berlin.inf.dpp.stf.test.stf.editor.TestSuite.class,
    de.fu_berlin.inf.dpp.stf.test.stf.menubar.TestSuite.class,
    de.fu_berlin.inf.dpp.stf.test.stf.view.TestSuite.class,
    de.fu_berlin.inf.dpp.stf.test.stf.view.sarosview.TestSuite.class,
    de.fu_berlin.inf.dpp.stf.test.stf.view.sarosview.content.TestSuite.class })
public class StfSelfTestTestSuite {
    // the class remains completely empty,
    // being used only as a holder for the above annotations
}