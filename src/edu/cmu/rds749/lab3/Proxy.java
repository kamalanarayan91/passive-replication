package edu.cmu.rds749.lab3;

import IceInternal.Ex;
import edu.cmu.rds749.common.AbstractProxy;
import edu.cmu.rds749.common.BankAccountStub;
import org.apache.commons.configuration2.Configuration;
import rds749.Checkpoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Failure Modes.
 * 1. Failure while making request
 * 2. Failure while change over.
 * 3. Failure while get state
 * 4. Failure while set State
 * 5. Failure while ?
 */

/**
 * Created by jiaqi on 8/28/16.
 *
 * Implements the Proxy.
 */
public class Proxy extends AbstractProxy
{
    // Server info
    public ArrayList<Server> backupServers;
    public HashMap<Long,Server> serverMap;
    public Server primaryServer;

    private Object operationsLock;
    private Object failOverLock;
    private HashSet<Integer> requestsDone;
    private Timer timer;
    private long checkpointFrequency;

    public Proxy(Configuration config)
    {
        super(config);
        primaryServer=null;
        backupServers = new ArrayList<Server>();
        operationsLock = new Object();
        failOverLock = new Object();
        requestsDone  = new HashSet<Integer>();
        serverMap = new HashMap<Long,Server>();
        checkpointFrequency = config.getLong("checkpointFrequency");
        timer = new Timer();
        System.err.println("CP:"+checkpointFrequency);
        timer.schedule(new CheckPointTask(),0,checkpointFrequency);
    }

    public void setPrimary(Server server)
    {
        primaryServer=server;
    }


    @Override
    protected void serverRegistered(long id, BankAccountStub stub)
    {
        synchronized (failOverLock)
        {
            Server server = new Server(stub);
            server.setId(id);
            serverMap.put(id, server);

            if (primaryServer == null)
            {
                setPrimary(server);

                System.out.println("Added Primary");
                try
                {
                    server.setPrimary();
                }
                catch (Exception e)
                {

                }
            }
            else
            {
                backupServers.add(server);

                try
                {
                    server.setBackup();
                }
                catch (Exception e)
                {

                }
                System.out.println("Added Backup");
            }

        }
    }

    @Override
    protected void beginReadBalance(int reqid)
    {
        synchronized (operationsLock)
        {
            List<Server> failedServers = new ArrayList<Server>();
            boolean primaryFailed = false;

            synchronized (failOverLock)
            {
                if (primaryServer != null)
                {
                    //maybe due to failure -- attempt recovery.
                    //maybe there are no servers at all.
                    try
                    {
                        primaryServer.beginReadBalance(reqid);
                    }
                    catch (Exception e)
                    {
                        // initiate changeover
                        System.err.println("Primary Failed!");
                        primaryFailed = true;
                        primaryServer = null;
                    }
                }
                else
                {
                    primaryFailed=true;
                }


                if(primaryFailed)
                {
                    //choose new Primary and set this stuff
                    if(failOver(reqid)==false) // no more backups. Return exception
                    {
                        clientProxy.RequestUnsuccessfulException(reqid);
                        return;
                    }

                }

                for(Server server: backupServers)
                {
                    try
                    {
                        server.beginReadBalance(reqid);
                    }
                    catch(Exception e)
                    {
                        serverMap.remove(server.getId());
                        backupServers.remove(server);
                    }
                }

                if(backupServers.size()==0 && primaryServer==null)
                {
                    clientProxy.RequestUnsuccessfulException(reqid);
                }

            }

        }
    }


    private boolean failOver(int reqId)
    {
        if(backupServers.size()==0)
        {
            return false;
        }

        boolean set = false;
        for(Server server: backupServers)
        {

            //Set as primary
            try
            {
                server.setPrimary();
            }
            catch (Exception e)
            {
                //failure!
                serverMap.remove(server.getId());
                backupServers.remove(server);
            }


            try
            {
                server.beginReadBalance(reqId);
            }
            catch (Exception e)
            {
                //failure!
                serverMap.remove(server.getId());
                backupServers.remove(server);
            }

            setPrimary(server);
            backupServers.remove(server);
            set = true;
            break;
        }

        return set;
    }



