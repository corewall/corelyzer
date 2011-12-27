package corelyzer.plugin.andrill;

public class ServerResponse {
    public static final byte USER_REGISTERED        = 0;
    public static final byte USER_NAME_EXISTS       = 1;
    public static final byte USER_LOGGED_IN         = 2;
    public static final byte USER_LOGGED_OUT        = 3;
    public static final byte USER_PASS_INVALID      = 4;
    public static final byte USER_ALREADY_LOGGED_IN = 5;
    public static final byte SERVER_UNAVAILABLE     = 6;
    public static final byte HEART_BEAT_REQUEST     = 7;
    public static final byte TRACK_CREATED          = 8;

    public static final byte SECTION_RECEIVED       = 10;
    public static final byte SPLIT_CORE_RECEIVED    = 11;
    public static final byte WHOLE_CORE_RECEIVED    = 12;
    public static final byte SPLIT_CORE_LIST        = 13;
    public static final byte WHOLE_CORE_LIST        = 14;
    public static final byte SPLIT_CORE_DATA        = 15;
    public static final byte WHOLE_CORE_DATA        = 16;
    public static final byte SECTION_ERR            = 17;
    public static final byte SPLIT_CORE_AVAILABLE   = 18;
    public static final byte WHOLE_CORE_AVAILABLE   = 19;
    public static final byte NEW_SECTION            = 20;
    public static final byte UPDATE_LOCAL_SECTION_LIST = 21;
    public static final byte SPLIT_CORE_LIST_DONE   = 22;
    public static final byte WHOLE_CORE_LIST_DONE   = 23;
    public static final byte SECTION_LIST_DONE      = 24;
    public static final byte RELAY_LOAD_SECTION     = 25;

    public static final byte NEW_DATASET_AVAILABLE  = 40;
    public static final byte DATASET_REQUEST        = 41;
    public static final byte DATASET_ERR            = 42;
    public static final byte DATASET_LIST           = 43;
    public static final byte TABLE_LIST             = 44;
    public static final byte TABLE_DATA             = 45;
    public static final byte TABLE_LIST_ERR         = 46;
    public static final byte TABLE_DATA_ERR         = 47;
    public static final byte RELAY_START_GRAPHS     = 48;
    public static final byte RELAY_MAKE_GRAPH       = 49;


    public static final byte NEW_CHAT               = 70;
    public static final byte NEW_CHAT_ENTRY         = 71;
    public static final byte CHAT_LIST_FOR_SPLIT_SECTION  = 72;
    public static final byte CHAT_LIST_FOR_WHOLE_SECTION  = 73;

    public static final byte HAVE_SECTION           = 80;
    public static final byte HAVE_NO_SECTION        = 81;
}
