package org.rohub.rodl.accesscontrol.model.dao;


import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.rohub.rodl.accesscontrol.model.AccessMode;
import org.rohub.rodl.db.dao.AbstractDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;

/**
 * Research Object access mode DAO.
 * 
 * @author pejot
 * 
 */
public final class ModeDAO extends AbstractDAO<AccessMode> {

    /** id. */
    private static final long serialVersionUID = -4468344863067565271L;


    /**
     * Find Access Control Mode by id.
     * 
     * @param id
     *            uri (id)
     * @return client or null
     */
    public AccessMode findById(Integer id) {
        return findByPrimaryKey(AccessMode.class, id);
    }


    /**
     * Find Access Control Mode by Research Object.
     * 
     * @param ro
     *            Research Object.
     * @return Access Control mode, null in case mode doesn't exists
     */
    public AccessMode findByResearchObject(String ro) {
        Criteria criteria = HibernateUtil.getSessionFactory().getCurrentSession().createCriteria(AccessMode.class);
        criteria.add(Restrictions.eq("ro", ro));
        @SuppressWarnings("unchecked")
        List<AccessMode> result = criteria.list();
        if (result.size() == 1) {
            return result.get(0);
        }
        return null;
    }
}
