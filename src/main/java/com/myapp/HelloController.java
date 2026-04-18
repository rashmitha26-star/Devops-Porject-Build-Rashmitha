package com.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HelloController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Welcome! Enter your name below.");
        return "index";
    }

    @PostMapping("/greet")
    public String greet(@RequestParam String name, Model model) {
        model.addAttribute("message", "Hello, " + name + "! App is running on Kubernetes.");
        return "index";
    }
}
