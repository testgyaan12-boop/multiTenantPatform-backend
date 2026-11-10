package com.restaurant.saas.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.restaurant.saas.common.config.StorageProperties;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.storage.crypto.CredentialCryptoService;
import com.restaurant.saas.storage.entity.CloudinaryAccount;
import com.restaurant.saas.storage.repository.CloudinaryAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CloudinaryAccountService {

    private final CloudinaryAccountRepository accountRepository;
    private final CredentialCryptoService crypto;
    private final StorageProperties properties;

    public CloudinaryAccountService(CloudinaryAccountRepository accountRepository,
                                    CredentialCryptoService crypto,
                                    StorageProperties properties) {
        this.accountRepository = accountRepository;
        this.crypto = crypto;
        this.properties = properties;
    }

    public record AccountView(Long id, String label, String cloudName, String apiKeyMasked,
                              String uploadPreset, String folderPrefix, Integer priority, Boolean isActive,
                              BigDecimal monthlyCreditLimit, BigDecimal creditsUsed, BigDecimal usedPercent,
                              Long localUploadCount, Long localBytesUsed, Instant usageCheckedAt,
                              Instant lastUploadAt, String lastError, Instant lastErrorAt) {
    }

    public record AccountRequest(String label, String cloudName, String apiKey, String apiSecret,
                                 String uploadPreset, String folderPrefix, Integer priority, Boolean isActive,
                                 BigDecimal monthlyCreditLimit) {
    }

    public record Summary(Long totalAccounts, Long activeAccounts, Long exhaustedAccounts,
                          BigDecimal totalCreditLimit, BigDecimal totalCreditsUsed,
                          BigDecimal totalRemainingCredits, List<AccountView> lowOrExhausted) {
    }

    public List<AccountView> listAccounts() {
        return accountRepository.findAllByOrderByPriorityAscIdAsc().stream()
            .map(this::toView)
            .toList();
    }

    public AccountView getAccount(Long id) {
        CloudinaryAccount account = accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
        return toView(account);
    }

    @Transactional
    public AccountView createAccount(AccountRequest request) {
        if (accountRepository.findByCloudName(request.cloudName()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "cloud_name_exists",
                "Cloudinary cloud_name " + request.cloudName() + " already exists");
        }
        if (request.apiSecret() == null || request.apiSecret().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "api_secret_required", "api_secret is required");
        }
        CloudinaryAccount account = new CloudinaryAccount();
        account.setLabel(request.label());
        account.setCloudName(request.cloudName());
        account.setApiKey(request.apiKey());
        account.setApiSecretEnc(crypto.encrypt(request.apiSecret()));
        account.setUploadPreset(request.uploadPreset());
        account.setFolderPrefix(request.folderPrefix());
        account.setPriority(request.priority() == null ? 0 : request.priority());
        account.setIsActive(request.isActive() == null || request.isActive());
        account.setMonthlyCreditLimit(request.monthlyCreditLimit() == null
            ? new BigDecimal("25.00") : request.monthlyCreditLimit());
        account = accountRepository.save(account);
        return toView(account);
    }

    @Transactional
    public AccountView updateAccount(Long id, AccountRequest request) {
        CloudinaryAccount account = accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
        CloudinaryAccount duplicate = accountRepository.findByCloudName(request.cloudName()).orElse(null);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "cloud_name_exists",
                "Cloudinary cloud_name " + request.cloudName() + " already exists");
        }
        account.setLabel(request.label());
        account.setCloudName(request.cloudName());
        if (isRealApiKey(request.apiKey())) {
            account.setApiKey(request.apiKey());
        }
        if (request.apiSecret() != null && !request.apiSecret().isBlank()) {
            account.setApiSecretEnc(crypto.encrypt(request.apiSecret()));
        }
        account.setUploadPreset(request.uploadPreset());
        account.setFolderPrefix(request.folderPrefix());
        account.setPriority(request.priority() == null ? 0 : request.priority());
        account.setIsActive(request.isActive() == null || request.isActive());
        account.setMonthlyCreditLimit(request.monthlyCreditLimit() == null
            ? new BigDecimal("25.00") : request.monthlyCreditLimit());
        account = accountRepository.save(account);
        return toView(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        CloudinaryAccount account = accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
        account.softDelete();
        accountRepository.save(account);
    }

    @Transactional
    public AccountView setActive(Long id, boolean active) {
        CloudinaryAccount account = accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
        account.setIsActive(active);
        account = accountRepository.save(account);
        return toView(account);
    }

    public AccountView testAccount(Long id) {
        CloudinaryAccount account = accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
        try {
            Map usage = usage(account);
            refreshCached(account, usage);
            accountRepository.save(account);
            return toView(account);
        } catch (Exception ex) {
            account.setLastError("test_failed: " + ex.getMessage());
            account.setLastErrorAt(Instant.now());
            accountRepository.save(account);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "cloudinary_test_failed",
                "Cloudinary credentials test failed: " + ex.getMessage());
        }
    }

    public AccountView refreshUsage(Long id) {
        CloudinaryAccount account = accountRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
        try {
            Map usage = usage(account);
            refreshCached(account, usage);
            accountRepository.save(account);
            return toView(account);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "cloudinary_refresh_failed",
                "Failed to refresh usage: " + ex.getMessage());
        }
    }

    /**
     * Pick the active account with remaining credits (least used). Skips exhausted / errored accounts.
     * Returns null when no account can accept uploads.
     */
    public CloudinaryAccount selectAccount() {
        List<CloudinaryAccount> accounts = accountRepository.findByIsActiveTrueAndIsDeleteFalseOrderByPriorityAscIdAsc();
        if (accounts.isEmpty()) {
            return null;
        }

        Integer threshold = parseInt(properties.cloudinary("exhausted-threshold-percent"), 90);
        Instant now = Instant.now();
        Long refreshMinutes = parseLong(properties.cloudinary("usage-refresh-minutes"), 15L);

        CloudinaryAccount best = null;
        BigDecimal bestRemaining = null;

        for (CloudinaryAccount account : accounts) {
            // skip recently errored accounts for a short cool-down
            if (account.getLastError() != null && account.getLastErrorAt() != null
                && now.isBefore(account.getLastErrorAt().plusSeconds(300))) {
                continue;
            }

            boolean needsRefresh = account.getUsageCheckedAt() == null
                || now.isAfter(account.getUsageCheckedAt().plusSeconds(refreshMinutes * 60));
            Map usage = null;
            if (needsRefresh) {
                try {
                    usage = usage(account);
                } catch (Exception ignored) {
                    usage = null;
                }
            }
            if (needsRefresh && usage != null) {
                refreshCached(account, usage);
            }

            BigDecimal usedPercent = account.getUsedPercent() == null ? BigDecimal.ZERO : account.getUsedPercent();
            if (usedPercent.compareTo(new BigDecimal(threshold)) >= 0) {
                continue; // exhausted
            }

            BigDecimal limit = account.getMonthlyCreditLimit() == null
                ? new BigDecimal("25.00") : account.getMonthlyCreditLimit();
            BigDecimal used = account.getCreditsUsed() == null ? BigDecimal.ZERO : account.getCreditsUsed();
            BigDecimal remaining = limit.subtract(used);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (bestRemaining == null || remaining.compareTo(bestRemaining) > 0) {
                best = account;
                bestRemaining = remaining;
            }
        }

        if (best != null && needsRefresh(best, now, refreshMinutes)) {
            // already refreshed above inside loop when needed
        }
        return best;
    }

    @Transactional
    public void recordUpload(Long accountId, Long bytes) {
        CloudinaryAccount account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return;
        }
        account.setLocalUploadCount((account.getLocalUploadCount() == null ? 0 : account.getLocalUploadCount()) + 1);
        account.setLocalBytesUsed((account.getLocalBytesUsed() == null ? 0 : account.getLocalBytesUsed()) + bytes);
        account.setLastUploadAt(Instant.now());
        account.setLastError(null);
        account.setLastErrorAt(null);
        accountRepository.save(account);
    }

    public Summary summary() {
        List<CloudinaryAccount> accounts = accountRepository.findAllByOrderByPriorityAscIdAsc();
        Long total = (long) accounts.size();
        Long active = accounts.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count();
        Long exhausted = accounts.stream()
            .filter(a -> {
                BigDecimal up = a.getUsedPercent() == null ? BigDecimal.ZERO : a.getUsedPercent();
                Integer threshold = parseInt(properties.cloudinary("exhausted-threshold-percent"), 90);
                return up.compareTo(new BigDecimal(threshold)) >= 0;
            }).count();
        BigDecimal totalLimit = accounts.stream()
            .map(a -> a.getMonthlyCreditLimit() == null ? new BigDecimal("25.00") : a.getMonthlyCreditLimit())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUsed = accounts.stream()
            .map(a -> a.getCreditsUsed() == null ? BigDecimal.ZERO : a.getCreditsUsed())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalLimit.subtract(totalUsed);
        List<AccountView> lowOrExhausted = accounts.stream()
            .filter(a -> {
                BigDecimal up = a.getUsedPercent() == null ? BigDecimal.ZERO : a.getUsedPercent();
                Integer threshold = parseInt(properties.cloudinary("exhausted-threshold-percent"), 90);
                return up.compareTo(new BigDecimal(threshold)) >= 0;
            })
            .map(this::toView)
            .toList();
        return new Summary(total, active, exhausted, totalLimit, totalUsed, totalRemaining, lowOrExhausted);
    }

    @Transactional
    public void reorder(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer priority = Integer.valueOf(item.get("priority").toString());
            CloudinaryAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cloudinary account " + id + " not found"));
            account.setPriority(priority);
            accountRepository.save(account);
        }
    }

    // ---------- private ----------

    private Cloudinary buildCloudinary(CloudinaryAccount account) {
        String secret = crypto.decrypt(account.getApiSecretEnc());
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", account.getCloudName(),
            "api_key", account.getApiKey(),
            "api_secret", secret,
            "secure", true
        ));
    }

    private Map usage(CloudinaryAccount account) throws Exception {
        Cloudinary cloudinary = buildCloudinary(account);
        return cloudinary.api().usage(ObjectUtils.emptyMap());
    }

    private void refreshCached(CloudinaryAccount account, Map usage) {
        try {
            Map credits = asMap(usage.get("credits"));
            Number usageNum = asNumber(credits.get("usage"));
            Number limitNum = asNumber(credits.get("limit"));
            BigDecimal used = usageNum == null ? BigDecimal.ZERO : BigDecimal.valueOf(usageNum.doubleValue());
            BigDecimal limit = limitNum == null ? new BigDecimal("25.00")
                : BigDecimal.valueOf(limitNum.doubleValue());
            account.setCreditsUsed(used);
            account.setMonthlyCreditLimit(limit);
            BigDecimal usedPercent = limit.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO : used.divide(limit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
            account.setUsedPercent(usedPercent.setScale(2, RoundingMode.HALF_UP));
            account.setUsageCheckedAt(Instant.now());
            account.setLastError(null);
            account.setLastErrorAt(null);
        } catch (Exception ignored) {
            // keep previous cached values
        }
    }

    private AccountView toView(CloudinaryAccount a) {
        String apiKeyMasked = mask(a.getApiKey(), 4);
        return new AccountView(
            a.getId(), a.getLabel(), a.getCloudName(), apiKeyMasked,
            a.getUploadPreset(), a.getFolderPrefix(), a.getPriority(), a.getIsActive(),
            a.getMonthlyCreditLimit(), a.getCreditsUsed(), a.getUsedPercent(),
            a.getLocalUploadCount(), a.getLocalBytesUsed(), a.getUsageCheckedAt(),
            a.getLastUploadAt(), a.getLastError(), a.getLastErrorAt());
    }

    private static boolean isRealApiKey(String value) {
        return value != null && !value.isBlank() && !value.contains("•");
    }

    private static String mask(String value, int show) {        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= show) {
            return value;
        }
        return "••••••••" + value.substring(value.length() - show);
    }

    private static Map asMap(Object o) {
        if (o instanceof Map) {
            return (Map) o;
        }
        return Map.of();
    }

    private static Number asNumber(Object o) {
        if (o instanceof Number) {
            return (Number) o;
        }
        return null;
    }

    private static Integer parseInt(String value, int def) {
        try {
            return value == null ? def : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static Long parseLong(String value, long def) {
        try {
            return value == null ? def : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static boolean needsRefresh(CloudinaryAccount a, Instant now, Long refreshMinutes) {
        return a.getUsageCheckedAt() == null || now.isAfter(a.getUsageCheckedAt().plusSeconds(refreshMinutes * 60));
    }
}
