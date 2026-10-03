package com.ebay.reporting;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AllureLogAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
    private static final ThreadLocal<Boolean> APPENDING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<StringBuilder> TEST_LOG = ThreadLocal.withInitial(StringBuilder::new);

    @Override
    protected void append(ILoggingEvent event) {
        if (APPENDING.get() || event.getLoggerName().startsWith("io.qameta")) {
            return;
        }
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isEmpty()) {
            return;
        }
        APPENDING.set(true);
        try {
            String message = event.getFormattedMessage();
            TEST_LOG.get().append(TIME.format(Instant.ofEpochMilli(event.getTimeStamp())))
                    .append(' ').append(String.format("%-5s", event.getLevel()))
                    .append(' ').append(shortName(event.getLoggerName()))
                    .append(" - ").append(message).append('\n');

            Level level = event.getLevel();
            if (level.isGreaterOrEqual(Level.ERROR)) {
                Allure.step("ERROR: " + message, Status.FAILED);
            } else if (level.isGreaterOrEqual(Level.WARN)) {
                Allure.step("WARN: " + message, Status.BROKEN);
            } else {
                Allure.step(message);
            }
        } finally {
            APPENDING.set(false);
        }
    }

    public static String drainTestLog() {
        String log = TEST_LOG.get().toString();
        TEST_LOG.remove();
        return log;
    }

    private static String shortName(String loggerName) {
        return loggerName.substring(loggerName.lastIndexOf('.') + 1);
    }
}
