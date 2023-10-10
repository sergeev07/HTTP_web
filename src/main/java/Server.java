import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    public static final int DEFAULT_POOL = 64;
    private ExecutorService executor;
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


    public Server() {
        this.executor = Executors.newFixedThreadPool(DEFAULT_POOL);
    }

    public Server(int poolSize) {
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public void connectionHandling(Socket socket) {
        try (socket;
             final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");


            if (parts.length != 3) {
                socket.close();
                return;
            }

            final var method = parts[0];
            final var path = parts[1];
            var request = new Request(method, path);

            if (validPaths.contains(path)) defaultHandler(request, out);

            if (!handlers.containsKey(request.getMethod())) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            } else {
                var handlersMap = handlers.get(request.getMethod());
                if (!handlersMap.containsKey(request.getPath())) {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                } else {
                    handlersMap.get(path).handle(request, out);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) handlers.put(method, new ConcurrentHashMap<>());
        if (!handlers.get(method).containsKey(path)) handlers.get(method).put(path, handler);
    }


    public void defaultHandler(Request request, BufferedOutputStream out) {
        String path = request.getPath();

        try {
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                executor.submit(() -> connectionHandling(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}