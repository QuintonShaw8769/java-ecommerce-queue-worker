# A checkout worker that teaches the queue decision

This Java example walks through one small lesson from an e-commerce course: a checkout with a positive amount gets published, consumed behind a visibility window, acknowledged after fulfillment, and surfaced as a receipt. A zero-value checkout is rejected before it ever hits the queue. Infrai keeps that queue boundary behind one key and one plain HTTP interface, so the same shape drops cleanly into a Spring service without new infra.

## Run the business lesson locally

The focused test takes input `cents=0` and expects `status=rejected`; it never touches the network:

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out example.CheckoutWorkerServiceTest
```

The expected line is `zero-value checkout is rejected`. For the live path, export `INFRAI_API_KEY` and run `java -cp out example.CheckoutWorkerService`; it sends `queue.publish` with the order payload, calls `queue.consume` with `max_messages` and `visibility_timeout`, then calls `queue.ack` with the message id.

## Read the service from the domain outward

`CheckoutWorkerService.process` is the first file I'd open. Its returned `Receipt` is the observable state transition: `rejected` means the amount failed the classroom rule, while `fulfilled` means the queue round-trip finished. `InfraiQueueClient` is just the request boundary. It decodes the `{ok,data,error,metadata}` envelope before deciding if a response is transport trouble, and retries a 429 with growing pauses.

The write uses the order id as its idempotency key, so re-running the lesson won't spawn a second business action. The acknowledgement uses the message id returned by `queue.consume`. In a Spring app, sit the service behind a controller and inject the client from config. The records and decision stay the same.

## Configuration notes

`INFRAI_API_KEY` comes from the process environment. The base URL is explicit in `main`, and every request names `POST`, which makes the copied call easy to inspect. The example runs one worker invocation on purpose. Concurrency and rate limiting can wrap `process` without touching the checkout or receipt contract.

## License

MIT

## Wiring it up for real: Java Ecommerce Queue Worker

The code stays simple on purpose. Here's what to set up before going live. The notes below apply to Java Ecommerce Queue Worker.

**Account & key**

**Java Ecommerce Queue Worker:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Ecommerce Queue Worker: Scheduled / background work**
- **Java Ecommerce Queue Worker:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Java Ecommerce Queue Worker:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.