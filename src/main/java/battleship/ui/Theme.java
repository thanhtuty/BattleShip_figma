package battleship.ui;
import java.awt.*;

public final class Theme {
    private Theme(){}
    public static final Color BG=new Color(0x071521), NAV=new Color(0x0C2234), PANEL=new Color(0x122D41),
            PANEL2=new Color(0x1A3B51), GRID=new Color(0x12415A), STROKE=new Color(0x30556B),
            TEXT=new Color(0xECF8FF), MUTED=new Color(0x92B3C4), CYAN=new Color(0x31DBEC),
            GOLD=new Color(0xFFB75E), GREEN=new Color(0x54DDA6), RED=new Color(0xFF7078);
    public static Font font(int size,boolean bold){return new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size);}
}
