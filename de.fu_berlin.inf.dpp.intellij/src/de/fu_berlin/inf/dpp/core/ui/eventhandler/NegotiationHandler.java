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

package de.fu_berlin.inf.dpp.core.ui.eventhandler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import de.fu_berlin.inf.dpp.core.Saros;
import de.fu_berlin.inf.dpp.core.monitoring.IStatus;
import de.fu_berlin.inf.dpp.core.monitoring.Status;
import de.fu_berlin.inf.dpp.intellij.runtime.UIMonitoredJob;
import de.fu_berlin.inf.dpp.intellij.ui.Messages;
import de.fu_berlin.inf.dpp.intellij.ui.util.DialogUtils;
import de.fu_berlin.inf.dpp.intellij.ui.util.NotificationPanel;
import de.fu_berlin.inf.dpp.intellij.ui.wizards.AddProjectToSessionWizard;
import de.fu_berlin.inf.dpp.intellij.ui.wizards.JoinSessionWizard;
import de.fu_berlin.inf.dpp.monitoring.IProgressMonitor;
import de.fu_berlin.inf.dpp.negotiation.FileList;
import de.fu_berlin.inf.dpp.negotiation.IncomingProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.IncomingSessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.OutgoingProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.OutgoingSessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.ProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.ProjectNegotiationData;
import de.fu_berlin.inf.dpp.negotiation.SessionNegotiation;
import de.fu_berlin.inf.dpp.net.util.XMPPUtils;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.session.INegotiationHandler;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.SarosSessionManager;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This handler is responsible for presenting and running the session and
 * project negotiations that are received by the Saros Session Manager
 * component.
 */
public class NegotiationHandler implements INegotiationHandler {

    private static final Logger LOG = Logger
        .getLogger(NegotiationHandler.class);
    private final ISarosSessionManager sessionManager;

    // FIXME use ISarosSessionManager interface
    public NegotiationHandler(SarosSessionManager sessionManager) {
        sessionManager.setNegotiationHandler(this);
        this.sessionManager = sessionManager;

    }

    private static String getNickname(JID jid) {
        return XMPPUtils.getNickname(null, jid, jid.getBase());
    }

    @Override
    public void handleOutgoingSessionNegotiation(
        OutgoingSessionNegotiation negotiation) {

        OutgoingInvitationJob outgoingInvitationJob = new OutgoingInvitationJob(
            negotiation);

        outgoingInvitationJob.schedule();
    }

    @Override
    public void handleIncomingSessionNegotiation(
        IncomingSessionNegotiation negotiation) {
        showIncomingInvitationUI(negotiation);
    }

    @Override
    public void handleOutgoingProjectNegotiation(
        OutgoingProjectNegotiation negotiation) {

        OutgoingProjectJob job = new OutgoingProjectJob(negotiation);
        job.schedule();
    }

    @Override
    public void handleIncomingProjectNegotiation(
        IncomingProjectNegotiation negotiation) {
        showIncomingProjectUI(negotiation);
    }

