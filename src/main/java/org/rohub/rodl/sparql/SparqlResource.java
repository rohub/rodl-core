package org.rohub.rodl.sparql;



import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.auth.RequestAttribute;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.exceptions.ForbiddenException;
import org.rohub.rodl.model.Builder;

import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * A SPARQL endpoint.
 * 
 * @author Piotr Hołubowicz
 * 
 */
@Path("sparql/")
public class SparqlResource {

    /** Resource builder. */
    @RequestAttribute("Builder")
    private Builder builder;


    /**
     * Execute a SPARQL query and return an XML response.
     * 
     * @param query
     *            a SPARQL query
     * @return an XML response
     */
    @GET
    @Produces({ "application/sparql-results+xml", "application/xml", "text/xml" })
    public Response executeSparqlGetXml(@QueryParam("query") String query) {
        return executeSparql(query, SparqlEngine.SPARQL_XML);
    }


    /**
     * Execute a SPARQL query and return a JSON response.
     * 
     * @param query
     *            a SPARQL query
     * @return a JSON response
     */
    @GET
    @Produces({ "application/sparql-results+json", "application/json" })
    public Response executeSparqlGetJson(@QueryParam("query") String query) {
        return executeSparql(query, SparqlEngine.SPARQL_JSON);
    }


    /**
     * Execute a SPARQL query and return an RDF/XML response.
     * 
     * @param query
     *            a SPARQL query
     * @return an RDF/XML response
     */
    @GET
    @Produces("application/rdf+xml")
    public Response executeSparqlGetRdfXml(@QueryParam("query") String query) {
        return executeSparql(query, RDFFormat.RDFXML);
    }


    /**
     * Execute a SPARQL query and return a Turtle response.
     * 
     * @param query
     *            a SPARQL query
     * @return a Turtle response
     */
    @GET
    @Produces({ "application/x-turtle", "text/turtle" })
    public Response executeSparqlGetTurtle(@QueryParam("query") String query) {
        return executeSparql(query, RDFFormat.TURTLE);
    }


    /**
     * Execute a SPARQL query sent by POST and return an XML response.
     * 
     * @param query
     *            a SPARQL query
     * @return an XML response
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({ "application/sparql-results+xml", "application/xml", "text/xml" })
    public Response executeSparqlPostXml(@FormParam("query") String query) {
        return executeSparql(query, SparqlEngine.SPARQL_XML);
    }


    /**
     * Execute a SPARQL query sent by POST and return a JSON response.
     * 
     * @param query
     *            a SPARQL query
     * @return a JSON response
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({ "application/sparql-results+json", "application/json" })
    public Response executeSparqlPostJson(@FormParam("query") String query) {
        return executeSparql(query, SparqlEngine.SPARQL_JSON);
    }


    /**
     * Execute a SPARQL query sent by POST and return an RDF/XML response.
     * 
     * @param query
     *            a SPARQL query
     * @return an RDF/XML response
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/rdf+xml")
    public Response executeSparqlPostRdfXml(@FormParam("query") String query) {
        return executeSparql(query, RDFFormat.RDFXML);
    }


    /**
     * Execute a SPARQL query sent by POST and return a Turtle response.
     * 
     * @param query
     *            a SPARQL query
     * @return a Turtle response
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({ "application/x-turtle", "text/turtle" })
    public Response executeSparqlPostTurtle(@FormParam("query") String query) {
        return executeSparql(query, RDFFormat.TURTLE);
    }


    /**
     * Execute a SPARQL query sent by multipart POST and return an XML response.
     * 
     * @param query
     *            a SPARQL query
     * @return an XML response
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ "application/sparql-results+xml", "application/xml", "text/xml" })
    public Response executeSparqlPostXmlMulti(@FormDataParam("query") String query) {
        return executeSparql(query, SparqlEngine.SPARQL_XML);
    }


    /**
     * Execute a SPARQL query sent by multipart POST and return a JSON response.
     * 
     * @param query
     *            a SPARQL query
     * @return an JSON response
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ "application/sparql-results+json", "application/json" })
    public Response executeSparqlPostJsonMulti(@FormDataParam("query") String query) {
        return executeSparql(query, SparqlEngine.SPARQL_JSON);
    }


    /**
     * Execute a SPARQL query sent by multipart POST and return an RDF/XML response.
     * 
     * @param query
     *            a SPARQL query
     * @return an RDF/XML response
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/rdf+xml")
    public Response executeSparqlPostRdfXmlMulti(@FormDataParam("query") String query) {
        return executeSparql(query, RDFFormat.RDFXML);
    }


    /**
     * Execute a SPARQL query sent by multipart POST and return a Turtle response.
     * 
     * @param query
     *            a SPARQL query
     * @return a Turtle response
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ "application/x-turtle", "text/turtle" })
    public Response executeSparqlPostTurtleMulti(@FormDataParam("query") String query) {
        return executeSparql(query, RDFFormat.TURTLE);
    }


    /**
     * Execute a SPARQL query.
     * 
     * @param query
     *            the SPARQL query
     * @param inFormat
     *            the RDF format of the results
     * @return 200 OK or 400 Bad Request
     */
    private Response executeSparql(String query, RDFFormat inFormat) {
        SparqlEngine engine = new SparqlEngine(builder);
        try {
            QueryResult queryResult = engine.executeSparql(query, inFormat);
            RDFFormat outFormat = queryResult.getFormat();
            ContentDisposition cd = ContentDisposition.type("attachment")
                    .fileName("result." + outFormat.getDefaultFileExtension()).build();
            return Response.ok(queryResult.getInputStream()).header("Content-disposition", cd)
                    .type(outFormat.getDefaultMIMEType()).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).type("text/plain").entity(e.getMessage()).build();
        }

    }


    /**
     * Execute a SPARQL update query.
     * 
     * @param query
     *            the SPARQL query
     * @return 200 OK or 400 Bad Request
     */
    @POST
    @Path("update")
    public Response executeSparqlUpdate(String query) {
        if (builder.getUser().getRole() != UserProfile.Role.ADMIN) {
            throw new ForbiddenException("Only admin users can perform sparql updates.");
        }
        SparqlEngine engine = new SparqlEngine(builder);
        try {
            engine.executeSparqlUpdate(query);
            return Response.ok().build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).type("text/plain").entity(e.getMessage()).build();
        }

    }
}
