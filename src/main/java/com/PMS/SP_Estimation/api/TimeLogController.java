package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.request.TimeLogRequest;
import com.PMS.SP_Estimation.dto.response.TimeLogResponse;
import com.PMS.SP_Estimation.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimeLogController {

    private final TimeLogService service;

    /** Developers log hours against a sub-task. */
    @PostMapping("/api/v1/subtasks/{subTaskId}/time-logs")
    public ResponseEntity<TimeLogResponse> log(@PathVariable Long subTaskId,
                                               @RequestBody @Valid TimeLogRequest req) {
        return ResponseEntity.ok(service.log(subTaskId, req));
    }

    @GetMapping("/api/v1/subtasks/{subTaskId}/time-logs")
    public ResponseEntity<List<TimeLogResponse>> listBySubTask(@PathVariable Long subTaskId) {
        return ResponseEntity.ok(service.listBySubTask(subTaskId));
    }

    @GetMapping("/api/v1/team/{memberId}/time-logs")
    public ResponseEntity<List<TimeLogResponse>> listByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(service.listByMember(memberId));
    }

    @GetMapping("/api/v1/time-logs/{id}")
    public ResponseEntity<TimeLogResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @DeleteMapping("/api/v1/time-logs/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
