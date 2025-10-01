# 🧾 Stripe Plugin for Kestra

The Stripe plugin allows you to interact with the [Stripe API](https://stripe.com/docs/api) directly from [Kestra](https://kestra.io).
It provides tasks for managing **customers, payments, payment methods, webhooks, and balances**.

---

## 🔐 Authentication

All tasks require a **Stripe API Key** (`sk_test_...` or `sk_live_...`).
You should store the key as a [Kestra Secret](https://kestra.io/docs/concepts/secrets) and reference it inside your task.

```yaml
apiKey: "{{ secret('STRIPE_API_KEY') }}"
```

- **Test keys** (`sk_test_...`) are used for sandbox testing.
- **Live keys** (`sk_live_...`) are used in production.
⚠️ Never hardcode keys directly into your YAML.

---

## ⚙️ Common Configuration

All Stripe tasks inherit these base properties:

| Property   | Type    | Required | Description                                                                 |
|------------|---------|----------|-----------------------------------------------------------------------------|
| `apiKey`   | string  | ✅        | Your Stripe API key.                                                        |
| `url`      | string  | ❌        | Base URL for Stripe API (defaults to `https://api.stripe.com`).             |
| `options`  | object  | ❌        | Optional HTTP configuration (timeouts, retries, proxies).                   |

---

## 📂 Available Tasks

### 1. Customers

- **CreateCustomer** → Create a new customer
- **UpdateCustomer** → Modify fields of an existing customer
- **GetCustomer** → Retrieve a customer by ID
- **DeleteCustomer** → Soft delete (deactivate) a customer
- **ListCustomers** → Paginate or filter customers

**Example: Create a Customer**

```yaml
id: create_customer
namespace: company.team

tasks:
  - id: create_customer
    type: io.kestra.plugin.stripe.customer.Create
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
    name: "John Doe"
    email: "john@example.com"
    metadata:
      plan: "pro"
      signup_source: "landing_page"
```

---

### 2. Payments

- **CreatePaymentIntent** → Initiate a payment with amount, currency, customer
- **ConfirmPaymentIntent** → Confirm a pending payment intent
- **RefundPayment** → Issue a refund (full or partial)
- **ListPaymentIntents** → Retrieve recent or filtered payment intents

**Example: Create and Confirm a PaymentIntent**

```yaml
tasks:
  - id: create_payment
    type: io.kestra.plugin.stripe.payment.CreateIntent
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
    amount: 5000          # amount in cents
    currency: "usd"
    customer: "cus_12345"

  - id: confirm_payment
    type: io.kestra.plugin.stripe.payment.ConfirmIntent
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
    paymentIntentId: "{{ outputs.create_payment.paymentIntent.id }}"
```

---

### 3. Payment Methods

- **CreatePaymentMethod** → Create a payment method (e.g., card, SEPA)
- **AttachPaymentMethod** → Attach payment method to a customer
- **DetachPaymentMethod** → Detach a payment method from a customer
- **ListPaymentMethods** → List all payment methods for a customer

**Example: Attach a Card to a Customer**

```yaml
tasks:
  - id: create_payment_method
    type: io.kestra.plugin.stripe.payment.CreateMethod
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
    type: "card"
    card:
      number: "4242424242424242"
      exp_month: 12
      exp_year: 2030
      cvc: "123"

  - id: attach_payment_method
    type: io.kestra.plugin.stripe.payment.AttachMethod
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
    customerId: "cus_12345"
    paymentMethodId: "{{ outputs.create_payment_method.paymentMethod.id }}"
```

---

### 4. Balance

- **RetrieveBalance** → Get the account’s available and pending balance.

```yaml
tasks:
  - id: get_balance
    type: io.kestra.plugin.stripe.balance.Retrieve
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
```

---

### 5. Webhooks

- **HandleWebhookEvent** → Receive and validate webhook events, trigger workflows.

```yaml
tasks:
  - id: webhook_handler
    type: io.kestra.plugin.stripe.webhook.HandleEvent
    apiKey: "{{ secret('STRIPE_API_KEY') }}"
    payload: "{{ trigger.body }}"
    signatureHeader: "{{ trigger.headers['Stripe-Signature'] }}"
    endpointSecret: "{{ secret('STRIPE_WEBHOOK_SECRET') }}"
```

---

## ✅ Best Practices

1. **Use Test Keys First**: Always test flows in Stripe’s test mode before production.
2. **Store Keys in Secrets**: Never hardcode your API keys.
3. **Chain Tasks**: You can chain tasks like creating a customer → creating payment intent → confirming payment.
4. **Error Handling**: Stripe errors are returned with full details in the `rawResponse` output.
