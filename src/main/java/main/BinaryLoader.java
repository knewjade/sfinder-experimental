package main;

import bin.SolutionBinary;
import bin.SolutionIntBinary;
import bin.SolutionShortBinary;

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

    public static SolutionShortBinary loadShortBinary(String name) throws IOException {
        int totalByte = getTotalByte(name);

        short[] shorts = new short[totalByte / 2];
        try (DataInputStream dataInStream = new DataInputStream(new BufferedInputStream(new FileInputStream(name)))) {
            for (int index = 0, length = shorts.length; index < length; index++) {
                shorts[index] = dataInStream.readShort();
            }
        }

        return new SolutionShortBinary(shorts);
    }

    public static SolutionIntBinary loadIntBinary(String name) throws IOException {
        int totalByte = getTotalByte(name);

        int[] ints = new int[totalByte / 4];
        try (DataInputStream dataInStream = new DataInputStream(new BufferedInputStream(new FileInputStream(name)))) {
            for (int index = 0, length = ints.length; index < length; index++) {
                ints[index] = dataInStream.readInt();
            }
        }

        return new SolutionIntBinary(ints);
    }
}
