package com.ebay.base;

import com.ebay.config.BrowserFactory;
import com.ebay.models.TestData;
import com.ebay.models.TestData.BrowserConfig;
import com.ebay.reporting.AllureLogAppender;
import com.ebay.utils.JsonDataReader;
import com.ebay.utils.ScreenRecorderUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Video;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public abstract class BaseTest {

    private static final String TEST_DATA_FILE = "testdata/testData.json";
    private static final long RECORDING_TAIL_MS = 2000;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected TestData data;
    protected Page page;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    @BeforeClass(alwaysRun = true)
    public void launchBrowser() {
        data = JsonDataReader.read(TEST_DATA_FILE, TestData.class);
        writeAllureMetadata();
        if (data.reporting().screenRecording()) {
            ScreenRecorderUtil.start(data.reporting().screenRecordingDir(), getClass().getSimpleName());
        }
        playwright = Playwright.create();
        browser = BrowserFactory.launch(playwright, data.browser());
    }

    protected void captureStep(String name) {
        if (data.reporting().stepScreenshots()) {
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(page.screenshot()), "png");
        }
    }

    protected void verify(String description, Runnable assertion) {
        assertion.run();
        log.info("Verified: {}", description);
    }

    @BeforeMethod(alwaysRun = true)
    public void openPage() {
        AllureLogAppender.drainTestLog();
        BrowserConfig cfg = data.browser();
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(cfg.viewportWidth(), cfg.viewportHeight());
        if (cfg.recordVideo()) {
            options.setRecordVideoDir(Paths.get(cfg.videoDir()))
                    .setRecordVideoSize(cfg.viewportWidth(), cfg.viewportHeight());
        }
        context = browser.newContext(options);
        context.setDefaultTimeout(cfg.defaultTimeoutMs());
        PlaywrightAssertions.setDefaultAssertionTimeout(cfg.defaultTimeoutMs());
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void closePage(ITestResult result) {
        if (page == null) {
            return;
        }
        if (!result.isSuccess()) {
            try {
                Allure.addAttachment("Failure screenshot", "image/png",
                        new ByteArrayInputStream(page.screenshot()), "png");
            } catch (Exception ignored) {
            }
        }
        Video video = page.video();
        context.close();
        if (video != null) {
            attachVideo(video.path());
        }
        Allure.addAttachment("Execution log", "text/plain", AllureLogAppender.drainTestLog(), ".log");
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (data != null && data.reporting().screenRecording()) {
            pause(RECORDING_TAIL_MS);
            ScreenRecorderUtil.stop();
        }
    }

    private void writeAllureMetadata() {
        Path resultsDir = Paths.get(System.getProperty("allure.results.directory", "allure-results"));
        Properties env = new Properties();
        env.setProperty("Browser", data.browser().name());
        env.setProperty("Headless", String.valueOf(data.browser().headless()));
        env.setProperty("Base URL", data.home().url());
        env.setProperty("Ship to", data.shipTo().country());
        env.setProperty("OS", System.getProperty("os.name"));
        env.setProperty("Java", System.getProperty("java.version"));
        try {
            Files.createDirectories(resultsDir);
            try (var out = Files.newOutputStream(resultsDir.resolve("environment.properties"))) {
                env.store(out, "Allure environment");
            }
            try (InputStream categories = getClass().getClassLoader().getResourceAsStream("allure/categories.json")) {
                if (categories != null) {
                    Files.copy(categories, resultsDir.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void attachVideo(Path path) {
        try {
            Allure.addAttachment("Execution video", "video/webm", Files.newInputStream(path), "webm");
        } catch (Exception ignored) {
        }
    }
}
