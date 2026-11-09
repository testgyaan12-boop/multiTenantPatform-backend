package com.restaurant.saas.menu.web;

import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.storage.service.StorageService;
import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Controller
@RequestMapping("/api/v1/menu")
public class MenuImageController {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_WIDTH = 1200;

    private final MenuItemRepository itemRepository;
    private final StorageService storageService;

    public MenuImageController(MenuItemRepository itemRepository, StorageService storageService) {
        this.itemRepository = itemRepository;
        this.storageService = storageService;
    }

    @PostConstruct
    void init() {
        // No local dirs required when using the configured storage provider.
    }

    public record ImageUploadResponse(String url) {
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('menu.manage')")
    @ResponseBody
    public ImageUploadResponse uploadMenuImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "empty_file", "No file was uploaded");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_file_type",
                "Image must be a PNG, JPEG, WEBP or GIF");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "file_too_large", "Image must be 5 MB or smaller");
        }

        Long tenantId = TenantContext.requireTenant();
        BufferedImage compressed = compress(file.getInputStream());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(compressed, "jpg", bos);
        String original = StringUtils.cleanPath(
            file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename());

        StorageService.UploadResult result = storageService.upload(
            new java.io.ByteArrayInputStream(bos.toByteArray()),
            original,
            "image/jpeg",
            bos.size(),
            "menu-items",
            tenantId);
        return new ImageUploadResponse(result.url());
    }

    @PostMapping(value = "/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('menu.manage')")
    @ResponseBody
    public ImageUploadResponse attachMenuImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        String url = uploadMenuImage(file).url();
        MenuItem item = itemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Menu item " + id + " not found"));
        item.setImageUrl(url);
        itemRepository.save(item);
        return new ImageUploadResponse(url);
    }

    private BufferedImage compress(InputStream input) throws IOException {
        BufferedImage source = ImageIO.read(input);
        if (source == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_image", "Unable to read image");
        }
        int width = source.getWidth();
        int height = source.getHeight();
        if (width > MAX_WIDTH) {
            height = (int) Math.round((double) height * MAX_WIDTH / width);
            width = MAX_WIDTH;
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }
}
