package de.fu_berlin.inf.dpp.intellij.project.filesystem;

import com.intellij.openapi.vfs.LocalFileSystem;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LocalFileSystem.class })
public class IntelliJProjectImplTest extends AbstractResourceTest {

    @Test
    public void testGetType() {
        mockFileSystem();
        IProject project = new IntelliJProjectImpl(getMockWorkspace(),
            new File("Project"));

        assertThat(project.getType()).isEqualTo(IResource.PROJECT);
    }
}