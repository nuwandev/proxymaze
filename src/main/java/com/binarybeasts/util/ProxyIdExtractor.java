package com.binarybeasts.util;

import java.net.URI;

public class ProxyIdExtractor {
    public static String extract(String url) {
        try {
            String path = new URI(url).getPath();
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("URL path is empty");
            }

            String[] parts = path.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isBlank()) {
                    return parts[i];
                }
            }
            throw new IllegalArgumentException("URL path does not contain an ID");
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract ID from URL: " + url);
        }
    }
}