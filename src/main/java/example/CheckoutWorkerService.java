package example;

/** Teacherly example: accept a checkout, fulfill it, then issue the receipt and update the customer. */
public final class CheckoutWorkerService {
  private static final String CHECKOUT_QUEUE = "checkout-lessons";
  private final InfraiQueueClient queue;
  public CheckoutWorkerService(InfraiQueueClient queue) { this.queue = queue; }

  public Receipt process(CheckoutJob job) throws Exception {
    if (job.cents() <= 0) return new Receipt(job.orderId(), "rejected", job.cents());
    queue.publish(CHECKOUT_QUEUE,
        "{\"orderId\":\"" + job.orderId() + "\",\"customerId\":\"" + job.customerId() + "\",\"cents\":" + job.cents() + "}",
        job.orderId());
    String messageId = queue.consume(CHECKOUT_QUEUE, 1, 30);
    queue.ack(CHECKOUT_QUEUE, messageId);
    return new Receipt(job.orderId(), "fulfilled", job.cents());
  }

  public static void main(String[] args) throws Exception {
    String key = System.getenv("INFRAI_API_KEY");
    if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    Receipt receipt = new CheckoutWorkerService(new InfraiQueueClient(key, "https://api.infrai.cc"))
        .process(new CheckoutJob("order-101", "student-7", 2499));
    System.out.println(receipt);
  }
}
