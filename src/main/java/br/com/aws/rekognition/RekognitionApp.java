package br.com.aws.rekognition;


import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import br.com.aws.rekognition.service.RekognitionService;

@SpringBootApplication
@ComponentScan("br.com.aws.rekognition.service")
@ComponentScan("br.com.aws.rekognition.config")
public class RekognitionApp implements CommandLineRunner  {
	
     @Autowired
    private RekognitionService rekognitionService;
	
    public static void main(String args[]) throws Exception {
         SpringApplication.run(RekognitionApp.class, args);
    }

    @Override
    public void run(String... args)   throws Exception {
    	Loader.load(opencv_core.class);
        Loader.load(opencv_imgproc.class);
        Loader.load(opencv_highgui.class);
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);

                   
        }
}
