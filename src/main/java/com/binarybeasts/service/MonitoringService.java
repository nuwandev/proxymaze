package com.binarybeasts.service;

import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.domain.RuntimeConfig;
import com.binarybeasts.engine.MonitoringEngine;
import com.binarybeasts.store.InMemoryStateStore;
import com.binarybeasts.util.ProxyIdExtractor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class MonitoringService {

    private final InMemoryStateStore store;
    private final MonitoringEngine engine;
    private final RuntimeConfig config;

    public MonitoringService(InMemoryStateStore store, MonitoringEngine engine,
                             RuntimeConfig config) {
        this.store = store;
        this.engine = engine;
        this.config = config;
    }

    public RuntimeConfig getConfig() {
        return config;
    }

    public void updateConfig(int intervalSeconds, int timeoutMs) {
        config.setCheckIntervalSeconds(intervalSeconds);
        config.setRequestTimeoutMs(timeoutMs);
        engine.reschedule();
    }

    public List<ProxyNode> addProxies(List<String> urls, boolean replace) {
        if (replace) store.clearPool();

        List<ProxyNode> added = new ArrayList<>();
        for (String url : urls) {
            String id = ProxyIdExtractor.extract(url);
            if (!store.findProxy(id).isPresent()) {
                ProxyNode node = new ProxyNode(id, url);
                store.addProxy(node);
                added.add(node);
            } else {
                added.add(store.findProxy(id).get());
            }
        }
        return added;
    }

    public Collection<ProxyNode> getAllProxies() {
        return store.getAllProxies();
    }

    public Optional<ProxyNode> getProxy(String id) {
        return store.findProxy(id);
    }

    public void clearPool() {
        store.clearPool();
    }

    public double calculateFailureRate() {
        Collection<ProxyNode> proxies = store.getAllProxies();
        long checked = proxies.stream()
                .filter(p -> p.getStatus() != ProxyStatus.PENDING)
                .count();
        if (checked == 0) return 0.0;
        long down = proxies.stream()
                .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                .count();
        return (double) down / checked;
    }
}