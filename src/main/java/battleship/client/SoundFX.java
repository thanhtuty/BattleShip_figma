package battleship.client;

import javax.sound.sampled.*;
import java.util.concurrent.*;

public final class SoundFX {
    private static volatile boolean enabled=true;
    private static final ExecutorService audio=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"game-audio");t.setDaemon(true);return t;});
    private SoundFX(){}
    public static void enabled(boolean value){enabled=value;}
    public static boolean enabled(){return enabled;}
    public static void play(String event){
        if(!enabled)return;
        audio.execute(()->{
            try{
                int[] notes=switch(event){
                    case "FIRE" -> new int[]{320};
                    case "HIT" -> new int[]{660,480};
                    case "MISS" -> new int[]{210};
                    case "SUNK" -> new int[]{740,600,450};
                    case "INVITE" -> new int[]{500,670};
                    case "ONLINE" -> new int[]{600,880};
                    case "WIN" -> new int[]{523,659,784,1046};
                    case "LOSS" -> new int[]{440,330,220};
                    default -> new int[]{520};
                };
                for(int hz:notes)tone(hz,115);
            }catch(Exception ignored){}
        });
    }
    private static void tone(int freq,int ms)throws Exception{
        int sampleRate=22050;
        byte[] bytes=new byte[sampleRate*ms/1000*2];
        for(int i=0;i<bytes.length/2;i++){
            double envelope=Math.min(1,i/440.0)*Math.min(1,(bytes.length/2-i)/900.0);
            short value=(short)(Math.sin(2*Math.PI*freq*i/sampleRate)*9000*envelope);
            bytes[i*2]=(byte)value;
            bytes[i*2+1]=(byte)(value>>8);
        }
        AudioFormat format=new AudioFormat(sampleRate,16,1,true,false);
        SourceDataLine line=AudioSystem.getSourceDataLine(format);
        line.open(format);line.start();line.write(bytes,0,bytes.length);line.drain();line.close();
    }
}
