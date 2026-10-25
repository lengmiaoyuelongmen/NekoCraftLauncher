#define MyAppName "Neko Craft Launcher"
#ifndef MyAppVersion
#define MyAppVersion "1.0.0"
#endif
#define MyAppPublisher "Neko Craft"
#define MyAppExeName "NekoCraftLauncher.exe"

[Setup]
AppId={{3F2C9A1E-7B4D-4C8E-9F10-6A5B2D3C4E5F}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\NekoCraftLauncher
DefaultGroupName=Neko Craft Launcher
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\release\installer-inno
OutputBaseFilename=NekoCraftLauncher-Setup-{#MyAppVersion}-x64
SetupIconFile=..\src\main\resources\icon.ico
WizardImageFile=wizard.bmp
WizardSmallImageFile=wizard-small.bmp
WizardStyle=modern dark includetitlebar
WizardSizePercent=110
Compression=lzma2
SolidCompression=yes
UninstallDisplayIcon={app}\NekoCraftLauncher.ico
ArchitecturesAllowed=x64compatible
ShowLanguageDialog=no
LanguageDetectionMethod=uilanguage

[Languages]
Name: "chinesesimplified"; MessagesFile: "ChineseSimplified.isl"

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "附加快捷方式："

[Files]
Source: "..\release\exe\NekoCraftLauncher\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "..\src\main\resources\icon.ico"; DestDir: "{app}"; DestName: "NekoCraftLauncher.ico"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\NekoCraftLauncher.ico"; IconIndex: 0
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\NekoCraftLauncher.ico"; IconIndex: 0; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "启动 {#MyAppName}"; Flags: nowait postinstall
