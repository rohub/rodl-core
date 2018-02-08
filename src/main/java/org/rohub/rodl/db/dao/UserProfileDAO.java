
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

import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.dl.UserMetadata.Role;

/**
 * RODL user model.
 * 
 * @author piotrhol
 * 
 */
public final class UserProfileDAO extends AbstractDAO<UserProfile> {

    /** id. */
    private static final long serialVersionUID = -4468344863067565271L;


    /**
     * Find by user login.
     * 
     * @param login
     *            user login
     * @return user profile or null
     */
    public UserProfile findByLogin(String login) {
        return findByPrimaryKey(UserProfile.class, login);
    }


    /**
     * Load from database or create a new instance.
     * 
     * @param login
     *            login
     * @param username
     *            username
     * @param role
     *            role
     * @return an instance
     */
    public UserProfile create(String login, String username, Role role) {
        UserProfile result = findByLogin(login);
        if (result == null) {
            return new UserProfile(login, username, role);
        }
        return result;
    }
}
