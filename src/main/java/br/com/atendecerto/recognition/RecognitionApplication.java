package br.com.atendecerto.recognition;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("br.com.atendecerto.recognition.service")
@ComponentScan("br.com.atendecerto.recognition.config")
public class RecognitionApplication implements CommandLineRunner  {
	
    public static void main(String args[]) throws Exception {
         SpringApplication.run(RecognitionApplication.class, args);
    }

    @Override
    public void run(String... args)   throws Exception {
    	Loader.load(opencv_core.class);
        Loader.load(opencv_imgproc.class);
        Loader.load(opencv_highgui.class);
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);

                   
        }
}
