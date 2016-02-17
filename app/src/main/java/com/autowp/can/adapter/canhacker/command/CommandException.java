package com.autowp.can.adapter.canhacker.command;

import com.autowp.can.adapter.canhacker.CanHackerException;

public class CommandException extends CanHackerException {
    public CommandException(String message)
    {
        super(message);
    }
}
