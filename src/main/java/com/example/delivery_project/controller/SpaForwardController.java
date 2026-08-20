package com.example.delivery_project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/plans",
            "/plans/new",
            "/plans/{planId:\\d+}"
    })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
