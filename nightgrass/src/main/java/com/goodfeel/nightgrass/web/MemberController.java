package com.goodfeel.nightgrass.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemberController {

    @GetMapping("/member")
    public String member(Model model) {
        return "member";
    }
}
