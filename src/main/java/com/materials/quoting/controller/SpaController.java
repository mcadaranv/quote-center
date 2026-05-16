package com.materials.quoting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards any unmatched, extension-less requests to the SPA index.html so that
 * Vue Router can handle client-side navigation (e.g. direct browser access to
 * /battery-quote-center/quotes or /battery-quote-center/quote-summary/1).
 *
 * Paths containing a dot (static assets, API responses) are intentionally
 * excluded by the regex so they continue to be handled normally.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {"/{path:[^.]*}", "/{path:[^.]*}/{id:[^.]*}"})
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}

