package org.rohub.rodl.storage;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.rohub.rodl.db.ResourceInfo;
import org.rohub.rodl.db.UserProfile;
import org.rohub.rodl.db.dao.ResourceInfoDAO;
import org.rohub.rodl.db.dao.UserProfileDAO;
import org.rohub.rodl.db.hibernate.HibernateUtil;
import org.rohub.rodl.dl.AccessDeniedException;
import org.rohub.rodl.dl.ConflictException;
import org.rohub.rodl.dl.DigitalLibrary;
import org.rohub.rodl.dl.DigitalLibraryException;
import org.rohub.rodl.dl.NotFoundException;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.dl.UserMetadata.Role;

import com.google.common.collect.Multimap;

/**
 * Filesystem-based digital library.
 * 
 * @author piotrekhol
 * 
 */
public class FilesystemDL implements DigitalLibrary {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(FilesystemDL.class);

    /** base path under which the files will be stored. */
    private Path basePath;
    

    /**
     * Constructor.
     * 
     * @param basePath
     *            file path under which the files will be stored
     */
    public FilesystemDL(String basePath) {
        if (basePath.endsWith("/")) {
            this.basePath = FileSystems.getDefault().getPath(basePath);
        } else {
            this.basePath = FileSystems.getDefault().getPath(basePath.concat("/"));
        }
    }


