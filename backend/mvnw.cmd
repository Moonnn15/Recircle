@echo off
chcp 65001 >nul
if not exist "%JAVA_HOME%\bin\java.exe" (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3.2.2\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3.2.2\jbr"
    )
)
set "MAVEN_HOME=C:\Users\monalisa\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4"
"%JAVA_HOME%\bin\java.exe" -classpath "%MAVEN_HOME%\boot\plexus-classworlds-2.6.0.jar" "-Dclassworlds.conf=%MAVEN_HOME%\bin\m2.conf" "-Dmaven.home=%MAVEN_HOME%" "-Dmaven.multiModuleProjectDirectory=." org.codehaus.plexus.classworlds.launcher.Launcher %*
