package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.TestStatus;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class LabRequestControllerTest {

    @Autowired
    LabRequestController labRequestController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);
        TestRequest assignedTestRequest = labRequestController.assignForLabTest(testRequest.getRequestId());
        assertThat(testRequest.getRequestId(), is(assignedTestRequest.getRequestId()));
        assertThat(RequestStatus.INITIATED, is(assignedTestRequest.getStatus()));
        assertNotNull(assignedTestRequest.getLabResult());
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){
        Long InvalidRequestId= -34L;
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
                    labRequestController.assignForLabTest(InvalidRequestId);
        });
        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        TestRequest testRequestUpdated = labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);

        assertThat(testRequest.getRequestId(), is(testRequestUpdated.getRequestId()));
        assertThat(RequestStatus.LAB_TEST_COMPLETED, is(testRequestUpdated.getStatus()));
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){
        Long InvalidRequestId= -34L;
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            labRequestController.updateLabTest(InvalidRequestId, createLabResult);
        });
        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);
        });
        assertThat("ConstraintViolationException", containsString(responseStatusException.getMessage()));
    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {
        CreateLabResult createLabResult = new CreateLabResult();
        createLabResult.setBloodPressure("133 mm");
        if (null != testRequest.getConsultation()) {
            createLabResult.setComments(testRequest.getConsultation().getComments());
        }
        createLabResult.setHeartBeat("120 bpm");
        createLabResult.setOxygenLevel("100 mmhg");
        createLabResult.setTemperature("99 c");
        createLabResult.setResult(TestStatus.NEGATIVE);
        return createLabResult;
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        List<TestRequest> testRequests = testRequestQueryService.findBy(status);
        if (null != testRequests && testRequests.size() > 0) {
            return testRequestQueryService.findBy(status).stream().findFirst().get();
        } else {
            return null;
        }
    }
}