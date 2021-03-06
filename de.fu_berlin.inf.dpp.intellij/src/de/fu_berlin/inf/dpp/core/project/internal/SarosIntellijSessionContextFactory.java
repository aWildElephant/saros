package de.fu_berlin.inf.dpp.core.project.internal;

import org.picocontainer.MutablePicoContainer;

import de.fu_berlin.inf.dpp.intellij.project.SharedResourcesManager;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionContextFactory;
import de.fu_berlin.inf.dpp.session.SarosCoreSessionContextFactory;

/**
 * IntelliJ implementation of the {@link ISarosSessionContextFactory} interface.
 */
public class SarosIntellijSessionContextFactory extends
    SarosCoreSessionContextFactory {

    @Override
    public void createNonCoreComponents(ISarosSession session,
        MutablePicoContainer container) {

        // Other
        container.addComponent(FollowingActivitiesManager.class);
        container.addComponent(SharedResourcesManager.class);
    }

}
