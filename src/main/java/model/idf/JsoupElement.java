package main.java.model.idf;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

public class JsoupElement extends Element{

    public JsoupElement(Tag tag, String baseUri) {
        super(tag, baseUri);
    }
    
    public JsoupElement(Tag tag, String baseUri, Attributes attr) {
        super(tag, baseUri, attr);
    }

}
