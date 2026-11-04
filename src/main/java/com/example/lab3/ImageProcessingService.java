package com.example.lab3;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    public static final int MAX_WIDTH = 300;
    private static final String WATERMARK = "Instakill Demo";

    /**
     * Scalează la max 300 px lățime (fără upscale, păstrând proporțiile)
     * și adaugă watermark jos-dreapta.
     *
     * @param ext jpg, jpeg, png sau gif
     * @throws IllegalArgumentException dacă fișierul nu e o imagine validă
     */
    public byte[] process(byte[] input, String ext) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
        if (src == null) {
            throw new IllegalArgumentException("Fișierul nu este o imagine validă");
        }

        boolean jpeg = ext.equals("jpg") || ext.equals("jpeg");
        int type = jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

        int w = Math.min(src.getWidth(), MAX_WIDTH);
        int h = Math.max(1, Math.round((float) src.getHeight() * w / src.getWidth()));

        BufferedImage out = new BufferedImage(w, h, type);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.drawImage(src, 0, 0, w, h, null);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, w / 18)));
            FontMetrics fm = g.getFontMetrics();
            int margin = Math.max(4, w / 50);
            int x = w - fm.stringWidth(WATERMARK) - margin;
            int y = h - fm.getDescent() - margin;

            g.setColor(new Color(0, 0, 0, 150));        // umbră
            g.drawString(WATERMARK, x + 1, y + 1);
            g.setColor(new Color(255, 255, 255, 220));  // text
            g.drawString(WATERMARK, x, y);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!ImageIO.write(out, jpeg ? "jpg" : ext, bos)) {
            throw new IOException("Format de ieșire nesuportat: " + ext);
        }
        return bos.toByteArray();
    }
}