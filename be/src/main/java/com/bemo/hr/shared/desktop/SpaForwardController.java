package com.bemo.hr.shared.desktop;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({"/", "/login", "/dashboard", "/categories", "/employees", "/imports", "/parties",
            "/reports", "/reports/{id}", "/operations", "/settings", "/users"})
    String forwardAngularRoutes() {
        return "forward:/index.html";
    }
}
