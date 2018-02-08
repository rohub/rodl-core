package org.rohub.rodl.model.RO;


import java.io.InputStream;
import java.net.URI;

import org.rohub.rodl.exceptions.BadRequestException;
import org.rohub.rodl.model.Builder;
import org.rohub.rodl.model.RO.Folder;
import org.rohub.rodl.model.RO.ResearchObject;

public class FolderBuilder {

    static final String DEFAULT_FOLDER_PATH = "model/ro/folder/folder.rdf";


    public void createROAggregated(Builder builder, ResearchObject researchObject, URI uri)
            throws BadRequestException {
        researchObject.aggregate(URI.create("http://example.org"));
        InputStream is = getClass().getClassLoader().getResourceAsStream("model/ro/folder/folder.rdf");
        researchObject.aggregate("ar1", is, "text/plain");
        is = getClass().getClassLoader().getResourceAsStream("model/ro/folder/folder.rdf");
        researchObject.aggregate("ar2", is, "text/plain");
        is = getClass().getClassLoader().getResourceAsStream("model/ro/folder/folder.rdf");
        researchObject.aggregate("ar3", is, "text/plain");
    }


    public Folder init(String path, Builder builder, ResearchObject researchObject, URI uri)
            throws BadRequestException {
        createROAggregated(builder, researchObject, uri);
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        return Folder.create(builder, researchObject, uri, is);
    }
}
