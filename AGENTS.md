# Kestra Stripe Plugin

## What

description = 'Stripe plugin for Kestra Exposes 15 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with Stripe, allowing orchestration of Stripe-based operations as part of data pipelines and automation workflows.

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

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
