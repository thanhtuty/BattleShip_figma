package battleship.client;

import battleship.game.FleetCodec;
import battleship.network.Protocol;
import battleship.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientApp extends JFrame {
    private final GameClient net=new GameClient();
    private final CardLayout cards=new CardLayout();
    private final JPanel root=new JPanel(cards);
    private String username="";
    private long matchId=-1;
    private String opponent="";
    private String pendingInvite="";
    private final DefaultListModel<String> onlineModel=new DefaultListModel<>();
    private final DefaultListModel<String> rankingModel=new DefaultListModel<>();
    private final DefaultListModel<String> historyModel=new DefaultListModel<>();
    private final List<FleetCodec.Placement> fleet=new ArrayList<>();
    private final BoardView ownBoard=new BoardView(), enemyBoard=new BoardView();
    private final JLabel roomStatus=Components.label("Chưa sẵn sàng",16,Theme.GOLD,true);
    private final JLabel battleStatus=Components.label("Đang chờ trận bắt đầu",15,Theme.MUTED,true);

    public static void main(String[] args){SwingUtilities.invokeLater(()->new ClientApp().setVisible(true));}

    public ClientApp(){
        setTitle("Battleship – Hải Chiến");
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1200,800);setMinimumSize(new Dimension(1080,700));setLocationRelativeTo(null);
        root.setBackground(Theme.BG);add(root);
        build();net.addListener(this::message);
        show("welcome");
    }

    private JPanel page(){
        JPanel p=new JPanel(new BorderLayout(16,16));p.setBackground(Theme.BG);p.setBorder(new EmptyBorder(24,34,24,34));return p;
    }
    private JPanel header(String title,String sub){
        JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(Components.label("⚓  BATTLESHIP / HẢI CHIẾN",21,Theme.CYAN,true));
        p.add(Box.createVerticalStrut(18));p.add(Components.label(title,30,Theme.TEXT,true));p.add(Box.createVerticalStrut(7));
        p.add(Components.label(sub,14,Theme.MUTED,false));return p;
    }
    private RoundedPanel card(String title){
        RoundedPanel p=new RoundedPanel(new BorderLayout(12,12),Theme.PANEL);p.setBorder(new EmptyBorder(20,20,20,20));
        p.add(Components.label(title,18,Theme.TEXT,true),BorderLayout.NORTH);return p;
    }
    private void show(String n){cards.show(root,n);}
    private void add(String n,JPanel p){root.add(p,n);}

    private void build(){
        add("welcome",welcome());add("login",login());add("register",register());add("lobby",lobby());
        add("friends",friends());add("waiting",waiting());add("placement",placement());add("battle",battle());
        add("result",result());add("ranking",ranking());add("history",history());add("settings",settings());
    }

    private JPanel welcome(){
        JPanel p=page();RoundedPanel h=card("");
        JPanel c=new JPanel();c.setOpaque(false);c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));
        c.add(Components.label("BATTLESHIP",60,Theme.TEXT,true));c.add(Components.label("THỐNG TRỊ ĐẠI DƯƠNG",24,Theme.CYAN,true));
        c.add(Box.createVerticalStrut(34));c.add(Components.label("TCP Multiplayer • Java Swing • SQL Server",17,Theme.MUTED,false));
        c.add(Box.createVerticalStrut(44));JButton b=Components.button("BẮT ĐẦU CHƠI  →",Theme.CYAN);b.addActionListener(e->show("login"));c.add(b);
        h.add(c);p.add(h);return p;
    }

    private JPanel login(){
        JPanel p=page();p.add(header("ĐĂNG NHẬP","Kết nối TCP Server và xác thực tài khoản"),BorderLayout.NORTH);
        RoundedPanel form=card("THÔNG TIN TÀI KHOẢN");form.setPreferredSize(new Dimension(560,430));
        JPanel f=new JPanel(new GridLayout(0,1,8,8));f.setOpaque(false);
        JTextField host=Components.field();host.setText("127.0.0.1");
        JTextField port=Components.field();port.setText("5050");
        JTextField user=Components.field();
        JPasswordField pass=new JPasswordField();pass.setBackground(Theme.NAV);pass.setForeground(Theme.TEXT);pass.setBorder(BorderFactory.createLineBorder(Theme.STROKE));
        f.add(Components.label("Server host",13,Theme.MUTED,true));f.add(host);f.add(Components.label("Port",13,Theme.MUTED,true));f.add(port);
        f.add(Components.label("Username",13,Theme.MUTED,true));f.add(user);f.add(Components.label("Password",13,Theme.MUTED,true));f.add(pass);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);
        JButton login=Components.button("ĐĂNG NHẬP",Theme.CYAN), reg=Components.button("ĐĂNG KÝ",Theme.PANEL2);
        actions.add(login);actions.add(reg);f.add(actions);form.add(f);
        login.addActionListener(e->{
            try{if(!net.isConnected())net.connect(host.getText().trim(),Integer.parseInt(port.getText().trim()));
                net.send("LOGIN",user.getText().trim(),new String(pass.getPassword()));}
            catch(Exception ex){error(ex.getMessage());}
        });
        reg.addActionListener(e->show("register"));
        JPanel center=new JPanel(new GridBagLayout());center.setOpaque(false);center.add(form);p.add(center);return p;
    }

    private JPanel register(){
        JPanel p=page();p.add(header("TẠO TÀI KHOẢN","Đăng ký tài khoản mới trên SQL Server"),BorderLayout.NORTH);
        RoundedPanel form=card("ĐĂNG KÝ");JPanel f=new JPanel(new GridLayout(0,1,8,8));f.setOpaque(false);
        JTextField user=Components.field(),email=Components.field();JPasswordField pass=new JPasswordField();
        pass.setBackground(Theme.NAV);pass.setForeground(Theme.TEXT);
        f.add(Components.label("Username",13,Theme.MUTED,true));f.add(user);f.add(Components.label("Email",13,Theme.MUTED,true));f.add(email);
        f.add(Components.label("Password",13,Theme.MUTED,true));f.add(pass);
        JButton b=Components.button("TẠO TÀI KHOẢN",Theme.CYAN), back=Components.button("QUAY LẠI",Theme.PANEL2);
        b.addActionListener(e->{if(!net.isConnected()){error("Hãy quay lại Login để kết nối server trước.");return;}net.send("REGISTER",user.getText().trim(),email.getText().trim(),new String(pass.getPassword()));});
        back.addActionListener(e->show("login"));f.add(b);f.add(back);form.add(f);
        JPanel center=new JPanel(new GridBagLayout());center.setOpaque(false);center.add(form);p.add(center);return p;
    }

    private JPanel lobby(){
        JPanel p=page();p.add(header("SẢNH HẢI CHIẾN","Người chơi online và chức năng chính"),BorderLayout.NORTH);
        JPanel g=new JPanel(new GridLayout(2,2,16,16));g.setOpaque(false);
        RoundedPanel online=card("NGƯỜI CHƠI ONLINE");
        JList<String> list=new JList<>(onlineModel);list.setBackground(Theme.NAV);list.setForeground(Theme.TEXT);online.add(new JScrollPane(list));
        JButton challenge=Components.button("THÁCH ĐẤU NGƯỜI ĐANG CHỌN",Theme.GOLD);
        challenge.addActionListener(e->{String t=list.getSelectedValue();if(t!=null)net.send("CHALLENGE",t);});online.add(challenge,BorderLayout.SOUTH);
        RoundedPanel profile=card("HỒ SƠ");profile.add(Components.label("Đã đăng nhập • Sẵn sàng chiến đấu",17,Theme.GREEN,true));
        RoundedPanel menu=card("CHỨC NĂNG");JPanel mm=new JPanel(new GridLayout(2,2,10,10));mm.setOpaque(false);
        String[][] items={{"LỜI MỜI / BẠN BÈ","friends"},{"BẢNG XẾP HẠNG","ranking"},{"LỊCH SỬ","history"},{"CÀI ĐẶT","settings"}};
        for(String[] a:items){JButton b=Components.button(a[0],Theme.PANEL2);b.addActionListener(e->{if(a[1].equals("ranking"))net.send("RANKING");if(a[1].equals("history"))net.send("HISTORY");show(a[1]);});mm.add(b);}menu.add(mm);
        RoundedPanel tip=card("HƯỚNG DẪN");tip.add(Components.label("Chọn người online → Thách đấu → Accept → Ready → Bắn theo lượt.",15,Theme.MUTED,false));
        g.add(online);g.add(profile);g.add(menu);g.add(tip);p.add(g);return p;
    }

    private JPanel friends(){
        JPanel p=page();p.add(header("BẠN BÈ & LỜI MỜI","Lời mời thách đấu đến sẽ hiện tại đây"),BorderLayout.NORTH);
        RoundedPanel c=card("LỜI MỜI HIỆN TẠI");JLabel invite=Components.label("Chưa có lời mời",18,Theme.MUTED,true);
        invite.setName("inviteLabel");c.add(invite);
        JPanel a=new JPanel(new FlowLayout(FlowLayout.LEFT));a.setOpaque(false);
        JButton ok=Components.button("CHẤP NHẬN",Theme.GREEN), no=Components.button("TỪ CHỐI",Theme.RED), back=Components.button("VỀ SẢNH",Theme.PANEL2);
        ok.addActionListener(e->{if(!pendingInvite.isBlank())net.send("ACCEPT",pendingInvite);});
        no.addActionListener(e->{if(!pendingInvite.isBlank())net.send("REJECT",pendingInvite);pendingInvite="";invite.setText("Chưa có lời mời");});
        back.addActionListener(e->show("lobby"));a.add(ok);a.add(no);a.add(back);c.add(a,BorderLayout.SOUTH);p.add(c);return p;
    }

    private JPanel waiting(){
        JPanel p=page();p.add(header("PHÒNG CHỜ","Hai người chuẩn bị trước khi đặt tàu"),BorderLayout.NORTH);
        RoundedPanel c=card("MATCH");c.add(roomStatus);
        JButton go=Components.button("BỐ TRÍ HẠM ĐỘI",Theme.CYAN);go.addActionListener(e->show("placement"));c.add(go,BorderLayout.SOUTH);p.add(c);return p;
    }

    private JPanel placement(){
        JPanel p=page();p.add(header("BỐ TRÍ HẠM ĐỘI","5 tàu: 5, 4, 3, 2, 2 ô"),BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout(20,0));center.setOpaque(false);
        BoardView pv=new BoardView();pv.setName("placementBoard");center.add(pv);
        RoundedPanel tools=card("ĐẶT TÀU");JPanel b=new JPanel(new GridLayout(0,1,10,10));b.setOpaque(false);
        JButton random=Components.button("ĐẶT NGẪU NHIÊN",Theme.GOLD), ready=Components.button("READY",Theme.CYAN);
        random.addActionListener(e->randomFleet(pv));ready.addActionListener(e->{if(fleet.size()!=5){error("Hãy đặt đủ 5 tàu.");return;}net.send("READY",FleetCodec.encode(fleet));show("waiting");});
        b.add(random);b.add(ready);tools.add(b);tools.setPreferredSize(new Dimension(330,300));center.add(tools,BorderLayout.EAST);p.add(center);return p;
    }

    private JPanel battle(){
        JPanel p=page();p.add(header("TRẬN ĐẤU","Bắn theo lượt • Hit/Sunk giữ lượt • Miss đổi lượt"),BorderLayout.NORTH);
        JPanel g=new JPanel(new GridLayout(1,2,20,20));g.setOpaque(false);
        RoundedPanel a=card("HẠM ĐỘI CỦA BẠN");a.add(ownBoard);
        RoundedPanel b=card("VÙNG BIỂN ĐỐI THỦ");b.add(enemyBoard);b.add(battleStatus,BorderLayout.SOUTH);
        enemyBoard.onClick((r,c)->net.send("ATTACK",String.valueOf(r),String.valueOf(c)));
        g.add(a);g.add(b);p.add(g);return p;
    }

    private JPanel result(){
        JPanel p=page();RoundedPanel c=card("KẾT QUẢ TRẬN");JLabel r=Components.label("Trận đã kết thúc",34,Theme.GREEN,true);r.setName("resultLabel");c.add(r);
        JButton b=Components.button("VỀ SẢNH",Theme.CYAN);b.addActionListener(e->{net.send("ONLINE");show("lobby");});c.add(b,BorderLayout.SOUTH);p.add(c);return p;
    }

    private JPanel ranking(){
        JPanel p=page();p.add(header("BẢNG XẾP HẠNG","Dữ liệu lấy trực tiếp từ SQL Server"),BorderLayout.NORTH);
        JList<String> l=new JList<>(rankingModel);l.setBackground(Theme.NAV);l.setForeground(Theme.TEXT);l.setFont(Theme.font(16,false));p.add(new JScrollPane(l));
        JButton b=Components.button("VỀ SẢNH",Theme.PANEL2);b.addActionListener(e->show("lobby"));p.add(b,BorderLayout.SOUTH);return p;
    }

    private JPanel history(){
        JPanel p=page();p.add(header("LỊCH SỬ TRẬN ĐẤU","20 trận gần nhất"),BorderLayout.NORTH);
        JList<String> l=new JList<>(historyModel);l.setBackground(Theme.NAV);l.setForeground(Theme.TEXT);l.setFont(Theme.font(16,false));p.add(new JScrollPane(l));
        JButton b=Components.button("VỀ SẢNH",Theme.PANEL2);b.addActionListener(e->show("lobby"));p.add(b,BorderLayout.SOUTH);return p;
    }

    private JPanel settings(){
        JPanel p=page();p.add(header("CÀI ĐẶT","Thông tin Client"),BorderLayout.NORTH);
        RoundedPanel c=card("THÔNG TIN");c.add(Components.label("Java Swing • TCP Socket • SQL Server",18,Theme.CYAN,true));
        JButton b=Components.button("VỀ SẢNH",Theme.PANEL2);b.addActionListener(e->show("lobby"));c.add(b,BorderLayout.SOUTH);p.add(c);return p;
    }

    private void randomFleet(BoardView view){
        fleet.clear();view.clear(true);
        int[] lens={5,4,3,2,2};String[] names={"Carrier","Battleship","Cruiser","Destroyer","Patrol"};
        boolean[][] used=new boolean[10][10];
        for(int i=0;i<lens.length;i++){
            int len=lens[i];
            for(int tries=0;tries<10000;tries++){
                boolean h=ThreadLocalRandom.current().nextBoolean();
                int r=ThreadLocalRandom.current().nextInt(10),c=ThreadLocalRandom.current().nextInt(10);
                boolean ok=true;
                for(int k=0;k<len;k++){int rr=r+(h?0:k),cc=c+(h?k:0);if(rr>=10||cc>=10||used[rr][cc]){ok=false;break;}}
                if(!ok)continue;
                fleet.add(new FleetCodec.Placement(names[i],len,r,c,h));
                for(int k=0;k<len;k++){int rr=r+(h?0:k),cc=c+(h?k:0);used[rr][cc]=true;view.ship(rr,cc);}
                break;
            }
        }
        ownBoard.clear(true);
        for(var f:fleet)for(int k=0;k<f.len();k++)ownBoard.ship(f.row()+(f.horizontal()?0:k),f.col()+(f.horizontal()?k:0));
    }

    private void message(Protocol.Message m){
        SwingUtilities.invokeLater(()->{
            try{
                switch(m.type()){
                    case "LOGIN_OK" -> {username=m.get(0);net.send("ONLINE");show("lobby");}
                    case "LOGIN_FAIL","REGISTER_FAIL","ERROR" -> error(m.get(0));
                    case "REGISTER_OK" -> {info("Đăng ký thành công. Hãy đăng nhập.");show("login");}
                    case "ONLINE_LIST" -> {onlineModel.clear();if(!m.get(0).isBlank())for(String n:m.get(0).split(","))if(!n.equals(username))onlineModel.addElement(n);}
                    case "INVITE" -> {pendingInvite=m.get(0);updateInviteLabel("Lời mời từ "+pendingInvite);show("friends");}
                    case "INVITE_REJECTED" -> info(m.get(0)+" đã từ chối lời mời.");
                    case "MATCH_CREATED" -> {matchId=Long.parseLong(m.get(0));opponent=m.get(1);roomStatus.setText("Match #"+matchId+" • Đối thủ: "+opponent);show("waiting");}
                    case "READY_STATUS" -> roomStatus.setText(m.get(0)+": "+m.get(1)+"   |   "+m.get(2)+": "+m.get(3));
                    case "GAME_START" -> {enemyBoard.clear(false);battleStatus.setText("Lượt hiện tại: "+m.get(0));show("battle");}
                    case "TURN" -> battleStatus.setText("Lượt hiện tại: "+m.get(0));
                    case "ATTACK_RESULT" -> {
                        String shooter=m.get(0);int r=Integer.parseInt(m.get(1)),c=Integer.parseInt(m.get(2));String res=m.get(3);
                        boolean hit=!res.equals("MISS");
                        if(shooter.equals(username))enemyBoard.shot(r,c,hit);else ownBoard.shot(r,c,hit);
                    }
                    case "GAME_OVER" -> {updateResult(m.get(0).equals("WIN")?"★ CHIẾN THẮNG ★":"THẤT BẠI",m.get(0).equals("WIN")?Theme.GREEN:Theme.RED);show("result");}
                    case "OPPONENT_LEFT" -> {info("Đối thủ đã thoát.");show("lobby");}
                    case "RANKING_BEGIN" -> rankingModel.clear();
                    case "RANKING_ROW" -> rankingModel.addElement(String.format("%-18s  W:%s  L:%s  Rating:%s  WinRate:%s%%",m.get(0),m.get(1),m.get(2),m.get(3),m.get(4)));
                    case "HISTORY_BEGIN" -> historyModel.clear();
                    case "HISTORY_ROW" -> historyModel.addElement("#"+m.get(0)+"  vs "+m.get(1)+"  "+m.get(2)+"  "+m.get(3));
                    case "INFO" -> info(m.get(0));
                }
            }catch(Exception e){error(e.getMessage());}
        });
    }

    private void updateInviteLabel(String text){
        for(Component c:((JPanel)root.getComponent(4)).getComponents()) if(c instanceof RoundedPanel rp)
            for(Component x:rp.getComponents()) if(x instanceof JLabel l && "inviteLabel".equals(l.getName())) l.setText(text);
    }
    private void updateResult(String text,Color color){
        for(Component c:((JPanel)root.getComponent(8)).getComponents()) if(c instanceof RoundedPanel rp)
            for(Component x:rp.getComponents()) if(x instanceof JLabel l && "resultLabel".equals(l.getName())){l.setText(text);l.setForeground(color);}
    }
    private void error(String s){JOptionPane.showMessageDialog(this,s,"Lỗi",JOptionPane.ERROR_MESSAGE);}
    private void info(String s){JOptionPane.showMessageDialog(this,s,"Thông báo",JOptionPane.INFORMATION_MESSAGE);}
}
