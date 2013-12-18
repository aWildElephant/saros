package de.fu_berlin.inf.dpp.ui.widgetGallery.demoSuits.preferences;

import org.eclipse.jface.preference.IPreferencePage;

import de.fu_berlin.inf.dpp.ui.preferencePages.AdvancedPreferencePage;
import de.fu_berlin.inf.dpp.ui.widgetGallery.annotations.Demo;
import de.fu_berlin.inf.dpp.ui.widgetGallery.demoExplorer.PreferencePageDemo;

@Demo
public class AdvancedPageDemo extends PreferencePageDemo {

    @Override
    public IPreferencePage getPreferencePage() {
        return new AdvancedPreferencePage();
    }

}
