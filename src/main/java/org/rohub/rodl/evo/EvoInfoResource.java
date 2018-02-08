package org.rohub.rodl.evo;


import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;

/**
 * REST API resource to get the evolution information of an RO.
 * 
 * @author piotrekhol
 * 
 */
@Path("evo/info")
public class EvoInfoResource {

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;


    /**
     * Get the evolution information of an RO.
     * 
     * @param researchObjectURI
     *            RO URI
     * @return ROEVO info in Turtle format
     */
    @GET
    @Produces("text/turtle")
    //TODO add content negotiation - why does it have to be Turtle?
    public Response evoInfoContent(@QueryParam("ro") URI researchObjectURI) {
        ResearchObject researchObject = ResearchObject.get(builder, researchObjectURI);
        if (researchObject == null) {
            new NotFoundException("Research Object not found");
        }
        return Response.ok(researchObject.getEvoInfo().getGraphAsInputStream(RDFFormat.TURTLE))
                .header("Content-Type", "text/turtle").build();
    }
}
