package br.com.aws.facialrecognition.service;

import br.com.aws.facialrecognition.dto.FaceAuthenticationResponse;
import br.com.aws.facialrecognition.handler.FacesNotMatchException;
import br.com.aws.facialrecognition.handler.IdNotFoundException;
import br.com.aws.facialrecognition.handler.MultipleFacesException;
import br.com.aws.facialrecognition.handler.NotHumanFaceException;
import br.com.aws.facialrecognition.handler.NotRegisteredFaceException;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.ResourceAlreadyExistsException;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.s3.AmazonS3;


import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



@Slf4j
@Service
public class FaceCamAuthenticationService {

    private final AmazonS3 s3client;
    private final AmazonRekognition rekognitionClient;
    private static final float SIMILARITY_PERCENTAGE = 50L;
    private static final String COLLECTION_ID = "users-photos";
    private static final String BUCKET_NAME = System.getenv("BUCKET_NAME");

    @Autowired
    public FaceCamAuthenticationService(AmazonS3 s3client,
                                     AmazonRekognition rekognitionClient) {
        this.s3client = s3client;
        this.rekognitionClient = rekognitionClient;
    }

    public void saveNewPhoto(MultipartFile photo) throws Exception {
        if (!isFace(photo.getBytes())) {
            throw new NotHumanFaceException();
        }

        uploadToBucket(photo, false);

        try {
            rekognitionClient.createCollection(new CreateCollectionRequest()
                    .withCollectionId(COLLECTION_ID));
        } catch (ResourceAlreadyExistsException e) {
            log.warn("Collection já existente");
        } catch (Exception e) {
            log.error("Erro ao criar collection");
            throw e;
        }

        rekognitionClient.indexFaces(new IndexFacesRequest()
                .withImage(new Image()
                        .withS3Object(new com.amazonaws.services.rekognition.model.S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(photo.getOriginalFilename())))
                .withCollectionId(COLLECTION_ID)
                .withExternalImageId(photo.getOriginalFilename())
                .withDetectionAttributes("ALL"));
    }

