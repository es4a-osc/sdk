@ECHO OFF

REM --------------------------------------------------------------------------
REM Simple扩展类库构造批处理脚本
REM 作者：树先生 xhwsd@qq.com
REM 版本：2026-9-20
REM --------------------------------------------------------------------------

TITLE Simple扩展类库构造

REM --------------------------------------------------------------------------

:预定义变量
ECHO [预定义变量]
REM 类库根目录（支持拖入类库文件夹或传递类库文件夹路径）
IF "%~1" == "" (
	ECHO - 请指定类库文件夹
	ECHO - 用法: %~nx0 "类库文件夹"
	GOTO 结束脚本
)
FOR %%I IN ("%~1") DO SET "LIBRARY_HOME=%%~fI"
IF NOT EXIST "%LIBRARY_HOME%\src" (
	ECHO - 未找到类库源码目录: %LIBRARY_HOME%\src
	GOTO 结束脚本
)
REM JavaSDK根目录
FOR %%I IN ("%~dp0..\tools\jdk1.8.0_503") DO SET "JAVA_HOME=%%~fI"
REM Android指定API等级目录（编译Simple运行库需要）
FOR %%I IN ("%~dp0..\tools\android\platforms\android-26") DO SET "ANDROID_LEVEL=%%~fI"
REM Simple根目录（编译器和所有运行库JAR包所在的根目录）
FOR %%I IN ("%~dp0..\simple") DO SET "SIMPLE_HOME=%%~fI"

REM 输出变量
ECHO LIBRARY_HOME=%LIBRARY_HOME%
ECHO JAVA_HOME=%JAVA_HOME%
ECHO ANDROID_LEVEL=%ANDROID_LEVEL%
ECHO SIMPLE_HOME=%SIMPLE_HOME%

REM --------------------------------------------------------------------------

:编译源文件
ECHO.
ECHO [编译源文件]
REM javac.exe 二进制控制台程序
SET "JAVAC_BINARY=%JAVA_HOME%\bin\javac"
REM 源文件编译后输出类目录
SET "CLASSES_DIR=%LIBRARY_HOME%\build\classes"
REM 检验指定目录是否存在
IF EXIST "%CLASSES_DIR%" (
	REM 删除指定目录
	RD /Q /S "%CLASSES_DIR%"
)
REM 创建目录
MD "%CLASSES_DIR%"
REM 所有依赖JAR包
SET "CLASS_PATH=%ANDROID_LEVEL%\android.jar;%SIMPLE_HOME%\SimpleAndroidRuntime.jar;%LIBRARY_HOME%\libs\*"
REM 欲要编译源文件列表
SET "SOURCE_LIST=%LIBRARY_HOME%\build\source.txt"
REM 列出指定目录及子目录的文件列表
DIR "%LIBRARY_HOME%\src\*.java" /S /B >"%SOURCE_LIST%"
REM 执行 javac.exe 编译源文件
"%JAVAC_BINARY%" -Xlint:unchecked -Xlint:deprecation -Xdiags:verbose -encoding utf-8 -d "%CLASSES_DIR%" -classpath "%CLASS_PATH%" "@%SOURCE_LIST%"
REM 检验前面 javac.exe 控制台程序返回值是否为 0
IF NOT %ERRORLEVEL% == 0 (
	ECHO - 编译源文件失败！
	GOTO 结束脚本
)
ECHO - 编译源文件完成！

REM --------------------------------------------------------------------------

:打包类文件
ECHO.
ECHO [打包类文件]
REM jar.exe 二进制控制台程序
SET "JAR_BINARY=%JAVA_HOME%\bin\jar"
REM 输出JAR包文件名
SET "JAR_FILE=%LIBRARY_HOME%\classes.jar"
REM 执行 jar.exe 打包类文件
"%JAR_BINARY%" -cf "%JAR_FILE%" -C "%CLASSES_DIR%" .
REM 检验前面 jar.exe 控制台程序返回值是否为 0
IF NOT %ERRORLEVEL% == 0 (
	ECHO - 打包类文件失败！
	GOTO 结束脚本
)
ECHO - 打包类文件完成！

REM --------------------------------------------------------------------------

:结束脚本
ECHO.
PAUSE
