package ch.uepaa.quickstart;

public class GlobalVar {

    public String getGlobalVar1() {
        return GlobalVar1;
    }

    public void setGlobalVar1(String GlobalVar1) {
        this.GlobalVar1 = GlobalVar1;
    }

    private String GlobalVar1 = "";

    private static final GlobalVar instance = new GlobalVar();

    private GlobalVar() {
    }

    public static GlobalVar getInstance() {
        return GlobalVar.instance;
    }

}
