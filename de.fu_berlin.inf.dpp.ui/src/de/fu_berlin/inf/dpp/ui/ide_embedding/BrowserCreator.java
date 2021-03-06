package de.fu_berlin.inf.dpp.ui.ide_embedding;

import org.eclipse.swt.widgets.Composite;
import org.jivesoftware.smack.util.StringUtils;

import de.fu_berlin.inf.ag_se.browser.extensions.IJQueryBrowser;
import de.fu_berlin.inf.ag_se.browser.functions.JavascriptFunction;
import de.fu_berlin.inf.ag_se.browser.swt.SWTJQueryBrowser;
import de.fu_berlin.inf.dpp.HTMLUIContextFactory;
import de.fu_berlin.inf.dpp.ui.manager.BrowserManager;
import de.fu_berlin.inf.dpp.ui.pages.IBrowserPage;

/**
 * This class represents the IDE-independent part of the browser creation. It
 * resorts to IDE-specific resource location however by using the correct
 * instance of {@link IUiResourceLocator} which is injected by PicoContainer.
 */
public class BrowserCreator {

    private final BrowserManager browserManager;

    private final IUiResourceLocator resourceLocator;

    /**
     * Created by PicoContainer
     * 
     * @param browserManager
     * @param resourceLocator
     * @see HTMLUIContextFactory
     */
    public BrowserCreator(BrowserManager browserManager,
        IUiResourceLocator resourceLocator) {
        this.browserManager = browserManager;
        this.resourceLocator = resourceLocator;
    }

    /**
     * Creates a new browser instance.
     * 
     * @param composite
     *            the composite enclosing the browser.
     * @param style
     *            the style of the browser instance.
     * @param page
     *            the page which should be displayed.
     * @return a browser instance which loads and renders the given
     *         {@link IBrowserPage BrowserPage}
     */
    public IJQueryBrowser createBrowser(Composite composite, int style,
        final IBrowserPage page) {

        final String resourceName = page.getRelativePath();
        assert resourceName != null;

        final IJQueryBrowser browser = SWTJQueryBrowser.createSWTBrowser(
            composite, style);

        String resourceLocation = resourceLocator
            .getResourceLocation(resourceName);

        if (resourceLocation == null) {
            browser.setText("<html><body><pre>" + "Resource <b>"
                + StringUtils.escapeForXML(resourceName)
                + "</b> could not be found.</pre></body></html>");
            return browser;
        }

        browser.open(resourceLocation, 5000);

        for (JavascriptFunction function : page.getJavascriptFunctions()) {
            browser.createBrowserFunction(function);
        }

        browserManager.setBrowser(page, browser);
        browser.runOnDisposal(new Runnable() {
            @Override
            public void run() {
                browserManager.removeBrowser(page);
            }
        });

        return browser;
    }
}
