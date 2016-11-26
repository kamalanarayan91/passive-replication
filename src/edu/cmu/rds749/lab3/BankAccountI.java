package edu.cmu.rds749.lab3;

import edu.cmu.rds749.common.AbstractServer;
import org.apache.commons.configuration2.Configuration;
import rds749.Checkpoint;

import java.util.PriorityQueue;


/**
 * Implements the BankAccounts transactions interface
 * Created by utsav on 8/27/16.
 */

public class BankAccountI extends AbstractServer
{
    private int balance = 0;
    private ProxyControl ctl;
    private Object logLock;
    // need data structure for logs
    private PriorityQueue<Request> logQueue;

    // need to know if primary
    private boolean isPrimary;
    private int lastReqId;


    public BankAccountI(Configuration config)
    {
        super(config);
        logLock = new Object();
        logQueue = new PriorityQueue<Request>();
    }

    @Override
    protected void doStart(ProxyControl ctl) throws Exception
    {
        this.ctl = ctl;
        this.lastReqId = -1;
        System.out.println("Started!");
    }

    @Override
    protected void handleBeginReadBalance(int reqid)
    {
        if(isPrimary)
        {
            System.out.println("Begin Read - primary");
            ctl.endReadBalance(reqid, balance);

            //assumption - requests should be in order!
            lastReqId = reqid;
        }
        else
        {
            //add to log
            System.out.println("Begin Read - primary Backup?");
            synchronized (logLock)
            {
                Request request = new Request(reqid, Request.READ);
                logQueue.offer(request);
            }
        }
    }

    @Override
    protected void handleBeginChangeBalance(int reqid, int update)
    {
        if(isPrimary)
        {
            //assumption - requests should be in order!
            balance += update;
            lastReqId = reqid;
            ctl.endChangeBalance(reqid, balance);

        }
        else
        {
            //add to log
            synchronized (logLock)
            {
                Request request = new Request(reqid, Request.CHANGE);
                request.setUpdate(update);
                logQueue.offer(request);
            }
        }
    }

    @Override
    protected Checkpoint handleGetState()
    {
        return new Checkpoint(lastReqId,balance);
    }

    @Override
    protected int handleSetState(Checkpoint checkpoint)
    {
        balance = checkpoint.state;
        lastReqId=checkpoint.reqid;


        //prune log before checkpoint
        int cpReqId = checkpoint.reqid;

        //return pruned log size
        synchronized (logLock)
        {
            while(logQueue.peek().getRequestId()<cpReqId)
            {
                logQueue.poll();
            }
        }
        return logQueue.size();
    }

    @Override
    protected void handleSetPrimary()
    {
        System.out.println("Set Primary!!!");
        isPrimary=true;
        return;
    }

    @Override
    protected void handleSetBackup()
    {
        System.out.println("Set Backup!!!");
        isPrimary = false;
        return;
    }
}