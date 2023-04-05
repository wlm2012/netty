package io.netty.example.nio.datagramChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class DatagramChannelClient {

    public static void main(String[] args) throws IOException {
        DatagramChannel client = null;
        client = DatagramChannel.open();

        client.bind(null);

        String msg = "Hello World!";
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8989);

        Charset charset = StandardCharsets.US_ASCII;
        System.out.println("client.send " + charset.decode(buffer));
        buffer.rewind();
        client.send(buffer, serverAddress);
        buffer.clear();
        System.out.println(" buffer.clear() " + charset.decode(buffer));
        buffer.clear();
        client.receive(buffer);
        System.out.println("client.receive " + charset.decode(buffer));

        buffer.flip();
        client.close();
    }
}