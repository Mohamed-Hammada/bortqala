package org.example.zkteco.gateway.adms;

import org.example.zkteco.adapter.adms.AdmsIngressStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdmsPushControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AdmsIngressStore store;

    @Test
    void capturesRawCdataPayload() throws Exception {
        mockMvc.perform(post("/iclock/cdata")
                        .queryParam("SN", "TEST-001")
                        .contentType("text/plain")
                        .content("table=ATTLOG\n1\t2026-08-06 08:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        assertEquals("TEST-001", store.recent(1).getFirst().serialNumber());
    }
}
