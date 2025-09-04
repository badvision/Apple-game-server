@echo off
REM
REM AGS - Windows startup batch file
REM
SET AGS_HOME=%CD%\

REM You can set two variables here:
REM   1. %JAVA_HOME% - to pick a particular java to run under
REM   2. %AGS_HOME% - to say where you installed AGS
REM
REM e.g. uncomment (remove the "REM" from in front of) and
REM      customize the following SET statements.  
REM Note: They must have a trailing backslash as in the examples!
REM 
REM SET AGS_HOME=C:\src\workspace\36\ags\
REM SET MY_JAVA_HOME=C:\Progra~1\IBM\Java142\bin\

set RXTX_PATH=rxtx-2.1-7-bins-r2
set RXTX_ARCH=%RXTX_PATH%\Windows\i368-mingw32

IF "%PROCESSOR_ARCHITECTURE%"=="x86" goto add_classpath

:bit64
REM Assume 64 bit Windows if not x86
set RXTX_PATH=ch-rxtx-2.2-20081207-win-x64
set RXTX_ARCH=%RXTX_PATH%

:add_classpath
SET CWD=%CD%
if "%ADTPRO_CLASSPATH_SET%" == "1" goto start
set ADTPRO_CLASSPATH_SET=1
PATH=%PATH%;%AGS_HOME%lib\rxtx\%RXTX_ARCH%

:start
CD "%AGS_HOME%"
start /min %MY_JAVA_HOME%java -Xms128m -Xmx256m -cp "%AGS_HOME%dist\ags.jar";"%AGS_HOME%lib\rxtx\%RXTX_PATH%\RXTXcomm.jar" ags.ui.host.Main %*
CD "%CWD%"
