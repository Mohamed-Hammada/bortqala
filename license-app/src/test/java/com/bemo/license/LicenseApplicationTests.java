package com.bemo.license;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:license;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "license.admin.key=test-admin-key", "license.signing.allow-ephemeral=true"
})
class LicenseApplicationTests {
    @Test void contextLoads() { }
}
