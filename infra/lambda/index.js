const sharp = require('sharp');
const { S3Client, GetObjectCommand, PutObjectCommand } = require("@aws-sdk/client-s3");

const s3 = new S3Client({ region: "ap-northeast-2" });

// 환경변수
const BUCKET_NAME = process.env.BUCKET_NAME;
const SOURCE_PREFIX = process.env.SOURCE_PREFIX || 'members/profile/originals/';
const DESTINATION_PREFIX = process.env.DESTINATION_PREFIX || 'members/profile/resized/thumbnail/';

const THUMBNAIL_SIZE = 150;
const QUALITY = 85;

function streamToBuffer(stream) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        stream.on("data", chunk => chunks.push(chunk));
        stream.on("end", () => resolve(Buffer.concat(chunks)));
        stream.on("error", reject);
    });
}

exports.handler = async (event) => {
    console.log('Event:', JSON.stringify(event, null, 2));
    
    // S3 이벤트에서 정보 추출
    const bucket = event.Records[0].s3.bucket.name;
    const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, ' '));
    
    console.log(`Event received - Bucket: ${bucket}, Key: ${key}`);
    
    // 버킷 검증
    if (bucket !== BUCKET_NAME) {
        console.log(`❌ Skip: Wrong bucket (expected: ${BUCKET_NAME})`);
        return { statusCode: 200, body: 'Skipped: wrong bucket' };
    }
    
    // 경로 검증
    if (!key.startsWith(SOURCE_PREFIX)) {
        console.log(`❌ Skip: Wrong path (expected: ${SOURCE_PREFIX})`);
        return { statusCode: 200, body: 'Skipped: wrong path' };
    }
    
    try {
        // 원본 이미지 다운로드
        console.log(`📥 Downloading: ${key}`);
        const originalImage = await s3.send(
            new GetObjectCommand({
                Bucket: bucket,
                Key: key
            })
        );

        const imageBuffer = await streamToBuffer(originalImage.Body);
        
        // 이미지 리사이징 (정사각형 썸네일)
        console.log(`🖼️  Resizing to ${THUMBNAIL_SIZE}x${THUMBNAIL_SIZE}...`);
        const resizedImage = await sharp(imageBuffer)
            .resize(THUMBNAIL_SIZE, THUMBNAIL_SIZE, {
                fit: 'cover',
                position: 'centre'
            })
            .webp({
                quality: QUALITY,
                effort: 6
            })
            .toBuffer();
        
        // 대상 key 생성
        const filename = key.split('/').pop();
        const nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        const destinationKey = `${DESTINATION_PREFIX}${nameWithoutExt}.webp`;
        
        // S3 업로드
        console.log(`📤 Uploading: ${destinationKey}`);
        await s3.send(
            new PutObjectCommand({
                Bucket: bucket,
                Key: destinationKey,
                Body: resizedImage,
                ContentType: "image/webp",
                CacheControl: "max-age=31536000"
            })
        );
        
        console.log(`✅ Success: ${key} → ${destinationKey}`);
        console.log(`Size: ${imageBuffer.length} → ${resizedImage.length} bytes`);
        
        return {
            statusCode: 200,
            body: JSON.stringify({
                original: key,
                thumbnail: destinationKey,
                size: `${THUMBNAIL_SIZE}x${THUMBNAIL_SIZE}`,
                originalSize: imageBuffer.length,
                thumbnailSize: resizedImage.length
            })
        };
        
    } catch (error) {
        console.error(`❌ Error: ${error.message}`);
        console.error(error.stack);
        
        // 에러가 발생해도 성공으로 처리 (무한 재시도 방지)
        return {
            statusCode: 200,
            body: JSON.stringify({
                error: error.message
            })
        };
    }
};