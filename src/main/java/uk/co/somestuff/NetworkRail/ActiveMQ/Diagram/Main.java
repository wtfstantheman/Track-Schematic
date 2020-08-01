package uk.co.somestuff.NetworkRail.ActiveMQ.Diagram;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class Main {

    static String blockHeight = "48";
    static String blockWidth = "48";
    static String ver = "1.5";

    public static void main(String[] args) throws URISyntaxException, IOException, CsvException, TransformerException {

        if (args.length < 1) {
            System.out.println("You need to supply a CSV file and the SVG asset folder in that order. example 'file.jar /home/stan/Documents/Book.csv /home/stan/Documents/svg/'");
            System.exit(0);
        }

        File originalCSV = new File(args[0]);
        File svgFolder = new File(args[1]);

        if (args.length > 3) {
            try {
                blockHeight = String.valueOf(Integer.parseInt(args[2]));
                blockWidth = String.valueOf(Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        System.out.println("[uk.co.somestuff.NetworkRail.ActiveMQ.Diagram] SVG Map Render Engine " + ver + "");

        /** Creating the svg dom **/

        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Document doc = impl.createDocument(svgNS, "svg", null);
        Element svgRoot = doc.getDocumentElement();

        /** Opens the CSV and reads all the lines **/

        CSVReader reader = new CSVReader(new FileReader(originalCSV));

        List<String[]> allRows = reader.readAll();

        int columns = allRows.get(0).length;
        int rows = allRows.size();

        for (int i = 0; i < allRows.size(); i++) {

            int extention = 1;

            for (int ii = 0; ii < allRows.get(i).length; ii++) {

                if (!allRows.get(i)[ii].isEmpty()) {

                    /** Removes the first empty space from the csv **/
                    if (i == 0 && ii == 0) {
                        allRows.get(i)[ii] = allRows.get(i)[ii].substring(1);
                    }


                    /** Creates the base for the svg reader for reading the squares **/
                    String parser = XMLResourceDescriptor.getXMLParserClassName();
                    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);

                    /** Creates the local x and y for later use **/
                    String x = String.valueOf(ii * Integer.parseInt(blockWidth));
                    String y = String.valueOf(i * Integer.parseInt(blockHeight));

                    /** TODO: Array lists to merge components **/

                    List<String> uriElements = new ArrayList<String>();

                    if (allRows.get(i)[ii].startsWith("[")) {
                        allRows.get(i)[ii] = allRows.get(i)[ii].replace(" ", "");
                        allRows.get(i)[ii] = allRows.get(i)[ii].substring(1, allRows.get(i)[ii].length()-1);
                        String[] elements = allRows.get(i)[ii].split(",");
                        uriElements.addAll(Arrays.asList(elements));
                    } else {
                        uriElements.add(allRows.get(i)[ii]);
                    }

                    for (String u : uriElements) {

                        /** Creates the URI object from the row and creates the attribute map **/
                        URI uri = new URI(u);
                        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
                        if (u.contains("?")) {
                            String query = uri.getQuery().substring(uri.getQuery().indexOf("?") + 1);
                            String[] pairs = query.split("&");
                            for (String pair : pairs) {
                                int idx = pair.indexOf("=");
                                try {
                                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (uri.getPath().equals("track")) {
                            if (ii >= 1) {
                                if (false) {
                                    //if (allRows.get(i)[ii - 1].equals(allRows.get(i)[ii])) {
                                    /** Get the previous element and ass the width too it **/
                                    for (int iii = 0; iii < svgRoot.getChildNodes().getLength(); iii++) {

                                        Element local = (Element) svgRoot.getChildNodes().item(iii);

                                        if (local.getAttribute("topX").equals(String.valueOf(Integer.parseInt(x) - (Integer.parseInt(blockWidth) * extention))) && local.getAttribute("topY").equals(y)) {
                                            local.setAttributeNS(null, "x2", String.valueOf(Float.parseFloat(local.getAttribute("x2")) + (extention * Integer.parseInt(blockWidth))));
                                        }
                                    }
                                    extention++;
                                } else {
                                    formatElement(x, y, Paths.get(String.valueOf(svgFolder), "track.svg").toString(), f, null, doc, svgRoot);
                                }
                            }
                        } else if (uri.getPath().toLowerCase().equals("text")) {
                            if (query_pairs.containsKey("text")) {
                                Map<String, Object> localMap = new LinkedHashMap<String, Object>();
                                localMap.put(":value", query_pairs.get("text"));
                                localMap.put("style", query_pairs.containsKey("style") ? query_pairs.get("style") : "");
                                formatElement(x, y, Paths.get(String.valueOf(svgFolder), "text.svg").toString(), f, localMap, doc, svgRoot);
                            } else {
                                System.out.println("[uk.co.somestuff.NetworkRail.Diagram] Cannot create 'text' element, inefficient parameters (" + String.valueOf(ii) + ", " + String.valueOf(i) + ")");
                            }
                        } else if (uri.getPath().toLowerCase().equals("points")) {
                            if (query_pairs.containsKey("face") && query_pairs.containsKey("turnout")) {
                                Map<String, Object> local = new LinkedHashMap<String, Object>();
                                for (Map.Entry<String, String> e : query_pairs.entrySet()) {
                                    if (e.getKey().startsWith("-")) {
                                        local.put(e.getKey().substring(1), e.getValue());
                                    }
                                }
                                formatElement(x, y, Paths.get(String.valueOf(svgFolder), "points-face-" + query_pairs.get("face") + "-turnout-" + query_pairs.get("turnout") + ".svg").toString(), f, local, doc, svgRoot);
                            } else {
                                System.out.println("[uk.co.somestuff.NetworkRail.Diagram] Cannot create 'points' element, inefficient parameters (" + String.valueOf(ii) + ", " + String.valueOf(i) + ")");
                            }
                        } else if (uri.getPath().toLowerCase().equals("half")) {
                            if (query_pairs.containsKey("face") && query_pairs.containsKey("turnout")) {
                                formatElement(x, y, Paths.get(String.valueOf(svgFolder), "half-face-" + query_pairs.get("face") + "-turnout-" + query_pairs.get("turnout") + ".svg").toString(), f, null, doc, svgRoot);
                            } else {
                                System.out.println("[uk.co.somestuff.NetworkRail.Diagram] Cannot create 'half' element, inefficient parameters (" + String.valueOf(ii) + ", " + String.valueOf(i) + ")");
                            }
                        } else if (uri.getPath().toLowerCase().equals("across")) {
                            if (query_pairs.containsKey("face") && query_pairs.containsKey("turnout")) {
                                formatElement(x, y, Paths.get(String.valueOf(svgFolder), "across-face-" + query_pairs.get("face") + "-turnout-" + query_pairs.get("turnout") + ".svg").toString(), f, null, doc, svgRoot);
                            } else {
                                System.out.println("[uk.co.somestuff.NetworkRail.Diagram] Cannot create 'across' element, inefficient parameters (" + String.valueOf(ii) + ", " + String.valueOf(i) + ")");
                            }
                        } else if (uri.getPath().toLowerCase().equals("berth")) {
                            Map<String, Object> berthMap = new LinkedHashMap<String, Object>();

                            Map<String, String> berth = new LinkedHashMap<String, String>();
                            berth.put(":key", "berth");
                            berth.put("id", query_pairs.get("id"));
                            berthMap.put(":berth", berth);

                            formatElement(x, y, Paths.get(String.valueOf(svgFolder), "berth.svg").toString(), f, berthMap, doc, svgRoot);
                        } else if (uri.getPath().toLowerCase().equals("signal")) {
                            if (query_pairs.containsKey("face") && query_pairs.containsKey("id") && query_pairs.containsKey("display")) {

                                Map<String, Object> signalMap = new LinkedHashMap<String, Object>();

                                Map<String, String> signalDisplay = new LinkedHashMap<String, String>();
                                signalDisplay.put(":key", "signalDisplay");
                                signalDisplay.put(":value", query_pairs.get("display"));
                                signalMap.put(":signalDisplay", signalDisplay);

                                Map<String, String> signalAspect = new LinkedHashMap<String, String>();
                                signalAspect.put(":key", "signalAspect");
                                signalAspect.put("id", query_pairs.get("id"));
                                signalMap.put(":signalAspect", signalAspect);

                                //signalMap.put(":signalDisplay", "signalDisplay?value=" + query_pairs.get("display"));
                                //signalMap.put(":signalAspect", "signalAspect?id=" + query_pairs.get("id"));

                                formatElement(x, y, Paths.get(String.valueOf(svgFolder), "signal-face-" + query_pairs.get("face") + ".svg").toString(), f, signalMap, doc, svgRoot);

                            } else {
                                System.out.println("[uk.co.somestuff.NetworkRail.Diagram] Cannot create 'signal' element, inefficient parameters (" + String.valueOf(ii) + ", " + String.valueOf(i) + ")");
                            }
                        } else if (uri.getPath().toLowerCase().equals("platform")) {
                            if (query_pairs.containsKey("text") && query_pairs.containsKey("position")) {
                                Map<String, Object> platformMap = new LinkedHashMap<String, Object>();

                                Map<String, String> platform = new LinkedHashMap<String, String>();
                                platform.put(":key", "platform");
                                platform.put(":value", query_pairs.get("text"));
                                platformMap.put(":platform", platform);

                                formatElement(x, y, Paths.get(String.valueOf(svgFolder), "platform-position-" + query_pairs.get("position") + ".svg").toString(), f, platformMap, doc, svgRoot);
                            } else {
                                System.out.println("[uk.co.somestuff.NetworkRail.Diagram] Cannot create 'platform' element, inefficient parameters (" + String.valueOf(ii) + ", " + String.valueOf(i) + ")");
                            }
                        }
                    }
                }

            }

        }

        Element s = doc.createElement("text");
        s.setAttributeNS(null, "x", "24");
        s.setAttributeNS(null, "y", "24");
        s.setAttributeNS(null, "style", "font-family:ArialMT;");
        s.setTextContent("[uk.co.somestuff.NetworkRail.ActiveMQ.Diagram] SVG Map Render Engine " + ver);
        svgRoot.appendChild(s);

        svgRoot.setAttributeNS(null, "width", String.valueOf((columns * Integer.parseInt(blockWidth))+Integer.parseInt(blockWidth)));
        svgRoot.setAttributeNS(null, "height", String.valueOf((rows * Integer.parseInt(blockHeight))+Integer.parseInt(blockHeight)));

        /** Creates the svg document from the xml object in batik **/
        StringWriter writer = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(writer));
        String xmlString = writer.getBuffer().toString();

        BufferedWriter Bwriter = new BufferedWriter(new FileWriter(originalCSV.getParent() + "/MAP.svg"));
        Bwriter.write(xmlString);
        Bwriter.close();

        System.out.println("[uk.co.somestuff.NetworkRail.ActiveMQ.Diagram] Complete");
        System.out.println("[uk.co.somestuff.NetworkRail.ActiveMQ.Diagram] Saved at '" + originalCSV.getParent() + "/MAP.svg'");
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(new File(originalCSV.getParent() + "/MAP.svg"));
        }

    }

    public static void formatElement(String x, String y, String filePath, SAXSVGDocumentFactory f, Map<String, Object> attributes, Document doc, Element svgRoot) throws IOException, URISyntaxException {

        Document svgDoc = f.createDocument(String.valueOf(new File(filePath).toURI()));
        Element s = (Element) svgDoc.getElementsByTagName("svg").item(0);

        for (int _i = 0; _i < s.getChildNodes().getLength(); _i++) {

            if (s.getChildNodes().item(_i).getLocalName() == null) { continue; }

            Element oldElement = (Element) svgDoc.getElementsByTagName("svg").item(0).getChildNodes().item(_i);

            if (attributes != null) {
                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    if (entry.getKey().startsWith(":") && !entry.getKey().equals(":value")) {
                        if (entry.getKey().substring(1).equals(oldElement.getAttribute("key"))) {
                            /** Ah it's the right element we can add the values **/
                            for (Map.Entry<String, String> localEntry : ((LinkedHashMap<String, String>) entry.getValue()).entrySet()) {
                                if (localEntry.getKey().equals(":value")) {
                                    oldElement.setTextContent(localEntry.getValue());
                                } else if (!localEntry.getKey().equals(":key")) {
                                    oldElement.setAttributeNS(null, localEntry.getKey(), localEntry.getValue());
                                }
                            }
                        }
                    } else if (entry.getKey().equals(":value")) {
                        oldElement.setTextContent(entry.getValue().toString());
                    } else {
                        oldElement.setAttributeNS(null, entry.getKey(), entry.getValue().toString());
                    }
                }
            }

            if (s.getChildNodes().item(_i).getLocalName().equals("line")) {
                oldElement.setAttributeNS(null, "x1", String.valueOf(Float.parseFloat(oldElement.getAttribute("x1")) + Integer.parseInt(x)));
                oldElement.setAttributeNS(null, "y1", String.valueOf(Float.parseFloat(oldElement.getAttribute("y1")) + Integer.parseInt(y)));
                oldElement.setAttributeNS(null, "x2", String.valueOf(Float.parseFloat(oldElement.getAttribute("x2")) + Integer.parseInt(x)));
                oldElement.setAttributeNS(null, "y2", String.valueOf(Float.parseFloat(oldElement.getAttribute("y2")) + Integer.parseInt(y)));
            } else if (s.getChildNodes().item(_i).getLocalName().equals("text") || s.getChildNodes().item(_i).getLocalName().equals("rect")) {
                oldElement.setAttributeNS(null, "x", oldElement.hasAttribute("x") ? String.valueOf(Float.parseFloat(oldElement.getAttribute("x")) + Integer.parseInt(x)) : x);
                oldElement.setAttributeNS(null, "y", oldElement.hasAttribute("y") ? String.valueOf(Float.parseFloat(oldElement.getAttribute("y")) + Integer.parseInt(y)) : y);
            } else if (s.getChildNodes().item(_i).getLocalName().equals("circle")) {
                oldElement.setAttributeNS(null, "cx", oldElement.hasAttribute("cx") ? String.valueOf(Float.parseFloat(oldElement.getAttribute("cx")) + Integer.parseInt(x)) : x);
                oldElement.setAttributeNS(null, "cy", oldElement.hasAttribute("cy") ? String.valueOf(Float.parseFloat(oldElement.getAttribute("cy")) + Integer.parseInt(y)) : y);
            }

            oldElement.setAttributeNS(null, "topX", x);
            oldElement.setAttributeNS(null, "topY", y);
            svgRoot.appendChild(doc.adoptNode(oldElement));
        }
    }

}
