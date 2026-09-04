package com.skillstorm.staffing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.staffing.dto.ConsultantResponse;
import com.skillstorm.staffing.dto.CreateConsultantRequest;
import com.skillstorm.staffing.dto.UpdateConsultantRequest;
import com.skillstorm.staffing.enums.SkillArea;
import com.skillstorm.staffing.exception.GlobalExceptionHandler;
import com.skillstorm.staffing.service.ConsultantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConsultantControllerTest {

    @Mock
    private ConsultantService consultantService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ConsultantController controller = new ConsultantController(consultantService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new JwtArgumentResolver())
                .build();
    }

    private static class JwtArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Jwt.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"), Map.of("sub", "test-user"));
        }
    }

    private ConsultantResponse sampleResponse(Long id) {
        return new ConsultantResponse(id, "Jane Doe", "Senior Consultant", SkillArea.AUDIT.getLabel(), true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void create_returns201WithCreatedConsultant() throws Exception {
        CreateConsultantRequest request = new CreateConsultantRequest();
        request.setName("Jane Doe");
        request.setEmail("jane.doe@example.com");
        request.setTitleRole("Senior Consultant");
        request.setPrimarySkillArea(SkillArea.AUDIT);

        when(consultantService.createConsultant(any(CreateConsultantRequest.class), eq("token"))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/consultants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void create_returns400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/consultants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateConsultantRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_returnsPageOfConsultants() throws Exception {
        PageImpl<ConsultantResponse> page = new PageImpl<>(
                List.of(sampleResponse(1L), sampleResponse(2L)),
                PageRequest.of(0, 10),
                2);

        when(consultantService.getAllConsultants(eq(0), eq(10), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/consultants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAll_passesPageAndSizeQueryParams() throws Exception {
        when(consultantService.getAllConsultants(eq(1), eq(5), isNull()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/consultants").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        verify(consultantService).getAllConsultants(1, 5, null);
    }

    @Test
    void getById_returnsConsultantWhenFound() throws Exception {
        when(consultantService.getConsultantById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/consultants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(consultantService.getConsultantById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultant 99 not found"));

        mockMvc.perform(get("/api/consultants/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Consultant 99 not found"));
    }

    @Test
    void update_returnsUpdatedConsultant() throws Exception {
        UpdateConsultantRequest request = new UpdateConsultantRequest();
        request.setName("Jane Smith");

        when(consultantService.updateConsultant(eq(1L), any(UpdateConsultantRequest.class)))
                .thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/consultants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns404WhenConsultantMissing() throws Exception {
        when(consultantService.updateConsultant(eq(1L), any(UpdateConsultantRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultant 1 not found"));

        mockMvc.perform(put("/api/consultants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateConsultantRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204AndInvokesService() throws Exception {
        mockMvc.perform(delete("/api/consultants/1"))
                .andExpect(status().isNoContent());

        verify(consultantService).deleteConsultant(1L);
    }
}
