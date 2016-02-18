package com.autowp.can.adapter.canhacker.command;

public abstract class Command {
    protected char name;
    
    abstract public byte[] getBytes();
}
