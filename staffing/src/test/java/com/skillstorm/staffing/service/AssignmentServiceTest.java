package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.EngagementClient;
import com.skillstorm.staffing.dto.AssignmentResponse;
import com.skillstorm.staffing.dto.CreateAssignmentRequest;
import com.skillstorm.staffing.dto.UpdateAssignmentStatusRequest;
import com.skillstorm.staffing.enums.AssignmentStatus;
import com.skillstorm.staffing.enums.EngagementRole;
import com.skillstorm.staffing.enums.SkillArea;
import com.skillstorm.staffing.kafka.NotificationEvent;
import com.skillstorm.staffing.kafka.NotificationEventPublisher;
import com.skillstorm.staffing.model.Assignment;
import com.skillstorm.staffing.model.Consultant;
import com.skillstorm.staffing.repository.AssignmentRepository;
import com.skillstorm.staffing.repository.ConsultantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ConsultantRepository consultantRepository;

    @Mock
    private ConsultantService consultantService;

    @Mock
    private EngagementClient engagementClient;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(assignmentRepository, consultantRepository, consultantService, engagementClient, notificationEventPublisher);
    }

    private Consultant consultant(Long id) {
        Consultant consultant = createConsultant();

        setField(consultant, "id", id);
        setField(consultant, "name", "Jane Doe");
        setField(consultant, "titleRole", "Senior Consultant");
        setField(
                consultant,
                "primarySkillArea",
                SkillArea.AUDIT.getLabel()
        );

        return consultant;
    }

    private Consultant createConsultant() {
        try {
            var constructor =
                    Consultant.class.getDeclaredConstructor();

            constructor.setAccessible(true);

            return constructor.newInstance();

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(
            Object target,
            String fieldName,
            Object value) {

        try {
            var field =
                    target.getClass()
                            .getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(target, value);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setId(
            Object target,
            Class<?> type,
            Long id) {

        setField(target, type, "id", id);
    }

    private void setField(
            Object target,
            Class<?> type,
            String fieldName,
            Object value) {

        try {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Assignment assignment(Long id, Long consultantId, Long engagementId, String status, boolean active) {
        Assignment assignment = new Assignment(consultantId, engagementId, EngagementRole.ASSOCIATE.getLabel(),
                LocalDate.of(2026, 1, 1));
        assignment.setAssignmentEndDate(LocalDate.of(2026, 6, 1));
        assignment.setStatus(status);
        assignment.setActive(active);
        setId(assignment, Assignment.class, id);
        return assignment;
    }

    private CreateAssignmentRequest createRequest(Long consultantId, Long engagementId, AssignmentStatus status) {
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setConsultantId(consultantId);
        request.setEngagementId(engagementId);
        request.setEngagementRole(EngagementRole.ASSOCIATE);
        request.setAssignmentStartDate(LocalDate.of(2026, 1, 1));
        request.setAssignmentEndDate(LocalDate.of(2026, 6, 1));
        request.setStatus(status);
        return request;
    }

    // ---- assignConsultant ----

    @Test
    void assignConsultant_createsNewAssignmentWithDefaultStatusWhenNull() {
        when(consultantService.findActiveOrThrow(1L)).thenReturn(consultant(1L));
        when(engagementClient.engagementExists(10L,"test")).thenReturn(true);
        when(assignmentRepository.findByConsultantIdAndEngagementId(1L, 10L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResponse response = assignmentService.assignConsultant(createRequest(1L, 10L, null),"test");

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ACTIVE.getLabel());
        assertThat(response.getConsultantName()).isEqualTo("Jane Doe");
        verify(notificationEventPublisher).publish(any(NotificationEvent.class));
    }

    @Test
    void assignConsultant_usesProvidedStatus() {
        when(consultantService.findActiveOrThrow(1L)).thenReturn(consultant(1L));
        when(engagementClient.engagementExists(10L,"test")).thenReturn(true);
        when(assignmentRepository.findByConsultantIdAndEngagementId(1L, 10L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResponse response = assignmentService.assignConsultant(createRequest(1L, 10L, AssignmentStatus.PENDING),"test");

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.PENDING.getLabel());
    }

    @Test
    void assignConsultant_throwsNotFoundWhenEngagementDoesNotExist() {
        when(consultantService.findActiveOrThrow(1L)).thenReturn(consultant(1L));
        when(engagementClient.engagementExists(10L,"test")).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.assignConsultant(createRequest(1L, 10L, null),"test"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Engagement 10 not found");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void assignConsultant_reactivatesInactiveExistingAssignment() {
        when(consultantService.findActiveOrThrow(1L)).thenReturn(consultant(1L));
        when(engagementClient.engagementExists(10L,"test")).thenReturn(true);
        Assignment inactive = assignment(5L, 1L, 10L, AssignmentStatus.CANCELLED.getLabel(), false);
        when(assignmentRepository.findByConsultantIdAndEngagementId(1L, 10L)).thenReturn(Optional.of(inactive));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResponse response = assignmentService.assignConsultant(createRequest(1L, 10L, AssignmentStatus.ACTIVE),"test");

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.isActive()).isTrue();
        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ACTIVE.getLabel());
    }

    @Test
    void assignConsultant_throwsConflictWhenAlreadyActivelyStaffed() {
        when(consultantService.findActiveOrThrow(1L)).thenReturn(consultant(1L));
        when(engagementClient.engagementExists(10L,"test")).thenReturn(true);
        Assignment active = assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true);
        when(assignmentRepository.findByConsultantIdAndEngagementId(1L, 10L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> assignmentService.assignConsultant(createRequest(1L, 10L, null),"test"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already staffed");

        verify(assignmentRepository, never()).save(any());
    }

    // ---- getAssignmentsByConsultant ----

    @Test
    void getAssignmentsByConsultant_returnsMappedResponses() {
        when(consultantService.findActiveOrThrow(1L)).thenReturn(consultant(1L));
        when(assignmentRepository.findByConsultantIdAndActiveTrue(1L))
                .thenReturn(List.of(assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true)));

        List<AssignmentResponse> responses = assignmentService.getAssignmentsByConsultant(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getConsultantName()).isEqualTo("Jane Doe");
    }

    @Test
    void getAssignmentsByConsultant_throwsNotFoundWhenConsultantMissing() {
        when(consultantService.findActiveOrThrow(99L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Consultant 99 not found"));

        assertThatThrownBy(() -> assignmentService.getAssignmentsByConsultant(99L))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- getAssignmentsByEngagement ----

    @Test
    void getAssignmentsByEngagement_resolvesConsultantNamesForEachAssignment() {
        when(assignmentRepository.findByEngagementIdAndActiveTrue(10L))
                .thenReturn(List.of(assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true)));
        when(consultantRepository.findAllById(List.of(1L))).thenReturn(List.of(consultant(1L)));

        List<AssignmentResponse> responses = assignmentService.getAssignmentsByEngagement(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getConsultantName()).isEqualTo("Jane Doe");
    }

    @Test
    void getAssignmentsByEngagement_returnsEmptyWhenNoneExist() {
        when(assignmentRepository.findByEngagementIdAndActiveTrue(10L)).thenReturn(List.of());
        when(consultantRepository.findAllById(List.of())).thenReturn(List.of());

        assertThat(assignmentService.getAssignmentsByEngagement(10L)).isEmpty();
    }

    // ---- getAssignmentHistoryByEngagement ----

    @Test
    void getAssignmentHistoryByEngagement_includesInactiveAssignments() {
        when(assignmentRepository.findByEngagementId(10L))
                .thenReturn(List.of(assignment(5L, 1L, 10L, AssignmentStatus.CANCELLED.getLabel(), false)));
        when(consultantRepository.findAllById(List.of(1L))).thenReturn(List.of(consultant(1L)));

        List<AssignmentResponse> responses = assignmentService.getAssignmentHistoryByEngagement(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isFalse();
    }

    // ---- cascadeRemoveFromEngagement ----

    @Test
    void cascadeRemoveFromEngagement_deactivatesAndCancelsAllActiveAssignments() {
        Assignment a1 = assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true);
        Assignment a2 = assignment(6L, 2L, 10L, AssignmentStatus.PENDING.getLabel(), true);
        when(assignmentRepository.findByEngagementIdAndActiveTrue(10L)).thenReturn(List.of(a1, a2));

        assignmentService.cascadeRemoveFromEngagement(10L);

        assertThat(a1.isActive()).isFalse();
        assertThat(a1.getStatus()).isEqualTo(AssignmentStatus.CANCELLED.getLabel());
        assertThat(a2.isActive()).isFalse();
        verify(assignmentRepository).saveAll(List.of(a1, a2));
    }

    @Test
    void cascadeRemoveFromEngagement_doesNothingWhenNoActiveAssignments() {
        when(assignmentRepository.findByEngagementIdAndActiveTrue(10L)).thenReturn(List.of());

        assignmentService.cascadeRemoveFromEngagement(10L);

        verify(assignmentRepository).saveAll(List.of());
    }

    // ---- removeAssignment ----

    @Test
    void removeAssignment_deactivatesAndCancelsAssignment() {
        Assignment existing = assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true);
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(existing));

        assignmentService.removeAssignment(5L);

        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getStatus()).isEqualTo(AssignmentStatus.CANCELLED.getLabel());
    }

    @Test
    void removeAssignment_throwsNotFoundWhenMissing() {
        when(assignmentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.removeAssignment(5L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void removeAssignment_throwsNotFoundWhenAlreadyInactive() {
        Assignment inactive = assignment(5L, 1L, 10L, AssignmentStatus.CANCELLED.getLabel(), false);
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> assignmentService.removeAssignment(5L))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- updateStatus ----

    @Test
    void updateStatus_marksOverriddenAndResolvesConsultantName() {
        Assignment existing = assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true);
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consultantRepository.findById(1L)).thenReturn(Optional.of(consultant(1L)));

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setStatus(AssignmentStatus.COMPLETED);

        AssignmentResponse response = assignmentService.updateStatus(5L, request);

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.COMPLETED.getLabel());
        assertThat(response.isStatusOverridden()).isTrue();
        assertThat(response.getConsultantName()).isEqualTo("Jane Doe");
    }

    @Test
    void updateStatus_returnsNullConsultantNameWhenConsultantMissing() {
        Assignment existing = assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true);
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consultantRepository.findById(1L)).thenReturn(Optional.empty());

        UpdateAssignmentStatusRequest request = new UpdateAssignmentStatusRequest();
        request.setStatus(AssignmentStatus.COMPLETED);

        AssignmentResponse response = assignmentService.updateStatus(5L, request);

        assertThat(response.getConsultantName()).isNull();
    }

    @Test
    void updateStatus_throwsNotFoundWhenAssignmentMissing() {
        when(assignmentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.updateStatus(5L, new UpdateAssignmentStatusRequest()))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- cascadeStatusFromEngagement ----

    @Test
    void cascadeStatusFromEngagement_cascadesPendingForPlanned() {
        Assignment a1 = assignment(5L, 1L, 10L, AssignmentStatus.PENDING.getLabel(), true);
        when(assignmentRepository.findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(10L)).thenReturn(List.of(a1));

        assignmentService.cascadeStatusFromEngagement(10L, "Planned");

        assertThat(a1.getStatus()).isEqualTo(AssignmentStatus.PENDING.getLabel());
        verify(assignmentRepository).saveAll(List.of(a1));
    }

    @Test
    void cascadeStatusFromEngagement_cascadesActiveForInProgress() {
        Assignment a1 = assignment(5L, 1L, 10L, AssignmentStatus.PENDING.getLabel(), true);
        when(assignmentRepository.findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(10L)).thenReturn(List.of(a1));

        assignmentService.cascadeStatusFromEngagement(10L, "In Progress");

        assertThat(a1.getStatus()).isEqualTo(AssignmentStatus.ACTIVE.getLabel());
    }

    @Test
    void cascadeStatusFromEngagement_cascadesCompletedForCompleted() {
        Assignment a1 = assignment(5L, 1L, 10L, AssignmentStatus.ACTIVE.getLabel(), true);
        when(assignmentRepository.findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(10L)).thenReturn(List.of(a1));

        assignmentService.cascadeStatusFromEngagement(10L, "Completed");

        assertThat(a1.getStatus()).isEqualTo(AssignmentStatus.COMPLETED.getLabel());
    }

    @Test
    void cascadeStatusFromEngagement_doesNothingForOnHold() {
        assignmentService.cascadeStatusFromEngagement(10L, "On Hold");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void cascadeStatusFromEngagement_doesNothingForUnrecognizedStatus() {
        assignmentService.cascadeStatusFromEngagement(10L, "Some Unknown Status");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void cascadeStatusFromEngagement_doesNothingForNullStatus() {
        assignmentService.cascadeStatusFromEngagement(10L, null);

        verifyNoInteractions(assignmentRepository);
    }
}
