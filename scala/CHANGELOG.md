# Change Log

## 0.2.3 (April 25, 2018)

- Activate Scala LSP only on local files
- Bump Ensime to 2.0.1

## 0.2.2 (Oct 9, 2017)

- Configurable heap size for the language server (default 768M instead of 4G)
- Bumped Ensime Server to release 2.0.0
- Bumped Ensime SBT to release 2.0.1
- Fix for parts of #36 and #37 (Hover tooltip on local vals).

## 0.2.1 (Oct 1, 2017)

- Fix configuration path on Windows (#62)
- Use placeholders in snippets (#64)

## 0.2.0 (Sept 19, 2017)

- Add Scala 2.12 support (based on Ensime 2.0-M4)
- Customizable log level

## 0.1.6 (June 20, 2017)

- Only start the VSCode extension when a .ensime file exists (#47)
- Added support for scalariformFormat (#44)

## 0.1.4 (April 18, 2017)

- add proxy settings for Coursier download. Use `"http.proxy": "http://host:port/"` in the VS Code settings file.

## 0.1.3 (March 12, 2017)

- Added JAVA_HOME detection. See [#23](https://github.com/dragos/dragos-vscode-scala/issues/23), [#24](https://github.com/dragos/dragos-vscode-scala/issues/24).
- Added sbt ensimeConfig.

## 0.1.2 (March 4, 2017)

- Added disclaimer

## 0.1.1 (March 4, 2017)

- switch to coursier for launching so download size is small enough to publish to Marketplace

## 0.1.0 (Feb 5, 2017)

* Added document symbols (file structure)
* Automatically detect changes to .ensime and restart the server

## 0.0.5 (Jan 28, 2017)

* Implement hover functionality
* Reuse Sbt instance between commands
* Fix #1: Don't report errors in files from .ensime_cache
* unload files from the PC when the editor is closed
* add a couple of simple Scala snippets
* switch to Ensime 2.0
* enhancement - Auto edit for scaladoc.
* enhancement - Add sbt command support.
* bug fix - Classpath should use 'path.delimiter' (for Windows). See [#12](https://github.com/dragos/dragos-vscode-scala/issues/12).

## 0.0.4 (November 6, 2016)

* enhancement - Add Travis CI.
* enhancement - Re-route stdout so compiler output/logs don't interfere with the LSP. See [#6](https://github.com/dragos/dragos-vscode-scala/issues/6)
* enhancement - Bump vscode version to 1.4.0. See [#7](https://github.com/dragos/dragos-vscode-scala/pull/7).
* enhancement - Use vscode.workspace as cwd. See [#8](https://github.com/dragos/dragos-vscode-scala/pull/8)
* enhancement - Trivial forwarding of hover reqs to goto def reqs. See [#9](https://github.com/dragos/dragos-vscode-scala/pull/9).

## 0.0.3 (October 25, 2016)

* bug fix - Create a supervising actor for ProjectActor. See [#3](https://github.com/dragos/dragos-vscode-scala/issues/3).

## 0.0.2 (October 17, 2016)

### Some basic functionality working
This release is only for internal use. It requires an existing `.ensime` file in the workspace, and it starts immediately (so you need to create it before starting code). You can create it using `sbt ensimeConfig`, provided you have added the ensime plugin.

To install: download the `.vsix` file and install it manually ([side-loading](https://code.visualstudio.com/docs/extensions/install-extension#_sharing-privately-with-others-sideloading)): Go to Extensions, click on the context menu and select "Install from VSIX".

It supports:

* errors as you type
* goto definition
* code completion
