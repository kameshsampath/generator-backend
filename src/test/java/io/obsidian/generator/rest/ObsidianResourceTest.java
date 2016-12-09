package io.obsidian.generator.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.net.URI;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.jaxrs.JAXRSArchive;

import io.obsidian.generator.util.JsonBuilder;

/**
 *
 */
@RunWith(Arquillian.class)
public class ObsidianResourceTest
{
   @Deployment
   public static Archive<?> createDeployment()
   {
      JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class);
      final File[] artifacts = Maven.resolver().loadPomFromFile("pom.xml")
               .resolve("org.jboss.forge:forge-service-core")
               .withTransitivity().asFile();
      deployment.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
      deployment.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class)
               .importDirectory("target/generator/WEB-INF/addons").as(GenericArchive.class),
               "/WEB-INF/addons", Filters.include(".*"));
      deployment.addResource(ObsidianResource.class);
      deployment.addPackages(true, "io.obsidian.generator");
      deployment.addAsLibraries(artifacts);
      return deployment;
   }

   @ArquillianResource
   private URI deploymentUri;

   private Client client;
   private WebTarget webTarget;

   @Before
   public void setup()
   {
      client = ClientBuilder.newClient();
      webTarget = client.target(UriBuilder.fromUri(deploymentUri).path("forge"));
   }

   @Test
   @RunAsClient
   public void shouldRespondWithVersion()
   {
      final Response response = webTarget.path("/version").request().get();
      assertNotNull(response);
      assertEquals(200, response.getStatus());

      response.close();
   }

   @Test
   @RunAsClient
   public void shouldGoToNextStep()
   {
      final JsonObject jsonObject = new JsonBuilder().createJson(1)
               .addInput("type", "Creates a new Obsidian :: Quickstart :: Vertx - Rest")
               .addInput("named", "demo")
               .addInput("topLevelPackage", "org.demo")
               .addInput("version", "1.0.0-SNAPSHOT").build();

      final Response response = webTarget.path("/commands/obsidian-new-quickstart/validate").request()
               .post(Entity.json(jsonObject.toString()));

      final String json = response.readEntity(String.class);
      // System.out.println(json);
      JsonObject object = Json.createReader(new StringReader(json)).readObject();
      assertNotNull(object);
      assertTrue("First step should be valid", object.getJsonArray("messages").isEmpty());
   }
}