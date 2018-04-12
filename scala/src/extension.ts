'use strict';

import * as path from 'path';
import * as VSCode from 'vscode';
import * as URL from 'url';

import { workspace, ExtensionContext, window } from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions } from 'vscode-languageclient';

import { Sbt } from './sbt';
import { Requirements } from './requirements';

export async function activate(context: ExtensionContext) {

  // sbt command support
  context.subscriptions.push(new Sbt(context));

  // find JDK_HOME or JAVA_HOME
  const req = new Requirements();
  let javaHome;
  try {
    javaHome = await req.getJavaHome();
  } catch (pathNotFound) {
    window.showErrorMessage(pathNotFound);
    return;
  }

  let toolsJar = javaHome + '/lib/tools.jar';
  console.info('Adding to classpath ' + toolsJar);

  // The server is implemented in Scala
  let coursierPath = path.join(context.extensionPath, './coursier');
  console.info('Using coursier ' + coursierPath);

  console.log('Workspace location is: ' + workspace.rootPath);

  let proxyArgs = []
  let proxySettings = workspace.getConfiguration().get('http.proxy').toString();
  if(proxySettings != '') {
    console.log('Using proxy: '+proxySettings);
    let proxyUrl = URL.parse(proxySettings);
    let javaProxyHttpHost = '-Dhttp.proxyHost='+proxyUrl.hostname
    let javaProxyHtppPort = '-Dhttp.proxyPort='+proxyUrl.port
    let javaProxyHttpsHost = '-Dhttps.proxyHost='+proxyUrl.hostname
    let javaProxyHttpsPort = '-Dhttps.proxyPort='+proxyUrl.port
    proxyArgs = [javaProxyHttpHost,javaProxyHtppPort,javaProxyHttpsHost,javaProxyHttpsPort]
  } else proxyArgs = []
  let logLevel = workspace.getConfiguration().get('scalaLanguageServer.logLevel')
  let logLevelStr = ''
  if (logLevel != null) logLevelStr = logLevel.toString()

  let heapSize = workspace.getConfiguration().get('scalaLanguageServer.heapSize')
  let heapSizeStr = '-Xmx768M'
  if (heapSize != null) heapSizeStr = '-Xmx' + heapSize.toString()

  let coursierArgs = ['launch', '-r', 'https://dl.bintray.com/dhpcs/maven', '-r', 'sonatype:releases', '-J', toolsJar, 'com.github.dragos:ensime-lsp_2.12:0.2.1', '-M', 'org.github.dragos.vscode.Main'];

  let javaArgs = proxyArgs.concat([
    heapSizeStr,
    '-Dvscode.workspace=' + workspace.rootPath,
    '-Dvscode.logLevel=' + logLevel,
    '-jar', coursierPath
  ]).concat(coursierArgs);

  // The debug options for the server
  let debugOptions = ['-Xdebug', '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000,quiet=y'];

  // If the extension is launched in debug mode then the debug server options are used
  // Otherwise the run options are used
  let serverOptions: ServerOptions = {
    run: { command: 'java', args: javaArgs },
    debug: { command: 'java', args: debugOptions.concat(javaArgs) }
  };

  // Options to control the language client
  let clientOptions: LanguageClientOptions = {
    // Register the server for plain text documents
    documentSelector: [
      { language: 'scala', scheme: 'file' },
      { language: 'scala', scheme: 'untitled' }
    ],
    synchronize: {
      // // Synchronize the setting section 'languageServerExample' to the server
      // configurationSection: 'languageServerExample',
      // Notify the server about file changes to '.clientrc files contain in the workspace
      fileEvents: workspace.createFileSystemWatcher(workspace.rootPath + '/.ensime')
    }
  };

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
  });
}
