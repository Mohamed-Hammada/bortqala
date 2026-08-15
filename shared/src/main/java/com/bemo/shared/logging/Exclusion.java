package com.bemo.shared.logging;

/**
 * A rule that decides whether a piece of data is sensitive enough to be excluded from logs
 * and, if so, what safe representation can replace it.
 */
public interface Exclusion {

    boolean excluded(String data);

    String excludedSafely(String data);
}
