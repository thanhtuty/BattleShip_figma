package battleship.network;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

public final class Protocol {
    private Protocol(){}

    private static String enc(String s){
        return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(StandardCharsets.UTF_8));
    }
    private static String dec(String s){
        return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);
    }

    public static String msg(String type,String... fields){
        StringBuilder sb=new StringBuilder(type);
        for(String f:fields) sb.append('|').append(enc(f));
        return sb.toString();
    }

    public static Message parse(String line){
        String[] p=line.split("\\|",-1);
        List<String> fs=new ArrayList<>();
        for(int i=1;i<p.length;i++) fs.add(dec(p[i]));
        return new Message(p[0],fs);
    }

    public record Message(String type,List<String> fields){
        public String get(int i){ return i<fields.size()?fields.get(i):""; }
        public int intAt(int i){ return Integer.parseInt(get(i)); }
        public long longAt(int i){ return Long.parseLong(get(i)); }
    }
}
