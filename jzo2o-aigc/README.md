# jzo2o-aigc

`jzo2o-aigc` is the independent AIGC microservice skeleton. It has no AIGC MySQL schema and does not depend on order, marketing, or trade modules.

## Local startup

Use local port `11511` when supplying runtime configuration, then build and start the service:

```bat
jzo2o-aigc-startup.bat
```

The committed `application.properties` intentionally contains only safe application defaults. Supply the shared Redis and Nacos connection settings through the deployment environment or private configuration center. Do not commit service addresses, credentials, tokens, or real model keys to Git.

## Model configuration

Provide model settings outside this repository using the `jzo2o.aigc.model.*` keys:

- `provider`
- `base-url`
- `api-key`
- `model`
- `temperature`
- `max-tokens`

The service uses safe local defaults for Ollama when private configuration is absent. Production values, particularly `jzo2o.aigc.model.api-key`, must remain in the private configuration center or deployment secret store.

## Gateway migration

Configure the private gateway route to send `/aigc/**` to this service. After the route has been validated with the deployed access controls, remove the corresponding legacy gateway whitelist entry. Keep both the route configuration and whitelist operations in the private operations environment; neither belongs in this repository.
