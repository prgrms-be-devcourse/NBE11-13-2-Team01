package com.example.delivery_project.service.component.route;

public interface RouteOptimizer {

    OptimizedRoute optimize(
            RouteOptimizationContext context
    );
}