    private void showIncomingInvitationUI(
        final IncomingSessionNegotiation negotiation) {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                JoinSessionWizard wizard = new JoinSessionWizard(negotiation);
                wizard.setModal(true);
                wizard.open();
            }
        }, ModalityState.defaultModalityState());

    }

    private void showIncomingProjectUI(
        final IncomingProjectNegotiation negotiation) {

        List<ProjectNegotiationData> pInfos = negotiation.getProjectInfos();
        final List<FileList> fileLists = new ArrayList<FileList>(pInfos.size());

        for (ProjectNegotiationData pInfo : pInfos) {
            fileLists.add(pInfo.getFileList());
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                AddProjectToSessionWizard wizard = new AddProjectToSessionWizard(
                    negotiation, negotiation.getPeer(), fileLists,
                    negotiation.getProjectNames());
                wizard.setModal(false);
                wizard.open();

            }
        }, ModalityState.defaultModalityState());
    }

    /**
     * OutgoingInvitationJob wraps the instance of
     * {@link OutgoingSessionNegotiation} and cares about handling the
     * exceptions like local or remote cancellation.
     * <p/>
     * It notifies the user about the progress using the Eclipse Jobs API and
     * interrupts the negotiation if the session closes.
     */
    private class OutgoingInvitationJob extends UIMonitoredJob {

        private final OutgoingSessionNegotiation negotiation;
        private final String peer;

        public OutgoingInvitationJob(OutgoingSessionNegotiation negotiation) {
            super(MessageFormat.format(
                Messages.NegotiationHandler_inviting_user,
                getNickname(negotiation.getPeer())));
            this.negotiation = negotiation;
            peer = negotiation.getPeer().getBase();
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                SessionNegotiation.Status status = negotiation.start(monitor);

                switch (status) {
                case CANCEL:
                    return Status.CANCEL_STATUS;
                case ERROR:
                    return new Status(IStatus.ERROR, Saros.PLUGIN_ID,
                        negotiation.getErrorMessage());
                case OK:
                    break;
                case REMOTE_CANCEL:
                    NotificationPanel
                        .showNotification(
                            Messages.NegotiationHandler_canceled_invitation,
                            MessageFormat
                                .format(
                                    Messages.NegotiationHandler_canceled_invitation_text,
                                    peer));

                    return new Status(
                        IStatus.CANCEL,
                        Saros.PLUGIN_ID,
                        MessageFormat
                            .format(
                                Messages.NegotiationHandler_canceled_invitation_text,
                                peer));

                case REMOTE_ERROR:
                    NotificationPanel
                        .showNotification(
                            Messages.NegotiationHandler_error_during_invitation,
                            MessageFormat
                                .format(
                                    Messages.NegotiationHandler_error_during_invitation_text,
                                    peer, negotiation.getErrorMessage()));

                    return new Status(
                        IStatus.ERROR,
                        Saros.PLUGIN_ID,
                        MessageFormat
                            .format(
                                Messages.NegotiationHandler_error_during_invitation_text,
                                peer, negotiation.getErrorMessage()));
                }
            } catch (Exception e) {
                LOG.error("This exception is not expected here: ", e);
                return new Status(IStatus.ERROR, Saros.PLUGIN_ID,
                    e.getMessage(), e);

            }

            sessionManager.startSharingProjects(negotiation.getPeer());

            return Status.OK_STATUS;
        }
    }

    private class OutgoingProjectJob extends UIMonitoredJob {

        private final OutgoingProjectNegotiation negotiation;
        private final String peer;

        public OutgoingProjectJob(
            OutgoingProjectNegotiation outgoingProjectNegotiation) {
            super(Messages.NegotiationHandler_sharing_project);
            negotiation = outgoingProjectNegotiation;
            peer = negotiation.getPeer().getBase();
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                ProjectNegotiation.Status status = negotiation.run(monitor);
                String peerName = getNickname(new JID(peer));

                final String message;

                switch (status) {
                case CANCEL:
                    return Status.CANCEL_STATUS;
                case ERROR:
                    return new Status(IStatus.ERROR, Saros.PLUGIN_ID,
                        negotiation.getErrorMessage());
                case OK:
                    break;
                case REMOTE_CANCEL:
                    message = MessageFormat
                        .format(
                            Messages.NegotiationHandler_project_sharing_canceled_text,
                            peerName);

                    ApplicationManager.getApplication().invokeLater(
                        new Runnable() {
                            @Override
                            public void run() {
                                DialogUtils
                                    .showInfo(
                                        null,
                                        message,
                                        Messages.NegotiationHandler_project_sharing_canceled_text);
                            }
                        });

                    return new Status(IStatus.CANCEL, Saros.PLUGIN_ID, message);

                case REMOTE_ERROR:
                    message = MessageFormat
                        .format(
                            Messages.NegotiationHandler_sharing_project_canceled_remotely,
                            peerName, negotiation.getErrorMessage());
                    NotificationPanel
                        .showNotification(
                            Messages.NegotiationHandler_sharing_project_canceled_remotely_text,
                            message);

                    return new Status(IStatus.ERROR, Saros.PLUGIN_ID, message);
                }
            } catch (Exception e) {
                LOG.error("This exception is not expected here: ", e);
                return new Status(IStatus.ERROR, Saros.PLUGIN_ID,
                    e.getMessage(), e);

            }

            return Status.OK_STATUS;
        }
    }
}
