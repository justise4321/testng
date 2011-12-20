package org.testng.reporters.jq;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.collections.ListMultiMap;
import org.testng.internal.Utils;
import org.testng.reporters.Files;
import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main implements IReporter {
  private static final String C = "class";
  private static final String D = "div";
  private static final String S = "span";

  private Model m_model;
  private String m_outputDirectory;
  private List<ISuite> m_suites;

  @Override
  public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
      String outputDirectory) {
    m_suites = suites;
    m_model = new Model(suites);
    m_outputDirectory = "/Users/cedric/java/misc/jquery";

    XMLStringBuffer xsb = new XMLStringBuffer("  ");
    xsb.push(D, C, "navigator-root");
    generateNavigator(xsb);
    xsb.pop(D);

    xsb.push(D, C, "wrapper");
    xsb.push(D, "class", "main-panel-root");

    int suiteCount = 0;
    for (ISuite s : suites) {
      generateSuitePanel(s, xsb, m_model.getTag(s));
//      System.out.println("Suite:" + s.getName());
//      for (ITestResult r : model.get(s)) {
//        System.out.println("  TestResult: " + r);
//      }
    }
    generateFooPanel(xsb, "test-panel");
    xsb.pop(D); // main-panel-root
    xsb.pop(D); // wrapper

    String all;
    try {
      all = Files.readFile(new File("/Users/cedric/java/misc/jquery/head3"));
      Utils.writeFile(m_outputDirectory, "index3.html", all + xsb.toXML());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private String maybe(int count, String s, String sep) {
    return count > 0 ? count + " " + s + sep: "";
  }
  /**
   * Create the left hand navigator.
   */
  private void generateNavigator(XMLStringBuffer main) {
    for (ISuite suite : m_suites) {
      if (suite.getResults().size() == 0) {
        continue;
      }

      XMLStringBuffer header = new XMLStringBuffer(main.getCurrentIndent());

      Map<String, ISuiteResult> results = suite.getResults();
      int failed = 0;
      int skipped = 0;
      int passed = 0;
      for (ISuiteResult result : results.values()) {
        ITestContext context = result.getTestContext();
        failed += context.getFailedTests().size();
        skipped += context.getSkippedTests().size();
        passed += context.getPassedTests().size();
      }

      header.push(D, C, "suite");
      header.push(D, C, "suite-header");
      header.addOptional(S, suite.getName(), C, "suite-name");
      header.push(D, C, "stats");
      int total = failed + skipped + passed;
      String stats = String.format("%s, %s %s %s",
          pluralize(total, "method"),
          maybe(failed, "failed", ", "),
          maybe(skipped, "skipped", ", "),
          maybe(passed, "passed", ""));
      header.push("ul");

      // Method stats
      header.push("li");
      header.addOptional(S, stats, C, "method-stats");
      header.pop("li");

      // Tests
      header.push("li");
      header.addOptional(S, String.format("%s ", pluralize(results.values().size(), "test"),
          C, "test-stats"));
      header.pop("li");

      // Test methods
      header.push("li");
      header.addString("Test methods");
      // List of failed methods
      header.push("ul");
      for (ITestResult tr : m_model.getTestResults(suite)) {
        if (tr.getStatus() == ITestResult.FAILURE) {
          String testName = Model.getTestResultName(tr);
          header.push("li");
          header.addOptional("a", testName, "href", "#" + m_model.getTag(tr));
          header.pop("li");
        }
      }
      header.pop("ul");
      header.pop("li");

      header.pop("ul");
      header.pop(D); // stats

      header.pop(D); // suite-header
      header.pop(D); // suite

      main.addString(header.toXML());
    }
  }

  private void generateFooPanel(XMLStringBuffer xsb, String divName) {
    xsb.push(D, C, "panel " + divName);
    xsb.addString("FOO PANEL");
    xsb.pop(D);
  }

  private String getTag(ITestResult tr) {
    if (tr.getStatus() == ITestResult.SUCCESS) return "passed";
    else if (tr.getStatus() == ITestResult.SKIP) return "skipped";
    else return "failed";
  }

  private void generateSuitePanel(ISuite suite, XMLStringBuffer xsb, String divName) {
    xsb.push(D, C, "panel " + divName);
//    if (m_model.getSize() > 0) {
      ListMultiMap<Class, ITestResult> byClass = m_model.getFailedResultsByClass();
      for (Class c : byClass.getKeys()) {
        generateClassPanel(c, byClass.get(c), xsb);
    }
    xsb.pop(D);
  }

  private void generateClassPanel(Class c, List<ITestResult> results,XMLStringBuffer xsb) {
    xsb.push(D, C, "class-header");

    // Passed/failed icon
    xsb.addEmptyElement("img", "src", getImage("failed"));
    xsb.addOptional(S, c.getName(), C, "class-name");
    xsb.pop(D);

    xsb.push(D, C, "class-content");

    for (ITestResult tr : result) {
      generateMethod(tr, xsb);
    }
    xsb.pop(D);
  }

  private void generateMethod(ITestResult tr, XMLStringBuffer xsb) {
    long time = tr.getEndMillis() - tr.getStartMillis();
    xsb.push(D, C, "method");
    xsb.push(D, C, "method-content");
    xsb.addEmptyElement("a", "name", Model.getTestResultName(tr));
    xsb.addOptional(S, tr.getMethod().getMethodName(), C, "method-name");

    // Parameters?
    if (tr.getParameters().length > 0) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (Object p : tr.getParameters()) {
        if (!first) sb.append(", ");
        first = false;
        sb.append(p != null ? p.toString() : "<NULL>");
      }
      xsb.addOptional(S, "(" + sb.toString() + ")", C, "parameters");
    }

    // Exception?
    if (tr.getThrowable() != null) {
      StringBuilder stackTrace = new StringBuilder();
      stackTrace.append("<b>\"")
              .append(tr.getThrowable().getMessage())
              .append("\"</b>")
              .append("<br>");
      for (StackTraceElement str : tr.getThrowable().getStackTrace()) {
        stackTrace.append(str.toString()).append("<br>");
      }
      xsb.addOptional(D, stackTrace.toString() + "\n",
          C, "stack-trace");
    }

    xsb.addOptional(S, " " + Long.toString(time) + " ms", C, "method-time");
    xsb.pop(D);
    xsb.pop(D);
  }

  private String pluralize(int count, String singular) {
    return Integer.toString(count) + " "
        + (count > 1 ? (singular.endsWith("s") ? singular + "es" : singular + "s") : singular);
  }

//  private static String getStatusImage(String status) {
//    return "up.png";
//    if ("passed".equals(status)) return "down.png";
//    else return "up.png";
//  }

  private static String getImage(String tagClass) {
    return tagClass + ".png";
  }
}
