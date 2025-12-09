package com.back.domain.post.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.back.domain.post.dto.req.PostImageReqBody;
import com.back.domain.post.dto.res.PostImageResBody;
import com.back.domain.post.entity.Post;
import com.back.domain.post.entity.PostImage;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3FolderType;
import com.back.global.s3.S3Uploader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostImageService {

	private final S3Uploader s3;

	public List<PostImage> createImages(Post post, List<MultipartFile> files, List<PostImageReqBody> reqBodies) {
		if (files == null || files.isEmpty()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지는 최소 1개 이상 등록해야 합니다.");
		}

		if (reqBodies == null || reqBodies.size() != files.size()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지 정보(images)와 파일 개수가 일치해야 합니다.");
		}

		List<PostImage> result = new ArrayList<>();

		for (int i = 0; i < files.size(); i++) {
			String url = s3.upload(files.get(i), S3FolderType.POST_IMAGE);
			boolean isPrimary = reqBodies.get(i).isPrimary();
			result.add(new PostImage(post, url, isPrimary));
		}

		return result;
	}

	public List<PostImage> updateImages(Post post, List<MultipartFile> files, List<PostImageReqBody> reqBodies) {

		List<Long> keepImageIds = reqBodies.stream()
			.map(PostImageReqBody::id)
			.filter(Objects::nonNull)
			.toList();

		post.getImages().stream()
			.filter(img -> !keepImageIds.contains(img.getId()))
			.forEach(img -> s3.delete(img.getImageUrl()));

		List<PostImage> result = new ArrayList<>();
		int fileIndex = 0;

		for (PostImageReqBody req : reqBodies) {
			if (req.id() != null) {

				PostImage existing = post.getImages()
					.stream()
					.filter(img -> img.getId().equals(req.id()))
					.findFirst()
					.orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND,
						"존재하지 않는 이미지입니다: " + req.id()));

				result.add(new PostImage(post, existing.getImageUrl(), req.isPrimary()));

			} else {

				if (files == null || fileIndex >= files.size()) {
					throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지 정보와 파일 개수가 일치하지 않습니다.");
				}

				String url = s3.upload(files.get(fileIndex), S3FolderType.POST_IMAGE);
				result.add(new PostImage(post, url, req.isPrimary()));
				fileIndex++;
			}
		}

		if (result.isEmpty()) {
			throw new ServiceException(HttpStatus.BAD_REQUEST, "이미지는 최소 1개 이상 등록해야 합니다.");
		}

		return result;
	}

	public void deleteImages(Post post) {
		post.getImages().forEach(img -> {
			if (img.getImageUrl() != null) {
				s3.delete(img.getImageUrl());
			}
		});
	}

	public String toPresignedUrl(String url) {
		if (url == null)
			return null;
		return s3.generatePresignedUrl(url);
	}

	public List<PostImageResBody> toImageResBodies(List<PostImage> images) {
		return images.stream()
			.map(img -> PostImageResBody.of(img, toPresignedUrl(img.getImageUrl())))
			.toList();
	}

	public String toThumbnailUrl(Post post) {
		return post.getImages().stream()
			.filter(PostImage::getIsPrimary)
			.findFirst()
			.map(img -> toPresignedUrl(img.getImageUrl()))
			.orElse(null);
	}
}
