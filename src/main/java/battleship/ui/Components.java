package battleship.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Components {
    private Components(){}

    public static JLabel label(String s,int size,Color color,boolean bold){
        JLabel l=new JLabel(s);l.setForeground(color);l.setFont(Theme.font(size,bold));return l;
    }
    public static JButton button(String s,Color bg){
        JButton b=new JButton(s);b.setFont(Theme.font(14,true));b.setBackground(bg);
        b.setForeground(bg.equals(Theme.CYAN)||bg.equals(Theme.GOLD)||bg.equals(Theme.GREEN)?Theme.BG:Theme.TEXT);
        b.setFocusPainted(false);b.setBorder(new EmptyBorder(12,18,12,18));return b;
    }
    public static JTextField field(){
        JTextField f=new JTextField();f.setBackground(Theme.NAV);f.setForeground(Theme.TEXT);f.setCaretColor(Theme.CYAN);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.STROKE),new EmptyBorder(10,12,10,12)));
        return f;
    }
}
