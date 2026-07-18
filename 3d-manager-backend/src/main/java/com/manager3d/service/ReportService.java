package com.manager3d.service;

import com.manager3d.dto.request.ReportCreateRequest;
import com.manager3d.dto.response.ReportResponse;
import com.manager3d.entity.Report;
import com.manager3d.entity.Scene;
import com.manager3d.entity.User;
import com.manager3d.repository.ReportRepository;
import com.manager3d.repository.SceneRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SceneRepository sceneRepository;

    @Transactional(readOnly = true)
    public Page<ReportResponse> searchReports(String keyword, String type, String status,
                                               Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Report.Type typeEnum = type != null ? Report.Type.valueOf(type) : null;
        Report.Status statusEnum = status != null ? Report.Status.valueOf(status) : null;
        Page<Report> reportPage = reportRepository.searchReports(keyword, typeEnum, statusEnum, ownerId, pageable);
        return reportPage.map(ReportResponse::from);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long id) {
        Report report = reportRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new RuntimeException("报表不存在"));
        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse createReport(ReportCreateRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Report report = Report.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType() != null ? Report.Type.valueOf(request.getType()) : Report.Type.DASHBOARD)
                .config(request.getConfig())
                .sceneId(request.getSceneId())
                .status(request.getStatus() != null ? Report.Status.valueOf(request.getStatus()) : Report.Status.DRAFT)
                .visibility(request.getVisibility() != null ?
                        Report.Visibility.valueOf(request.getVisibility()) : Report.Visibility.PRIVATE)
                .thumbnailKey(request.getThumbnailKey())
                .owner(owner)
                .build();

        report = reportRepository.save(report);
        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse updateReport(Long id, ReportCreateRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报表不存在"));

        if (request.getName() != null) report.setName(request.getName());
        if (request.getDescription() != null) report.setDescription(request.getDescription());
        if (request.getType() != null) report.setType(Report.Type.valueOf(request.getType()));
        if (request.getConfig() != null) report.setConfig(request.getConfig());
        if (request.getSceneId() != null) report.setSceneId(request.getSceneId());
        if (request.getStatus() != null) report.setStatus(Report.Status.valueOf(request.getStatus()));
        if (request.getVisibility() != null) report.setVisibility(Report.Visibility.valueOf(request.getVisibility()));
        if (request.getThumbnailKey() != null) report.setThumbnailKey(request.getThumbnailKey());

        report = reportRepository.save(report);
        return ReportResponse.from(report);
    }

    @Transactional
    public void deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报表不存在"));
        reportRepository.delete(report);
    }

    @Transactional
    public ReportResponse copyReport(Long id, String username) {
        Report original = reportRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new RuntimeException("报表不存在"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Report copy = Report.builder()
                .name(original.getName() + "_副本")
                .description(original.getDescription())
                .type(original.getType())
                .config(original.getConfig())
                .sceneId(original.getSceneId())
                .status(Report.Status.DRAFT)
                .visibility(Report.Visibility.PRIVATE)
                .thumbnailKey(original.getThumbnailKey())
                .owner(user)
                .build();

        copy = reportRepository.save(copy);
        return ReportResponse.from(copy);
    }

    @Transactional
    public ReportResponse updateReportStatus(Long id, String status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报表不存在"));
        report.setStatus(Report.Status.valueOf(status));
        report = reportRepository.save(report);
        return ReportResponse.from(report);
    }

    /**
     * 从场景生成报表：以场景的sceneData作为报表config基础，关联sceneId
     */
    @Transactional
    public ReportResponse generateReportFromScene(Long sceneId, String username) {
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Report report = Report.builder()
                .name(scene.getName() + "_报表")
                .description(scene.getDescription())
                .type(Report.Type.DASHBOARD)
                .config(scene.getSceneData())
                .sceneId(scene.getId())
                .status(Report.Status.DRAFT)
                .visibility(Report.Visibility.PRIVATE)
                .thumbnailKey(scene.getThumbnailKey())
                .owner(owner)
                .build();

        report = reportRepository.save(report);
        return ReportResponse.from(report);
    }
}
