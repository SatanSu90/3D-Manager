package com.manager3d.aspect;

import com.manager3d.entity.OperationLog;
import com.manager3d.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    // 模块映射: URL前缀 -> 模块名
    private static final Map<String, String> MODULE_MAP = new HashMap<>();
    static {
        MODULE_MAP.put("/api/auth", "auth");
        MODULE_MAP.put("/api/users", "user");
        MODULE_MAP.put("/api/roles", "role");
        MODULE_MAP.put("/api/departments", "department");
        MODULE_MAP.put("/api/models", "model");
        MODULE_MAP.put("/api/scenes", "scene");
        MODULE_MAP.put("/api/datasources", "datasource");
        MODULE_MAP.put("/api/categories", "category");
        MODULE_MAP.put("/api/tags", "tag");
        MODULE_MAP.put("/api/indicators", "indicator");
        MODULE_MAP.put("/api/analysis", "analysis");
        MODULE_MAP.put("/api/system", "system");
    }

    // 拦截所有controller方法
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    // 只记录写操作 POST/PUT/DELETE/PATCH
    @Pointcut("(execution(* *.*(..)) && (@annotation(org.springframework.web.bind.annotation.PostMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PatchMapping)))")
    public void writeOperationPointcut() {}

    @Around("controllerPointcut() && writeOperationPointcut()")
    public Object aroundWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog.Status status = OperationLog.Status.SUCCESS;
        String errorMessage = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = OperationLog.Status.FAILED;
            errorMessage = e.getMessage();
            if (errorMessage == null) errorMessage = e.getClass().getSimpleName();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                recordLog(joinPoint, status, errorMessage, (int) duration);
            } catch (Exception ex) {
                log.warn("记录操作日志失败: {}", ex.getMessage());
            }
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint, OperationLog.Status status,
                           String errorMessage, int durationMs) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest request = attrs.getRequest();

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 跳过 /api/datasources/test 这类纯测试请求也可以记录，这里保留记录
        String module = resolveModule(uri);
        String action = resolveAction(method, uri);
        String targetType = resolveTargetType(uri);
        String targetId = resolveTargetId(uri);

        // 当前用户
        Long userId = null;
        String username = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
            // 通过 username 推断 userId：需要查询数据库，但为了避免循环依赖，这里只记录 username
            // userId 留空，可由后续通过 username 关联
        }

        String description = buildDescription(method, module, action, targetId);

        OperationLog logEntry = OperationLog.builder()
                .userId(userId)
                .username(username)
                .module(module)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .requestUrl(uri)
                .requestMethod(method)
                .ipAddress(getClientIp(request))
                .status(status)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();

        operationLogService.logAsync(logEntry);
    }

    private String resolveModule(String uri) {
        for (Map.Entry<String, String> entry : MODULE_MAP.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "unknown";
    }

    private String resolveAction(String method, String uri) {
        // 特殊处理: 登录/登出
        if (uri.contains("/auth/login")) return "login";
        if (uri.contains("/auth/logout")) return "logout";
        if (uri.contains("/auth/register")) return "register";

        // 特殊处理: 包含明确动作词
        if (uri.contains("/test")) return "test";
        if (uri.contains("/query")) return "query";
        if (uri.contains("/assign")) return "assign";
        if (uri.contains("/status")) return "updateStatus";
        if (uri.contains("/role") && (uri.contains("/users/") || uri.matches(".*/\\d+/users/\\d+"))) {
            return method.equals("DELETE") ? "removeRole" : "assignRole";
        }
        if (uri.matches(".*/\\d+/users(/.*)?")) {
            return method.equals("DELETE") ? "removeUser" : "assignUser";
        }
        if (uri.contains("/cleanup")) return "cleanup";
        if (uri.contains("/batch")) return "batchUpdate";

        return switch (method) {
            case "POST" -> "create";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> "operation";
        };
    }

    private String resolveTargetType(String uri) {
        if (uri.startsWith("/api/users")) return "user";
        if (uri.startsWith("/api/roles")) return "role";
        if (uri.startsWith("/api/departments")) return "department";
        if (uri.startsWith("/api/models")) return "model";
        if (uri.startsWith("/api/scenes")) return "scene";
        if (uri.startsWith("/api/datasources")) return "datasource";
        if (uri.startsWith("/api/categories")) return "category";
        if (uri.startsWith("/api/tags")) return "tag";
        if (uri.startsWith("/api/indicators")) return "indicator";
        if (uri.startsWith("/api/analysis")) return "analysis";
        if (uri.startsWith("/api/system/config")) return "system_config";
        if (uri.startsWith("/api/system/logs")) return "operation_log";
        return null;
    }

    private String resolveTargetId(String uri) {
        // 提取 URL 中的数字 ID
        Pattern pattern = Pattern.compile("/(\\d+)(?:/|$)");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String buildDescription(String method, String module, String action, String targetId) {
        StringBuilder sb = new StringBuilder();
        sb.append(action);
        if (module != null) sb.append(" ").append(module);
        if (targetId != null) sb.append(" #").append(targetId);
        return sb.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
