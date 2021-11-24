package camunda.cloud.testing.demo;

import io.camunda.testing.assertions.BpmnAssert;
import io.camunda.testing.assertions.DeploymentAssert;
import io.camunda.testing.assertions.JobAssert;
import io.camunda.testing.assertions.ProcessInstanceAssert;
import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.camunda.community.eze.ZeebeEngineClock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ZeebeAssertions
class DemoApplicationTests {
	private ZeebeClient client;
	private ZeebeEngine engine;
	private ZeebeEngineClock clock;
	private RecordStreamSource recordStreamSource;

	void initDeployment(){
		client.newDeployCommand()
				.addResourceFromClasspath("test-process.bpmn")
				.send()
				.join();
	}

	ProcessInstanceAssert initProcessInstanceStart(){
		ProcessInstanceEvent event = client.newCreateInstanceCommand()
				.bpmnProcessId("TestProcess")
				.latestVersion()
				.send()
				.join();
		return BpmnAssert.assertThat(event);
	}

	@Test
	void testDeployment() {
		//When
		DeploymentEvent event = client.newDeployCommand()
				.addResourceFromClasspath("test-process.bpmn")
				.send()
				.join();

		//Then
		DeploymentAssert assertions = BpmnAssert.assertThat(event);
	}

	@Test
	void testProcessInstanceStart(){
		//Given
		initDeployment();

		//When
		ProcessInstanceEvent event = client.newCreateInstanceCommand()
				.bpmnProcessId("TestProcess")
				.latestVersion()
				.send()
				.join();

		//Then
		ProcessInstanceAssert assertions = BpmnAssert.assertThat(event);
		assertions.hasPassedElement("StartEvent_1");
	}

	@Test
	void testJobAssertion() throws InterruptedException {
		//Given
		initDeployment();
		initProcessInstanceStart();
		Thread.sleep(100);

		//When
		ActivateJobsResponse response = client.newActivateJobsCommand()
				.jobType("serviceTask")
				.maxJobsToActivate(1)
				.send()
				.join();

		//Then
		ActivatedJob activatedJob = response.getJobs().get(0);
		JobAssert assertions = BpmnAssert.assertThat(activatedJob);
		client.newCompleteCommand(activatedJob.getKey()).send().join();
	}

	@Test
	void testCompletionOfInstance() throws InterruptedException {
		//Given
		initDeployment();
		ProcessInstanceAssert instanceAssert = initProcessInstanceStart();
		Thread.sleep(100);

		//When
		ActivateJobsResponse response = client.newActivateJobsCommand()
				.jobType("serviceTask")
				.maxJobsToActivate(1)
				.send()
				.join();
		ActivatedJob activatedJob = response.getJobs().get(0);
		client.newCompleteCommand(activatedJob.getKey()).send().join();
		Thread.sleep(100);

		//then
		instanceAssert.isCompleted();
	}
}
