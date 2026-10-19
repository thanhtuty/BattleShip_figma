package battleship.client;

import battleship.game.FleetCodec;
import battleship.network.Protocol;
import battleship.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientApp extends JFrame {
    private static final int W=1200,H=760;

    private final GameClient net=new GameClient();
    private final CardLayout cards=new CardLayout();
    private final JPanel root=new JPanel(cards);

    private String username="";
    private String opponent="";
    private String pendingInvite="";
    private long matchId=-1;
    private final List<String> onlineUsers=new ArrayList<>();
    private final List<FleetCodec.Placement> fleet=new ArrayList<>();

    private JLabel globalOnlineLabel;
    private JLabel lobbyUserLabel;
    private JLabel lobbyWinRateLabel;
    private final JLabel[] statValue=new JLabel[4];
    private JLabel onlineCountLabel;
    private JLabel inviteLabel;
    private JLabel roomPlayer1;
    private JLabel roomPlayer2;
    private JLabel roomState1;
    private JLabel roomState2;
    private JLabel battleStatus;
    private JLabel clockLabel;
    private JLabel missedCountLabel;
    private int myMissedTurns=0;
    private int enemyMissedTurns=0;
    private JComboBox<String> playerSelect;
    private JComboBox<String> shipSelect;
    private boolean horizontal=true;
    private JLabel myShipsLabel;
    private JLabel enemyShipsLabel;
    private JLabel eventBanner;
    private JTextArea battleLog;
    private JLabel resultTitle;
    private JLabel resultPlayers;
    private JTextField challengeField;

    private final JLabel[] rankPos=new JLabel[5];
    private final JLabel[] rankName=new JLabel[5];
    private final JLabel[] rankScore=new JLabel[5];
    private final JLabel[] rankRate=new JLabel[5];
    private int rankCursor=0;

    private final JLabel[] onlineNameLabel=new JLabel[3];
    private final JLabel[] historyTitle=new JLabel[3];
    private final JLabel[] historyMeta=new JLabel[3];
    private int historyCursor=0;

    private final BoardView placementBoard=new BoardView(39);
    private final BoardView ownBoard=new BoardView(32);
    private final BoardView enemyBoard=new BoardView(32);

    public static void main(String[] args){
        SwingUtilities.invokeLater(()->new ClientApp().setVisible(true));
    }

    public ClientApp(){
        setTitle("Battleship – Hải Chiến");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        root.setPreferredSize(new Dimension(W,H));
        root.setBackground(Theme.BG);
        setContentPane(root);
        buildScreens();
        pack();
        setLocationRelativeTo(null);

        net.addListener(this::message);
        placementBoard.onClick(this::placeShip);
        enemyBoard.onClick((r,c)->{
            if(net.isConnected()){SoundFX.play("FIRE");net.send("ATTACK",String.valueOf(r),String.valueOf(c));}
        });

        addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent e){
                try{net.close();}catch(Exception ignored){}
            }
        });
        showScreen("welcome");
    }

    private JPanel baseScreen(String screenSubtitle){
        JPanel p=new JPanel(null);
        p.setBackground(Theme.BG);
        p.setPreferredSize(new Dimension(W,H));

        JPanel accent=new JPanel();
        accent.setBackground(Theme.CYAN);
        accent.setBounds(0,0,W,7);
        p.add(accent);

        place(p,Components.label("⚓  BATTLESHIP  /  HẢI CHIẾN",21,Theme.CYAN,true),35,22,430,30);

        JLabel sub=Components.label(screenSubtitle,12,Theme.MUTED,false);
        place(p,sub,36,50,520,20);

        JLabel right=Components.label(username.isBlank()?"DESKTOP CLIENT":"●  "+username.toUpperCase()+"   •   ONLINE",12,
                username.isBlank()?Theme.MUTED:Theme.GREEN,true);
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        place(p,right,850,28,315,22);
        right.putClientProperty("globalOnline",Boolean.TRUE);

        JPanel line=new JPanel();
        line.setBackground(Theme.STROKE);
        line.setBounds(30,73,1140,1);
        p.add(line);
        return p;
    }

    private void refreshGlobalLabels(){
        for(Component screen:root.getComponents()){
            if(screen instanceof JPanel panel){
                for(Component c:panel.getComponents()){
                    if(c instanceof JLabel l && Boolean.TRUE.equals(l.getClientProperty("globalOnline"))){
                        l.setText(username.isBlank()?"DESKTOP CLIENT":"●  "+username.toUpperCase()+"   •   ONLINE");
                        l.setForeground(username.isBlank()?Theme.MUTED:Theme.GREEN);
                    }
                }
            }
        }
    }

    private void place(JPanel p,JComponent c,int x,int y,int w,int h){
        c.setBounds(x,y,w,h);
        p.add(c);
    }

    private RoundedPanel panel(JPanel p,int x,int y,int w,int h){
        RoundedPanel rp=new RoundedPanel(null,Theme.PANEL);
        rp.setBounds(x,y,w,h);
        p.add(rp);
        return rp;
    }

    private JLabel text(JPanel p,String s,int x,int y,int w,int h,int size,Color c,boolean bold){
        JLabel l=Components.label(s,size,c,bold);
        place(p,l,x,y,w,h);
        return l;
    }

    private JButton button(JPanel p,String s,int x,int y,int w,int h,Color c){
        JButton b=Components.button(s,c);
        place(p,b,x,y,w,h);
        return b;
    }

    private void buildScreens(){
        root.add(welcome(),"welcome");
        root.add(login(),"login");
        root.add(register(),"register");
        root.add(lobby(),"lobby");
        root.add(match(),"match");
        root.add(friends(),"friends");
        root.add(waiting(),"waiting");
        root.add(placement(),"placement");
        root.add(battle(),"battle");
        root.add(result(),"result");
        root.add(ranking(),"ranking");
        root.add(history(),"history");
    }

    private void showScreen(String name){ cards.show(root,name); }

    private JPanel welcome(){
        JPanel p=baseScreen("Màn hình giới thiệu & bắt đầu");
        RoundedPanel hero=panel(p,74,133,1052,535);

        text(hero,"BATTLESHIP",51,78,550,82,64,Theme.TEXT,true);
        text(hero,"THỐNG TRỊ ĐẠI DƯƠNG",54,174,430,38,24,Theme.CYAN,true);
        text(hero,"Lập chiến thuật • Điều khiển hạm đội • Chiến thắng đối thủ",54,228,600,30,16,Theme.MUTED,false);

        JButton start=button(hero,"BẮT ĐẦU CHƠI   →",54,313,240,55,Theme.CYAN);
        start.addActionListener(e->showScreen("login"));
        JButton guide=button(hero,"HƯỚNG DẪN",312,313,175,55,Theme.PANEL2);
        guide.addActionListener(e->info("Luồng chơi: Đăng nhập → Tìm/Thách đấu → Đặt tàu → Ready → Bắn theo lượt."));

        text(hero,"MULTIPLAYER  •  JAVA CLIENT / SERVER  •  SQL SERVER",55,462,560,24,12,Theme.MUTED,true);

        BoardView preview=new BoardView(26);
        preview.setInteractive(false);
        preview.setBounds(690,74,preview.boardPixelSize(),preview.boardPixelSize());
        hero.add(preview);
        int[][] demo={{0,1,5,1},{5,3,4,0},{1,6,3,1},{8,7,2,0},{4,8,2,1}};
        for(int[] s:demo)for(int k=0;k<s[2];k++)preview.ship(s[0]+(s[3]==1?0:k),s[1]+(s[3]==1?k:0));
        return p;
    }

    private JPanel login(){
        JPanel p=baseScreen("Xác thực người chơi");
        RoundedPanel form=panel(p,307,127,586,550);
        text(form,"ĐĂNG NHẬP TÀI KHOẢN",34,32,500,35,23,Theme.TEXT,true);
        text(form,"Chào mừng trở lại, thuyền trưởng!",34,78,500,28,16,Theme.MUTED,false);

        text(form,"Tên đăng nhập",34,139,200,24,14,Theme.TEXT,true);
        JTextField user=Components.field();place(form,user,34,170,518,53);

        text(form,"Mật khẩu",34,249,200,24,14,Theme.TEXT,true);
        JPasswordField pass=Components.password();place(form,pass,34,280,518,53);

        JButton login=button(form,"ĐĂNG NHẬP",34,369,518,52,Theme.CYAN);
        JButton register=button(form,"ĐĂNG KÝ TÀI KHOẢN",34,437,518,50,Theme.PANEL2);
        text(form,"Server mặc định: 127.0.0.1:5050",34,505,260,22,12,Theme.MUTED,false);
        JLabel forgot=text(form,"Quên mật khẩu?",390,505,160,22,12,Theme.CYAN,false);
        forgot.setHorizontalAlignment(SwingConstants.RIGHT);

        login.addActionListener(e->{
            String u=user.getText().trim();
            if(u.isBlank()){error("Vui lòng nhập tên đăng nhập.");return;}
            try{
                ensureConnected();
                net.send("LOGIN",u,new String(pass.getPassword()));
            }catch(Exception ex){error(ex.getMessage());}
        });
        register.addActionListener(e->{
            try{ensureConnected();showScreen("register");}catch(Exception ex){error(ex.getMessage());}
        });
        return p;
    }

    private JPanel register(){
        JPanel p=baseScreen("Tạo hồ sơ người chơi");
        RoundedPanel form=panel(p,307,111,586,606);
        text(form,"TẠO TÀI KHOẢN MỚI",34,27,500,35,23,Theme.TEXT,true);

        JTextField user=Components.field();
        JTextField email=Components.field();
        JPasswordField pass=Components.password();
        JPasswordField confirm=Components.password();
        JComponent[] fields={user,email,pass,confirm};
        String[] labels={"Tên người chơi","Email","Mật khẩu","Xác nhận mật khẩu"};
        for(int i=0;i<4;i++){
            int y=83+i*103;
            text(form,labels[i],34,y,250,22,14,Theme.TEXT,true);
            place(form,fields[i],34,y+26,518,52);
        }

        JButton create=button(form,"TẠO TÀI KHOẢN",34,508,518,51,Theme.CYAN);
        JButton back=button(form,"QUAY LẠI ĐĂNG NHẬP",34,565,518,32,Theme.PANEL2);
        create.addActionListener(e->{
            String u=user.getText().trim(), em=email.getText().trim();
            String pw=new String(pass.getPassword()), cf=new String(confirm.getPassword());
            if(u.isBlank()||pw.isBlank()){error("Tên người chơi và mật khẩu không được để trống.");return;}
            if(!pw.equals(cf)){error("Mật khẩu xác nhận không khớp.");return;}
            try{ensureConnected();net.send("REGISTER",u,em,pw);}catch(Exception ex){error(ex.getMessage());}
        });
        back.addActionListener(e->showScreen("login"));
        return p;
    }

    private void sidebar(JPanel p,String selected){
        JPanel side=new JPanel(null);
        side.setBackground(Theme.NAV);
        side.setBounds(0,75,217,685);
        p.add(side);

        String[] labels={"Sảnh game","Thách đấu","Bạn bè","Lịch sử","Bảng xếp hạng","Cài đặt"};
        String[] icons={"◈","▣","✦","◷","★","⚙"};
        String[] targets={"lobby","match","friends","history","ranking","history"};
        for(int i=0;i<labels.length;i++){
            JButton b=Components.button(icons[i]+"   "+labels[i],labels[i].equals(selected)?Theme.PANEL2:Theme.NAV);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setForeground(labels[i].equals(selected)?Theme.CYAN:Theme.MUTED);
            b.setBounds(12,42+i*74,193,48);
            final int idx=i;
            b.addActionListener(e->{
                if(idx==3)net.send("HISTORY");
                if(idx==0)net.send("PROFILE");
                if(idx==4)net.send("RANKING");
                showScreen(targets[idx]);
            });
            side.add(b);
        }
        text(side,"●   Đang kết nối",28,635,160,24,12,Theme.GREEN,true);
    }

    private JPanel lobby(){
        JPanel p=baseScreen("Trang chính sau đăng nhập");
        sidebar(p,"Sảnh game");
        text(p,"SẢNH HẢI CHIẾN",251,112,430,42,31,Theme.TEXT,true);
        text(p,"Xin chào, thuyền trưởng! Sẵn sàng ra khơi?",253,158,500,26,15,Theme.MUTED,false);

        RoundedPanel mode=panel(p,251,224,573,215);
        text(mode,"TRẬN ĐẤU TRỰC TUYẾN",22,20,400,28,18,Theme.TEXT,true);
        text(mode,"Đối đầu 1 vs 1 trên đại dương",22,72,420,32,21,Theme.CYAN,true);
        text(mode,"Tìm đối thủ hoặc mời bạn bè vào phòng chơi",22,118,430,24,14,Theme.MUTED,false);
        JButton find=button(mode,"TÌM TRẬN NGAY   →",22,154,235,46,Theme.CYAN);
        find.addActionListener(e->showScreen("match"));

        RoundedPanel profile=panel(p,847,224,321,215);
        text(profile,"HỒ SƠ THUYỀN TRƯỞNG",22,20,280,27,17,Theme.TEXT,true);
        lobbyUserLabel=text(profile,"Captain_01",22,73,270,32,22,Theme.TEXT,true);
        text(profile,"Thành tích cá nhân",22,118,260,24,14,Theme.MUTED,false);
        lobbyWinRateLabel=text(profile,"Đang tải...",22,159,280,24,15,Theme.GREEN,true);

        RoundedPanel stats=panel(p,251,466,917,232);
        text(stats,"THỐNG KÊ MÙA GIẢI",22,18,300,28,18,Theme.TEXT,true);
        String[][] st={{"—","TRẬN ĐẤU"},{"—","CHIẾN THẮNG"},{"—","THẤT BẠI"},{"—","ĐIỂM RANK"}};
        for(int i=0;i<4;i++){
            RoundedPanel c=new RoundedPanel(null,Theme.PANEL2,null,14);
            c.setBounds(22+i*220,67,202,116);stats.add(c);
            statValue[i]=text(c,st[i][0],18,13,160,40,31,Theme.CYAN,true);
            text(c,st[i][1],18,68,160,22,11,Theme.MUTED,true);
        }
        return p;
    }

    private JPanel match(){
        JPanel p=baseScreen("Ghép trận và gửi lời mời");
        sidebar(p,"Thách đấu");
        text(p,"TÌM ĐỐI THỦ",251,111,430,42,31,Theme.TEXT,true);
        text(p,"Chọn phương thức bắt đầu trận đấu",253,158,500,26,15,Theme.MUTED,false);

        RoundedPanel quick=panel(p,251,215,537,230);
        text(quick,"GHÉP TRẬN NGẪU NHIÊN",22,20,400,28,18,Theme.TEXT,true);
        text(quick,"Bàn chơi 10 × 10 • 2 người",22,75,300,26,16,Theme.MUTED,false);
        JButton quickBtn=button(quick,"BẮT ĐẦU TÌM",22,137,206,50,Theme.CYAN);
        JButton refresh=button(quick,"LÀM MỚI",242,137,150,50,Theme.PANEL2);
        quickBtn.addActionListener(e->quickChallenge());
        refresh.addActionListener(e->net.send("ONLINE"));

        RoundedPanel status=panel(p,809,215,359,230);
        text(status,"TRẠNG THÁI MẠNG",22,20,280,28,18,Theme.TEXT,true);
        text(status,"●  Server đang hoạt động",22,75,290,28,17,Theme.GREEN,true);
        onlineCountLabel=text(status,"Người chơi online: 0",22,119,280,26,15,Theme.MUTED,false);

        RoundedPanel direct=panel(p,251,475,917,217);
        text(direct,"THÁCH ĐẤU NGƯỜI CHƠI",22,20,360,28,18,Theme.TEXT,true);
        playerSelect=new JComboBox<>();
        playerSelect.setBackground(Theme.NAV);
        playerSelect.setForeground(Theme.TEXT);
        challengeField=Components.field();
        place(direct,playerSelect,22,88,590,54);
        JButton send=button(direct,"GỬI LỜI MỜI",631,88,254,54,Theme.GOLD);
        send.addActionListener(e->{
            String target=playerSelect.getSelectedItem()==null?"":playerSelect.getSelectedItem().toString();
            if(target.isBlank()){error("Nhập username đối thủ.");return;}
            net.send("CHALLENGE",target);
        });
        return p;
    }

    private JPanel friends(){
        JPanel p=baseScreen("Danh sách lời mời thách đấu");
        sidebar(p,"Bạn bè");
        text(p,"BẠN BÈ & LỜI MỜI",251,111,500,42,30,Theme.TEXT,true);
        text(p,"Quản lý lời mời trận đấu trực tuyến",253,158,500,26,14,Theme.MUTED,false);

        text(p,"LỜI MỜI ĐẾN",251,208,165,32,12,Theme.TEXT,true);
        text(p,"BẠN BÈ / ONLINE",440,208,165,32,12,Theme.MUTED,true);

        RoundedPanel invite=panel(p,251,270,910,82);
        inviteLabel=text(invite,"Chưa có lời mời",22,13,500,28,18,Theme.TEXT,true);
        text(invite,"Lời mời thi đấu trực tuyến",22,45,350,22,12,Theme.MUTED,false);
        JButton accept=button(invite,"CHẤP NHẬN",674,18,145,46,Theme.GREEN);
        JButton reject=button(invite,"TỪ CHỐI",825,18,64,46,Theme.RED);
        reject.setFont(Theme.font(11,true));
        accept.addActionListener(e->{if(!pendingInvite.isBlank())net.send("ACCEPT",pendingInvite);});
        reject.addActionListener(e->{if(!pendingInvite.isBlank())net.send("REJECT",pendingInvite);pendingInvite="";inviteLabel.setText("Chưa có lời mời");});

        for(int i=0;i<3;i++){
            int y=380+i*92;
            RoundedPanel row=panel(p,251,y,910,76);
            onlineNameLabel[i]=text(row,"—",22,13,280,27,17,Theme.TEXT,true);
            text(row,"Danh sách người chơi online",22,44,300,20,12,Theme.MUTED,false);
            JButton c=button(row,"ĐẾN THÁCH ĐẤU",710,15,178,46,Theme.PANEL2);
            c.addActionListener(e->showScreen("match"));
        }
        return p;
    }

    private JPanel waiting(){
        JPanel p=baseScreen("Sẵn sàng trước khi vào game");
        text(p,"PHÒNG CHỜ • MATCH",64,112,500,42,31,Theme.TEXT,true);
        text(p,"Hai người chơi xác nhận trước khi bắt đầu",66,158,520,24,14,Theme.MUTED,false);
        text(p,"CHỜ ĐỦ 2 NGƯỜI SẴN SÀNG",892,123,260,30,12,Theme.TEXT,true);

        RoundedPanel a=panel(p,70,240,501,320);
        text(a,"THUYỀN TRƯỞNG 01",22,19,300,28,18,Theme.TEXT,true);
        roomPlayer1=text(a,"Captain_01",36,88,400,40,28,Theme.CYAN,true);
        roomState1=text(a,"● Chưa sẵn sàng",36,154,350,28,16,Theme.GOLD,false);
        JButton arrange=button(a,"BỐ TRÍ HẠM ĐỘI",36,218,303,54,Theme.CYAN);
        arrange.addActionListener(e->showScreen("placement"));

        RoundedPanel b=panel(p,629,240,501,320);
        text(b,"THUYỀN TRƯỞNG 02",22,19,300,28,18,Theme.TEXT,true);
        roomPlayer2=text(b,"Đang chờ đối thủ",36,88,400,40,28,Theme.GOLD,true);
        roomState2=text(b,"● Chưa sẵn sàng",36,154,350,28,16,Theme.GOLD,false);
        button(b,"ĐANG CHỜ...",36,218,303,54,Theme.PANEL2).setEnabled(false);

        button(p,"RỜI PHÒNG",516,664,168,48,Theme.RED).addActionListener(e->leaveMatch());
        return p;
    }

    private JPanel placement(){
        JPanel p=baseScreen("Thiết lập vị trí tàu trước trận");
        text(p,"BỐ TRÍ HẠM ĐỘI",54,110,430,42,31,Theme.TEXT,true);
        text(p,"Sắp xếp 5 chiến hạm trước giờ khai chiến",55,158,520,26,14,Theme.MUTED,false);

        placementBoard.setBounds(96,227,placementBoard.boardPixelSize(),placementBoard.boardPixelSize());
        p.add(placementBoard);

        RoundedPanel tools=panel(p,599,215,534,410);
        text(tools,"CHỌN TÀU ĐỂ ĐẶT",20,18,350,28,18,Theme.TEXT,true);
        String[][] ships={{"Tàu sân bay","5 ô"},{"Thiết giáp hạm","4 ô"},{"Tuần dương hạm","3 ô"},{"Tàu khu trục","2 ô"},{"Tàu tuần tra","2 ô"}};
        for(int i=0;i<5;i++){
            RoundedPanel row=new RoundedPanel(null,Theme.PANEL2,null,12);
            row.setBounds(20,68+i*62,493,49);tools.add(row);
            text(row,ships[i][0],17,13,300,24,14,Theme.TEXT,true);
            text(row,ships[i][1],410,13,60,24,13,Theme.CYAN,true);
        }

        shipSelect=new JComboBox<>(new String[]{"Carrier","Battleship","Cruiser","Destroyer","Patrol"});
        shipSelect.setBackground(Theme.NAV);shipSelect.setForeground(Theme.TEXT);
        place(p,shipSelect,589,650,185,48);
        JButton rotate=button(p,"XOAY: NGANG",599,578,174,42,Theme.PANEL2);
        rotate.addActionListener(e->{horizontal=!horizontal;rotate.setText(horizontal?"XOAY: NGANG":"XOAY: DỌC");});
        button(p,"XẾP LẠI",790,578,138,42,Theme.RED).addActionListener(e->clearFleet());
        JButton random=button(p,"ĐẶT NGẪU NHIÊN",788,656,190,50,Theme.GOLD);
        JButton ready=button(p,"SẴN SÀNG",996,656,166,50,Theme.CYAN);
        random.addActionListener(e->randomFleet());
        ready.addActionListener(e->{
            if(fleet.size()!=5){error("Hãy đặt đủ 5 tàu trước khi Ready.");return;}
            net.send("READY",FleetCodec.encode(fleet));
            showScreen("waiting");
        });
        return p;
    }

    private JPanel battle(){
        JPanel p=baseScreen("Chiến trường 1v1 theo lượt");
        text(p,"TRẬN ĐẤU",48,109,430,42,30,Theme.TEXT,true);
        text(p,"Chọn ô trên vùng biển đối thủ để khai hỏa",50,157,550,25,14,Theme.MUTED,false);
        battleStatus=text(p,"ĐANG CHỜ LƯỢT",958,121,196,30,12,Theme.TEXT,true);
        clockLabel=text(p,"00:15",830,121,120,30,20,Theme.GOLD,true);

        text(p,"HẠM ĐỘI CỦA BẠN",80,211,300,28,17,Theme.CYAN,true);
        text(p,"VÙNG BIỂN ĐỐI THỦ",746,211,300,28,17,Theme.GOLD,true);

        ownBoard.setBounds(67,247,ownBoard.boardPixelSize(),ownBoard.boardPixelSize());
        enemyBoard.setBounds(727,247,enemyBoard.boardPixelSize(),enemyBoard.boardPixelSize());
        p.add(ownBoard);p.add(enemyBoard);

        RoundedPanel mid=panel(p,426,240,302,413);
        text(mid,"CẬP NHẬT TRẬN ĐẤU",14,14,276,30,16,Theme.CYAN,true);
        JLabel matchLabel=text(mid,"Match ID: cập nhật khi ghép trận",14,47,274,21,11,Theme.MUTED,false);
        matchLabel.setName("matchInfoLabel");
        myShipsLabel=text(mid,"BẠN CÒN: 5/5 TÀU",14,81,274,25,14,Theme.GREEN,true);
        enemyShipsLabel=text(mid,"ĐỐI THỦ CÒN: 5/5 TÀU",14,111,274,25,14,Theme.GOLD,true);
        eventBanner=text(mid,"Đang chờ khai hỏa...",14,148,274,39,12,Theme.CYAN,true);
        text(mid,"NHẬT KÝ PHÁT BẮN",14,193,274,23,13,Theme.TEXT,true);
        battleLog=new JTextArea();
        battleLog.setEditable(false);
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        battleLog.setBackground(Theme.NAV);
        battleLog.setForeground(Theme.TEXT);
        battleLog.setCaretColor(Theme.CYAN);
        battleLog.setFont(Theme.font(12,false));
        battleLog.setBorder(BorderFactory.createEmptyBorder(8,9,8,9));
        JScrollPane scroll=new JScrollPane(battleLog);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.STROKE));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setBounds(14,222,274,176);
        mid.add(scroll);

        button(p,"RỜI TRẬN",518,667,160,46,Theme.RED).addActionListener(e->leaveMatch());
        return p;
    }

    private JPanel result(){
        JPanel p=baseScreen("Thắng / thua và thống kê sau trận");
        RoundedPanel c=panel(p,225,126,750,567);
        resultTitle=text(c,"★  CHIẾN THẮNG  ★",135,69,500,56,37,Theme.GREEN,true);
        resultTitle.setHorizontalAlignment(SwingConstants.CENTER);
        text(c,"Trận hải chiến đã kết thúc!",160,143,430,28,16,Theme.MUTED,false).setHorizontalAlignment(SwingConstants.CENTER);

        JPanel line=new JPanel();line.setBackground(Theme.STROKE);line.setBounds(47,211,656,1);c.add(line);
        resultPlayers=text(c,"CAPTAIN_01      VS      CAPTAIN_02",75,252,600,45,23,Theme.TEXT,true);
        resultPlayers.setHorizontalAlignment(SwingConstants.CENTER);
        text(c,"Kết quả được lưu vào SQL Server và cập nhật bảng xếp hạng.",90,354,570,28,14,Theme.MUTED,false).setHorizontalAlignment(SwingConstants.CENTER);

        JButton again=button(c,"CHƠI LẠI",105,467,250,54,Theme.CYAN);
        JButton lobby=button(c,"VỀ SẢNH",374,467,250,54,Theme.PANEL2);
        again.addActionListener(e->showScreen("match"));
        lobby.addActionListener(e->{net.send("ONLINE");showScreen("lobby");});
        return p;
    }

    private JPanel ranking(){
        JPanel p=baseScreen("Danh hiệu & thứ hạng người chơi");
        sidebar(p,"Bảng xếp hạng");
        text(p,"BẢNG XẾP HẠNG",251,111,460,42,31,Theme.TEXT,true);
        text(p,"Huyền thoại đại dương • Mùa giải hiện tại",253,158,520,24,14,Theme.MUTED,false);
        text(p,"MÙA GIẢI 2026",980,120,170,30,12,Theme.TEXT,true);

        for(int i=0;i<5;i++){
            int y=224+i*91;
            RoundedPanel row=panel(p,251,y,910,76);
            rankPos[i]=text(row,String.format("%02d",i+1),24,20,55,32,20,Theme.CYAN,true);
            rankName[i]=text(row,"—",105,19,340,32,18,Theme.TEXT,true);
            rankScore[i]=text(row,"— điểm",600,21,190,28,16,Theme.GOLD,true);
            rankRate[i]=text(row,"—%",817,21,70,28,15,Theme.GREEN,true);
        }
        return p;
    }

    private JPanel history(){
        JPanel p=baseScreen("Trạng thái người chơi và tuỳ chỉnh");
        sidebar(p,"Lịch sử");
        text(p,"LỊCH SỬ TRẬN ĐẤU",251,110,480,42,30,Theme.TEXT,true);
        text(p,"Theo dõi thành tích và tùy chọn hệ thống",253,158,520,24,14,Theme.MUTED,false);

        for(int i=0;i<3;i++){
            int y=224+i*91;
            RoundedPanel row=panel(p,251,y,910,76);
            historyTitle[i]=text(row,"Chưa có dữ liệu",20,12,520,28,16,Theme.TEXT,true);
            historyMeta[i]=text(row,"—",20,43,520,20,12,Theme.MUTED,false);
            JLabel badge=text(row,"KẾT QUẢ",710,21,175,30,11,Theme.CYAN,true);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
        }

        RoundedPanel settings=panel(p,251,530,910,172);
        text(settings,"CÀI ĐẶT NHANH",22,17,300,28,17,Theme.TEXT,true);
        text(settings,"Âm thanh trò chơi",22,71,300,24,16,Theme.TEXT,false);
        JButton sound=button(settings,"ÂM THANH: BẬT",745,61,147,42,Theme.PANEL2);
        sound.addActionListener(e->{SoundFX.enabled(!SoundFX.enabled());sound.setText(SoundFX.enabled()?"ÂM THANH: BẬT":"ÂM THANH: TẮT");});
        text(settings,"Thông báo thách đấu",22,126,300,24,16,Theme.TEXT,false);
        text(settings,"CHƯA HỖ TRỢ",770,122,125,30,12,Theme.MUTED,true).setHorizontalAlignment(SwingConstants.CENTER);
        return p;
    }

    private void ensureConnected() throws Exception{
        if(net.isConnected())return;
        String host=System.getProperty("battleship.host",System.getenv().getOrDefault("BATTLESHIP_HOST","127.0.0.1"));
        int port=Integer.parseInt(System.getProperty("battleship.port",System.getenv().getOrDefault("BATTLESHIP_PORT","5050")));
        net.connect(host,port);
    }

    private void quickChallenge(){
        if(onlineUsers.isEmpty()){info("Hiện chưa có đối thủ online.");return;}
        net.send("CHALLENGE",onlineUsers.get(0));
    }

    private void leaveMatch(){
        if(matchId<0)return;
        int choice=JOptionPane.showConfirmDialog(this,"Rời trận sẽ bị xử thua sau khi bắt đầu. Bạn chắc chắn?","Xác nhận rời trận",JOptionPane.YES_NO_OPTION);
        if(choice==JOptionPane.YES_OPTION)net.send("LEAVE");
    }

    private void clearFleet(){
        fleet.clear();
        placementBoard.clear(true);
        ownBoard.clear(true);
    }

    private void placeShip(int row,int col){
        if(shipSelect==null||matchId<0)return;
        String name=(String)shipSelect.getSelectedItem();
        if(name==null||fleet.stream().anyMatch(f->f.name().equals(name)))return;
        int length=switch(name){case "Carrier"->5;case "Battleship"->4;case "Cruiser"->3;default->2;};
        battleship.game.Board test=new battleship.game.Board(10);
        for(var f:fleet)test.place(f.name(),f.len(),f.row(),f.col(),f.horizontal());
        if(!test.canPlace(row,col,length,horizontal)){error("Tàu vượt biên hoặc bị chồng lên tàu khác");return;}
        fleet.add(new FleetCodec.Placement(name,length,row,col,horizontal));
        for(int i=0;i<length;i++){
            int rr=row+(horizontal?0:i),cc=col+(horizontal?i:0);
            placementBoard.ship(rr,cc);ownBoard.ship(rr,cc);
        }
    }

    private void randomFleet(){
        clearFleet();
        int[] lens={5,4,3,2,2};
        String[] names={"Carrier","Battleship","Cruiser","Destroyer","Patrol"};
        boolean[][] used=new boolean[10][10];

        for(int i=0;i<lens.length;i++){
            int len=lens[i];
            for(int tries=0;tries<10000;tries++){
                boolean horizontal=ThreadLocalRandom.current().nextBoolean();
                int r=ThreadLocalRandom.current().nextInt(10);
                int c=ThreadLocalRandom.current().nextInt(10);
                boolean ok=true;
                for(int k=0;k<len;k++){
                    int rr=r+(horizontal?0:k),cc=c+(horizontal?k:0);
                    if(rr>=10||cc>=10||used[rr][cc]){ok=false;break;}
                }
                if(!ok)continue;
                fleet.add(new FleetCodec.Placement(names[i],len,r,c,horizontal));
                for(int k=0;k<len;k++){
                    int rr=r+(horizontal?0:k),cc=c+(horizontal?k:0);
                    used[rr][cc]=true;
                    placementBoard.ship(rr,cc);
                    ownBoard.ship(rr,cc);
                }
                break;
            }
        }
    }

    private void message(Protocol.Message m){
        SwingUtilities.invokeLater(()->{
            try{
                switch(m.type()){
                    case "LEFT_OK" -> {matchId=-1;clearFleet();showScreen("lobby");}
                    case "LOGOUT_OK" -> {username="";matchId=-1;pendingInvite="";onlineUsers.clear();refreshGlobalLabels();showScreen("login");}
                    case "LOGIN_OK" -> {
                        username=m.get(0);
                        if(lobbyUserLabel!=null)lobbyUserLabel.setText(username);
                        refreshGlobalLabels();
                        net.send("ONLINE");
                        net.send("PROFILE");
                        showScreen("lobby");
                    }
                    case "PROFILE_DATA" -> updateProfile(m);
                    case "DISCONNECTED" -> {
                        username="";
                        onlineUsers.clear();
                        refreshGlobalLabels();
                        showScreen("login");
                        error(m.get(0));
                    }
                    case "LOGIN_FAIL","REGISTER_FAIL","ERROR" -> error(m.get(0));
                    case "REGISTER_OK" -> {
                        info("Đăng ký thành công. Hãy đăng nhập.");
                        showScreen("login");
                    }
                    case "ONLINE_LIST" -> {
                        onlineUsers.clear();
                        if(!m.get(0).isBlank()){
                            for(String n:m.get(0).split(",")) if(!n.equals(username)) onlineUsers.add(n);
                        }
                        if(onlineCountLabel!=null)onlineCountLabel.setText("Người chơi online: "+onlineUsers.size());
                        if(playerSelect!=null){playerSelect.removeAllItems();for(String name:onlineUsers)playerSelect.addItem(name);}
                        for(int i=0;i<onlineNameLabel.length;i++)if(onlineNameLabel[i]!=null)onlineNameLabel[i].setText(i<onlineUsers.size()?onlineUsers.get(i):"—");
                        if(challengeField!=null && challengeField.getText().isBlank() && !onlineUsers.isEmpty())
                            challengeField.setText(onlineUsers.get(0));
                    }
                    case "INVITE" -> {
                        SoundFX.play("INVITE");
                        pendingInvite=m.get(0);
                        if(inviteLabel!=null)inviteLabel.setText(pendingInvite);
                        showScreen("friends");
                    }
                    case "INVITE_REJECTED" -> info(m.get(0)+" đã từ chối lời mời.");
                    case "MATCH_CREATED" -> {
                        matchId=Long.parseLong(m.get(0));
                        myMissedTurns=0;enemyMissedTurns=0;
                        clearFleet();
                        opponent=m.get(1);
                        if(roomPlayer1!=null)roomPlayer1.setText(username);
                        if(roomPlayer2!=null)roomPlayer2.setText(opponent);
                        if(roomState1!=null)roomState1.setText("● Chưa sẵn sàng");
                        if(roomState2!=null)roomState2.setText("● Chưa sẵn sàng");
                        showScreen("waiting");
                    }
                    case "READY_STATUS" -> {
                        String n1=m.get(0),r1=m.get(1),n2=m.get(2),r2=m.get(3);
                        if(roomPlayer1!=null)roomPlayer1.setText(n1);
                        if(roomPlayer2!=null)roomPlayer2.setText(n2);
                        if(roomState1!=null){roomState1.setText("● "+(Boolean.parseBoolean(r1)?"Sẵn sàng":"Chưa sẵn sàng"));roomState1.setForeground(Boolean.parseBoolean(r1)?Theme.GREEN:Theme.GOLD);}
                        if(roomState2!=null){roomState2.setText("● "+(Boolean.parseBoolean(r2)?"Sẵn sàng":"Chưa sẵn sàng"));roomState2.setForeground(Boolean.parseBoolean(r2)?Theme.GREEN:Theme.GOLD);}
                    }
                    case "GAME_START" -> {
                        enemyBoard.clear(false);
                        if(battleLog!=null)battleLog.setText("");
                        if(myShipsLabel!=null)myShipsLabel.setText("BẠN CÒN: 5/5 TÀU");
                        if(enemyShipsLabel!=null)enemyShipsLabel.setText("ĐỐI THỦ CÒN: 5/5 TÀU");
                        if(eventBanner!=null)eventBanner.setText("Trận đấu bắt đầu!");
                        appendBattleLog("Bắt đầu trận #"+matchId+" • lượt đầu: "+m.get(0));
                        enemyBoard.setInteractive(m.get(0).equals(username));
                        if(battleStatus!=null)battleStatus.setText(m.get(0).equals(username)?"LƯỢT CỦA BẠN":"LƯỢT ĐỐI THỦ");
                        showScreen("battle");
                    }
                    case "TIMER" -> {if(clockLabel!=null)clockLabel.setText(String.format("00:%02d",Integer.parseInt(m.get(0))));}
                    case "TURN_TIMEOUT" -> {
                        appendBattleLog(m.get(0)+" bỏ lượt lần "+m.get(1)+"/4");
                        if(m.get(0).equals(username))myMissedTurns=Integer.parseInt(m.get(1));else enemyMissedTurns=Integer.parseInt(m.get(1));
                        if(missedCountLabel!=null)missedCountLabel.setText("Bỏ lượt: Bạn "+myMissedTurns+"/4 • Đối thủ "+enemyMissedTurns+"/4");
                        if(eventBanner!=null)eventBanner.setText("Hết giờ: "+m.get(0)+" ("+m.get(1)+"/4)");
                    }
                    case "TURN" -> {
                        boolean mine=m.get(0).equals(username);
                        if(battleStatus!=null){battleStatus.setText(mine?"LƯỢT CỦA BẠN":"LƯỢT ĐỐI THỦ");battleStatus.setForeground(mine?Theme.CYAN:Theme.GOLD);}
                        enemyBoard.setInteractive(mine);
                    }
                    case "FLEET_STATE" -> {
                        int myCount=m.get(0).equals(username)?Integer.parseInt(m.get(1)):Integer.parseInt(m.get(3));
                        int enemyCount=m.get(0).equals(username)?Integer.parseInt(m.get(3)):Integer.parseInt(m.get(1));
                        if(myShipsLabel!=null)myShipsLabel.setText("BẠN CÒN: "+myCount+"/5 TÀU");
                        if(enemyShipsLabel!=null)enemyShipsLabel.setText("ĐỐI THỦ CÒN: "+enemyCount+"/5 TÀU");
                    }
                    case "ATTACK_RESULT" -> {
                        String shooter=m.get(0);
                        SoundFX.play(m.get(3).equals("SUNK")?"SUNK":m.get(3).equals("HIT")?"HIT":"MISS");
                        int r=Integer.parseInt(m.get(1)),c=Integer.parseInt(m.get(2));
                        boolean hit=!m.get(3).equals("MISS");
                        boolean mine=shooter.equals(username);
                        if(mine)enemyBoard.shot(r,c,hit);else ownBoard.shot(r,c,hit);
                        String coord=""+(char)('A'+c)+(r+1);
                        String resultText=m.get(3).equals("MISS")?"TRƯỢT":m.get(3).equals("HIT")?"TRÚNG":"CHÌM TÀU";
                        String ship=m.get(4);
                        String details="["+(mine?"Bạn":"Đối thủ")+"] "+coord+" → "+resultText;
                        if(m.get(3).equals("SUNK"))details+=": "+shipDisplayName(ship);
                        appendBattleLog(details);
                        if(eventBanner!=null){
                            eventBanner.setText("<html>"+(m.get(3).equals("SUNK")?(mine?"Bạn đánh chìm ":"Đối thủ đánh chìm ")+shipDisplayName(ship):details)+"</html>");
                            eventBanner.setForeground(m.get(3).equals("SUNK")?Theme.RED:hit?Theme.GREEN:Theme.MUTED);
                        }
                    }
                    case "GAME_OVER" -> {
                        net.send("PROFILE");
                        boolean win=m.get(0).equals("WIN");
                        SoundFX.play(win?"WIN":"LOSS");
                        matchId=-1;
                        resultTitle.setText(win?"★  CHIẾN THẮNG  ★":"THẤT BẠI");
                        resultTitle.setForeground(win?Theme.GREEN:Theme.RED);
                        resultPlayers.setText(username.toUpperCase()+"      VS      "+m.get(1).toUpperCase());
                        showScreen("result");
                    }
                    case "OPPONENT_LEFT" -> {matchId=-1;info("Đối thủ đã thoát khỏi trận.");net.send("ONLINE");showScreen("lobby");}
                    case "RANKING_BEGIN" -> {
                        rankCursor=0;
                        for(int i=0;i<5;i++){rankName[i].setText("—");rankScore[i].setText("— điểm");rankRate[i].setText("—%");}
                    }
                    case "RANKING_ROW" -> {
                        if(rankCursor<5){
                            rankName[rankCursor].setText(m.get(0));
                            rankScore[rankCursor].setText(m.get(3)+" điểm");
                            rankRate[rankCursor].setText(m.get(4)+"%");
                            rankCursor++;
                        }
                    }
                    case "HISTORY_BEGIN" -> {
                        historyCursor=0;
                        for(int i=0;i<3;i++){historyTitle[i].setText("Chưa có dữ liệu");historyMeta[i].setText("—");}
                    }
                    case "HISTORY_ROW" -> {
                        if(historyCursor<3){
                            historyTitle[historyCursor].setText((m.get(2).equals("WIN")?"THẮNG":"THUA")+" • "+m.get(1));
                            historyMeta[historyCursor].setText("Match #"+m.get(0)+" • "+m.get(3));
                            historyCursor++;
                        }
                    }
                    case "INFO" -> info(m.get(0));
                }
            }catch(Exception e){error(e.getMessage());}
        });
    }

    private void appendBattleLog(String line){
        if(battleLog==null)return;
        String time=java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        battleLog.append("["+time+"] "+line+"\n");
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    private String shipDisplayName(String name){
        return switch(name){
            case "Carrier" -> "Tàu sân bay";
            case "Battleship" -> "Thiết giáp hạm";
            case "Cruiser" -> "Tuần dương hạm";
            case "Destroyer" -> "Tàu khu trục";
            case "Patrol" -> "Tàu tuần tra";
            default -> name;
        };
    }

    private void updateProfile(Protocol.Message m){
        int wins=Integer.parseInt(m.get(1)),losses=Integer.parseInt(m.get(2)),rating=Integer.parseInt(m.get(3));
        if(lobbyUserLabel!=null)lobbyUserLabel.setText(m.get(0));
        String[] vals={""+(wins+losses),""+wins,""+losses,""+rating};
        for(int i=0;i<4;i++)if(statValue[i]!=null)statValue[i].setText(vals[i]);
        if(lobbyWinRateLabel!=null)lobbyWinRateLabel.setText(
            String.format("Tỉ lệ thắng: %.1f%%",wins+losses==0?0.0:(100.0*wins/(wins+losses))));
    }
    private void error(String s){
        GameDialog.show(this,"Lỗi",s==null?"Lỗi không xác định":s,true);
    }

    private void info(String s){
        GameDialog.show(this,"Thông báo",s,false);
    }
}
