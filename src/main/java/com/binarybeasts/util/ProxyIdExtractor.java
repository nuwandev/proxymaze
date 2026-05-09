package com.binarybeasts.util;

import java.net.URI;

public class ProxyIdExtractor {
    public static String extract(String url) {
        try {
            String path = new URI(url).getPath();
            String[] parts = path.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract ID from URL: " + url);
        }
    }
}