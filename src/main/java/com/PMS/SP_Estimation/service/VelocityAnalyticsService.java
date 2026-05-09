package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.response.VelocityTrendResponse;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.repo.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VelocityAnalyticsService {

    private final SprintRepository sprintRepo;

    // Domain benchmarks from SV dataset
    private static final Map<String, Double> DOMAIN_AVG = Map.of(
        "Finance",    26.31, "Logistics",  26.08,
        "E-commerce", 24.16, "Education",  23.84, "Healthcare", 22.17
    );

    /**
     * Compute avg_velocity_last3 and avg_velocity_last5 for a project.
     * Mirrors the SV dataset feature engineering exactly.
     * These feed directly into the ML velocity prediction model.
     */
    public Map<String, Double> computeRollingVelocities(Long projectId) {
        List<Sprint> completed = sprintRepo
            .findByProjectIdAndStatus(projectId, Sprint.Status.COMPLETED,
                Sort.by(Sort.Direction.DESC, "endDate"));

        List<Integer> velocities = completed.stream()
            .map(Sprint::getActualVelocity)
            .filter(Objects::nonNull)
            .toList();

        double avg3 = velocities.stream().limit(3)
            .mapToInt(Integer::intValue).average().orElse(0.0);
        double avg5 = velocities.stream().limit(5)
            .mapToInt(Integer::intValue).average().orElse(avg3);

        return Map.of(
            "avg_velocity_last3", Math.round(avg3 * 10.0) / 10.0,
            "avg_velocity_last5", Math.round(avg5 * 10.0) / 10.0,
            "velocity_trend",     Math.round((avg3 - avg5) * 10.0) / 10.0
        );
    }

    /**
     * Full velocity trend report for the dashboard.
     */
    public VelocityTrendResponse getVelocityTrend(Long projectId) {
        List<Sprint> completed = sprintRepo
            .findByProjectIdAndStatus(projectId, Sprint.Status.COMPLETED,
                Sort.by(Sort.Direction.ASC, "endDate"));

        List<VelocityTrendResponse.VelocityPoint> points = completed.stream()
            .map(s -> VelocityTrendResponse.VelocityPoint.builder()
                .sprintId(s.getId())
                .sprintName(s.getName())
                .actualVelocity(s.getActualVelocity() != null ? s.getActualVelocity() : 0)
                .predictedVelocity(s.getPredictedVelocity() != null ? s.getPredictedVelocity() : 0)
                .completionRate(s.getCompletionRate() != null ? s.getCompletionRate() : 0.0)
                .endDate(s.getEndDate())
                .build())
            .toList();

        Map<String, Double> rolling = computeRollingVelocities(projectId);
        double avg3  = rolling.get("avg_velocity_last3");
        double avg5  = rolling.get("avg_velocity_last5");
        double trend = rolling.get("velocity_trend");

        String trendDir = trend > 1.0 ? "IMPROVING" : trend < -1.0 ? "DECLINING" : "STABLE";

        // Team efficiency: velocity per developer
        Sprint latest = completed.isEmpty() ? null : completed.get(completed.size() - 1);
        int teamSize = (latest != null && latest.getProject() != null
                        && latest.getProject().getTeamSize() != null)
            ? latest.getProject().getTeamSize() : 5;
        double velPerDev = teamSize > 0 ? avg3 / teamSize : 0.0;

        String domain     = (latest != null && latest.getProject() != null)
            ? latest.getProject().getDomain() : "unknown";
        double domainAvg  = DOMAIN_AVG.getOrDefault(capitalise(domain), 24.45);
        String benchmark  = avg3 >= domainAvg * 1.10 ? "ABOVE_BENCHMARK"
                          : avg3 >= domainAvg * 0.90 ? "ON_BENCHMARK"
                          : "BELOW_BENCHMARK";

        return VelocityTrendResponse.builder()
            .projectId(projectId)
            .history(points)
            .avgVelocityLast3(avg3)
            .avgVelocityLast5(avg5)
            .trend(trend)
            .trendDirection(trendDir)
            .velocityPerDev(Math.round(velPerDev * 100.0) / 100.0)
            .domainBenchmark(benchmark)
            .domainAvgVelocity(domainAvg)
            .build();
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
