package example;

public final class CheckoutWorkerServiceTest {
  public static void main(String[] args) throws Exception {
    CheckoutWorkerService service = new CheckoutWorkerService(new InfraiQueueClient("test", "http://127.0.0.1:1"));
    Receipt result = service.process(new CheckoutJob("order-test", "student-test", 0));
    if (!"rejected".equals(result.status())) throw new AssertionError(result);
    System.out.println("zero-value checkout is rejected");
  }
}
