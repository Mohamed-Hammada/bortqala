package com.bemo.hr.product.pack;

import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class IndustryImportTemplateRegistry {
    private final Map<String, TemplateDescriptor> templates = new LinkedHashMap<>();

    public IndustryImportTemplateRegistry() {
        register(new TemplateDescriptor("WORKERS", "workers.xlsx", "workforce-workers", true, "/workforce/import"));
        register(new TemplateDescriptor("ATTENDANCE", "attendance.xlsx", "attendance", true, "/imports"));
        register(new TemplateDescriptor("ADVANCES", "advances.xlsx", "advances", true, "/workforce/advances"));
        register(new TemplateDescriptor("ITEMS", "items.xlsx", "items", true, "/smart-import/items"));
        register(new TemplateDescriptor("CUSTOMERS", "customers.xlsx", "parties", true, "/smart-import/parties"));
        register(new TemplateDescriptor("OPENING_STOCK", "opening-stock.xlsx", "items", true, "/smart-import/items"));
        register(new TemplateDescriptor("SUPPLIERS", "suppliers.xlsx", "parties", true, "/smart-import/parties"));
    }

    private void register(TemplateDescriptor descriptor) {
        templates.put(descriptor.fileName().toLowerCase(Locale.ROOT), descriptor);
        templates.put(descriptor.key().toLowerCase(Locale.ROOT), descriptor);
    }

    public boolean supports(String nameOrKey) {
        if (nameOrKey == null) return false;
        return templates.containsKey(nameOrKey.toLowerCase(Locale.ROOT));
    }

    public void validateTemplates(List<String> namesOrKeys) {
        if (namesOrKeys != null) {
            for (String item : namesOrKeys) {
                if (!supports(item)) {
                    log.warn("Unknown import template in industry pack: {}", item);
                    throw new BusinessRuleException("INDUSTRY_PACK_TEMPLATE_UNKNOWN", "INDUSTRY_PACK_TEMPLATE_UNKNOWN", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    public List<TemplateDescriptor> resolveTemplates(List<String> namesOrKeys) {
        if (namesOrKeys == null || namesOrKeys.isEmpty()) {
            return List.of();
        }
        List<TemplateDescriptor> list = new ArrayList<>();
        for (String item : namesOrKeys) {
            var desc = templates.get(item.toLowerCase(Locale.ROOT));
            if (desc != null) {
                list.add(desc);
            }
        }
        return list;
    }

    public record TemplateDescriptor(
            String key,
            String fileName,
            String workflow,
            boolean downloadable,
            String route
    ) {}
}
