package br.com.atendecerto.recognition.service;

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

import org.apache.tomcat.util.http.fileupload.IOUtils;
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

import br.com.atendecerto.recognition.dto.RecognitionResponse;
import br.com.atendecerto.recognition.handler.MultipleFacesException;
import br.com.atendecerto.recognition.handler.NotHumanFaceException;
import br.com.atendecerto.recognition.handler.NotRegisteredFaceException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class RecognitionService {
    private final AmazonS3 s3client;
    private final AmazonRekognition RecognitionClient;
    private static final float SIMILARITY_PERCENTAGE = 50L;
    private static final String COLLECTION_ID = "users-fotos";
    private static final String BUCKET_NAME = System.getenv("BUCKET_NAME");

    @Autowired
    public RecognitionService(AmazonS3 s3client,
        AmazonRekognition RecognitionClient) {
        this.s3client = s3client;
        this.RecognitionClient = RecognitionClient;
    }

    public void salvar(MultipartFile foto) throws Exception {
        if (!isFace(foto.getBytes())) {
            throw new NotHumanFaceException();
        }

        uploadToBucket(foto, false);

        try {
            RecognitionClient.createCollection(new CreateCollectionRequest()
                    .withCollectionId(COLLECTION_ID));
        } catch (ResourceAlreadyExistsException e) {
            log.warn("Collection já existente");
        } catch (Exception e) {
            log.error("Erro ao criar collection");
            throw e;
        } 

        RecognitionClient.indexFaces(new IndexFacesRequest()
                .withImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(foto.getOriginalFilename())))
                .withCollectionId(COLLECTION_ID)
                .withExternalImageId(foto.getOriginalFilename())
                .withDetectionAttributes("ALL"));
    }

    public RecognitionResponse buscar(MultipartFile foto) throws Exception {
        if (!isFace(foto.getBytes())) {
            throw new NotHumanFaceException();
        }

        uploadToBucket(foto, true);
        
        SearchFacesByImageResult result = RecognitionClient.searchFacesByImage(new SearchFacesByImageRequest()
                .withCollectionId(COLLECTION_ID)
                .withImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(foto.getOriginalFilename() + "temp")))
                .withFaceMatchThreshold(SIMILARITY_PERCENTAGE));

        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, foto.getOriginalFilename() + "temp"));

        if (result.getFaceMatches().isEmpty()) {
            throw new NotRegisteredFaceException();
        }	

        FaceMatch matchedFace = result.getFaceMatches().stream()
                .max(Comparator.comparing(f -> f.getFace().getConfidence()))
                .orElseThrow(Exception::new);

        return RecognitionResponse.builder()
                .idUsuario(matchedFace.getFace().getExternalImageId())
                .confianca(BigDecimal.valueOf(matchedFace.getFace().getConfidence()))
                .similaridade(BigDecimal.valueOf(matchedFace.getSimilarity()))
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

    public RecognitionResponse comparar(MultipartFile foto, String id) throws Exception {
        if (!isFace(foto.getBytes())) {
            System.out.println("Rosto não humano. Tente novamente.");
            return null;
        } else {
        	// transforma foto em bytebuffer
            ByteBuffer byteBuffer = ByteBuffer.wrap(foto.getBytes());
            CompareFacesResult result = RecognitionClient.compareFaces(new CompareFacesRequest()
                    .withSourceImage(new Image()
                            .withS3Object(new S3Object()
                                    .withBucket(BUCKET_NAME)
                                    .withName(id + ".jpg")))
                    //envia diretamente a imagem para comparar
                    .withTargetImage(new Image()
                            .withBytes(byteBuffer))
                    .withSimilarityThreshold(SIMILARITY_PERCENTAGE));
                    

            List<CompareFacesMatch> faceMatches = result.getFaceMatches();
            for (CompareFacesMatch match : faceMatches) {
                System.out.println("Similaridade: " + match.getSimilarity());
            }

            s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, foto.getOriginalFilename() + "temp"));

            if (result.getFaceMatches().isEmpty()) {
                //throw new FacesNotMatchException();
            	System.out.println("Os rostos não coincidem");
            }
            
            CompareFacesMatch matchedFace = result.getFaceMatches().stream()
                    .max(Comparator.comparingDouble(f -> f.getSimilarity()))
                    .orElseThrow(() -> new RuntimeException("Nenhuma correspondência encontrada"));

            return RecognitionResponse.builder()
                    .idUsuario(id+".jpg")
                    .confianca(BigDecimal.valueOf(matchedFace.getFace().getConfidence()))
                    .similaridade(BigDecimal.valueOf(matchedFace.getSimilarity()))
                    .build();
        }
    }

    public RecognitionResponse compararFotos(MultipartFile foto1, MultipartFile foto2) throws Exception {
        if (!isFace(foto1.getBytes())) {
            System.out.println("Rosto não humano. Tente novamente.");
        }

        uploadToBucket(foto1, true);
        uploadToBucket(foto2, true);
        
        CompareFacesResult result = RecognitionClient.compareFaces(new CompareFacesRequest()
                .withSourceImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(foto1.getOriginalFilename() + "temp")))
                .withTargetImage(new Image()
                        .withS3Object(new S3Object()
                                .withBucket(BUCKET_NAME)
                                .withName(foto2.getOriginalFilename() + "temp")))
                .withSimilarityThreshold(SIMILARITY_PERCENTAGE));

        List<CompareFacesMatch> faceMatches = result.getFaceMatches();
        for (CompareFacesMatch match : faceMatches) {
            System.out.println("Face similarity: " + match.getSimilarity());
        }

        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, foto1.getOriginalFilename() + "temp"));
        s3client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, foto2.getOriginalFilename() + "temp"));
        
        CompareFacesMatch matchedFace = result.getFaceMatches().stream()
                .max(Comparator.comparingDouble(f -> f.getSimilarity()))
                .orElse(null);  

        if(matchedFace == null){
            System.out.println("Nenhuma correspondência encontrada");
            return null;
        }

        return  RecognitionResponse.builder()
        .idUsuario(foto2.getOriginalFilename())
        .confianca(BigDecimal.valueOf(matchedFace.getFace().getConfidence()))
        .similaridade(BigDecimal.valueOf(matchedFace.getSimilarity()))
        .build();
    }
    

    public Boolean isFace(byte[] foto) throws Exception {
        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image().withBytes(ByteBuffer.wrap(foto)))
                .withAttributes(Attribute.ALL);

        DetectFacesResult result = RecognitionClient.detectFaces(request);
        if (result.getFaceDetails().isEmpty()) {
            return false;
        }
        if(result.getFaceDetails().size() > 1){
            throw new MultipleFacesException();
        }
        FaceDetail faceDetail = result.getFaceDetails().get(0);

        boolean hasGlasses = faceDetail.getEyeglasses().getValue();
        
        System.out.println("Usa óculos: " + hasGlasses);
        return result.getFaceDetails().get(0).getConfidence() > 90;
    }

    
    private void uploadToBucket(MultipartFile foto, boolean isTemp) throws Exception {
        String fileName = isTemp ? foto.getOriginalFilename() + "temp" : foto.getOriginalFilename();

        File file = new File(Objects.requireNonNull(fileName));
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(foto.getBytes());
        }

        s3client.putObject(new PutObjectRequest(BUCKET_NAME, fileName, file));
        file.delete();
    }
    
}
