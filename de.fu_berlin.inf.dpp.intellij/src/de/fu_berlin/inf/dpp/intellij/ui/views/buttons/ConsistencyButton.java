/*
 *
 *  DPP - Serious Distributed Pair Programming
 *  (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2010
 *  (c) NFQ (www.nfq.com) - 2014
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 1, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * /
 */

package de.fu_berlin.inf.dpp.intellij.ui.views.buttons;

import com.intellij.openapi.application.ApplicationManager;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.concurrent.watchdog.ConsistencyWatchdogClient;
import de.fu_berlin.inf.dpp.concurrent.watchdog.IsInconsistentObservable;
import de.fu_berlin.inf.dpp.intellij.ui.Messages;
import de.fu_berlin.inf.dpp.intellij.ui.actions.ConsistencyAction;
import de.fu_berlin.inf.dpp.intellij.ui.util.DialogUtils;
import de.fu_berlin.inf.dpp.intellij.ui.util.NotificationPanel;
import de.fu_berlin.inf.dpp.observables.ValueChangeListener;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.ISessionLifecycleListener;
import de.fu_berlin.inf.dpp.session.NullSessionLifecycleListener;
import de.fu_berlin.inf.dpp.session.SessionEndReason;

import org.apache.log4j.Logger;
import org.picocontainer.annotations.Inject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Button for triggering a {@link ConsistencyAction}. Displays a different symbol
 * when state is inconsistent or not.
 *
 * FIXME: Remove awkward session handling together with UI components created
 * with session.
 */
public class ConsistencyButton extends ToolbarButton {
    private static final Logger LOG = Logger.getLogger(ConsistencyButton.class);

    private static final String IN_SYNC_ICON_PATH = "/icons/famfamfam/in_sync.png";
    private static final String OUT_SYNC_ICON_PATH = "/icons/famfamfam/out_sync.png";

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled() || sessionInconsistencyState == null)
                return;

            if (!sessionInconsistencyState.isInconsistent)
                return;

            setEnabledFromUIThread(false);

            final Set<SPath> paths = new HashSet<SPath>(
                sessionInconsistencyState.watchdogClient.getPathsWithWrongChecksums());

            String inconsistentFiles = createConfirmationMessage(paths);

            if (!DialogUtils.showQuestion(null, inconsistentFiles,
                Messages.ConsistencyAction_confirm_dialog_title)) {
                setEnabledFromUIThread(true);
                return;
            }

            sessionInconsistencyState.action.execute();

        }
    };

    private final ISessionLifecycleListener sessionLifecycleListener = new NullSessionLifecycleListener() {
        @Override
        public void sessionStarted(ISarosSession newSarosSession) {
            setSarosSession(newSarosSession);
            setEnabledFromUIThread(true);
        }

        @Override
        public void sessionEnded(ISarosSession oldSarosSession,
            SessionEndReason reason) {
            setSarosSession(null);
            setEnabledFromUIThread(false);
        }
    };

    private final ValueChangeListener<Boolean> isConsistencyListener = new ValueChangeListener<Boolean>() {

        @Override
        public void setValue(Boolean newValue) {
            handleConsistencyChange(newValue);
        }
    };

    @Inject
    private ISarosSessionManager sessionManager;

    @Inject
    private IsInconsistentObservable inconsistentObservable;

    private volatile SessionInconsistencyState sessionInconsistencyState;

    /**
     * Creates a Consistency button, adds a sessionListener and disables the button.
     */
    public ConsistencyButton() {
        super(ConsistencyAction.NAME, "Recover inconsistencies",
            IN_SYNC_ICON_PATH, "Files are consistent");
        SarosPluginContext.initComponent(this);

        setSarosSession(sessionManager.getSarosSession());
        sessionManager.addSessionLifecycleListener(sessionLifecycleListener);

        addActionListener(actionListener);
        setEnabled(false);
    }

    private class SessionInconsistencyState {

        private ConsistencyAction action;

        private final ActionListener consistencyActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setInconsistent(
                    !watchdogClient.getPathsWithWrongChecksums().isEmpty());
            }
        };

        private boolean isInconsistent = false;

        private ConsistencyWatchdogClient watchdogClient;

        /**
         * Creates an object to store the inconsistency warning state for a
         * session.
         */
        public SessionInconsistencyState(ISarosSession sarosSession) {

            watchdogClient = (ConsistencyWatchdogClient) sarosSession
                .getComponent(ConsistencyWatchdogClient.class);

            action = new ConsistencyAction(watchdogClient);
            action.addActionListener(consistencyActionListener);
        }

    }

    public void setInconsistent(boolean isInconsistent) {
        sessionInconsistencyState.isInconsistent = isInconsistent;

        if (isInconsistent) {
            setEnabledFromUIThread(true);
            setIcon(OUT_SYNC_ICON_PATH, "Files are NOT consistent");
        } else {
            setEnabledFromUIThread(false);
            setIcon(IN_SYNC_ICON_PATH, "Files are consistent");
        }
    }

    private void setSarosSession(ISarosSession newSession) {
        if (sessionInconsistencyState != null) {
            inconsistentObservable.remove(isConsistencyListener);
        }

        if (newSession != null)
            sessionInconsistencyState = new SessionInconsistencyState(newSession);

        if (sessionInconsistencyState != null) {
            inconsistentObservable.addAndNotify(isConsistencyListener);
        }
    }

    /**
     * This method activates the consistency recovery button, if an inconsistency
     * was detected and displays a tooltip.
     */
    private void handleConsistencyChange(final Boolean isInconsistent) {

        LOG.debug(
            "Inconsistency indicator goes: " + (isInconsistent ? "on" : "off"));

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                setInconsistent(isInconsistent);
            }
        });

        if (!isInconsistent) {
            showNotification(
                Messages.ConsistencyAction_tooltip_no_inconsistency);
            return;
        }

        final Set<SPath> paths = new HashSet<SPath>(
            sessionInconsistencyState.watchdogClient.getPathsWithWrongChecksums());

        final String files = createInconsistentPathsMessage(paths);

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                // set tooltip
                showNotification(MessageFormat.format(
                    Messages.ConsistencyAction_tooltip_inconsistency_detected,
                    files));

                NotificationPanel.showNotification(
                    Messages.ConsistencyAction_title_inconsistency_detected,
                    MessageFormat.format(
                        Messages.ConsistencyAction_message_inconsistency_detected,
                        files)
                );
            }
        });
    }

    private String createConfirmationMessage(Set<SPath> paths) {
        StringBuilder sbInconsistentFiles = new StringBuilder();
        for (SPath path : paths) {
            sbInconsistentFiles.append("project: ");
            sbInconsistentFiles.append(path.getProject().getName());
            sbInconsistentFiles.append(", file: ");
            sbInconsistentFiles
                .append(path.getProjectRelativePath().toOSString());
            sbInconsistentFiles.append("\n");

        }

        sbInconsistentFiles.append("Please confirm project modifications.\n\n"
            + "                + The recovery process will perform changes to files and folders of the current shared project(s).\n\n"
            + "                + The affected files and folders may be either modified, created, or deleted.");
        return sbInconsistentFiles.toString();
    }

    private String createInconsistentPathsMessage(Set<SPath> paths) {
        StringBuilder sb = new StringBuilder();

        for (SPath path : paths) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(path.getFullPath().toOSString());
        }

        return sb.toString();
    }

    private void showNotification(String text) {
        NotificationPanel.showNotification(text, "Consistency warning");
    }
}
