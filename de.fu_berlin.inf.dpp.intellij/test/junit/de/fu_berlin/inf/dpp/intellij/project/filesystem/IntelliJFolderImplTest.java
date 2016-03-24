package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import de.fu_berlin.inf.dpp.filesystem.IFolder;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LocalFileSystem.class, ApplicationManager.class,
    Application.class })
public class IntelliJFolderImplTest extends AbstractResourceTest {

    public static final String TEST_FOLDER_NAME = "folder";
    public static final String CHILD_DIRECTORY_NAME = "childDirectory";
    public static final String OTHER_CHILD_DIRECTORY_NAME = "otherChildDirectory";
    public static final String CHILD_FILE_NAME = "childFile.txt";

    @Test
    public void testCreate() throws IOException {
        mockApplicationManager();
        mockFileSystem();

        IFolder folder = new IntelliJFolderImpl(getMockProject(),
            new File(TEST_FOLDER_NAME));

        folder.create(false, false);
        assertThat(folder.exists()).isTrue();
    }

    @Test
    public void testMembers() throws IOException {
        mockApplicationManager();
        mockFileSystem();
        IFolder folder = createTestHierarchy();

        assertThat(folder.members()).hasSize(3)
            .isEqualTo(folder.members(IResource.NONE));
    }

    @Test
    public void testMemberFiles() throws IOException {
        mockApplicationManager();
        mockFileSystem();
        IFolder folder = createTestHierarchy();

        assertThat(folder.members(IResource.FILE)).hasSize(1);
    }

    @Test
    public void testMemberFolders() throws IOException {
        mockApplicationManager();
        mockFileSystem();
        IFolder folder = createTestHierarchy();

        assertThat(folder.members(IResource.FOLDER)).hasSize(2);
    }

    private IFolder createTestHierarchy() throws IOException {
        folder.newFolder(TEST_PROJECT_NAME, TEST_FOLDER_NAME);
        folder.newFolder(TEST_PROJECT_NAME, TEST_FOLDER_NAME,
            CHILD_DIRECTORY_NAME);
        folder.newFolder(TEST_PROJECT_NAME, TEST_FOLDER_NAME,
            OTHER_CHILD_DIRECTORY_NAME);
        folder.newFile(
            TEST_PROJECT_NAME + '/' + TEST_FOLDER_NAME + '/' + CHILD_FILE_NAME);

        return new IntelliJFolderImpl(getMockProject(),
            new File(TEST_FOLDER_NAME));
    }
}