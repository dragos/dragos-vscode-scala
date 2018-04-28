'use strict';

import * as path from 'path';
import * as URL from 'url';

import { workspace, ExtensionContext, window, languages, IndentAction } from 'vscode';
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

  const toolsJar = javaHome + '/lib/tools.jar';
  console.info('Adding to classpath ' + toolsJar);

  // The server is implemented in Scala
  const coursierPath = path.join(context.extensionPath, './coursier');
  console.info('Using coursier ' + coursierPath);

  console.log('Workspace location is: ' + workspace.rootPath);

  let proxyArgs = [];
  const proxySettings = workspace.getConfiguration().get('http.proxy').toString();
  if (proxySettings !== '') {
    console.log('Using proxy: ' + proxySettings);
    const proxyUrl = URL.parse(proxySettings);
    const javaProxyHttpHost = '-Dhttp.proxyHost=' + proxyUrl.hostname;
    const javaProxyHtppPort = '-Dhttp.proxyPort=' + proxyUrl.port;
    const javaProxyHttpsHost = '-Dhttps.proxyHost=' + proxyUrl.hostname;
    const javaProxyHttpsPort = '-Dhttps.proxyPort=' + proxyUrl.port;
    proxyArgs = [javaProxyHttpHost,javaProxyHtppPort,javaProxyHttpsHost,javaProxyHttpsPort];
  } else proxyArgs = [];
  const logLevel = workspace.getConfiguration().get('scalaLanguageServer.logLevel');
  let logLevelStr = '';
  if (logLevel != null) logLevelStr = logLevel.toString();

  const heapSize = workspace.getConfiguration().get('scalaLanguageServer.heapSize');
  let heapSizeStr = '-Xmx768M';
  if (heapSize != null) heapSizeStr = '-Xmx' + heapSize.toString();

  // tslint:disable-next-line:max-line-length
  const coursierArgs = ['launch', '-r', 'https://dl.bintray.com/dhpcs/maven', '-r', 'sonatype:releases', '-J', toolsJar, 'com.github.dragos:ensime-lsp_2.12:0.2.3', '-M', 'org.github.dragos.vscode.Main'];

  const javaArgs = proxyArgs.concat([
    heapSizeStr,
    '-Dvscode.workspace=' + workspace.rootPath,
    '-Dvscode.logLevel=' + logLevel,
    '-Densime.index.no.reverse.lookups=true',
    '-jar', coursierPath,
  ]).concat(coursierArgs);

  // The debug options for the server
  // tslint:disable-next-line:max-line-length
  const debugOptions = ['-Xdebug', '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000,quiet=y'];

  // If the extension is launched in debug mode then the debug server options are used
  // Otherwise the run options are used
  const serverOptions: ServerOptions = {
    run: { command: 'java', args: javaArgs },
    debug: { command: 'java', args: debugOptions.concat(javaArgs) },
  };

  // Options to control the language client
  const clientOptions: LanguageClientOptions = {
    // Register the server for plain text documents
    documentSelector: [
      { language: 'scala', scheme: 'file' },
      { language: 'scala', scheme: 'untitled' },
    ],
    synchronize: {
      // // Synchronize the setting section 'languageServerExample' to the server
      // configurationSection: 'languageServerExample',
      // Notify the server about file changes to '.clientrc files contain in the workspace
      fileEvents: workspace.createFileSystemWatcher(workspace.rootPath + '/.ensime'),
    },
  };

  // Create the language client and start the client.
  // tslint:disable-next-line:max-line-length
  const disposable = new LanguageClient('Scala Server', serverOptions, clientOptions, false).start();

  // Push the disposable to the context's subscriptions so that the
  // client can be deactivated on extension deactivation
  context.subscriptions.push(disposable);

  // Taken from the Java plugin, this configuration can't be (yet) defined in the
  //  `scala.configuration.json` file
  languages.setLanguageConfiguration('scala', {
    onEnterRules: [
      {
        // e.g. /** | */
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        afterText: /^\s*\*\/$/,
        action: { indentAction: IndentAction.IndentOutdent, appendText: ' * ' },
      },
      {
        // e.g. /** ...|
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        action: { indentAction: IndentAction.None, appendText: ' * ' },
      },
      {
        // e.g.  * ...|
        beforeText: /^(\t|(\ \ ))*\ \*(\ ([^\*]|\*(?!\/))*)?$/,
        action: { indentAction: IndentAction.None, appendText: '* ' },
      },
      {
        // e.g.  */|
        beforeText: /^(\t|(\ \ ))*\ \*\/\s*$/,
        action: { indentAction: IndentAction.None, removeText: 1 },
      },
    ],
  });
}
