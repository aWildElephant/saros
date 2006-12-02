/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitšt Berlin - Fachbereich Mathematik und Informatik - 2006
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
package de.fu_berlin.inf.dpp.net.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import de.fu_berlin.inf.dpp.activities.TextSelectionActivity;
import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.TextEditActivity;
import de.fu_berlin.inf.dpp.net.IActivitySequencer;
import de.fu_berlin.inf.dpp.net.TimedActivity;
import de.fu_berlin.inf.dpp.project.IActivityManager;
import de.fu_berlin.inf.dpp.project.IActivityProvider;

/**
 * Implements {@link IActivitySequencer} and {@link IActivityManager}.
 * 
 * @author rdjemili
 */
public class ActivitySequencer implements IActivitySequencer, IActivityManager {
	// TODO separate into two classes!?

	private static Logger log = Logger.getLogger(ActivitySequencer.class.getName());

	private List<IActivity> activities = new LinkedList<IActivity>();

	private List<IActivity> flushedLog = new LinkedList<IActivity>();

	private List<IActivityProvider> providers = new LinkedList<IActivityProvider>();

	private List<TimedActivity> queue = new CopyOnWriteArrayList<TimedActivity>();

	private int timestamp = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.project.IActivityManager
	 */
	public void exec(IActivity activity) {
		try {
			for (IActivityProvider executor : providers) {
				executor.exec(activity);
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while executing activity.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.net.IActivitySequencer
	 */
	public void exec(TimedActivity timedActivity) {
		queue.add(timedActivity);
		execQueue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.IActivitySequencer
	 */
	public void exec(List<TimedActivity> activities) {
		queue.addAll(activities);
		execQueue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.IActivityManager
	 */
	public List<IActivity> flush() {
		List<IActivity> out = new ArrayList<IActivity>(activities);
		activities.clear();
		out = optimize(out);
		flushedLog.addAll(out);
		return out.size() > 0 ? out : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.net.IActivitySequencer
	 */
	public List<TimedActivity> flushTimed() {
		List<IActivity> activities = flush();

		if (activities == null)
			return null;

		List<TimedActivity> timedActivities = new ArrayList<TimedActivity>();

		for (IActivity activity : activities) {
			timedActivities.add(new TimedActivity(activity, timestamp++));
		}

		return timedActivities;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.IActivityManager
	 */
	public void addProvider(IActivityProvider provider) {
		providers.add(provider);
		provider.addActivityListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.IActivityManager
	 */
	public void removeProvider(IActivityProvider provider) {
		providers.remove(provider);
		provider.removeActivityListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.IActivitySequencer
	 */
	public List<IActivity> getLog() {
		return flushedLog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.IActivityListener
	 */
	public void activityCreated(IActivity activity) {
		activities.add(activity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fu_berlin.inf.dpp.net.IActivitySequencer
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Executes as much activities as possible from the current queue regarding
	 * to their individual time stamps.
	 */
	private void execQueue() {
		boolean executed = false;

		for (TimedActivity timedActivity : queue) {
			if (timedActivity.getTimestamp() <= timestamp) { // HACK
				queue.remove(timedActivity);

				timestamp++;
				exec(timedActivity.getActivity());
				executed = true;
			}
		}

		if (executed)
			execQueue();
	}

	// TODO extract this into the activities themselves
	private List<IActivity> optimize(List<IActivity> activities) {
		List<IActivity> result = new ArrayList<IActivity>(activities.size());

		ITextSelection selection = null;
		for (IActivity activity : activities) {
			if (activity instanceof TextEditActivity) {
				TextEditActivity textEdit = (TextEditActivity) activity;

				textEdit = joinTextEdits(result, textEdit);

				selection = new TextSelection(textEdit.offset + textEdit.text.length(), 0);
				result.add(textEdit);

			} else if (activity instanceof TextSelectionActivity) {
				TextSelectionActivity textSelection = (TextSelectionActivity) activity;

				selection = new TextSelection(textSelection.getOffset(), textSelection.getLength());

			} else {
				selection = addSelection(result, selection);
				result.add(activity);
			}
		}

		selection = addSelection(result, selection);

		return result;
	}

	private TextEditActivity joinTextEdits(List<IActivity> result, TextEditActivity textEdit) {
		if (result.size() == 0)
			return textEdit;

		IActivity lastActivity = result.get(result.size() - 1);
		if (lastActivity instanceof TextEditActivity) {
			TextEditActivity lastTextEdit = (TextEditActivity) lastActivity;

			if (textEdit.offset == lastTextEdit.offset + lastTextEdit.text.length()) {
				result.remove(lastTextEdit);
				textEdit = new TextEditActivity(lastTextEdit.offset, lastTextEdit.text
					+ textEdit.text, lastTextEdit.replace + textEdit.replace);
			}
		}

		return textEdit;
	}

	private ITextSelection addSelection(List<IActivity> result, ITextSelection selection) {
		if (selection == null)
			return null;

		if (result.size() > 0) {
			IActivity lastActivity = result.get(result.size() - 1);
			if (lastActivity instanceof TextEditActivity) {
				TextEditActivity lastTextEdit = (TextEditActivity) lastActivity;

				if (selection.getOffset() == lastTextEdit.offset + lastTextEdit.text.length()
					&& selection.getLength() == 0) {

					return selection;
				}
			}
		}

		result.add(new TextSelectionActivity(selection.getOffset(), selection.getLength()));

		selection = null;
		return selection;
	}
}
