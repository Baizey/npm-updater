# Changelog

## 1.3.0

- Added support for fix to latest if current version does not exist
- Internal refactoring

## 1.2.1 - 1.2.3

Various minor fixes, primarily updating to say supporting newer IDE versions

## 1.2.0

- Support private registries by including authentication header
- Given options of bumping patch, minor and major rather than always to latest

## 1.1.1

- Optimize package info fetching from registry, they will now be fetched once in parallel and then refetched in the
  background occasionally to ensure fresh version suggestions
- Fix quick-fixes not following changes in file

## 1.1.0

- Add support to show when packages are deprecated
- Add support for versioning with "latest"

## 1.0.0

Initial release