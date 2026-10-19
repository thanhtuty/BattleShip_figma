package battleship.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public class BoardView extends JPanel {
    private final JButton[][] cells=new JButton[10][10];
    private BiConsumer<Integer,Integer> click;

    public BoardView(){
        setLayout(new GridLayout(10,10,2,2));setOpaque(false);
        for(int r=0;r<10;r++)for(int c=0;c<10;c++){
            int rr=r,cc=c;
            JButton b=new JButton();b.setPreferredSize(new Dimension(34,34));b.setBackground(Theme.GRID);
            b.setBorder(BorderFactory.createLineBorder(Theme.PANEL2));b.setFocusPainted(false);
            b.addActionListener(e->{if(click!=null)click.accept(rr,cc);});
            cells[r][c]=b;add(b);
        }
    }
    public void onClick(BiConsumer<Integer,Integer> c){click=c;}
    public void clear(boolean own){
        for(var row:cells)for(var b:row){b.setText("");b.setBackground(Theme.GRID);b.setEnabled(true);}
    }
    public void ship(int r,int c){cells[r][c].setBackground(Theme.CYAN);}
    public void shot(int r,int c,boolean hit){
        JButton b=cells[r][c];b.setBackground(hit?Theme.RED:Theme.TEXT);b.setForeground(Theme.BG);b.setText(hit?"×":"•");b.setEnabled(false);
    }
}
