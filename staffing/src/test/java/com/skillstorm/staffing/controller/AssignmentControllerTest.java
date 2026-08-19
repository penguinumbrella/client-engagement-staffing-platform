package com.skillstorm.staffing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CascadeAssignmentStatusRequest;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
import com.skillstorm.staffing.dto.UpdateAssignmentStatusRequest;
import com.skillstorm.staffing.enums.AssignmentStatus;
import com.skillstorm.staffing.enums.EngagementRole;
import com.skillstorm.staffing.exception.GlobalExceptionHandler;
import com.skillstorm.staffing.service.AssignmentService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    @Mock
    private AssignmentService assignmentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        AssignmentController controller = new AssignmentController(assignmentService);
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

    private AssignmentResponse sampleResponse(Long id) {
        return new AssignmentResponse(id, 1L, "Jane Doe", 10L, EngagementRole.ASSOCIATE.getLabel(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), AssignmentStatus.ACTIVE.getLabel(), false, true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void assign_returns201WithCreatedAssignment() throws Exception {
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setConsultantId(1L);
        request.setEngagementId(10L);
        request.setEngagementRole(EngagementRole.ASSOCIATE);
        request.setAssignmentStartDate(LocalDate.of(2026, 1, 1));
        request.setAssignmentEndDate(LocalDate.of(2026, 6, 1));

        when(assignmentService.assignConsultant(any(CreateAssignmentRequest.class), eq("token"))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void assign_returns400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAssignmentRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assign_returns409WhenAlreadyStaffed() throws Exception {
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setConsultantId(1L);
        request.setEngagementId(10L);
        request.setEngagementRole(EngagementRole.ASSOCIATE);
        request.setAssignmentStartDate(LocalDate.of(2026, 1, 1));
        request.setAssignmentEndDate(LocalDate.of(2026, 6, 1));

        when(assignmentService.assignConsultant(any(CreateAssignmentRequest.class), eq("token")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Consultant 1 is already staffed on engagement 10"));

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getByConsultant_returnsAssignmentsForConsultant() throws Exception {
        when(assignmentService.getAssignmentsByConsultant(1L)).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/assignments/consultant/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consultantId").value(1));
    }

    @Test
    void getByEngagement_returnsActiveAssignmentsForEngagement() throws Exception {
        when(assignmentService.getAssignmentsByEngagement(10L)).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/assignments/engagement/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].engagementId").value(10));
    }

    @Test
    void getHistoryByEngagement_returnsFullHistory() throws Exception {
        when(assignmentService.getAssignmentHistoryByEngagement(10L)).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/assignments/engagement/10/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void remove_returns204AndInvokesService() throws Exception {
        mockMvc.perform(delete("/api/assignments/1"))
                .andExpect(status().isNoContent());

        verify(assignmentService).removeAssignment(1L);
    }

    @Test
    void updateStatus_returnsUpdatedAssignment() throws Exception {
        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setStatus(AssignmentStatus.COMPLETED);

        when(assignmentService.updateStatus(eq(1L), any(UpdateAssignmentStatusRequest.class)))
                .thenReturn(sampleResponse(1L));

        mockMvc.perform(patch("/api/assignments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_returns400WhenStatusMissing() throws Exception {
        mockMvc.perform(patch("/api/assignments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAssignmentStatusRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cascadeStatus_returns204AndInvokesService() throws Exception {
        CascadeAssignmentStatusRequest request = new CascadeAssignmentStatusRequest();
        request.setEngagementStatus("In Progress");

        mockMvc.perform(patch("/api/assignments/engagement/10/cascade-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(assignmentService).cascadeStatusFromEngagement(10L, "In Progress");
    }

    @Test
    void cascadeStatus_returns400WhenEngagementStatusBlank() throws Exception {
        CascadeAssignmentStatusRequest request = new CascadeAssignmentStatusRequest();
        request.setEngagementStatus("");

        mockMvc.perform(patch("/api/assignments/engagement/10/cascade-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cascadeDelete_returns204AndInvokesService() throws Exception {
        mockMvc.perform(delete("/api/assignments/engagement/10"))
                .andExpect(status().isNoContent());

        verify(assignmentService).cascadeRemoveFromEngagement(10L);
    }
}
