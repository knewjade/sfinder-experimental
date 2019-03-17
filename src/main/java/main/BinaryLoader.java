package main;

import bin.SolutionBinary;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BinaryLoader {
    public static SolutionBinary load(String name) throws IOException {
        int totalByte = getTotalByte(name);

        byte[] bytes = new byte[totalByte];
        try (DataInputStream dataInStream = new DataInputStream(new BufferedInputStream(new FileInputStream(name)))) {
            dataInStream.readFully(bytes, 0, bytes.length);
        }

        return new SolutionBinary(bytes);
    }

    private static int getTotalByte(String name) throws IOException {
        return (int) Files.size(Paths.get(name));
    }
}
