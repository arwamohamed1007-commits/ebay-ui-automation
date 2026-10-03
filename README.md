# eBay UI Automation (Playwright Java)

UI test automation for eBay built with **Playwright for Java**, **TestNG**, the **Page Object Model**, and **Allure** reporting.
All test data lives in an external JSON file — nothing is hardcoded in the tests.

## Scenario
1. Open https://www.ebay.com/ and verify the home page loaded (eBay logo, `eBay Home` title, search box)
2. Set the **Ship to** location to *United States* (car filters such as *Transmission* are only offered for US locations)
3. Search for **mazda mx-5** and validate the results
4. Print/log the number of results
5. Filter with **Transmission → Manual** from the left-hand panel
6. Verify the number of results changed (and narrowed)

## Tech stack
| Purpose        | Tool                         |
|----------------|------------------------------|
| Browser driver | Playwright for Java          |
| Test runner    | TestNG                       |
| Test data      | JSON + Jackson               |
| Reporting      | Allure (+ screenshots/video) |
| Screen video   | Monte Screen Recorder        |
| Build          | Maven                        |

## Project structure
```
src
├── main/java/com/ebay
│   ├── config/BrowserFactory.java      # Launches msedge / chrome / chromium / firefox / webkit
│   ├── models/TestData.java            # Typed model of testData.json
│   ├── pages/BasePage.java             # Common browser actions (click, type, waits, scroll...)
│   ├── pages/HomePage.java             # Home page: logo, ship-to location, search
│   ├── pages/SearchResultsPage.java    # Results count, result cards, left-hand filters
│   ├── utils/JsonDataReader.java       # Reads JSON test data from the classpath
│   └── utils/ScreenRecorderUtil.java   # Records the screen from browser launch to closure
└── test
    ├── java/com/ebay/base/BaseTest.java          # Playwright lifecycle, screenshots, video
    ├── java/com/ebay/tests/SearchAndFilterTest.java
    └── resources
        ├── testdata/testData.json      # <- all test data & browser config
        └── testng.xml
```

## Prerequisites
- JDK 17+
- Maven 3.9+
- Microsoft Edge (default browser in `testData.json`; change `browser.name` to use another one)

Playwright downloads its own driver automatically on the first run.

## Run
```bash
mvn clean test
```

## Execution report
```bash
mvn clean test allure:report
```
Generates a single self-contained file, `target/site/allure-maven-plugin/index.html`, that opens with a double-click (no server needed). It contains:
- pass/fail status and duration of each step
- a screenshot after every step (home page, ship-to set, search results, filtered results)
- the result count before and after filtering
- the browser video of the run, and a screenshot on failure
- environment info (browser, OS, Java, base URL, ship-to country)

`mvn allure:serve` opens the same report in the browser directly.

## Video proof
Every run records the **whole screen** from **before the browser launches until after it closes**, saved as
`recordings/SearchAndFilterTest_<date>_<time>.avi` (Motion-JPEG, plays in VLC / Windows Media Player).
Turn it on/off in `testData.json` → `reporting.screenRecording`.

Playwright's page-only video is also saved to `target/videos/` and attached to the report.

## Changing test data
Edit `src/test/resources/testdata/testData.json`:
```json
{
  "browser":  { "name": "msedge", "headless": false, "slowMoMs": 300, ... },
  "reporting": { "screenRecording": true, "screenRecordingDir": "recordings", "stepScreenshots": true },
  "shipTo":   { "country": "United States", "postalCode": "10001" },
  "home":     { "url": "https://www.ebay.com/", "expectedLogoTitle": "eBay Home" },
  "search":   { "searchTerm": "mazda mx-5", "filterGroup": "Transmission", "filterOption": "Manual" }
}
```

## Notes
- eBay sometimes opens its own *"Are you shipping to …?"* prompt. It holds the same country/zip form as the Ship-to dialog, so `HomePage` fills whichever one is open.
- eBay may block traffic it considers automated if the suite is run many times in a row. If you see a *"SORRY – Something went wrong"* page, wait a few minutes and run again.
