package com.back.global.s3;

import com.back.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class S3Uploader {

	private S3Client s3;
	private S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.cloudfront.domain}")
	private String cloudfrontDomain;

	public S3Uploader(S3Client s3, S3Presigner s3Presigner) {
		this.s3 = s3;
		this.s3Presigner = s3Presigner;
	}

	public String upload(MultipartFile image, S3FolderType folderType) {
		try {
			String filename = UUID.randomUUID() + "-" + image.getOriginalFilename();
			String key = folderType.getPath() + filename;

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(image.getContentType())
				.build();

			s3.putObject(putObjectRequest, RequestBody.fromBytes(image.getBytes()));

			return "https://" + cloudfrontDomain + "/" + key;

		} catch (IOException e) {
			throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 실패");
		}
	}

	public void delete(String imageUrl) {
		try {
			String key = extractKey(imageUrl);

			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

			s3.deleteObject(deleteObjectRequest);

		} catch (Exception e) {
			throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 삭제 실패");
		}
	}

	public String generatePresignedUrl(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return null;
		}
		String key = extractKey(imageUrl);

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.getObjectRequest(getObjectRequest)
				.signatureDuration(Duration.ofMinutes(10))
				.build();

		PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
		return presigned.url().toString();
	}

	public String uploadProfileOriginal(MultipartFile image) {
		try {
			String filename = UUID.randomUUID() + "-" + image.getOriginalFilename();
			String key = S3FolderType.MEMBER_PROFILE_ORIGINAL.getPath() + filename;

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(image.getContentType())
					.build();

			s3.putObject(putObjectRequest, RequestBody.fromBytes(image.getBytes()));

			log.info("프로필 원본 업로드 완료: {}", key);

			// CloudFront 설정되어 있으면 CloudFront URL, 아니면 S3 URL
			if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
				return "https://" + cloudfrontDomain + "/" + key;
			} else {
				return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
			}

		} catch (IOException e) {
			throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지 업로드 실패");
		}
	}

	public String getProfileThumbnailUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			return null;
		}

		try {
			// URL에서 key 추출
			String key = extractKey(originalUrl);

			// members/profile/originals/uuid-photo.jpg인지 확인
			if (!key.startsWith("members/profile/originals/")) {
				log.warn("프로필 이미지가 아닙니다: {}", key);
				// 프로필 이미지 아니면 기존 방식 (Presigned URL) 사용
				return generatePresignedUrl(originalUrl);
			}

			// 파일명 추출 및 확장자 제거
			String filename = key.substring("members/profile/originals/".length());
			String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
			String thumbnailKey = "members/profile/resized/thumbnail/" + nameWithoutExt + ".webp";

			// CloudFront 설정되어 있으면 CloudFront URL, 아니면 S3 URL
			if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
				return "https://" + cloudfrontDomain + "/" + thumbnailKey;
			} else {
				return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + thumbnailKey;
			}

		} catch (Exception e) {
			log.error("썸네일 URL 생성 실패: {}", originalUrl, e);
			// 실패하면 원본 URL 그대로 반환
			return originalUrl;
		}
	}

	public void deleteProfileSafely(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return;
		}

		try {
			String key = extractKey(imageUrl);

			// 프로필 원본만 삭제 (썸네일은 Lambda가 생성한 것이므로 그냥 둠)
			if (key.startsWith("members/profile/originals/")) {
				delete(imageUrl);
				log.info("프로필 원본 이미지 삭제: {}", key);
			} else {
				log.warn("프로필 원본 이미지가 아니므로 삭제 안 함: {}", key);
			}
		} catch (Exception e) {
			log.error("프로필 이미지 삭제 실패: {}", imageUrl, e);
		}
	}

	// S3 URL에서 객체 키 추출
	private String extractKey(String url) {
		int idx = url.indexOf(".net/");
		if (idx != -1) {
			return url.substring(idx + 5); // ".net/" 이후
		}

		idx = url.indexOf(".com/");
		if (idx != -1) {
			return url.substring(idx + 5); // ".com/" 이후
		}

		throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 URL 형식 오류");
	}
}
