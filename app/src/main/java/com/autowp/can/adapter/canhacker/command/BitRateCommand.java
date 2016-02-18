package com.autowp.can.adapter.canhacker.command;

/**
 * Sn[CR]
 * 
 * This command will set the CAN controller to a predefined standard bit rate.
 * It works only after power up or if controller is in reset mode after command C.
 * The following bit rates are available:
 * 
 * S0 10Kbps 
 * S1 20Kbps 
 * S2 50Kbps 
 * S3 100Kbps 
 * S4 125Kbps 
 * S5 250Kbps 
 * S6 500Kbps 
 * S7 800Kbps 
 * S8 1Mbps 
 * 
 * Return: [CR] or [BEL] 
 */
public class BitRateCommand extends Command {
    public enum BitRate {
        S0 ('0'),
        S1 ('1'),
        S2 ('2'),
        S3 ('3'),
        S4 ('4'),
        S5 ('5'),
        S6 ('6'),
        S7 ('7'),
        S8 ('8');
        
        protected char value;
        
        BitRate (char value) {
            this.value = value;
        }
        
        public char getValue() {
            return this.value;
        }
    }
    
    protected BitRate bitRate;
    
    public BitRateCommand(BitRate bitRate) throws CommandException
    {
        this.name = 'S';
        
        if (bitRate == null) {
            throw new CommandException("BitRate cannot be null");
        }
        
        this.bitRate = bitRate;
    }
    
    @Override
    public byte[] getBytes() {
        return new byte[] {(byte) this.name, (byte) this.bitRate.getValue()};
    }

}
