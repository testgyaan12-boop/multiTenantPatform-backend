package com.restaurant.saas.menu.web;

import com.restaurant.saas.menu.dto.PublicMenuResponse;
import com.restaurant.saas.menu.service.PublicMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/menu")
public class PublicMenuController {

    private final PublicMenuService publicMenuService;

    public PublicMenuController(PublicMenuService publicMenuService) {
        this.publicMenuService = publicMenuService;
    }

    @GetMapping("/{tableCode}")
    public PublicMenuResponse menu(@PathVariable String tableCode) {
        return publicMenuService.menuForTable(tableCode);
    }
}
