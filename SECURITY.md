# Security policy

## Reporting a vulnerability

Do not open a public issue for a vulnerability that could expose files, passwords, paths, host capabilities, signatures, or recovery data.

Use GitHub's private **Report a vulnerability** flow when it is available for this repository. Before the repository is public, contact the maintainer through an already established private channel. Include only the minimum information needed to reproduce the issue and agree on a secure sample-transfer method before sending an archive.

Please include:

- Archive Manager version and versionCode;
- AutoJs6 version, versionCode, and Explorer Action protocol;
- Android version and device ABI;
- affected archive format and operation;
- whether encryption or multiple volumes are involved;
- a concise impact statement and reproduction outline;
- whether a synthetic, redistributable proof is available.

Never send a real password, personal archive, signing key, `sign.properties`, keystore, access token, or unredacted device path.

## Supported version

Until the first public GitHub release is published, only the latest locally verified release candidate is actively assessed. After publication, this section must be updated for every release line that receives security fixes.

## Security model

Archive Manager deliberately operates through bounded AutoJs6 host contracts:

- no broad storage or network permission;
- short-lived read descriptors and host-owned output transactions;
- opaque IDs instead of filesystem paths for sibling volumes, pending output, replacement history, and source-recovery history;
- structural path isolation that cannot be disabled by a resource-budget confirmation;
- complete read-back verification before archive creation or replacement is published;
- no-overwrite recovery and identity checks around every host mutation;
- best-effort clearing and no durable storage of passwords.

See [docs/FORMAT_CAPABILITIES.md](docs/FORMAT_CAPABILITIES.md) for the precise writable and read-only boundaries.
