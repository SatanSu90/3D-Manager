package com.manager3d.service;

import com.manager3d.config.MinioConfig;
import com.manager3d.dto.request.SceneSaveRequest;
import com.manager3d.dto.response.SceneResponse;
import com.manager3d.entity.Scene;
import com.manager3d.entity.User;
import com.manager3d.repository.SceneRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SceneService {

    private final SceneRepository sceneRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final MinioConfig minioConfig;

    @Transactional(readOnly = true)
    public Page<Scene> getScenes(Long creatorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (creatorId != null) {
            return sceneRepository.findByCreatorIdOrderByUpdatedAtDesc(creatorId, pageable);
        }
        return sceneRepository.findAll(pageable);
    }

    /**
     * 根据用户名获取该用户的所有场景
     */
    @Transactional(readOnly = true)
    public List<SceneResponse> getScenesByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return sceneRepository.findByCreatorIdWithCreator(user.getId())
                .stream()
                .map(s -> SceneResponse.fromScene(s, minioService))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Scene getScene(Long id) {
        return sceneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
    }

    @Transactional
    public SceneResponse saveScene(String username, SceneSaveRequest request) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String thumbnailKey = null;
        if (request.getThumbnailBase64() != null && !request.getThumbnailBase64().isEmpty()) {
            thumbnailKey = UUID.randomUUID().toString() + "/scene_thumb.jpg";
            MultipartFile thumbnailFile = base64ToMultipartFile(request.getThumbnailBase64(), thumbnailKey);
            minioService.uploadFile(minioConfig.getBucketThumbnails(), thumbnailKey, thumbnailFile);
        }

        Scene scene = Scene.builder()
                .name(request.getName())
                .sceneData(request.getSceneData())
                .thumbnailKey(thumbnailKey)
                .creator(creator)
                .build();

        scene = sceneRepository.save(scene);
        return SceneResponse.fromScene(scene, minioService);
    }

    @Transactional
    public SceneResponse updateScene(Long id, SceneSaveRequest request) {
        Scene scene = sceneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));

        if (request.getName() != null) scene.setName(request.getName());
        if (request.getSceneData() != null) scene.setSceneData(request.getSceneData());

        if (request.getThumbnailBase64() != null && !request.getThumbnailBase64().isEmpty()) {
            if (scene.getThumbnailKey() != null) {
                minioService.deleteFile(minioConfig.getBucketThumbnails(), scene.getThumbnailKey());
            }
            String thumbnailKey = UUID.randomUUID().toString() + "/scene_thumb.jpg";
            MultipartFile thumbnailFile = base64ToMultipartFile(request.getThumbnailBase64(), thumbnailKey);
            minioService.uploadFile(minioConfig.getBucketThumbnails(), thumbnailKey, thumbnailFile);
            scene.setThumbnailKey(thumbnailKey);
        }

        scene = sceneRepository.save(scene);
        return SceneResponse.fromScene(scene, minioService);
    }

    @Transactional
    public void deleteScene(Long id) {
        Scene scene = getScene(id);
        if (scene.getThumbnailKey() != null) {
            minioService.deleteFile(minioConfig.getBucketThumbnails(), scene.getThumbnailKey());
        }
        sceneRepository.delete(scene);
    }

    @Transactional(readOnly = true)
    public SceneResponse getSceneById(Long id) {
        Scene scene = sceneRepository.findByIdWithCreator(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        return SceneResponse.fromScene(scene, minioService);
    }

    /**
     * 导出场景为 GLB 文件
     * GLB 是二进制 glTF 格式，场景数据通过 Base64 编码存储
     * 这里将场景的 sceneData 解析并返回为 GLB 二进制流
     */
    @Transactional(readOnly = true)
    public Resource exportSceneAsGlb(Long id) {
        Scene scene = getScene(id);
        String sceneData = scene.getSceneData();

        byte[] glbData;
        if (sceneData != null && !sceneData.isEmpty()) {
            // 场景数据可能是 Base64 编码的 GLB 文件
            if (sceneData.startsWith("data:application/octet-stream;base64,")) {
                // 去除前缀
                String base64Data = sceneData.substring(sceneData.indexOf(",") + 1);
                glbData = Base64.getDecoder().decode(base64Data);
            } else if (sceneData.startsWith("data:model/gltf-binary;base64,")) {
                // glTF 二进制格式
                String base64Data = sceneData.substring(sceneData.indexOf(",") + 1);
                glbData = Base64.getDecoder().decode(base64Data);
            } else {
                // 尝试直接作为 Base64 解码
                try {
                    glbData = Base64.getDecoder().decode(sceneData);
                } catch (IllegalArgumentException e) {
                    // 如果不是有效的 Base64 或 GLB，返回空文件或占位符
                    glbData = createPlaceholderGlb(scene.getName());
                }
            }
        } else {
            glbData = createPlaceholderGlb(scene.getName());
        }

        return new ByteArrayResource(glbData);
    }

    /**
     * 创建占位符 GLB 文件
     * 这是一个最小化的 glTF 2.0 JSON 文件包装为二进制格式
     */
    private byte[] createPlaceholderGlb(String sceneName) {
        // 创建一个最小的 glTF 文件（只有默认场景）
        String gltfJson = String.format(
            "{\"asset\":{\"version\":\"2.0\",\"generator\":\"3D Manager\"},\"scene\":0,\"scenes\":[{\"name\":\"%s\",\"nodes\":[]}],\"nodes\":[]}",
            sceneName != null ? sceneName : "Exported Scene"
        );

        // GLB 文件格式: magic(4) + version(4) + length(4) + JSON chunk length(4) + JSON chunk type(4) + JSON bytes + BIN chunk (optional)
        int jsonBytesLength = gltfJson.getBytes().length;
        // GLB 头部: magic + version + length
        // JSON Chunk: chunkLength + chunkType (0x4E4F534A = JSON) + jsonBytes
        // BIN Chunk: 0 + 0 + 0 (空 BIN)
        int binChunkLength = 8; // 空的 BIN chunk header
        int totalLength = 12 + 8 + jsonBytesLength + binChunkLength;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // GLB Header
        baos.write(0x67); // 'g'
        baos.write(0x6C); // 'l'
        baos.write(0x54); // 'T'
        baos.write(0x46); // 'F'
        writeInt32(baos, 2); // version
        writeInt32(baos, totalLength); // total length

        // JSON Chunk
        writeInt32(baos, jsonBytesLength); // chunk length
        baos.write(0x4E); // 'J'
        baos.write(0x4F); // 'O'
        baos.write(0x53); // 'S'
        baos.write(0x4E); // 'N'
        baos.writeBytes(gltfJson.getBytes());

        // BIN Chunk (empty)
        writeInt32(baos, 0); // chunk length
        baos.write(0x42); // 'B'
        baos.write(0x49); // 'I'
        baos.write(0x4E); // 'N'
        baos.write(0x20); // ' '

        return baos.toByteArray();
    }

    private void writeInt32(ByteArrayOutputStream baos, int value) {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 24) & 0xFF);
    }

    /**
     * 将 Base64 字符串转换为 MultipartFile
     */
    private MultipartFile base64ToMultipartFile(String base64Data, String filename) {
        String cleanBase64 = base64Data;
        if (base64Data.contains(",")) {
            cleanBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(cleanBase64);
        return new Base64MultipartFile(bytes, filename);
    }

    /**
     * 自定义的 MultipartFile 实现，用于 Base64 数据
     */
    private static class Base64MultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String filename;

        public Base64MultipartFile(byte[] bytes, String filename) {
            this.bytes = bytes;
            this.filename = filename;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "image/jpeg";
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(bytes);
            }
        }
    }

    /**
     * 简单的 Resource 实现，用于返回字节数组
     */
    private static class ByteArrayResource implements Resource {
        private final byte[] data;

        public ByteArrayResource(byte[] data) {
            this.data = data;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public String getDescription() {
            return "GLB Resource";
        }

        @Override
        public Resource createRelative(String relativePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public File getFile() {
            throw new UnsupportedOperationException("ByteArrayResource does not support getFile()");
        }

        @Override
        public URI getURI() {
            throw new UnsupportedOperationException("ByteArrayResource does not support getURI()");
        }

        @Override
        public URL getURL() {
            throw new UnsupportedOperationException("ByteArrayResource does not support getURL()");
        }

        @Override
        public String getFilename() {
            return "scene.glb";
        }

        @Override
        public long contentLength() {
            return data.length;
        }

        @Override
        public long lastModified() {
            return 0;
        }
    }
}
