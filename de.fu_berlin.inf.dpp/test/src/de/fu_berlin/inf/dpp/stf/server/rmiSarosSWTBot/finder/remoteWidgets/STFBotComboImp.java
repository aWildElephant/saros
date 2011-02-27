package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets;

import java.rmi.RemoteException;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;

public class STFBotComboImp extends AbstractRmoteWidget implements STFBotCombo {

    private static transient STFBotComboImp self;

    private SWTBotCombo widget;

    /**
     * {@link STFBotComboImp} is a singleton, but inheritance is possible.
     */
    public static STFBotComboImp getInstance() {
        if (self != null)
            return self;
        self = new STFBotComboImp();
        return self;
    }

    public STFBotCombo setWidget(SWTBotCombo ccomb) {
        this.widget = ccomb;
        return this;
    }

    /**************************************************************
     * 
     * exported functions
     * 
     **************************************************************/

    /**********************************************
     * 
     * finders
     * 
     **********************************************/
    public STFBotMenu contextMenu(String text) throws RemoteException {
        return stfBotMenu.setWidget(widget.contextMenu(text));
    }

    /**********************************************
     * 
     * actions
     * 
     **********************************************/

    public void typeText(String text) throws RemoteException {
        widget.typeText(text);

    }

    public void typeText(String text, int interval) throws RemoteException {
        widget.typeText(text, interval);
    }

    public void setFocus() throws RemoteException {
        widget.setFocus();
    }

    public void setText(String text) throws RemoteException {
        widget.setText(text);
    }

    public void setSelection(String text) throws RemoteException {
        widget.setSelection(text);
    }

    public void setSelection(int index) throws RemoteException {
        widget.setSelection(index);
    }

    /**********************************************
     * 
     * states
     * 
     **********************************************/
    public int itemCount() throws RemoteException {
        return widget.itemCount();
    }

    public String[] items() throws RemoteException {
        return widget.items();
    }

    public String selection() throws RemoteException {
        return widget.selection();
    }

    public int selectionIndex() throws RemoteException {
        return widget.selectionIndex();
    }

    public boolean isEnabled() throws RemoteException {
        return widget.isEnabled();
    }

    public boolean isVisible() throws RemoteException {
        return widget.isVisible();
    }

    public boolean isActive() throws RemoteException {
        return widget.isActive();
    }

    public String getText() throws RemoteException {
        return widget.getText();
    }

    public String getToolTipText() throws RemoteException {
        return widget.getText();
    }

}