/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
 */
package org.opengrok.indexer.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.opengrok.indexer.Metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is handy for logging messages (and updating metrics)
 * about duration of a task.
 */
public class Statistics {

    private final Instant startTime;

    public Statistics() {
      startTime = Instant.now();
  }

    /**
     * Log a message with duration using provided logger if the supplied log level is active.
     * @param logger logger instance
     * @param logLevel log level
     * @param msg message string
     * @param duration duration to report
     * @return whether logging was performed
     */
    private boolean logIt(Logger logger, Level logLevel, String msg, Duration duration) {
        if (logger.isLoggable(logLevel)) {
            String timeStr = StringUtils.getReadableTime(duration.toMillis());
            logger.log(logLevel, String.format("%s (took %s)", msg, timeStr));
            return true;
        }

        return false;
    }

    /**
     * Log a message along with how much time it took since the constructor was called.
     * @param logger logger instance
     * @param logLevel log level
     * @param msg message string
     * @return whether logging was performed
     */
    public boolean report(Logger logger, Level logLevel, String msg) {
        return logIt(logger, logLevel, msg, Duration.between(startTime, Instant.now()));
    }

    /**
     * Log a message along with how much time it took since the constructor was called.
     * If there is a metrics registry, it will update the timer specified by the meter name.
     * @param logger logger instance
     * @param logLevel log level
     * @param msg message string
     * @param meterName name of the meter
     * @see Metrics#getRegistry()
     * @return whether logging was performed
     */
    public boolean report(Logger logger, Level logLevel, String msg, String meterName) {
        return report(logger, logLevel, msg, meterName, new String[]{});
    }

    /**
     * Log a message along with how much time it took since the constructor was called.
     * If there is a metrics registry, it will update the timer specified by the meter name.
     * @param logger logger instance
     * @param logLevel log level
     * @param msg message string
     * @param meterName name of the meter
     * @param tags array of tags for the meter
     * @see Metrics#getRegistry()
     * @return whether logging was performed
     */
    public boolean report(Logger logger, Level logLevel, String msg, String meterName, String[] tags) {
        Duration duration = Duration.between(startTime, Instant.now());

        boolean ret = logIt(logger, logLevel, msg, duration);

        MeterRegistry registry = Metrics.getRegistry();
        if (registry != null) {
            Timer.builder(meterName).
                    tags(tags).
                    register(registry).
                    record(duration);
        }

        return ret;
    }

    /**
     * Log a message along with how much time it took since the constructor was called.
     * If there is a metrics registry, it will update the timer specified by the meter name.
     * The log level is {@code INFO}.
     * @param logger logger instance
     * @param msg message string
     * @param meterName name of the meter
     * @return whether logging was performed
     */
    public boolean report(Logger logger, String msg, String meterName) {
        return report(logger, Level.INFO, msg, meterName);
    }

    /**
     * log a message along with how much time it took since the constructor was called.
     * The log level is {@code INFO}.
     * @param logger logger instance
     * @param msg message string
     * @return whether logging was performed
     */
    public boolean report(Logger logger, String msg) {
        return report(logger, Level.INFO, msg);
    }
}
