package org.rohub.rodl;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.accesscontrol.AccessModeResource;
import org.rohub.rodl.accesscontrol.PermissionResource;
import org.rohub.rodl.evo.CopyResource;
import org.rohub.rodl.evo.EvoInfoResource;
import org.rohub.rodl.evo.FinalizeResource;
import org.rohub.rodl.vocabulary.AccessControlService;
import org.rohub.rodl.vocabulary.RO;
import org.rohub.rodl.vocabulary.ROEVOService;
import org.rohub.rodl.zip.ROFromZipResource;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.view.Viewable;

/**
 * The base URI of RODL.
 * 
 * @author piotrekhol
 * 
 */
@Path("/")
public class RootResource {

	/**
	 * Return the main HTML page.
	 * 
	 * @param uriInfo
	 *            URI info
	 * @return an HTML page
	 */
	@GET
	@Produces("text/html")
	public Response index(@Context UriInfo uriInfo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("version", ApplicationProperties.getVersion());
		map.put("rosrsuri", uriInfo.getAbsolutePathBuilder().path("ROs/").build());
		map.put("roevouri", uriInfo.getAbsolutePathBuilder().path("evo/").build());
		return Response.ok(new Viewable("/index", map)).build();
	}

	/**
	 * Get a service description as an RDF graph.
	 * 
	 * @param uriInfo
	 *            injected context information
	 * @param accept
	 *            accept header
	 * @return RDF service description, format subject to content-negotiation
	 */
	@GET
	@Produces({ "application/rdf+xml", "text/turtle", "*/*" })
	public Response getServiceDescription(@Context UriInfo uriInfo,
			@HeaderParam("Accept") String accept) {
		RDFFormat format = RDFFormat.forMIMEType(accept, RDFFormat.RDFXML);

		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		Resource service = model.createResource(uriInfo.getAbsolutePath().toString());

		URI basePermissionsUri = uriInfo.getAbsolutePathBuilder().path(PermissionResource.class)
				.build();
		Literal permissionsTpl = model.createLiteral(basePermissionsUri.toString() + "{?ro}");
		service.addProperty(AccessControlService.permissions, permissionsTpl);

		URI baseModesUri = uriInfo.getAbsolutePathBuilder().path(AccessModeResource.class).build();
		Literal modesTpl = model.createLiteral(baseModesUri.toString() + "{?ro}");
		service.addProperty(AccessControlService.modes, modesTpl);

		URI copyUri = uriInfo.getAbsolutePathBuilder().path(CopyResource.class).build();
		Literal copyTpl = model.createLiteral(copyUri.toString());
		URI finalizeUri = uriInfo.getAbsolutePathBuilder().path(FinalizeResource.class).build();
		Literal finalizeTpl = model.createLiteral(finalizeUri.toString());
		URI infoUri = uriInfo.getAbsolutePathBuilder().path(EvoInfoResource.class).build();
		Literal infoTpl = model.createLiteral(infoUri.toString() + "{?ro}");
		service.addProperty(ROEVOService.copy, copyTpl);
		service.addProperty(ROEVOService.finalize, finalizeTpl);
		service.addProperty(ROEVOService.info, infoTpl);

		URI zipUri = uriInfo.getAbsolutePathBuilder().path(ROFromZipResource.class).build();
		service.addProperty(RO.createFromZip, zipUri.resolve("create").toString());
		service.addProperty(RO.uploadAZip, zipUri.resolve("upload").toString());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out, format.getName().toUpperCase(), null);
		InputStream description = new ByteArrayInputStream(out.toByteArray());

		return Response.ok(description, format.getDefaultMIMEType()).build();
	}
}
