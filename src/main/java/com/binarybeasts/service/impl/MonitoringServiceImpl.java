package com.binarybeasts.service.impl;

import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.domain.RuntimeConfig;
import com.binarybeasts.engine.MonitoringEngine;
import com.binarybeasts.service.AlertService;
import com.binarybeasts.service.MonitoringService;
import com.binarybeasts.store.InMemoryStateStore;
import com.binarybeasts.util.ProxyIdExtractor;
import com.binarybeasts.util.LogHighlighter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class MonitoringServiceImpl implements MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    private final InMemoryStateStore store;
    private final MonitoringEngine engine;
    private final RuntimeConfig config;
    private final AlertService alertService;

    public MonitoringServiceImpl(InMemoryStateStore store, MonitoringEngine engine,
                                 RuntimeConfig config, AlertService alertService) {
        this.store = store;
        this.engine = engine;
        this.config = config;
        this.alertService = alertService;
    }

    @Override
    public RuntimeConfig getConfig() {
        return config;
    }

    @Override
    public void updateConfig(int intervalSeconds, int timeoutMs) {
        config.setCheckIntervalSeconds(intervalSeconds);
        config.setRequestTimeoutMs(timeoutMs);
        engine.reschedule();
        LogHighlighter.info(log, "Config", "Updated — interval={}s, timeout={}ms", intervalSeconds, timeoutMs);
    }

    @Override
    public List<ProxyNode> addProxies(List<String> urls, boolean replace) {
        if (replace) {
            store.clearPool();
            boolean resolved = alertService.resolveActiveAlert("pool replaced");
            LogHighlighter.info(log, "Pool", "Replaced — cleared existing proxies; active alert resolved={}", resolved);
        }

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

        LogHighlighter.info(log, "Pool", "Added {} proxies — pool size now {}", added.size(), store.getPool().size());
        engine.reschedule();
        return added;
    }

    @Override
    public Collection<ProxyNode> getAllProxies() {
        return store.getAllProxies();
    }

    @Override
    public Optional<ProxyNode> getProxy(String id) {
        return store.findProxy(id);
    }

    @Override
    public void clearPool() {
        int size = store.getPool().size();
        store.clearPool();
        boolean resolved = alertService.resolveActiveAlert("pool cleared");
        LogHighlighter.info(log, "Pool", "Cleared {} proxies — alert history preserved; active alert resolved={}", size, resolved);
    }

    @Override
    public double calculateFailureRate() {
        Collection<ProxyNode> all = store.getAllProxies();
        if (all.isEmpty()) return 0.0;
        long down = all.stream()
                .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                .count();
        return (double) down / all.size();
    }
}