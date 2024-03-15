document.addEventListener('DOMContentLoaded', function () {
    const INTERVALO_CAPTURA = 1000;
    const SIMILARITY_PERCENTAGE = 90;
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
        setInterval(async () => {
            const detections = await faceapi.detectAllFaces(video, new faceapi.TinyFaceDetectorOptions()).withFaceLandmarks().withFaceExpressions();
    
            if (detections.length > 0) {
                console.log("Face detectada");
                const currentTime = new Date().getTime();
                if (currentTime - lastCompareTime >= 10000) {
                    const image = captureImage(video);
                    const imageData = {
                        foto: image,
                        id: 'otavio'
                    };
                    sendImageToAPI(imageData);
                    lastCompareTime = currentTime;
                }
            }
        }, INTERVALO_CAPTURA); // Intervalo para detecção de faces
    });
    
    // Função auxiliar para capturar imagem do vídeo 
    function captureImage(videoElement) {
        const canvas = document.createElement('canvas');
        canvas.width = videoElement.videoWidth;
        canvas.height = videoElement.videoHeight;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
        return canvas.toDataURL('image/jpeg');
    }
    
    // Função para enviar a imagem para a API
    function sendImageToAPI(imageData) {
        const formData = new FormData();
        formData.append('foto', dataURItoBlob(imageData.foto), 'image.jpeg');
        formData.append('id', imageData.id);

        fetch('http://localhost:8080/recognition/comparar', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to send image to API');
            }
            return response.json();
        })
        .then(data => {
            console.log('API response:', data);
            if (data.similaridade >= SIMILARITY_PERCENTAGE) {
                console.log('Usuário reconhecido');
                video.srcObject.getVideoTracks().forEach(track => track.stop());
                    video.remove();
                    document.getElementById('success').style.display = 'block';
                    document.getElementById('similaridade').innerText = data.similaridade + '%';
            } else {
                console.log('Usuário não reconhecido. Similaridade:', data.similaridade);
            }
            
        })
        .catch(error => {
            console.error('Error sending image to API:', error);
        });
    }

    // Função auxiliar para converter a imagem em Blob
    function dataURItoBlob(dataURI) {
        const byteString = atob(dataURI.split(',')[1]);
        const mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];
        const ab = new ArrayBuffer(byteString.length);
        const ia = new Uint8Array(ab);
        for (let i = 0; i < byteString.length; i++) {
            ia[i] = byteString.charCodeAt(i);
        }
        const blob = new Blob([ab], { type: mimeString });
        return blob;
    }
});
