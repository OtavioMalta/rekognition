package br.com.aws.facialrecognition;

import org.bytedeco.javacv.FrameGrabber;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import br.com.aws.facialrecognition.service.FaceAuthenticationService;
import br.com.aws.facialrecognition.service.FaceCamAuthenticationService;

@SpringBootApplication
public class Main {
	
	/*  private final FaceCamAuthenticationService faceAuthenticationService;

	   public Main(FaceCamAuthenticationService faceAuthenticationService) {
	        this.faceAuthenticationService = faceAuthenticationService;
	    }


        public static void main(String[] args) {
            // Inicie a aplicação Spring Boot
            ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

            // Obtenha a instância de FaceCamAuthenticationService do contexto Spring
            FaceCamAuthenticationService faceCamAuthenticationService = context.getBean(FaceCamAuthenticationService.class);

           try {
                // Chame o método main da FaceCamAuthenticationService
                faceCamAuthenticationService.openCam("ID_DA_FACE"); // substitua "ID_DA_FACE" pelo valor correto
            } catch (FrameGrabber.Exception e) {
                // Lide com exceções se necessário
                e.printStackTrace();
            } finally {
                // Feche o contexto Spring
                context.close();
            }
        }*/
}
