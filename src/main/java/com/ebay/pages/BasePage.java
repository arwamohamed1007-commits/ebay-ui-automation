package com.ebay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasePage {

    protected final Page page;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected BasePage(Page page) {
        this.page = page;
    }

    protected void navigate(String url) {
        log.info("Navigating to {}", url);
        page.navigate(url);
    }

    protected void click(Locator locator) {
        locator.click();
    }

    protected void type(Locator locator, String text) {
        locator.fill(text);
    }

    protected void waitForVisible(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    protected boolean isVisible(Locator locator) {
        return locator.isVisible();
    }

    protected void scrollIntoView(Locator locator) {
        locator.scrollIntoViewIfNeeded();
    }

    protected String getText(Locator locator) {
        return locator.innerText().trim();
    }

    public String currentUrl() {
        return page.url();
    }
}
