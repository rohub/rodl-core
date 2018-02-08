package org.rohub.rodl.model.ORE;


import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.rohub.rodl.dl.AccessDeniedException;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.RDF.Thing;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.vocabulary.ORE;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Represents an ore:Proxy.
 * 
 * @author piotrekhol
 * 
 */
public class Proxy extends Thing {

    /** Aggregated resource. */
    protected AggregatedResource proxyFor;

    /** Aggregating resource. */
    protected Aggregation proxyIn;


    /**
     * Constructor.
     * 
     * @param user
     *            user creating the instance
     * @param dataset
     *            custom dataset
     * @param useTransactions
     *            should transactions be used. Note that not using transactions on a dataset which already uses
     *            transactions may make it unreadable.
     * @param uri
     *            proxy URI
     */
    public Proxy(UserMetadata user, Dataset dataset, boolean useTransactions, URI uri) {
        super(user, dataset, useTransactions, uri);
    }


    public Aggregation getProxyIn() {
        return proxyIn;
    }


    public void setProxyIn(Aggregation proxyIn) {
        this.proxyIn = proxyIn;
    }


    public AggregatedResource getProxyFor() {
        return proxyFor;
    }


    public void setProxyFor(AggregatedResource proxyFor) {
        this.proxyFor = proxyFor;
    }


    @Override
    public void save()
            throws ConflictException, DigitalLibraryException, AccessDeniedException, NotFoundException {
        super.save();
        proxyIn.getResourceMap().saveProxy(this);
    }


    /**
     * Delete the proxy. Doesn't delete the proxied resource.
     */
    @Override
    public void delete() {
        getProxyIn().getResourceMap().deleteResource(this);
        getProxyIn().getResourceMap().serialize();
        getProxyIn().getProxies().remove(uri);
        super.delete();
    }


    /**
     * Find the proxyFor resource URI in the proxy RDF description.
     * 
     * @param researchObject
     *            research object which will aggregate the proxy
     * @param content
     *            proxy description
     * @return URI of the proxied resource of null if not found
     * @throws BadRequestException
     *             if the description is not valid
     */
    public static URI assemble(ResearchObject researchObject, InputStream content)
            throws BadRequestException {
        Objects.requireNonNull(researchObject, "Research object cannot be null");
        Objects.requireNonNull(content, "Input stream cannot be null");
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        model.read(content, researchObject.getUri().toString());
        ExtendedIterator<Individual> it = model.listIndividuals(ORE.Proxy);
        if (it.hasNext()) {
            NodeIterator it2 = it.next().listPropertyValues(ORE.proxyFor);
            if (it2.hasNext()) {
                RDFNode proxyForResource = it2.next();
                if (proxyForResource.isURIResource()) {
                    try {
                        return new URI(proxyForResource.asResource().getURI());
                    } catch (URISyntaxException e) {
                        throw new BadRequestException("Wrong target resource URI", e);
                    }
                } else {
                    throw new BadRequestException("The target is not an URI resource.");
                }
            } else {
                return null;
            }
        } else {
            throw new BadRequestException("The entity body does not define any ore:Proxy.");
        }
    }

}
