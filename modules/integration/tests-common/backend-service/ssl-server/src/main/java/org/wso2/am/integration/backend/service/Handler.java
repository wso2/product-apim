package org.wso2.am.integration.backend.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

// Define the handler that processes incoming HTTP requests
public class Handler implements HttpHandler {
    int statusCode;
    String content;

    public Handler(int statusCode, String content){
        this.statusCode = statusCode;
        this.content = content;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Set the response headers (matching the ones from the SSL socket server)
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("X-Correlation-ID", "9f22c69b-6673-4326-8aff-0c0c097cd3c0");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "authorization,Access-Control-Allow-Origin,Content-Type,SOAPAction,apikey,testKey,Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Date", "Tue, 14 Dec 2021 08:15:17 GMT");
        exchange.getResponseHeaders().set("Connection", "Close");

        // Send response headers with status code and the length of the JSON response

        exchange.sendResponseHeaders(statusCode, content.getBytes().length);

        // Write the JSON response to the output stream
        OutputStream os = exchange.getResponseBody();
        os.write(content.getBytes());
        os.close();
    }

    public static String readFile(String fileLocation) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileLocation));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String everything = sb.toString();
            return everything;
        } finally {
            br.close();
        }
    }
}
