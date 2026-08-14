package com.example.delivery_project.service.component.route;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Set;

@Component
public class DijkstraRouteOptimizer implements RouteOptimizer {
    @Override
    public OptimizedRoute optimize(
            RouteOptimizationContext context
    ) {
        PriorityQueue<SearchNode> routesToVisit = new PriorityQueue<>(
                Comparator.comparingLong(
                        SearchNode::totalDurationSeconds
                )
        );

        Map<RouteState, Long> minimumDurationByState = new HashMap<>();
        RouteState initialState = new RouteState(
                context.currentStopId(),
                Set.of()
        );

        SearchNode initialNode = new SearchNode(
                initialState,
                List.of(),
                0L
        );

        routesToVisit.add(initialNode);
        minimumDurationByState.put(initialState, 0L);

        int expandedStateCount = 0;
        while (!routesToVisit.isEmpty()) {
            SearchNode current = routesToVisit.poll();

            if (isOutdated(current, minimumDurationByState)) {
                continue;
            }

            expandedStateCount++;

            if (visitedEveryCandidate(current, context)) {
                return new OptimizedRoute(
                        current.stopOrder(),
                        current.totalDurationSeconds(),
                        expandedStateCount
                );
            }

            visitUnvisitedStops(
                    context,
                    current,
                    routesToVisit,
                    minimumDurationByState
            );
        }

        // TODO 커스텀 예외로 변경
        throw new IllegalStateException(
                "모든 후보 배송지를 방문할 수 있는 경로가 없습니다."
        );
    }

    private boolean isOutdated(
            SearchNode node,
            Map<RouteState, Long> minimumDurationByState
    ) {
        long minimumDuration = minimumDurationByState.getOrDefault(
                        node.state(),
                        Long.MAX_VALUE
                );

        return node.totalDurationSeconds() > minimumDuration;
    }

    private boolean visitedEveryCandidate(
            SearchNode node,
            RouteOptimizationContext context
    ) {
        return node.state()
                .visitedStopIds()
                .size()
                == context.candidateStopIds().size();
    }

    private void visitUnvisitedStops(
            RouteOptimizationContext context,
            SearchNode current,
            PriorityQueue<SearchNode> routesToVisit,
            Map<RouteState, Long> minimumDurationByState
    ) {
        for (Long nextStopId
                : context.candidateStopIds()) {
            if (current.state()
                    .visitedStopIds()
                    .contains(nextStopId)) {
                continue;
            }

            visitStop(
                    context,
                    current,
                    nextStopId,
                    routesToVisit,
                    minimumDurationByState
            );
        }
    }

    private void visitStop(
            RouteOptimizationContext context,
            SearchNode current,
            Long nextStopId,
            PriorityQueue<SearchNode> routesToVisit,
            Map<RouteState, Long> minimumDurationByState
    ) {
        OptionalLong travelDuration = context.travelCostMatrix().findDuration(
                current.state().currentStopId(),
                nextStopId
        );

        if (travelDuration.isEmpty()) {
            return;
        }

        Set<Long> nextVisitedStopIds = new HashSet<>(current.state().visitedStopIds());
        nextVisitedStopIds.add(nextStopId);

        List<Long> nextStopOrder = new ArrayList<>(current.stopOrder());
        nextStopOrder.add(nextStopId);

        long nextTotalDuration = Math.addExact(
                current.totalDurationSeconds(),
                travelDuration.getAsLong()
        );

        RouteState nextState = new RouteState(
                nextStopId,
                nextVisitedStopIds
        );

        long knownDuration = minimumDurationByState.getOrDefault(
                nextState,
                Long.MAX_VALUE
        );

        if (nextTotalDuration >= knownDuration) {
            return;
        }

        minimumDurationByState.put(
                nextState,
                nextTotalDuration
        );
        routesToVisit.add(new SearchNode(
                nextState,
                nextStopOrder,
                nextTotalDuration
        ));
    }

    private record RouteState(
            Long currentStopId,
            Set<Long> visitedStopIds
    ) {
        private RouteState {
            visitedStopIds = Set.copyOf(
                    visitedStopIds
            );
        }
    }

    private record SearchNode(
            RouteState state,
            List<Long> stopOrder,
            long totalDurationSeconds
    ) {
        private SearchNode {
            stopOrder = List.copyOf(stopOrder);
        }
    }
}
