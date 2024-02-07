

// Configure AWS SDK
AWS.config.update({
  region: 'us-east-2',  // Replace with your AWS region, e.g., 'us-east-1'
  accessKeyId: 'AKIAXLZC4UQFHFKM3PHN',  // Replace with your AWS access key ID
  secretAccessKey: '1L6BoMTqwEPa6vIJc9GCK5zGfUv94rs72iuoWyCm'  // Replace with your AWS secret access key
});

const BUCKET_NAME = 'atendebucket'; // Replace with your S3 bucket name
const SIMILARITY_PERCENTAGE = 90; // Set your desired similarity threshold

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


video.addEventListener('play', () => {
	const canvas = faceapi.createCanvasFromMedia(video);
	document.body.append(canvas);
	const displaySize = { width: video.width, height: video.height };
	faceapi.matchDimensions(canvas, displaySize);
	setInterval(async () => {
		const detections = await faceapi.detectAllFaces(video, 
		new faceapi.TinyFaceDetectorOptions()).withFaceLandmarks().withFaceExpressions();
		const resizedDetections = faceapi.resizeResults(detections, displaySize);
		canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
		faceapi.draw.drawDetections(canvas, resizedDetections);
		//faceapi.draw.drawFaceLandmarks(canvas, resizedDetections);
		//faceapi.draw.drawFaceExpressions(canvas, resizedDetections);

        // verifica se alguma face foi detectada
        if (resizedDetections.length > 0) {
            lastCapturedImage = captureImage(video);
            //Compara a imagem capturada com a imagem do banco de dados
            //compareImages(lastCapturedImage);

        }
	}, 1000);
	
});

function captureImage(videoElement) {
    const canvas = document.createElement('canvas');
    canvas.width = videoElement.width;
    canvas.height = videoElement.height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);

    // Converte o canvas para o formato de dados da imagem
    return canvas.toDataURL('image/png').replace(/^data:image\/png;base64,/, '');
}


function saveImage(videoElement, imageName) {
    const canvas = document.createElement('canvas');
    canvas.width = videoElement.width;
    canvas.height = videoElement.height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);

    // Converte o canvas para o formato de dados da imagem
    const imageData = canvas.toDataURL('image/png').replace(/^data:image\/png;base64,/, '');

    // Cria um objeto Blob representando a imagem
    const blob = base64ToBlob(imageData, 'image/png');

    // Cria um link para download e simula um clique para baixar a imagem
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.download = `${imageName}.png`;
    link.click();
}

// Função auxiliar para converter base64 para Blob
function base64ToBlob(base64, mimeType) {
    const byteString = atob(base64);
    const arrayBuffer = new ArrayBuffer(byteString.length);
    const intArray = new Uint8Array(arrayBuffer);

    for (let i = 0; i < byteString.length; i++) {
        intArray[i] = byteString.charCodeAt(i);
    }

    return new Blob([intArray], { type: mimeType });
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





function deleteObject(bucket, key) {
    return new Promise((resolve, reject) => {
        const params = {
            Bucket: bucket,
            Key: key
        };
        s3.deleteObject(params, (err, data) => {
            if (err) {
                reject(err);
            } else {
                resolve(data);
            }
        });
    });
}


function compareImages(imageData) {
    const params = {
        SourceImage: {
            Bytes: base64ToArrayBuffer(imageData)
        },
        TargetImage: {
            S3Object: {
                Bucket: BUCKET_NAME,
                Name: 'otavio.jpg'
            }
        },
        SimilarityThreshold: SIMILARITY_PERCENTAGE

    };

    rekognition.compareFaces(params, (err, data) => {
        if (err) {
            console.error("Error comparing images:", err);
        } else {
            if (data.FaceMatches.length > 0) {
                console.log("similarity: " + data.FaceMatches[0].Similarity + "%");
                console.log("Face matched!");
            } else {
                
                console.log("similarity: " + data.FaceMatches[0].Similarity + "%");
                console.log("Face not matched!");
            }
        }
    });
}



/*
function doesImageExist(id) {
    return new Promise((resolve, reject) => {
        const params = {
            Bucket: BUCKET_NAME,
            Key: id + '.jpg'
        };

        s3.headObject(params, (err, data) => {
            if (err) {
                console.error("Error checking image existence:", err);
                if (err.code === 'NotFound') {
                    resolve(false);
                } else {
                    reject(err);
                }
            } else {
                resolve(true);
            }
        });
    });
}


function downloadFromBucket(id) {
        return new Promise((resolve, reject) => {
            const params = {
                Bucket: BUCKET_NAME,
                Key: id + '.jpg'
            };
            s3.getObject(params, (err, data) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(data.Body);
                }
            });
        });
    }

function uploadToBucket(photo, isTemp) {
	return new Promise((resolve, reject) => {
		const params = {
			Bucket: BUCKET_NAME,
			Key: photo.name + (isTemp ? 'temp' : '') + '.jpg',
			Body: photo
		};
		s3.upload(params, (err, data) => {
			if (err) {
				reject(err);
			} else {
				resolve(data);
			}
		});
	});
}


/*const video = document.getElementById('video');

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


video.addEventListener('play', () => {
	const canvas = faceapi.createCanvasFromMedia(video);
	document.body.append(canvas);
	const displaySize = { width: video.width, height: video.height };
	faceapi.matchDimensions(canvas, displaySize);
	setInterval(async () => {
		const detections = await faceapi.detectAllFaces(video, 
		new faceapi.TinyFaceDetectorOptions()).withFaceLandmarks().withFaceExpressions();
		const resizedDetections = faceapi.resizeResults(detections, displaySize);
		canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
		faceapi.draw.drawDetections(canvas, resizedDetections);
		//faceapi.draw.drawFaceLandmarks(canvas, resizedDetections);
		//faceapi.draw.drawFaceExpressions(canvas, resizedDetections);
		
	}, 100);
	
});*/