package net.sourceforge.kolmafia.utilities;

import java.util.Iterator;
import java.util.Map;
import net.sourceforge.kolmafia.RequestLogger;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class HTMLParserUtils {
  private HTMLParserUtils() {}

  public static final HtmlCleaner configureDefaultParser() {
    HtmlCleaner cleaner = new HtmlCleaner();

    CleanerProperties props = cleaner.getProperties();
    props.setTranslateSpecialEntities(false);
    props.setRecognizeUnicodeChars(false);
    props.setOmitXmlDeclaration(true);

    return cleaner;
  }

  // Log cleaned HTML

  public static final void logHTML(final Node node) {
    if (node != null) {
      StringBuffer buffer = new StringBuffer();
      HTMLParserUtils.logHTML(node, buffer, 0);
    }
  }

  private static void logHTML(final Node node, final StringBuffer buffer, int level) {
    String name = node.nodeName();

    // Skip scripts
    if (name.equals("script")) {
      return;
    }

    HTMLParserUtils.indent(buffer, level);
    HTMLParserUtils.printTag(buffer, node);
    RequestLogger.updateDebugLog(buffer.toString());

    Iterator<Node> it = node.childNodes().iterator();
    while (it.hasNext()) {
      Node child = it.next();

      if (child instanceof Comment object) {
        String content = object.getData();
        HTMLParserUtils.indent(buffer, level + 1);
        buffer.append("<!--");
        buffer.append(content);
        buffer.append("-->");
        RequestLogger.updateDebugLog(buffer.toString());
        continue;
      }

      if (child instanceof TextNode object) {
        String content = object.text().trim();
        if (content.equals("")) {
          continue;
        }

        HTMLParserUtils.indent(buffer, level + 1);
        buffer.append(content);
        RequestLogger.updateDebugLog(buffer.toString());
        continue;
      }

      HTMLParserUtils.logHTML(child, buffer, level + 1);
    }
  }

  private static void indent(final StringBuffer buffer, int level) {
    buffer.setLength(0);
    for (int i = 0; i < level; ++i) {
      buffer.append(" ");
      buffer.append(" ");
    }
  }

  private static void printTag(final StringBuffer buffer, Node node) {
    String name = node.nodeName();
    var attributes = node.attributes();

    buffer.append("<");
    buffer.append(name);

    if (!attributes.isEmpty()) {
        for (Attribute attr : attributes) {
            buffer.append(" ");
            buffer.append(attr.getKey());
            buffer.append("=\"");
            buffer.append(attr.getValue());
            buffer.append("\"");
        }
    }
    buffer.append(">");
  }
}
