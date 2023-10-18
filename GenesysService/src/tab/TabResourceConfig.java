package tab;

import org.glassfish.jersey.server.ResourceConfig;
import main.Runner;

public class TabResourceConfig extends ResourceConfig {
	public TabResourceConfig(){
		register(AuthenticationFilter.class);
		register(tab.rbac.RbacSystem.class);
		register(tab.oauth2.OAuth2System.class);
		register(tab.rec.RecSystem.class);
		register(tab.rec.FLPRecSystem.class);
		register(tab.rest.CalloutServer.class);
		register(tab.rest.ReportServer.class);
		register(tab.rest.AsrtransServer.class);
		register(com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider.class);
		register(org.glassfish.jersey.jetty.JettyHttpContainerProvider.class);
		register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
		register(new tab.RestApplicationEventListener());
		switch(Runner.nJerseyLogLevel){
		case 0:
			break;
		case 1:
			break;
		case 2:
			property(org.glassfish.jersey.server.ServerProperties.TRACING, "OFF");
			property(org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL, "INFO");
			break;
		case 3:
			property(org.glassfish.jersey.server.ServerProperties.TRACING, "ON_DEMAND");
			property(org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL, "WARNING");
			break;
		default:
			property(org.glassfish.jersey.server.ServerProperties.TRACING, "ALL");
			property(org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL, "SEVERE");
			break;
		}
	}
}
