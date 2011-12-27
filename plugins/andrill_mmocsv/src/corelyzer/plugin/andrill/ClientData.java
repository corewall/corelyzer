package corelyzer.plugin.andrill;

public class ClientData {
    float   x;
    float   y;
    String  name;
    String  realname;
    int     state;
    boolean loggedIn;
    int     userID;
    long    lastHeartBeat;
    long    curPosIndex;

    /** Only used for client's internal management */
    int     freeDrawID;
    boolean startHeartbeatCheck;

    public ClientData() {
        x = 0;
        y = 0;
        name = new String("Unknown");
        loggedIn = false;
        userID = 1;
        freeDrawID = -1;
        startHeartbeatCheck = false;
        curPosIndex = 0;
        lastHeartBeat = 0;
    }

    public float   getX()       { return x; }
    public float   getY()       { return y; }
    public String  getName()    { return name; }
    public String  getRealName(){ return realname; }
    public boolean isLoggedIn() { return loggedIn; }
    public int     getState()   { return state; }
    public int     getUserID()  { return userID; }
    public long    getCurPosIndex() { return curPosIndex; }
    
    public long    getLastHeartBeat() { 
        return lastHeartBeat;
    }
    
    public int     getFreeDrawID() { 
        return freeDrawID; 
    }
    
    public void setX(float p)           { x = p; }
    public void setY(float p)           { y = p; }
    public void setXY(float x, float y) { this.x = x; this.y = y; }
    public void setName(String s)       { name = s; }
    public void setRealName(String s)   { realname = s;}
    public void setLoggedIn(boolean b)  { loggedIn = b; }
    public void setState(int s)         { state = s; }
    public void setUserID(int i)        { userID = i; }
    public void setCurPosIndex(long i)  { curPosIndex = i; }
    public void setFreeDrawID(int i)    { freeDrawID = i; }

    public void setLastHeartBeat(long b) { 
        lastHeartBeat = b; 
        System.out.println("User " + userID + " HB: " + lastHeartBeat);
    }
}
