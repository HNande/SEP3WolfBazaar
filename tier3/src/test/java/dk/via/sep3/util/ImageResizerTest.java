package dk.via.sep3.util;

import dk.via.sep3.support.TestImages;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageResizerTest {

    @Test
    void smallImageIsReturnedUnchanged() throws IOException {
        byte[] original = TestImages.png(120, 80);

        ImageResizer.Resized resized = ImageResizer.fitWithin(original, "PNG");

        assertThat(resized.bytes()).isSameAs(original);
        assertThat(resized.width()).isEqualTo(120);
        assertThat(resized.height()).isEqualTo(80);
    }

    @Test
    void imageExactlyAtTheLimitIsNotResized() throws IOException {
        byte[] original = TestImages.png(ImageResizer.MAX_SIZE, ImageResizer.MAX_SIZE);

        assertThat(ImageResizer.fitWithin(original, "PNG").bytes()).isSameAs(original);
    }

    @Test
    void wideImageIsScaledDownKeepingAspectRatio() throws IOException {
        ImageResizer.Resized resized = ImageResizer.fitWithin(TestImages.png(1000, 500), "PNG");

        assertThat(resized.width()).isEqualTo(200);
        assertThat(resized.height()).isEqualTo(100);
        assertThat(ImageResizer.dimensions(resized.bytes())).containsExactly(200, 100);
    }

    @Test
    void tallJpegIsScaledDownAndStaysReadable() throws IOException {
        ImageResizer.Resized resized = ImageResizer.fitWithin(TestImages.jpg(300, 900), "jpeg");

        assertThat(resized.width()).isEqualTo(67);
        assertThat(resized.height()).isEqualTo(200);
        assertThat(ImageResizer.dimensions(resized.bytes())).containsExactly(67, 200);
    }

    @Test
    void extremeAspectRatioNeverProducesAZeroDimension() throws IOException {
        ImageResizer.Resized resized = ImageResizer.fitWithin(TestImages.png(2000, 1), "PNG");

        assertThat(resized.width()).isEqualTo(200);
        assertThat(resized.height()).isEqualTo(1);
    }

    @Test
    void unreadableBytesAreRejected() {
        byte[] garbage = {1, 2, 3, 4};

        assertThatThrownBy(() -> ImageResizer.fitWithin(garbage, "PNG"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImageResizer.dimensions(garbage))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dimensionsReadsWidthAndHeight() throws IOException {
        assertThat(ImageResizer.dimensions(TestImages.png(13, 7))).containsExactly(13, 7);
    }
}
