package org.jmb;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class EchoResource {

    @Inject
    Configuration configuration;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response processRequest(@QueryParam("message") String message) throws InterruptedException {
        Thread.sleep(configuration.delayInMiliseconds());
        var lagMessage = String.format("%d ms", configuration.delayInMiliseconds());
        return new Response(lagMessage, "OK", message);
    }
}