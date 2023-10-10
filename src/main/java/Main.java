public class Main {
    public static void main(String[] args) {
        final var server = new Server(32);
        // код инициализации сервера (из вашего предыдущего ДЗ)

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", (request, out) -> {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0" + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        });

        server.addHandler("POST", "/messages", (request, out) -> {
            var response = "This is POST request";
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0" + response.length() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.listen(9999);
    }
}
