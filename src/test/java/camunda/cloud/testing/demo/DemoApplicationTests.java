package camunda.cloud.testing.demo;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.DeploymentAssert;
import io.camunda.zeebe.process.test.assertions.JobAssert;
import io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert;
import io.camunda.zeebe.process.test.extensions.ZeebeProcessTest;
import io.camunda.zeebe.process.test.testengine.InMemoryEngine;
import io.camunda.zeebe.process.test.testengine.RecordStreamSource;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
public class DemoApplicationTests {
	private InMemoryEngine engine;
	private ZeebeClient client;
	private RecordStreamSource recordStreamSource;

	private void initDeployment(){
		client.newDeployCommand()
				.addResourceFromClasspath("test-process.bpmn")
				.send()
				.join();
	}

	private ProcessInstanceAssert initProcessInstanceStart(){
		ProcessInstanceEvent event = client.newCreateInstanceCommand()
				.bpmnProcessId("TestProcess")
				.latestVersion()
				.send()
				.join();
		return BpmnAssert.assertThat(event);
	}

	@Test
	public void testDeployment() {
		//When
		DeploymentEvent event = client.newDeployCommand()
				.addResourceFromClasspath("test-process.bpmn")
				.send()
				.join();

		//Then
		BpmnAssert.assertThat(event);
	}

	@Test
	public void testProcessInstanceStart(){
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
	public void testJobAssertion()  {
		//Given
		initDeployment();
		initProcessInstanceStart();

		//When
		ActivateJobsResponse response = client.newActivateJobsCommand()
				.jobType("serviceTask")
				.maxJobsToActivate(1)
				.send()
				.join();

		//Then
		ActivatedJob activatedJob = response.getJobs().get(0);
		BpmnAssert.assertThat(activatedJob);
		client.newCompleteCommand(activatedJob.getKey()).send().join();
	}

	@Test
	public void testCompletionOfInstance() throws InterruptedException {
		//Given
		initDeployment();
		ProcessInstanceAssert instanceAssert = initProcessInstanceStart();

		//When
		ActivateJobsResponse response = client.newActivateJobsCommand()
				.jobType("serviceTask")
				.maxJobsToActivate(1)
				.send()
				.join();
		ActivatedJob activatedJob = response.getJobs().get(0);
		client.newCompleteCommand(activatedJob.getKey()).send().join();
		engine.waitForIdleState();

		//then
		instanceAssert.isCompleted();
	}
}
