package com.bakdata.conquery.resources.admin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll
@Path("/")
@AuthCookie
public class JobsResource {
	
	private final JobManager jobManager;
	
	@POST @Path("/jobs") @Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addDemoJob() {
		jobManager.addSlowJob(new Job() {
			private final UUID id = UUID.randomUUID();
			@Override
			public void execute() {
				while(!progressReporter.isDone()) {
					progressReporter.report(0.01d);
					if(progressReporter.getProgress()>=1) {
						progressReporter.done();
					}
					Uninterruptibles.sleepUninterruptibly((int)(Math.random()*200), TimeUnit.SECONDS);
				}
			}

			@Override
			public String getLabel() {
				return "Demo "+id;
			}
		});
		
		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(AdminUIResource.class, "getJobs").build())
			.build();
	}
}
