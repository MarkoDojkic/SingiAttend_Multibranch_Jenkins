package dev.markodojkic.singiattend.server.saml;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
public final class SamlUtils {

    private static final String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#";

    private SamlUtils() {}

    // =========================
    // PUBLIC ENTRY POINT
    // =========================
    public static boolean validate(String samlResponse) {
        try {
            if (samlResponse == null || samlResponse.trim().isEmpty()) {
                log.warn("SAML response is null or empty");
                return false;
            }

            Document doc = parse(samlResponse.getBytes(StandardCharsets.UTF_8));

            if (!verifySignature(doc, loadCert())) {
                log.warn("Invalid SAML signature");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating SAML response: {}", e.getMessage(), e);
            return false;
        }
    }

    // =========================
    // CERT LOADING (classpath)
    // =========================
    private static X509Certificate loadCert() throws Exception {
        InputStream is = SamlUtils.class.getClassLoader().getResourceAsStream("saml/crt.pem");

        if (is == null) {
            throw new IllegalStateException("Missing cert: saml/crt.pem");
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(is);
    }

    // =========================
    // SIGNATURE VERIFY
    // =========================
    private static boolean verifySignature(Document doc, X509Certificate cert) throws Exception {

        NodeList nl = doc.getElementsByTagNameNS(DSIG_NS, "Signature");

        if (nl.getLength() == 0) {
            throw new IllegalStateException("No Signature found");
        }

        Node sigNode = nl.item(0);

        // If Signature contains KeyInfo with an X509Certificate, extract it and use its public key
        java.security.PublicKey verificationKey = cert.getPublicKey();
        try {
            NodeList certNodes = ((Element) sigNode).getElementsByTagNameNS(DSIG_NS, "X509Certificate");
            if (certNodes != null && certNodes.getLength() > 0) {
                String b64 = certNodes.item(0).getTextContent().replaceAll("\\s+", "");
                try {
                    byte[] der = Base64.getDecoder().decode(b64);
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate sigCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
                    verificationKey = sigCert.getPublicKey();
                    log.debug("Using certificate from Signature KeyInfo for verification: subject={}", sigCert.getSubjectX500Principal());
                } catch (Exception e) {
                    log.debug("Unable to parse X509Certificate from Signature KeyInfo: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("No KeyInfo/X509Certificate found in Signature: {}", e.getMessage());
        }

        // Ensure ID attributes on the referenced element are marked so URI resolution works
        try {
            NodeList refs = ((Element) sigNode).getElementsByTagNameNS(DSIG_NS, "Reference");
            if (refs != null && refs.getLength() > 0) {
                String uri = ((Element) refs.item(0)).getAttribute("URI");
                if (uri != null && !uri.isEmpty()) {
                    String id = uri.startsWith("#") ? uri.substring(1) : uri;
                    Node referenced = findElementById(doc, id);
                    if (referenced != null && referenced.getNodeType() == Node.ELEMENT_NODE) {
                        Element refElem = (Element) referenced;
                        NamedNodeMap attrs = refElem.getAttributes();
                        for (int i = 0; i < attrs.getLength(); i++) {
                            Node a = attrs.item(i);
                            String an = a.getNodeName();
                            if ("ID".equals(an) || "Id".equals(an) || "id".equals(an)) {
                                try {
                                    refElem.setIdAttribute(an, true);
                                    log.debug("Marked attribute '{}' on element '{}' as ID for signature resolution", an, refElem.getNodeName());
                                } catch (Exception ignore) {
                                    log.debug("Could not mark attribute {} as ID: {}", an, ignore.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to pre-mark ID attributes for signature reference resolution: {}", e.getMessage());
        }

        DOMValidateContext ctx = new DOMValidateContext(verificationKey, sigNode);
        ctx.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);

        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = factory.unmarshalXMLSignature(ctx);

        boolean coreValidity = signature.validate(ctx);
        if (!coreValidity) {
            log.warn("Signature core validation failed");
            try {
                boolean sv = signature.getSignatureValue().validate(ctx);
                log.warn("SignatureValue validation: {}", sv);
            } catch (Exception ex) {
                log.warn("Error validating SignatureValue: {}", ex.getMessage());
            }
            try {
                @SuppressWarnings("unchecked")
                List<Reference> refs = signature.getSignedInfo().getReferences();
                for (int i = 0; i < refs.size(); i++) {
                    try {
                        boolean rv = refs.get(i).validate(ctx);
                        log.warn("Reference[{}] valid: {}", i, rv);
                    } catch (Exception ex) {
                        log.warn("Reference[{}] validation threw: {}", i, ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not inspect references: {}", ex.getMessage());
            }
        }

        return coreValidity;
    }

    // =========================
    // XML PARSING
    // =========================
    public static Document parse(byte[] xmlBytes) throws Exception {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new IllegalArgumentException("XML bytes cannot be null or empty");
        }

        log.debug("Parsing XML document with {} bytes", xmlBytes.length);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);

        // XXE Protection
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (Exception e) {
            log.warn("Unable to set XXE protection features: {}", e.getMessage());
        }

        try {
            var documentBuilder = factory.newDocumentBuilder();
            var doc = documentBuilder.parse(new ByteArrayInputStream(xmlBytes));

            if (doc == null) {
                log.error("XML parsing returned null document");
                throw new IllegalStateException("Document builder returned null");
            }

            var rootElement = doc.getDocumentElement();
            if (rootElement == null) {
                log.error("Document has no root element");
                throw new IllegalStateException("No root element in parsed document");
            }

            log.debug("Successfully parsed XML document. Root element: {} (namespace: {})",
                    rootElement.getNodeName(), rootElement.getNamespaceURI());
            return doc;
        } catch (Exception e) {
            log.error("Error parsing XML document: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find the node in the document which has an attribute ID/Id/id equal to the provided id.
     */
    public static Node findElementById(Document doc, String id) {
        if (id == null || id.isEmpty()) return null;
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            NamedNodeMap attrs = n.getAttributes();
            if (attrs == null) continue;
            Node a = attrs.getNamedItem("ID");
            if (a == null) a = attrs.getNamedItem("Id");
            if (a == null) a = attrs.getNamedItem("id");
            if (a != null && id.equals(a.getNodeValue())) return n;
        }
        return null;
    }

}
