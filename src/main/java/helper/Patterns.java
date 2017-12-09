package helper;

public class Patterns {
    public static String hold(int cycle) {
        switch (cycle) {
            case 1:
                return "*p7,*p4";
            case 2:
                return "*,*p3,*p7";
            case 3:
                return "*,*p7,*p3";
            case 4:
                return "*,*p4,*p6";
            case 5:
                return "*,*,*p7,*p2";
            case 6:
                return "*,*p5,*p5";
            case 7:
                return "*,*p2,*p7,*";
            case 8:
                return "*,*p6,*p4";
        }
        throw new IllegalArgumentException("cycle should be [1, 8]");
    }

    public static String withoutHold(int cycle) {
        switch (cycle) {
            case 1:
                return "*p7,*p3";
            case 2:
                return "*p4,*p6";
            case 3:
                return "*,*p7,*p2";
            case 4:
                return "*p5,*p5";
            case 5:
                return "*p2,*p7,*";
            case 6:
                return "*p6,*p4";
            case 7:
                return "*p3,*p7";
        }
        throw new IllegalArgumentException("cycle should be [1, 7]");
    }
}
