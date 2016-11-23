/* --------------------------------------------------------------------------------------------
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 * ------------------------------------------------------------------------------------------ */
'use strict';

import * as path from 'path';
import * as VSCode from 'vscode';

import { workspace, Disposable, ExtensionContext, commands, window } from 'vscode';
import { LanguageClient, LanguageClientOptions, SettingMonitor, ServerOptions, TransportKind } from 'vscode-languageclient';

export function activate(context: ExtensionContext) {

  let toolsJar = process.env.JAVA_HOME + "/lib/tools.jar"
  console.info("Adding to classpath " + toolsJar);

  // The server is implemented in Scala
  let assemblyPath = path.join(context.extensionPath, "./server/ensimeServer-assembly-0.1.0.jar")
  console.info("Using " + assemblyPath);

  console.log("Workspace location is: " + workspace.rootPath)

  let javaArgs = ["-Dvscode.workspace=" + workspace.rootPath, "-cp", toolsJar + path.delimiter + assemblyPath, "org.github.dragos.vscode.Main"];
  // The debug options for the server
  let debugOptions = ["-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000,quiet=y"];

  // If the extension is launched in debug mode then the debug server options are used
  // Otherwise the run options are used
  let serverOptions: ServerOptions = {
    run: { command: "java", args: javaArgs },
    debug: { command: "java", args: debugOptions.concat(javaArgs) }
  }

  // Options to control the language client
  let clientOptions: LanguageClientOptions = {
    // Register the server for plain text documents
    documentSelector: ['scala'],
    // synchronize: {
    // 	// Synchronize the setting section 'languageServerExample' to the server
    // 	configurationSection: 'languageServerExample',
    // 	// Notify the server about file changes to '.clientrc files contain in the workspace
    // 	fileEvents: workspace.createFileSystemWatcher('**/.clientrc')
    // }
  }

  // Create the language client and start the client.
  let disposable = new LanguageClient('Scala Server', serverOptions, clientOptions, false).start();

  // Push the disposable to the context's subscriptions so that the
  // client can be deactivated on extension deactivation
  context.subscriptions.push(disposable);

  // Taken from the Java plugin, this configuration can't be (yet) defined in the
  //  `scala.configuration.json` file
  VSCode.languages.setLanguageConfiguration('scala', {
    onEnterRules: [
      {
        // e.g. /** | */
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        afterText: /^\s*\*\/$/,
        action: { indentAction: VSCode.IndentAction.IndentOutdent, appendText: ' * ' }
      },
      {
        // e.g. /** ...|
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        action: { indentAction: VSCode.IndentAction.None, appendText: ' * ' }
      },
      {
        // e.g.  * ...|
        beforeText: /^(\t|(\ \ ))*\ \*(\ ([^\*]|\*(?!\/))*)?$/,
        action: { indentAction: VSCode.IndentAction.None, appendText: '* ' }
      },
      {
        // e.g.  */|
        beforeText: /^(\t|(\ \ ))*\ \*\/\s*$/,
        action: { indentAction: VSCode.IndentAction.None, removeText: 1 }
      }
    ]
  })

  // sbt command support
  const runCommandInIntegratedTerminal = (args: string[], cwd: string) => {
    const terminal = window.createTerminal('sbt');
    terminal.show();
    terminal.sendText(args.join(' '));
  }

  const runSbtCommand = (args: string[], cwd?: string) => {
    workspace.saveAll().then(() => {
      if (!cwd) {
        cwd = workspace.rootPath;
      }
      args.splice(0, 0, 'sbt');
      if (typeof window.createTerminal === 'function') {
        runCommandInIntegratedTerminal(args, cwd);
      }
    });
  }

  const runSbtUpdate = () => {
    runSbtCommand(['update']);
  }

  const runSbtCompile = () => {
    runSbtCommand(['compile']);
  }

  const runSbtRun = () => {
    runSbtCommand(['run']);
  }

  const runSbtTest = () => {
    runSbtCommand(['test']);
  }

  const runSbtClean = () => {
    runSbtCommand(['clean']);
  }

  const runSbtReload = () => {
    runSbtCommand(['reload']);
  }

  const runSbtPackage = () => {
    runSbtCommand(['package']);
  }

  const registerCommands = (context: ExtensionContext) => {
    context.subscriptions.push(
      commands.registerCommand('sbt.update', runSbtUpdate),
      commands.registerCommand('sbt.compile', runSbtCompile),
      commands.registerCommand('sbt.run', runSbtRun),
      commands.registerCommand('sbt.test', runSbtTest),
      commands.registerCommand('sbt.clean', runSbtClean),
      commands.registerCommand('sbt.reload', runSbtReload),
      commands.registerCommand('sbt.package', runSbtPackage)
    );
  }

  registerCommands(context);

}
