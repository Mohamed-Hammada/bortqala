package com.bemo.hr.shared.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SpaForwardController {

    @RequestMapping(value = {
            "/{path:^(?!api$|actuator$|assets$|icons$)[^\\.]*$}/**",
            "/{path:^(?!api$|actuator$|assets$|icons$)[^\\.]*$}"
    })
    public ModelAndView forward() {
        return new ModelAndView("forward:/index.html");
    }
}
