package io.netty.example.nio.socketChannel;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SocketChannelServer {

    public static void main(String[] args) throws IOException {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress("localhost", 9000));

            Path path = Paths.get("." + File.separator + "temp.txt");
            FileChannel fileChannel = FileChannel.open(path);
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            while (fileChannel.read(byteBuffer) > 0) {
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
            }
            fileChannel.close();
            System.out.println("File Sent");
        }
    }
}
