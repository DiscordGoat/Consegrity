/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.adapters;

import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.LogLevel;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.adapters.LogAdapter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDKLogAdapter
implements LogAdapter {
    private final Logger logger;

    public JDKLogAdapter(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void log(LogLevel level, String message) {
        switch (Objects.requireNonNull(level, "level")) {
            case DEBUG: {
                this.logger.log(Level.FINE, message);
                break;
            }
            case INFO: {
                this.logger.log(Level.INFO, message);
                break;
            }
            case WARN: {
                this.logger.log(Level.WARNING, message);
                break;
            }
            case ERROR: {
                this.logger.log(Level.SEVERE, message);
            }
        }
    }

    @Override
    public void log(LogLevel level, String message, Throwable throwable) {
        switch (Objects.requireNonNull(level, "level")) {
            case DEBUG: {
                this.logger.log(Level.FINE, message, throwable);
                break;
            }
            case INFO: {
                this.logger.log(Level.INFO, message, throwable);
                break;
            }
            case WARN: {
                this.logger.log(Level.WARNING, message, throwable);
                break;
            }
            case ERROR: {
                this.logger.log(Level.SEVERE, message, throwable);
            }
        }
    }
}

