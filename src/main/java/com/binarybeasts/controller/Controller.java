package com.binarybeasts.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @PostMapping("/config")
    public void config() {

    }

    @PostMapping("/proxies")
    public void proxies() {

    }

    @GetMapping("/proxies")
    public void getProxies() {

    }

    @GetMapping("/alerts")
    public void alerts() {

    }

    @PostMapping("/webhooks")
    public void webhooks() {

    }

    @PostMapping("/integrations")
    public void integrations() {

    }

}


