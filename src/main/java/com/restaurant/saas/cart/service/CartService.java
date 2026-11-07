package com.restaurant.saas.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.cart.dto.AddCartItemRequest;
import com.restaurant.saas.cart.dto.CartResponse;
import com.restaurant.saas.cart.dto.ModifierSelection;
import com.restaurant.saas.cart.entity.Cart;
import com.restaurant.saas.cart.entity.CartItem;
import com.restaurant.saas.cart.repository.CartItemRepository;
import com.restaurant.saas.cart.repository.CartRepository;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.customer.entity.GuestSession;
import com.restaurant.saas.customer.service.GuestSessionService;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemVariant;
import com.restaurant.saas.menu.entity.Modifier;
import com.restaurant.saas.menu.entity.ModifierGroup;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.menu.repository.MenuItemVariantRepository;
import com.restaurant.saas.menu.repository.ModifierGroupRepository;
import com.restaurant.saas.menu.repository.ModifierRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemVariantRepository variantRepository;
    private final ModifierRepository modifierRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final BranchRepository branchRepository;
    private final RestaurantRepository restaurantRepository;
    private final GuestSessionService guestSessionService;
    private final ObjectMapper objectMapper;

    public CartService(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        MenuItemRepository menuItemRepository,
        MenuItemVariantRepository variantRepository,
        ModifierRepository modifierRepository,
        ModifierGroupRepository modifierGroupRepository,
        BranchRepository branchRepository,
        RestaurantRepository restaurantRepository,
        GuestSessionService guestSessionService,
        ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.variantRepository = variantRepository;
        this.modifierRepository = modifierRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.branchRepository = branchRepository;
        this.restaurantRepository = restaurantRepository;
        this.guestSessionService = guestSessionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CartResponse getOrCreate(String sessionToken) {
        GuestSession session = guestSessionService.requireActive(sessionToken);
        Cart cart = cartRepository.findByGuestSessionIdAndStatus(session.getId(), "OPEN")
            .orElseGet(() -> createCart(session));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String sessionToken, AddCartItemRequest request) {
        GuestSession session = guestSessionService.requireActive(sessionToken);
        Cart cart = cartRepository.findByGuestSessionIdAndStatus(session.getId(), "OPEN")
            .orElseGet(() -> createCart(session));

        MenuItem item = menuItemRepository.findByIdAndTenantId(request.menuItemId(), session.getTenantId())
            .orElseThrow(() -> new NotFoundException("Menu item", request.menuItemId()));
        if (!Boolean.TRUE.equals(item.getIsAvailable())) {
            throw new ConflictException("item_unavailable", "This item is currently unavailable");
        }

        BigDecimal unitPrice = item.getPrice();
        if (request.variantId() != null) {
            MenuItemVariant variant = variantRepository
                .findByIdAndTenantId(request.variantId(), session.getTenantId())
                .orElseThrow(() -> new NotFoundException("Variant", request.variantId()));
            if (!variant.getMenuItemId().equals(item.getId())) {
                throw new ConflictException("invalid_variant", "Variant does not belong to this item");
            }
            unitPrice = unitPrice.add(variant.getPriceDelta());
        }

        List<ModifierSelection> selections = resolveModifiers(session.getTenantId(), item.getId(), request.modifierIds());
        BigDecimal modifierTotal = selections.stream()
            .map(ModifierSelection::priceDelta)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartItem cartItem = new CartItem();
        cartItem.setTenantId(session.getTenantId());
        cartItem.setCartId(cart.getId());
        cartItem.setMenuItemId(item.getId());
        cartItem.setVariantId(request.variantId());
        cartItem.setQuantity(request.quantity());
        cartItem.setUnitPrice(unitPrice);
        cartItem.setModifierTotal(modifierTotal);
        cartItem.setLineTotal(unitPrice.add(modifierTotal).multiply(BigDecimal.valueOf(request.quantity())));
        cartItem.setNotes(request.notes());
        cartItem.setModifierSelections(ModifierSelection.write(objectMapper, selections));
        cartItemRepository.save(cartItem);

        recalculate(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(String sessionToken, Long cartItemId, int quantity) {
        GuestSession session = guestSessionService.requireActive(sessionToken);
        Cart cart = requireOpenCart(session);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
            .orElseThrow(() -> new NotFoundException("Cart item", cartItemId));
        item.setQuantity(quantity);
        item.setLineTotal(item.getUnitPrice().add(item.getModifierTotal())
            .multiply(BigDecimal.valueOf(quantity)));
        cartItemRepository.save(item);
        recalculate(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String sessionToken, Long cartItemId) {
        GuestSession session = guestSessionService.requireActive(sessionToken);
        Cart cart = requireOpenCart(session);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
            .orElseThrow(() -> new NotFoundException("Cart item", cartItemId));
        item.softDelete();
        cartItemRepository.save(item);
        recalculate(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clear(String sessionToken) {
        GuestSession session = guestSessionService.requireActive(sessionToken);
        Cart cart = cartRepository.findByGuestSessionIdAndStatus(session.getId(), "OPEN")
            .orElseGet(() -> createCart(session));
        for (CartItem item : cartItemRepository.findByCartIdOrderByIdAsc(cart.getId())) {
            item.softDelete();
            cartItemRepository.save(item);
        }
        recalculate(cart);
        return toResponse(cart);
    }

    @Transactional(readOnly = true)
    public Cart requireOpenCart(GuestSession session) {
        return cartRepository.findByGuestSessionIdAndStatus(session.getId(), "OPEN")
            .orElseThrow(() -> new NotFoundException("Cart not found"));
    }

    @Transactional
    public Cart recalculate(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        BigDecimal subtotal = items.stream()
            .map(CartItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Restaurant restaurant = restaurantRepository.findByTenantId(cart.getTenantId()).orElse(null);
        BigDecimal taxRate = restaurant == null ? BigDecimal.ZERO : restaurant.getTaxRate();
        BigDecimal serviceRate = restaurant == null ? BigDecimal.ZERO : restaurant.getServiceChargeRate();

        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal service = subtotal.multiply(serviceRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grand = subtotal.add(tax).add(service)
            .add(cart.getTipTotal()).subtract(cart.getDiscountTotal());

        cart.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        cart.setTaxTotal(tax);
        cart.setServiceCharge(service);
        cart.setGrandTotal(grand.setScale(2, RoundingMode.HALF_UP));
        return cartRepository.save(cart);
    }

    private List<ModifierSelection> resolveModifiers(Long tenantId, Long menuItemId, List<Long> modifierIds) {
        if (modifierIds == null || modifierIds.isEmpty()) {
            return List.of();
        }
        List<Long> allowedGroupIds = modifierGroupRepository.findGroupIdsForMenuItem(menuItemId);
        Map<Long, Modifier> loaded = new LinkedHashMap<>();
        for (Modifier modifier : modifierRepository.findAllById(modifierIds)) {
            loaded.put(modifier.getId(), modifier);
        }
        List<ModifierSelection> selections = new ArrayList<>();
        for (Long modifierId : modifierIds) {
            Modifier modifier = loaded.get(modifierId);
            if (modifier == null) {
                throw new NotFoundException("Modifier", modifierId);
            }
            if (!tenantId.equals(modifier.getTenantId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_modifier", "Modifier does not belong to this restaurant");
            }
            if (!allowedGroupIds.contains(modifier.getModifierGroupId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_modifier", "Modifier is not offered for this item");
            }
            if (selections.stream().anyMatch(s -> s.modifierId().equals(modifierId))) {
                continue;
            }
            selections.add(new ModifierSelection(modifier.getId(), modifier.getName(), modifier.getPriceDelta()));
        }
        return selections;
    }

    private Cart createCart(GuestSession session) {
        Branch branch = branchRepository.findById(session.getBranchId())
            .orElseThrow(() -> new NotFoundException("Branch", session.getBranchId()));
        Restaurant restaurant = restaurantRepository.findByTenantId(session.getTenantId()).orElse(null);
        Cart cart = new Cart();
        cart.setTenantId(session.getTenantId());
        cart.setBranchId(session.getBranchId());
        cart.setTableId(session.getTableId());
        cart.setGuestSessionId(session.getId());
        cart.setStatus("OPEN");
        String branchCurrency = branch.getCurrency();
        if (branchCurrency != null && !branchCurrency.isBlank()) {
            cart.setCurrency(branchCurrency);
        } else {
            cart.setCurrency(restaurant != null ? restaurant.getCurrency() : "USD");
        }
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTaxTotal(BigDecimal.ZERO);
        cart.setServiceCharge(BigDecimal.ZERO);
        cart.setTipTotal(BigDecimal.ZERO);
        cart.setDiscountTotal(BigDecimal.ZERO);
        cart.setGrandTotal(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> items = cartItemRepository
            .findByCartIdOrderByIdAsc(cart.getId()).stream()
            .map(item -> new CartResponse.CartItemResponse(
                item.getId(),
                item.getMenuItemId(),
                menuItemRepository.findById(item.getMenuItemId())
                    .map(MenuItem::getName).orElse("Item"),
                item.getVariantId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getModifierTotal(),
                item.getLineTotal(),
                item.getNotes(),
                ModifierSelection.read(objectMapper, item.getModifierSelections()).stream()
                    .map(selection -> new CartResponse.ModifierChoice(
                        selection.modifierId(), selection.name(), selection.priceDelta()))
                    .toList()))
            .toList();

        return new CartResponse(
            cart.getId(),
            cart.getStatus(),
            cart.getCurrency(),
            cart.getSubtotal(),
            cart.getTaxTotal(),
            cart.getServiceCharge(),
            cart.getTipTotal(),
            cart.getDiscountTotal(),
            cart.getGrandTotal(),
            items
        );
    }
}
