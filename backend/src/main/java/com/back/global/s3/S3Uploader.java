package com.back.global.s3;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.back.global.exception.ServiceException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service

public class S3Uploader {

	private S3Client s3;
	private S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public S3Uploader(S3Client s3, S3Presigner s3Presigner) {
		this.s3 = s3;
		this.s3Presigner = s3Presigner;
	}

	public String upload(MultipartFile image) {
		try {
			String key = "post/" + UUID.randomUUID() + "-" + image.getOriginalFilename();

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(image.getContentType())
				.build();

			s3.putObject(putObjectRequest, RequestBody.fromBytes(image.getBytes()));

			return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;

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

	private String extractKey(String url) {
		int idx = url.indexOf(".com/");
		if (idx == -1) {
			throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 URL 형식 오류");
		}
		return url.substring(idx + 5); // ".com/" 이후부터 추출
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

}
