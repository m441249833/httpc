import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;

//httpc (get|post) [-v] (-h "k:v")* [-d inline-data] [-f file] URL
//httpc help (get|post)
//get http://httpbin.org/get?course=networking&assignment=1
//get -v http://httpbin.org/get?course=networking&assignment=1
//post -h Content-Type:application/json -d '{"Assignment":1}' http://httpbin.org/post
//post -f file.txt http://httpbin.org/post

public class httpc {
	
	//used to inform the user to follow the format.
	final static String ERROR_MESSAGE= "Wrong argument format, please follow:\r\n"
			+"httpc (get|post) [-v] (-h \"k:v\")* [-d inline-data] [-f file] URL\r\n" + 
			  "httpc help (get|post)";
	
	public static void main(String[] args) {
		
		try {
			//A method to process command line arguments
			parseArgs(args);
			
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void parseArgs(String[] args) throws Exception{
		
		int n = args.length;
		
		//check the length of the args array.
		if  (n==0) {
			System.out.println("no arguments found.");
		}else {	
			// check the first argument is help.
			if (args[0].toLowerCase().equals("help")) {
				if (n==1) {
					
					System.out.println(
						"httpc is a curl-like application but supports HTTP protocol only.\r\n" + 
						"Usage:\r\n" + 
						" httpc command [arguments]\r\n" + 
						"The commands are:\r\n" + 
						" get executes a HTTP GET request and prints the response.\r\n" + 
						" post executes a HTTP POST request and prints the response.\r\n" + 
						" help prints this screen.\r\n" + 
						"Use \"httpc help [command]\" for more information about a command."		
							);			
				}else if (n==2){
					//check if the second argument is get or post, when the first argument is help.
					if (args[1].toLowerCase().equals("get")) {
						
						System.out.println("usage: httpc get [-v] [-h key:value] URL\r\n" + 
								"Get executes a HTTP GET request for a given URL.\r\n" + 
								" -v Prints the detail of the response such as protocol, status,and headers.\r\n" + 
								"-h key:value Associates headers to HTTP Request with the format 'key:value'.\r\n"
								);
					}else if (args[1].toLowerCase().equals("post")) {
						
						System.out.println(
								"usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\r\n" + 
								"Post executes a HTTP POST request for a given URL with inline data or from file.\r\n" + 
								"-v Prints the detail of the response such as protocol, status, and headers.'key:value'.\r\n" + 
								"-d string Associates an inline data to the body HTTP POST request.\r\n" + 
								"-f file Associates the content of a file to the body HTTP POST request.\r\n" + 
								"Either [-d] or [-f] can be used but not both."
								);
						
					}else {
						System.out.println("Wrong command, please enter \"get\" or \"post\".");
					}
					
				}else System.out.println(ERROR_MESSAGE);
				
				// detected the first argument is get.
			}else if (args[0].toLowerCase().equals("get")) {
				try {
					// to track the options are on or off.
					boolean verboseOption = false;
					boolean headersOption = false;
					String url = null;
					//A map is used to store headers when -h option is on. 
					Map<String,String> headers = new HashMap<String,String>();
					
					
					url = args[n-1];
					// iterate the args array, check if any of the methods is requested.
					for (int i = 0;i<n;i++) {
						if (args[i].toLowerCase().equals("-v")) {
							if (headersOption) {
								System.err.println("-h before -v error.");
								System.exit(0);
							}
							verboseOption = true;
						}
						if (args[i].toLowerCase().equals("-h")) {
							headersOption = true;
							for (int j = i+1;j<n-1;j++) {
								String keyStr = args[j].substring(0, args[j].indexOf(":"));
								String valueStr = args[j].substring(args[j].indexOf(":")+1,args[j].length());
							    headers.put(keyStr,valueStr);
							}
						}
					}
					
					if (!headersOption) headers = null;
					
					sendGet(url,verboseOption,headers);
				}catch(Exception e) {
					System.err.println(ERROR_MESSAGE);
				}		
				
				//detected the first argument is post.
			}else if (args[0].toLowerCase().equals("post")) {
				try {
					boolean verboseOption = false;
					boolean headersOption = false;
					boolean inlineOption = false;
					boolean fileOption = false;
					
					String url = null;
					Map<String,String> headers = new HashMap<String,String>();
					String inline = null;
					File file=null;
					
					
					url = args[n-1];
					for (int i = 0;i<n;i++) {
						if (args[i].toLowerCase().equals("-v")) {
							if (headersOption) {
								System.err.println("-h before -v error.");
								System.exit(0);
							}
							if (inlineOption) {
								System.err.println("-d before -v error.");
								System.exit(0);
							}
							if (fileOption) {
								System.err.println("-f before -v error.");
								System.exit(0);
							}
							verboseOption = true;
						}
						if (args[i].toLowerCase().equals("-h")) {
							if (inlineOption) {
								System.err.println("-d before -h error.");
								System.exit(0);
							}
							if (fileOption) {
								System.err.println("-f before -h error.");
								System.exit(0);
							}
							headersOption = true;
							for (int j = i+1;j<n-1;j++) {
								if ((args[j].toLowerCase().equals("-d")) || (args[j].toLowerCase().equals("-f"))) break;
								String keyStr = args[j].substring(0, args[j].indexOf(":"));
								String valueStr = args[j].substring(args[j].indexOf(":")+1,args[j].length());
							    headers.put(keyStr,valueStr);
							  
							}
						}
						if (args[i].toLowerCase().equals("-d")) {
							inlineOption = true;
							inline = args[i+1];
							
							
						}
						if (args[i].toLowerCase().equals("-f")) {
							fileOption = true;
							file = new File(args[i+1]);
							
							
						}
					}
					
					if ((inlineOption)&&(fileOption)) {
						System.err.println("Either [-d] or [-f] can be used but not both.");
						throw new Exception();
						
					}
					
					if (!headersOption) headers = null;
					
					sendPost(url,verboseOption,headers,inline,file);
				}catch(Exception e) {
					System.err.println(ERROR_MESSAGE);
				}
			}else {
				System.err.println(ERROR_MESSAGE);
			}
			
		}
	}

	public static void sendGet(String url, boolean verbose,Map<String, String> headers) throws Exception{
				
		//set up GET request and establish the connection.
		URL website = new URL(url);
		Socket socket = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(website.getHost(),80);
		socket.connect(socketAddress);
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("GET "+url+" HTTP/1.0\r\n");
		sb.append("User-Agent: Concordia-HTTP/1.0\r\n");

		
		if (headers != null) {
			for(String key:headers.keySet()) {
				sb.append(key+":"+headers.get(key)+"\r\n");
			}
		}
		sb.append("\r\n");
		
		OutputStreamWriter os = new OutputStreamWriter(socket.getOutputStream());
		BufferedWriter bf = new BufferedWriter(os);
		
		//send request
		bf.write(sb.toString());
		bf.flush();
		
    	// read response
		InputStream is = socket.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuilder content =new StringBuilder();	
		while((line = br.readLine())!=null) {
			content.append(line+"\r\n");
		}
		
		//check verbose option
		String result = content.toString();
		if (!verbose) {
			result = result.substring(result.indexOf('{'));
		}
		System.out.println(result);
						
	}
	
	public static void sendPost(String url,boolean verbose,Map<String, String> headers,String inline, File file) throws Exception {	
		URL website = new URL(url);
		Socket socket = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(website.getHost(),80);
		socket.connect(socketAddress);
		
		StringBuilder sb = new StringBuilder();		
		sb.append("POST "+url+" HTTP/1.0\r\n");	
		sb.append("User-Agent:Concordia-HTTP/1.0\r\n");
		
		if (headers != null) {
			for(String key:headers.keySet()) {
				sb.append(key+":"+headers.get(key)+"\r\n");
			}
		}
		
		//sb.append("\r\n");


		String entityBody="";
		
		// send post with inline data when it is typed
		if (inline != null) {
			entityBody+=inline;
		}
		
		// open file and send its content.
		if (file != null) {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = "";
			String fileContent="";
			while ((line = in.readLine())!=null) {
				fileContent +=line;
			}
			in.close();	
			entityBody+=fileContent;
		}
		
		String contentLength=String.valueOf(entityBody.length());
		String contentType = "text/plain";
		sb.append("Content-Length:"+contentLength+"\r\n");
		sb.append("Content-Type:"+contentType+"\r\n");
		sb.append("\r\n").append(entityBody);
		
		//System.out.println(sb.toString());
		//send request
		OutputStreamWriter os = new OutputStreamWriter(socket.getOutputStream());
		BufferedWriter bf = new BufferedWriter(os);
		bf.write(sb.toString());
		bf.flush();
		
		//read response
		InputStream is = socket.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuilder content =new StringBuilder();	
		while((line = br.readLine())!=null) {
			content.append(line+"\r\n");
		}
		
		String result = content.toString();
		
		//check verbose option
		if (!verbose) {
			result = result.substring(result.indexOf('{'));
		}
		System.out.println(result);
		
	}
	

}