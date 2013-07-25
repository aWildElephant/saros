/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.net.internal.extensions;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;

import de.fu_berlin.inf.dpp.activities.SPathDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.AbstractActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.ChecksumActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.ChecksumErrorActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.EditorActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.FileActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.FolderActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.JupiterActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.PermissionActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.ProgressActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.RecoveryFileActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.TextSelectionActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.VCSActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.ViewportActivityDataObject;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.JupiterVectorTime;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.text.DeleteOperation;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.text.InsertOperation;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.text.NoOperation;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.text.SplitOperation;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.text.TimestampOperation;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.TimedActivities;
import de.fu_berlin.inf.dpp.net.internal.TimedActivityDataObject;

/* TODO correct this class to use the correct provider */

@Component(module = "net")
public class ActivitiesExtension extends SarosPacketExtension {

    public static final Provider PROVIDER = new Provider();

    /**
     * @JTourBusStop 7, Activity creation, IActivityDataObject registration:
     * 
     *               All IActivityDataObject implementations should be
     *               registered with the XStream extensions provider, otherwise
     *               annotations like XStreamAlias will not be honored.
     */

    /**
     * 
     */

    public static class Provider extends
        XStreamExtensionProvider<TimedActivities> {
        private Provider() {
            super("activityDataObjects", TimedActivities.class,
                AbstractActivityDataObject.class,
                EditorActivityDataObject.class, FileActivityDataObject.class,
                RecoveryFileActivityDataObject.class,
                FolderActivityDataObject.class,
                PermissionActivityDataObject.class,
                TextSelectionActivityDataObject.class,
                ViewportActivityDataObject.class,
                TimedActivityDataObject.class, JupiterActivityDataObject.class,
                JupiterVectorTime.class, DeleteOperation.class,
                InsertOperation.class, NoOperation.class, SplitOperation.class,
                TimestampOperation.class, JID.class, SPathDataObject.class,
                ChecksumActivityDataObject.class,
                ChecksumErrorActivityDataObject.class,
                ProgressActivityDataObject.class, VCSActivityDataObject.class);
        }

        public PacketExtension create(String sessionID,
            List<TimedActivityDataObject> activities) {
            return create(new TimedActivities(sessionID,
                new ArrayList<TimedActivityDataObject>(activities)));
        }
    }
}
