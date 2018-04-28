import { workspace, ExtensionContext, commands, window, Terminal, Disposable } from 'vscode';

export class Sbt {

  public constructor(context: ExtensionContext) {
    // sbt command support
    let terminal: Terminal = null;
    const runCommandInIntegratedTerminal = (args: string[], cwd: string) => {
      if (terminal === null) {
        terminal = window.createTerminal('sbt');
        // start sbt
        terminal.sendText('sbt', true);
      }
      terminal.show();
      terminal.sendText(args.join(' '));
    };

    const runSbtCommand = (args: string[], cwd?: string) => {
      workspace.saveAll().then(() => {
        if (!cwd) {
          // tslint:disable-next-line:no-parameter-reassignment
          cwd = workspace.rootPath;
        }
        if (typeof window.createTerminal === 'function') {
          runCommandInIntegratedTerminal(args, cwd);
        }
      });
    };

    const runSbtUpdate = () => {
      runSbtCommand(['update']);
    };

    const runSbtCompile = () => {
      runSbtCommand(['compile']);
    };

    const runSbtRun = () => {
      runSbtCommand(['run']);
    };

    const runSbtTest = () => {
      runSbtCommand(['test']);
    };

    const runSbtClean = () => {
      runSbtCommand(['clean']);
    };

    const runSbtReload = () => {
      runSbtCommand(['reload']);
    };

    const runSbtPackage = () => {
      runSbtCommand(['package']);
    };

    const runSbtEnsimeConfig = () => {
      runSbtCommand(['ensimeConfig']);
    };
    
    const runSbtscalariformFormat = () => {
      runSbtCommand(['scalariformFormat']);
    };

    const registerCommands = (ctx: ExtensionContext) => {
      ctx.subscriptions.push(
        commands.registerCommand('sbt.update', runSbtUpdate),
        commands.registerCommand('sbt.compile', runSbtCompile),
        commands.registerCommand('sbt.run', runSbtRun),
        commands.registerCommand('sbt.test', runSbtTest),
        commands.registerCommand('sbt.clean', runSbtClean),
        commands.registerCommand('sbt.reload', runSbtReload),
        commands.registerCommand('sbt.package', runSbtPackage),
        commands.registerCommand('sbt.ensimeConfig', runSbtEnsimeConfig),
        commands.registerCommand('sbt.scalariformFormat', runSbtscalariformFormat),
      );
    };

    registerCommands(context);
  }

  public dispose() {
    // NOP
  }

}
