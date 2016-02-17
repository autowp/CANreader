package com.autowp.can.adapter.netcan;

import com.autowp.can.adapter.canhacker.CanHackerException;

@SuppressWarnings("serial")
public class CanHackerNetException extends CanHackerException {
    public CanHackerNetException(String message)
    {
        super(message);
    }

}
