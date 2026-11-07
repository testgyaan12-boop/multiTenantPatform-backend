package com.restaurant.saas.common.seed;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import com.restaurant.saas.employee.entity.Employee;
import com.restaurant.saas.employee.entity.StaffTask;
import com.restaurant.saas.employee.entity.TableAssignment;
import com.restaurant.saas.employee.repository.EmployeeRepository;
import com.restaurant.saas.employee.repository.StaffTaskRepository;
import com.restaurant.saas.employee.repository.TableAssignmentRepository;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemVariant;
import com.restaurant.saas.menu.entity.Modifier;
import com.restaurant.saas.menu.entity.ModifierGroup;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.menu.repository.MenuItemVariantRepository;
import com.restaurant.saas.menu.repository.ModifierGroupRepository;
import com.restaurant.saas.menu.repository.ModifierRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.subscription.entity.Subscription;
import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import com.restaurant.saas.subscription.repository.SubscriptionPlanRepository;
import com.restaurant.saas.subscription.repository.SubscriptionRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemVariantRepository variantRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final DiningTableRepository tableRepository;
    private final EmployeeRepository employeeRepository;
    private final StaffTaskRepository taskRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public DevDataSeeder(
        UserRepository userRepository,
        RoleRepository roleRepository,
        TenantRepository tenantRepository,
        RestaurantRepository restaurantRepository,
        BranchRepository branchRepository,
        MenuCategoryRepository categoryRepository,
        MenuItemRepository menuItemRepository,
        MenuItemVariantRepository variantRepository,
        ModifierGroupRepository modifierGroupRepository,
        ModifierRepository modifierRepository,
        DiningTableRepository tableRepository,
        EmployeeRepository employeeRepository,
        StaffTaskRepository taskRepository,
        TableAssignmentRepository tableAssignmentRepository,
        SubscriptionPlanRepository planRepository,
        SubscriptionRepository subscriptionRepository,
        PasswordEncoder passwordEncoder,
        SeedProperties properties,
        JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.variantRepository = variantRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierRepository = modifierRepository;
        this.tableRepository = tableRepository;
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.tableAssignmentRepository = tableAssignmentRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDemoCustomization();
        ensureDemoStaff();
        if (userRepository.existsByEmail(properties.adminEmail())) {
            log.info("Dev seed already present; skipping");
            return;
        }

        Tenant demoTenant = tenantRepository.findBySlug("demo-bistro").orElseGet(() -> {
            Tenant tenant = new Tenant();
            tenant.setName("Demo Bistro");
            tenant.setSlug("demo-bistro");
            tenant.setStatus("ACTIVE");
            tenant.setContactEmail(properties.adminEmail());
            tenant.setTimezone("UTC");
            tenant.setCurrency("USD");
            tenant.setTrialEndsAt(Instant.now().plus(30, ChronoUnit.DAYS));
            return tenantRepository.save(tenant);
        });

        SubscriptionPlan growth = planRepository.findByCode("GROWTH").orElse(null);
        if (growth != null && subscriptionRepository.findByTenantId(demoTenant.getId()).isEmpty()) {
            Subscription subscription = new Subscription();
            subscription.setTenantId(demoTenant.getId());
            subscription.setPlanId(growth.getId());
            subscription.setStatus("ACTIVE");
            subscription.setCurrentPeriodStart(Instant.now());
            subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
            subscriptionRepository.save(subscription);
        }

        Restaurant restaurant = restaurantRepository.findByTenantId(demoTenant.getId()).orElseGet(() -> {
            Restaurant r = new Restaurant();
            r.setTenantId(demoTenant.getId());
            r.setCode("DEMO");
            r.setName("Demo Bistro");
            r.setLegalName("Demo Bistro LLC");
            r.setDescription("A cozy neighborhood bistro serving seasonal comfort food.");
            r.setTimezone("UTC");
            r.setCurrency("USD");
            r.setTaxRate(new BigDecimal("0.0825"));
            r.setServiceChargeRate(new BigDecimal("0.0500"));
            return restaurantRepository.save(r);
        });

        Branch branch = branchRepository.findByTenantIdOrderByNameAsc(demoTenant.getId()).stream()
            .findFirst()
            .orElseGet(() -> {
                Branch b = new Branch();
                b.setTenantId(demoTenant.getId());
                b.setRestaurantId(restaurant.getId());
                b.setCode("MAIN");
                b.setName("Downtown");
                b.setAddressLine1("123 Main Street");
                b.setCity("Springfield");
                b.setState("IL");
                b.setPostalCode("62701");
                b.setCountry("US");
                b.setPhone("+1 555 0100");
                b.setEmail("downtown@demo-bistro.local");
                b.setIsDefault(Boolean.TRUE);
                return branchRepository.save(b);
            });

        MenuCategory mains = categoryRepository
            .findByTenantIdOrderBySortOrderAscNameAsc(demoTenant.getId()).stream()
            .filter(c -> c.getName().equals("Mains"))
            .findFirst()
            .orElseGet(() -> {
                MenuCategory c = new MenuCategory();
                c.setTenantId(demoTenant.getId());
                c.setRestaurantId(restaurant.getId());
                c.setName("Mains");
                c.setDescription("Hearty plates from the kitchen");
                c.setSortOrder(10);
                return categoryRepository.save(c);
            });

        MenuCategory drinks = categoryRepository
            .findByTenantIdOrderBySortOrderAscNameAsc(demoTenant.getId()).stream()
            .filter(c -> c.getName().equals("Drinks"))
            .findFirst()
            .orElseGet(() -> {
                MenuCategory c = new MenuCategory();
                c.setTenantId(demoTenant.getId());
                c.setRestaurantId(restaurant.getId());
                c.setName("Drinks");
                c.setDescription("House-made beverages");
                c.setSortOrder(20);
                return categoryRepository.save(c);
            });

        if (menuItemRepository.countByTenantId(demoTenant.getId()) == 0) {
            createItem(demoTenant, restaurant, mains, "BRG-001", "Classic Burger",
                "Beef patty, cheddar, pickles, house sauce", "16.50", true, 1);
            createItem(demoTenant, restaurant, mains, "PST-001", "Truffle Pasta",
                "Tagliatelle, mushroom cream, parmesan", "19.00", true, 2);
            createItem(demoTenant, restaurant, mains, "SAL-001", "Harvest Salad",
                "Roasted squash, greens, goat cheese, pepitas", "13.50", false, 3);
            createItem(demoTenant, restaurant, drinks, "LEM-001", "House Lemonade",
                "Fresh lemon, mint, cane sugar", "4.50", false, 1);
            createItem(demoTenant, restaurant, drinks, "ESP-001", "Espresso",
                "Double shot, locally roasted", "3.25", false, 2);
        }

        if (tableRepository.findByTenantIdAndBranchIdOrderByTableNumberAsc(
                demoTenant.getId(), branch.getId()).isEmpty()) {
            for (int i = 1; i <= 6; i++) {
                DiningTable table = new DiningTable();
                table.setTenantId(demoTenant.getId());
                table.setBranchId(branch.getId());
                table.setTableNumber(String.valueOf(i));
                table.setName("Table " + i);
                table.setCapacity(i <= 4 ? 2 : 4);
                tableRepository.save(table);
            }
        }

        Role superAdmin = roleRepository.findSystemRoleWithPermissions("SUPER_ADMIN")
            .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role missing"));
        Role owner = roleRepository.findSystemRoleWithPermissions("TENANT_OWNER")
            .orElseThrow(() -> new IllegalStateException("TENANT_OWNER role missing"));

        User admin = new User();
        admin.setEmail(properties.adminEmail());
        admin.setFirstName("Platform");
        admin.setLastName("Admin");
        admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(Boolean.TRUE);
        admin.getRoles().add(superAdmin);
        userRepository.save(admin);

        User ownerUser = new User();
        ownerUser.setTenantId(demoTenant.getId());
        ownerUser.setEmail("owner@demo-bistro.local");
        ownerUser.setFirstName("Demo");
        ownerUser.setLastName("Owner");
        ownerUser.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        ownerUser.setStatus("ACTIVE");
        ownerUser.setEmailVerified(Boolean.TRUE);
        ownerUser.getRoles().add(owner);
        userRepository.save(ownerUser);

        log.info("Seeded dev data: admin={} owner=owner@demo-bistro.local", properties.adminEmail());
        ensureDemoCustomization();
    }

    private void ensureDemoCustomization() {
        Tenant demoTenant = tenantRepository.findBySlug("demo-bistro").orElse(null);
        if (demoTenant == null) {
            return;
        }
        Restaurant restaurant = restaurantRepository.findByTenantId(demoTenant.getId()).orElse(null);
        if (restaurant == null) {
            return;
        }

        List<MenuItem> items = menuItemRepository.findByTenantIdOrderBySortOrderAscNameAsc(demoTenant.getId());
        if (items.isEmpty()) {
            return;
        }
        java.util.Set<String> vegetarianSkus =
            java.util.Set.of("PST-001", "SAL-001", "LEM-001", "ESP-001");
        for (MenuItem item : items) {
            boolean vegetarian = vegetarianSkus.contains(item.getSku());
            if (vegetarian != item.getIsVegetarian()) {
                item.setIsVegetarian(vegetarian);
                menuItemRepository.save(item);
            }
        }

        MenuItem burger = findBySku(items, "BRG-001");
        MenuItem pasta = findBySku(items, "PST-001");
        MenuItem salad = findBySku(items, "SAL-001");
        MenuItem lemonade = findBySku(items, "LEM-001");
        MenuItem espresso = findBySku(items, "ESP-001");

        if (burger != null && variantRepository.findByMenuItemIdOrderBySortOrderAsc(burger.getId()).isEmpty()) {
            createVariant(burger, "Regular", "0", true, 1);
            createVariant(burger, "Large", "80", false, 2);
        }
        if (pasta != null && variantRepository.findByMenuItemIdOrderBySortOrderAsc(pasta.getId()).isEmpty()) {
            createVariant(pasta, "Regular", "0", true, 1);
            createVariant(pasta, "Large", "100", false, 2);
        }
        if (lemonade != null && variantRepository.findByMenuItemIdOrderBySortOrderAsc(lemonade.getId()).isEmpty()) {
            createVariant(lemonade, "Glass", "0", true, 1);
            createVariant(lemonade, "Jug", "60", false, 2);
        }
        if (espresso != null && variantRepository.findByMenuItemIdOrderBySortOrderAsc(espresso.getId()).isEmpty()) {
            createVariant(espresso, "Single", "0", true, 1);
            createVariant(espresso, "Double", "50", false, 2);
        }

        ModifierGroup dipGroup = createGroup(demoTenant, restaurant, "Choose your dip",
            "SINGLE", true, 1, 1, 10);
        Modifier mayo = createModifier(demoTenant, dipGroup, "Mint Mayo", "0", true, 1);
        Modifier chili = createModifier(demoTenant, dipGroup, "Chili Mayo", "0", false, 2);
        if (burger != null) {
            ensureMapping(burger.getId(), dipGroup.getId());
        }

        ModifierGroup addOnGroup = createGroup(demoTenant, restaurant, "Add-ons",
            "MULTI", false, 0, 3, 20);
        Modifier cheese = createModifier(demoTenant, addOnGroup, "Extra Cheese", "40", false, 1);
        Modifier bacon = createModifier(demoTenant, addOnGroup, "Crispy Bacon", "60", false, 2);
        Modifier spicy = createModifier(demoTenant, addOnGroup, "Extra Spicy", "0", false, 3);
        for (MenuItem item : java.util.Arrays.asList(burger, pasta, salad)) {
            if (item != null) {
                ensureMapping(item.getId(), addOnGroup.getId());
            }
        }

        log.info("Seeded demo customization data (variants, modifiers) for demo-bistro; "
            + "mods={}/{}/{}/{}/{}/{}", mayo.getId(), chili.getId(), cheese.getId(),
            bacon.getId(), spicy.getId(), dipGroup.getId());
    }

    private MenuItem findBySku(List<MenuItem> items, String sku) {
        return items.stream().filter(i -> sku.equals(i.getSku())).findFirst().orElse(null);
    }

    private void createVariant(MenuItem item, String name, String priceDelta, boolean isDefault, int sort) {
        MenuItemVariant variant = new MenuItemVariant();
        variant.setTenantId(item.getTenantId());
        variant.setMenuItemId(item.getId());
        variant.setName(name);
        variant.setPriceDelta(new BigDecimal(priceDelta));
        variant.setIsDefault(isDefault);
        variant.setSortOrder(sort);
        variantRepository.save(variant);
    }

    private ModifierGroup createGroup(
        Tenant tenant, Restaurant restaurant, String name, String selectionType,
        boolean required, int min, int max, int sort) {
        List<ModifierGroup> existing = modifierGroupRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenant.getId());
        return existing.stream()
            .filter(g -> g.getName().equals(name))
            .findFirst()
            .orElseGet(() -> {
                ModifierGroup group = new ModifierGroup();
                group.setTenantId(tenant.getId());
                group.setRestaurantId(restaurant.getId());
                group.setName(name);
                group.setSelectionType(selectionType);
                group.setIsRequired(required);
                group.setMinSelections(min);
                group.setMaxSelections(max);
                group.setSortOrder(sort);
                return modifierGroupRepository.save(group);
            });
    }

    private Modifier createModifier(Tenant tenant, ModifierGroup group, String name, String priceDelta,
        boolean isDefault, int sort) {
        List<Modifier> existing = modifierRepository.findByModifierGroupIdOrderBySortOrderAsc(group.getId());
        return existing.stream()
            .filter(m -> m.getName().equals(name))
            .findFirst()
            .orElseGet(() -> {
                Modifier modifier = new Modifier();
                modifier.setTenantId(tenant.getId());
                modifier.setModifierGroupId(group.getId());
                modifier.setName(name);
                modifier.setPriceDelta(new BigDecimal(priceDelta));
                modifier.setIsDefault(isDefault);
                modifier.setSortOrder(sort);
                return modifierRepository.save(modifier);
            });
    }

    private void ensureMapping(Long menuItemId, Long modifierGroupId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM menu_item_modifier_groups WHERE menu_item_id = ? AND modifier_group_id = ?",
            Integer.class, menuItemId, modifierGroupId);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO menu_item_modifier_groups (menu_item_id, modifier_group_id) VALUES (?, ?)",
                menuItemId, modifierGroupId);
        }
    }

    private void createItem(
        Tenant tenant, Restaurant restaurant, MenuCategory category,
        String sku, String name, String description, String price, boolean featured, int sort) {
        MenuItem item = new MenuItem();
        item.setTenantId(tenant.getId());
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(category.getId());
        item.setSku(sku);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setCurrency("USD");
        item.setIsFeatured(featured);
        item.setSortOrder(sort);
        item.setPrepTimeMinutes(15);
        menuItemRepository.save(item);
    }

    private void ensureDemoStaff() {
        Tenant demoTenant = tenantRepository.findBySlug("demo-bistro").orElse(null);
        if (demoTenant == null) {
            return;
        }
        Branch branch = branchRepository.findByTenantIdOrderByNameAsc(demoTenant.getId()).stream()
            .findFirst().orElse(null);
        if (branch == null) {
            return;
        }
        if (employeeRepository.findByTenantId(demoTenant.getId()).stream().anyMatch(e -> "EMP-W1".equals(e.getEmployeeCode()))) {
            return;
        }
        List<DiningTable> tables = tableRepository.findByTenantIdAndBranchIdOrderByTableNumberAsc(
            demoTenant.getId(), branch.getId());

        Role waiterRole = roleRepository.findSystemRoleWithPermissions("WAITER")
            .orElseThrow(() -> new IllegalStateException("WAITER role missing"));
        Role chefRole = roleRepository.findSystemRoleWithPermissions("CHEF")
            .orElseThrow(() -> new IllegalStateException("CHEF role missing"));
        Role helperRole = roleRepository.findSystemRoleWithPermissions("KITCHEN_HELPER")
            .orElseThrow(() -> new IllegalStateException("KITCHEN_HELPER role missing"));
        Role confirmRole = roleRepository.findSystemRoleWithPermissions("ORDER_CONFIRMATION")
            .orElseThrow(() -> new IllegalStateException("ORDER_CONFIRMATION role missing"));
        Role managerRole = roleRepository.findSystemRoleWithPermissions("TENANT_MANAGER")
            .orElseThrow(() -> new IllegalStateException("TENANT_MANAGER role missing"));

        Employee avery = staff(demoTenant, branch, waiterRole, "Avery", "Singh",
            "avery.waiter@demo-bistro.local", "EMP-W1", "Morning Shift", true);
        Employee blake = staff(demoTenant, branch, waiterRole, "Blake", "Chen",
            "blake.waiter@demo-bistro.local", "EMP-W2", "Evening Shift", false);
        staff(demoTenant, branch, chefRole, "Casey", "Ortiz",
            "casey.chef@demo-bistro.local", "EMP-K1", "Kitchen AM", true);
        staff(demoTenant, branch, helperRole, "Devon", "Park",
            "devon.helper@demo-bistro.local", "EMP-K2", "Kitchen PM", false);
        staff(demoTenant, branch, confirmRole, "Emery", "Lane",
            "emery.confirm@demo-bistro.local", "EMP-C1", "Front Desk", true);
        staff(demoTenant, branch, managerRole, "Finley", "Brooks",
            "finley.manager@demo-bistro.local", "EMP-M1", "Full Time", true);

        if (!tables.isEmpty()) {
            assignTable(demoTenant, branch, tables.get(0), avery);
            if (tables.size() > 1) assignTable(demoTenant, branch, tables.get(1), avery);
            if (tables.size() > 2) assignTable(demoTenant, branch, tables.get(2), blake);
            if (tables.size() > 3) assignTable(demoTenant, branch, tables.get(3), blake);
        }

        if (taskRepository.count() == 0) {
            seedTask(demoTenant, branch, avery, "Serve table 1", "SERVE_TABLE", "HIGH",
                "ASSIGNED", tables.isEmpty() ? null : tables.get(0).getId(), Instant.now().plus(30, ChronoUnit.MINUTES));
            seedTask(demoTenant, branch, avery, "Clean table 2 after guests leave", "CLEAN_TABLE", "NORMAL",
                "IN_PROGRESS", tables.size() > 1 ? tables.get(1).getId() : null, Instant.now().plus(60, ChronoUnit.MINUTES));
            seedTask(demoTenant, branch, blake, "Serve table 3", "SERVE_TABLE", "NORMAL",
                "ASSIGNED", tables.size() > 2 ? tables.get(2).getId() : null, Instant.now().plus(45, ChronoUnit.MINUTES));
            seedTask(demoTenant, branch, blake, "Reset dining area", "CLEAN_TABLE", "LOW",
                "COMPLETED", tables.size() > 3 ? tables.get(3).getId() : null, Instant.now().minus(2, ChronoUnit.HOURS));
            StaffTask chefTask = seedTask(demoTenant, branch, employeeRepository.findByTenantIdAndEmployeeCode(demoTenant.getId(), "EMP-K1").orElse(null),
                "Prepare dinner prep list", "KITCHEN_PREPARATION", "NORMAL", "IN_PROGRESS", null,
                Instant.now().plus(90, ChronoUnit.MINUTES));
            if (chefTask != null) {
                chefTask.setStartedAt(Instant.now().minus(20, ChronoUnit.MINUTES));
                taskRepository.save(chefTask);
            }
            seedTask(demoTenant, branch, employeeRepository.findByTenantIdAndEmployeeCode(demoTenant.getId(), "EMP-K2").orElse(null),
                "Restock beverage station", "RESTOCK_SUPPLIES", "URGENT", "ASSIGNED", null,
                Instant.now().plus(15, ChronoUnit.MINUTES));
            seedTask(demoTenant, branch, employeeRepository.findByTenantIdAndEmployeeCode(demoTenant.getId(), "EMP-C1").orElse(null),
                "Review incoming QR orders", "CONFIRM_ORDER", "HIGH", "ASSIGNED", null,
                Instant.now().plus(20, ChronoUnit.MINUTES));
            seedTask(demoTenant, branch, employeeRepository.findByTenantIdAndEmployeeCode(demoTenant.getId(), "EMP-M1").orElse(null),
                "Opening checklist for tomorrow", "OPENING_CHECKLIST", "LOW", "BLOCKED", null,
                Instant.now().plus(12, ChronoUnit.HOURS));
        }
        log.info("Seeded demo staff: employees={} tasks={} assignments={}",
            employeeRepository.findByTenantId(demoTenant.getId()).size(),
            taskRepository.count(),
            tableAssignmentRepository.findByTenantIdAndIsActiveTrue(demoTenant.getId()).size());
    }

    private Employee staff(Tenant tenant, Branch branch, Role role, String first, String last,
                           String email, String code, String shift, boolean onDuty) {
        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        user.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        user.setStatus("ACTIVE");
        user.setEmailVerified(Boolean.TRUE);
        user.getRoles().add(role);
        userRepository.save(user);

        Employee employee = new Employee();
        employee.setTenantId(tenant.getId());
        employee.setBranchId(branch.getId());
        employee.setUserId(user.getId());
        employee.setEmployeeCode(code);
        employee.setJobTitle(role.getName());
        employee.setHiredAt(java.time.LocalDate.now().minusMonths(6));
        employee.setShiftName(shift);
        employee.setStatus("ACTIVE");
        employee.setOnDuty(onDuty);
        employee.setOnDutySince(onDuty ? Instant.now().minus(java.time.Duration.ofHours(2)) : null);
        return employeeRepository.save(employee);
    }

    private void assignTable(Tenant tenant, Branch branch, DiningTable table, Employee employee) {
        boolean exists = tableAssignmentRepository.findByTenantIdAndBranchIdAndIsActiveTrue(tenant.getId(), branch.getId())
            .stream().anyMatch(a -> a.getTableId().equals(table.getId()));
        if (exists) {
            return;
        }
        TableAssignment assignment = new TableAssignment();
        assignment.setTenantId(tenant.getId());
        assignment.setBranchId(branch.getId());
        assignment.setTableId(table.getId());
        assignment.setEmployeeId(employee.getId());
        assignment.setAssignedAt(Instant.now().minus(java.time.Duration.ofHours(1)));
        tableAssignmentRepository.save(assignment);
    }

    private StaffTask seedTask(Tenant tenant, Branch branch, Employee employee, String title, String type,
                               String priority, String status, Long tableId, Instant dueAt) {
        if (employee == null) {
            return null;
        }
        StaffTask task = new StaffTask();
        task.setTenantId(tenant.getId());
        task.setBranchId(branch.getId());
        task.setTitle(title);
        task.setTaskType(type);
        task.setPriority(priority);
        task.setStatus(status);
        task.setAssignedToEmployeeId(employee.getId());
        task.setTableId(tableId);
        task.setDueAt(dueAt);
        task.setCreatedAt(Instant.now().minus(java.time.Duration.ofHours(3)));
        if ("IN_PROGRESS".equals(status)) {
            task.setStartedAt(Instant.now().minus(java.time.Duration.ofHours(1)));
        }
        if ("COMPLETED".equals(status)) {
            task.setStartedAt(Instant.now().minus(java.time.Duration.ofHours(2)));
            task.setCompletedAt(Instant.now().minus(java.time.Duration.ofHours(1)));
        }
        if ("BLOCKED".equals(status)) {
            task.setStatusNote("Waiting for the delivery truck");
        }
        return taskRepository.save(task);
    }
}
