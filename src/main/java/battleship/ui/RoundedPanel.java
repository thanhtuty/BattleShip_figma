package battleship.ui;

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {
    private final Color fill;
    public RoundedPanel(LayoutManager lm,Color fill){super(lm);this.fill=fill;setOpaque(false);}
    @Override protected void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);
        g2.setColor(Theme.STROKE);g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);
        g2.dispose();super.paintComponent(g);
    }
}
