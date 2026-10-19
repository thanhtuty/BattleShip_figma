package battleship.client;

import battleship.network.Protocol;
import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GameClient implements Closeable {
    private Socket socket;
    private volatile boolean closing=false;
    private BufferedReader in;
    private PrintWriter out;
    private final CopyOnWriteArrayList<Consumer<Protocol.Message>> listeners=new CopyOnWriteArrayList<>();

    public void addListener(Consumer<Protocol.Message> l){listeners.add(l);}
    public void removeListener(Consumer<Protocol.Message> l){listeners.remove(l);}

    public void connect(String host,int port)throws IOException{
        if(isConnected())return;
        closing=false;
        Socket connected=new Socket(host,port);
        socket=connected;
        BufferedReader reader=new BufferedReader(new InputStreamReader(connected.getInputStream()));
        in=reader;
        out=new PrintWriter(new OutputStreamWriter(connected.getOutputStream()),true);
        Thread t=new Thread(()->{
            try{
                String line;
                while((line=reader.readLine())!=null){
                    Protocol.Message m=Protocol.parse(line);
                    for(var l:listeners) l.accept(m);
                }
            }catch(IOException ignored){} finally {
                try{connected.close();}catch(IOException ignored){}
                if(socket==connected&&!closing){
                    Protocol.Message event=new Protocol.Message("DISCONNECTED",java.util.List.of("Mất kết nối TCP tới server"));
                    for(var listener:listeners)listener.accept(event);
                }
            }
        },"server-listener");
        t.setDaemon(true);t.start();
    }

    public void send(String type,String...fields){
        if(!isConnected()||out==null) throw new IllegalStateException("Chưa kết nối server");
        out.println(Protocol.msg(type,fields));
        if(out.checkError())throw new IllegalStateException("Không gửi được dữ liệu TCP");
    }
    public boolean isConnected(){return socket!=null&&socket.isConnected()&&!socket.isClosed();}
    @Override public void close()throws IOException{closing=true;if(socket!=null)socket.close();out=null;in=null;}
}
