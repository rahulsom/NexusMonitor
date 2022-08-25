package com.github.rahulsom.nexusmonitor

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine
import groovy.util.slurpersupport.GPathResult
import groovyx.net.http.HTTPBuilder
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper

import java.text.SimpleDateFormat

class SendEmail {
  private static final String Iso8601Format = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  private final File root
  private final JavaMailSender mailSender
  private final ConfigObject config

  static void main(String[] args) {
    SendEmail sendEmail = args.length == 1 ? new SendEmail(args[0]) : new SendEmail()
    sendEmail.run()
  }

  SendEmail() {
    this(null)
  }

  SendEmail(String root) {
    if (root == null) {
      this.root = new File(".")
    } else {
      this.root = new File(root)
    }
    config = new ConfigSlurper().parse(new File(this.root, 'NexusMonitorConfig.groovy').toURI().toURL())
    mailSender = initMailSender()
  }

  def run () {
    println("root     : ${root}")
    def feeds = config.nexusmonitor.feeds

    def lastRunFile = new File(root, 'lastrun.json')
    def lastRun = lastRunFile.exists() ? new JsonSlurper().parseText(lastRunFile.text) : [:]
    feeds.each { repository ->
      println "Name     : ${repository.name}"
      println "Last Run : ${lastRun[repository.name] ?: 'None'}"
      def rss = getRecentReleases(repository)
      println "Feed Date: ${rss.channel.date}"
      def lastRunTime = lastRun[repository.name] ?
        new SimpleDateFormat(Iso8601Format).parse(lastRun[repository.name] as String) :
        new Date(0)
      rss.channel.item.each {
        def itemTime = new SimpleDateFormat(Iso8601Format).parse(it.date.toString())
        if (itemTime > lastRunTime) {
          def pomUrl = it.link.toString()
          try {
            if (pomUrl.endsWith('.pom')) {
              processPom(pomUrl, repository)
            } else {
              processFile(pomUrl, repository, it.title.toString())
            }
          } catch (FileNotFoundException e) {
            println e.message
          } catch (Exception e) {
            e.printStackTrace()
          }
        }
      }
      lastRun[repository.name] = rss.channel.date.toString()
    }

    lastRunFile.text = new JsonBuilder(lastRun).toPrettyString()
    println "...done"
  }

  private void processPom(String pomUrl, Repository repository) {
    def pom = new XmlSlurper().parse(new URL(pomUrl).openStream())
    def groupId = pom.groupId.toString()
    def artifactId = pom.artifactId.toString()
    def version = pom.version.toString()
    println "    New Artifact: (Group: ${groupId}, Artifact: ${artifactId}, Version: ${version})"
    def artifact = [
        groupId: groupId,
        artifactId: artifactId,
        version: version,
        project: artifactId,
        repo: repository.name,
        packaging: pom.packaging.toString() ?: 'jar',
        repoHome: repository.repoUrl,
        classifier: ''
    ]
    artifact.with {
      fileUrl = "${repoHome}${groupId.replace('.','/')}/${artifactId}/${version}/${artifactId}-${version}.${packaging}"
    }

    notifyOfBuild(artifact, repository)
  }

  private void processFile(String fileUrl, Repository repository, String title) {
    println "    New file: $fileUrl for $title"
    def parts = title.split(':')
    def artifact = [
        fileUrl: fileUrl,
        groupId: parts[0],
        artifactId: parts[1],
        version: parts[2],
        project: parts[1],
        repo: repository.name,
        packaging: fileUrl.split(/\./)[-1],
        repoHome: repository.repoUrl,
        classifier: parts[3]
    ]

    notifyOfBuild(artifact, repository)
  }

  void notifyOfBuild(Map artifact, Repository repository) {
    def msg = mailSender.createMimeMessage()
    def helper = new MimeMessageHelper(msg)
    if (config.nexusmonitor.from) {
      if (config.nexusmonitor.from.address) {
        if (config.nexusmonitor.from.personal) {
          helper.setFrom config.nexusmonitor.from.address, config.nexusmonitor.from.personal
        } else {
          helper.setFrom config.nexusmonitor.from.address
        }
      }
    }

    repository.recipients.each {
      helper.addTo (it)
    }

    def subjectTemplate = '[$version] $project is on $repo'
    def bodyTemplate = getTemplate(repository.name)

    def engine = new SimpleTemplateEngine()

    helper.subject = engine.createTemplate(subjectTemplate).make(artifact).toString()
    def text = engine.createTemplate(bodyTemplate).make(artifact).toString()
    def newText = new CssInliner().process(text)

    helper.setText(newText, true)
    mailSender.send(msg)
    println "[${artifact.repo}] Mailing ${repository.recipients} about ${artifact.project} ${artifact.version}"
  }

  Reader getTemplate(String repoName) {
    try {
      new File("${repoName}.html").newReader()
    } catch (Exception ignored) {
      println "No template found for ${repoName}. Using default."
      this.class.classLoader.getResourceAsStream('basic.html').newReader()
    }
  }

  private JavaMailSenderImpl initMailSender() {
    new JavaMailSenderImpl(config.nexusmonitor.mail)
  }

  static GPathResult getRecentReleases(Repository repository) {
    def feedHome = repository.feedUrl
    println "feedHome : $feedHome"
    def builder = new HTTPBuilder(feedHome)
    builder.auth.basic repository.username, repository.password
    def bais = builder.get(path: 'recentlyDeployedReleaseArtifacts')
    new XmlSlurper().parse(bais)
  }

}

