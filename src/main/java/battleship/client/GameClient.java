package battleship.client;

import battleship.network.Protocol;
import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GameClient implements Closeable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final CopyOnWriteArrayList<Consumer<Protocol.Message>> listeners=new CopyOnWriteArrayList<>();

    public void addListener(Consumer<Protocol.Message> l){listeners.add(l);}
    public void removeListener(Consumer<Protocol.Message> l){listeners.remove(l);}

    public void connect(String host,int port)throws IOException{
        if(isConnected())return;
        socket=new Socket(host,port);
        in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
        Thread t=new Thread(()->{
            try{
                String line;
                while((line=in.readLine())!=null){
                    Protocol.Message m=Protocol.parse(line);
                    for(var l:listeners) l.accept(m);
                }
            }catch(IOException ignored){}
        },"server-listener");
        t.setDaemon(true);t.start();
    }

    public void send(String type,String...fields){
        if(out==null) throw new IllegalStateException("Chưa kết nối server");
        out.println(Protocol.msg(type,fields));
    }
    public boolean isConnected(){return socket!=null&&socket.isConnected()&&!socket.isClosed();}
    @Override public void close()throws IOException{if(socket!=null)socket.close();}
}
