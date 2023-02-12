import nebula.plugin.contacts.Contact

plugins {
  id("java")
  id("groovy")
  id("application")
  id("com.github.johnrengelman.shadow").version("7.1.2")

  id("com.github.rahulsom.waena.root").version("0.6.1")
  id("com.github.rahulsom.waena.published").version("0.6.1")
}

group = "com.github.rahulsom"
description = "Notify on new releases from Sonatype Nexus"

contacts {
  validateEmails = true

  addPerson("rahulsom@noreply.github.com", closureOf<Contact> {
    moniker("Rahul Somasunderam")
    roles("owner")
    github("https://github.com/rahulsom")
  })
}

application {
  mainClass.set("com.github.rahulsom.nexusmonitor.SendEmail")
}

repositories {
  mavenCentral()
}

dependencies {
  // The production code uses the SLF4J logging API at compile time
  implementation("org.slf4j:slf4j-api:2.0.6")
  implementation("org.springframework:spring-context-support:5.3.25")
  implementation("javax.mail:mail:1.4.7")
  implementation("org.jsoup:jsoup:1.15.3")
  implementation("commons-codec:commons-codec:1.15")
  implementation("commons-io:commons-io:2.11.0")
  implementation("org.codehaus.groovy.modules.http-builder:http-builder:0.7.1")
  implementation("org.codehaus.groovy:groovy:2.5.21")
  implementation("org.codehaus.groovy:groovy-xml:2.5.21")
  implementation("org.codehaus.groovy:groovy-json:2.5.21")
  implementation("org.codehaus.groovy:groovy-templates:2.5.21")

  testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
  testImplementation("com.github.kristofa:mock-http-server:4.1")
  testImplementation("org.subethamail:subethasmtp-wiser:1.2")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
