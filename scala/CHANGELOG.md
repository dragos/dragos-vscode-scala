# Change Log

## 0.0.5 (MMM d, yyyy)

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
