package com.PMS.SP_Estimation.dto.request;

import lombok.Data;

@Data
public class SprintCompletionRequest {
    /** SP actually delivered. If null, computed from DONE backlog items. */
    private Integer actualVelocity;
    /** Optional manual override; otherwise computed from DONE / planned. */
    private Double  completionRate;
    /** Bugs created during the sprint. */
    private Integer numBugsThisSprint;
    /** Items carried over (not DONE). If null, computed from non-DONE items. */
    private Integer carryoverTasks;
}
