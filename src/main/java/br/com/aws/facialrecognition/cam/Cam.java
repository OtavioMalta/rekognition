package br.com.aws.facialrecognition.cam;


import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.multipart.MultipartFile;

import br.com.aws.facialrecognition.dto.FaceAuthenticationResponse;
import br.com.aws.facialrecognition.service.FaceCamAuthenticationService;

@SpringBootApplication
@ComponentScan("br.com.aws.facialrecognition.service")
@ComponentScan("br.com.aws.facialrecognition.config")
public class Cam implements CommandLineRunner  {
	
     @Autowired
    private FaceCamAuthenticationService faceCamAuthenticationService;

	
    public static void main(String args[]) throws Exception {
         // SpringApplication.run(Cam.class, args);

    }

    @Override
    public void run(String... args)   throws Exception {
    	/*  Loader.load(opencv_core.class);
        Loader.load(opencv_imgproc.class);
        Loader.load(opencv_highgui.class);
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);

       OpenCVFrameConverter.ToMat converteMat = new OpenCVFrameConverter.ToMat();
        OpenCVFrameGrabber camera = new OpenCVFrameGrabber(0);
        String[] pessoas = {"", "Jones", "Gabriel"};
        camera.start();
        CascadeClassifier detectorFace = new CascadeClassifier("src\\main\\resources\\haarcascade_frontalface_alt.xml");

        //corrige o erro getDefaultScreenDevice
        System.setProperty("java.awt.headless", "false");
        
        CanvasFrame cFrame = new CanvasFrame("Reconhecimento.CAM", CanvasFrame.getDefaultGamma() / camera.getGamma());
        Frame frameCapturado = null;
        Mat imagemColorida = new Mat();

		long lastCaptureTime = System.currentTimeMillis();
		int captureInterval = 5000; // Intervalo (5 segundos)

        System.out.println("Digite seu id: ");
        Scanner cadastro = new Scanner(System.in);
        String idPessoa = cadastro.nextLine();

        MultipartFile fotoOriginal = faceCamAuthenticationService.downloadImage(idPessoa);
        while ((frameCapturado = camera.grab()) != null) {
            //converte imagem da camera em jpg

            imagemColorida = converteMat.convert(frameCapturado);
            Mat imagemCinza = new Mat();
            cvtColor(imagemColorida, imagemCinza, COLOR_BGRA2GRAY);
            RectVector facesDetectadas = new RectVector();
            detectorFace.detectMultiScale(imagemCinza, facesDetectadas, 1.1, 2, 0, new Size(100,100), new Size(500,500));
            
            for (int i = 0; i < facesDetectadas.size(); i++) {
                Rect dadosFace = facesDetectadas.get(i);
                rectangle(imagemColorida, dadosFace, new Scalar(115,25,230,0),2,0,0);
                Mat faceCapturada = new Mat(imagemCinza, dadosFace);
                
                if ((faceCapturada.size(0) == 160) || (faceCapturada.size(1) == 160)){
                    continue;
                }  
                resize(faceCapturada, faceCapturada, new Size(160,160));
                
                int x = Math.max(dadosFace.tl().x() - 10, 0);
                int y = Math.max(dadosFace.tl().y() - 10, 0);
                
                //puttext em vermelho
                putText(imagemColorida, "", new Point(x, y), FONT_HERSHEY_PLAIN, 1.4, new Scalar(255,0,0,0));
                // aumenta a espessura da linha
                
                if (System.currentTimeMillis() - lastCaptureTime >= captureInterval) {
                    
                    resize(faceCapturada, faceCapturada, new Size(160,160));

                    imwrite("src\\fotos\\"+idPessoa +".jpg", faceCapturada);

                    // Pega a foto salvada
                    BufferedImage img = ImageIO.read(new File("src\\fotos\\"+idPessoa +".jpg"));
                    MultipartFile fotoCapturada = carregarImagemComoMultipartFile("src\\fotos\\"+idPessoa+".jpg");
                    
                    //if(faceCamAuthenticationService.isFace(multipartFile.getBytes())) {
                        FaceAuthenticationResponse response = faceCamAuthenticationService.compareFaces(fotoCapturada, fotoOriginal);
                        // apaga a foto
                        Files.deleteIfExists(Paths.get("src\\fotos\\"+idPessoa +".jpg"));
                        if (response == null) {
                        	System.out.println("Rosto não encontrado");
                        } else if(response.getSimilarityPercentage().doubleValue() > 90) {
                            System.out.println("Rosto reconhecido");
                            // encerra a camera
                            cFrame.dispose();
                            camera.stop();
                            // encerra a aplicação
                            System.exit(0);
                            
                        } else {
                            System.out.println("Rosto não reconhecido");
                        }
                    
                    
                    System.out.println("Foto src\\fotos\\"+idPessoa +".jpg capturada\n");
                    lastCaptureTime = System.currentTimeMillis();
                }
                
                
                
            
            }
            if (cFrame.isVisible()) {
                cFrame.showImage(frameCapturado);
            }
        }
        cFrame.dispose();
        camera.stop();
    }
      private static MultipartFile carregarImagemComoMultipartFile(String caminho) {
        try {
            // Lê o conteúdo do arquivo como bytes
            byte[] bytes = Files.readAllBytes(Paths.get(caminho));

            // Cria um MultipartFile a partir dos bytes
            return new MultipartFile() {
                @Override
                public String getName() {
                    return null;
                }

                @Override
                public String getOriginalFilename() {
                    return null;
                }

                @Override
                public String getContentType() {
                    return null;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public long getSize() {
                    return 0;
                }

                @Override
                public byte[] getBytes() throws IOException {
                    return bytes;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    Files.write(dest.toPath(), bytes);
                }
            };
        } catch (IOException e) {
            // Lida com possíveis erros de IO
            e.printStackTrace();
            return null;
        }*/
    }
}
