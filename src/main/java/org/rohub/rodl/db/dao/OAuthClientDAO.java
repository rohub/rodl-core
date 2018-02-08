
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

import org.rohub.rodl.db.OAuthClient;

/**
 * RODL user model.
 * 
 * @author piotrhol
 * 
 */
public final class OAuthClientDAO extends AbstractDAO<OAuthClient> {

    /** id. */
    private static final long serialVersionUID = -4468344863067565271L;


    /**
     * Find in database by client id.
     * 
     * @param clientId
     *            client id
     * @return client or null
     */
    public OAuthClient findById(String clientId) {
        return findByPrimaryKey(OAuthClient.class, clientId);
    }


    /**
     * Find all clients.
     * 
     * @return a list of clients
     */
    public List<OAuthClient> findAll() {
        return findAll(OAuthClient.class);
    }
}
