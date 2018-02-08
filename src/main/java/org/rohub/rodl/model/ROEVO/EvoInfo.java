package org.rohub.rodl.model.ROEVO;


import java.net.URI;

import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.evo.EvoType;
import org.rohub.rodl.model.ORE.AggregatedResource;
import org.rohub.rodl.model.RO.ResearchObject;

import com.hp.hpl.jena.query.Dataset;

public abstract class EvoInfo extends AggregatedResource {
	
    protected EvoType evoType;


    public EvoInfo(UserMetadata user, Dataset dataset, boolean useTransactions, ResearchObject researchObject, URI uri) {
        super(user, dataset, useTransactions, researchObject, uri);
    }


    public EvoType getEvoType() {
        return this.evoType;
    }


    public void setEvoType(EvoType evoType) {
        this.evoType = evoType;
    }


    public abstract void load();


    /**
     * Update the evolution info in the triplestore and in the storage based on its properties.
     */
    public void updateHistory() {
        save();
        serialize();
    }

}
