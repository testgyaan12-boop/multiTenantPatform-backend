package com.restaurant.saas.subscription.web;

import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import com.restaurant.saas.subscription.repository.SubscriptionPlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/plans")
public class PublicPlansController {

    private final SubscriptionPlanRepository planRepository;

    public PublicPlansController(SubscriptionPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<SubscriptionPlan> plans() {
        return planRepository.findPublicByStatus("ACTIVE");
    }
}
