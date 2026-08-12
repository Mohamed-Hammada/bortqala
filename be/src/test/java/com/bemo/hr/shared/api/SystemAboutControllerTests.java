package com.bemo.hr.shared.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAboutControllerTests {

    @Test
    void returnsSafeSystemAboutMetadata() {
        SystemAboutController controller = new SystemAboutController();
        var response = controller.getSystemAbout();

        assertThat(response).isNotNull();
        assertThat(response.productName()).isEqualTo("BEMO ERP");
        assertThat(response.version()).isEqualTo("1.8.7");
        assertThat(response.apiVersion()).isEqualTo("v1");
        assertThat(response.supportEnabled()).isTrue();
    }
}
