document.addEventListener('DOMContentLoaded', function () {
    // Configure AWS SDK
    AWS.config.update({
        region: 'us-east-2', 
        accessKeyId: '',  
        secretAccessKey: ''
    });
    
    const BUCKET_NAME = ''; 
    const id = 'otavio';
    const Intervalo_captura = 1000;
    const SIMILARITY_PERCENTAGE = 90;
    const rekognition = new AWS.Rekognition();
    const s3 = new AWS.S3();
    
    const video = document.getElementById('video');
    
    Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri('https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights'),
        faceapi.nets.faceLandmark68Net.loadFromUri('https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights'),
        faceapi.nets.faceRecognitionNet.loadFromUri('https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights'),
        faceapi.nets.faceExpressionNet.loadFromUri('https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights')
    ]).then(startVideo);
    
    function startVideo() {
        navigator.getUserMedia(
            { video: {} },
            stream => video.srcObject = stream,
            err => console.error(err)
        );
    }
    
    let lastCompareTime = 0;
    
    // Inicia a detecção de faces
    video.addEventListener('play', () => {
        const canvas = faceapi.createCanvasFromMedia(video);
        document.body.append(canvas);
        const displaySize = { width: video.width, height: video.height };
        faceapi.matchDimensions(canvas, displaySize);
    
        setInterval(async () => {
            const detections = await faceapi.detectAllFaces(video, new faceapi.TinyFaceDetectorOptions()).withFaceLandmarks().withFaceExpressions();
            const resizedDetections = faceapi.resizeResults(detections, displaySize);
            canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
            faceapi.draw.drawDetections(canvas, resizedDetections);
    
            if (resizedDetections.length > 0) {
                console.log("Face detectada");
                const currentTime = new Date().getTime();
                if (currentTime - lastCompareTime >= 10000) {
                    compareImages(captureImage(video));
                    lastCompareTime = currentTime;
                }
            }
        }, Intervalo_captura); // Intervalo para detecção de faces
    });
    
    // Função auxiliar para capturar imagem do vídeo 
    function captureImage(videoElement) {
        const canvas = document.createElement('canvas');
        canvas.width = videoElement.width;
        canvas.height = videoElement.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
        return canvas.toDataURL('image/png').replace(/^data:image\/png;base64,/, '');
    }
    
    // Função auxiliar para converter base64 para arraybuffer
    function base64ToArrayBuffer(base64) {
        const binaryString = atob(base64);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return bytes.buffer;
    }
    
    // Função para comparar imagens
    function compareImages(imageData) {
        const params = {
            SourceImage: {
                Bytes: base64ToArrayBuffer(imageData)
            },
            TargetImage: {
                S3Object: {
                    Bucket: BUCKET_NAME,
                    Name: id + '.jpg'
                }
            },
            SimilarityThreshold: SIMILARITY_PERCENTAGE

        };
    
        rekognition.compareFaces(params, (err, data) => {
            if (err) {
                console.error("Erro ao comparar imagens", err);
            } else {
                if (data.FaceMatches.length > 0 && data.FaceMatches[0].Similarity >= SIMILARITY_PERCENTAGE) {
                    console.log("Similaridade:", data.FaceMatches[0].Similarity);
                    console.log("Face corresponde!");
                    // tirar o video, parar a detecção de faces e exibir a mensagem de sucesso com a porcentagem de similaridade
                    video.srcObject.getVideoTracks().forEach(track => track.stop());
                    video.remove();
                    document.getElementById('success').style.display = 'block';
                    document.getElementById('similaridade').innerText = data.FaceMatches[0].Similarity + '%';
                } else if (data.FaceMatches.length > 0) {
                    console.log("Similaridade:", data.FaceMatches[0].Similarity);
                    console.log("Face não corresponde!");
                } else {
                    console.log("Nenhuma face correspondente encontrada");
                }
            }
        });
    }
});