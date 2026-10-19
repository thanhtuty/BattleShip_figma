package battleship.server;

import battleship.dao.UserDAO;
import battleship.network.Protocol;
import java.io.*;
import java.net.Socket;

public class PlayerSession implements Runnable {
    private final Socket socket;
    private final BattleshipServer server;
    private BufferedReader in;
    private PrintWriter out;
    private UserDAO.User user;
    private volatile Long matchId;

    public PlayerSession(Socket socket,BattleshipServer server){this.socket=socket;this.server=server;}
    public UserDAO.User user(){return user;}
    public void user(UserDAO.User u){this.user=u;}
    public String username(){return user==null?null:user.username();}
    public int userId(){return user==null?0:user.id();}
    public Long matchId(){return matchId;}
    public void matchId(Long v){matchId=v;}

    public synchronized void send(String type,String...fields){
        if(out!=null) out.println(Protocol.msg(type,fields));
    }

    @Override public void run(){
        try(socket){
            in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
            send("WELCOME","Battleship TCP Server");
            String line;
            while((line=in.readLine())!=null){
                try{ server.handle(this,Protocol.parse(line)); }
                catch(Exception e){ send("ERROR",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()); }
            }
        }catch(IOException ignored){
        }finally{
            server.disconnect(this);
        }
    }
}
