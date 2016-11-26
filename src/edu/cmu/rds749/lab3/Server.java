package edu.cmu.rds749.lab3;

import edu.cmu.rds749.common.BankAccountStub;
import rds749.Checkpoint;

/**
 * Created by sharath on 17/9/16.
 */
public class Server
{

    private long Id;
    private BankAccountStub serverObject;


    public Server( BankAccountStub serverObject)
    {
        this.serverObject = serverObject;


    }


    public long getId()
    {
        return Id;
    }

    public void setId(long id)
    {
        Id = id;
    }

    public void beginReadBalance(int reqid) throws Exception
    {
        serverObject.beginReadBalance(reqid);
    }


    public void beginChangeBalance(int reqid,int balance) throws Exception
    {
        serverObject.beginChangeBalance(reqid,balance);
    }

    protected Checkpoint getState() throws Exception
    {
        return serverObject.getState();
    }

    protected void setState(Checkpoint checkpoint) throws Exception
    {

        serverObject.setState(checkpoint);

    }

    protected void setPrimary() throws Exception{
        serverObject.setPrimary();
    }

    protected void setBackup() throws Exception{
        serverObject.setBackup();
    }

}
