package com.autowp.can;

public class CanBusSpecs {
    protected int speed; // kbit
    
    protected int[] multiframeAbitrationID = new int[0];
    
    public int getSpeed()
    {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
    
    public boolean isMultiFrame(int id)
    {
        boolean result = false;
        for (int i=0; i<multiframeAbitrationID.length; i++) {
            if (multiframeAbitrationID[i] == id) {
                result = true;
                break;
            }
        }
        
        return result;
    }
}
