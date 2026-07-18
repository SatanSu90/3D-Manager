package com.manager3d.service;

import com.manager3d.config.MinioConfig;
import com.manager3d.dto.response.ResourceResponse;
import com.manager3d.entity.Resource;
import com.manager3d.entity.User;
import com.manager3d.repository.ResourceRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final MinioConfig minioConfig;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> searchResources(String keyword, String type,
                                                   Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Resource.Type typeEnum = type != null ? Resource.Type.valueOf(type) : null;
        Page<Resource> resourcePage = resourceRepository.searchResources(keyword, typeEnum, ownerId, pageable);
        return resourcePage.map(r -> ResourceResponse.from(r, minioService));
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long id) {
        Resource resource = resourceRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        return ResourceResponse.from(resource, minioService);
    }

    /**
     * 上传资源到MinIO。MinIO不可用时上传接口返回错误提示但不影响其他功能。
     */
    @Transactional
    public ResourceResponse uploadResource(MultipartFile file, String type, String tags, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        Resource.Type typeEnum = type != null ? Resource.Type.valueOf(type) : Resource.Type.IMAGE;
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileKey = UUID.randomUUID().toString().replace("-", "") + extension;

        // 上传到MinIO（容错处理）
        try {
            minioService.uploadFile(minioConfig.getBucketResources(), fileKey, file);
        } catch (Exception e) {
            log.error("资源上传到MinIO失败: {}", e.getMessage(), e);
            throw new RuntimeException("资源上传失败：文件存储服务不可用，请稍后重试");
        }

        // 获取图片宽高（仅IMAGE类型）
        Integer width = null;
        Integer height = null;
        if (typeEnum == Resource.Type.IMAGE) {
            try (InputStream is = file.getInputStream()) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    width = img.getWidth();
                    height = img.getHeight();
                }
            } catch (Exception e) {
                log.warn("读取图片尺寸失败: {}", e.getMessage());
            }
        }

        Resource resource = Resource.builder()
                .name(originalName != null ? originalName : fileKey)
                .type(typeEnum)
                .fileKey(fileKey)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .width(width)
                .height(height)
                .tags(tags)
                .owner(owner)
                .build();

        resource = resourceRepository.save(resource);
        return ResourceResponse.from(resource, minioService);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        // 删除MinIO文件（容错处理）
        if (resource.getFileKey() != null) {
            try {
                minioService.deleteFile(minioConfig.getBucketResources(), resource.getFileKey());
            } catch (Exception e) {
                log.warn("删除MinIO文件失败: {}", e.getMessage());
            }
        }
        resourceRepository.delete(resource);
    }

    @Transactional
    public Map<String, Object> batchDelete(List<Long> ids) {
        List<Resource> resources = resourceRepository.findAllById(ids);
        int success = 0;
        int failed = 0;
        for (Resource resource : resources) {
            try {
                if (resource.getFileKey() != null) {
                    minioService.deleteFile(minioConfig.getBucketResources(), resource.getFileKey());
                }
                resourceRepository.delete(resource);
                success++;
            } catch (Exception e) {
                log.warn("删除资源失败 id={}: {}", resource.getId(), e.getMessage());
                failed++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("total", resources.size());
        return result;
    }

    /**
     * 下载资源文件。返回Resource + contentType。
     */
    @Transactional(readOnly = true)
    public ResourceDownloadResult downloadResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        InputStream stream = minioService.getFile(minioConfig.getBucketResources(), resource.getFileKey());
        String filename = resource.getName();
        return new ResourceDownloadResult(
                new InputStreamResource(stream),
                resource.getMimeType() != null ? resource.getMimeType() : "application/octet-stream",
                filename
        );
    }

    public record ResourceDownloadResult(InputStreamResource resource, String contentType, String filename) {}
}
