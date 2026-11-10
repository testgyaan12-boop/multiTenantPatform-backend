package com.restaurant.saas.storage.service;

import com.restaurant.saas.common.config.StorageProperties;
import com.restaurant.saas.storage.entity.StoredFile;
import com.restaurant.saas.storage.repository.StoredFileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StorageReportService {

    private static final BigDecimal DEFAULT_CREDITS_PER_ACCOUNT = new BigDecimal("25.00");
    private static final long DEFAULT_CAPACITY_PER_ACCOUNT = 26_843_545_600L; // 25 GiB

    private final StoredFileRepository fileRepository;
    private final CloudinaryAccountService accountService;
    private final StorageService storageService;
    private final StorageProperties properties;

    public StorageReportService(StoredFileRepository fileRepository,
                                CloudinaryAccountService accountService,
                                StorageService storageService,
                                StorageProperties properties) {
        this.fileRepository = fileRepository;
        this.accountService = accountService;
        this.storageService = storageService;
        this.properties = properties;
    }

    public record Overview(long totalFiles, long totalBytes,
                           long monthlyFiles, long monthlyBytes,
                           long previousMonthFiles, long previousMonthBytes,
                           long allocatedBytes, BigDecimal usedPercent,
                           long totalAccounts, long activeAccounts, long exhaustedAccounts,
                           BigDecimal creditsUsed, BigDecimal creditsLimit, BigDecimal creditsRemaining,
                           String planLabel, BigDecimal monthlyCostUsd, boolean freeTier,
                           int warningThresholdPercent, int criticalThresholdPercent,
                           String provider) {
    }

    public record GrowthPoint(String date, long bytesAdded, long bytesTotal, long files) {
    }

    public record FileTypeStat(String type, long count, long bytes, BigDecimal percent) {
    }

    public record FileView(long id, Long tenantId, String name, String contentType, long sizeBytes,
                           String publicUrl, String provider, String category, Instant createdAt,
                           String status) {
    }

    public record PagedFiles(List<FileView> content, int page, int size, long totalElements, int totalPages) {
    }

    public Overview overview(Long tenantId) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        long totalFiles = fileRepository.countAll(tenantId);
        long totalBytes = fileRepository.sumBytes(tenantId);

        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant nextMonthStart = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant();
        Instant prevMonthStart = today.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant();

        long monthlyFiles = fileRepository.countBetween(tenantId, monthStart, nextMonthStart);
        long monthlyBytes = fileRepository.sumBytesBetween(tenantId, monthStart, nextMonthStart);
        long prevMonthFiles = fileRepository.countBetween(tenantId, prevMonthStart, monthStart);
        long prevMonthBytes = fileRepository.sumBytesBetween(tenantId, prevMonthStart, monthStart);

        CloudinaryAccountService.Summary accountSummary = accountService.summary();
        long activeAccounts = accountSummary.activeAccounts() == null ? 0 : accountSummary.activeAccounts();
        long totalAccounts = accountSummary.totalAccounts() == null ? 0 : accountSummary.totalAccounts();
        long exhaustedAccounts = accountSummary.exhaustedAccounts() == null ? 0 : accountSummary.exhaustedAccounts();

        long perAccount = parseLong(properties.cloudinary("capacity-per-account-bytes"), DEFAULT_CAPACITY_PER_ACCOUNT);
        long allocatedBytes = Math.max(1, activeAccounts) * perAccount;

        BigDecimal usedPercent = allocatedBytes <= 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(totalBytes)
                .divide(BigDecimal.valueOf(allocatedBytes), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal creditsUsed = accountSummary.totalCreditsUsed() == null
            ? BigDecimal.ZERO : accountSummary.totalCreditsUsed();
        BigDecimal creditsLimit = accountSummary.totalCreditLimit() == null
            ? BigDecimal.ZERO : accountSummary.totalCreditLimit();
        BigDecimal creditsRemaining = accountSummary.totalRemainingCredits() == null
            ? BigDecimal.ZERO : accountSummary.totalRemainingCredits();

        BigDecimal freeAllowance = DEFAULT_CREDITS_PER_ACCOUNT
            .multiply(BigDecimal.valueOf(Math.max(activeAccounts, 1)));
        boolean freeTier = exhaustedAccounts == 0
            && creditsLimit.compareTo(freeAllowance) <= 0;

        String planLabel;
        if (totalAccounts == 0) {
            planLabel = "No account";
        } else if (exhaustedAccounts > 0) {
            planLabel = "At limit";
        } else if (freeTier) {
            planLabel = "Free tier";
        } else {
            planLabel = "Custom limits";
        }
        BigDecimal monthlyCostUsd = freeTier ? BigDecimal.ZERO : null;

        int warning = parseInt(properties.cloudinary("warning-threshold-percent"), 75);
        int critical = parseInt(properties.cloudinary("critical-threshold-percent"), 90);
        String provider = properties.getProvider() == null ? "local" : properties.getProvider();

        return new Overview(totalFiles, totalBytes, monthlyFiles, monthlyBytes,
            prevMonthFiles, prevMonthBytes, allocatedBytes, usedPercent,
            totalAccounts, activeAccounts, exhaustedAccounts,
            creditsUsed, creditsLimit, creditsRemaining,
            planLabel, monthlyCostUsd, freeTier, warning, critical, provider);
    }

    public List<GrowthPoint> growth(Long tenantId, Integer days, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        LocalDate start;
        LocalDate end;
        if (from != null && to != null) {
            start = from;
            end = to;
        } else {
            int span = days == null || days <= 0 ? 30 : days;
            end = today;
            start = today.minusDays(span - 1L);
        }
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        Instant fromInstant = start.atStartOfDay(zone).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(zone).toInstant();

        long baseline = fileRepository.sumBytesBefore(tenantId, fromInstant);
        List<Object[]> rows = fileRepository.dailyGrowth(tenantId, fromInstant, toInstant);

        Map<String, long[]> byDate = new HashMap<>();
        for (Object[] row : rows) {
            String key = toDateString(row[0]);
            long count = toLong(row[1]);
            long bytes = toLong(row[2]);
            byDate.put(key, new long[]{bytes, count});
        }

        List<GrowthPoint> points = new ArrayList<>();
        long running = baseline;
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            long[] values = byDate.getOrDefault(cursor.toString(), new long[]{0L, 0L});
            running += values[0];
            points.add(new GrowthPoint(cursor.toString(), values[0], running, values[1]));
        }
        return points;
    }

    public List<FileTypeStat> fileTypes(Long tenantId) {
        List<Object[]> rows = fileRepository.fileTypeDistribution(tenantId);
        Map<String, long[]> byType = new HashMap<>();
        for (Object[] row : rows) {
            String type = String.valueOf(row[0]);
            byType.put(type, new long[]{toLong(row[1]), toLong(row[2])});
        }
        long totalCount = byType.values().stream().mapToLong(v -> v[0]).sum();
        List<FileTypeStat> result = new ArrayList<>();
        for (String type : List.of("IMAGE", "DOCUMENT", "VIDEO", "OTHER")) {
            long[] values = byType.getOrDefault(type, new long[]{0L, 0L});
            BigDecimal percent = totalCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(values[0])
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(1, RoundingMode.HALF_UP);
            result.add(new FileTypeStat(type, values[0], values[1], percent));
        }
        return result;
    }

    public List<FileView> recent(Long tenantId, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        List<StoredFile> files = fileRepository.findRecent(tenantId, PageRequest.of(0, size));
        return files.stream().map(this::toView).toList();
    }

    public PagedFiles files(Long tenantId, int page, int size, String search, String type) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedType = type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)
            ? null : type.trim().toUpperCase();

        Page<Object[]> rows = fileRepository.pageFiles(tenantId, normalizedSearch, normalizedType,
            PageRequest.of(safePage, safeSize));
        List<FileView> content = rows.getContent().stream()
            .map(this::toView)
            .toList();
        return new PagedFiles(content, safePage, safeSize, rows.getTotalElements(), rows.getTotalPages());
    }

    public FileView upload(InputStream input, String originalName, String contentType, long sizeBytes, Long tenantId)
        throws IOException {
        FileStorage.UploadResult result = storageService.upload(input, originalName, contentType, sizeBytes,
            "platform", tenantId);
        StoredFile stored = fileRepository.findByStorageKey(result.storageKey()).orElse(null);
        if (stored != null) {
            return toView(stored);
        }
        return new FileView(0L, tenantId, originalName,
            contentType == null ? "application/octet-stream" : contentType, sizeBytes,
            result.url(), result.cloudinaryAccountId() != null ? "CLOUDINARY" : "LOCAL",
            category(contentType), Instant.now(), result.url() != null ? "READY" : "PENDING");
    }

    // ---------- mapping / helpers ----------

    private FileView toView(StoredFile f) {
        return new FileView(
            f.getId() == null ? 0L : f.getId(),
            f.getTenantId(),
            f.getOriginalName(),
            f.getContentType(),
            f.getSizeBytes() == null ? 0L : f.getSizeBytes(),
            f.getPublicUrl(),
            f.getProvider(),
            category(f.getContentType()),
            f.getCreatedAt(),
            f.getPublicUrl() != null ? "READY" : "PENDING");
    }

    private FileView toView(Object[] row) {
        long id = toLong(row[0]);
        Long tenantId = row[1] == null ? null : toLong(row[1]);
        String name = row[2] == null ? "" : String.valueOf(row[2]);
        String contentType = row[3] == null ? "application/octet-stream" : String.valueOf(row[3]);
        long sizeBytes = toLong(row[4]);
        String publicUrl = row[5] == null ? null : String.valueOf(row[5]);
        String provider = row[6] == null ? "LOCAL" : String.valueOf(row[6]);
        Instant createdAt = toInstant(row[7]);
        String category = row[8] == null ? category(contentType) : String.valueOf(row[8]);
        return new FileView(id, tenantId, name, contentType, sizeBytes, publicUrl, provider,
            category, createdAt, publicUrl != null ? "READY" : "PENDING");
    }

    private static String category(String contentType) {
        if (contentType == null) {
            return "OTHER";
        }
        String type = contentType.toLowerCase();
        if (type.startsWith("image/")) {
            return "IMAGE";
        }
        if (type.startsWith("video/")) {
            return "VIDEO";
        }
        if (type.equals("application/pdf") || type.contains("word") || type.contains("excel")
            || type.contains("sheet") || type.contains("presentation") || type.startsWith("text/")) {
            return "DOCUMENT";
        }
        return "OTHER";
    }

    private static String toDateString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        String text = value.toString();
        return text.length() > 10 ? text.substring(0, 10) : text;
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static int parseInt(String value, int def) {
        try {
            return value == null ? def : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static long parseLong(String value, long def) {
        try {
            return value == null ? def : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
