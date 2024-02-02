package br.com.aws.facialrecognition.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

@Slf4j
@Service
public class FaceAuthenticationService {

    private final AmazonS3 s3client;
    private final AmazonRekognition rekognitionClient;
    private static final float SIMILARITY_PERCENTAGE = 50L;
    private static final String COLLECTION_ID = "users-photos";
    private static final String BUCKET_NAME = System.getenv("BUCKET_NAME");

    public FaceAuthenticationService(AmazonS3 s3client,
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

    private Boolean isFace(byte[] photo) throws Exception {
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
}
