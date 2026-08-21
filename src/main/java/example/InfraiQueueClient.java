package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small boundary client: decode the business envelope before HTTP status handling. */
public final class InfraiQueueClient {
  private static final Pattern MESSAGE_ID = Pattern.compile("\\\"message_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key;
  private final String base;

  public InfraiQueueClient(String key, String base) { this.key = key; this.base = base; }

  public String publish(String queue, String payload, String idempotencyKey) throws IOException, InterruptedException {
    return post("/v1/queue/publish", "{\"queue\":" + quote(queue) + ",\"payload\":" + quote(payload)
        + ",\"idempotency_key\":" + quote(idempotencyKey) + "}");
  }

  public String consume(String queue, int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
    String response = post("/v1/queue/consume", "{\"queue\":" + quote(queue) + ",\"max_messages\":"
        + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}");
    Matcher match = MESSAGE_ID.matcher(response);
    if (!match.find()) throw new IllegalStateException("Infrai consume response has no message_id: " + response);
    return match.group(1);
  }

  public String ack(String queue, String messageId) throws IOException, InterruptedException {
    return post("/v1/queue/ack", "{\"queue\":" + quote(queue) + ",\"message_id\":" + quote(messageId) + "}");
  }

  private String post(String path, String body) throws IOException, InterruptedException {
    int attempt = 0;
    while (true) {
      HttpRequest req = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20))
          .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      String env = res.body();
      if (res.statusCode() == 429 && attempt++ < 4) {
        Thread.sleep((long) Math.pow(2, attempt) * 250);
        continue;
      }
      if (res.statusCode() >= 500) throw new IOException("Infrai transport failure: " + res.statusCode());
      if (!env.contains("\"ok\":true")) throw new IllegalStateException("Infrai request rejected: " + env);
      return env;
    }
  }

  private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}
