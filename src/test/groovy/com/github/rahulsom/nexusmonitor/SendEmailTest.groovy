package com.github.rahulsom.nexusmonitor

import com.github.kristofa.test.http.Method
import com.github.kristofa.test.http.MockHttpServer
import com.github.kristofa.test.http.SimpleHttpResponseProvider
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.subethamail.wiser.Wiser
import spock.lang.Specification

import java.nio.file.Files

class SendEmailTest extends Specification {

  void "no config blows up"() {
    given:
    def tempDir = Files.createTempDirectory("nexusmonitor").toFile()

    when:
    new SendEmail(tempDir.getAbsolutePath()).run()

    then:
    thrown(FileNotFoundException)
  }

  String createConfig(int mailPort, int httpPort) {
    println "mailPort: ${mailPort}, httpPort: ${httpPort}"
    // language=groovy
    """
        import com.github.rahulsom.nexusmonitor.Repository

        def globalRecipients = [
            'somebody@example.com'
        ]

        nexusmonitor.feeds = [
            new Repository(
                name: 'repo1',
                feedUrl: 'http://localhost:${httpPort}/nexus/service/local/feeds/',
                repoUrl: 'http://localhost:${httpPort}/nexus/content/repositories/public/',
                recipients: globalRecipients + []
            )
        ]

        nexusmonitor {
          from {
            address = 'nexus@example.com'
            personal = 'Nexus'
          }
          mail {
            host = 'localhost'
            port = $mailPort
          }
        }
        """.toString()
  }

  int randomPort() {
    ServerSocket s = new ServerSocket(0)
    def port = s.getLocalPort()
    s.close()
    println "port: ${port}"
    return port
  }

  void "test change found on new instance"() {
    given:
    def responseProvider = new SimpleHttpResponseProvider()
    def server = new MockHttpServer(randomPort(), responseProvider)
    server.start()
    def wiser = new Wiser()
    int wiserPort = randomPort()
    wiser.port = wiserPort
    wiser.start()
    def tempDir = Files.createTempDirectory("nexusmonitor").toFile()
    new File(tempDir, 'NexusMonitorConfig.groovy').text = createConfig(wiserPort, server.port)

    when:
    responseProvider.expect(Method.GET, "/nexus/service/local/feeds/recentlyDeployedReleaseArtifacts")
        .respondWith(200, "application/rss",
            // language=xml
            """\
            <rss>
              <channel>
                <date>2022-10-03T12:34:56Z</date>
                <item>
                  <title>com.foo:bar:1.2:jar</title>
                  <link>http://localhost:8081/nexus/content/repositories/public/com/foo/bar/1.2/bar-1.2.jar</link>
                  <description>Nexus Repository</description>
                  <date>2022-10-03T12:34:56Z</date>
                </item>
              </channel>
            </rss>""")
    new SendEmail(tempDir.getAbsolutePath()).run()

    then:
    server.verify()
    wiser.messages.size() == 1
    wiser.messages[0].envelopeReceiver == 'somebody@example.com'
    wiser.messages[0].envelopeSender == 'nexus@example.com'
    wiser.messages[0].mimeMessage.getContent().toString().contains('http://localhost:8081/nexus/content/repositories/public/com/foo/bar/1.2/bar-1.2.jar')
    new JsonSlurper().parse(new File(tempDir, 'lastrun.json')) == ["repo1": "2022-10-03T12:34:56Z"]

    cleanup:
    server.stop()
    wiser.stop()
  }

  void "test change found on existing instance"() {
    given:
    def responseProvider = new SimpleHttpResponseProvider()
    def server = new MockHttpServer(randomPort(), responseProvider)
    server.start()
    def wiser = new Wiser()
    int wiserPort = randomPort()
    wiser.port = wiserPort
    wiser.start()
    def tempDir = Files.createTempDirectory("nexusmonitor").toFile()
    new File(tempDir, 'NexusMonitorConfig.groovy').text = createConfig(wiserPort, server.port)
    new File(tempDir, 'lastrun.json').text = new JsonBuilder(["repo1": "2022-10-03T12:34:56Z"]).toPrettyString()

    when:
    responseProvider.expect(Method.GET, "/nexus/service/local/feeds/recentlyDeployedReleaseArtifacts")
        .respondWith(200, "application/rss",
            // language=xml
            """\
            <rss>
              <channel>
                <date>2022-10-04T12:34:56Z</date>
                <item>
                  <title>com.foo:bar:1.3:jar</title>
                  <link>http://localhost:8081/nexus/content/repositories/public/com/foo/bar/1.3/bar-1.3.jar</link>
                  <description>Nexus Repository</description>
                  <date>2022-10-04T12:34:56Z</date>
                </item>
                <item>
                  <title>com.foo:bar:1.2:jar</title>
                  <link>http://localhost:8081/nexus/content/repositories/public/com/foo/bar/1.2/bar-1.2.jar</link>
                  <description>Nexus Repository</description>
                  <date>2022-10-03T12:34:56Z</date>
                </item>
              </channel>
            </rss>""")
    new SendEmail(tempDir.getAbsolutePath()).run()

    then:
    server.verify()
    wiser.messages.size() == 1
    wiser.messages[0].envelopeReceiver == 'somebody@example.com'
    wiser.messages[0].envelopeSender == 'nexus@example.com'
    wiser.messages[0].mimeMessage.getContent().toString().contains('http://localhost:8081/nexus/content/repositories/public/com/foo/bar/1.3/bar-1.3.jar')
    new JsonSlurper().parse(new File(tempDir, 'lastrun.json')) == ["repo1": "2022-10-04T12:34:56Z"]

    cleanup:
    server.stop()
    wiser.stop()
  }
}
