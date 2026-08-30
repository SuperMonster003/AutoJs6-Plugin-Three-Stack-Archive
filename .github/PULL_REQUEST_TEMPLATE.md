## Summary

Describe the user-visible outcome and the archive formats or Explorer Action protocol layers affected.

## Safety boundary

- [ ] No broad storage or network permission was added.
- [ ] Existing files are not overwritten outside an explicit transactional replacement contract.
- [ ] Cancellation, failure, source changes, low space, and cleanup behavior are defined.
- [ ] Unsafe archive paths and special entries remain isolated.
- [ ] Passwords and opaque host identifiers are not persisted or logged.

## Compatibility

- [ ] The format matrix and user instructions remain accurate.
- [ ] New binary fixtures are synthetic, reproducible, licensed for redistribution, and recorded with producer version and SHA-256.
- [ ] Explorer Action API changes are append-only and keep older Binder transaction numbers stable.
- [ ] Ordinary AutoJs6 file-manager layout and established action styling are unchanged unless the change is plugin-specific and documented.

## Verification

- [ ] JVM tests pass.
- [ ] Android instrumentation passes on the applicable minimum and current Android versions.
- [ ] Lint and Release/R8 builds pass.
- [ ] APK signature, alignment, manifest, ABIs, size, and SHA-256 were checked when release output changed.
- [ ] Localized README and Changelog outputs were regenerated and the generator is idempotent.

## Evidence

List commands, device serial aliases, fixture names, and results. Do not include passwords, private paths, or user data.
