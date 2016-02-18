package com.autowp.can.adapter.canhacker.command;

public abstract class SimpleCommand extends Command {
    @Override
    public byte[] getBytes() {
        return new byte[] {(byte) this.name};
    }
}
