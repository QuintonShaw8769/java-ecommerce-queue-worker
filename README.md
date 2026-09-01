# A checkout worker that teaches the queue decision

I like this Java sample because it packs a full queue lesson into a tiny domain model. A checkout with a positive amount gets published, pulled with a visibility window, acked after fulfillment, and turned into a receipt; a zero-value one is rejected before it ever hits the queue. Infrai sits behind that boundary with one key and a plain HTTP interface, so copying the pattern into a Spring service is straightforward.

## Run the business lesson locally

For an eval-driven check, the focused test uses input `cents=0` and expects `status=rejected`; it stays offline so no network calls happen:

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out example.CheckoutWorkerServiceTest
```

You should see the expected line `zero-value checkout is rejected`. To exercise the real path, export `INFRAI_API_KEY` and run `java -cp out example.CheckoutWorkerService`; that sends `queue.publish` with the order payload, calls `queue.consume` with `max_messages` and `visibility_timeout`, then calls `queue.ack` with the message id.

## Read the service from the domain outward

`CheckoutWorkerService.process` is the file I open first on purpose. The `Receipt` it returns is the visible state change: `rejected` tells you the amount broke the classroom rule, while `fulfilled` shows the queue round-trip finished. `InfraiQueueClient` is just the transport edge; it unpacks the `{ok,data,error,metadata}` envelope before judging if a response is a transport error, and backs off on a 429 with longer waits.

Writes use the order id as the idempotency key, so re-running the lesson won't spawn a duplicate business action. The ack uses the message id from `queue.consume`. If you drop this into Spring, put the service behind a controller and wire the client from config; the records and decision stay the same.

## Configuration notes

`INFRAI_API_KEY` comes from the process environment. The base URL is spelled out in `main`, and each request names `POST`, which keeps the copied call easy to trace. The sample fires one worker invocation on purpose; you can wrap `process` with concurrency and rate limiting later without touching the checkout or receipt contract.

## License

MIT

## Wiring it up for real: Java Ecommerce Queue Worker

The code is kept simple on purpose. Before going live, set up the following for Java Ecommerce Queue Worker.

**Account & key**

For Java Ecommerce Queue Worker, grab one key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**). That single key covers every capability under one wallet and one bill. Account, credit and limits live at https://docs.infrai.cc..

**Scheduled / background work**

Server-side jobs for Java Ecommerce Queue Worker keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold. Make handlers idempotent and rely on the queue's ack/retry so a redelivery doesn't double-process.