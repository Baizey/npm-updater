# npm-updater

<!-- Plugin description -->
This intellij plugin features:
- auto-detection of your projects npm-registry url. Detection supports both npm and yarn
- Inspects your package.json to find discrepancies between latest version and your versions
- Keeps the ~ ^ >= style that you're using

Credit to https://github.com/unger1984/npm-dependency-checker v0.1.2 which this is originally based off of

## How to use?

Once plugin is installed, go to the package.json file. If plugin finds that newer version is available, you'll see it highlighted.
To fix that, press Cmd+Enter (Alt+Enter) or click the action indicator to the left of the caret to open the action list and choose "Replace with".
<!-- Plugin description end -->