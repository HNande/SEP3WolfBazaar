package dk.via.sep3.support;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Builds small real images in memory so tests do not depend on fixture files. */
public final class TestImages {

    private TestImages() {
    }

    public static byte[] png(int width, int height) {
        return encode(width, height, BufferedImage.TYPE_INT_ARGB, "png");
    }

    public static byte[] jpg(int width, int height) {
        return encode(width, height, BufferedImage.TYPE_INT_RGB, "jpg");
    }

    private static byte[] encode(int width, int height, int type, String format) {
        BufferedImage image = new BufferedImage(width, height, type);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
