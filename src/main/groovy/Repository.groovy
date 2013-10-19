import groovy.transform.CompileStatic

/**
 * Created with IntelliJ IDEA.
 * User: rahulsomasunderam
 * Date: 10/10/13
 * Time: 8:05 AM
 * To change this template use File | Settings | File Templates.
 */
@CompileStatic
class Repository {
  String name
  String feedUrl
  String username = 'admin'
  String password = 'admin123'
  String repoUrl
  List<String> recipients


  public java.lang.String toString() {
    return "Repository{" +
        "name='" + name + '\'' +
        ", feedUrl='" + feedUrl + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        ", repoUrl='" + repoUrl + '\'' +
        ", recipients=" + recipients +
        '}';
  }
}
