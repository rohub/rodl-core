
package org.rohub.rodl.fs;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.AccessDeniedException;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.DigitalLibrary;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.storage.FilesystemDL;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author piotrhol
 * 
 */
public class BasicTest {

    private static final String MAIN_FILE_MIME_TYPE = "text/plain";

    private static final String MAIN_FILE_CONTENT = "test";

    private static final String MAIN_FILE_PATH = "mainFile.txt";

    private static final UserMetadata ADMIN = new UserMetadata("wfadmin", "John Doe", Role.ADMIN);

    private static final UserMetadata USER = new UserMetadata("test-" + new Date().getTime(), "test user",
            Role.AUTHENTICATED);

    private static final URI RO_URI = URI.create("http://example.org/ROs/foobar/");

    private static final String BASE = "/tmp/testdl/";

    private static final String USER_PASSWORD = "foo";


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
            throws Exception {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        Files.createDirectories(Paths.get(BASE));
    }


    @After
    public void tearDown()
            throws IOException {
        try {
            DigitalLibrary dl = new FilesystemDL(BASE);
            dl.deleteResearchObject(RO_URI);
        } catch (Exception e) {

        }
        try {
            DigitalLibrary dlA = new FilesystemDL(BASE);
            dlA.deleteUser(USER.getLogin());
        } catch (Exception e) {

        }
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        try {
            Files.delete(Paths.get(BASE));
        } catch (DirectoryNotEmptyException | NoSuchFileException e) {
        }
    }


    /**
     * Test method for
     * {@link pl.psnc.dl.wf4ever.dlibra.helpers.DLibraDataSource#createVersion(java.lang.String, java.lang.String, java.lang.String, java.net.URI)}
     * .
     * 
     * @throws DigitalLibraryException
     * @throws ConflictException
     * @throws NotFoundException
     * @throws IOException
     * @throws AccessDeniedException
     * @throws org.rohub.rodl.dl.AccessDeniedException
     */
    @Test
    public final void testCreateVersionStringStringStringURI()
            throws DigitalLibraryException, NotFoundException, ConflictException, IOException,
            org.rohub.rodl.dl.AccessDeniedException {
        DigitalLibrary dlA = new FilesystemDL(BASE);
        assertTrue(dlA.createOrUpdateUser(USER.getLogin(), USER_PASSWORD, USER.getName()));
        DigitalLibrary dl = new FilesystemDL(BASE);
        dl.createResearchObject(RO_URI, new ByteArrayInputStream(MAIN_FILE_CONTENT.getBytes()), MAIN_FILE_PATH,
            MAIN_FILE_MIME_TYPE);
        InputStream in = dl.getFileContents(RO_URI, MAIN_FILE_PATH);
        try {
            String file = IOUtils.toString(in);
            assertEquals("Manifest is properly saved", MAIN_FILE_CONTENT, file);
        } finally {
            in.close();
        }
    }


    @Test
    public final void testGetUserProfile()
            throws DigitalLibraryException, IOException, NotFoundException {
        DigitalLibrary dlA = new FilesystemDL(BASE);
        assertTrue(dlA.createOrUpdateUser(USER.getLogin(), USER_PASSWORD, USER.getName()));
        assertFalse(dlA.createOrUpdateUser(USER.getLogin(), USER_PASSWORD, USER.getName()));
        DigitalLibrary dl = new FilesystemDL(BASE);
        UserMetadata user = dl.getUserProfile(USER.getLogin());
        Assert.assertEquals("User login is equal", USER.getLogin(), user.getLogin());
        Assert.assertEquals("User name is equal", USER.getName(), user.getName());
    }


    @Test
    public final void testStoreAttributes()
            throws DigitalLibraryException, IOException, ConflictException, NotFoundException, AccessDeniedException {
        DigitalLibrary dlA = new FilesystemDL(BASE);
        dlA.createOrUpdateUser(USER.getLogin(), USER_PASSWORD, USER.getName());
        DigitalLibrary dl = new FilesystemDL(BASE);
        dl.createResearchObject(RO_URI, new ByteArrayInputStream(MAIN_FILE_CONTENT.getBytes()), MAIN_FILE_PATH,
            MAIN_FILE_MIME_TYPE);
        Multimap<URI, Object> atts = HashMultimap.create();
        atts.put(URI.create("a"), "foo");
        atts.put(URI.create("a"), "bar");
        atts.put(URI.create("b"), "lorem ipsum");
        dl.storeAttributes(RO_URI, atts);
    }
}
