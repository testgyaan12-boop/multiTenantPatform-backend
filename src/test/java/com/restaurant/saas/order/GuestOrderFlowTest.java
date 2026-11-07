package com.restaurant.saas.order;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestOrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private DiningTableRepository tableRepository;

    private String qrToken;
    private Long itemId;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("Flow Tenant");
        tenant.setSlug("flow-" + System.nanoTime());
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        Restaurant restaurant = new Restaurant();
        restaurant.setTenantId(tenant.getId());
        restaurant.setCode("FLOW");
        restaurant.setName("Flow Restaurant");
        restaurant.setCurrency("USD");
        restaurant.setTaxRate(new BigDecimal("0.1000"));
        restaurant = restaurantRepository.save(restaurant);

        Branch branch = new Branch();
        branch.setTenantId(tenant.getId());
        branch.setRestaurantId(restaurant.getId());
        branch.setCode("F1");
        branch.setName("Flow Branch");
        branch.setAddressLine1("1 Flow St");
        branch.setCity("Flowtown");
        branch = branchRepository.save(branch);

        MenuCategory category = new MenuCategory();
        category.setTenantId(tenant.getId());
        category.setRestaurantId(restaurant.getId());
        category.setName("Flow Mains");
        category = categoryRepository.save(category);

        MenuItem item = new MenuItem();
        item.setTenantId(tenant.getId());
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(category.getId());
        item.setSku("FLOW-1");
        item.setName("Flow Burger");
        item.setPrice(new BigDecimal("10.00"));
        item.setCurrency("USD");
        item = menuItemRepository.save(item);
        itemId = item.getId();

        DiningTable table = new DiningTable();
        table.setTenantId(tenant.getId());
        table.setBranchId(branch.getId());
        table.setTableNumber("T1");
        table = tableRepository.save(table);
        qrToken = table.getQrToken();
    }

    @Test
    void guestCanOrderAndPay() throws Exception {
        String session = mockMvc.perform(post("/api/v1/public/guest-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableCode\":\"" + qrToken + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String sessionToken = objectMapper.readTree(session).get("sessionToken").asText();

        mockMvc.perform(get("/api/v1/public/menu/" + qrToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantName").value("Flow Restaurant"))
            .andExpect(jsonPath("$.categories[0].items[0].name").value("Flow Burger"));

        mockMvc.perform(post("/api/v1/public/cart/items")
                .header("X-Guest-Session", sessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"menuItemId\":" + itemId + ",\"quantity\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotal").value(20.00))
            .andExpect(jsonPath("$.taxTotal").value(2.00))
            .andExpect(jsonPath("$.grandTotal").value(22.00));

        String order = mockMvc.perform(post("/api/v1/public/orders")
                .header("X-Guest-Session", sessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Flow Guest\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.grandTotal").value(22.00))
            .andReturn().getResponse().getContentAsString();
        String publicToken = objectMapper.readTree(order).get("publicToken").asText();

        mockMvc.perform(post("/api/v1/public/orders/" + publicToken + "/pay")
                .header("Idempotency-Key", "flow-" + publicToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"card\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/v1/public/orders/" + publicToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("PAID"))
            .andExpect(jsonPath("$.items[0].name").value("Flow Burger"));
    }

    @Test
    void emptyCartCheckoutIsRejected() throws Exception {
        String session = mockMvc.perform(post("/api/v1/public/guest-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableCode\":\"" + qrToken + "\"}"))
            .andReturn().getResponse().getContentAsString();
        String sessionToken = objectMapper.readTree(session).get("sessionToken").asText();

        mockMvc.perform(post("/api/v1/public/orders")
                .header("X-Guest-Session", sessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Empty Guest\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void invalidTableCodeIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/public/guest-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableCode\":\"not-a-real-code\"}"))
            .andExpect(status().isNotFound());
    }
}
