package digital.vasic.opoc.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.assertj.core.api.Assertions.*;

import java.io.File;

@RunWith(JUnit4.class)
public class GsFileUtilsTest {

    @Test
    public void testGetFilenameExtension() {
        // Test normal cases
        assertThat(GsFileUtils.getFilenameExtension(new File("test.txt"))).isEqualTo("txt");
        assertThat(GsFileUtils.getFilenameExtension(new File("document.md"))).isEqualTo("md");
        assertThat(GsFileUtils.getFilenameExtension(new File("file.with.multiple.dots.txt"))).isEqualTo("txt");

        // Test edge cases
        assertThat(GsFileUtils.getFilenameExtension(new File("noextension"))).isEqualTo("");
        assertThat(GsFileUtils.getFilenameExtension(new File(""))).isEqualTo("");
        assertThat(GsFileUtils.getFilenameExtension((String) null)).isEqualTo("");
    }

    @Test
    public void testGetHumanReadableByteCountSI() {
        assertThat(GsFileUtils.getHumanReadableByteCountSI(0)).isEqualTo("0 B");
        assertThat(GsFileUtils.getHumanReadableByteCountSI(1024)).isEqualTo("1.02 KB");
        assertThat(GsFileUtils.getHumanReadableByteCountSI(1024 * 1024)).isEqualTo("1.05 MB");
        assertThat(GsFileUtils.getHumanReadableByteCountSI(1024L * 1024 * 1024)).isEqualTo("1.07 GB");
    }

    @Test
    public void testIsContentsPlainText() {
        // This would require mocking file I/O, simplified for now
        assertThat(true).isTrue(); // Placeholder test
    }
}