    @Override
    protected void beginChangeBalance(int reqid, int update)
    {
        synchronized (operationsLock)
        {
            List<Server> failedServers = new ArrayList<Server>();
            boolean primaryFailed = false;

            synchronized (failOverLock)
            {
                if (primaryServer != null)
                {
                    //maybe due to failure -- attempt recovery.
                    //maybe there are no servers at all.
                    try
                    {
                        primaryServer.beginChangeBalance(reqid,update);
                    }
                    catch (Exception e)
                    {
                        // initiate changeover
                        System.err.println("Primary Failed!");
                        primaryFailed = true;
                        primaryServer = null;
                    }
                }
                else
                {
                    primaryFailed=true;
                }


                if(primaryFailed)
                {
                    //choose new Primary and set this stuff
                    if(failOver(reqid,update)==false) // no more backups. Return exception
                    {
                        clientProxy.RequestUnsuccessfulException(reqid);
                        return;
                    }

                }

                for(Server server: backupServers)
                {
                    try
                    {
                        server.beginChangeBalance(reqid,update);
                    }
                    catch(Exception e)
                    {
                        serverMap.remove(server.getId());
                        backupServers.remove(server);
                    }
                }

                if(backupServers.size()==0 && primaryServer==null)
                {
                    clientProxy.RequestUnsuccessfulException(reqid);
                }

            }

        }
    }

    private boolean failOver(int reqId, int update)
    {
        if(backupServers.size()==0)
        {
            return false;
        }

        boolean set = false;
        for(Server server: backupServers)
        {

            //Set as primary
            try
            {
                server.setPrimary();
            }
            catch (Exception e)
            {
                //failure!
                serverMap.remove(server.getId());
                backupServers.remove(server);
            }


            try
            {
                server.beginChangeBalance(reqId,update);
            }
            catch (Exception e)
            {
                //failure!
                serverMap.remove(server.getId());
                backupServers.remove(server);
            }

            setPrimary(server);
            set = true;
            backupServers.remove(server);
            break;
        }

        return set;

    }

    @Override
    protected void endReadBalance(long serverid, int reqid, int balance)
    {
        synchronized (operationsLock)
        {
            //dont care who sends it
            System.out.println("In End Read");
            if(requestsDone.contains(reqid))
            {
                return;
            }

            clientProxy.endReadBalance(reqid,balance);
            requestsDone.add(reqid);

        }
    }

    @Override
    protected void endChangeBalance(long serverid, int reqid, int balance)
    {
        synchronized (operationsLock)
        {
            //dont care who sends it
            System.out.println("End Change Balance:");
            if(requestsDone.contains(reqid))
            {
                return;
            }

            clientProxy.endChangeBalance(reqid,balance);
            requestsDone.add(reqid);

            System.out.println("(In Proxy)");
        }
    }

    @Override
    protected void serversFailed(List<Long> failedServers)
    {

        synchronized (failOverLock)
        {
            super.serversFailed(failedServers);
            simpleFailOver(failedServers);
        }

    }

    /**
     * Simple Fail Over: Removes servers from list.
     * @param failedServers
     */
    private void simpleFailOver(List<Long> failedServers)
    {
        for(Long id: failedServers)
        {

            Server server = serverMap.get(id);
            serverMap.remove(server.getId());

            if(backupServers.contains(server))
            {
                backupServers.remove(server);
            }

            if(server==primaryServer)
            {
                System.out.println("Primary Failed -  Heartbeat");
                primaryServer=null;
            }
        }
    }


    //Timer Tasks
    class CheckPointTask extends TimerTask
    {

        @Override
        public void run()
        {
            synchronized (failOverLock)
            {
                Checkpoint checkpoint = null;
                if(primaryServer!=null)
                {
                    try
                    {
                        checkpoint = primaryServer.getState();

                    }
                    catch (Exception e)
                    {

                    }
                    System.err.println("Cp:"+checkpoint.reqid +" "+checkpoint.state);

                    if(checkpoint!=null)
                    {
                        for (Server server : backupServers)
                        {
                            try
                            {
                                server.setState(checkpoint);
                            }
                            catch (Exception e)
                            {

                            }
                        }
                    }

                }
            }

        }
    }

}
