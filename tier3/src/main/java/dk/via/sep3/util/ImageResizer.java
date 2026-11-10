package dk.via.sep3.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/** Scales images down to fit inside 200x200. Never upscales, keeps the aspect ratio. */
public final class ImageResizer {

    public static final int MAX_SIZE = 200;

    private ImageResizer() {
    }

    public record Resized(byte[] bytes, int width, int height) {}

    public static Resized fitWithin(byte[] original, String format) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
        if (src == null) {
            throw new IllegalArgumentException("Not a readable image");
        }
        int w = src.getWidth();
        int h = src.getHeight();

        if (w <= MAX_SIZE && h <= MAX_SIZE) {
            return new Resized(original, w, h);
        }

        double scale = Math.min((double) MAX_SIZE / w, (double) MAX_SIZE / h);
        int targetWidth = Math.max(1, (int) Math.round(w * scale));
        int targetHeight = Math.max(1, (int) Math.round(h * scale));

        boolean jpeg = format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg");

        BufferedImage current = src;
        while (current.getWidth() > targetWidth * 2 || current.getHeight() > targetHeight * 2) {
            int nextW = Math.max(targetWidth, current.getWidth() / 2);
            int nextH = Math.max(targetHeight, current.getHeight() / 2);
            current = scaleStep(current, nextW, nextH, jpeg);
        }
        BufferedImage dst = scaleStep(current, targetWidth, targetHeight, jpeg);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(dst, jpeg ? "jpg" : "png", out)) {
            throw new IOException("No image writer available for " + format);
        }
        return new Resized(out.toByteArray(), targetWidth, targetHeight);
    }

    public static int[] dimensions(byte[] image) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(image));
        if (img == null) {
            throw new IllegalArgumentException("Not a readable image");
        }
        return new int[]{img.getWidth(), img.getHeight()};
    }

    private static BufferedImage scaleStep(BufferedImage src, int nw, int nh, boolean jpeg) {
        BufferedImage dst = new BufferedImage(nw, nh, jpeg ? TYPE_INT_RGB : TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        if (jpeg) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
        }
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }
}
