package com.shc.silenceengine.graphics;

import com.shc.silenceengine.core.SilenceException;
import com.shc.silenceengine.graphics.opengl.Primitive;
import com.shc.silenceengine.graphics.opengl.Texture;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sri Harsha Chilakapati
 */
public class TrueTypeFont
{
    public static final int STYLE_NORMAL = Font.PLAIN;
    public static final int STYLE_BOLD   = Font.BOLD;
    public static final int STYLE_ITALIC = Font.ITALIC;

    private static final int STANDARD_CHARACTERS = 256;

    private FontChar[] chars = new FontChar[STANDARD_CHARACTERS];

    private boolean antiAlias = true;

    private Texture[]   fontTexture;
    private Font        awtFont;
    private FontMetrics fontMetrics;

    public TrueTypeFont(String name, int style, int size)
    {
        this(new Font(name, style, size));
    }

    public TrueTypeFont(InputStream is)
    {
        this(is, true);
    }

    public TrueTypeFont(InputStream is, boolean antiAlias)
    {
        this(is, STYLE_NORMAL, 18, antiAlias);
    }

    public TrueTypeFont(InputStream is, int style, int size, boolean antiAlias)
    {
        try
        {
            this.awtFont = Font.createFont(Font.TRUETYPE_FONT, is);
            this.antiAlias = antiAlias;

            awtFont = awtFont.deriveFont(style, (float) size);

            createSet();
        }
        catch (Exception e)
        {
            throw new SilenceException(e.getMessage());
        }
    }

    public TrueTypeFont(Font fnt)
    {
        this(fnt, true);
    }

    public TrueTypeFont(Font fnt, boolean antiAlias)
    {
        this.awtFont = fnt;
        this.antiAlias = antiAlias;

        createSet();
    }

    private void createSet()
    {
        // A temporary BufferedImage to get access to FontMetrics
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tmp.createGraphics();

        g2d.setFont(awtFont);

        if (antiAlias)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        fontMetrics = g2d.getFontMetrics();

        int positionX = 0;
        int positionY = 0;

        int page = 0;

        final int padding = fontMetrics.getMaxAdvance();
        final int maxTexWidth = 1024;
        final int maxTexHeight = 1024;

        List<Texture> pages = new ArrayList<>();

        for (int i = 0; i < STANDARD_CHARACTERS; i++)
        {
            char ch = (char) i;
            chars[i] = new FontChar();

            if (positionX + 2 * padding > maxTexWidth)
            {
                positionX = 0;
                positionY += fontMetrics.getHeight() + padding;
            }

            if (positionY + 2 * padding > maxTexHeight)
            {
                positionX = positionY = 0;
                page++;
            }

            chars[i].advance = fontMetrics.stringWidth("_" + ch) - fontMetrics.charWidth('_');
            chars[i].padding = padding;
            chars[i].page = page;

            chars[i].x = positionX;
            chars[i].y = positionY;
            chars[i].w = chars[i].advance + (2 * padding);
            chars[i].h = fontMetrics.getHeight();

            positionX += chars[i].w + 10;
        }

        g2d.dispose();

        BufferedImage pageImage = new BufferedImage(maxTexWidth, maxTexHeight, BufferedImage.TYPE_INT_ARGB);
        g2d = pageImage.createGraphics();

        g2d.setFont(awtFont);
        g2d.setColor(java.awt.Color.BLACK);

        if (antiAlias)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        page = 0;

        for (int i = 0; i < STANDARD_CHARACTERS; i++)
        {
            FontChar fntChar = chars[i];

            if (page != fntChar.page)
            {
                g2d.dispose();
                pages.add(Texture.fromBufferedImage(pageImage));

                pageImage = new BufferedImage(maxTexWidth, maxTexHeight, BufferedImage.TYPE_INT_ARGB);
                g2d = pageImage.createGraphics();

                g2d.setFont(awtFont);
                g2d.setColor(java.awt.Color.BLACK);

                if (antiAlias)
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                page = fntChar.page;
            }

            g2d.drawString(String.valueOf((char) i), chars[i].x + padding, chars[i].y + fontMetrics.getAscent());
        }

        g2d.dispose();

        pages.add(Texture.fromBufferedImage(pageImage));

        fontTexture = new Texture[pages.size()];
        fontTexture = pages.toArray(fontTexture);
    }

    public void drawString(Batcher b, String text, float x, float y)
    {
        drawString(b, text, x, y, Color.WHITE);
    }

    public void drawString(Batcher b, String text, float x, float y, Color col)
    {
        Texture current = Texture.CURRENT;

        b.begin(Primitive.TRIANGLES);
        {
            float startX = x;

            Texture page = null;

            for (char ch : text.toCharArray())
            {
                FontChar c = chars[(int) ch];

                if (ch == '\n')
                {
                    y += fontMetrics.getHeight();
                    x = startX;

                    continue;
                }

                Texture charPage = fontTexture[chars[ch].page];

                if (page == null || page != charPage)
                {
                    b.flush();

                    page = charPage;
                    page.bind();
                }

                float minU = c.x / page.getWidth();
                float maxU = (c.x + c.w) / page.getWidth();
                float minV = c.y / page.getHeight();
                float maxV = (c.y + c.h) / page.getHeight();

                b.vertex(x - c.padding, y);
                b.color(col);
                b.texCoord(minU, minV);

                b.vertex(x + chars[ch].w - c.padding, y);
                b.color(col);
                b.texCoord(maxU, minV);

                b.vertex(x - c.padding, y + chars[ch].h);
                b.color(col);
                b.texCoord(minU, maxV);

                b.vertex(x + chars[ch].w - c.padding, y);
                b.color(col);
                b.texCoord(maxU, minV);

                b.vertex(x - c.padding, y + chars[ch].h);
                b.color(col);
                b.texCoord(minU, maxV);

                b.vertex(x + chars[ch].w - c.padding, y + chars[ch].h);
                b.color(col);
                b.texCoord(maxU, maxV);

                x += c.advance;
            }
        }
        b.end();

        current.bind();
    }

    public int getWidth(String str)
    {
        int width = 0;
        int lineWidth = 0;

        for (char ch : str.toCharArray())
        {
            if (ch == '\n')
            {
                width = Math.max(width, lineWidth);
                lineWidth = 0;
                continue;
            }

            lineWidth += chars[(int) ch].advance;
        }

        width = Math.max(width, lineWidth);

        return width;
    }

    public TrueTypeFont derive(float size)
    {
        return new TrueTypeFont(awtFont.deriveFont(size));
    }

    public void dispose()
    {
        for (Texture texture : fontTexture)
            texture.dispose();
    }

    public int getHeight()
    {
        return fontMetrics.getHeight();
    }

    private static class FontChar
    {
        public int x;
        public int y;
        public int w;
        public int h;
        public int advance;
        public int padding;
        public int page;
    }
}
