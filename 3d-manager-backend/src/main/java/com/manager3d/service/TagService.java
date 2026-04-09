package com.manager3d.service;

import com.manager3d.entity.Tag;
import com.manager3d.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Transactional
    public Tag createTag(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("标签名称不能为空");
        }
        if (tagRepository.existsByName(name)) {
            throw new RuntimeException("标签已存在");
        }
        Tag tag = Tag.builder().name(name).build();
        return tagRepository.save(tag);
    }

    @Transactional
    public Tag updateTag(Long id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("标签名称不能为空");
        }
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        tag.setName(name);
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }
}
