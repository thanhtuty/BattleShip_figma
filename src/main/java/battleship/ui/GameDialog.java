package battleship.ui;

import javax.swing.*;
import java.awt.*;

public final class GameDialog {
    private GameDialog(){}

    public static void show(Window owner,String title,String message,boolean problem){
        JDialog dialog=new JDialog(owner,title,Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        JPanel outer=new JPanel(new BorderLayout());
        outer.setBackground(Theme.STROKE);
        outer.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        RoundedPanel body=new RoundedPanel(null,Theme.PANEL);
        body.setPreferredSize(new Dimension(480,235));
        outer.add(body);

        JLabel heading=Components.label((problem?"!  ":"✓  ")+title.toUpperCase(),21,problem?Theme.RED:Theme.CYAN,true);
        heading.setBounds(28,25,420,35);body.add(heading);
        JLabel detail=Components.label("<html><div style='width:400px'>"+safe(message)+"</div></html>",14,Theme.TEXT,false);
        detail.setVerticalAlignment(SwingConstants.TOP);
        detail.setBounds(28,82,424,74);body.add(detail);
        JButton ok=Components.button("ĐÓNG",problem?Theme.RED:Theme.CYAN);
        ok.setBounds(310,167,143,43);
        ok.addActionListener(e->dialog.dispose());body.add(ok);
        dialog.setContentPane(outer);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void toast(Window owner,String message,boolean warning){
        JDialog dialog=new JDialog(owner);
        dialog.setUndecorated(true);
        RoundedPanel panel=new RoundedPanel(null,Theme.PANEL);
        panel.setPreferredSize(new Dimension(480,108));
        JLabel title=Components.label(warning?"CẢNH BÁO LƯỢT CHƠI":"CẬP NHẬT TRẬN",16,warning?Theme.GOLD:Theme.CYAN,true);
        title.setBounds(18,12,440,30);panel.add(title);
        JLabel text=Components.label("<html><div style='width:432px'>"+safe(message)+"</div></html>",13,Theme.TEXT,false);
        text.setBounds(18,47,438,51);panel.add(text);
        dialog.setContentPane(panel);dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setLocation(dialog.getX(),dialog.getY()-240);
        javax.swing.Timer expiry=new javax.swing.Timer(2500,e->dialog.dispose());
        expiry.setRepeats(false);expiry.start();
        dialog.setVisible(true);
    }

    public static boolean confirm(Window owner,String title,String message){
        final boolean[] accepted={false};
        JDialog dialog=new JDialog(owner,title,Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        RoundedPanel body=new RoundedPanel(null,Theme.PANEL);
        body.setPreferredSize(new Dimension(480,230));
        JLabel heading=Components.label(title,21,Theme.CYAN,true);
        heading.setBounds(26,24,420,37);body.add(heading);
        JLabel detail=Components.label("<html><div style='width:400px'>"+safe(message)+"</div></html>",15,Theme.TEXT,false);
        detail.setBounds(26,78,420,72);body.add(detail);
        JButton yes=Components.button("XÁC NHẬN",Theme.RED);
        yes.setBounds(265,164,185,45);body.add(yes);
        yes.addActionListener(e->{accepted[0]=true;dialog.dispose();});
        JButton no=Components.button("HỦY",Theme.PANEL2);
        no.setBounds(26,164,210,45);body.add(no);
        no.addActionListener(e->dialog.dispose());
        dialog.setContentPane(body);dialog.pack();dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
        return accepted[0];
    }

    private static String safe(String s){
        return (s==null?"":s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
