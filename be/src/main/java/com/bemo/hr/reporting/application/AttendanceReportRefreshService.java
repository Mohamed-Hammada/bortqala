package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.application.BiometricImportCompletedEvent;
import com.bemo.hr.employee.domain.PayCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps biometric import fast: imports only mark affected months dirty.
 * The actual monthly attendance/report calculation is performed lazily when
 * the dashboard asks for that month (or explicitly via the repair endpoint).
 * Existing ReportingService rules remain the single source of truth for
 * PRESENT/LATE/overtime/etc.; this class does not duplicate those formulas.
 */
@Service
public class AttendanceReportRefreshService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceReportRefreshService.class);

    private final ReportingService reportingService;
    private final ZoneId companyZone;
    private final Set<YearMonth> dirtyPeriods = ConcurrentHashMap.newKeySet();

    public AttendanceReportRefreshService(
            ReportingService reportingService,
            @Value("${hr.company-zone:Africa/Cairo}") String companyZone) {
        this.reportingService = reportingService;
        this.companyZone = ZoneId.of(companyZone);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterBiometricImport(BiometricImportCompletedEvent event) {
        if (event.firstPunch() == null && event.lastPunch() == null) return;
        Instant first = event.firstPunch() != null ? event.firstPunch() : event.lastPunch();
        Instant last = event.lastPunch() != null ? event.lastPunch() : event.firstPunch();
        YearMonth start = YearMonth.from(first.atZone(companyZone));
        YearMonth end = YearMonth.from(last.atZone(companyZone));
        if (end.isBefore(start)) {
            YearMonth swap = start; start = end; end = swap;
        }
        for (YearMonth period = start; !period.isAfter(end); period = period.plusMonths(1)) {
            dirtyPeriods.add(period);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void markOperationalMonthsDirtyOnStartup() {
        YearMonth current = YearMonth.now(companyZone);
        dirtyPeriods.add(current.minusMonths(1));
        dirtyPeriods.add(current);
    }

    public boolean needsRefresh(YearMonth period, boolean reportExists) {
        return !reportExists || dirtyPeriods.contains(period);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean refreshMonth(int year, int month, String actor) {
        YearMonth period = YearMonth.of(year, month);
        try {
            Method method = resolveMonthlyCalculationMethod();
            if (method == null) {
                log.warn("No unambiguous monthly ReportingService calculation method was found; {} remains dirty.", period);
                return false;
            }
            Object[] args = buildArguments(method, period, actor);
            if (args == null) {
                log.warn("ReportingService method {} could not be mapped safely for monthly refresh; {} remains dirty.", method, period);
                return false;
            }
            method.setAccessible(true);
            method.invoke(reportingService, args);
            dirtyPeriods.remove(period);
            log.info("Attendance report recalculated for {} using ReportingService method {}", period, method.getName());
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            log.warn("Could not recalculate attendance report for {}: {}", period, ex.getMessage(), ex);
            return false;
        }
    }

    private Method resolveMonthlyCalculationMethod() {
        var supported = new ArrayList<MethodScore>();
        for (Method method : ReportingService.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) continue;
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!(name.contains("recalculate") || name.contains("rebuild") || name.contains("regenerate")
                    || name.contains("generate") || name.contains("calculate") || name.contains("create")
                    || name.contains("build") || name.contains("refresh"))) continue;
            if (!canMap(method)) continue;
            int score = 0;
            if (name.contains("recalculate") || name.contains("rebuild") || name.contains("regenerate")) score += 120;
            else if (name.contains("generate") || name.contains("calculate")) score += 90;
            else if (name.contains("refresh")) score += 80;
            else if (name.contains("create")) score += 60;
            else if (name.contains("build")) score += 40;
            // Strongly prefer the existing attendance-specific monthly calculation path.
            // ReportingService can contain unrelated payroll/finance/report builders with similar verbs.
            if (name.contains("attendance")) score += 160;
            if (name.contains("month")) score += 25;
            if (name.contains("report")) score += 30;
            if (method.getReturnType().getSimpleName().toLowerCase(Locale.ROOT).contains("attendance")) score += 80;
            if (Arrays.stream(method.getParameterTypes()).anyMatch(t -> t == PayCycle.class)) score += 20;
            supported.add(new MethodScore(method, score));
        }
        if (supported.isEmpty()) return null;
        supported.sort(Comparator.comparingInt(MethodScore::score).reversed());
        if (supported.size() > 1 && supported.get(0).score() == supported.get(1).score()) {
            log.warn("Ambiguous ReportingService monthly methods: {} and {}", supported.get(0).method(), supported.get(1).method());
            return null;
        }
        return supported.get(0).method();
    }

    private boolean canMap(Method method) {
        int dates = 0;
        int ints = 0;
        int strings = 0;
        boolean periodSignal = false;
        for (Class<?> type : method.getParameterTypes()) {
            if (type == YearMonth.class || type == PayCycle.class) periodSignal = true;
            else if (type == LocalDate.class) dates++;
            else if (type == int.class || type == Integer.class) ints++;
            else if (type == String.class) strings++;
            else if (type == UUID.class) { /* optional category/branch scope; null means all */ }
            else if (type.isRecord()) {
                if (!canBuildRecord(type)) return false;
                periodSignal = true;
            } else return false;
        }
        if (dates >= 2 || ints >= 2) periodSignal = true;
        return periodSignal && strings <= 1;
    }

    private boolean canBuildRecord(Class<?> type) {
        for (RecordComponent c : type.getRecordComponents()) {
            Class<?> t = c.getType();
            if (t == String.class || t == LocalDate.class || t == YearMonth.class || t == PayCycle.class || t == UUID.class
                    || t == int.class || t == Integer.class || t == boolean.class || t == Boolean.class || t.isEnum()) continue;
            return false;
        }
        return true;
    }

    private Object[] buildArguments(Method method, YearMonth period, String actor) throws ReflectiveOperationException {
        Object[] args = new Object[method.getParameterCount()];
        int dateIndex = 0;
        int intIndex = 0;
        int stringCount = (int) Arrays.stream(method.getParameterTypes()).filter(t -> t == String.class).count();
        for (int i = 0; i < args.length; i++) {
            Class<?> type = method.getParameterTypes()[i];
            if (type == YearMonth.class) args[i] = period;
            else if (type == PayCycle.class) args[i] = PayCycle.MONTHLY;
            else if (type == LocalDate.class) args[i] = dateIndex++ == 0 ? period.atDay(1) : period.atEndOfMonth();
            else if (type == int.class || type == Integer.class) args[i] = intIndex++ == 0 ? period.getYear() : period.getMonthValue();
            else if (type == String.class && stringCount == 1) args[i] = actor == null ? "system" : actor;
            else if (type == UUID.class) args[i] = null;
            else if (type.isRecord()) args[i] = buildRecord(type, period, actor);
            else return null;
        }
        return args;
    }

    private Object buildRecord(Class<?> type, YearMonth period, String actor) throws ReflectiveOperationException {
        RecordComponent[] components = type.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        int unnamedDate = 0;
        int unnamedInt = 0;
        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            Class<?> t = component.getType();
            types[i] = t;
            String name = component.getName().toLowerCase(Locale.ROOT);
            if (t == YearMonth.class) args[i] = period;
            else if (t == PayCycle.class) args[i] = PayCycle.MONTHLY;
            else if (t == LocalDate.class) {
                if (name.contains("end") || name.equals("to")) args[i] = period.atEndOfMonth();
                else if (name.contains("start") || name.equals("from")) args[i] = period.atDay(1);
                else args[i] = unnamedDate++ == 0 ? period.atDay(1) : period.atEndOfMonth();
            } else if (t == int.class || t == Integer.class) {
                if (name.contains("month")) args[i] = period.getMonthValue();
                else if (name.contains("year")) args[i] = period.getYear();
                else args[i] = unnamedInt++ == 0 ? period.getYear() : period.getMonthValue();
            } else if (t == String.class) {
                if (name.contains("actor") || name.contains("user") || name.endsWith("by")) args[i] = actor;
                else if (name.contains("cycle")) args[i] = "MONTHLY";
                else args[i] = null;
            } else if (t == UUID.class) args[i] = null;
            else if (t == boolean.class || t == Boolean.class) args[i] = false;
            else if (t.isEnum()) args[i] = Arrays.stream(t.getEnumConstants())
                    .filter(v -> ((Enum<?>) v).name().equals("MONTHLY")).findFirst().orElse(null);
            else return null;
        }
        Constructor<?> constructor = type.getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    private record MethodScore(Method method, int score) {}
}
