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
    private Object operationsLock;

    public BankAccountI(Configuration config)
    {
        super(config);
        logLock = new Object();
        operationsLock = new Object();
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
        synchronized (operationsLock) {
            if (isPrimary) {
                System.out.println("Begin Read - primary");
                ctl.endReadBalance(reqid, balance);

                //assumption - requests should be in order!
                lastReqId = reqid;
            } else {
                //add to log
                System.out.println("Begin Read - primary Backup?");
                synchronized (logLock) {
                    Request request = new Request(reqid, Request.READ);
                    logQueue.offer(request);
                }
            }
        }
    }

    @Override
    protected void handleBeginChangeBalance(int reqid, int update)
    {
        synchronized (operationsLock) {
            if (isPrimary) {
                //assumption - requests should be in order!
                balance += update;
                lastReqId = reqid;
                ctl.endChangeBalance(reqid, balance);

            } else {
                //add to log
                synchronized (logLock) {
                    Request request = new Request(reqid, Request.CHANGE);
                    request.setUpdate(update);
                    logQueue.offer(request);
                }
            }
        }
    }

    @Override
    protected Checkpoint handleGetState()
    {
        synchronized (operationsLock)
        {
            return new Checkpoint(lastReqId, balance);
        }
    }

    @Override
    protected int handleSetState(Checkpoint checkpoint)
    {
        synchronized (operationsLock) {
            balance = checkpoint.state;
            lastReqId = checkpoint.reqid;

            System.err.println("Cp received:" + balance + " last:"+lastReqId);


            //prune log before checkpoint
            int cpReqId = checkpoint.reqid;

            System.err.println("Original Size:"+ logQueue.size());

            //return pruned log size
            synchronized (logLock)
            {
                while (!logQueue.isEmpty() &&logQueue.peek().getRequestId() <= cpReqId)
                {
                    logQueue.poll();
                }
            }

            System.out.println("Pruned Size:"+ logQueue.size());
            return logQueue.size();
        }
    }

    @Override
    protected void handleSetPrimary()
    {
        synchronized (operationsLock)
        {
            System.out.println("Set Primary!!!");
            //assumes cP is already applied
            while(!logQueue.isEmpty())
            {

                Request head = logQueue.poll();

                if(head.getRequestType() == Request.READ)
                {
                    System.err.println("Replay:" + head.getRequestId() +" READ");
                    ctl.endReadBalance(head.getRequestId(),balance);
                }
                else
                {
                    System.err.println("Replay:" + head.getRequestId() +" UPDATE" + ":" + head.getUpdate());
                    balance += head.getUpdate();
                    ctl.endChangeBalance(head.getRequestId(),balance);
                }

            }
            isPrimary = true;
            return;
        }
    }

    @Override
    protected void handleSetBackup()
    {
        synchronized (operationsLock)
        {
            System.err.println("Set Backup!!!");
            isPrimary = false;
            return;
        }
    }
}