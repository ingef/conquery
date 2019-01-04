package com.bakdata.conquery.resources.admin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ServiceLoader;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import lombok.Getter;

@Getter
public class AdminUIServlet {
	
	private DatasetsProcessor datasetsProcessor;

	public void register(MasterCommand masterCommand) {
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(masterCommand.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");
		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));
		 
		masterCommand.getEnvironment().admin().addServlet("admin", servletContainerHolder.getContainer()).addMapping("/admin/*");
		
		jerseyConfig.register(new JacksonMessageBodyProvider(masterCommand.getEnvironment().getObjectMapper()));
		
		//jerseyConfig.getSingletons().add(new UnitOfWorkResourceMethodDispatchAdapter(hibernateBundle.getSessionFactory()));
		jerseyConfig.register(new AdminUIResource(masterCommand.getConfig(), masterCommand.getNamespaces(), masterCommand.getJobManager()));
		DatasetsResource datasets = new DatasetsResource(masterCommand.getConfig(), masterCommand.getStorage(), masterCommand.getNamespaces(), masterCommand.getJobManager());
		datasetsProcessor = datasets.getProcessor();
		jerseyConfig.register(datasets);
		jerseyConfig.register(new JobsResource(masterCommand.getJobManager()));
		jerseyConfig.register(new MultiPartFeature());
		jerseyConfig.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), ServiceLoader.load(ViewRenderer.class)));
		
		jerseyConfig.register(new ParamConverterProvider() {
			@Override
			public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
				if(IId.class.isAssignableFrom(rawType)) {
					return new ParamConverter<T>() {
						
						private final Parser parser = IId.createParser((Class)rawType);
						
						@Override
						public T fromString(String value) {
							return (T) parser.parse(value);
						}

						@Override
						public String toString(T value) {
							return value.toString();
						}
					};
				}
				return null;
			}
		});
	}
}
