package io.netty.example.nio;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class ChannelDemo {

    public static void main(String[] args) throws FileNotFoundException {
        RandomAccessFile file = new RandomAccessFile("./temp.txt","r");

    }
}
