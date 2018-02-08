package org.rohub.rodl.model.RO;


import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.AO.Annotation;
import org.rohub.rodl.model.ORE.AggregatedResource;
import org.rohub.rodl.model.ORE.ResourceMap;
import org.rohub.rodl.model.RDF.Thing;
import org.rohub.rodl.vocabulary.AO;
import org.rohub.rodl.vocabulary.FOAF;
import org.rohub.rodl.vocabulary.ORE;
import org.rohub.rodl.vocabulary.RO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * ro:Manifest.
 * 
 */
public class Manifest extends ResourceMap {

	/**
	 * Constructor.
	 * 
	 * @param user
	 *            user creating the instance
	 * @param dataset
	 *            custom dataset
	 * @param useTransactions
	 *            should transactions be used. Note that not using transactions
	 *            on a dataset which already uses transactions may make it
	 *            unreadable.
	 * @param uri
	 *            manifest uri
	 * @param researchObject
	 *            research object being described
	 */
	public Manifest(UserMetadata user, Dataset dataset, boolean useTransactions, URI uri,
			ResearchObject researchObject) {
		super(user, dataset, useTransactions, researchObject, uri);
	}

	@Override
	public void save() {
		super.save();
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			model.createIndividual(aggregation.getUri().toString(), RO.ResearchObject);
			model.createIndividual(uri.toString(), RO.Manifest);
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	@Override
	public void delete() {
		super.delete();
	}

	/**
	 * Create and save a new manifest.
	 * 
	 * @param builder
	 *            model instance builder
	 * @param uri
	 *            manifest URI
	 * @param researchObject
	 *            research object that is described
	 * @return a new manifest
	 */
	public static Manifest create(Builder builder, URI uri, ResearchObject researchObject) {
		Manifest manifest = builder.buildManifest(uri, researchObject, builder.getUser(),
				DateTime.now());
		manifest.save();
		manifest.serialize();
		return manifest;
	}

	/**
	 * Create an initial manifest copying the static facts (creation date and
	 * author) from the provided manifest.
	 * 
	 * @param builder
	 *            model instance builder
	 * @param researchObject
	 *            research object described by the new manifest
	 * @return the new manifest
	 */
	public Manifest copy(Builder builder, ResearchObject researchObject) {
		URI manifestUri = researchObject.getUri().resolve(this.getRawPath());
		Manifest manifest = builder.buildManifest(manifestUri, researchObject, getCreator(),
				getCreated());
		manifest.save();
		return manifest;
	}

	/**
	 * Save the ro:Resource RDF class for an aggregated resource.
	 * 
	 * @param resource
	 *            the ro:Resource
	 */
	public void saveRoResourceClass(AggregatedResource resource) {
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			model.createIndividual(resource.getUri().toString(), RO.Resource);
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Remove the ro:Resource RDF class for an aggregated ro:Resource.
	 * 
	 * @param resource
	 *            the ro:Resource
	 */
	public void removeRoResourceClass(AggregatedResource resource) {
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			Individual resourceR = model.getIndividual(resource.getUri().toString());
			resourceR.removeRDFType(RO.Resource);
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Save the data specific to ro:Folder in the manifest.
	 * 
	 * @param folder
	 *            the ro:Folder
	 */
	public void saveFolderData(Folder folder) {
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			Individual folderInd = model.createIndividual(folder.getUri().toString(), RO.Folder);
			com.hp.hpl.jena.rdf.model.Resource folderMapR = model.createResource(folder
					.getResourceMap().getUri().toString());
			folderInd.addProperty(ORE.isDescribedBy, folderMapR);
			if (folder.isRootFolder()) {
				Individual ro = model.getIndividual(getResearchObject().getUri().toString());
				ro.addProperty(RO.rootFolder, folderInd);
			}
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Save the metadata of the serialization.
	 * 
	 * @param resource
	 *            a resource
	 */
	public void saveRoStats(AggregatedResource resource) {
		if (resource.getStats() != null) {
			boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
			try {
				com.hp.hpl.jena.rdf.model.Resource resourceR = model.getResource(resource.getUri()
						.toString());
				if (resource.getStats().getName() != null) {
					model.add(resourceR, RO.name,
							model.createTypedLiteral(resource.getStats().getName()));
				}
				model.add(resourceR, RO.filesize,
						model.createTypedLiteral(resource.getStats().getSizeInBytes()));
				if (resource.getStats().getChecksum() != null
						&& resource.getStats().getDigestMethod() != null) {
					model.add(resourceR, RO.checksum, model.createResource(String.format(
							"urn:%s:%s", resource.getStats().getDigestMethod(), resource.getStats()
									.getChecksum())));
				}
				commitTransaction(transactionStarted);
			} finally {
				endTransaction(transactionStarted);
			}
		}
	}

	/**
	 * Identify all aggregated resource, reusing the existing instances where
	 * possible.
	 * 
	 * @param resources2
	 *            ro:Resource instances
	 * @param folders2
	 *            ro:Folder instances
	 * @param annotations2
	 *            ro:AggregatedAnnotation instances
	 * @return all aggregated resources
	 */
	public Map<URI, AggregatedResource> extractAggregatedResources(Map<URI, Resource> resources2,
			Map<URI, Folder> folders2, Map<URI, Annotation> annotations2) {
		boolean transactionStarted = beginTransaction(ReadWrite.READ);
		try {
			Map<URI, AggregatedResource> aggregated = new HashMap<>();
			String queryString = String
					.format("PREFIX ore: <%s> PREFIX dcterms: <%s> PREFIX foaf: <%s> SELECT ?resource ?proxy ?created ?creator ?creatorname WHERE { <%s> ore:aggregates ?resource . OPTIONAL { ?resource a ore:AggregatedResource . } OPTIONAL { ?proxy ore:proxyFor ?resource . } OPTIONAL { ?resource dcterms:creator ?creator . OPTIONAL { ?creator foaf:name ?creatorname . } } OPTIONAL { ?resource dcterms:created ?created . } }",
							ORE.NAMESPACE, DCTerms.NS, FOAF.NAMESPACE, aggregation.getUri()
									.toString());

			Query query = QueryFactory.create(queryString);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			try {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution solution = results.next();
					AggregatedResource resource;
					RDFNode r = solution.get("resource");
					URI rUri = URI.create(r.asResource().getURI());
					if (resources2.containsKey(rUri)) {
						resource = resources2.get(rUri);
					} else if (folders2.containsKey(rUri)) {
						resource = folders2.get(rUri);
					} else if (annotations2.containsKey(rUri)) {
						resource = annotations2.get(rUri);
					} else {
						RDFNode p = solution.get("proxy");
						URI pUri = p != null ? URI.create(p.asResource().getURI()) : null;
						RDFNode creatorNode = solution.get("creator");
						URI resCreator = creatorNode != null && creatorNode.isURIResource() ? URI
								.create(creatorNode.asResource().getURI()) : null;
						RDFNode creatorNameNode = solution.get("creatorname");
						String resCreatorName = creatorNameNode != null ? creatorNameNode
								.asLiteral().getString() : null;
						UserProfile profile = null;
						if (resCreatorName != null || resCreator != null) {
							profile = new UserProfile(resCreatorName, resCreatorName, null,
									resCreator);
						}
						RDFNode createdNode = solution.get("created");
						DateTime resCreated = createdNode != null && createdNode.isLiteral() ? DateTime
								.parse(createdNode.asLiteral().getString()) : null;
						resource = builder.buildAggregatedResource(rUri, getResearchObject(),
								profile, resCreated);
						if (pUri != null) {
							resource.setProxy(builder.buildProxy(pUri, resource,
									getResearchObject()));
						}
					}
					aggregated.put(rUri, resource);
				}
			} finally {
				qe.close();
			}
			// This is normally not necessary but helps if some old ROs don't
			// explicitly use the ore:AggregatedResource class
			aggregated.putAll(resources2);
			aggregated.putAll(folders2);
			aggregated.putAll(annotations2);
			return aggregated;
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Identify ro:Resources that are not ro:Folders, aggregated by the RO.
	 * 
	 * @return a set of resources (not loaded)
	 */
	public Map<URI, Resource> extractResources() {
		ResourceFactory resourceFactory = new ResourceFactory(builder);
		boolean transactionStarted = beginTransaction(ReadWrite.READ);
		try {
			Map<URI, Resource> resources2 = new HashMap<>();
			String queryString = String
					.format("PREFIX ore: <%s> PREFIX dcterms: <%s> PREFIX ro: <%s> PREFIX foaf: <%s> SELECT ?resource ?proxy ?created ?creator ?creatorname WHERE { <%s> ore:aggregates ?resource . ?resource a ro:Resource . ?proxy ore:proxyFor ?resource . OPTIONAL { ?resource dcterms:creator ?creator . OPTIONAL { ?creator foaf:name ?creatorname . } } OPTIONAL { ?resource dcterms:created ?created . } }",
							ORE.NAMESPACE, DCTerms.NS, RO.NAMESPACE, FOAF.NAMESPACE, aggregation
									.getUri().toString());

			Query query = QueryFactory.create(queryString);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			try {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution solution = results.next();
					RDFNode r = solution.get("resource");
					if (r.as(Individual.class).hasRDFType(RO.Folder)) {
						continue;
					}
					URI rUri = URI.create(r.asResource().getURI());
					RDFNode p = solution.get("proxy");
					URI pUri = URI.create(p.asResource().getURI());
					RDFNode creatorNode = solution.get("creator");
					URI resCreator = creatorNode != null && creatorNode.isURIResource() ? URI
							.create(creatorNode.asResource().getURI()) : null;
					RDFNode creatorNameNode = solution.get("creatorname");
					String resCreatorName = creatorNameNode != null ? creatorNameNode.asLiteral()
							.getString() : null;
					UserProfile profile = null;
					if (resCreatorName != null || resCreator != null) {
						profile = new UserProfile(resCreatorName, resCreatorName, null, resCreator);
					}
					RDFNode createdNode = solution.get("created");
					DateTime resCreated = createdNode != null && createdNode.isLiteral() ? DateTime
							.parse(createdNode.asLiteral().getString()) : null;
					Resource resource = resourceFactory.buildResource(rUri, getResearchObject(),
							profile, resCreated);
					if (pUri != null) {
						resource.setProxy(builder.buildProxy(pUri, resource, getResearchObject()));
					}
					resources2.put(rUri, resource);
				}
			} finally {
				qe.close();
			}
			return resources2;
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Identify ro:Resources that are not ro:Folders, aggregated by the RO.
	 * 
	 * @return a set of folders (not loaded)
	 */
	public Map<URI, Folder> extractFolders() {
		boolean transactionStarted = beginTransaction(ReadWrite.READ);
		try {
			Map<URI, Folder> folders2 = new HashMap<>();
			String queryString = String
					.format("PREFIX ore: <%s> PREFIX dcterms: <%s> PREFIX ro: <%s> PREFIX foaf: <%s> SELECT ?folder ?proxy ?resourcemap ?created ?creator ?creatorname WHERE { <%s> ore:aggregates ?folder . ?folder a ro:Folder ; ore:isDescribedBy ?resourcemap . OPTIONAL { ?proxy ore:proxyFor ?folder . } OPTIONAL { ?folder dcterms:creator ?creator . OPTIONAL { ?creator foaf:name ?creatorname . } } OPTIONAL { ?folder dcterms:created ?created . } }",
							ORE.NAMESPACE, DCTerms.NS, RO.NAMESPACE, FOAF.NAMESPACE, aggregation
									.getUri().toString());

			Query query = QueryFactory.create(queryString);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			try {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution solution = results.next();
					RDFNode f = solution.get("folder");
					URI fURI = URI.create(f.asResource().getURI());
					RDFNode p = solution.get("proxy");
					URI pUri = p != null ? URI.create(p.asResource().getURI()) : null;
					RDFNode rm = solution.get("resourcemap");
					URI rmUri = URI.create(rm.asResource().getURI());
					RDFNode creatorNode = solution.get("creator");
					URI resCreator = creatorNode != null && creatorNode.isURIResource() ? URI
							.create(creatorNode.asResource().getURI()) : null;
					RDFNode creatorNameNode = solution.get("creatorname");
					String resCreatorName = creatorNameNode != null ? creatorNameNode.asLiteral()
							.getString() : null;
					UserProfile profile = null;
					if (resCreatorName != null || resCreator != null) {
						profile = new UserProfile(resCreatorName, resCreatorName, null, resCreator);
					}
					RDFNode createdNode = solution.get("created");
					DateTime resCreated = createdNode != null && createdNode.isLiteral() ? DateTime
							.parse(createdNode.asLiteral().getString()) : null;

					String queryString2 = String.format(
							"PREFIX ro: <%s> ASK { <%s> ro:rootFolder <%s> }", RO.NAMESPACE,
							uri.toString(), fURI.toString());
					Query query2 = QueryFactory.create(queryString2);
					QueryExecution qe2 = QueryExecutionFactory.create(query2, model);
					boolean isRootFolder = false;
					try {
						isRootFolder = qe2.execAsk();
					} finally {
						qe2.close();
					}

					Folder folder = builder.buildFolder(fURI, getResearchObject(), profile,
							resCreated, rmUri);
					folder.setRootFolder(isRootFolder);
					if (pUri != null) {
						folder.setProxy(builder.buildProxy(pUri, folder, getResearchObject()));
					}
					folders2.put(fURI, folder);
				}
			} finally {
				qe.close();
			}
			return folders2;
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Identify ro:AggregatedAnnotations that aggregated by the RO.
	 * 
	 * @return a multivalued map of annotations, with bodies not loaded
	 */
	public Map<URI, Annotation> extractAnnotations() {
		boolean transactionStarted = beginTransaction(ReadWrite.READ);
		try {
			Map<URI, Annotation> annotationsByUri = new HashMap<>();
			String queryString = String
					.format("PREFIX ore: <%s> PREFIX dcterms: <%s> PREFIX ao: <%s> PREFIX foaf: <%s> PREFIX ro: <%s> SELECT ?annotation ?body ?target ?created ?creator ?creatorname WHERE { <%s> ore:aggregates ?annotation . ?annotation a ro:AggregatedAnnotation ; ro:annotatesAggregatedResource ?target . OPTIONAL {?annotation ao:body ?body. }  OPTIONAL { ?proxy ore:proxyFor ?annotation . } OPTIONAL { ?annotation dcterms:creator ?creator . OPTIONAL { ?creator foaf:name ?creatorname . } } OPTIONAL { ?annotation dcterms:created ?created . } }",
							ORE.NAMESPACE, DCTerms.NS, AO.NAMESPACE, FOAF.NAMESPACE, RO.NAMESPACE,
							aggregation.getUri().toString());

			Query query = QueryFactory.create(queryString);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			try {
				Map<RDFNode, URI> blankNodes = new HashMap<>();
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution solution = results.next();
					RDFNode a = solution.get("annotation");
					URI aURI;
					if (a.isURIResource()) {
						aURI = URI.create(a.asResource().getURI());
					} else {
						aURI = getUri().resolve(UUID.randomUUID().toString());
						blankNodes.put(a, aURI);
					}
					RDFNode p = solution.get("proxy");
					URI pUri = p != null ? URI.create(p.asResource().getURI()) : null;
					RDFNode t = solution.get("target");
					URI tUri = URI.create(t.asResource().getURI());
					Annotation annotation;
					if (annotationsByUri.containsKey(aURI)) {
						annotation = annotationsByUri.get(aURI);
						annotation.getAnnotated().add(builder.buildThing(tUri));
					} else {
						URI bUri = null;
						if (solution.get("body") != null) {
							RDFNode b = solution.get("body");
							bUri = URI.create(b.asResource().getURI());
						}

						RDFNode creatorNode = solution.get("creator");
						URI resCreator = creatorNode != null && creatorNode.isURIResource() ? URI
								.create(creatorNode.asResource().getURI()) : null;
						RDFNode creatorNameNode = solution.get("creatorname");
						String resCreatorName = creatorNameNode != null ? creatorNameNode
								.asLiteral().getString() : null;
						UserProfile profile = null;
						if (resCreatorName != null || resCreator != null) {
							profile = new UserProfile(resCreatorName, resCreatorName, null,
									resCreator);
						}
						RDFNode createdNode = solution.get("created");
						DateTime resCreated = createdNode != null && createdNode.isLiteral() ? DateTime
								.parse(createdNode.asLiteral().getString()) : null;
						if (bUri != null) {
							// FIXME don't build things, look for them
							annotation = builder.buildAnnotation(aURI, getResearchObject(),
									builder.buildThing(bUri), builder.buildThing(tUri), profile,
									resCreated);
						} else {
							// FIXME don't build things, look for them
							annotation = builder.buildAnnotation(aURI, getResearchObject(),
									new Thing(null, null, false, null), new Thing(null, null,
											false, null), profile, resCreated);
						}

						if (pUri != null) {
							annotation.setProxy(builder.buildProxy(pUri, annotation,
									getResearchObject()));
						}
						annotationsByUri.put(annotation.getUri(), annotation);
					}
				}
				for (Entry<RDFNode, URI> e : blankNodes.entrySet()) {
					changeBlankNodeToUriResources(e.getValue(), e.getKey());
				}
			} finally {
				qe.close();
			}
			return annotationsByUri;
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Identify if this RO has alternative URIs, i.e. an RO bundle.
	 * 
	 * @param mimeType
	 *            MIME type of the alternative format
	 * @return the URI of the bundle or null
	 */
	public URI extractAlternativeFormat(String mimeType) {
		boolean transactionStarted = beginTransaction(ReadWrite.READ);
		try {
			String queryString = String
					.format("PREFIX dcterms: <%s> SELECT ?format WHERE { <%s> dcterms:hasFormat ?format . ?format dcterms:format \"%s\" . }",
							DCTerms.NS, aggregation.getUri().toString(), mimeType);

			Query query = QueryFactory.create(queryString);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			try {
				ResultSet results = qe.execSelect();
				if (results.hasNext()) {
					QuerySolution solution = results.next();
					RDFNode r = solution.get("format");
					return URI.create(r.asResource().getURI());
				} else {
					return null;
				}
			} finally {
				qe.close();
			}
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Save an alternative format of this RO.
	 * 
	 * @param formatUri
	 *            URI of the alternative representation
	 * @param mimeType
	 *            MIME type of the alternative representation
	 */
	public void saveAlternativeFormat(URI formatUri, String mimeType) {
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			Individual aggregationR = model.getIndividual(aggregation.getUri().toString());
			com.hp.hpl.jena.rdf.model.Resource formatR = model.createResource(formatUri.toString());
			aggregationR.addProperty(DCTerms.hasFormat, formatR);
			formatR.addProperty(DCTerms.format, model.createLiteral(mimeType));
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Identify all Research Objects that aggregate this RO.
	 * 
	 * @return a collection of URIs, possibly empty.
	 */
	public Collection<URI> extractAggregatingROUris() {
		Set<URI> uris = new HashSet<>();
		boolean transactionStarted = beginTransaction(ReadWrite.READ);
		try {
			String queryString = String
					.format("PREFIX ore: <%s> PREFIX ro: <%s> SELECT ?parent WHERE { ?parent a ro:ResearchObject; ore:aggregates <%s> . }",
							ORE.NAMESPACE, RO.NAMESPACE, aggregation.getUri().toString());

			Query query = QueryFactory.create(queryString);
			QueryExecution qe = QueryExecutionFactory.create(query, model);
			try {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution solution = results.next();
					RDFNode r = solution.get("parent");
					uris.add(URI.create(r.asResource().getURI()));
				}
			} finally {
				qe.close();
			}
		} finally {
			endTransaction(transactionStarted);
		}
		return uris;
	}

	@Override
	public ResearchObject getResearchObject() {
		return (ResearchObject) aggregation;
	}

	/**
	 * Save an annotation in the manifest.
	 * 
	 * @param annotation
	 *            an annotation
	 */
	public void saveAnnotationData(Annotation annotation) {
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			com.hp.hpl.jena.rdf.model.Resource bodyR = model.createResource(annotation.getBody()
					.getUri().toString());
			Individual annotationInd = model.createIndividual(annotation.getUri().toString(),
					RO.AggregatedAnnotation);
			annotationInd.addRDFType(AO.Annotation);
			model.removeAll(annotationInd, AO.body, null);
			model.add(annotationInd, AO.body, bodyR);
			model.removeAll(annotationInd, RO.annotatesAggregatedResource, null);
			for (Thing targetThing : annotation.getAnnotated()) {
				com.hp.hpl.jena.rdf.model.Resource target = model.createResource(targetThing
						.getUri().normalize().toString());
				model.add(annotationInd, RO.annotatesAggregatedResource, target);
			}
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Save a nested RO as both ore:AggregatedResource and ro:ResearchObject,
	 * with links to the manifest.
	 * 
	 * @param nestedResearchObject
	 *            a nested RO
	 */
	public void saveNestedResearchObject(ResearchObject nestedResearchObject) {
		super.saveNestedAggregation(nestedResearchObject);
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			Individual aggregation = model.getIndividual(nestedResearchObject.getUri().toString());
			aggregation.addRDFType(RO.ResearchObject);
			aggregation.addRDFType(RO.Resource);
			Individual manifestR = model.getIndividual(nestedResearchObject.getResourceMap()
					.getUri().toString());
			manifestR.addRDFType(RO.Manifest);
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

	/**
	 * Save that this RO is aggregated by another.
	 * 
	 * @param parentResearchObject
	 *            aggregating RO
	 */
	public void saveIsNestedInResearchObject(ResearchObject parentResearchObject) {
		super.saveIsNestedInAggregation(parentResearchObject);
		boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
		try {
			Individual aggregation = model.getIndividual(parentResearchObject.getUri().toString());
			aggregation.addRDFType(RO.ResearchObject);
			Individual manifestR = model.getIndividual(parentResearchObject.getResourceMap()
					.getUri().toString());
			manifestR.addRDFType(RO.Manifest);
			commitTransaction(transactionStarted);
		} finally {
			endTransaction(transactionStarted);
		}
	}

}
