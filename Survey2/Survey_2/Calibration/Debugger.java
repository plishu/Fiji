public class Debugger{
    public static boolean DEBUGMODE = false;
    private static Debugger bugsquasher = null;

    public static Debugger getInstance(){
        if( bugsquasher == null ){
            bugsquasher = new Debugger();
        }
        return bugsquasher;
    }

    public static Debugger getInstance(boolean bol){
        Debugger db = getInstance();
        db.DEBUGMODE = bol;

        return db;
    }

    public static boolean getDebugMode(){
        return DEBUGMODE;
    }
}
