@ECHO OFF
REM 编译指定Simple项目的应用 xhwsd@qq.com 2026-10-14

REM --------------------------------------------------

:接受参数
REM Simple项目文件
SET "PROJECT_FILE=%~1"
REM 参数值无效
IF "%PROJECT_FILE%" == "" (
	ECHO - 请指定要编译的 .properties 项目文件
	ECHO - 用法: %~nx0 "项目文件.properties"
	GOTO 结束退出
)

REM 检验项目文件
IF NOT EXIST "%PROJECT_FILE%" (
	ECHO - 未找到项目文件: "%PROJECT_FILE%"
	GOTO 结束退出
)

REM --------------------------------------------------

:定义变量
REM JavaSDK根目录：
FOR %%I IN ("%~dp0..\tools\jdk1.8.0_503") DO SET "JAVA_HOME=%%~fI"
REM AndroidSDK的API等级
SET API_LEVEL=26
REM AndroidSDK根目录，必须包含：
REM .\platforms\android-%API_LEVEL%\android.jar
REM .\platforms\android-%API_LEVEL%\tools\lib\d8.jar
FOR %%I IN ("%~dp0..\tools\android") DO SET "ANDROID_HOME=%%~fI"
REM Simple根目录，必须包含：
REM .\SimpleAndroidRuntime.jar
REM .\SimpleCompiler.jar
FOR %%I IN ("%~dp0..\simple") DO SET "SIMPLE_HOME=%%~fI"
REM 扩展库根目录，可选；目录不存在时编译器只加载Simple运行库
FOR %%I IN ("%~dp0..\libraries") DO SET "LIBRARIES_HOME=%%~fI"

REM --------------------------------------------------

:设置环境变量
REM 设置path环境变量
SET path=%JAVA_HOME%\bin;%path%
REM 设置classpath环境变量
SET classpath=.;%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\dt.jar

REM --------------------------------------------------

:编译应用
REM 通过 java.exe 使用编译器编译指定项目
"%JAVA_HOME%\bin\java" -classpath "%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\lib\d8.jar;%SIMPLE_HOME%\SimpleCompiler.jar" simple.compiler.Main "%PROJECT_FILE%"
IF NOT %ERRORLEVEL% == 0 (
	ECHO - 编译应用失败！
	GOTO 结束退出
)

REM --------------------------------------------------

:结束退出
ECHO.
REM 仅供工具调用，请勿 PAUSE 和 EXIT /B 1
