package br.com.aws.rekognition.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VideoElementController {

    @GetMapping("/videoElement")
    public String videoElement() {
        return "VideoElement";
    }
}
