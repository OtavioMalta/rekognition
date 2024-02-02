package br.com.aws.facialrecognition.service;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.apache.tomcat.util.http.fileupload.IOUtils;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
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

import br.com.aws.facialrecognition.dto.FaceAuthenticationResponse;
import br.com.aws.facialrecognition.handler.FacesNotMatchException;
import br.com.aws.facialrecognition.handler.IdNotFoundException;
import br.com.aws.facialrecognition.handler.MultipleFacesException;
import br.com.aws.facialrecognition.handler.NotHumanFaceException;
import br.com.aws.facialrecognition.handler.NotRegisteredFaceException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.rekognition.model.CreateFaceLivenessSessionRequest;



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
    
    public MultipartFile downloadImage(String id) {
        com.amazonaws.services.s3.model.S3Object s3Object = s3client.getObject(BUCKET_NAME, id + ".jpg");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(s3Object.getObjectContent(), baos);
            return new MultipartFile() {
                @Override
                public String getName() {
                    return id + ".jpg";
                }

                @Override
                public String getOriginalFilename() {
                    return id + ".jpg";
                }

                @Override
                public String getContentType() {
                    return "image/jpeg";
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public long getSize() {
                    return baos.size();
                }

                @Override
                public byte[] getBytes() {
                    return baos.toByteArray();
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(baos.toByteArray());
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    Files.write(dest.toPath(), baos.toByteArray());
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public FaceAuthenticationResponse compareFaces(MultipartFile photo1, MultipartFile photo2) throws Exception {
        if (!isFace(photo1.getBytes())) {
            throw new NotHumanFaceException();
        }

        uploadToBucket(photo1, true);
        uploadToBucket(photo2, true);
        
        CompareFacesResult result = rekognitionClient.compareFaces(new CompareFacesRequest()
                .withSourceImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(photo1.getOriginalFilename() + "temp")))
                .withTargetImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(photo2.getOriginalFilename() + "temp")))
                .withSimilarityThreshold(SIMILARITY_PERCENTAGE));

        List<CompareFacesMatch> faceMatches = result.getFaceMatches();
        for (CompareFacesMatch match : faceMatches) {
            System.out.println("Face similarity: " + match.getSimilarity());
        }

        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, photo1.getOriginalFilename() + "temp"));
        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, photo2.getOriginalFilename() + "temp"));
        
        CompareFacesMatch matchedFace = result.getFaceMatches().stream()
                .max(Comparator.comparingDouble(f -> f.getSimilarity()))
                .orElse(null);  

        if(matchedFace == null){
            System.out.println("Nenhuma correspondência encontrada");
            return null;
        }

        return  FaceAuthenticationResponse.builder()
        .userName(photo2.getOriginalFilename())
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
        return result.getFaceDetails().get(0).getConfidence() > 90;
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
  
    public void main(String idPessoa)   throws Exception {
        Loader.load(opencv_core.class);
       Loader.load(opencv_imgproc.class);
       Loader.load(opencv_highgui.class);
       Loader.load(org.bytedeco.opencv.global.opencv_core.class);

      try (OpenCVFrameConverter.ToMat converteMat = new OpenCVFrameConverter.ToMat()) {
		try (OpenCVFrameGrabber camera = new OpenCVFrameGrabber(0)) {
			   camera.start();
			   try (CascadeClassifier detectorFace = new CascadeClassifier("src\\main\\resources\\haarcascade_frontalface_alt.xml")) {
				//corrige o erro getDefaultScreenDevice
				   System.setProperty("java.awt.headless", "false");
				   
				   CanvasFrame cFrame = new CanvasFrame("Reconhecimento.CAM", CanvasFrame.getDefaultGamma() / camera.getGamma());
				   Frame frameCapturado = null;
				   Mat imagemColorida = new Mat();

					long lastCaptureTime = System.currentTimeMillis();
					int captureInterval = 5000; // Intervalo (5 segundos)


				   MultipartFile fotoOriginal = downloadImage(idPessoa);
				   while ((frameCapturado = camera.grab()) != null) {
				       //converte imagem da camera em jpg

				       imagemColorida = converteMat.convert(frameCapturado);
				       Mat imagemCinza = new Mat();
				       cvtColor(imagemColorida, imagemCinza, COLOR_BGRA2GRAY);
				       RectVector facesDetectadas = new RectVector();
				       detectorFace.detectMultiScale(imagemCinza, facesDetectadas, 1.1, 2, 0, new Size(100,100), new Size(500,500));
				       
				       for (int i = 0; i < facesDetectadas.size(); i++) {
				           Rect dadosFace = facesDetectadas.get(i);
				           rectangle(imagemColorida, dadosFace, new Scalar(255,0,255,0));


				           Mat faceCapturada = new Mat(imagemCinza, dadosFace);
				           
				           if ((faceCapturada.size(0) == 160) || (faceCapturada.size(1) == 160)){
				               continue;
				           }  
				           resize(faceCapturada, faceCapturada, new Size(160,160));
				           
				           int x = Math.max(dadosFace.tl().x() - 10, 0);
				           int y = Math.max(dadosFace.tl().y() - 10, 0);

				           //puttext em vermelho
				           putText(imagemColorida, "", new Point(x, y), FONT_HERSHEY_PLAIN, 1.4, new Scalar(255,0,0,255));
				           
				           
				           if (System.currentTimeMillis() - lastCaptureTime >= captureInterval) {
				               
				               resize(faceCapturada, faceCapturada, new Size(160,160));

				               //salva a foto
				               imwrite("src\\fotos\\"+idPessoa +".jpg", faceCapturada);

				               // Pega a foto salvada
				               BufferedImage img = ImageIO.read(new File("src\\fotos\\"+idPessoa +".jpg"));
				               MultipartFile multipartFile = downloadImage("src\\fotos\\"+idPessoa+".jpg");
				               
				               //if(faceCamAuthenticationService.isFace(multipartFile.getBytes())) {
				                   FaceAuthenticationResponse response = compareFaces(multipartFile, fotoOriginal);
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
			}

			   camera.stop();
		}
	}
   }
    
}
