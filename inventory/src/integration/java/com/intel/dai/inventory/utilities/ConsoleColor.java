package com.intel.dai.inventory.utilities;

public class ConsoleColor {
    //    def red(def msg) { return str }
    static public String black(String msg) { return black + msg + nc; }

    static public String red(String msg) { return red + msg + nc; }

    static public String green(String msg) { return green + msg + nc; }

    static public String yellow(String msg) { return yellow + msg + nc; }

    static public String blue(String msg) { return blue + msg + nc; }

    static public String magenta(String msg) { return magenta + msg + nc; }

    static public String cyan(String msg) { return cyan + msg + nc; }

    static public String white(String msg) { return white + msg + nc; }

    static public final String nc = "\033[0m"; // No Color (reset);
    static private final String black = "\033[30m";
    static private final String red = "\033[31m";
    static private final String green = "\033[32m";
    static private final String yellow = "\033[33m";
    static private final String blue = "\033[34m";
    static private final String magenta = "\033[35m";
    static private final String cyan = "\033[36m";
    static private final String white = "\033[37m";
}
