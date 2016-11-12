package edu.cmu.rds749.lab3;

import edu.cmu.rds749.common.AbstractServer;
import org.apache.commons.configuration2.Configuration;
import rds749.Checkpoint;

/**
 * Implements the BankAccounts transactions interface
 * Created by utsav on 8/27/16.
 */

public class BankAccountI extends AbstractServer
{
    private int balance = 0;
    private ProxyControl ctl;

    public BankAccountI(Configuration config) {
        super(config);
    }

    @Override
    protected void doStart(ProxyControl ctl) throws Exception {
        this.ctl = ctl;
    }

    @Override
    protected void handleBeginReadBalance(int reqid)
    {
    }

    @Override
    protected void handleBeginChangeBalance(int reqid, int update) {
    }

    @Override
    protected Checkpoint handleGetState()
    {
        return new Checkpoint(0,0);
    }

    @Override
    protected void handleSetState(Checkpoint checkpoint)
    {
    }

    @Override
    protected void handleSetPrimary()
    {

    }

    @Override
    protected void handleSetBackup()
    {

    }
}