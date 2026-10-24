@echo off
setlocal
set PROJ=%~dp0
if "%PROJ:~-1%"=="\" set PROJ=%PROJ:~0,-1%

rem ---- pick a JDK: JAVA_HOME first, then the common install path ----
set JDK=
if exist "%JAVA_HOME%\bin\java.exe" set JDK=%JAVA_HOME%
if "%JDK%"=="" if exist "C:\Program Files\Java\jdk-21.0.11\bin\java.exe" set JDK=C:\Program Files\Java\jdk-21.0.11
if "%JDK%"=="" (
  echo [ERROR] No JDK found. Set JAVA_HOME to a JDK 21 or newer.
  pause
  exit /b 1
)

set CLASSES=%PROJ%\target\classes
set FATJAR=%PROJ%\target\mc-launcher.jar

rem ---- fast path: a Maven build already produced the runnable jar ----
if exist "%FATJAR%" (
  echo [1/1] Starting %FATJAR%
  cd /d "%PROJ%"
  "%JDK%\bin\java.exe" -jar "%FATJAR%"
  if errorlevel 1 pause
  exit /b 0
)

rem ---- fallback: compile with javac, then run with JavaFX on the module path ----
set M2=%USERPROFILE%\.m2\repository
set JFX=%M2%\org\openjfx
set MODPATH=%JFX%\javafx-base\21.0.6\javafx-base-21.0.6-win.jar;%JFX%\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar;%JFX%\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;%JFX%\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar
set DEPS=%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;%M2%\org\sejda\imageio\webp-imageio\0.1.6\webp-imageio-0.1.6.jar

if not exist "%CLASSES%" mkdir "%CLASSES%"

echo [1/2] Compiling sources...
dir /b /s "%PROJ%\src\main\java\com\example\demo\*.java" > "%TEMP%\mc-launcher-src.txt"
"%JDK%\bin\javac.exe" -encoding UTF-8 -nowarn -d "%CLASSES%" -cp "%DEPS%;%MODPATH%" @"%TEMP%\mc-launcher-src.txt"
if errorlevel 1 (
  echo [ERROR] Compile failed.
  pause
  exit /b 1
)

echo [2/2] Starting launcher...
cd /d "%PROJ%"
"%JDK%\bin\java.exe" --module-path "%MODPATH%" --add-modules javafx.controls,javafx.fxml -cp "%CLASSES%;%DEPS%" -Dfile.encoding=UTF-8 com.example.demo.Launcher
if errorlevel 1 pause
