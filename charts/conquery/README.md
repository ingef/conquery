# Conquery Helm Chart

## Test with Podman
1. Generate kube objects locally
   ```bash
   helm template --dry-run  --generate-name charts/conquery/ > play.yaml
   ```

   Optionally override the [defaults](./values.yaml) with your own `values.yml` by adding `-f values.yml` to the `template` command.
2. Deploy in podman
   ```
   podman kube play --replace --publish "8200:80,8280:8080,8281:8081" play.yaml
   ```
