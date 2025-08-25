package com.coin.demo.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UiController {

    @GetMapping("/")
    public String index(@RequestParam(name = "userId", required = false) Long userId, Model model) {
        model.addAttribute("userId", userId == null ? 1L : userId);
        return "index";
    }
}

