@ECHO OFF
REM 编译指定Simple项目的应用，并安装启动它 xhwsd@qq.com 2026-9-4

REM ---接受参数---
REM Simple项目文件
SET "PROJECT_FILE=%~1"
REM 参数值无效
IF "%PROJECT_FILE%" == "" (
	ECHO.
	ECHO - 请指定要编译的 .properties 项目文件
	ECHO - 用法: %~nx0 "项目文件.properties"
	GOTO EndExecution
)
REM 文件不存在
IF NOT EXIST "%PROJECT_FILE%" (
	ECHO.
	ECHO - 未找到项目文件: %PROJECT_FILE%
	GOTO EndExecution
)

REM ---定义变量---
REM JavaSDK根目录，必须包含：
REM .\bin
REM .\lib\dt.jar
REM .\lib\tools.jar
FOR %%I IN ("%~dp0..\tools\jdk1.8.0_503") DO SET "JAVA_HOME=%%~fI"
REM AndroidSDK根目录，必须包含：
REM .\tools\lib\androidprefs.jar
REM .\tools\lib\apkbuilder.jar
REM .\tools\lib\jarutils.jar
REM .\platforms\android-%API_LEVEL%\android.jar
REM .\platforms\android-%API_LEVEL%\tools\lib\dx.jar
FOR %%I IN ("%~dp0..\tools\android") DO SET "ANDROID_HOME=%%~fI"
REM AndroidSDK的API等级
SET API_LEVEL=26
REM Simple根目录，必须包含：
REM .\SimpleAndroidRuntime.jar
REM .\SimpleCompiler.jar
FOR %%I IN ("%~dp0..\simple") DO SET "SIMPLE_HOME=%%~fI"
REM 扩展库根目录，可选；目录不存在时编译器只加载Simple运行库
FOR %%I IN ("%~dp0..\libraries") DO SET "LIBRARIES_HOME=%%~fI"

REM ---设置环境变量---
REM 设置path环境变量
SET path=%JAVA_HOME%\bin;%path%
REM 设置classpath环境变量
SET classpath=.;%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\dt.jar

REM ---调用编译器构建APK---
ECHO - 编译应用：
java -classpath "%ANDROID_HOME%\tools\lib\apkbuilder.jar;%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\lib\dx.jar;%ANDROID_HOME%\tools\lib\androidprefs.jar;%ANDROID_HOME%\tools\lib\jarutils.jar;%SIMPLE_HOME%\SimpleCompiler.jar" simple.compiler.Main "%PROJECT_FILE%"
IF NOT %ERRORLEVEL% == 0 (
	ECHO.
	ECHO - 应用编译失败。
	GOTO EndExecution
)

REM ---接受参数---
REM APK文件
SET "APK_FILE=%~2"
REM 参数值无效
IF "%APK_FILE%" == "" (
	ECHO.
	ECHO - 请指定要调试的 .apk 应用文件
	ECHO - 用法: %~nx0 "应用文件.apk"
	GOTO EndExecution
)
REM 文件不存在
IF NOT EXIST "%APK_FILE%" (
	ECHO.
	ECHO - 未找到应用文件: %APK_FILE%
	GOTO EndExecution
)

REM ---定义变量---
REM AndroidSDK根目录，必须包含：
REM .\tools\adb.exe
REM .\platforms\android-%API_LEVEL%\tools\aapt.exe
FOR %%I IN ("%~dp0..\tools\android") DO SET "ANDROID_HOME=%%~fI"
REM AndroidSDK的API等级
SET API_LEVEL=26
REM Android调试桥
SET "ADB_FILE=%ANDROID_HOME%\tools\adb.exe"
IF NOT EXIST "%ADB_FILE%" (
	ECHO.
	ECHO - 未找到SDK的ADB工具: %ADB_FILE%
	GOTO EndExecution
)
REM Android资源打包工具
SET "AAPT_FILE=%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\aapt.exe"
IF NOT EXIST "%AAPT_FILE%" (
	ECHO.
	ECHO - 未找到SDK的AAPT工具: %AAPT_FILE%
	GOTO EndExecution
)

REM ---取应用包名---
ECHO.
ECHO - 取应用包名：
SET "PACKAGE_NAME="
REM 直接从APK文件读取应用包名
FOR /F "tokens=2 delims='" %%I IN ('CALL "%AAPT_FILE%" dump badging "%APK_FILE%" 2^>NUL ^| FINDSTR /B /C:"package: name="') DO SET "PACKAGE_NAME=%%I"
IF NOT DEFINED PACKAGE_NAME (
	ECHO.
	ECHO - 未能从APK取得应用包名: %APK_FILE%
	GOTO EndExecution
)
ECHO %PACKAGE_NAME%

REM ---安装应用---
ECHO.
ECHO - 安装应用： 
"%ADB_FILE%" install -r "%APK_FILE%"
IF NOT %ERRORLEVEL% == 0 (
	ECHO.
	ECHO - 应用安装失败。
	GOTO EndExecution
)

REM ---启动应用---
ECHO.
ECHO - 启动应用：
"%ADB_FILE%" shell am start -n %PACKAGE_NAME%/simple.runtime.android.MainActivity
IF NOT %ERRORLEVEL% == 0 (
	ECHO.
	ECHO - 应用启动失败。
	GOTO EndExecution
)

REM ---结束执行---
:EndExecution
ECHO.

