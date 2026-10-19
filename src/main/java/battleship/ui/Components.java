package battleship.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Components {
    private Components(){}

    public static JLabel label(String text,int size,Color color,boolean bold){
        JLabel l=new JLabel(text);
        l.setForeground(color);
        l.setFont(Theme.font(size,bold));
        return l;
    }

    public static JButton button(String text,Color bg){
        return new RoundButton(text,bg);
    }

    public static JTextField field(){
        JTextField f=new JTextField();
        styleField(f);
        return f;
    }

    public static JPasswordField password(){
        JPasswordField f=new JPasswordField();
        styleField(f);
        return f;
    }

    private static void styleField(JTextField f){
        f.setFont(Theme.font(15,false));
        f.setForeground(Theme.TEXT);
        f.setBackground(Theme.NAV);
        f.setCaretColor(Theme.CYAN);
        f.setSelectionColor(Theme.PANEL2);
        f.setSelectedTextColor(Theme.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.STROKE),
                new EmptyBorder(9,14,9,14)));
    }

    public static class RoundButton extends JButton {
        private final Color bg;
        public RoundButton(String text,Color bg){
            super(text);
            this.bg=bg;
            setFont(Theme.font(14,true));
            setForeground(bg.equals(Theme.CYAN)||bg.equals(Theme.GOLD)||bg.equals(Theme.GREEN)?Theme.BG:Theme.TEXT);
            setFocusPainted(false);
            setBorder(new EmptyBorder(10,16,10,16));
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Color c=isEnabled()?bg:bg.darker();
            if(getModel().isRollover()) c=c.brighter();
            if(getModel().isPressed()) c=c.darker();
            g2.setColor(c);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),18,18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
