package com.ebay.utils;

import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.KeyFrameIntervalKey;
import static org.monte.media.FormatKeys.MIME_AVI;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.FormatKeys.MimeTypeKey;
import static org.monte.media.VideoFormatKeys.CompressorNameKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;
import static org.monte.media.VideoFormatKeys.QualityKey;

public final class ScreenRecorderUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenRecorderUtil.class);
    private static final int FRAME_RATE = 10;

    private static ScreenRecorder recorder;

    private ScreenRecorderUtil() {
    }

    public static void start(String outputDir, String name) {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("No display available - screen recording skipped");
            return;
        }
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            File dir = new File(outputDir);
            dir.mkdirs();
            String fileName = name + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            recorder = new NamedScreenRecorder(gc, dir, fileName,
                    new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI),
                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_MJPG,
                            CompressorNameKey, ENCODING_AVI_MJPG, DepthKey, 24,
                            FrameRateKey, Rational.valueOf(FRAME_RATE), QualityKey, 0.5f,
                            KeyFrameIntervalKey, FRAME_RATE * 60),
                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "black",
                            FrameRateKey, Rational.valueOf(30)));
            recorder.start();
            log.info("Screen recording started");
        } catch (IOException | AWTException e) {
            log.warn("Could not start screen recording: {}", e.getMessage());
            recorder = null;
        }
    }

    public static void stop() {
        if (recorder == null) {
            return;
        }
        try {
            recorder.stop();
            File video = recorder.getCreatedMovieFiles().isEmpty() ? null : recorder.getCreatedMovieFiles().get(0);
            log.info("Screen recording saved to {}", video == null ? "<none>" : video.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not stop screen recording: {}", e.getMessage());
        } finally {
            recorder = null;
        }
    }

    private static final class NamedScreenRecorder extends ScreenRecorder {
        private final String fileName;

        NamedScreenRecorder(GraphicsConfiguration gc, File dir, String fileName,
                            Format fileFormat, Format screenFormat, Format mouseFormat)
                throws IOException, AWTException {
            super(gc, gc.getBounds(), fileFormat, screenFormat, mouseFormat, null, dir);
            this.fileName = fileName;
        }

        @Override
        protected File createMovieFile(Format fileFormat) throws IOException {
            movieFolder.mkdirs();
            return new File(movieFolder, fileName + ".avi");
        }
    }
}
