package yak.etc;

import java.io.IOException;

public class DemoServer extends BaseServer {
	public static void main(String[] args) throws IOException {
		System.err.println("Hello, World");
		new DemoServer(9999).run();
	}

	public DemoServer(int port) {
		super(port);
	}

	public Response handleRequest(Request req) {
		String z = "{REQ PATH=" + Show(req.path) + " QUERY=" + Show(req.query)
				+ "}";

		System.err.println(z);
		return new Response(z);
	}
}
