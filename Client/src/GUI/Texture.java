package GUI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URL;

public class Texture extends Canvas{

    private String imgFileName;
    private Image img;
    private AffineTransform transform;

    public Texture(String fileName) {
        imgFileName = "../cardTexture/" + fileName + ".png";
        URL imgUrl = getClass().getResource(imgFileName);
        transform = new AffineTransform();
        transform.scale(0.25,0.25);

        if (imgUrl == null) {
            System.err.println("Couldn't find file: " + imgFileName);
        } else {
            try {
                img = ImageIO.read(imgUrl);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;

        g2D.drawImage(img, transform, null);
    }

    public void translate(double dx, double dy) {
        transform.translate(dx, dy);
    }

    public void rotate(double theta) {
        transform.rotate(theta);
    }

    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }
}