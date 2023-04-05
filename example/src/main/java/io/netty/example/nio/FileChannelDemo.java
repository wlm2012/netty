package io.netty.example.nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class FileChannelDemo {

    public static void main(String[] args) throws IOException {
        //append the content to existing file
        writeFileChannel(ByteBuffer.wrap("Welcome to TutorialsPoint".getBytes()));
        //read the file
        readFileChannel();
    }

    private static void readFileChannel() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile("." + File.separator + "temp.txt", "r");
             FileChannel fileChannel = file.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(128);
            Charset charset = StandardCharsets.US_ASCII;
            while (fileChannel.read(byteBuffer) > 0) {
                // potion指针指向0
//                byteBuffer.rewind();
                // 切换读写位置的potion指针
                byteBuffer.flip();
                System.out.println(charset.decode(byteBuffer));
                // 清楚缓存，并把potion指向0
                byteBuffer.clear();
            }
        }
    }

    private static void writeFileChannel(ByteBuffer byteBuffer) throws IOException {
        Set<StandardOpenOption> options = new HashSet<>();
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.APPEND);
        Path path = Paths.get("." + File.separator + "temp.txt");
        FileChannel fileChannel = FileChannel.open(path, options);
        fileChannel.write(byteBuffer);
        fileChannel.close();
    }


}
