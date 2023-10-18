package tab;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

public class RestApplicationEventListener implements ApplicationEventListener {
	private volatile int requestCnt = 0;
	@Override
	public void onEvent(ApplicationEvent event) {
		switch (event.getType()) {
	        case INITIALIZATION_FINISHED:
	            System.out.println("Application "
	                    + event.getResourceConfig().getApplicationName()
	                    + " was initialized.");
	            break;
	        case DESTROY_FINISHED:
	            System.out.println("Application " 
	            		+ event.getResourceConfig().getApplicationName() 
	            		+ " destroyed.");
	            break;
		default:
			 System.out.println("Application " 
	            		+ event.getResourceConfig().getApplicationName() 
	            		+ " eventType:" + event.getType());
			break;
	    }
	}

	@Override
	public RequestEventListener onRequest(RequestEvent arg0) {
		requestCnt++;
        System.out.println("Request " + requestCnt + " started.");
        return new RestRequestEventListener(requestCnt);
	}

}
