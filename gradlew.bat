@echo off
setlocal

set DIR=%~dp0

if "%JAVA_HOME%"=="" (
    set "JAVACMD=java"
) else (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if not exist "%JAVACMD%" (
    echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    exit /b 1
)

if not exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
    echo ERROR: Missing Gradle wrapper JAR at %DIR%gradle\wrapper\gradle-wrapper.jar
    echo Please generate the wrapper with: gradle wrapper
    exit /b 1
)

"%JAVACMD%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
