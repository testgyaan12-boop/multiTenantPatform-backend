package com.restaurant.saas.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.saas.identity.dto.AuthResponse;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MenuAdminApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long categoryId;
    private Long emptyCategoryId;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();

        Tenant tenant = new Tenant();
        tenant.setName("Menu Admin Tenant");
        tenant.setSlug("menu-admin-" + suffix);
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        Restaurant restaurant = new Restaurant();
        restaurant.setTenantId(tenant.getId());
        restaurant.setCode("MADT");
        restaurant.setName("Menu Admin Restaurant");
        restaurant.setCurrency("USD");
        restaurant.setTaxRate(new BigDecimal("0.1000"));
        restaurant = restaurantRepository.save(restaurant);

        MenuCategory category = new MenuCategory();
        category.setTenantId(tenant.getId());
        category.setRestaurantId(restaurant.getId());
        category.setName("Admin Mains");
        category = categoryRepository.save(category);
        categoryId = category.getId();

        MenuCategory emptyCategory = new MenuCategory();
        emptyCategory.setTenantId(tenant.getId());
        emptyCategory.setRestaurantId(restaurant.getId());
        emptyCategory.setName("Admin Empty");
        emptyCategory = categoryRepository.save(emptyCategory);
        emptyCategoryId = emptyCategory.getId();

        MenuItem item = new MenuItem();
        item.setTenantId(tenant.getId());
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(categoryId);
        item.setSku("MADT-UNAVAIL");
        item.setName("Unavailable Toast");
        item.setPrice(new BigDecimal("5.00"));
        item.setCurrency("USD");
        item.setIsAvailable(false);
        menuItemRepository.save(item);

        Role ownerRole = roleRepository.findSystemRoleWithPermissions("TENANT_OWNER").orElseThrow();
        User user = new User();
        user.setEmail("menu-admin-" + suffix + "@example.com");
        user.setTenantId(tenant.getId());
        user.setFirstName("Menu");
        user.setLastName("Admin");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setStatus("ACTIVE");
        user.getRoles().add(ownerRole);
        userRepository.save(user);

        token = loginToken(user.getEmail());
    }

    @Test
    void ownerSearchFindsUnavailableItemsByNameOrSku() throws Exception {
        mockMvc.perform(get("/api/v1/menu/items")
                .param("search", "MADT-UNAVAIL")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].sku").value("MADT-UNAVAIL"))
            .andExpect(jsonPath("$[0].updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/menu/items")
                .param("search", "unavailable toast")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Unavailable Toast"));
    }

    @Test
    void duplicateSkuIsRejectedAndNewItemReturnsUpdatedAt() throws Exception {
        mockMvc.perform(post("/api/v1/menu/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId":%d,"sku":"MADT-UNAVAIL","name":"Copycat","price":4.00}
                    """.formatted(categoryId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("sku_taken"));

        mockMvc.perform(post("/api/v1/menu/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId":%d,"sku":"MADT-NEW-1","name":"Fresh Bowl","price":7.50}
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("MADT-NEW-1"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void categoryLifecycleEnforcesBusinessRules() throws Exception {
        mockMvc.perform(post("/api/v1/menu/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Admin Mains"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("category_exists"));

        MvcResult created = mockMvc.perform(post("/api/v1/menu/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Admin Sides API","description":"Crispy things"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Admin Sides API"))
            .andReturn();
        long newId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/menu/categories/" + newId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Admin Sides Renamed"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Admin Sides Renamed"));

        mockMvc.perform(put("/api/v1/menu/categories/" + newId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Admin Mains"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("category_exists"));

        mockMvc.perform(delete("/api/v1/menu/categories/" + categoryId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("category_in_use"));

        mockMvc.perform(delete("/api/v1/menu/categories/" + newId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/menu/categories")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/v1/menu/categories/" + emptyCategoryId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/menu/categories")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    private String loginToken(String email) {
        try {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","password":"Password123!"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
            return objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class).accessToken();
        } catch (Exception ex) {
            throw new IllegalStateException("login failed", ex);
        }
    }
}
