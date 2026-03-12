package eu.asangarin.endereyesgui.util;

import net.minecraft.ChatFormatting;

import java.awt.*;

public enum EnderEyeDifficult {
    EASY(Color.GREEN),
    NORMAL(Color.YELLOW),
    HARD(Color.RED);

    private final Color color;
    EnderEyeDifficult(Color color){
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public ChatFormatting getColorForChat(){
        if (this==EASY){
            return ChatFormatting.GREEN;
        }else if(this==NORMAL ){
            return ChatFormatting.YELLOW;
        }else {
            return ChatFormatting.RED;
        }
    }
    public String getTranslate(){
        return  "endereyes.difficult."+name().toLowerCase();
    }
}
