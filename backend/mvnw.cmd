@echo off
if not exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
)
"C:\Users\monalisa\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4\bin\mvn.cmd" %*
