package org.rohub.rodl.db;


import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.rohub.rodl.db.ResourceInfo;

public class ResourceInfoTest {

    String path = "path";
    String name = "name";
    String checksum = "checksum";
    long sizeInBytes = 100;
    String digestMethod = "method";
    DateTime lastModified = DateTime.now();
    String mimeType = "text/plain";


    @Test
    public void testConstructor() {
        ResourceInfo info = new ResourceInfo(path, name, checksum, sizeInBytes, digestMethod, lastModified, mimeType);
        Assert.assertEquals(path, info.getPath());
        Assert.assertEquals(name, info.getName());
        Assert.assertEquals(checksum, info.getChecksum());
        Assert.assertEquals(sizeInBytes, info.getSizeInBytes());
        Assert.assertEquals(digestMethod, info.getDigestMethod());
        Assert.assertEquals(lastModified, info.getLastModified());
        Assert.assertEquals(mimeType, info.getMimeType());
    }
}