    /**
     * Get resource paths for a folder path.
     * 
     * @param path
     *            path to the folder
     * @return list of resources, excluding folders
     * @throws DigitalLibraryException
     *             an error while traversing the filesystem
     */
    private List<Path> getResourcePaths(Path path)
            throws DigitalLibraryException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (entry.toFile().isDirectory()) {
                    result.addAll(getResourcePaths(entry));
                } else {
                    result.add(entry);
                }
            }
        } catch (DirectoryIteratorException | IOException e) {
            // I/O error encounted during the iteration, the cause is an IOException
            throw new DigitalLibraryException(e);
        }
        return result;
    }


    @Override
    public InputStream getZippedFolder(URI ro, String folder)
            throws DigitalLibraryException, NotFoundException {
        final Path roPath = getPath(ro, null);
        Path path = getPath(ro, folder);
        final List<Path> paths = getResourcePaths(path);

        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out;
        try {
            out = new PipedOutputStream(in);
        } catch (IOException e) {
            throw new RuntimeException("This should never happen", e);
        }
        final ZipOutputStream zipOut = new ZipOutputStream(out);
        new Thread("edition zip downloader (" + path.toString() + ")") {

            @Override
            public void run() {
                try {
                    for (Path filePath : paths) {
                        ZipEntry entry = new ZipEntry(roPath.relativize(filePath).normalize().toString());
                        zipOut.putNextEntry(entry);
                        InputStream in = Files.newInputStream(filePath);
                        try {
                            IOUtils.copy(in, zipOut);
                        } finally {
                            in.close();
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Zip transmission failed", e);
                } finally {
                    try {
                        zipOut.close();
                    } catch (Exception e) {
                        LOGGER.warn("Could not close the ZIP file: " + e.getMessage());
                        try {
                            out.close();
                        } catch (IOException e1) {
                            LOGGER.error("Could not close the ZIP output stream", e1);
                        }
                    }
                }
            };
        }.start();
        return in;
    }


    @Override
    public InputStream getFileContents(URI ro, String filePath)
            throws DigitalLibraryException, NotFoundException {
        Path path = getPath(ro, filePath);
        try {
            return Files.newInputStream(path);
        } catch (NoSuchFileException e) {
            throw new NotFoundException("File doesn't exist", e);
        } catch (IOException e) {
            throw new DigitalLibraryException(e);
        }
    }


    @Override
    public boolean fileExists(URI ro, String filePath)
            throws DigitalLibraryException {
        Path path = getPath(ro, filePath);
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }


    @Override
    public ResourceInfo createOrUpdateFile(URI ro, String filePath, InputStream inputStream, String mimeType)
            throws DigitalLibraryException {
        Path path = getPath(ro, filePath);
        try {
            Files.createDirectories(path.getParent());
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            return updateFileInfo(ro, filePath, mimeType);
        } catch (IOException e) {
            throw new DigitalLibraryException(e);
        }
    }


    @Override
    public ResourceInfo updateFileInfo(URI ro, String filePath, String mimeType)
            throws NotFoundException, DigitalLibraryException, AccessDeniedException {
        try {
            Path path = getPath(ro, filePath);
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            String md5;
            if (attributes.isRegularFile()) {
                FileInputStream fis = new FileInputStream(path.toFile());
                md5 = DigestUtils.md5Hex(fis);
            } else {
                LOGGER.warn(path.toString() + " is not a regular file, the checksum will not be calculated.");
                md5 = null;
            }

            DateTime lastModified = new DateTime(attributes.lastModifiedTime().toMillis());
            ResourceInfoDAO dao = new ResourceInfoDAO();
            ResourceInfo res = dao.create(path.toString(), path.getFileName().toString(), md5, attributes.size(),
                "MD5", lastModified, mimeType);
            dao.save(res);
            
            File roDirectory = new File(getPath(ro, "").toString());
            LOGGER.debug("RO folder: " + roDirectory.getPath());
            
            return res;
        } catch (IOException e) {
            throw new DigitalLibraryException(e);
        }
    }


    @Override
    public ResourceInfo getFileInfo(URI ro, String filePath) {
        Path path = getPath(ro, filePath);
        ResourceInfoDAO dao = new ResourceInfoDAO();
        LOGGER.debug("Searching stats for " + path.toString());
        return dao.findByPath(path.toString());
    }


    @Override
    public void deleteFile(URI ro, String filePath)
            throws DigitalLibraryException, NotFoundException {
        Path path = getPath(ro, filePath);
        try {
            try {
                Files.delete(path);
            } catch (DirectoryNotEmptyException e) {
                LOGGER.debug("Won't delete a folder from DL storage: " + e.getMessage());
            }
            ResourceInfoDAO dao = new ResourceInfoDAO();
            ResourceInfo res = dao.findByPath(path.toString());
            if (res != null) {
                dao.delete(res);
            } else {
                LOGGER.warn("Resource info not found in database: " + path);
            }
            HibernateUtil.getSessionFactory().getCurrentSession().flush();
        } catch (NoSuchFileException e) {
            throw new NotFoundException("File doesn't exist: " + path, e);
        } catch (IOException e) {
            throw new DigitalLibraryException(e);
        }
        try {
            path = path.getParent();
            while (path != null && !path.equals(basePath)) {
                Files.delete(path);
                path = path.getParent();
            }
        } catch (DirectoryNotEmptyException e) {
            //it was non empty
            LOGGER.debug("Tried to delete a directory: " + e.getMessage());
        } catch (IOException e) {
            throw new DigitalLibraryException(e);
        }
    }


    @Override
    public void createResearchObject(URI ro, InputStream mainFileContent, String mainFilePath, String mainFileMimeType)
            throws DigitalLibraryException, ConflictException {
        createOrUpdateFile(ro, mainFilePath, mainFileContent, mainFileMimeType);
    }


    @Override
    public void deleteResearchObject(URI ro)
            throws DigitalLibraryException, NotFoundException {
        Path path = getPath(ro, null);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    ResourceInfoDAO dao = new ResourceInfoDAO();
                    ResourceInfo res = dao.findByPath(file.toString());
                    dao.delete(res);
                    HibernateUtil.getSessionFactory().getCurrentSession().flush();
                    return FileVisitResult.CONTINUE;
                }


                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }

            });
        } catch (NoSuchFileException e) {
            throw new NotFoundException("RO doesn't exist: " + ro.toString(), e);
        } catch (IOException e) {
            throw new DigitalLibraryException(e);
        }
    }


    @Override
    public boolean createOrUpdateUser(String login, String password, String username)
            throws DigitalLibraryException {
        UserProfile.Role role;
        if (login.equals("wfadmin")) {
            role = Role.ADMIN;
        } else if (login.equals("wf4ever_reader")) {
            role = Role.PUBLIC;
        } else {
            role = Role.AUTHENTICATED;
        }
        UserProfileDAO dao = new UserProfileDAO();
        if (userExists(login)) {
            UserProfile updatedUser = dao.findByLogin(login);
            updatedUser.setName(username);
            dao.save(updatedUser);
            HibernateUtil.getSessionFactory().getCurrentSession().flush();
            return false;
        }
        UserProfile user2 = dao.create(login, username, role);
        dao.save(user2);
        HibernateUtil.getSessionFactory().getCurrentSession().flush();
        return true;
    }


    @Override
    public UserMetadata getUserProfile(String login) {
        UserProfileDAO dao = new UserProfileDAO();
        return dao.findByLogin(login);
    }


    @Override
    public boolean userExists(String userId)
            throws DigitalLibraryException {
        UserProfileDAO dao = new UserProfileDAO();
        return dao.findByLogin(userId) != null;
    }


    @Override
    public void deleteUser(String userId)
            throws DigitalLibraryException, NotFoundException {
        UserProfileDAO dao = new UserProfileDAO();
        UserProfile user2 = dao.findByLogin(userId);
        if (user2 == null) {
            throw new NotFoundException("user not found");
        } else {
            dao.delete(user2);
            HibernateUtil.getSessionFactory().getCurrentSession().flush();
        }
    }


    @Override
    public InputStream getZippedResearchObject(URI ro)
            throws DigitalLibraryException, NotFoundException {
        return getZippedFolder(ro, ".");
    }


    @Override
    public void storeAttributes(URI ro, Multimap<URI, Object> roAttributes)
            throws NotFoundException, DigitalLibraryException {
        // TODO Auto-generated method stub

    }


    /**
     * Calculate path from a resource URI.
     * 
     * @param ro
     *            research object
     * @param resourcePath
     *            path or null
     * @return filesystem path
     */
    private Path getPath(URI ro, String resourcePath) {
        Path path = basePath;
        if (ro.getHost() != null) {
            path = path.resolve(ro.getHost());
        }
        if (ro.getPath() != null) {
            if (ro.getPath().startsWith("/")) {
                path = path.resolve(ro.getPath().substring(1));
            } else {
                path = path.resolve(ro.getPath());
            }
        }
        if (resourcePath != null) {
            path = path.resolve(resourcePath);
        }
        return path.normalize();
    }

}
