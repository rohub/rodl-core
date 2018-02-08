package org.rohub.rodl.model.ROEVO;


import java.net.URI;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.ORE.AggregatedResource;
import org.rohub.rodl.model.RDF.Thing;

import com.hp.hpl.jena.query.Dataset;

public class Change extends Thing {

    public enum ChangeType {
        ADDITION,
        MODIFICATION,
        REMOVAL
    }


    private ChangeSpecification changeSpecification;

    private AggregatedResource resource;

    private ChangeType changeType;


    public Change(UserMetadata user, Dataset dataset, Boolean useTransactions, URI uri,
            ChangeSpecification changeSpecification) {
        super(user, dataset, useTransactions, uri);
        this.changeSpecification = changeSpecification;
    }


    public static Change create(Builder builder, ChangeSpecification changeSpecification, AggregatedResource resource,
            ChangeType type) {
        URI uri = UriBuilder.fromUri(changeSpecification.getUri()).path("changes/" + UUID.randomUUID().toString())
                .build();
        Change change = builder.buildChange(uri, changeSpecification, resource, type);
        change.save();
        return change;
    }


    @Override
    public void save() {
        super.save();
        this.changeSpecification.getResearchObject().getImmutableEvoInfo().saveChange(this);
    }


    @Override
    public void delete() {
        this.changeSpecification.getResearchObject().getImmutableEvoInfo().deleteResource(this);
        super.delete();
    }


    public ChangeSpecification getChangeSpecification() {
        return changeSpecification;
    }


    public AggregatedResource getResource() {
        return resource;
    }


    public void setResource(AggregatedResource resource) {
        this.resource = resource;
    }


    public ChangeType getChangeType() {
        return changeType;
    }


    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

}
