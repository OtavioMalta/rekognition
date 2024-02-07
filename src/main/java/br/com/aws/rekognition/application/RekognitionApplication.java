package br.com.aws.rekognition.application;


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

import br.com.aws.rekognition.dto.RekognitionResponse;
import br.com.aws.rekognition.service.RekognitionService;

@SpringBootApplication
@ComponentScan("br.com.aws.rekognition.service")
@ComponentScan("br.com.aws.rekognition.config")
public class RekognitionApplication implements CommandLineRunner  {
	
     @Autowired
    private RekognitionService rekognitionService;
	
    public static void main(String args[]) throws Exception {
         SpringApplication.run(RekognitionApplication.class, args);
    }

    @Override
    public void run(String... args)   throws Exception {
    	Loader.load(opencv_core.class);
        Loader.load(opencv_imgproc.class);
        Loader.load(opencv_highgui.class);
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);

        OpenCVFrameConverter.ToMat converteMat = new OpenCVFrameConverter.ToMat();
        OpenCVFrameGrabber camera = new OpenCVFrameGrabber(0);
        camera.start();
        CascadeClassifier detectorFace = new CascadeClassifier("src\\main\\resources\\static\\haarcascade_frontalface_default.xml");

        //Corrige o erro getDefaultScreenDevice
        System.setProperty("java.awt.headless", "false");
        
        CanvasFrame cFrame = new CanvasFrame("Rekognition", CanvasFrame.getDefaultGamma() / camera.getGamma());
        Frame frameCapturado = null;
        Mat imagemColorida = new Mat();

		long lastCaptureTime = System.currentTimeMillis();
		int captureInterval = 5000; // Intervalo (5 segundos)

        System.out.println("Digite seu id: ");
        Scanner cadastro = new Scanner(System.in);
        String idPessoa = cadastro.nextLine();

        while ((frameCapturado = camera.grab()) != null) {
            imagemColorida = converteMat.convert(frameCapturado);
            Mat imagemCinza = new Mat();
            cvtColor(imagemColorida, imagemCinza, COLOR_BGRA2GRAY);
            RectVector facesDetectadas = new RectVector();
            detectorFace.detectMultiScale(imagemCinza, facesDetectadas, 1.1, 2, 0, new Size(100,100), new Size(500,500));
            
            for (int i = 0; i < facesDetectadas.size(); i++) {
                Rect dadosFace = facesDetectadas.get(i);
                rectangle(imagemColorida, dadosFace, new Scalar(115,25,230,0),2,0,0);
                Mat faceCapturada = new Mat(imagemCinza, dadosFace);
                
                // Se a face capturada for menor que 160x160, pula para a próxima
                if ((faceCapturada.size(0) == 160) || (faceCapturada.size(1) == 160)){
                    continue;
                }  
                
                resize(faceCapturada, faceCapturada, new Size(160,160));
                
                int x = Math.max(dadosFace.tl().x() - 10, 0);
                int y = Math.max(dadosFace.tl().y() - 10, 0);
                
                putText(imagemColorida, "", new Point(x, y), FONT_HERSHEY_PLAIN, 1.4, new Scalar(255,0,0,0));
                
                if (System.currentTimeMillis() - lastCaptureTime >= captureInterval) {
                    resize(faceCapturada, faceCapturada, new Size(160,160));
                    imwrite("src\\fotos\\"+idPessoa +".jpg", faceCapturada);

                    // Pega a foto salvada
                    BufferedImage img = ImageIO.read(new File("src\\fotos\\"+idPessoa +".jpg"));
                    MultipartFile fotoCapturada = carregarImagemComoMultipartFile("src\\fotos\\"+idPessoa+".jpg");
                   
                    RekognitionResponse response = rekognitionService.comparar(fotoCapturada, idPessoa);

                    // apaga a foto
                    Files.deleteIfExists(Paths.get("src\\fotos\\"+idPessoa +".jpg"));
                    if (response == null) {
                        System.out.println("Rosto não encontrado");
                    } else if(response.getSimilaridade().doubleValue() > 90) {
                        System.out.println("Rosto reconhecido");
                        cFrame.dispose();
                        camera.stop();
                        System.exit(0);
                    } else {
                        System.out.println("Rosto não reconhecido");
                    }
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
            byte[] bytes = Files.readAllBytes(Paths.get(caminho));

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
            e.printStackTrace();
            return null;
        }
    }
}
