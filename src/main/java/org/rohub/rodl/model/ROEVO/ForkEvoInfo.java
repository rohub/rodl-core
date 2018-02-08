package org.rohub.rodl.model.ROEVO;


import java.net.URI;

import org.joda.time.DateTime;
import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.ResearchObject;
import org.rohub.rodl.vocabulary.PROV;
import org.rohub.rodl.vocabulary.RO;
import org.rohub.rodl.vocabulary.ROEVO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;

public class ForkEvoInfo extends LiveEvoInfo {

	public ForkEvoInfo(UserMetadata user, Dataset dataset, boolean useTransactions, ResearchObject researchObject,
			URI uri) {
		super(user, dataset, useTransactions, researchObject, uri);
	}
	
	
	public static ForkEvoInfo create(Builder builder, URI uri, ResearchObject researchObject) {
        ForkEvoInfo evoInfo = builder.buildForkEvoInfo(uri, researchObject, builder.getUser(), DateTime.now());
        evoInfo.save();
        evoInfo.scheduleToSerialize(researchObject.getUri(), RDFFormat.TURTLE);
        researchObject.getAggregatedResources().put(evoInfo.getUri(), evoInfo);
        return evoInfo;
    }
	
	@Override
    public void save() {
        super.save();
        boolean transactionStarted = beginTransaction(ReadWrite.WRITE);
        try {
        	ResearchObject obj = getResearchObject();
            Individual ro = model.createIndividual(getResearchObject().getUri().toString(), RO.ResearchObject);
            ro.addRDFType(ROEVO.ForkedRO);
            ro.addProperty(PROV.wasDerivedFrom, model.createIndividual(obj.getCopyOf().getUri().toString(), RO.ResearchObject));
            commitTransaction(transactionStarted);
        } finally {
            endTransaction(transactionStarted);
        }
    }

}
