package battleship.server;

import battleship.config.DbConfig;
import battleship.dao.*;
import battleship.game.*;
import battleship.network.Protocol;

import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class BattleshipServer {
    private final int port;
    private final Map<String,PlayerSession> online=new ConcurrentHashMap<>();
    private final Map<Long,MatchRoom> rooms=new ConcurrentHashMap<>();
    private final UserDAO users=new UserDAO();
    private final Map<String,Set<String>> invitations=new ConcurrentHashMap<>();
    private final MatchDAO matches=new MatchDAO();
    private final FriendDAO friends=new FriendDAO();
    private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"turn-clock");t.setDaemon(true);return t;});

    public BattleshipServer(int port){this.port=port;}

    public static void main(String[] args)throws Exception{
        int p=args.length>0?Integer.parseInt(args[0]):5050;
        new BattleshipServer(p).start();
    }

    public void start()throws Exception{
        System.out.println("SQL Server: "+(DbConfig.ping()?"CONNECTED":"NOT CONNECTED"));
        clock.scheduleAtFixedRate(this::tick,1,1,TimeUnit.SECONDS);
        try(ServerSocket ss=new ServerSocket(port)){
            System.out.println("Battleship TCP server listening on "+port);
            while(true) new Thread(new PlayerSession(ss.accept(),this)).start();
        }
    }

    public void handle(PlayerSession s, Protocol.Message m)throws Exception{
        switch(m.type()){
            case "REGISTER" -> register(s,m);
            case "LOGIN" -> login(s,m);
            case "LOGOUT" -> logout(s);
            case "LEAVE" -> leave(s);
            case "ONLINE" -> sendOnline(s);
            case "CHALLENGE" -> challenge(s,m.get(0));
            case "ACCEPT" -> accept(s,m.get(0));
            case "REJECT" -> reject(s,m.get(0));
            case "READY" -> ready(s,m.get(0));
            case "ATTACK" -> attack(s,m.intAt(0),m.intAt(1));
            case "RANKING" -> ranking(s);
            case "HISTORY" -> history(s);
            case "PROFILE" -> profile(s);
            case "ADD_FRIEND" -> addFriend(s,m.get(0));
            case "FRIENDS" -> friends(s);
            case "PING" -> s.send("PONG");
            default -> s.send("ERROR","UNKNOWN_COMMAND");
        }
    }

    private void register(PlayerSession s,Protocol.Message m){
        try{
            var u=users.register(m.get(0),m.get(1),m.get(2));
            s.send("REGISTER_OK",u.username());
        }catch(SQLException e){
            s.send("REGISTER_FAIL",e.getMessage());
        }
    }

    private void login(PlayerSession s,Protocol.Message m)throws SQLException{
        var u=users.login(m.get(0),m.get(1));
        if(u==null){s.send("LOGIN_FAIL","Sai tài khoản hoặc mật khẩu");return;}
        if(online.containsKey(u.username())){s.send("LOGIN_FAIL","Tài khoản đang đăng nhập");return;}
        s.user(u);online.put(u.username(),s);
        s.send("LOGIN_OK",u.username(),String.valueOf(u.id()),String.valueOf(u.rating()));
        profile(s);
        broadcastOnline();
        friends(s);
    }

    private void challenge(PlayerSession s,String target){
        requireLogin(s);
        PlayerSession t=online.get(target);
        if(t==null){s.send("ERROR","Người chơi không online");return;}
        if(t==s){s.send("ERROR","Không thể tự thách đấu");return;}
        if(s.matchId()!=null||t.matchId()!=null){s.send("ERROR","Một trong hai người đang ở trong trận");return;}
        invitations.computeIfAbsent(t.username(),k->ConcurrentHashMap.newKeySet()).add(s.username());
        t.send("INVITE",s.username());
        s.send("INFO","Đã gửi lời mời tới "+target);
    }

    private synchronized void accept(PlayerSession s,String challenger)throws SQLException{
        requireLogin(s);
        PlayerSession c=online.get(challenger);
        if(c==null){s.send("ERROR","Người thách đấu đã offline");return;}
        Set<String> pending=invitations.get(s.username());
        if(pending==null || !pending.remove(challenger)){s.send("ERROR","Lời mời không tồn tại hoặc đã hết hạn");return;}
        if(s.matchId()!=null||c.matchId()!=null){s.send("ERROR","Trận không còn khả dụng");return;}
        long id=matches.create(c.userId(),s.userId());
        MatchRoom r=new MatchRoom(id,c,s);rooms.put(id,r);
        c.matchId(id);s.matchId(id);
        c.send("MATCH_CREATED",String.valueOf(id),s.username());
        s.send("MATCH_CREATED",String.valueOf(id),c.username());
    }

    private void reject(PlayerSession s,String challenger){
        Set<String> pending=invitations.get(s.username());
        if(pending!=null)pending.remove(challenger);
        PlayerSession c=online.get(challenger);
        if(c!=null)c.send("INVITE_REJECTED",s.username());
    }

    private synchronized void ready(PlayerSession s,String fleet)throws SQLException{
        MatchRoom r=roomOf(s);
        if(r.started || r.ready(s)){s.send("ERROR","Đã sẵn sàng hoặc trận đã bắt đầu");return;}
        Board b=FleetCodec.toBoard(fleet);
        r.setReady(s,b);
        matches.event(r.id,s.userId(),"READY",null,null,null);
        r.p1.send("READY_STATUS",r.p1.username(),String.valueOf(r.r1),r.p2.username(),String.valueOf(r.r2));
        r.p2.send("READY_STATUS",r.p1.username(),String.valueOf(r.r1),r.p2.username(),String.valueOf(r.r2));
        if(r.r1&&r.r2&&!r.started){
            r.start();matches.start(r.id);
            String first=r.currentUserId==r.p1.userId()?r.p1.username():r.p2.username();
            r.p1.send("GAME_START",first);
            r.p2.send("GAME_START",first);
            sendFleetState(r);
            sendTurn(r);
        }
    }

    private synchronized void attack(PlayerSession s,int row,int col)throws SQLException{
        MatchRoom r=roomOf(s);
        if(!r.started||r.finished){s.send("ERROR","Trận chưa bắt đầu hoặc đã kết thúc");return;}
        if(r.currentUserId!=s.userId()){s.send("ERROR","Chưa đến lượt của bạn");return;}
        if(System.currentTimeMillis()>=r.turnDeadline){s.send("ERROR","Đã hết thời gian của lượt này");return;}
        Board enemy=r.enemyBoard(s);
        Board.Shot result=enemy.attack(row,col);
        if(result==Board.Shot.ALREADY){s.send("ERROR","Ô này đã bắn");return;}
        PlayerSession target=r.other(s);
        String shipName=result==Board.Shot.SUNK?enemy.lastSunkShip():"";
        matches.event(r.id,s.userId(),"ATTACK",row,col,result==Board.Shot.SUNK?"SUNK:"+shipName:result.name());

        s.send("ATTACK_RESULT",s.username(),String.valueOf(row),String.valueOf(col),result.name(),shipName);
        target.send("ATTACK_RESULT",s.username(),String.valueOf(row),String.valueOf(col),result.name(),shipName);

        sendFleetState(r);
        if(enemy.allSunk()){
            endMatch(r,s,target,"ALL_SUNK");
            return;
        }

        // Luật chuẩn bài: trúng giữ lượt, trượt đổi lượt.
        r.nextTurn(result==Board.Shot.MISS?target:s);
        sendTurn(r);
    }



    private synchronized void tick(){
        try{
            for(MatchRoom r:new ArrayList<>(rooms.values())){
                if(!r.started||r.finished)continue;
                long remaining=r.turnDeadline-System.currentTimeMillis();
                if(remaining>0)continue;
                PlayerSession late=r.p1.userId()==r.currentUserId?r.p1:r.p2;
                int times=r.incrementMisses(late);
                matches.event(r.id,late.userId(),"TURN_TIMEOUT",null,null,String.valueOf(times));
                r.p1.send("TURN_TIMEOUT",late.username(),String.valueOf(times));
                r.p2.send("TURN_TIMEOUT",late.username(),String.valueOf(times));
                if(times>3){endMatch(r,r.other(late),late,"TIMEOUT");}
                else{r.nextTurn(r.other(late));sendTurn(r);}
            }
        }catch(Exception e){System.err.println("Turn clock: "+e.getMessage());}
    }

    private void sendTurn(MatchRoom r){
        String current=r.p1.userId()==r.currentUserId?r.p1.username():r.p2.username();
        String remaining=String.valueOf(Math.max(0,(r.turnDeadline-System.currentTimeMillis()+999)/1000));
        r.p1.send("TURN",current,remaining);
        r.p2.send("TURN",current,remaining);
    }

    private void endMatch(MatchRoom r,PlayerSession winner,PlayerSession loser,String reason)throws SQLException{
        if(r.finished)return;
        matches.finish(r.id,winner.userId(),loser.userId());
        r.finished=true;
        matches.event(r.id,loser.userId(),"GAME_OVER",null,null,reason);
        winner.send("GAME_OVER","WIN",loser.username(),reason);
        loser.send("GAME_OVER","LOSS",winner.username(),reason);
        winner.matchId(null);loser.matchId(null);rooms.remove(r.id);
    }

    private synchronized void leave(PlayerSession s)throws SQLException{
        if(s.matchId()==null){s.send("LEFT_OK");return;}
        MatchRoom r=roomOf(s);
        if(r.started)endMatch(r,r.other(s),s,"FORFEIT");
        else{
            matches.cancel(r.id);
            r.other(s).send("OPPONENT_LEFT",s.username());
            r.other(s).matchId(null);s.matchId(null);rooms.remove(r.id);
            s.send("LEFT_OK");
        }
    }

    private synchronized void logout(PlayerSession s)throws SQLException{
        if(s.matchId()!=null)leave(s);
        online.remove(s.username(),s);
        s.user(null);
        s.send("LOGOUT_OK");
        broadcastOnline();
    }

    private void sendFleetState(MatchRoom r){
        String a=String.valueOf(r.b1.remainingShips()),b=String.valueOf(r.b2.remainingShips());
        r.p1.send("FLEET_STATE",r.p1.username(),a,r.p2.username(),b);
        r.p2.send("FLEET_STATE",r.p1.username(),a,r.p2.username(),b);
    }

    private void addFriend(PlayerSession s,String target)throws SQLException{
        requireLogin(s);
        if(!online.containsKey(target)){s.send("ERROR","Người chơi không online");return;}
        if(friends.add(s.userId(),target)){
            s.send("FRIEND_ADDED",target);
            PlayerSession other=online.get(target);
            if(other!=null){other.send("FRIEND_ADDED",s.username());friends(other);}
            friends(s);
        }else s.send("ERROR","Không tìm thấy người chơi hoặc không thể kết bạn");
    }

    private void friends(PlayerSession s)throws SQLException{
        requireLogin(s);
        s.send("FRIENDS_LIST",String.join(",",friends.list(s.userId())));
    }

    private void profile(PlayerSession s)throws SQLException{
        requireLogin(s);
        var u=users.profile(s.userId());
        if(u!=null)s.send("PROFILE_DATA",u.username(),String.valueOf(u.wins()),String.valueOf(u.losses()),String.valueOf(u.rating()));
    }

    private void ranking(PlayerSession s)throws SQLException{
        List<UserDAO.RankRow> list=users.ranking(20);
        s.send("RANKING_BEGIN",String.valueOf(list.size()));
        for(var x:list) s.send("RANKING_ROW",x.username(),String.valueOf(x.wins()),String.valueOf(x.losses()),String.valueOf(x.rating()),String.format(Locale.US,"%.2f",x.winRate()));
        s.send("RANKING_END");
    }

    private void history(PlayerSession s)throws SQLException{
        requireLogin(s);
        var list=matches.history(s.userId());
        s.send("HISTORY_BEGIN",String.valueOf(list.size()));
        for(var x:list) s.send("HISTORY_ROW",String.valueOf(x.matchId()),x.opponent(),x.result(),String.valueOf(x.finishedAt()));
        s.send("HISTORY_END");
    }

    private MatchRoom roomOf(PlayerSession s){
        if(s.matchId()==null) throw new IllegalStateException("Bạn chưa ở trong trận");
        MatchRoom r=rooms.get(s.matchId());
        if(r==null) throw new IllegalStateException("Không tìm thấy trận");
        return r;
    }
    private void requireLogin(PlayerSession s){if(s.user()==null)throw new IllegalStateException("Chưa đăng nhập");}

    private void sendOnline(PlayerSession s){
        List<String> names=new ArrayList<>(online.keySet());Collections.sort(names);
        s.send("ONLINE_LIST",String.join(",",names));
    }
    private void broadcastOnline(){for(PlayerSession s:online.values())sendOnline(s);}

    public void disconnect(PlayerSession s){
        if(s.username()!=null){
            online.remove(s.username(),s);
            invitations.remove(s.username());
            for(Set<String> pending:invitations.values())pending.remove(s.username());
        }
        Long mid=s.matchId();
        if(mid!=null){
            MatchRoom r=rooms.remove(mid);
            if(r!=null&&!r.finished){
                matches.cancel(r.id);
                PlayerSession other=r.other(s);
                other.send("OPPONENT_LEFT",s.username());
                other.matchId(null);
            }
        }
        s.matchId(null);
        broadcastOnline();
    }
}
