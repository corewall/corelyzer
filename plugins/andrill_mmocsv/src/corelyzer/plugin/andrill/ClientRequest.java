package corelyzer.plugin.andrill;

public class ClientRequest {

    // USER RELATED MESSAGES
    public static final byte REGISTER_USER     = 0;
    public static final byte LOGIN             = 1;
    public static final byte LOGOUT            = 2;
    public static final byte HEAD_COUNT        = 3;
    public static final byte SHUTDOWN_SERVER   = 4;
    public static final byte HEART_BEAT        = 5;

    // IMAGE RELATED MESSAGES
    public static final byte NEW_CORE_SECTION        = 10;
    public static final byte NEW_SPLIT_CORE          = 11;
    public static final byte NEW_WHOLE_CORE          = 12;
    public static final byte POSITION_SPLIT_CORES    = 13;
    public static final byte POSITION_WHOLE_CORES    = 14;
    public static final byte LIST_SPLIT_CORES        = 15;
    public static final byte LIST_WHOLE_CORES        = 16;
    public static final byte GET_SPLIT_CORE_DATA     = 17;
    public static final byte GET_WHOLE_CORE_DATA     = 18;
    public static final byte LIST_SECTIONS           = 19;
    public static final byte HAS_SECTION             = 40;

    // DATASET RELATED MESSAGES
    public static final byte NEW_DATASET    = 20;
    public static final byte END_DATASET    = 21;
    public static final byte NEW_ROW        = 22;
    public static final byte NEW_TABLE      = 23;
    public static final byte END_TABLE      = 24;

    public static final byte LIST_DATASETS  = 25;
    public static final byte LIST_TABLES    = 26;

    public static final byte TABLE_DATA         = 28;
    public static final byte CREATE_GRAPH_EVENT = 29;

    public static final byte MAKE_DATA_REQUESTS = 30;
    public static final byte RELAY_LOAD_SECTION = 31;
    public static final byte RELAY_START_GRAPHS = 32;
    public static final byte RELAY_MAKE_GRAPH   = 33;


    // CHAT RELATED MESSAGES
    public static final byte NEW_CHAT           = 60;
    public static final byte NEW_CHAT_ENTRY     = 61;

    public static final byte RUN_BACKUP     = 100;
}
