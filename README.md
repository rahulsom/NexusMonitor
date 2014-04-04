NexusMonitor
============

Monitors nexus for new release artifacts and sends emails.

Usage
---
1. Download the sample NexusMonitorConfig.groovy and set it up to suit your needs.
2. Run 

```
java -jar nexus-monitor.jar 
```

I prefer configuring cron jobs to get this to run automatically.

```
0 0 * * * wget https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.rahulsom&a=nexus-monitor&v=1.0-SNAPSHOT&e=jar -O /root/nexus-monitor.jar
* * * * * java -jar /root/nexus-monitor.jar >> /root/NexusMonitor.log
```
