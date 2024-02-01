package br.com.aws.facialrecognition.cam;


import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;

import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_PLAIN;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*; 

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.opencv.core.Core;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.multipart.MultipartFile;

import br.com.aws.facialrecognition.service.FaceCamAuthenticationService;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_highgui;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;

@SpringBootApplication
@ComponentScan("br.com.aws.facialrecognition.service")
@ComponentScan("br.com.aws.facialrecognition.config")
public class Cam implements CommandLineRunner  {
	
     @Autowired
    private FaceCamAuthenticationService faceCamAuthenticationService;

	
    public static void main(String args[]) throws Exception {
           SpringApplication.run(Cam.class, args);

    }

    @Override
    public void run(String... args)   throws Exception {
        // Load OpenCV native libraries
        Loader.load(opencv_core.class);
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
        
        while ((frameCapturado = camera.grab()) != null) {
            //converte imagem da camera em jpg

            Thread.sleep(3000);
            imagemColorida = converteMat.convert(frameCapturado);
            Mat imagemCinza = new Mat();
            cvtColor(imagemColorida, imagemCinza, COLOR_BGRA2GRAY);
            RectVector facesDetectadas = new RectVector();
            detectorFace.detectMultiScale(imagemCinza, facesDetectadas, 1.1, 2, 0, new Size(100,100), new Size(500,500));
            

              


            for (int i = 0; i < facesDetectadas.size(); i++) {
                Rect dadosFace = facesDetectadas.get(i);
                rectangle(imagemColorida, dadosFace, new Scalar(0,255,0,0));
                Mat faceCapturada = new Mat(imagemCinza, dadosFace);
                
                if ((faceCapturada.size(0) == 160) || (faceCapturada.size(1) == 160)){
                    continue;
                }  
                resize(faceCapturada, faceCapturada, new Size(160,160));
                
                int x = Math.max(dadosFace.tl().x() - 10, 0);
                int y = Math.max(dadosFace.tl().y() - 10, 0);
                putText(imagemColorida, "pessoa", new Point(x, y), FONT_HERSHEY_PLAIN, 1.4, new Scalar(0,255,0,0));
                
                // atribui a imagem jpg a variavel imagem



                imwrite("src\\fotos\\pessoa." + "1" + ".jpg", faceCapturada);
                System.out.println("Foto " + "1" + " capturada\n");
                
                Thread.sleep(2000);

                // Pega a foto salvada
                BufferedImage img = ImageIO.read(new File("src\\fotos\\pessoa." + "1" + ".jpg"));
                MultipartFile multipartFile = carregarImagemComoMultipartFile("src\\fotos\\pessoa.1.jpg");
                
                Boolean isFace = faceCamAuthenticationService.isFace(multipartFile.getBytes());
                
                
            
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
        }
    }
    /*public static MultipartFile convertToMultipartFile(opencv_core.Mat image, String fileName) throws IOException {
        // Convert Mat to JPEG format
        opencv_core.MatOfByte matOfByte = new opencv_core.MatOfByte();
        opencv_imgcodecs.imencode(".jpg", image, matOfByte);

        // Convert MatOfByte to byte array
        BytePointer bytePointer = matOfByte.data();
        byte[] byteArray = new byte[(int) matOfByte.total()];
        bytePointer.get(byteArray);

        // Create MultipartFile
        InputStream inputStream = new ByteArrayInputStream(byteArray);
        return new MyMultipartFile(inputStream, byteArray.length, fileName);
    }

    private static class MyMultipartFile implements MultipartFile {

        private final InputStream inputStream;
        private final long size;
        private final String originalFilename;

        public MyMultipartFile(InputStream inputStream, long size, String originalFilename) {
            this.inputStream = inputStream;
            this.size = size;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "image/jpeg"; // Set appropriate content type
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public byte[] getBytes() throws IOException {
            // This method might be called by some libraries, so it's good to implement it.
            return new byte[0];
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
            // You can implement this method if needed
        }
    }*/
}
