package org.example.config;

/**
 * BitCaskKey is a marker interface for keys used in BitCask.
 * It must be serializable and support equality and hash-based comparisons.
 */
public interface BitCaskKey extends Serializable {
    // In Java, all objects already support equals() and hashCode(), which are
    // used for comparison. No need for a separate 'comparable' constraint like in Go.
}
