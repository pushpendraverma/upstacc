package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.TestStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class ConsultationControllerTest {

    @Autowired
    ConsultationController consultationController;

    @Autowired
    LabRequestController labRequestController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
        TestRequest testRequestAssigned =  consultationController.assignForConsultation(testRequest.getRequestId());
        assertThat(testRequest.getRequestId(), is(testRequestAssigned.getRequestId()));
        assertThat(RequestStatus.DIAGNOSIS_IN_PROCESS, is(testRequestAssigned.getStatus()));
        assertNotNull(testRequestAssigned.getConsultation());
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception(){
        Long InvalidRequestId= -34L;
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            consultationController.assignForConsultation(InvalidRequestId);
        });
        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        testRequest.setStatus(RequestStatus.COMPLETED);
        TestRequest testRequestUpdated = consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);
        assertThat(testRequest.getRequestId(), is(testRequestUpdated.getRequestId()));
        assertThat(RequestStatus.COMPLETED, is(testRequestUpdated.getStatus()));
        assertThat(createConsultationRequest.getSuggestion(), is(testRequestUpdated.getConsultation().getSuggestion()));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception(){
        Long invalidRequestId= -34L;
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            consultationController.updateConsultation(invalidRequestId, createConsultationRequest);
        });
        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        createConsultationRequest.setSuggestion(null);
        assertThrows(ResponseStatusException.class, () -> {
            consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);
        });
    }

    private CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();
        if (null != testRequest.getLabResult() && null != testRequest.getLabResult().getResult() && TestStatus.POSITIVE == testRequest.getLabResult().getResult()) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
            createConsultationRequest.setComments("Take care");
        } else {
            createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
            createConsultationRequest.setComments("Ok");
        }
        return createConsultationRequest;
    }

    private TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }
}