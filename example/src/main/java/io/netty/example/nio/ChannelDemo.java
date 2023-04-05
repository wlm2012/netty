package io.netty.example.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ChannelDemo {

    public static void main(String[] args) throws IOException {
        System.out.println(System.getProperty("user.dir"));
        try (RandomAccessFile file = new RandomAccessFile("." + File.separator + "temp.txt", "r");
             FileChannel fileChannel = file.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            while (fileChannel.read(byteBuffer) > 0) {
                System.out.println("fileChannel.read");
                // flip the buffer to prepare for get operation
                byteBuffer.flip();
                while (byteBuffer.hasRemaining()) {
                    System.out.println("byteBuffer.get()");
                    System.out.print((char) byteBuffer.get());
                }
            }
        }


    }
}
