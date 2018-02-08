
package org.rohub.rodl.db.dao;

/*-
 * #%L
 * ROHUB
 * %%
 * Copyright (C) 2010 - 2018 PSNC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.rohub.rodl.db.AccessToken;
import org.rohub.rodl.db.OAuthClient;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.hibernate.HibernateUtil;

/**
 * RODL user model.
 * 
 * @author piotrhol
 * 
 */
public final class AccessTokenDAO extends AbstractDAO<AccessToken> {

    /** id. */
    private static final long serialVersionUID = -4468344863067565271L;


    /**
     * Find the access token by its value.
     * 
     * @param value
     *            access token value
     * @return access token active record
     */
    public AccessToken findByValue(String value) {
        return findByPrimaryKey(AccessToken.class, value);
    }


    /**
     * Find an access token by its client id, user id or both.
     * 
     * @param client
     *            client
     * @param userProfile
     *            owner
     * @return access token active record
     */
    @SuppressWarnings("unchecked")
    public List<AccessToken> findByClientOrUser(OAuthClient client, UserProfile userProfile) {
        Criteria criteria = HibernateUtil.getSessionFactory().getCurrentSession().createCriteria(AccessToken.class);
        if (client != null) {
            criteria.add(Restrictions.eq("client.clientId", client.getClientId()));
        }
        if (userProfile != null) {
            criteria.add(Restrictions.eq("user.login", userProfile.getLogin()));
        }
        return criteria.list();
    }

}
