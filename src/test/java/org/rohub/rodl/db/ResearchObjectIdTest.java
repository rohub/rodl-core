package org.rohub.rodl.db;


import java.net.URI;

import org.junit.Assert;
import org.junit.Test;
import org.rohub.rodl.db.ResearchObjectId;

public class ResearchObjectIdTest {

    URI idUri = URI.create("http://www.example.com/ROs/ResearchObject/");


    @Test
    public void testConstructor() {
        ResearchObjectId id = new ResearchObjectId(idUri);
        Assert.assertEquals(idUri, id.getId());
    }
}
