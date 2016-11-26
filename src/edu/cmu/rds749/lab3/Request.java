package edu.cmu.rds749.lab3;

import java.util.Comparator;

/**
 * Created by sharath on 17/11/16.
 */
public class Request implements Comparable<Request>
{
    public static final int CHANGE = 1;
    public static final int READ = 0;
    private int requestType;
    private int requestId;

    //only if it is update;
    private int update;

    public Request(int requestId,int requestType)
    {
        this.requestType = requestType;
        this.requestId = requestId;
    }

    public int getRequestType()
    {
        return requestType;
    }

    public void setRequestType(int requestType)
    {
        this.requestType = requestType;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId)
    {
        this.requestId = requestId;
    }

    public int getUpdate()
    {
        return update;
    }

    public void setUpdate(int update)
    {
        this.update = update;
    }


    public int compareTo(Request another)
    {
        return Integer.compare(this.requestId,another.requestId);
    }
}

