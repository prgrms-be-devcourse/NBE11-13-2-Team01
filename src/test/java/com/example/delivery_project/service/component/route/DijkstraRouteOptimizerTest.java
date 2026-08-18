package com.example.delivery_project.service.component.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DijkstraRouteOptimizerTest {

    private final RouteOptimizer routeOptimizer =
            new DijkstraRouteOptimizer();

    @Test
    @DisplayName("전체 이동시간이 가장 짧은 배송지 순서를 반환한다")
    void optimize_returns_the_shortest_stop_order() {
        RouteOptimizationContext context = context(
                0L,
                List.of(1L, 2L, 3L),
                Map.ofEntries(
                        duration(0L, 1L, 10L),
                        duration(0L, 2L, 3L),
                        duration(0L, 3L, 8L),
                        duration(1L, 2L, 10L),
                        duration(1L, 3L, 2L),
                        duration(2L, 1L, 4L),
                        duration(2L, 3L, 10L),
                        duration(3L, 1L, 1L),
                        duration(3L, 2L, 2L)
                )
        );

        OptimizedRoute result =
                routeOptimizer.optimize(context);

        printResult(
                "전체 이동시간 최소 경로",
                context,
                result
        );

        assertThat(result.stopIds())
                .containsExactly(2L, 1L, 3L);
        assertThat(result.totalDurationSeconds())
                .isEqualTo(9L);
    }

    @Test
    @DisplayName("가장 가까운 배송지부터 방문하는 탐욕 경로보다 전체 비용이 짧은 경로를 찾는다")
    void optimize_does_not_choose_a_greedy_route() {
        RouteOptimizationContext context = context(
                0L,
                List.of(1L, 2L, 3L),
                Map.ofEntries(
                        duration(0L, 1L, 1L),
                        duration(0L, 2L, 2L),
                        duration(0L, 3L, 50L),
                        duration(1L, 2L, 100L),
                        duration(1L, 3L, 100L),
                        duration(2L, 1L, 1L),
                        duration(2L, 3L, 1L),
                        duration(3L, 1L, 1L),
                        duration(3L, 2L, 1L)
                )
        );

        OptimizedRoute result =
                routeOptimizer.optimize(context);

        printResult(
                "탐욕 경로와 다른 최적 경로",
                context,
                result
        );

        assertThat(result.stopIds())
                .containsExactly(2L, 3L, 1L);
        assertThat(result.totalDurationSeconds())
                .isEqualTo(4L);
    }

    @Test
    @DisplayName("후보 배송지가 없으면 빈 경로를 반환한다")
    void optimize_returns_an_empty_route() {
        RouteOptimizationContext context = context(
                0L,
                List.of(),
                Map.of()
        );

        OptimizedRoute result =
                routeOptimizer.optimize(context);

        printResult(
                "후보 배송지가 없는 경로",
                context,
                result
        );

        assertThat(result.stopIds()).isEmpty();
        assertThat(result.totalDurationSeconds()).isZero();
    }

    @Test
    @DisplayName("모든 후보를 방문할 경로가 없으면 실패한다")
    void optimize_fails_when_route_is_disconnected() {
        RouteOptimizationContext context = context(
                0L,
                List.of(1L, 2L),
                Map.ofEntries(duration(0L, 1L, 1L))
        );

        assertThatThrownBy(() -> routeOptimizer.optimize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("방문할 수 있는 경로가 없습니다");
    }

    @Test
    @DisplayName("후보 배송지 ID는 중복될 수 없다")
    void context_rejects_duplicate_candidates() {
        assertThatThrownBy(() -> context(
                0L,
                List.of(1L, 1L),
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("현재 배송지는 후보 목록에 포함될 수 없다")
    void context_rejects_current_stop_as_candidate() {
        assertThatThrownBy(() -> context(
                1L,
                List.of(1L, 2L),
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이동시간은 음수일 수 없다")
    void matrix_rejects_negative_duration() {
        assertThatThrownBy(() -> new TravelCostMatrix(
                Map.of(new RouteLeg(1L, 2L), -1L)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private void printResult(
            String scenario,
            RouteOptimizationContext context,
            OptimizedRoute result
    ) {
        System.out.println();
        System.out.println(
                "===== 다익스트라 비교군: "
                        + scenario
                        + " ====="
        );
        System.out.println(
                "현재 배송지: "
                        + context.currentStopId()
        );
        System.out.println(
                "후보 배송지: "
                        + context.candidateStopIds()
        );

        Long fromStopId = context.currentStopId();

        for (Long toStopId : result.stopIds()) {
            long duration = context.travelCostMatrix()
                    .findDuration(fromStopId, toStopId)
                    .orElseThrow();

            System.out.printf(
                    "구간: %d -> %d (%d초)%n",
                    fromStopId,
                    toStopId,
                    duration
            );

            fromStopId = toStopId;
        }

        System.out.println(
                "추천 순서: " + result.stopIds()
        );
        System.out.println(
                "총 이동시간: "
                        + result.totalDurationSeconds()
                        + "초"
        );
        System.out.println(
                "확장한 상태 수: "
                        + result.expandedStateCount()
        );
        System.out.println("====================================");
    }

    private RouteOptimizationContext context(
            Long currentStopId,
            List<Long> candidateStopIds,
            Map<RouteLeg, Long> durations
    ) {
        return new RouteOptimizationContext(
                currentStopId,
                candidateStopIds,
                new TravelCostMatrix(durations)
        );
    }

    private Map.Entry<RouteLeg, Long> duration(
            Long fromStopId,
            Long toStopId,
            Long durationSeconds
    ) {
        return Map.entry(
                new RouteLeg(fromStopId, toStopId),
                durationSeconds
        );
    }
}
