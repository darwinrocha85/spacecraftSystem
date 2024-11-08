package com.rocha.spacecraftmanagementsystem.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql") // Carga datos de prueba en la base de datos
class SpacecraftControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetSpacecraftById_Success() throws Exception {
        mockMvc.perform(get("/api/spacecrafts/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(21)))
                .andExpect(jsonPath("$.name", is("USS Enterprise")));
    }

    @Test
    void testGetSpacecraftById_NotFound() throws Exception {
        mockMvc.perform(get("/api/spacecrafts/99"))
                .andExpect(status().isNotFound());
    }
}
