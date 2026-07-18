package com.manager3d.service;

import com.manager3d.dto.response.OperationLogResponse;
import com.manager3d.entity.OperationLog;
import com.manager3d.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository logRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(OperationLog logEntry) {
        try {
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    @Transactional
    public void log(OperationLog logEntry) {
        try {
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<OperationLogResponse> list(String module, String action, Long userId,
                                           String status, LocalDateTime startDate, LocalDateTime endDate,
                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        OperationLog.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = OperationLog.Status.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // 无效状态返回空结果
                return Page.<OperationLog>empty(pageable).map(OperationLogResponse::from);
            }
        }
        Page<OperationLog> logPage = logRepository.search(module, action, userId, statusEnum, startDate, endDate, pageable);
        return logPage.map(OperationLogResponse::from);
    }

    @Transactional
    public int cleanup(int days) {
        if (days <= 0) {
            throw new RuntimeException("清理天数必须大于0");
        }
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        long deleted = logRepository.deleteByCreatedAtBefore(before);
        log.info("清理 {} 天前操作日志，共删除 {} 条", days, deleted);
        return (int) deleted;
    }
}
