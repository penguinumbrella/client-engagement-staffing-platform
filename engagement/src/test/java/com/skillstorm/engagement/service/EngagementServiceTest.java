package com.skillstorm.engagement.service;

import com.skillstorm.engagement.client.AuthClient;
import com.skillstorm.engagement.client.ClientClient;
import com.skillstorm.engagement.client.StaffingClient;
import com.skillstorm.engagement.dto.CreateEngagementRequest;
import com.skillstorm.engagement.dto.EngagementResponse;
import com.skillstorm.engagement.dto.UpdateEngagementRequest;
import com.skillstorm.engagement.enums.EngagementStatus;
import com.skillstorm.engagement.enums.EngagementType;
import com.skillstorm.engagement.kafka.NotificationEvent;
import com.skillstorm.engagement.kafka.NotificationEventPublisher;
import com.skillstorm.engagement.model.Engagement;
import com.skillstorm.engagement.repository.EngagementRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {

    @Mock
    private EngagementRepository engagementRepository;

    @Mock
    private StaffingClient staffingClient;

    @Mock
    private ClientClient clientClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private EngagementService engagementService;

    private static final String TOKEN = "test-token";
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        engagementService = new EngagementService(engagementRepository, staffingClient, clientClient, authClient, notificationEventPublisher);
        lenient().when(authClient.getUsersByRole(any(), any())).thenReturn(List.of());
    }

    private Engagement activeEngagement(Long id, String status) {
        Engagement engagement = new Engagement(
                "Audit Rollout", 10L, EngagementType.AUDIT.getLabel(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), status);
        try {
            var idField = Engagement.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(engagement, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return engagement;
    }

    private CreateEngagementRequest createRequest(LocalDate start, LocalDate end, EngagementStatus status) {
        CreateEngagementRequest request = new CreateEngagementRequest();
        request.setEngagementName("Audit Rollout");
        request.setClientId(10L);
        request.setEngagementType(EngagementType.AUDIT);
        request.setSummary("Q1 audit");
        request.setStartDate(start);
        request.setTargetEndDate(end);
        request.setStatus(status);
        return request;
    }

    // ---- createEngagement ----

    @Test
    void createEngagement_savesAndReturnsResponse_withDefaultStatusWhenNull() {
        CreateEngagementRequest request = createRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), null);
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authClient.getUsersByRole(any(), any())).thenReturn(List.of(
                new com.skillstorm.engagement.dto.AuthUserResponse(UUID.randomUUID(), "Bob", "Smith", "bob@example.com", "ENGAGEMENT_MANAGER", true)));

        EngagementResponse response = engagementService.createEngagement(request, TOKEN, ACTOR_ID);

        assertThat(response.getStatus()).isEqualTo(EngagementStatus.PLANNED.getLabel());
        assertThat(response.getEngagementName()).isEqualTo("Audit Rollout");
        assertThat(response.getClientId()).isEqualTo(10L);
        verify(engagementRepository).save(any(Engagement.class));
        verify(notificationEventPublisher).publish(any(NotificationEvent.class));
    }

    @Test
    void createEngagement_usesProvidedStatusWhenPresent() {
        CreateEngagementRequest request = createRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), EngagementStatus.IN_PROGRESS);
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EngagementResponse response = engagementService.createEngagement(request, TOKEN, ACTOR_ID);

        assertThat(response.getStatus()).isEqualTo(EngagementStatus.IN_PROGRESS.getLabel());
    }

    @Test
    void createEngagement_throwsWhenStartDateAfterTargetEndDate() {
        CreateEngagementRequest request = createRequest(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> engagementService.createEngagement(request, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("startDate must not be after targetEndDate");

        verifyNoInteractions(engagementRepository);
    }

    // ---- getAllEngagements ----

    @Test
    void getAllEngagements_returnsOnlyActiveEngagementsMappedToResponses() {
        when(engagementRepository.findByActiveTrue())
                .thenReturn(List.of(activeEngagement(1L, EngagementStatus.PLANNED.getLabel())));

        List<EngagementResponse> responses = engagementService.getAllEngagements();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getAllEngagements_returnsEmptyListWhenNoneExist() {
        when(engagementRepository.findByActiveTrue()).thenReturn(List.of());

        List<EngagementResponse> responses = engagementService.getAllEngagements();

        assertThat(responses).isEmpty();
    }

    // ---- getEngagementById ----

    @Test
    void getEngagementById_returnsResponseWhenActiveEngagementExists() {
        when(engagementRepository.findById(1L))
                .thenReturn(Optional.of(activeEngagement(1L, EngagementStatus.PLANNED.getLabel())));

        EngagementResponse response = engagementService.getEngagementById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getEngagementById_throwsNotFoundWhenEngagementDoesNotExist() {
        when(engagementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engagementService.getEngagementById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Engagement 99 not found");
    }

    @Test
    void getEngagementById_throwsNotFoundWhenEngagementIsInactive() {
        Engagement inactive = activeEngagement(2L, EngagementStatus.PLANNED.getLabel());
        inactive.setActive(false);
        when(engagementRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> engagementService.getEngagementById(2L))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- getEngagementsByClientId ----

    @Test
    void getEngagementsByClientId_returnsMatchingActiveEngagements() {
        when(engagementRepository.findByClientIdAndActiveTrue(10L))
                .thenReturn(List.of(activeEngagement(1L, EngagementStatus.PLANNED.getLabel())));

        List<EngagementResponse> responses = engagementService.getEngagementsByClientId(10L);

        assertThat(responses).extracting(EngagementResponse::getClientId).containsExactly(10L);
    }

    // ---- updateEngagement ----

    @Test
    void updateEngagement_appliesPartialChangesAndPersists() {
        Engagement existing = activeEngagement(1L, EngagementStatus.PLANNED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setEngagementName("Renamed Engagement");

        EngagementResponse response = engagementService.updateEngagement(1L, request, TOKEN, ACTOR_ID);

        assertThat(response.getEngagementName()).isEqualTo("Renamed Engagement");
        verify(staffingClient, never()).cascadeAssignmentStatus(anyLong(), any(), any());
    }

    @Test
    void updateEngagement_cascadesStatusChangeToStaffingClient() {
        Engagement existing = activeEngagement(1L, EngagementStatus.PLANNED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setStatus(EngagementStatus.IN_PROGRESS);

        EngagementResponse response = engagementService.updateEngagement(1L, request, TOKEN, ACTOR_ID);

        assertThat(response.getStatus()).isEqualTo(EngagementStatus.IN_PROGRESS.getLabel());
        verify(staffingClient).cascadeAssignmentStatus(1L, EngagementStatus.IN_PROGRESS.getLabel(), TOKEN);
    }

    @Test
    void updateEngagement_doesNotCascadeWhenStatusUnchanged() {
        Engagement existing = activeEngagement(1L, EngagementStatus.PLANNED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setStatus(EngagementStatus.PLANNED);

        engagementService.updateEngagement(1L, request, TOKEN, ACTOR_ID);

        verify(staffingClient, never()).cascadeAssignmentStatus(anyLong(), any(), any());
    }

    @Test
    void updateEngagement_rejectsCancelledStatusInPlainUpdate() {
        Engagement existing = activeEngagement(1L, EngagementStatus.PLANNED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setStatus(EngagementStatus.CANCELLED);

        assertThatThrownBy(() -> engagementService.updateEngagement(1L, request, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Use the cancel endpoint");

        verify(engagementRepository, never()).save(any());
    }

    @Test
    void updateEngagement_throwsWhenResultingTimelineIsInvalid() {
        Engagement existing = activeEngagement(1L, EngagementStatus.PLANNED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateEngagementRequest request = new UpdateEngagementRequest();
        request.setStartDate(LocalDate.of(2027, 1, 1));

        assertThatThrownBy(() -> engagementService.updateEngagement(1L, request, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class);

        verify(engagementRepository, never()).save(any());
    }

    @Test
    void updateEngagement_throwsNotFoundWhenEngagementMissing() {
        when(engagementRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engagementService.updateEngagement(1L, new UpdateEngagementRequest(), TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- deleteEngagement ----

    @Test
    void deleteEngagement_softDeletesAndCascadesToStaffingClient() {
        Engagement existing = activeEngagement(1L, EngagementStatus.IN_PROGRESS.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        engagementService.deleteEngagement(1L, TOKEN, ACTOR_ID);

        ArgumentCaptor<Engagement> captor = ArgumentCaptor.forClass(Engagement.class);
        verify(engagementRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        verify(staffingClient).cascadeEngagementCancelled(1L, TOKEN);
    }

    @Test
    void deleteEngagement_throwsNotFoundWhenEngagementMissing() {
        when(engagementRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engagementService.deleteEngagement(1L, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(staffingClient);
        verify(engagementRepository, never()).save(any());
    }

    // ---- cancelEngagement ----

    @Test
    void cancelEngagement_cancelsActiveEngagementAndCascades() {
        Engagement existing = activeEngagement(1L, EngagementStatus.IN_PROGRESS.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(engagementRepository.save(any(Engagement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EngagementResponse response = engagementService.cancelEngagement(1L, TOKEN, ACTOR_ID);

        assertThat(response.getStatus()).isEqualTo(EngagementStatus.CANCELLED.getLabel());
        verify(staffingClient).cascadeEngagementCancelled(1L, TOKEN);
    }

    @Test
    void cancelEngagement_throwsConflictWhenAlreadyCompleted() {
        Engagement existing = activeEngagement(1L, EngagementStatus.COMPLETED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> engagementService.cancelEngagement(1L, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only active engagements can be cancelled");

        verify(engagementRepository, never()).save(any());
        verifyNoInteractions(staffingClient);
    }

    @Test
    void cancelEngagement_throwsConflictWhenAlreadyCancelled() {
        Engagement existing = activeEngagement(1L, EngagementStatus.CANCELLED.getLabel());
        when(engagementRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> engagementService.cancelEngagement(1L, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void cancelEngagement_throwsNotFoundWhenEngagementMissing() {
        when(engagementRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engagementService.cancelEngagement(1L, TOKEN, ACTOR_ID))
                .isInstanceOf(ResponseStatusException.class);
    }
}