    public FaceAuthenticationResponse searchFace(MultipartFile photo) throws Exception {
        if (!isFace(photo.getBytes())) {
            throw new NotHumanFaceException();
        }

        uploadToBucket(photo, true);
        
        SearchFacesByImageResult result = rekognitionClient.searchFacesByImage(new SearchFacesByImageRequest()
                .withCollectionId(COLLECTION_ID)
                .withImage(new Image()
                        .withS3Object(new com.amazonaws.services.rekognition.model.S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(photo.getOriginalFilename() + "temp")))
                .withFaceMatchThreshold(SIMILARITY_PERCENTAGE));

        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, photo.getOriginalFilename() + "temp"));

        if (result.getFaceMatches().isEmpty()) {
            throw new NotRegisteredFaceException();
        }	

        FaceMatch matchedFace = result.getFaceMatches().stream()
                .max(Comparator.comparing(f -> f.getFace().getConfidence()))
                .orElseThrow(Exception::new);

        return FaceAuthenticationResponse.builder()
                .userName(matchedFace.getFace().getExternalImageId())
                .confidencePercentage(BigDecimal.valueOf(matchedFace.getFace().getConfidence()))
                .similarityPercentage(BigDecimal.valueOf(matchedFace.getSimilarity()))
                .build();
    }
    
    public boolean doesImageExist(String id) {
        return s3client.doesObjectExist(BUCKET_NAME, id + ".jpg");
    }

    // fazer download da imagem do s3
    public void downloadImage(String id) {
        s3client.getObject(BUCKET_NAME, id + ".jpg");
    }

    //comparar duas imagens recebidas por parametro
    
    
    public FaceAuthenticationResponse compareFaces(MultipartFile photo, String id) throws Exception {
        if (!isFace(photo.getBytes())) {
            throw new NotHumanFaceException();
        }
        
        if(!doesImageExist(id)) {
            throw new IdNotFoundException();
        }

        uploadToBucket(photo, true);

        

        CompareFacesResult result = rekognitionClient.compareFaces(new CompareFacesRequest()
                .withSourceImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(id+ ".jpg")))
                .withTargetImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(photo.getOriginalFilename() + "temp")))
                .withSimilarityThreshold(SIMILARITY_PERCENTAGE));

        List<CompareFacesMatch> faceMatches = result.getFaceMatches();
        for (CompareFacesMatch match : faceMatches) {
            System.out.println("Face similarity: " + match.getSimilarity());
        }

        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, photo.getOriginalFilename() + "temp"));

        if (result.getFaceMatches().isEmpty()) {
            throw new FacesNotMatchException();
        }
        
        CompareFacesMatch matchedFace = result.getFaceMatches().stream()
                .max(Comparator.comparingDouble(f -> f.getSimilarity()))
                .orElseThrow(() -> new RuntimeException("Nenhuma correspondência encontrada"));

        return FaceAuthenticationResponse.builder()
                .userName(id+".jpg")
                .confidencePercentage(BigDecimal.valueOf(matchedFace.getFace().getConfidence()))
                .similarityPercentage(BigDecimal.valueOf(matchedFace.getSimilarity()))
                .build();
    }

    public Boolean isFace(byte[] photo) throws Exception {
        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image().withBytes(ByteBuffer.wrap(photo)))
                .withAttributes(Attribute.ALL);

        DetectFacesResult result = rekognitionClient.detectFaces(request);
        if (result.getFaceDetails().isEmpty()) {
            return false;
        }
        if(result.getFaceDetails().size() > 1){
            throw new MultipleFacesException();
        }
        FaceDetail faceDetail = result.getFaceDetails().get(0);

        boolean hasGlasses = faceDetail.getEyeglasses().getValue();
        System.out.println("Has glasses: " + hasGlasses);
        if (hasGlasses) {
            return false;
        }
        return result.getFaceDetails().get(0).getConfidence() > 90;
    }

     private static void detectarFace(RekognitionClient rekognitionClient, String caminhoImagem) {
        try {
            // Ler a imagem como bytes
            SdkBytes bytes = SdkBytes.fromByteBuffer(ByteBuffer.wrap(Files.readAllBytes(new File(caminhoImagem).toPath())));

           
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void uploadToBucket(MultipartFile photo, boolean isTemp) throws Exception {
        String fileName = isTemp ? photo.getOriginalFilename() + "temp" : photo.getOriginalFilename();

        File file = new File(Objects.requireNonNull(fileName));
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(photo.getBytes());
        }

        s3client.putObject(new PutObjectRequest(BUCKET_NAME, fileName, file));
        file.delete();
    }
    
    public void openCam(String id) throws FrameGrabber.Exception {
            OpenCVFrameConverter.ToMat converteMat = new OpenCVFrameConverter.ToMat();
            OpenCVFrameGrabber camera = new OpenCVFrameGrabber(0);
            String[] pessoas = {"", "Jones", "Gabriel"};
            camera.start();
            
            CascadeClassifier detectorFace = new CascadeClassifier("src\\main\\resources\\haarcascade_frontalface_alt.xml");
            
            //FaceRecognizer reconhecedor = EigenFaceRecognizer.create();             // *antes: createEigenFaceRecognizer();
            //reconhecedor.read("src\\recursos\\classificadorEigenFaces.yml");        // *antes: load()
            //reconhecedor.setThreshold(0);
            
            //FaceRecognizer reconhecedor = FisherFaceRecognizer.create();
            //reconhecedor.read("src\\recursos\\classificadorFisherFaces.yml");
            
            //FaceRecognizer reconhecedor = LBPHFaceRecognizer.create();
            //reconhecedor.read("src\\recursos\\classificadorLBPH.yml");
            
            
            CanvasFrame cFrame = new CanvasFrame("ReconhecimentoFACECAM", CanvasFrame.getDefaultGamma() / camera.getGamma());
            Frame frameCapturado = null;
            Mat imagemColorida = new Mat();
            
            while ((frameCapturado = camera.grab()) != null) {
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
                }
                if (cFrame.isVisible()) {
                    cFrame.showImage(frameCapturado);
                }
            }
            cFrame.dispose();
            camera.stop();
    	
    
       /* OpenCVFrameConverter.ToMat converteMat = new OpenCVFrameConverter.ToMat();
        OpenCVFrameGrabber camera = new OpenCVFrameGrabber(0);
        camera.start();
    
        // Corrige o erro getDefaultScreenDevice 
        System.setProperty("java.awt.headless", "false");

		CanvasFrame cFrame = new CanvasFrame("Preview", CanvasFrame.getDefaultGamma()/camera.getGamma()); // Aceleracao de Hardware
       
        Frame frameCapturado = null;
        Mat imagemColorida = new Mat();
        
        while ((frameCapturado = camera.grab()) != null) {

			Java2DFrameConverter converter = new Java2DFrameConverter();
			BufferedImage bufferedImage = converter.convert(frameCapturado);

			imagemColorida = converteMat.convert(frameCapturado);
			ByteBuffer imageBytes1 = null;

			// Converte a imagem OpenCV para o formato aceito pelo AWS Rekognition
			try {
			    Java2DFrameConverter converter1 = new Java2DFrameConverter();
			    BufferedImage bufferedImage1 = converter.convert(frameCapturado);
			   
                CreateStreamProcessorRequest processorRequest = rekognitionClient.createStreamProcessor(new CreateStreamProcessorRequest()
                        .withInput(new StreamProcessorInput()
                                .withKinesisVideoStream(new KinesisVideoStream()
                                        .withArn("arn:aws:kinesisvideo:us-east-1:123456789012:stream/ExampleStream/1476725624098"))
                                .withInputFormat(ImageFormat.JSON))
                        .withOutput(new StreamProcessorOutput()
                                .withKinesisDataStream(new KinesisDataStream()
                                        .withArn("arn:aws:kinesis:us-east-1:123456789012:stream/ExampleOutputStream"))))
                        .withName("my-stream-processor")
                        .withSettings(new StreamProcessorSettings()
                                .withFaceSearch(new FaceSearchSettings()
                                        .withCollectionId("my-collection")
                                        .withFaceMatchThreshold(90F)));
                

			    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
			        imageBytes1 = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
			    }
			} catch (IOException e) {
			    // Lida com qualquer erro de IO ao converter a imagem
			    e.printStackTrace();
			}
            imagemColorida = converteMat.convert(frameCapturado);
            ByteBuffer imageBytes = ByteBuffer.allocateDirect(imagemColorida.cols() * imagemColorida.rows() * imagemColorida.channels());
            
            DetectFacesResult detectFacesResult = rekognitionClient.detectFaces(new DetectFacesRequest()
                    .withImage(new Image()
                            .withBytes((imageBytes)))
                    .withAttributes(Attribute.ALL));


        	List<FaceDetail> faceDetails = detectFacesResult.getFaceDetails();
        	for (FaceDetail faceDetail : faceDetails) {
        	    BoundingBox boundingBox = faceDetail.getBoundingBox();
                // Desenha um retângulo na face detectada
                int x = (int) (boundingBox.getLeft() * imagemColorida.cols());
                int y = (int) (boundingBox.getTop() * imagemColorida.rows());
                int w = (int) (boundingBox.getWidth() * imagemColorida.cols());
                int h = (int) (boundingBox.getHeight() * imagemColorida.rows());
                rectangle(imagemColorida, new Rect(x, y, w, h), new Scalar(0, 255, 0, 0));


        	}

           if (cFrame.isVisible()) {
                cFrame.showImage(frameCapturado);
            }}
        
        cFrame.dispose();
        camera.stop()*/
    }
    
    /*public static void compareTwoFaces(RekognitionClient rekClient, Float similarityThreshold, String sourceImage, String targetImage) {
        try {
            InputStream sourceStream = new FileInputStream(sourceImage);
            InputStream tarStream = new FileInputStream(targetImage);
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);
            SdkBytes targetBytes = SdkBytes.fromInputStream(tarStream);
            	
            // Create an Image object for the source image.
            Image souImage = Image.builder()
                .bytes(sourceBytes)
                .build();

            Image tarImage = Image.builder()
                .bytes(targetBytes)
                .build();

            CompareFacesRequest facesRequest = CompareFacesRequest.builder()
                .sourceImage(souImage)
                .targetImage(tarImage)
                .similarityThreshold(similarityThreshold)
                .build();

            // Compare the two images.
            CompareFacesResponse compareFacesResult = rekClient.compareFaces(facesRequest);
            List<CompareFacesMatch> faceDetails = compareFacesResult.faceMatches();
            for (CompareFacesMatch match: faceDetails){
                ComparedFace face= match.face();
                BoundingBox position = face.boundingBox();
                System.out.println("Face at " + position.left().toString()
                        + " " + position.top()
                        + " matches with " + face.confidence().toString()
                        + "% confidence.");

            }
            List<ComparedFace> uncompared = compareFacesResult.unmatchedFaces();
            System.out.println("There was " + uncompared.size() + " face(s) that did not match");
            System.out.println("Source image rotation: " + compareFacesResult.sourceImageOrientationCorrection());
            System.out.println("target image rotation: " + compareFacesResult.targetImageOrientationCorrection());

        } catch(RekognitionException | FileNotFoundException e) {
            System.out.println("Failed to load source image " + sourceImage);
            System.exit(1);
        }
    }*/
    public static void main(String id) throws FrameGrabber.Exception {
        OpenCVFrameConverter.ToMat converteMat = new OpenCVFrameConverter.ToMat();
        OpenCVFrameGrabber camera = new OpenCVFrameGrabber(0);
        String[] pessoas = {"", "Jones", "Gabriel"};
        camera.start();
        
        CascadeClassifier detectorFace = new CascadeClassifier("src\\main\\resources\\haarcascade_frontalface_alt.xml");
        System.setProperty("java.awt.headless", "true");

        
        CanvasFrame cFrame = new CanvasFrame("Reconhecimento", CanvasFrame.getDefaultGamma() / camera.getGamma());
        Frame frameCapturado = null;
        Mat imagemColorida = new Mat();
        
        while ((frameCapturado = camera.grab()) != null) {
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
            }
            if (cFrame.isVisible()) {
                cFrame.showImage(frameCapturado);
            }
        }
        cFrame.dispose();
        camera.stop();
    }
    
}
