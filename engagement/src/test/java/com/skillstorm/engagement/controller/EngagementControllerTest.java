package com.skillstorm.engagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skillstorm.engagement.dto.CreateEngagementRequest;
import com.skillstorm.engagement.dto.EngagementResponse;
import com.skillstorm.engagement.dto.UpdateEngagementRequest;
import com.skillstorm.engagement.enums.EngagementStatus;
import com.skillstorm.engagement.enums.EngagementType;
import com.skillstorm.engagement.exception.GlobalExceptionHandler;
import com.skillstorm.engagement.service.EngagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EngagementControllerTest {

    @Mock
    private EngagementService engagementService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getTokenValue()).thenReturn("test-token");
        lenient().when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ENGAGEMENT_MANAGER"));
        lenient().when(jwt.getSubject()).thenReturn("11111111-1111-1111-1111-111111111111");

        EngagementController controller = new EngagementController(engagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return jwt;
                    }
                })
                .build();
    }

    private EngagementResponse sampleResponse(Long id) {
        return new EngagementResponse(id, "Audit Rollout", 10L, EngagementType.AUDIT.getLabel(), "Q1 audit",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), EngagementStatus.PLANNED.getLabel(), true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void create_returns201WithCreatedEngagement() throws Exception {
        CreateEngagementRequest request = new CreateEngagementRequest();
        request.setEngagementName("Audit Rollout");
        request.setClientId(10L);
        request.setEngagementType(EngagementType.AUDIT);
        request.setStartDate(LocalDate.of(2026, 1, 1));
        request.setTargetEndDate(LocalDate.of(2026, 6, 1));

        when(engagementService.createEngagement(any(CreateEngagementRequest.class), any(String.class), any(UUID.class))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/engagements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.engagementName").value("Audit Rollout"));
    }

    @Test
    void create_returns400WhenRequiredFieldsMissing() throws Exception {
        CreateEngagementRequest request = new CreateEngagementRequest();

        mockMvc.perform(post("/api/engagements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_returnsListOfEngagements() throws Exception {
        when(engagementService.getAllEngagements()).thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        mockMvc.perform(get("/api/engagements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_returnsEngagementWhenFound() throws Exception {
        when(engagementService.getEngagementById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/engagements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(engagementService.getEngagementById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Engagement 99 not found"));

        mockMvc.perform(get("/api/engagements/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Engagement 99 not found"));
    }

    @Test
    void getByClient_returnsEngagementsForClient() throws Exception {
        when(engagementService.getEngagementsByClientId(10L)).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/engagements/client/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientId").value(10));
    }

    @Test
    void update_returnsUpdatedEngagement() throws Exception {
        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setEngagementName("Renamed");

        when(engagementService.updateEngagement(eq(1L), any(UpdateEngagementRequest.class), any(String.class), any(UUID.class)))
                .thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/engagements/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns409WhenServiceRejectsTransition() throws Exception {
        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setStatus(EngagementStatus.CANCELLED);

        when(engagementService.updateEngagement(eq(1L), any(UpdateEngagementRequest.class), any(String.class), any(UUID.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the cancel endpoint"));

        mockMvc.perform(put("/api/engagements/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204AndInvokesService() throws Exception {
        mockMvc.perform(delete("/api/engagements/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(engagementService).deleteEngagement(eq(1L), any(String.class), any(UUID.class));
    }

    @Test
    void cancel_returnsCancelledEngagement() throws Exception {
        EngagementResponse cancelled = sampleResponse(1L);
        when(engagementService.cancelEngagement(eq(1L), any(String.class), any(UUID.class))).thenReturn(cancelled);

        mockMvc.perform(post("/api/engagements/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_returns409WhenAlreadyTerminal() throws Exception {
        when(engagementService.cancelEngagement(eq(1L), any(String.class), any(UUID.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Only active engagements can be cancelled"));

        mockMvc.perform(post("/api/engagements/1/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only active engagements can be cancelled"));
    }
}
