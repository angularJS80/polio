package com.cho.polio.presentation.sample;

import com.cho.polio.presentation.enums.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SAMPLE)
public class SampleController {
    @GetMapping("/hello")
    public String home() {
        return "Hello, Spring Security!";
    }
}