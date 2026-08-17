package com.bemo.hr.reporting.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.reporting.api.AttendanceExceptionApi;
import com.bemo.hr.reporting.domain.*;
import com.bemo.hr.reporting.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class AttendanceExceptionService {
    private final AttendancePolicyRepository policyRepository;
    private final AttendanceExceptionRepository exceptionRepository;
    private final AttendanceReportRepository reportRepository;
    private final DailyAttendanceResultRepository resultRepository;
    private final AuditService auditService;

    @Transactional(readOnly=true)
    public List<AttendanceExceptionApi.PolicyResponse> policies(){return policyRepository.findAllByOrderByPriorityDescEffectiveFromDesc().stream().map(this::policy).toList();}

    @Transactional
    public AttendanceExceptionApi.PolicyResponse createPolicy(AttendanceExceptionApi.PolicyRequest request,String actor){
        validatePolicy(request);
        AttendancePolicy saved=policyRepository.save(new AttendancePolicy(request.name(),request.scopeType(),request.scopeId(),
                request.effectiveFrom(),request.effectiveTo(),request.priority(),request.lateThresholdMinutes(),
                request.earlyThresholdMinutes(),request.maxShiftMinutes(),request.missingPunchScore(),request.singlePunchScore(),
                request.lateScore(),request.earlyScore(),request.payrollBlockScore(),request.active()));
        auditService.record("CREATE","ATTENDANCE_POLICY",saved.getId(),actor,"{\"scope\":\""+saved.getScopeType()+"\"}",null);
        return policy(saved);
    }

    @Transactional
    public int detect(String reportId, String actor) {
        requireReport(reportId);
        List<AttendancePolicy> policies = policyRepository.findAllByOrderByPriorityDescEffectiveFromDesc();
        Set<String> existingKeys = exceptionRepository.findByReportIdOrderByScoreDescWorkDateAsc(reportId).stream()
                .map(e -> e.getDailyResultId() + ":" + e.getExceptionType())
                .collect(Collectors.toSet());
        List<AttendanceException> toSave = new ArrayList<>();
        for (DailyAttendanceResult result : resultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(reportId)) {
            AttendancePolicy policy = resolve(policies, result);
            for (Candidate candidate : candidates(result, policy)) {
                String key = result.getId() + ":" + candidate.type();
                if (!existingKeys.contains(key)) {
                    existingKeys.add(key);
                    toSave.add(new AttendanceException(result, candidate.type(), candidate.score(), candidate.key(), policy,
                            candidate.score() >= policy.getPayrollBlockScore()));
                }
            }
        }
        if (!toSave.isEmpty()) {
            exceptionRepository.saveAll(toSave);
        }
        int created = toSave.size();
        auditService.record("DETECT", "ATTENDANCE_EXCEPTION", reportId, actor, "{\"created\":" + created + "}", null);
        return created;
    }

    @Transactional(readOnly=true)
    public AttendanceExceptionApi.WorkbenchResponse workbench(String reportId){
        requireReport(reportId);
        List<AttendanceException> exceptions=exceptionRepository.findByReportIdOrderByScoreDescWorkDateAsc(reportId);
        Map<String,DailyAttendanceResult> results=resultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(reportId).stream()
                .collect(Collectors.toMap(DailyAttendanceResult::getId,Function.identity()));
        List<AttendanceExceptionApi.ExceptionView> views=exceptions.stream().map(e->view(e,results.get(e.getDailyResultId()))).toList();
        int open=(int)exceptions.stream().filter(e->e.getStatus()==AttendanceExceptionStatus.OPEN).count();
        int critical=(int)exceptions.stream().filter(e->e.getStatus()==AttendanceExceptionStatus.OPEN&&e.isPayrollBlocking()).count();
        int resolved=exceptions.size()-open;
        int employees=(int)exceptions.stream().map(AttendanceException::getEmployeeId).distinct().count();
        return new AttendanceExceptionApi.WorkbenchResponse(new AttendanceExceptionApi.Summary(exceptions.size(),open,critical,resolved,employees),views);
    }

    @Transactional(readOnly=true)
    public AttendanceExceptionApi.BulkPreview preview(String reportId,AttendanceExceptionApi.BulkRequest request){
        requireEditable(reportId);
        List<String> requestedIds=distinctIds(request.exceptionIds());
        List<AttendanceException> selected=selected(reportId,requestedIds);
        Set<String> editableIds=selected.stream().filter(e->e.getStatus()==AttendanceExceptionStatus.OPEN||e.replay(request.operationId())).map(AttendanceException::getId).collect(Collectors.toSet());
        List<String> excluded=requestedIds.stream().filter(id->!editableIds.contains(id)).toList();
        int editable=(int)selected.stream().filter(e->e.getStatus()==AttendanceExceptionStatus.OPEN).count();
        int blockers=(int)selected.stream().filter(e->e.getStatus()==AttendanceExceptionStatus.OPEN&&e.isPayrollBlocking()).count();
        return new AttendanceExceptionApi.BulkPreview(requestedIds.size(),editable,excluded.size(),blockers,excluded);
    }

    @Transactional
    public AttendanceExceptionApi.BulkResult apply(String reportId,AttendanceExceptionApi.BulkRequest request,String actor){
        AttendanceReport report=requireEditable(reportId);
        List<String> requestedIds=distinctIds(request.exceptionIds());
        List<AttendanceException> selected=selectedForUpdate(reportId,requestedIds);
        int applied=0,replayed=0,skipped=requestedIds.size()-selected.size();
        for(AttendanceException exception:selected){
            if(exception.replay(request.operationId())){replayed++;continue;}
            if(exception.getStatus()!=AttendanceExceptionStatus.OPEN){skipped++;continue;}
            DailyAttendanceResult result=resultRepository.findById(exception.getDailyResultId())
                    .orElseThrow(()->error("ATTENDANCE_RESULT_NOT_FOUND",HttpStatus.NOT_FOUND));
            applyResolution(result,request.resolution(),request.reason(),actor);
            exception.resolve(request.resolution(),request.reason(),request.operationId(),actor);
            resultRepository.save(result);exceptionRepository.save(exception);applied++;
        }
        int unresolved=(int)resultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(reportId).stream().filter(DailyAttendanceResult::isBlocking).count();
        report.updateUnresolvedCount(unresolved);reportRepository.save(report);
        auditService.record("BULK_RESOLVE","ATTENDANCE_EXCEPTION",reportId,actor,
                "{\"operationId\":\""+request.operationId()+"\",\"applied\":"+applied+",\"resolution\":\""+request.resolution()+"\"}",null);
        return new AttendanceExceptionApi.BulkResult(workbench(reportId),applied,replayed,skipped);
    }

    @Transactional(readOnly=true)
    public void assertPayrollReady(String reportId,String employeeId){
        if(reportId==null)return;
        AttendanceReport report=requireReport(reportId);
        if(report.getStatus()!=ReportStatus.APPROVED&&report.getStatus()!=ReportStatus.EXPORTED){
            throw error("PAYROLL_ATTENDANCE_REPORT_NOT_APPROVED",HttpStatus.CONFLICT);
        }
        long blockers=employeeId==null?exceptionRepository.countByReportIdAndStatusAndPayrollBlockingTrue(reportId,AttendanceExceptionStatus.OPEN)
                :exceptionRepository.countByReportIdAndEmployeeIdAndStatusAndPayrollBlockingTrue(reportId,employeeId,AttendanceExceptionStatus.OPEN);
        if(blockers>0)throw error("PAYROLL_ATTENDANCE_EXCEPTIONS_OPEN",HttpStatus.CONFLICT);
    }

    @Transactional(readOnly=true)
    public void assertNoCriticalOpen(String reportId){
        if(exceptionRepository.countByReportIdAndStatusAndPayrollBlockingTrue(reportId,AttendanceExceptionStatus.OPEN)>0){
            throw error("RPT_CRITICAL_ATTENDANCE_EXCEPTIONS",HttpStatus.CONFLICT);
        }
    }

    private AttendancePolicy resolve(List<AttendancePolicy> policies,DailyAttendanceResult result){
        return policies.stream().filter(p->p.applies(result.getEmployeeId(),result.getCategoryId(),result.getWorkDate()))
                .sorted(Comparator.comparingInt(AttendancePolicy::specificity).reversed()
                        .thenComparing(AttendancePolicy::getPriority,Comparator.reverseOrder())
                        .thenComparing(AttendancePolicy::getEffectiveFrom,Comparator.reverseOrder())
                        .thenComparing(AttendancePolicy::getId)).findFirst()
                .orElseGet(AttendancePolicy::defaultPolicy);
    }
    private List<Candidate> candidates(DailyAttendanceResult r,AttendancePolicy p){
        List<Candidate> out=new ArrayList<>();
        if(r.getStatus()==DailyStatus.NO_PUNCH)out.add(new Candidate(AttendanceExceptionType.NO_PUNCH,p.getMissingPunchScore(),"attendance.exception.noPunch"));
        if(r.getStatus()==DailyStatus.SINGLE_PUNCH)out.add(new Candidate(AttendanceExceptionType.SINGLE_PUNCH,p.getSinglePunchScore(),"attendance.exception.singlePunch"));
        if(r.getStatus()==DailyStatus.MISSING_SCHEDULE)out.add(new Candidate(AttendanceExceptionType.MISSING_SCHEDULE,100,"attendance.exception.missingSchedule"));
        if(r.getLateMinutes()>p.getLateThresholdMinutes())out.add(new Candidate(AttendanceExceptionType.LATE,scaled(p.getLateScore(),r.getLateMinutes()-p.getLateThresholdMinutes()),"attendance.exception.late"));
        if(r.getEarlyLeaveMinutes()>p.getEarlyThresholdMinutes())out.add(new Candidate(AttendanceExceptionType.EARLY_LEAVE,scaled(p.getEarlyScore(),r.getEarlyLeaveMinutes()-p.getEarlyThresholdMinutes()),"attendance.exception.earlyLeave"));
        if(r.getWorkedMinutes()>p.getMaxShiftMinutes())out.add(new Candidate(AttendanceExceptionType.EXCESS_SHIFT,80,"attendance.exception.excessShift"));
        return out;
    }
    private static int scaled(int base,int excess){return Math.min(100,base+Math.min(40,excess/15*5));}
    private void applyResolution(DailyAttendanceResult r,AttendanceExceptionResolution resolution,String reason,String actor){
        switch(resolution){
            case MARK_PRESENT->r.decide(AttendanceDecision.NORMAL_DAY,r.getExpectedMinutes(),reason,actor);
            case MARK_ABSENT->r.decide(AttendanceDecision.ABSENCE,0,reason,actor);
            case ACCEPT->{if(r.isBlocking())r.decide(r.getStatus()==DailyStatus.NO_PUNCH?AttendanceDecision.ABSENCE:AttendanceDecision.NORMAL_DAY,r.getStatus()==DailyStatus.NO_PUNCH?0:r.getExpectedMinutes(),reason,actor);}
            case IGNORE->{ }
        }
    }
    private List<AttendanceException> selected(String reportId,List<String> ids){
        Map<String,AttendanceException> found=exceptionRepository.findAllById(ids).stream().filter(e->e.getReportId().equals(reportId)).collect(Collectors.toMap(AttendanceException::getId,Function.identity()));
        return ids.stream().map(found::get).filter(Objects::nonNull).toList();
    }
    private List<AttendanceException> selectedForUpdate(String reportId,List<String> ids){
        Map<String,AttendanceException> found=exceptionRepository.findAllByIdForUpdate(ids).stream().filter(e->e.getReportId().equals(reportId)).collect(Collectors.toMap(AttendanceException::getId,Function.identity()));
        return ids.stream().map(found::get).filter(Objects::nonNull).toList();
    }
    private static List<String> distinctIds(List<String> ids){return new ArrayList<>(new LinkedHashSet<>(ids));}
    private AttendanceReport requireReport(String id){return reportRepository.findById(id).orElseThrow(()->error("RPT_NOT_FOUND",HttpStatus.NOT_FOUND));}
    private AttendanceReport requireEditable(String id){AttendanceReport r=requireReport(id);if(r.getStatus()!=ReportStatus.IN_REVIEW)throw error("ATTENDANCE_EXCEPTION_PERIOD_LOCKED",HttpStatus.CONFLICT);return r;}
    private void validatePolicy(AttendanceExceptionApi.PolicyRequest r){if(r.scopeType()!=AttendancePolicyScope.TENANT&&(r.scopeId()==null||r.scopeId().isBlank())||r.effectiveTo()!=null&&r.effectiveTo().isBefore(r.effectiveFrom()))throw error("ATTENDANCE_POLICY_INVALID",HttpStatus.BAD_REQUEST);}
    private AttendanceExceptionApi.PolicyResponse policy(AttendancePolicy p){return new AttendanceExceptionApi.PolicyResponse(p.getId(),p.getName(),p.getScopeType(),p.getScopeId(),p.getEffectiveFrom(),p.getEffectiveTo(),p.getPriority(),p.getLateThresholdMinutes(),p.getEarlyThresholdMinutes(),p.getMaxShiftMinutes(),p.getMissingPunchScore(),p.getSinglePunchScore(),p.getLateScore(),p.getEarlyScore(),p.getPayrollBlockScore(),p.isActive(),p.getVersion());}
    private AttendanceExceptionApi.ExceptionView view(AttendanceException e,DailyAttendanceResult r){return new AttendanceExceptionApi.ExceptionView(e.getId(),e.getReportId(),e.getDailyResultId(),e.getEmployeeId(),r==null?"—":r.getEmployeeName(),e.getCategoryId(),r==null?"—":r.getCategoryName(),e.getWorkDate(),e.getExceptionType(),e.getScore(),metric(e,r),e.getExplanationKey(),e.getPolicyId(),e.getPolicyName(),e.getPolicyVersion(),e.getPolicySnapshotJson(),e.getPolicyScope(),e.isPayrollBlocking(),e.getStatus(),e.getResolution(),e.getReason(),e.getVersion());}
    private static int metric(AttendanceException e,DailyAttendanceResult r){if(r==null)return 0;return switch(e.getExceptionType()){case LATE->r.getLateMinutes();case EARLY_LEAVE->r.getEarlyLeaveMinutes();case EXCESS_SHIFT->r.getWorkedMinutes();default->r.getExpectedMinutes();};}
    private static BusinessRuleException error(String code,HttpStatus status){return new BusinessRuleException(code,code,status);}
    private record Candidate(AttendanceExceptionType type,int score,String key){}
}
