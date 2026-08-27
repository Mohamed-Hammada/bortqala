package com.bemo.hr.platform.application;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.application.HrConfigurationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final HrConfigurationService hrConfigurationService;

    public SearchService(HrConfigurationService hrConfigurationService) {
        this.hrConfigurationService = hrConfigurationService;
    }

    public PlatformApi.SearchResponse search(String query, String appId) {
        String q = query.toLowerCase().trim();
        List<PlatformApi.SearchResultItem> results = new ArrayList<>();

        if (q.length() < 2) {
            return new PlatformApi.SearchResponse(results);
        }

        List<EmployeeApi.Response> employees = hrConfigurationService.listEmployees();
        for (EmployeeApi.Response emp : employees) {
            if (emp.fullName().toLowerCase().contains(q)
                    || emp.employeeCode().toLowerCase().contains(q)) {
                results.add(new PlatformApi.SearchResultItem(
                        "employee", emp.id(), emp.fullName(),
                        emp.employeeCode(), "/employees"));
                if (results.size() >= 20) break;
            }
        }

        return new PlatformApi.SearchResponse(results);
    }
}
