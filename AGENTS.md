# Kestra Stripe Plugin

## What

- Provides plugin components under `io.kestra.plugin.stripe`.
- Includes classes such as `ConfirmIntent`, `CreateMethod`, `ListIntents`, `DetachMethod`.

## Why

- This plugin integrates Kestra with Stripe Balance.
- It provides tasks that fetch Stripe balances and payouts.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `stripe`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.stripe.balance.Retrieve`
- `io.kestra.plugin.stripe.customer.Create`
- `io.kestra.plugin.stripe.customer.Delete`
- `io.kestra.plugin.stripe.customer.Get`
- `io.kestra.plugin.stripe.customer.List`
- `io.kestra.plugin.stripe.customer.Update`
- `io.kestra.plugin.stripe.payment.AttachMethod`
- `io.kestra.plugin.stripe.payment.ConfirmIntent`
- `io.kestra.plugin.stripe.payment.CreateIntent`
- `io.kestra.plugin.stripe.payment.CreateMethod`
- `io.kestra.plugin.stripe.payment.DetachMethod`
- `io.kestra.plugin.stripe.payment.ListIntents`
- `io.kestra.plugin.stripe.payment.ListMethods`
- `io.kestra.plugin.stripe.payment.Refund`
- `io.kestra.plugin.stripe.webhook.HandleEvent`

### Project Structure

```
plugin-stripe/
├── src/main/java/io/kestra/plugin/stripe/webhook/
├── src/test/java/io/kestra/plugin/stripe/webhook/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
