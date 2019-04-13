package com.dinstone.np.netty.echo;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class NioEchoServerTest {

	public static void main(String[] args) throws Exception {
		Socket c = new Socket("localhost", 2222);
		c.setReceiveBufferSize(100);
		System.out.println("ReceiveBufferSize is "+c.getReceiveBufferSize());
		
		OutputStream out = c.getOutputStream();
		StringBuilder b = new StringBuilder();
		while (true) {
			int r = System.in.read();
			b.append(r);
			if (r == 10) {
				String message = b.toString();
				System.out.println("message:" + message);
				b = new StringBuilder();
			}
			if ((char) r == 'q') {
				break;
			}
			out.write(r);
		}

		InputStream in = c.getInputStream();
		int temp = -1;
		while ((temp = in.read()) != -1) {
			System.out.println(temp);
		}
		
		System.out.println("game over");
	}

}
