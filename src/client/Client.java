package client;

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.cert.*;

/*
 * This example shows how to set up a key manager to perform client
 * authentication.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class Client {

	public static void main(String[] args) throws Exception {
		String host = null;
		int port = 1337; // Default port
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "] = " + args[i]);
		}
		if (args.length < 0) {
			System.out.println("USAGE: java client host port");
			System.exit(-1);
		}
		try { /* get input parameters */
			host = "localhost";
			port = 1337;
			//host = args[0];
			//port = Integer.parseInt(args[1]);
		} catch (IllegalArgumentException e) {
			System.out.println("USAGE: java client host port");
			System.exit(-1);
		}

		try { /* set up a key manager for client authentication */
			SSLSocketFactory factory = null;
			try {
				char[] password = "password".toCharArray();
				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				SSLContext ctx = SSLContext.getInstance("TLS");
				BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Specify a keystore ...");
				String keystore = read.readLine();
				System.out.println("Enter password for keystore ...");
				String pw = read.readLine();
				ks.load(new FileInputStream(keystore), pw.toCharArray()); // keystore
																			// password
																			// (storepass)
				ts.load(new FileInputStream("clienttruststore"), password); // truststore
																			// password
																			// (storepass);
				System.out.println("Enter password for key ...");
				pw = read.readLine();
				kmf.init(ks, pw.toCharArray()); // user password (keypass)
				tmf.init(ts); // keystore can be used as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				factory = ctx.getSocketFactory();
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
			SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
			System.out.println("\nsocket before handshake:\n" + socket + "\n");

			/*
			 * send http request
			 *
			 * See SSLSocketClient.java for more information about why there is
			 * a forced handshake here when using PrintWriters.
			 */
			socket.startHandshake();

			SSLSession session = socket.getSession();
			X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
			String subject = cert.getSubjectDN().getName();
			System.out.println(
					"certificate name (subject DN field) on certificate received from server:\n" + subject + "\n");
			System.out.println("issuer: " + cert.getIssuerDN().getName());
			System.out.println("serialno: " + cert.getSerialNumber().toString());
			System.out.println("socket after handshake:\n" + socket + "\n");
			System.out.println("secure connection established\n\n");
			BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String msg;
			
			//password implementation
			System.out.println("Enter password:");
			msg = read.readLine();
			out.println(msg);
			out.flush();
			System.out.println(in.readLine()); //auth message
			char[] inData;
			for (;;) {
				inData = new char[10000];
				System.out.print(">");
				msg = read.readLine();
				if (msg.equalsIgnoreCase("quit")) {
					break;
				}
				//System.out.print("sending '" + msg + "' to server...");
				out.println(msg);
				out.flush();
				//System.out.println("done");

				//System.out.println("received '" + in.readLine() + "' from server\n");
				int length = in.read(inData);
				String toPrint = "";
				for (int i = 0; i<length ; i++){
					toPrint += Character.toString(inData[i]);
				}
				System.out.println(toPrint);
			}
			in.close();
			out.close();
			read.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
