package com.ebay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SearchResultsPage extends BasePage {

    private static final String PLACEHOLDER_TITLE = "Shop on eBay";

    private final Locator resultsHeading;
    private final Locator resultsCount;
    private final Locator resultCards;
    private final Locator currentPageNumber;
    private final Locator nextPageLink;

    public SearchResultsPage(Page page) {
        super(page);
        this.resultsHeading = page.locator("#srp-results-heading");
        this.resultsCount = resultsHeading.locator("span.BOLD").first();
        this.resultCards = page.locator("li.s-card");
        Locator pagination = page.locator(".s-pagination__container nav.pagination");
        this.currentPageNumber = pagination.locator("a.pagination__item[aria-current='page']");
        this.nextPageLink = pagination.locator("a.pagination__next");
    }

    public SearchResultsPage waitForResults() {
        waitForVisible(resultsHeading);
        waitForVisible(resultCards.first());
        return this;
    }

    public Locator resultsHeading() {
        return resultsHeading;
    }

    public Locator resultCards() {
        return resultCards;
    }

    public String resultsHeadingText() {
        return getText(resultsHeading);
    }

    public int getResultsCount() {
        String raw = getText(resultsCount).replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            throw new IllegalStateException("Could not read result count from heading: " + resultsHeadingText());
        }
        return Integer.parseInt(raw);
    }

    public SearchResultsPage applyFilter(String group, String option) {
        log.info("Applying filter {} -> {}", group, option);
        String headingBefore = resultsHeadingText();

        Locator groupLabel = page.locator("span.details__label", new Page.LocatorOptions().setHasText(group)).first();
        scrollIntoView(groupLabel);

        Locator groupContainer = groupLabel.locator(
                "xpath=ancestor::*[.//*[contains(@class,'su-selection-group__label--container')]][1]");
        Locator optionLabel = groupContainer.locator(".su-selection-group__label--container")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + escapeForJsRegex(option) + "\\b")))
                .first();

        if (!isVisible(optionLabel)) {
            click(groupLabel);
        }
        waitForVisible(optionLabel);
        click(optionLabel);

        assertThat(resultsHeading).not().hasText(headingBefore);
        return waitForResults();
    }

    public record ResultCheck(int page, String title, String attributes, boolean matches) {
        @Override
        public String toString() {
            return "Page " + page + ": " + title + " | " + attributes;
        }
    }

    public List<ResultCheck> checkResultsShow(String text) {
        String startUrl = currentUrl();
        List<ResultCheck> checks = new ArrayList<>();
        int pageNumber = 1;
        while (true) {
            checks.addAll(checkCurrentPage(text, pageNumber));
            if (!hasNextPage()) {
                break;
            }
            goToNextPage();
            pageNumber++;
        }
        long mismatches = checks.stream().filter(c -> !c.matches()).count();
        log.info("Checked '{}' on {} results across {} page(s): {} did not match",
                text, checks.size(), pageNumber, mismatches);

        if (pageNumber > 1) {
            navigate(startUrl);
            waitForResults();
        }
        return checks;
    }

    private boolean hasNextPage() {
        return nextPageLink.count() > 0 && !"true".equals(nextPageLink.getAttribute("aria-disabled"));
    }

    private void goToNextPage() {
        String pageBefore = getText(currentPageNumber);
        log.info("Going to results page after {}", pageBefore);
        scrollIntoView(nextPageLink);
        click(nextPageLink);
        assertThat(currentPageNumber).not().hasText(pageBefore);
        waitForResults();
    }

    @SuppressWarnings("unchecked")
    private List<ResultCheck> checkCurrentPage(String text, int pageNumber) {
        List<Map<String, Object>> cards = new ArrayList<>((List<Map<String, Object>>) resultCards.evaluateAll("""
                cards => cards
                    .filter(c => c.offsetParent !== null)
                    .map(c => ({
                        title: (c.querySelector('.s-card__title')?.innerText || '')
                            .replace('Opens in a new window or tab', '').replace(/\\s+/g, ' ').trim(),
                        attributes: Array.from(c.querySelectorAll('span.su-styled-text.secondary'))
                            .map(s => s.innerText.trim())
                    }))
                """));
        cards.removeIf(card -> PLACEHOLDER_TITLE.equalsIgnoreCase((String) card.get("title")));
        if (cards.isEmpty()) {
            throw new IllegalStateException("No visible result cards to verify '" + text + "' against");
        }

        List<Pattern> wordPatterns = Arrays.stream(text.trim().split("\\s+"))
                .map(word -> Pattern.compile("(?<![\\w-])" + Pattern.quote(word) + "(?![\\w-])",
                        Pattern.CASE_INSENSITIVE))
                .toList();
        List<ResultCheck> checks = new ArrayList<>();
        for (Map<String, Object> card : cards) {
            String title = (String) card.get("title");
            List<String> attributes = (List<String>) card.get("attributes");
            String cardText = title + "\n" + String.join("\n", attributes);
            boolean matches = wordPatterns.stream().allMatch(p -> p.matcher(cardText).find());
            ResultCheck check = new ResultCheck(pageNumber, title, String.join(" ", attributes), matches);
            if (!matches) {
                log.warn("'{}' not shown on result: {}", text, check);
            }
            checks.add(check);
        }
        log.info("Page {}: checked {} results for '{}'", pageNumber, checks.size(), text);
        return checks;
    }

    private static String escapeForJsRegex(String text) {
        return text.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }
}
