package camunda.cloud.testing.demo;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableZeebeClient
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@ZeebeWorker(type = "serviceTask")
	public void handleJobFoo(final JobClient client, final ActivatedJob job) {
		client.newCompleteCommand(job.getKey())
				.variables("{}")
				.send()
				.exceptionally( throwable -> { throw new RuntimeException("Could not complete job " + job, throwable); });
	}
}