package org.rohub.rodl.accesscontrol.model.dao;


import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rohub.rodl.AbstractUnitTest;
import org.rohub.rodl.accesscontrol.dicts.Role;
import org.rohub.rodl.accesscontrol.model.Permission;
import org.rohub.rodl.accesscontrol.model.dao.PermissionDAO;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;

public class PermissionDAOTest extends AbstractUnitTest {

    String id;
    String roUri = "http://www.example.com/ROs/1/";
    URI userUri;
    UserProfileDAO userProfileDAO = new UserProfileDAO();
    Permission permission;
    PermissionDAO dao = new PermissionDAO();
    UserProfile profile;


    @Before
    public void setUp()
            throws Exception {
        super.setUp();
        id = "http://www.example.com/accesscontrol/permissions/" + UUID.randomUUID().toString();
        userUri = URI.create("http://testuser.myopenid.com/" + UUID.randomUUID().toString());
        profile = new UserProfile(userUri.toString(), "name", org.rohub.rodl.dl.UserMetadata.Role.AUTHENTICATED,
                userUri);
        userProfileDAO.save(profile);
        permission = new Permission();
        permission.setRo(roUri);
        permission.setRole(Role.EDITOR);
        permission.setUser(profile);
    }


    @After
    public void tearDown()
            throws Exception {
        userProfileDAO.delete(profile);
        super.tearDown();
    }


    @Test
    public void testCRUD() {
        dao.save(permission);
        permission = dao.findById(permission.getId());
        Assert.assertNotNull(permission);
        dao.delete(permission);
        permission = dao.findById(permission.getId());
        Assert.assertNull(permission);

    }


    @Test
    public void tesPermissionByRo() {
        dao.save(permission);
        List<Permission> permissions = dao.findByResearchObject(roUri);
        Assert.assertEquals(1, permissions.size());
        dao.delete(permission);
        permissions = dao.findByResearchObject(roUri);
        Assert.assertEquals(0, permissions.size());

    }


    @Test
    public void testFindByUserAndRo() {
        dao.save(permission);
        List<Permission> permissions = dao.findByUserROAndPermission(permission.getUser(), permission.getRo(),
            permission.getRole());
        Assert.assertEquals(1, permissions.size());
        permissions = dao.findByUserROAndPermission(permission.getUser(), permission.getRo() + "worng-uri",
            permission.getRole());
        dao.delete(permission);
    }


    @Test
    public void testUserProfileForeignKeyBehaviour() {
        permission.setUser(null);
        try {
            dao.save(permission);
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
            Assert.fail("expected ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
            return;
        }
    }
}
