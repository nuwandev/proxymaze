package com.binarybeasts.engine;

public interface ProxyProbeClient {
    ProbeOutcome probe(String url, int timeoutMs);
}

