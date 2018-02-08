
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

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.rohub.rodl.db.ResourceInfo;

/**
 * RODL user model.
 * 
 * @author piotrhol
 * 
 */
public final class ResourceInfoDAO extends AbstractDAO<ResourceInfo> {

    /** id. */
    private static final long serialVersionUID = -4468344863067565271L;


    /**
     * Load an instance or create a new one.
     * 
     * @param path
     *            file path
     * @param name
     *            file name
     * @param checksum
     *            checksum
     * @param sizeInBytes
     *            size in bytes
     * @param digestMethod
     *            i.e. MD5, SHA1
     * @param lastModified
     *            date of last modification
     * @param mimeType
     *            MIME type
     * @return an instance
     */
    public ResourceInfo create(String path, String name, String checksum, long sizeInBytes, String digestMethod,
            DateTime lastModified, String mimeType) {
        ResourceInfo res = findByPath(path);
        if (res == null) {
            return new ResourceInfo(path, name, checksum, sizeInBytes, digestMethod, lastModified, mimeType);
        } else {
            res.setName(name);
            res.setChecksum(checksum);
            res.setSizeInBytes(sizeInBytes);
            res.setDigestMethod(digestMethod);
            res.setLastModifiedInMilis(lastModified.getMillis());
            res.setMimeType(mimeType);
            return res;
        }
    }


    /**
     * Find by file path.
     * 
     * @param path
     *            file path
     * @return resource info or null
     */
    public ResourceInfo findByPath(String path) {
        return findByPrimaryKey(ResourceInfo.class, path);
    }


    /**
     * Find all resources that have a path ending with the specified sufix.
     * 
     * @param sufix
     *            the sufix, without the trailing %
     * @return a list of resources that have a matching path
     */
    public List<ResourceInfo> findByPathSufix(String sufix) {
        Criterion criterion = Restrictions.ilike("path", "%" + sufix);
        return findByCriteria(ResourceInfo.class, criterion);
    }


    /**
     * Get all resources stored.
     * 
     * @return list of stored resources
     */
    public List<ResourceInfo> all() {
        return super.findAll(ResourceInfo.class);

    }

}
