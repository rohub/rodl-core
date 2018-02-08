package org.rohub.rodl.accesscontrol.model.dao;


import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rohub.rodl.AbstractUnitTest;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.accesscontrol.model.dao.ModeDAO;

public class ModeDAOTest extends AbstractUnitTest {

    String id = "http://www.example.com/accesscontrol/modes/1";
    String roUri = "http://www.example.com/ROs/" + UUID.randomUUID().toString() + "/";
    ModeDAO dao = new ModeDAO();
    AccessMode mode;


    @Before
    @Override
    public void setUp()
            throws Exception {
        super.setUp();
        mode = new AccessMode();
        mode.setRo(roUri);
        mode.setMode(org.rohub.rodl.accesscontrol.dicts.Mode.PRIVATE);
    }


    @Test
    public void testCRUD() {
        dao.save(mode);
        Assert.assertNotNull(dao.findById(mode.getId()));
        dao.delete(mode);
        Assert.assertNull(dao.findById(mode.getId()));
    }


    @Test
    public void testGetModeByRO() {
        dao.save(mode);
        Assert.assertNotNull(dao.findByResearchObject(mode.getRo()));
        dao.delete(mode);
        Assert.assertNull(dao.findByResearchObject(mode.getRo()));
    }

}
