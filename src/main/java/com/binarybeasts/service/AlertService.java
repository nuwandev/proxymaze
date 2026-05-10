package com.binarybeasts.service;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.ProxyNode;

import java.util.Collection;
import java.util.List;

public interface AlertService {

    void evaluate(Collection<ProxyNode> proxies);

    List<Alert> getAllAlerts();

    Alert getActiveAlert();

    List<String> getFrozenFailedIds(Alert alert);

    List<String> getCurrentFailedIds();

    boolean resolveActiveAlert(String reason);
}
