package com.ebay.config;

import com.ebay.models.TestData.BrowserConfig;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

public final class BrowserFactory {

    private BrowserFactory() {
    }

    public static Browser launch(Playwright playwright, BrowserConfig config) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(config.headless())
                .setSlowMo(config.slowMoMs());
        if (config.args() != null) {
            options.setArgs(config.args());
        }

        return switch (config.name().toLowerCase()) {
            case "chromium" -> playwright.chromium().launch(options);
            case "chrome" -> playwright.chromium().launch(options.setChannel("chrome"));
            case "msedge", "edge" -> playwright.chromium().launch(options.setChannel("msedge"));
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> throw new IllegalArgumentException("Unsupported browser: " + config.name());
        };
    }
}
