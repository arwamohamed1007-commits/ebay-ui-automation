package com.ebay.pages;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class HomePage extends BasePage {

    private static final Pattern SEARCH_RESULTS_URL = Pattern.compile("/sch/");
    private static final double SHIP_TO_CLICK_TIMEOUT_MS = 5000;

    private final Locator logoTitle;
    private final Locator logo;
    private final Locator searchInput;
    private final Locator searchButton;
    private final Locator shipToButton;
    private final Locator shipToCountryName;
    private final Locator shipToPrompt;
    private final FrameLocator shipToForm;

    public HomePage(Page page) {
        super(page);
        this.logoTitle = page.locator("#ebayLogoTitle");
        this.logo = page.locator("svg:has(#ebayLogoTitle)");
        this.searchInput = page.locator("#gh-ac");
        this.searchButton = page.locator("button:has(.gh-search-button__label)");

        this.shipToButton = page.locator(".gh-ship-to__menu");
        this.shipToCountryName = page.locator(".gh-ship-to__menu-country-name");

        Locator visibleShipToForm = page.locator("iframe[src*='buyer-preferences/shipping-address']")
                .filter(new Locator.FilterOptions().setVisible(true));
        this.shipToPrompt = visibleShipToForm.and(page.locator("iframe[src*='flowType=NBA']"));
        this.shipToForm = visibleShipToForm.first().contentFrame();
    }

    public Locator shipToCountryName() {
        return shipToCountryName;
    }

    public HomePage setShipToLocation(String country, String postalCode) {
        log.info("Setting ship-to location to {} {}", country, postalCode);
        if (isVisible(shipToPrompt)) {
            log.info("eBay's ship-to prompt is already open - using it");
        } else {
            openShipToDialog();
        }
        shipToForm.locator("#country").selectOption(new SelectOption().setLabel(country));
        Locator postalCodeInput = shipToForm.locator("#postalCode");
        if (isVisible(postalCodeInput)) {
            type(postalCodeInput, postalCode);
        }
        click(shipToForm.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("Confirm")));

        assertThat(shipToCountryName).hasText(country);
        page.waitForLoadState(LoadState.LOAD);
        return this;
    }

    private void openShipToDialog() {
        try {
            shipToButton.click(new Locator.ClickOptions().setTimeout(SHIP_TO_CLICK_TIMEOUT_MS));
        } catch (TimeoutError e) {
            log.info("eBay's ship-to prompt opened over the button - using it");
        }
    }

    public HomePage open(String url) {
        navigate(url);
        page.waitForLoadState(LoadState.LOAD);
        return this;
    }

    public Locator logo() {
        return logo;
    }

    public Locator logoTitle() {
        return logoTitle;
    }

    public Locator searchInput() {
        return searchInput;
    }

    public SearchResultsPage searchFor(String term) {
        log.info("Searching for '{}'", term);
        type(searchInput, term);
        assertThat(searchInput).hasValue(term);
        click(searchButton);
        page.waitForURL(SEARCH_RESULTS_URL);
        return new SearchResultsPage(page);
    }
}
