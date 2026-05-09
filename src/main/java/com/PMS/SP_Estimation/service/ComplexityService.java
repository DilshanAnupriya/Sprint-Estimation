package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.response.BacklogAnalysisResponse;
import com.PMS.SP_Estimation.dto.response.BacklogItemResponse;
import com.PMS.SP_Estimation.dto.response.ComplexityScoreResponse;
import com.PMS.SP_Estimation.dto.response.CompletionTimeEstimate;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComplexityService {

    private final BacklogItemRepository backlogRepo;

    // Hours per SP lookup — derived from avg_completion_time_hr in SP dataset
    private static final Map<Integer, Double> HOURS_PER_SP = Map.of(
        1,  5.6,
        2,  9.5,
        3,  13.3,
        5,  22.7,
        8,  36.2,
        13, 58.5
    );

    private static final int[] FIBONACCI = {1, 2, 3, 5, 8, 13};

    /**
     * Compute complexity score for a backlog item.
     * Formula derived from SP dataset feature correlations with story_points.
     * Correlation with story_points = 0.71
     *
     * Weight breakdown:
     *   num_components   → 0.25 (highest structural complexity driver)
     *   external_apis    → 0.20 (integration overhead)
     *   has_integration  → 0.15 (binary flag)
     *   has_security     → 0.15 (compliance/auth adds effort)
     *   has_ui_complexity→ 0.10 (frontend complexity)
     *   word_count norm  → 0.15 (description length = scope clarity)
     */
    public ComplexityScoreResponse computeComplexity(Long itemId) {
        BacklogItem item = backlogRepo.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Backlog item not found"));

        int     wordCount      = item.getUserStory() != null
                                 ? item.getUserStory().split("\\s+").length : 0;
        int     numComponents  = item.getNumComponents() != null ? item.getNumComponents() : 1;
        int     externalApis   = item.getExternalApis()  != null ? item.getExternalApis()  : 0;
        boolean hasIntegration = Boolean.TRUE.equals(item.getHasIntegration());
        boolean hasSecurity    = Boolean.TRUE.equals(item.getHasSecurity());
        boolean hasUiComplex   = Boolean.TRUE.equals(item.getHasUiComplexity());

        double score = numComponents  * 0.25
                     + externalApis   * 0.20
                     + (hasIntegration ? 1 : 0) * 0.15
                     + (hasSecurity   ? 1 : 0) * 0.15
                     + (hasUiComplex  ? 1 : 0) * 0.10
                     + (wordCount / 19.0)       * 0.15;   // 19 = max word_count in dataset

        String band;
        if      (score < 0.50) band = "LOW";
        else if (score < 1.00) band = "MEDIUM";
        else if (score < 1.80) band = "HIGH";
        else                   band = "VERY_HIGH";

        // Ambiguity: short description + high expected SP
        // From dataset: 7.5% of stories have word_count ≤ 5 and SP ≥ 5
        boolean ambiguous = wordCount <= 5 && (numComponents >= 3 || externalApis >= 1);
        String ambiguityReason = null;
        if (ambiguous)
            ambiguityReason = "User story is too brief (" + wordCount
                + " words) for a complex task. Refine before estimating.";

        String recommendation = switch (band) {
            case "VERY_HIGH" -> "Consider splitting this story. High complexity increases estimation error.";
            case "HIGH"      -> "Review with the team before sprint commitment.";
            case "MEDIUM"    -> "Standard effort story. Suitable for sprint planning.";
            default          -> "Low complexity. Safe to include without detailed review.";
        };

        // Save back to entity
        item.setComplexityScore(score);
        item.setIsAmbiguous(ambiguous);
        item.setAmbiguityReason(ambiguityReason);
        backlogRepo.save(item);

        return ComplexityScoreResponse.builder()
            .backlogItemId(itemId)
            .title(item.getTitle())
            .numComponents(numComponents)
            .externalApis(externalApis)
            .hasIntegration(hasIntegration)
            .hasSecurity(hasSecurity)
            .hasUiComplexity(hasUiComplex)
            .wordCount(wordCount)
            .complexityScore(Math.round(score * 1000.0) / 1000.0)
            .complexityBand(band)
            .isAmbiguous(ambiguous)
            .ambiguityReason(ambiguityReason)
            .recommendation(recommendation)
            .build();
    }

    /**
     * Estimate completion hours based on story points.
     * Source: avg_completion_time_hr column in SP dataset.
     * Pattern: roughly 4.5 × SP hours (linear relationship in data).
     */
    public CompletionTimeEstimate estimateHours(int storyPoints, String riskLevel) {
        double hours = HOURS_PER_SP.getOrDefault(storyPoints,
            HOURS_PER_SP.entrySet().stream()
                .min(Comparator.comparingInt(e -> Math.abs(e.getKey() - storyPoints)))
                .map(Map.Entry::getValue).orElse(storyPoints * 4.5));

        // Adjust for risk: HIGH risk adds 20% buffer
        if ("HIGH".equals(riskLevel))   hours *= 1.20;
        if ("MEDIUM".equals(riskLevel)) hours *= 1.10;

        String confidence = switch (riskLevel) {
            case "HIGH"   -> "LOW — consider splitting this story";
            case "MEDIUM" -> "MODERATE — discuss assumptions first";
            default       -> "HIGH — reliable estimate";
        };

        return CompletionTimeEstimate.builder()
            .storyPoints(storyPoints)
            .estimatedHours(Math.round(hours * 10.0) / 10.0)
            .estimatedDays(Math.round((hours / 8.0) * 10.0) / 10.0)
            .confidenceBand(confidence)
            .build();
    }

    /**
     * Bulk complexity analysis for all items in a project's backlog.
     */
    public BacklogAnalysisResponse analyseBacklog(Long projectId,
                                                  double predictedVelocity) {
        List<BacklogItem> items = backlogRepo.findByProjectId(projectId);

        int totalSP        = items.stream()
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int ambiguous      = (int) items.stream()
            .filter(i -> Boolean.TRUE.equals(i.getIsAmbiguous())).count();
        int highComplex    = (int) items.stream()
            .filter(i -> i.getNumComponents() != null && i.getNumComponents() >= 5).count();
        int highRisk       = (int) items.stream()
            .filter(i -> "HIGH".equals(i.getEstimationRisk())).count();
        double totalHours  = items.stream()
            .filter(i -> i.getStoryPoints() != null)
            .mapToDouble(i -> HOURS_PER_SP.getOrDefault(
                i.getStoryPoints(), i.getStoryPoints() * 4.5))
            .sum();
        int sprintsNeeded  = predictedVelocity > 0
            ? (int) Math.ceil(totalSP / predictedVelocity) : -1;

        Map<String, Integer> byPriority = new HashMap<>();
        Map<String, Integer> byStatus   = new HashMap<>();
        items.forEach(i -> {
            if (i.getPriority() != null)
                byPriority.merge(i.getPriority().name(), 1, Integer::sum);
            if (i.getStatus() != null)
                byStatus.merge(i.getStatus().name(), 1, Integer::sum);
        });

        List<BacklogItemResponse> ambiguousList = items.stream()
            .filter(i -> Boolean.TRUE.equals(i.getIsAmbiguous()))
            .map(this::toSimpleResponse)
            .toList();

        double avgSp = items.isEmpty() ? 0.0
            : Math.round((totalSP / (double) items.size()) * 100.0) / 100.0;

        return BacklogAnalysisResponse.builder()
            .projectId(projectId)
            .totalItems(items.size())
            .totalStoryPoints(totalSP)
            .featureCount((int) items.stream()
                .filter(i -> i.getTaskType() == BacklogItem.TaskType.FEATURE).count())
            .bugCount((int) items.stream()
                .filter(i -> i.getTaskType() == BacklogItem.TaskType.BUG).count())
            .enhancementCount((int) items.stream()
                .filter(i -> i.getTaskType() == BacklogItem.TaskType.ENHANCEMENT).count())
            .avgSpByTaskType(avgSp)
            .ambiguousItems(ambiguous)
            .highComplexityItems(highComplex)
            .highRiskItems(highRisk)
            .ambiguousItemList(ambiguousList)
            .sprintsNeeded(sprintsNeeded)
            .estimatedTotalHours(Math.round(totalHours * 10.0) / 10.0)
            .byPriority(byPriority)
            .byStatus(byStatus)
            .build();
    }

    private BacklogItemResponse toSimpleResponse(BacklogItem i) {
        return BacklogItemResponse.builder()
            .id(i.getId())
            .title(i.getTitle())
            .storyPoints(i.getStoryPoints())
            .estimationRisk(i.getEstimationRisk())
            .build();
    }
}
