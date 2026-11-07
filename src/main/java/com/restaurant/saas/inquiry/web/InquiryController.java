package com.restaurant.saas.inquiry.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.inquiry.service.InquiryService;
import com.restaurant.saas.inquiry.service.InquiryService.InquiryUpdateRequest;
import com.restaurant.saas.inquiry.service.InquiryService.InquiryView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inquiry.read')")
    public PageResponse<InquiryView> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return inquiryService.list(status, search, page, size);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inquiry.manage')")
    public InquiryView update(
        @PathVariable Long id,
        @Valid @RequestBody InquiryUpdateRequest request) {
        return inquiryService.update(id, request);
    }
}
