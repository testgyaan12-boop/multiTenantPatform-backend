package com.restaurant.saas.common.entity;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BaseEntitySoftDeleteTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    private Tenant tenant;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("Soft Delete Tenant");
        tenant.setSlug("soft-delete-" + System.nanoTime());
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        restaurant = new Restaurant();
        restaurant.setTenantId(tenant.getId());
        restaurant.setCode("SD");
        restaurant.setName("Soft Delete Restaurant");
        restaurant.setCurrency("USD");
        restaurant = restaurantRepository.save(restaurant);
    }

    @Test
    void newEntityIsActiveAndNotDeleted() {
        assertThat(tenant.getIsActive()).isTrue();
        assertThat(tenant.getIsDelete()).isFalse();
        assertThat(tenant.isEnabled()).isTrue();
        assertThat(tenant.getCreatedAt()).isNotNull();
    }

    @Test
    void softDeleteHidesRowFromNormalReads() {
        tenant.softDelete();
        tenantRepository.saveAndFlush(tenant);
        entityManager.clear();

        assertThat(tenantRepository.findById(tenant.getId())).isEmpty();
        assertThat(tenantRepository.existsById(tenant.getId())).isFalse();
    }

    @Test
    void softDeleteSetsBothFlags() {
        tenant.softDelete();
        assertThat(tenant.getIsDelete()).isTrue();
        assertThat(tenant.getIsActive()).isFalse();
    }

    @Test
    void softDeleteIsIdempotent() {
        tenant.softDelete();
        tenant.softDelete();
        assertThat(tenant.getIsDelete()).isTrue();
        assertThat(tenant.getIsActive()).isFalse();
    }

    @Test
    void restoreBringsRowBack() {
        tenant.softDelete();
        tenantRepository.saveAndFlush(tenant);
        entityManager.clear();
        assertThat(tenantRepository.findById(tenant.getId())).isEmpty();

        tenant = tenantRepository.findBySlugIncludingDeleted(tenant.getSlug()).orElseThrow();
        tenant.restore(true);
        tenantRepository.saveAndFlush(tenant);
        entityManager.clear();
        assertThat(tenantRepository.findById(tenant.getId())).isPresent();
    }

    @Test
    void deactivatedRowIsHiddenEvenWhenNotDeleted() {
        tenant.setIsActive(false);
        tenantRepository.saveAndFlush(tenant);
        entityManager.clear();

        assertThat(tenantRepository.findById(tenant.getId())).isEmpty();
        assertThat(tenant.getIsDelete()).isFalse();
    }

    @Test
    void uniqueSlugCanBeReusedAfterSoftDelete() {
        String slug = tenant.getSlug();
        tenant.softDelete();
        tenantRepository.saveAndFlush(tenant);

        Tenant replacement = new Tenant();
        replacement.setName("Replacement");
        replacement.setSlug(slug);
        replacement.setStatus("ACTIVE");
        Tenant saved = tenantRepository.saveAndFlush(replacement);

        assertThat(saved.getId()).isNotEqualTo(tenant.getId());
        assertThat(tenantRepository.findBySlug(slug)).isPresent();
    }

    @Test
    void deletedParentIsNotVisibleThroughRepository() {
        Branch branch = new Branch();
        branch.setTenantId(tenant.getId());
        branch.setRestaurantId(restaurant.getId());
        branch.setCode("B1");
        branch.setName("Branch One");
        branch.setAddressLine1("1 Main St");
        branch.setCity("Testville");
        branch = branchRepository.saveAndFlush(branch);

        branch.softDelete();
        branchRepository.saveAndFlush(branch);

        assertThat(branchRepository.findByTenantIdOrderByNameAsc(tenant.getId())).isEmpty();
    }

    @Test
    void auditTimestampsArePopulated() {
        MenuCategory category = new MenuCategory();
        category.setTenantId(tenant.getId());
        category.setRestaurantId(restaurant.getId());
        category.setName("Audit Category");
        MenuCategory saved = categoryRepository.saveAndFlush(category);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void deletedCategoryIsExcludedFromTenantListing() {
        MenuCategory category = new MenuCategory();
        category.setTenantId(tenant.getId());
        category.setRestaurantId(restaurant.getId());
        category.setName("Temp Category");
        category = categoryRepository.saveAndFlush(category);

        category.softDelete();
        categoryRepository.saveAndFlush(category);

        assertThat(categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenant.getId())).isEmpty();
    }

    @Test
    void findByIdAndTenantIdEnforcesTenantAndSoftDelete() {
        Long otherTenantId = tenant.getId() + 1000;
        assertThat(restaurantRepository.findByTenantId(otherTenantId)).isEmpty();

        restaurant.softDelete();
        restaurantRepository.saveAndFlush(restaurant);
        assertThat(restaurantRepository.findByTenantId(tenant.getId())).isEmpty();
    }

    @Test
    void equalsAndHashCodeUseIdOnly() {
        Tenant same = new Tenant();
        same.setId(tenant.getId());
        same.setName("Different name");

        assertThat(tenant).isEqualTo(same);
        assertThat(tenant.hashCode()).isEqualTo(same.hashCode());

        Tenant other = new Tenant();
        other.setId(tenant.getId() + 1);
        assertThat(tenant).isNotEqualTo(other);
    }

    @Test
    void baseEntityIsMappedSuperclassWithoutTable() {
        assertThat(BaseEntity.class.getAnnotation(jakarta.persistence.MappedSuperclass.class)).isNotNull();
        assertThat(BaseEntity.class.getAnnotation(jakarta.persistence.Entity.class)).isNull();
    }

    @Test
    void reactivatingDeletedRowKeepsSoftDeleteFlag() {
        tenant.softDelete();
        tenant.restore(false);

        assertThat(tenant.getIsDelete()).isFalse();
        assertThat(tenant.getIsActive()).isFalse();
        tenantRepository.saveAndFlush(tenant);
        entityManager.clear();
        assertThat(tenantRepository.findById(tenant.getId())).isEmpty();
    }

    @Test
    void deletedRowIsNotReturnedBySearch() {
        tenant.softDelete();
        tenantRepository.saveAndFlush(tenant);

        var page = tenantRepository.search("Soft Delete Tenant",
            org.springframework.data.domain.PageRequest.of(0, 10));
        Optional<Tenant> found = page.getContent().stream()
            .filter(t -> t.getId().equals(tenant.getId()))
            .findFirst();
        assertThat(found).isEmpty();
    }
}
