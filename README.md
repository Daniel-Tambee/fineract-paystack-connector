# Fineract ↔ Paystack Connector

A standalone Spring Boot service that lets an Apache Fineract deployment accept
**Paystack** payments and automatically post them as **loan repayments** or
**savings deposits**.

Fineract doesn't have a native "install a plugin" mechanism for payment
gateways — the standard, supported pattern is exactly this: a small sidecar
service that (1) talks to Paystack to create/verify charges and (2) talks back
to Fineract's REST API to record the resulting transaction. This keeps your
Fineract instance unmodified and upgrade-safe.

## How it works

```
Payer          Connector (this service)         Paystack              Fineract
  |                    |                            |                     |
  |--POST /payments/---|                            |                     |
  |     initialize     |--create PENDING record---->|                     |
  |                    |--POST /transaction/-------->|                    |
  |                    |     initialize             |                     |
  |<---authorizationUrl+access_code-------------------|                    |
  |                    |                            |                     |
  |--redirect to Paystack checkout, pays----------->|                     |
  |                    |                            |                     |
  |                    |<--webhook: charge.success---|                    |
  |                    |--verify signature (HMAC)   |                     |
  |                    |--GET /transaction/verify--->|                    |
  |                    |<--confirmed amount/status---|                    |
  |                    |--POST loan repayment or savings deposit--------->|
  |                    |<--transaction id-------------------------------- |
  |                    |--mark record SUCCESS       |                     |
```

Key design choices:

- **Webhook-first, verify-always.** The webhook only tells the connector
  *which* reference to check; it never trusts the webhook body's amount or
  status directly. It always calls Paystack's `/transaction/verify` endpoint
  server-to-server before touching Fineract. This defeats spoofed webhooks.
- **Signature verification.** Every webhook is checked against Paystack's
  `x-paystack-signature` header (HMAC-SHA512 of the raw body with your secret
  key) before it's parsed at all.
- **Idempotent reconciliation.** Paystack may send the same webhook more than
  once. Reconciliation is a no-op once a payment transaction reaches a
  terminal state (`SUCCESS`/`FAILED`).
- **Fail-safe on partial failure.** If Paystack confirms payment but posting
  to Fineract throws (e.g. Fineract is briefly down), the local record stays
  `PENDING` (not silently lost) and the error is logged for manual
  reconciliation or an automatic retry.

## Project layout

```
src/main/java/com/paystackfineract/connector/
  config/       Paystack/Fineract/connector settings + RestTemplate beans
  controller/   PaymentController (initialize/status), WebhookController
  service/      PaystackService, FineractService, PaymentReconciliationService
  model/        PaymentTransaction entity, enums
  repository/   Spring Data JPA repository
  security/     Webhook HMAC verifier, API-key filter
  exception/    Custom exceptions + a @RestControllerAdvice handler
```

## Configuration

All settings are environment-variable driven (see `application.yml`):

| Variable | Description | Default |
|---|---|---|
| `PAYSTACK_SECRET_KEY` | Your Paystack secret key (`sk_live_...` / `sk_test_...`) | — (required) |
| `PAYSTACK_PUBLIC_KEY` | Public key, informational only | — |
| `PAYSTACK_BASE_URL` | Paystack API base | `https://api.paystack.co` |
| `PAYSTACK_CALLBACK_URL` | Where Paystack redirects the payer after checkout | — |
| `FINERACT_BASE_URL` | Your Fineract API base, e.g. `https://fineract.example.com/fineract-provider/api/v1` | `https://localhost/fineract-provider/api/v1` |
| `FINERACT_USERNAME` / `FINERACT_PASSWORD` | Fineract API user (Basic Auth) — give it a role scoped only to loan/savings transaction commands | `mifos` / `password` |
| `FINERACT_TENANT_ID` | `Fineract-Platform-TenantId` header | `default` |
| `FINERACT_TRUST_SELF_SIGNED` | `true` to trust Fineract's demo self-signed cert. **Set to `false` in production** and install the real cert instead | `true` |
| `FINERACT_PAYSTACK_PAYMENT_TYPE_ID` | Fineract payment-type id to tag these transactions with (Admin → Products → Payment Types) | — |
| `CONNECTOR_API_KEY` | Shared secret required in `X-API-KEY` header on `/api/v1/payments/**`. Leave unset only for local dev | — |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `DB_DRIVER` | Override the datasource; defaults to in-memory H2 | H2 in-memory |

For production, point the datasource at Postgres, e.g.:
```
DB_URL=jdbc:postgresql://db-host:5432/paystack_connector
DB_USERNAME=connector
DB_PASSWORD=********
DB_DRIVER=org.postgresql.Driver
```

## Running it

```bash
mvn spring-boot:run
```

or build a jar:

```bash
mvn clean package
java -jar target/fineract-paystack-connector-1.0.0.jar
```

Register the webhook URL in your Paystack dashboard under
**Settings → API Keys & Webhooks**:
```
https://your-connector-host/api/v1/webhooks/paystack
```

## API

### 1. Initialize a payment

```
POST /api/v1/payments/initialize
X-API-KEY: <your connector api key>
Content-Type: application/json

{
  "email": "borrower@example.com",
  "amount": 5000.00,
  "currency": "NGN",
  "fineractClientId": 12,
  "accountType": "LOAN",
  "fineractAccountId": 34,
  "callbackUrl": "https://yourapp.com/payment/complete"
}
```

`accountType` is `"LOAN"` (posts a repayment against `fineractAccountId` =
loan id) or `"SAVINGS"` (posts a deposit against `fineractAccountId` =
savings account id).

Response:
```json
{
  "reference": "PSTK-AB12CD34EF56GH78IJ",
  "authorizationUrl": "https://checkout.paystack.com/xyz",
  "accessCode": "xyz"
}
```
Redirect the payer to `authorizationUrl` (or use it with Paystack's inline
JS/mobile SDK).

### 2. Check payment status

```
GET /api/v1/payments/{reference}
X-API-KEY: <your connector api key>
```

Returns the `PaymentTransaction` record, including `status`
(`PENDING`/`SUCCESS`/`FAILED`), `paystackTransactionId`, and
`fineractTransactionId` once posted.

### 3. Webhook (called by Paystack, not by you)

```
POST /api/v1/webhooks/paystack
x-paystack-signature: <HMAC-SHA512 of raw body>
```

## Things to adapt before going to production

- **Fineract API user permissions.** Create a dedicated Fineract user/role for
  this connector with only the loan-transaction and savings-transaction
  permissions it needs — don't reuse an admin account.
- **Transport security.** Terminate TLS in front of this connector and set
  `FINERACT_TRUST_SELF_SIGNED=false` once Fineract has a real certificate.
- **Retry/async webhook handling.** The webhook handler currently processes
  synchronously. Under load, consider pushing the reference onto a queue and
  returning `200` immediately, then reconciling asynchronously (Paystack
  retries webhooks that don't get a fast `2xx`, which this design also
  tolerates thanks to idempotent reconciliation).
- **Currency handling.** `amount` in `/initialize` is in the major unit (e.g.
  Naira) and is converted to the minor unit (kobo) automatically. Double check
  this conversion if you support currencies with different minor-unit scales.
- **Reconciliation job.** Add a scheduled task that re-verifies any
  `PENDING` transaction older than N minutes, in case a webhook is ever lost
  entirely (belt-and-suspenders alongside Paystack's own retries).
- **Observability.** `/actuator/health` is exposed; wire up real alerting on
  `PENDING` transactions that don't resolve within a reasonable window, since
  those represent money Paystack has collected but Fineract hasn't recorded.
