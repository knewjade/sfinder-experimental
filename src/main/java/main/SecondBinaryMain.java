package main;

import java.io.*;

public class SecondBinaryMain {
    public static void main(String[] args) throws IOException {
        String postfix = "";
        String name = "output/9pieces_" + postfix + ".bin";
        try (DataInputStream dataInStream = new DataInputStream(new BufferedInputStream(new FileInputStream(name)))) {
//            dataInStream.readBoolean()
        }
    }
}
