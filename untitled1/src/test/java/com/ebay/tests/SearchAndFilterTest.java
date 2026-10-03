package com.ebay.tests;

import com.ebay.base.BaseTest;
import com.ebay.models.TestData.HomeData;
import com.ebay.models.TestData.SearchData;
import com.ebay.models.TestData.ShipToData;
import com.ebay.pages.HomePage;
import com.ebay.pages.SearchResultsPage;
import com.ebay.pages.SearchResultsPage.ResultCheck;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Epic("eBay Web")
@Feature("Search")
public class SearchAndFilterTest extends BaseTest {

    @Test(description = "Search eBay and narrow the results with a left-hand filter")
    @Story("Search and filter results")
    @Severity(SeverityLevel.CRITICAL)
    @Description("""
            Opens the eBay home page, sets the ship-to location, searches, and checks every result \
            (on every results page) shows the search term. Then applies a left-hand filter, checks the \
            result count narrows, and checks every filtered result shows the filter value.""")
    public void searchAndFilterResults() {
        ShipToData shipTo = data.shipTo();
        HomeData home = data.home();
        SearchData search = data.search();
        String filter = search.filterGroup() + " -> " + search.filterOption();
        HomePage homePage = new HomePage(page);

        Allure.parameter("Search term", search.searchTerm());
        Allure.parameter("Filter", filter);
        Allure.parameter("Ship to", shipTo.country() + " " + shipTo.postalCode());

        Allure.step("Open eBay home page", () -> {
            homePage.open(home.url());
            verify("Home page URL starts with " + home.url(), () -> Assert.assertTrue(
                    homePage.currentUrl().startsWith(home.url()), "Unexpected home page URL: " + homePage.currentUrl()));
            verify("eBay logo is visible", () -> assertThat(homePage.logo()).isVisible());
            verify("Logo title is '" + home.expectedLogoTitle() + "'",
                    () -> assertThat(homePage.logoTitle()).hasText(home.expectedLogoTitle()));
            verify("Search box is visible", () -> assertThat(homePage.searchInput()).isVisible());
            captureStep("Home page");
        });

        Allure.step("Set ship-to location to " + shipTo.country(), () -> {
            homePage.setShipToLocation(shipTo.country(), shipTo.postalCode());
            verify("Header shows ship-to country '" + shipTo.country() + "'",
                    () -> assertThat(homePage.shipToCountryName()).hasText(shipTo.country()));
            captureStep("Ship-to location set");
        });

        SearchResultsPage resultsPage = Allure.step("Search for '" + search.searchTerm() + "'",
                () -> homePage.searchFor(search.searchTerm()).waitForResults());

        int initialCount = Allure.step("Check the search results", () -> {
            verify("Results heading mentions '" + search.searchTerm() + "'",
                    () -> assertThat(resultsPage.resultsHeading()).containsText(search.searchTerm()));
            verify("Result cards are displayed",
                    () -> Assert.assertTrue(resultsPage.resultCards().count() > 0, "No result cards were displayed"));

            int count = resultsPage.getResultsCount();
            log.info("Search '{}' returned {} results", search.searchTerm(), count);
            verify("At least one result was found", () -> Assert.assertTrue(count > 0, "Expected at least one search result"));
            captureStep("Search results");
            return count;
        });

        Allure.step("Check every result shows '" + search.searchTerm() + "'",
                () -> assertEveryResultShows(resultsPage, search.searchTerm()));

        Allure.step("Filter by " + filter, () -> {
            resultsPage.applyFilter(search.filterGroup(), search.filterOption());
            captureStep("Filtered results");
        });

        Allure.step("Check the result count narrowed", () -> {
            int filteredCount = resultsPage.getResultsCount();
            log.info("After filter {}: {} results (was {})", filter, filteredCount, initialCount);
            verify("Result count narrowed from " + initialCount + " to " + filteredCount,
                    () -> Assert.assertTrue(filteredCount < initialCount,
                            "Filtering should narrow results: before=" + initialCount + ", after=" + filteredCount));
            verify("Filter still returns results",
                    () -> Assert.assertTrue(filteredCount > 0, "Filter returned no results"));
        });

        Allure.step("Check every result shows '" + search.filterOption() + "'",
                () -> assertEveryResultShows(resultsPage, search.filterOption()));
    }

    private void assertEveryResultShows(SearchResultsPage resultsPage, String text) {
        List<ResultCheck> checks = resultsPage.checkResultsShow(text);
        Allure.addAttachment("Results checked for '" + text + "'", "text/csv", toCsv(checks), ".csv");

        List<ResultCheck> mismatches = checks.stream().filter(c -> !c.matches()).toList();
        verify("All " + checks.size() + " results show '" + text + "'", () -> Assert.assertTrue(mismatches.isEmpty(),
                mismatches.size() + " of " + checks.size() + " result(s) don't show '" + text + "':\n"
                        + mismatches.stream().map(ResultCheck::toString).collect(Collectors.joining("\n"))));
    }

    private static String toCsv(List<ResultCheck> checks) {
        StringBuilder csv = new StringBuilder("Page,Result,Title,Attributes\n");
        for (ResultCheck c : checks) {
            csv.append(c.page()).append(',')
                    .append(c.matches() ? "PASS" : "FAIL").append(',')
                    .append(csvCell(c.title())).append(',')
                    .append(csvCell(c.attributes())).append('\n');
        }
        return csv.toString();
    }

    private static String csvCell(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
