package com.skillstorm.staffing.service;

import com.skillstorm.staffing.client.AuthClient;
import com.skillstorm.staffing.dto.AuthUserResponse;
import com.skillstorm.staffing.dto.ConsultantResponse;
import com.skillstorm.staffing.dto.CreateConsultantRequest;
import com.skillstorm.staffing.dto.UpdateConsultantRequest;
import com.skillstorm.staffing.enums.SkillArea;
import com.skillstorm.staffing.model.Consultant;
import com.skillstorm.staffing.repository.ConsultantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultantServiceTest {

    @Mock
    private ConsultantRepository consultantRepository;

    @Mock
    private AuthClient authClient;

    /*
     * Let Mockito create ConsultantService using its current constructor.
     *
     * This avoids hard-coding the old:
     *
     * new ConsultantService(consultantRepository)
     *
     * which no longer matches the production constructor.
     */
    @InjectMocks
    private ConsultantService consultantService;

    private Consultant activeConsultant(Long id) {

        Consultant consultant = createConsultant();

        consultant.setName("Jane Doe");
        consultant.setTitleRole("Senior Consultant");
        consultant.setPrimarySkillArea(
                SkillArea.AUDIT.getLabel()
        );
        consultant.setActive(true);

        setId(consultant, id);

        return consultant;
    }

    /*
     * Consultant's no-argument constructor is not public,
     * so the test creates it reflectively instead of changing
     * the production entity just for testing.
     */
    private Consultant createConsultant() {

        try {
            Constructor<Consultant> constructor =
                    Consultant.class.getDeclaredConstructor();

            constructor.setAccessible(true);

            return constructor.newInstance();

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setId(
            Consultant consultant,
            Long id) {

        try {
            var idField =
                    Consultant.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(consultant, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createConsultant_savesAndReturnsResponse() {

        CreateConsultantRequest request =
                new CreateConsultantRequest();

        request.setName("Jane Doe");
        request.setTitleRole("Senior Consultant");
        request.setPrimarySkillArea(
                SkillArea.AUDIT
        );
        request.setEmail("jane.doe@example.com");

        when(
                authClient.getUserByEmail(
                        "jane.doe@example.com",
                        "test-token"
                )
        ).thenReturn(
                new AuthUserResponse(
                        UUID.randomUUID(),
                        "Jane",
                        "Doe",
                        "jane.doe@example.com",
                        "CONSULTANT",
                        true
                )
        );

        when(
                consultantRepository.findByUserId(any())
        ).thenReturn(Optional.empty());

        when(
                consultantRepository.save(
                        any(Consultant.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ConsultantResponse response =
                consultantService.createConsultant(
                        request,
                        "test-token"
                );

        assertThat(response.getName())
                .isEqualTo("Jane Doe");

        assertThat(response.getTitleRole())
                .isEqualTo("Senior Consultant");

        assertThat(response.getPrimarySkillArea())
                .isEqualTo(
                        SkillArea.AUDIT.getLabel()
                );
    }

    @Test
    void getAllConsultants_returnsOnlyActiveConsultants() {
        Pageable pageable = PageRequest.of(0, 10);

        when(consultantRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeConsultant(1L)), pageable, 1));

        Page<ConsultantResponse> responses =
                consultantService.getAllConsultants(0, 10, null);

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getAllConsultants_returnsEmptyPageWhenNoneExist() {
        when(consultantRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(consultantService.getAllConsultants(0, 10, null).getContent()).isEmpty();
    }

    @Test
    void getAllConsultants_filtersBySkillAreaWhenProvided() {
        Pageable pageable = PageRequest.of(0, 10);

        when(consultantRepository.findByActiveTrueAndPrimarySkillAreaIn(eq(List.of("Audit")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeConsultant(1L)), pageable, 1));

        Page<ConsultantResponse> responses =
                consultantService.getAllConsultants(0, 10, List.of("Audit"));

        assertThat(responses.getContent()).hasSize(1);
        verify(consultantRepository, never()).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void getConsultantById_returnsResponseWhenActiveConsultantExists() {

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(
                        activeConsultant(1L)
                )
        );

        ConsultantResponse response =
                consultantService
                        .getConsultantById(1L);

        assertThat(response.getId())
                .isEqualTo(1L);
    }

    @Test
    void getConsultantById_throwsNotFoundWhenMissing() {

        when(
                consultantRepository.findById(99L)
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                consultantService
                        .getConsultantById(99L)
        )
                .isInstanceOf(
                        ResponseStatusException.class
                )
                .hasMessageContaining(
                        "Consultant 99 not found"
                );
    }

    @Test
    void getConsultantById_throwsNotFoundWhenInactive() {

        Consultant inactive =
                activeConsultant(1L);

        inactive.setActive(false);

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(inactive)
        );

        assertThatThrownBy(() ->
                consultantService
                        .getConsultantById(1L)
        )
                .isInstanceOf(
                        ResponseStatusException.class
                );
    }

    @Test
    void updateConsultant_appliesOnlyProvidedFields() {

        Consultant existing =
                activeConsultant(1L);

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                consultantRepository.save(
                        any(Consultant.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        UpdateConsultantRequest request =
                new UpdateConsultantRequest();

        request.setName("Jane Smith");

        ConsultantResponse response =
                consultantService
                        .updateConsultant(
                                1L,
                                request
                        );

        assertThat(response.getName())
                .isEqualTo("Jane Smith");

        assertThat(response.getTitleRole())
                .isEqualTo(
                        "Senior Consultant"
                );
    }

    @Test
    void updateConsultant_updatesSkillAreaWhenProvided() {

        Consultant existing =
                activeConsultant(1L);

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                consultantRepository.save(
                        any(Consultant.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        UpdateConsultantRequest request =
                new UpdateConsultantRequest();

        request.setPrimarySkillArea(
                SkillArea.TECHNOLOGY
        );

        ConsultantResponse response =
                consultantService
                        .updateConsultant(
                                1L,
                                request
                        );

        assertThat(
                response.getPrimarySkillArea()
        ).isEqualTo(
                SkillArea.TECHNOLOGY.getLabel()
        );
    }

    @Test
    void updateConsultant_throwsNotFoundWhenMissing() {

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                consultantService
                        .updateConsultant(
                                1L,
                                new UpdateConsultantRequest()
                        )
        )
                .isInstanceOf(
                        ResponseStatusException.class
                );
    }

    @Test
    void deleteConsultant_softDeletesConsultant() {

        Consultant existing =
                activeConsultant(1L);

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(existing)
        );

        consultantService.deleteConsultant(1L);

        ArgumentCaptor<Consultant> captor =
                ArgumentCaptor.forClass(
                        Consultant.class
                );

        verify(
                consultantRepository
        ).save(
                captor.capture()
        );

        assertThat(
                captor.getValue().isActive()
        ).isFalse();
    }

    @Test
    void deleteConsultant_throwsNotFoundWhenMissing() {

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                consultantService
                        .deleteConsultant(1L)
        )
                .isInstanceOf(
                        ResponseStatusException.class
                );

        verify(
                consultantRepository,
                never()
        ).save(any());
    }

    @Test
    void findActiveOrThrow_returnsActiveConsultant() {

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(
                        activeConsultant(1L)
                )
        );

        Consultant consultant =
                consultantService
                        .findActiveOrThrow(1L);

        assertThat(consultant.getId())
                .isEqualTo(1L);
    }

    @Test
    void findActiveOrThrow_throwsNotFoundWhenInactive() {

        Consultant inactive =
                activeConsultant(1L);

        inactive.setActive(false);

        when(
                consultantRepository.findById(1L)
        ).thenReturn(
                Optional.of(inactive)
        );

        assertThatThrownBy(() ->
                consultantService
                        .findActiveOrThrow(1L)
        )
                .isInstanceOf(
                        ResponseStatusException.class
                );
    }
}