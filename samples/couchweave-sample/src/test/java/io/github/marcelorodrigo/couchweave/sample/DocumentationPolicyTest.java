package io.github.marcelorodrigo.couchweave.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationPolicyTest {

    private static final Pattern PUBLIC_COORDINATE = Pattern.compile(
            "`(io\\.github\\.marcelorodrigo:couchweave-[a-z-]+)`"
    );

    @Test
    @DisplayName("should match documented public artifacts to deployable reactor modules")
    void shouldMatchDocumentedPublicArtifactsToDeployableReactorModules() throws Exception {
        // given
        var reactorRoot = reactorRoot();
        var documentation = Files.readString(reactorRoot.resolve("docs/compatibility.md"));

        // when
        var documentedCoordinates = documentedCoordinates(documentation);
        var deployableCoordinates = deployableCoordinates(reactorRoot);

        // then
        assertThat(documentedCoordinates).containsExactlyInAnyOrderElementsOf(deployableCoordinates);
    }

    @Test
    @DisplayName("should match documented Java and Spring Boot versions to build properties")
    void shouldMatchDocumentedJavaAndSpringBootVersionsToBuildProperties() throws Exception {
        // given
        var reactorRoot = reactorRoot();
        var documentation = Files.readString(reactorRoot.resolve("docs/compatibility.md"));
        var parentProject = parse(reactorRoot.resolve("pom.xml"));

        // when
        var javaVersion = text(parentProject, "/*[local-name()='project']/*[local-name()='properties']/*[local-name()='java.version']");
        var springBootVersion = text(parentProject, "/*[local-name()='project']/*[local-name()='properties']/*[local-name()='spring-boot.version']");
        var springBootReleaseLine = springBootVersion.substring(0, springBootVersion.lastIndexOf('.') + 1) + "x";

        // then
        assertThat(documentation)
                .contains("| Java | " + javaVersion + " minimum |")
                .contains("| Spring Boot | " + springBootReleaseLine + " | " + springBootVersion + " |");
    }

    private static Path reactorRoot() {
        var candidate = Path.of("").toAbsolutePath();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                    && Files.isRegularFile(candidate.resolve("docs/compatibility.md"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate the CouchWeave reactor root");
    }

    private static Set<String> documentedCoordinates(String documentation) {
        var coordinates = new LinkedHashSet<String>();
        var matcher = PUBLIC_COORDINATE.matcher(documentation);
        while (matcher.find()) {
            coordinates.add(matcher.group(1));
        }
        return coordinates;
    }

    private static Set<String> deployableCoordinates(Path reactorRoot) throws Exception {
        var parentProject = parse(reactorRoot.resolve("pom.xml"));
        var groupId = text(parentProject, "/*[local-name()='project']/*[local-name()='groupId']");
        var modules = nodes(parentProject, "/*[local-name()='project']/*[local-name()='modules']/*[local-name()='module']");
        var coordinates = new LinkedHashSet<String>();

        for (var index = 0; index < modules.getLength(); index++) {
            var modulePath = modules.item(index).getTextContent().trim();
            var moduleProject = parse(reactorRoot.resolve(modulePath).resolve("pom.xml"));
            var deploySkip = text(moduleProject, "/*[local-name()='project']/*[local-name()='properties']/*[local-name()='maven.deploy.skip']");
            if ("false".equals(deploySkip)) {
                var artifactId = text(moduleProject, "/*[local-name()='project']/*[local-name()='artifactId']");
                coordinates.add(groupId + ":" + artifactId);
            }
        }

        return coordinates;
    }

    private static Document parse(Path pom) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(pom.toFile());
    }

    private static String text(Document document, String expression) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expression, document).trim();
    }

    private static NodeList nodes(Document document, String expression) throws Exception {
        return (NodeList) XPathFactory.newInstance()
                .newXPath()
                .evaluate(expression, document, XPathConstants.NODESET);
    }
}
