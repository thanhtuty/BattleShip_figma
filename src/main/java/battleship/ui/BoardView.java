package battleship.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public class BoardView extends JPanel {
    private final JButton[][] cells=new JButton[10][10];
    private BiConsumer<Integer,Integer> click;
    private final int cell;

    public BoardView(){ this(32); }

    public BoardView(int cellSize){
        this.cell=cellSize;
        int size=cell*10+34;
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(size,size));

        for(int c=0;c<10;c++){
            JLabel l=Components.label(String.valueOf((char)('A'+c)),10,Theme.MUTED,true);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBounds(26+c*cell,0,cell,22);
            add(l);
        }
        for(int r=0;r<10;r++){
            JLabel l=Components.label(String.valueOf(r+1),10,Theme.MUTED,true);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBounds(0,24+r*cell,24,cell);
            add(l);
        }

        for(int r=0;r<10;r++) for(int c=0;c<10;c++){
            final int rr=r,cc=c;
            JButton b=new JButton();
            b.setBounds(26+c*cell,24+r*cell,cell-2,cell-2);
            b.setBackground(Theme.GRID);
            b.setForeground(Theme.BG);
            b.setFont(Theme.font(Math.max(12,cell/2),true));
            b.setBorder(BorderFactory.createLineBorder(Theme.PANEL2));
            b.setFocusPainted(false);
            b.setMargin(new Insets(0,0,0,0));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e->{ if(click!=null) click.accept(rr,cc); });
            cells[r][c]=b;
            add(b);
        }
    }

    public int boardPixelSize(){ return cell*10+34; }
    public void onClick(BiConsumer<Integer,Integer> c){ click=c; }

    public void clear(boolean own){
        for(var row:cells) for(var b:row){
            b.setText("");
            b.setBackground(Theme.GRID);
            b.setForeground(Theme.BG);
            b.setEnabled(true);
        }
    }

    public void ship(int r,int c){
        cells[r][c].setBackground(Theme.CYAN);
    }

    public void shot(int r,int c,boolean hit){
        JButton b=cells[r][c];
        b.setBackground(hit?Theme.RED:Theme.TEXT);
        b.setForeground(Theme.BG);
        b.setText(hit?"×":"•");
        b.setEnabled(false);
    }

    public void setInteractive(boolean enabled){
        for(var row:cells) for(var b:row) if(b.getText().isEmpty()) b.setEnabled(enabled);
    }
}
