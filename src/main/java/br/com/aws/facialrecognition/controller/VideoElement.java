package br.com.aws.facialrecognition.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VideoElement {

    @GetMapping("/videoElement")
    public String home() {
    	System.out.println("OLA MUNDO");
        return "VideoElement"; // Isso corresponderá ao nome do arquivo HTML, sem a extensão
    }
}
