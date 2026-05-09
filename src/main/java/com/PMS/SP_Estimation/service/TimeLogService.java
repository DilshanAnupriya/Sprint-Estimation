package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.TimeLogRequest;
import com.PMS.SP_Estimation.dto.response.TimeLogResponse;
import com.PMS.SP_Estimation.entity.SubTask;
import com.PMS.SP_Estimation.entity.TeamMember;
import com.PMS.SP_Estimation.entity.TimeLog;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.SubTaskRepository;
import com.PMS.SP_Estimation.repo.TeamMemberRepository;
import com.PMS.SP_Estimation.repo.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeLogService {

    private final TimeLogRepository    timeLogRepo;
    private final SubTaskRepository    subTaskRepo;
    private final TeamMemberRepository teamRepo;
    private final SubTaskService       subTaskService;

    public TimeLogResponse log(Long subTaskId, TimeLogRequest req) {
        SubTask subTask = subTaskRepo.findById(subTaskId)
            .orElseThrow(() -> new ResourceNotFoundException("Sub-task not found: " + subTaskId));
        TeamMember member = teamRepo.findById(req.getTeamMemberId())
            .orElseThrow(() -> new ResourceNotFoundException("Team member not found: " + req.getTeamMemberId()));

        TimeLog log = TimeLog.builder()
            .subTask(subTask)
            .teamMember(member)
            .hours(req.getHours())
            .workDate(req.getWorkDate() != null ? req.getWorkDate() : LocalDate.now())
            .description(req.getDescription())
            .createdAt(LocalDateTime.now())
            .build();
        TimeLog saved = timeLogRepo.save(log);

        // Update sub-task running total
        double total = timeLogRepo.findBySubTaskId(subTaskId).stream()
            .mapToDouble(TimeLog::getHours).sum();
        subTaskService.recomputeActualHours(subTaskId, total);

        return toResponse(saved);
    }

    public List<TimeLogResponse> listBySubTask(Long subTaskId) {
        return timeLogRepo.findBySubTaskId(subTaskId).stream().map(this::toResponse).toList();
    }

    public List<TimeLogResponse> listByMember(Long memberId) {
        return timeLogRepo.findByTeamMemberId(memberId).stream().map(this::toResponse).toList();
    }

    public TimeLogResponse get(Long id) {
        return toResponse(timeLogRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Time log not found: " + id)));
    }

    public void delete(Long id) {
        TimeLog log = timeLogRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Time log not found: " + id));
        Long subTaskId = log.getSubTask() != null ? log.getSubTask().getId() : null;
        timeLogRepo.deleteById(id);
        if (subTaskId != null) {
            double total = timeLogRepo.findBySubTaskId(subTaskId).stream()
                .mapToDouble(TimeLog::getHours).sum();
            subTaskService.recomputeActualHours(subTaskId, total);
        }
    }

    private TimeLogResponse toResponse(TimeLog log) {
        return TimeLogResponse.builder()
            .id(log.getId())
            .subTaskId(log.getSubTask() != null ? log.getSubTask().getId() : null)
            .subTaskTitle(log.getSubTask() != null ? log.getSubTask().getTitle() : null)
            .teamMemberId(log.getTeamMember() != null ? log.getTeamMember().getId() : null)
            .teamMemberName(log.getTeamMember() != null ? log.getTeamMember().getName() : null)
            .hours(log.getHours())
            .workDate(log.getWorkDate())
            .description(log.getDescription())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
