package com.architect;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class IconGenerator {

    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Deep slate background with subtle radial glow
        Point2D center = new Point2D.Float(size / 2f, size / 2f);
        float radius = size * 0.7f;
        float[] dist = {0.0f, 0.7f, 1.0f};
        Color[] colors = {new Color(0x23, 0x27, 0x33), new Color(0x16, 0x18, 0x20), new Color(0x0c, 0x0d, 0x12)};
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(p);
        g.fillRect(0, 0, size, size);

        // 2. Subtle architectural blueprint grid in the background
        g.setColor(new Color(255, 255, 255, 12));
        g.setStroke(new BasicStroke(1f));
        int gridSize = 32;
        for (int x = 32; x < size; x += gridSize) {
            g.drawLine(x, 32, x, size - 32);
        }
        for (int y = 32; y < size; y += gridSize) {
            g.drawLine(32, y, size - 32, y);
        }

        // Circular blueprint arcs
        g.setColor(new Color(212, 175, 55, 30));
        g.drawOval(size / 2 - 160, size / 2 - 160, 320, 320);
        g.drawOval(size / 2 - 200, size / 2 - 200, 400, 400);

        // 3. Ornate Golden Borders
        // Outer border
        g.setColor(new Color(0xbf, 0x93, 0x3b));
        g.setStroke(new BasicStroke(6f));
        g.drawRect(20, 20, size - 40, size - 40);

        // Inner fine border
        g.setColor(new Color(0xf5, 0xd7, 0x7f));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(28, 28, size - 56, size - 56);

        // Corner decorative ornaments
        g.setColor(new Color(0xf5, 0xd7, 0x7f));
        int cSize = 24;
        // Top-left
        g.fillRect(20, 20, cSize, 6);
        g.fillRect(20, 20, 6, cSize);
        // Top-right
        g.fillRect(size - 20 - cSize, 20, cSize, 6);
        g.fillRect(size - 26, 20, 6, cSize);
        // Bottom-left
        g.fillRect(20, size - 26, cSize, 6);
        g.fillRect(20, size - 20 - cSize, 6, cSize);
        // Bottom-right
        g.fillRect(size - 20 - cSize, size - 26, cSize, 6);
        g.fillRect(size - 26, size - 20 - cSize, 6, cSize);

        // Diamond corner studs
        int[][] studs = {{28, 28}, {size - 28, 28}, {28, size - 28}, {size - 28, size - 28}};
        for (int[] s : studs) {
            int sx = s[0], sy = s[1];
            int[] dx = {sx, sx + 5, sx, sx - 5};
            int[] dy = {sy - 5, sy, sy + 5, sy};
            g.fillPolygon(dx, dy, 4);
        }

        // 4. Central Architect Emblem (Compass & Masonry Wall / Crown)
        int cx = size / 2;
        int cy = 210;

        // Golden glow behind emblem
        RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(cx, cy), 130f,
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(245, 215, 127, 70), new Color(191, 147, 59, 25), new Color(0, 0, 0, 0)});
        g.setPaint(glow);
        g.fillOval(cx - 130, cy - 130, 260, 260);

        // Masonry Battlement / Crown above compass
        g.setColor(new Color(0xf7, 0xd9, 0x86));
        int wallY = cy - 75;
        g.fillRect(cx - 60, wallY, 120, 16);
        // Crenellations
        for (int b = -55; b <= 35; b += 25) {
            g.fillRect(cx + b, wallY - 12, 16, 14);
        }

        // Drawing Compass / Dividers (Vector Art)
        // Hinge / Pivot at top
        g.setPaint(new GradientPaint(cx - 16, cy - 65, new Color(0xff, 0xeb, 0xaa), cx + 16, cy - 35, new Color(0xb8, 0x86, 0x28)));
        g.fillOval(cx - 18, cy - 60, 36, 36);
        g.setColor(new Color(0x3a, 0x2b, 0x08));
        g.fillOval(cx - 7, cy - 49, 14, 14);

        // Compass Arms
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(cx, cy - 45, new Color(0xff, 0xee, 0xbe), cx - 75, cy + 85, new Color(0x9a, 0x6e, 0x16)));
        g.drawLine(cx - 10, cy - 42, cx - 72, cy + 85); // Left arm

        g.setPaint(new GradientPaint(cx, cy - 45, new Color(0xff, 0xee, 0xbe), cx + 75, cy + 85, new Color(0x9a, 0x6e, 0x16)));
        g.drawLine(cx + 10, cy - 42, cx + 72, cy + 85); // Right arm

        // Compass needle tips (silver steel)
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xe0, 0xe8, 0xf0));
        g.drawLine(cx - 72, cy + 85, cx - 80, cy + 108);
        g.drawLine(cx + 72, cy + 85, cx + 80, cy + 108);

        // Measuring arc between arms
        g.setStroke(new BasicStroke(5f));
        g.setColor(new Color(0xdf, 0xb0, 0x48));
        g.drawArc(cx - 65, cy + 15, 130, 75, 205, 130);

        // 5. Typography
        // Top caption: "SONGS OF SYX"
        Font fontSubTop = new Font(Font.SERIF, Font.BOLD, 17);
        g.setFont(fontSubTop);
        g.setColor(new Color(0xdf, 0xb8, 0x60));
        String subTop = "S O N G S   O F   S Y X";
        FontMetrics fmSubTop = g.getFontMetrics();
        g.drawString(subTop, cx - fmSubTop.stringWidth(subTop) / 2, 362);

        // Divider lines beside top caption
        int sw = fmSubTop.stringWidth(subTop) / 2;
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(0xbf, 0x93, 0x3b, 160));
        g.drawLine(cx - sw - 50, 356, cx - sw - 12, 356);
        g.drawLine(cx + sw + 12, 356, cx + sw + 50, 356);

        // Main Title: "SYX ARCHITECT"
        Font fontTitle = new Font(Font.SERIF, Font.BOLD, 42);
        g.setFont(fontTitle);
        FontMetrics fmTitle = g.getFontMetrics();
        String title = "SYX ARCHITECT";
        int titleW = fmTitle.stringWidth(title);
        int titleX = cx - titleW / 2;
        int titleY = 412;

        // Text drop shadow (solid dark + blurred gold)
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString(title, titleX + 3, titleY + 3);

        // Gold gradient fill for title
        GradientPaint titleGrad = new GradientPaint(titleX, titleY - 35, new Color(0xff, 0xf2, 0xc8), titleX, titleY, new Color(0xc9, 0x95, 0x2b));
        g.setPaint(titleGrad);
        g.drawString(title, titleX, titleY);

        // Golden accent bar beneath title
        g.setPaint(new GradientPaint(cx - 140, 0, new Color(0,0,0,0), cx, 0, new Color(0xf0, 0xca, 0x68)));
        g.fillRect(cx - 140, 426, 140, 3);
        g.setPaint(new GradientPaint(cx, 0, new Color(0xf0, 0xca, 0x68), cx + 140, 0, new Color(0,0,0,0)));
        g.fillRect(cx, 426, 140, 3);

        // Bottom Subtitle: "CREATIVE & SCHÖNBAU SUITE"
        Font fontBottom = new Font(Font.SANS_SERIF, Font.BOLD, 17);
        g.setFont(fontBottom);
        FontMetrics fmBottom = g.getFontMetrics();
        String bottom = "CREATIVE & SCHÖNBAU SUITE";
        int bottomW = fmBottom.stringWidth(bottom);
        g.setColor(new Color(0xee, 0xf2, 0xf6));
        g.drawString(bottom, cx - bottomW / 2, 456);

        g.dispose();

        File out1 = new File("mod-files/icon.png");
        out1.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out1);
        System.out.println("Generated: " + out1.getAbsolutePath());
    }
}
