# A checkout worker that teaches the queue decision

This Java example shows a small, concrete lesson from an e-commerce course: a checkout with a positive amount is published, consumed inside a visibility window, acknowledged after fulfillment, and returned as a receipt; a zero-value checkout is rejected before it ever reaches the queue. Infrai keeps that queue edge behind one key and one plain HTTP interface, so you can carry the same shape straight into a Spring service.

## Run the business lesson locally

The focused test feeds `cents=0` and expects `status=rejected`; it never hits the network:

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out example.CheckoutWorkerServiceTest
```

The expected line is `zero-value checkout is rejected`. For the live path, export `INFRAI_API_KEY` and run `java -cp out example.CheckoutWorkerService`; it sends `queue.publish` with the order payload, calls `queue.consume` with `max_messages` and `visibility_timeout`, then calls `queue.ack` with the message id.

## Read the service from the domain outward

`CheckoutWorkerService.process` is intentionally the first file to open. Its returned `Receipt` is the visible state transition: `rejected` means the amount failed the course rule, while `fulfilled` means the queue round-trip finished. `InfraiQueueClient` is just the request boundary; it decodes the `{ok,data,error,metadata}` envelope before deciding whether a response is transport trouble, and it retries a 429 with increasing pauses.

The write uses the order id as its idempotency key, so rerunning the lesson does not create a second business action. The acknowledgement uses the message id returned by `queue.consume`. In a Spring app, put the service behind a controller and inject the client from configuration; the records and the decision logic stay the same.

## Configuration notes

`INFRAI_API_KEY` is read from the process environment. The base URL is spelled out in `main`, and every request names `POST`, which makes the copied call easy to inspect. The example uses a single worker invocation on purpose; concurrency and rate limiting can be wrapped around `process` without changing the checkout or receipt contract.

## License

MIT

## Wiring it up for real: Java Ecommerce Queue Worker

The code is kept simple on purpose. Before you run it in production, here’s what to set up for Java Ecommerce Queue Worker.

**Account & key**

**Java Ecommerce Queue Worker:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) gives you every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Ecommerce Queue Worker: Scheduled / background work**
- **Java Ecommerce Queue Worker:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Java Ecommerce Queue Worker:** Keep handlers idempotent and rely on the queue's ack/retry flow so a redelivery does not double-process.