package com.manager3d.service;

import com.manager3d.config.MinioConfig;
import com.manager3d.entity.Category;
import com.manager3d.entity.Model;
import com.manager3d.entity.Tag;
import com.manager3d.entity.User;
import com.manager3d.repository.CategoryRepository;
import com.manager3d.repository.ModelRepository;
import com.manager3d.repository.TagRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final MinioConfig minioConfig;

    @Transactional(readOnly = true)
    public Page<Model> searchModels(String keyword, Long categoryId, Long uploaderId,
                                     int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return modelRepository.searchModels(keyword, categoryId, uploaderId, pageable);
    }

    @Transactional(readOnly = true)
    public Model getModelById(Long id) {
        return modelRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("模型不存在"));
    }

    @Transactional
    public Model uploadModel(String name, String description, Long categoryId,
                              List<Long> tagIds, MultipartFile file, MultipartFile thumbnail,
                              Long uploaderId) {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String format = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        if (!List.of("glb", "gltf").contains(format)) {
            throw new RuntimeException("仅支持 .glb 和 .gltf 格式");
        }

        String fileKey = UUID.randomUUID().toString() + "/" + originalFilename;
        minioService.uploadFile(minioConfig.getBucketModels(), fileKey, file);

        String thumbnailKey = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailKey = UUID.randomUUID().toString() + "/thumbnail.jpg";
            minioService.uploadFile(minioConfig.getBucketThumbnails(), thumbnailKey, thumbnail);
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        Set<Tag> tags = new HashSet<>();
        if (tagIds != null) {
            tags = tagIds.stream()
                    .map(tagRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        }

        Model model = Model.builder()
                .name(name)
                .description(description)
                .fileKey(fileKey)
                .thumbnailKey(thumbnailKey)
                .fileSize(file.getSize())
                .format(format)
                .category(category)
                .uploader(uploader)
                .downloadCount(0)
                .tags(tags)
                .build();

        return modelRepository.save(model);
    }

    @Transactional
    public void deleteModel(Long id, Long currentUserId, boolean isAdmin) {
        Model model = getModelById(id);

        if (!isAdmin && !model.getUploader().getId().equals(currentUserId)) {
            throw new RuntimeException("无权删除此模型");
        }

        if (model.getFileKey() != null) {
            minioService.deleteFile(minioConfig.getBucketModels(), model.getFileKey());
        }
        if (model.getThumbnailKey() != null) {
            minioService.deleteFile(minioConfig.getBucketThumbnails(), model.getThumbnailKey());
        }

        modelRepository.delete(model);
    }

    @Transactional
    public Model updateModel(Long id, String name, String description, Long categoryId,
                              List<Long> tagIds, Long currentUserId, boolean isAdmin) {
        Model model = getModelById(id);

        if (!isAdmin && !model.getUploader().getId().equals(currentUserId)) {
            throw new RuntimeException("无权修改此模型");
        }

        if (name != null) model.setName(name);
        if (description != null) model.setDescription(description);

        if (categoryId != null) {
            model.setCategory(categoryRepository.findById(categoryId).orElse(null));
        }

        if (tagIds != null) {
            Set<Tag> tags = tagIds.stream()
                    .map(tagRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            model.setTags(tags);
        }

        return modelRepository.save(model);
    }

    @Transactional
    public String getDownloadUrl(Long id) {
        Model model = getModelById(id);
        model.setDownloadCount(model.getDownloadCount() + 1);
        modelRepository.save(model);
        return minioService.getModelUrl(model.getFileKey());
    }

    public String getThumbnailUrl(Model model) {
        if (model.getThumbnailKey() == null) return null;
        return minioService.getThumbnailUrl(model.getThumbnailKey());
    }
}
