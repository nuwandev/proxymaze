package com.binarybeasts.service;

import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.RuntimeConfig;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MonitoringService {

    RuntimeConfig getConfig();

    void updateConfig(int intervalSeconds, int timeoutMs);

    List<ProxyNode> addProxies(List<String> urls, boolean replace);

    Collection<ProxyNode> getAllProxies();

    Optional<ProxyNode> getProxy(String id);

    void clearPool();

    double calculateFailureRate();
}
