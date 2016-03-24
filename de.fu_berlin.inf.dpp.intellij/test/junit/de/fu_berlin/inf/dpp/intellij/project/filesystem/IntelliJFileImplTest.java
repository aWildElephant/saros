/*
 *
 *  DPP - Serious Distributed Pair Programming
 *  (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2010
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 1, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * /
 */

package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import de.fu_berlin.inf.dpp.filesystem.IFile;
import de.fu_berlin.inf.dpp.filesystem.IPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@PrepareForTest({ LocalFileSystem.class, ApplicationManager.class,
    Application.class })
@RunWith(PowerMockRunner.class)
public class IntelliJFileImplTest extends IntelliJResourceImplTest {

    public static final String TEST_FILE_NAME = "testFile.txt";
    public static final String OTHER_FILE_NAME = "otherFile.txt";

    @Test
    public void testIfNotPresentExistIsFalse() throws Exception {
        IFile file = new IntelliJFileImpl(getMockProject(),
            new File(OTHER_FILE_NAME));

        assertTrue(!file.exists());
    }

    @Test
    public void testExists() throws Exception {
        IFile file = createTestFile();

        assertTrue(file.exists());
    }

    @Test
    public void testCreate() throws IOException {
        mockApplicationManager();
        mockFileSystem();
        IFile file = new IntelliJFileImpl(getMockProject(),
            new File(TEST_FILE_NAME));

        file.create(new ByteArrayInputStream(new byte[] {}), false);

        assertTrue(file.exists());
    }

    @Test
    public void testGetSize() throws Exception {
        mockFileSystem();
        IFile file = createFileWithContent();

        assertEquals(file.getSize(), 4);
    }

    @Test
    public void testDelete() throws IOException {
        mockApplicationManager();
        mockFileSystem();
        IFile file = createTestFile();

        file.delete(0);

        assertFalse(file.exists());
    }

    @Test
    public void testMove() throws IOException {
        mockApplicationManager();
        mockFileSystem();
        IFile file = createTestFile();

        String oldPath = file.getLocation().toPortableString();
        IPath destination = IntelliJPathImpl
            .fromString(folder.getRoot().getPath()).append(TEST_PROJECT_NAME)
            .append(OTHER_FILE_NAME);

        file.move(destination, false);

        assertFalse(new File(oldPath).exists());
        assertTrue(new File(destination.toPortableString()).exists());
        assertEquals(file.getLocation(), destination);
    }

    @Test
    public void testGetFullPath() throws Exception {
        IFile file = createTestFile();

        assertEquals(IntelliJPathImpl.fromString(folder.getRoot().getPath())
                .append(TEST_PROJECT_NAME).append(TEST_FILE_NAME),
            file.getFullPath());
    }

    @Test
    public void testGetName() throws Exception {
        IFile file = createTestFile();

        assertEquals(TEST_FILE_NAME, file.getName());
    }

    @Test
    public void testGetProjectRelativePath() throws Exception {
        IFile file = createTestFile();

        assertEquals(TEST_FILE_NAME,
            file.getProjectRelativePath().toPortableString());
    }

    @Test
    public void testIsAccessible() throws Exception {
        IFile file = createTestFile();

        assertTrue(file.isAccessible());
    }

    private IntelliJFileImpl createTestFile() throws IOException {
        folder.newFile(TEST_PROJECT_NAME + "/" + TEST_FILE_NAME);
        return new IntelliJFileImpl(getMockProject(), new File(TEST_FILE_NAME));
    }

    /* This method do not use IntelliJFileImpl#setContents since the VFS is
     * not be available during the tests. */
    private IFile createFileWithContent() throws Exception {
        IFile file = createTestFile();

        FileOutputStream fos = new FileOutputStream(
            file.getLocation().toFile());
        fos.write(new byte[] { 1, 1, 1, 1 });
        fos.close();

        return file;
    }
}