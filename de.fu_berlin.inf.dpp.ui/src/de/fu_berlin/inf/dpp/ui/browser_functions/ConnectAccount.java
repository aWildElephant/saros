package de.fu_berlin.inf.dpp.ui.browser_functions;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import de.fu_berlin.inf.ag_se.browser.functions.JavascriptFunction;
import de.fu_berlin.inf.dpp.HTMLUIContextFactory;
import de.fu_berlin.inf.dpp.HTMLUIStrings;
import de.fu_berlin.inf.dpp.ui.JavaScriptAPI;
import de.fu_berlin.inf.dpp.ui.core_facades.StateFacade;
import de.fu_berlin.inf.dpp.ui.model.Account;
import de.fu_berlin.inf.dpp.util.ThreadUtils;

/**
 * Offers a via Javascript invokable method to connect a given Account. *
 * <p>
 * JS-signature: "void __java_connect(String Account);"
 */
public class ConnectAccount extends JavascriptFunction {
    private static final Logger LOG = Logger.getLogger(ConnectAccount.class);

    private final StateFacade stateFacade;
    public static final String JS_NAME = "connect";

    /**
     * Created by PicoContainer
     * 
     * @param stateFacade
     * @see HTMLUIContextFactory
     */
    public ConnectAccount(StateFacade stateFacade) {
        super(NameCreator.getConventionName(JS_NAME));
        this.stateFacade = stateFacade;
    }

    @Override
    public Object function(Object[] arguments) {
        if (arguments.length > 0 && arguments[0] != null) {
            Gson gson = new Gson();
            final Account account = gson.fromJson((String) arguments[0],
                Account.class);
            ThreadUtils.runSafeAsync(LOG, new Runnable() {
                @Override
                public void run() {
                    stateFacade.connect(account);
                }
            });
        } else {
            LOG.error("Connect was called without an account.");
            JavaScriptAPI.showError(browser,
                HTMLUIStrings.CONNECT_WITHOUT_ACCOUNT);
        }
        return null;
    }
}
