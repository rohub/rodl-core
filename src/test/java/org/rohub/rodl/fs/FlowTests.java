
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.AccessDeniedException;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.DigitalLibrary;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.ResourceMetadata;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;
import org.rohub.rodl.storage.FilesystemDL;

/**
 * @author piotrek
 * 
 */
public class FlowTests {

    private static final UserMetadata ADMIN = new UserMetadata("wfadmin", "John Doe", Role.ADMIN);

    private static final UserMetadata USER = new UserMetadata("test-" + new Date().getTime(), "test user",
            Role.AUTHENTICATED);

    private DigitalLibrary dl;

    private static final FileRecord[] files = new FileRecord[3];

    private static final String[] directories = { "", "dir/", "testdir" };

    private static final String MAIN_FILE_MIME_TYPE = "text/plain";

    private static final String MAIN_FILE_CONTENT = "test";

    private static final String MAIN_FILE_PATH = "mainFile.txt";

    private static final URI RO_URI = URI.create("http://example.org/ROs/foobar/");

    private static final String BASE = "/tmp/testdl/";

    private static final String USER_PASSWORD = "foo";


    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass()
            throws Exception {
    }


    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() {
    }


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
            throws Exception {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        dl = new FilesystemDL(BASE);
        dl.createOrUpdateUser(USER.getLogin(), USER_PASSWORD, USER.getName());
        dl = new FilesystemDL(BASE);
        dl.createResearchObject(RO_URI, new ByteArrayInputStream(MAIN_FILE_CONTENT.getBytes()), MAIN_FILE_PATH,
            MAIN_FILE_MIME_TYPE);

        files[0] = new FileRecord("singleFiles/file1.txt", "file1.txt", "text/plain");
        files[1] = new FileRecord("singleFiles/file2.txt", "dir/file2.txt", "text/plain");
        files[2] = new FileRecord("singleFiles/file3.jpg", "testdir/file3.jpg", "image/jpg");
        Files.createDirectories(Paths.get(BASE));
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown()
            throws Exception {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        dl = new FilesystemDL(BASE);
        dl.deleteResearchObject(RO_URI);
        dl = new FilesystemDL(BASE);
        dl.deleteUser(USER.getLogin());
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        try {
            Files.delete(Paths.get(BASE));
        } catch (DirectoryNotEmptyException | NoSuchFileException e) {
            // was not empty
        }
    }


    @Test
    public final void testAddingResources()
            throws DigitalLibraryException, IOException, NotFoundException, ConflictException, AccessDeniedException {
        createOrUpdateFile(files[0]);
        createOrUpdateFile(files[1]);
        getZippedVersion();
        getFileContent(files[0]);
        getFileContent(files[1]);
        checkFileExists(files[0].path);
        getZippedFolder(directories[1]);
        createOrUpdateFile(files[0]);
        createOrUpdateFile(files[1]);
        deleteFile(files[0].path);
        deleteFile(files[1].path);
        checkNoFile(files[0].path);
        checkNoFile(files[1].path);
    }


    @Test
    @Ignore
    public final void testEmptyDirectory()
            throws DigitalLibraryException, IOException, NotFoundException, AccessDeniedException {
        createOrUpdateDirectory(directories[1]);
        getZippedFolder(directories[1]);
        createOrUpdateFile(files[1]);
        deleteFile(files[1].path);
        getZippedFolder(directories[1]);
        deleteFile(directories[1]);
        checkNoFile(directories[1]);
    }


    @Test
    @Ignore
    public final void testPermissions()
            throws DigitalLibraryException, IOException, NotFoundException, ConflictException, AccessDeniedException {
        createOrUpdateFile(files[0]);
        createOrUpdateFile(files[1]);
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        dl = new FilesystemDL(BASE);
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        getFileContent(files[0]);
        getFileContent(files[1]);
        checkCantCreateOrUpdateFile(files[0]);
        checkCantCreateOrUpdateFile(files[1]);
    }


    private void checkNoFile(String path)
            throws DigitalLibraryException, IOException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            dl.getFileContents(RO_URI, path).close();
            fail("Deleted file doesn't throw IdNotFoundException");
        } catch (NotFoundException e) {
            // good
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void checkFileExists(String path)
            throws DigitalLibraryException, NotFoundException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            Assert.assertTrue(dl.fileExists(RO_URI, path));
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void deleteFile(String path)
            throws DigitalLibraryException, NotFoundException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            dl.deleteFile(RO_URI, path);
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void getZippedFolder(String path)
            throws DigitalLibraryException, IOException, NotFoundException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            InputStream zip = dl.getZippedFolder(RO_URI, path);
            assertNotNull(zip);
            zip.close();
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void getFileContent(FileRecord file)
            throws DigitalLibraryException, IOException, NotFoundException, AccessDeniedException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            InputStream f = dl.getFileContents(RO_URI, file.path);
            assertNotNull(f);
            f.close();
            assertEquals(file.mimeType, dl.getFileInfo(RO_URI, file.path).getMimeType());
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void getZippedVersion()
            throws DigitalLibraryException, NotFoundException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            InputStream zip1 = dl.getZippedResearchObject(RO_URI);
            assertNotNull(zip1);
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void createOrUpdateFile(FileRecord file)
            throws DigitalLibraryException, IOException, NotFoundException, AccessDeniedException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            InputStream f = file.open();
            ResourceMetadata r1 = dl.createOrUpdateFile(RO_URI, file.path, f, file.mimeType);
            f.close();
            assertNotNull(r1);
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private void checkCantCreateOrUpdateFile(FileRecord file)
            throws DigitalLibraryException, IOException, NotFoundException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        InputStream f = file.open();
        try {
            dl.createOrUpdateFile(RO_URI, file.path, f, file.mimeType);
            fail("Should throw an exception when creating file");
        } catch (Exception e) {
            // good
        } finally {
            f.close();
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }

    }


    private void createOrUpdateDirectory(String path)
            throws DigitalLibraryException, IOException, NotFoundException, AccessDeniedException {
        HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().begin();
        try {
            ResourceMetadata r1 = dl.createOrUpdateFile(RO_URI, path, new ByteArrayInputStream(new byte[0]),
                "text/plain");
            assertNotNull(r1);
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
        }
    }


    private class FileRecord {

        public String name;

        public String path;

        public String mimeType;


        /**
         * @param name
         * @param dir
         * @param path
         */
        public FileRecord(String name, String path, String mimeType) {
            this.name = name;
            this.path = path;
            this.mimeType = mimeType;
        }


        public InputStream open() {
            return this.getClass().getClassLoader().getResourceAsStream(name);
        }
    }

}
