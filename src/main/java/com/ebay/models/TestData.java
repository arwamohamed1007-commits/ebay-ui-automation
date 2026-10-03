package com.ebay.models;

import java.util.List;

public record TestData(BrowserConfig browser, ReportingConfig reporting, ShipToData shipTo, HomeData home, SearchData search) {

    public record BrowserConfig(
            String name,
            List<String> args,
            boolean headless,
            int slowMoMs,
            int defaultTimeoutMs,
            int viewportWidth,
            int viewportHeight,
            boolean recordVideo,
            String videoDir) {
    }

    public record ReportingConfig(boolean screenRecording, String screenRecordingDir, boolean stepScreenshots) {
    }

    public record ShipToData(String country, String postalCode) {
    }

    public record HomeData(String url, String expectedLogoTitle) {
    }

    public record SearchData(String searchTerm, String filterGroup, String filterOption) {
    }
}
