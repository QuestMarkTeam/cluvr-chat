package com.example.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DebugHeaderController {
    @GetMapping("/debug/headers")
    public void debugHeaders(@RequestHeader Map<String, String> headers) {
        headers.forEach((k, v) -> System.out.println(k + ": " + v));
    }
} 