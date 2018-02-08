package org.rohub.rodl.integration.rosrs;


import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.rohub.rodl.integration.AbstractIntegrationTest;
import org.rohub.rodl.integration.IntegrationTest;
import org.rohub.rodl.vocabulary.AccessControlService;
import org.rohub.rodl.vocabulary.NotificationService;
import org.rohub.rodl.vocabulary.ROEVOService;

import com.damnhandy.uri.template.UriTemplate;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;

@Category(IntegrationTest.class)
public class RootResourceTest extends AbstractIntegrationTest {

	@Test
	public void testNotificationServiceDescription() {
		Model model = ModelFactory.createDefaultModel();
		model.read(resource().getURI().toString());
		RDFNode val = model.getProperty(model.createResource(resource().getURI().toString()),
				NotificationService.notifications).getObject();
		UriTemplate uriTemplate = UriTemplate.fromTemplate(val.toString());
		uriTemplate.set("ro", "ro-value");
		uriTemplate.set("from", "from-value");
		uriTemplate.set("to", "to-value");
		uriTemplate.set("source", "source-value");
		uriTemplate.set("limit", "limit-value");
		Assert.assertTrue(uriTemplate.getValues().containsKey("ro"));
		Assert.assertTrue(uriTemplate.getValues().containsKey("from"));
		Assert.assertTrue(uriTemplate.getValues().containsKey("to"));
		Assert.assertTrue(uriTemplate.getValues().containsKey("source"));
		Assert.assertTrue(uriTemplate.getValues().containsKey("limit"));
		Assert.assertFalse(uriTemplate.getValues().containsKey("any-other"));
	}

	@Test
	public void testAccessControlDescription() {
		Model model = ModelFactory.createDefaultModel();
		model.read(resource().getURI().toString());
		RDFNode val = model.getProperty(model.createResource(resource().getURI().toString()),
				AccessControlService.permissions).getObject();
		Assert.assertNotNull(val);
		val = model.getProperty(model.createResource(resource().getURI().toString()),
				AccessControlService.modes).getObject();
		Assert.assertNotNull(val);
	}

	@Test
	public void testEvolutionServiceDescription() {
		Model model = ModelFactory.createDefaultModel();
		model.read(resource().getURI().toString());
		RDFNode val = model.getProperty(model.createResource(resource().getURI().toString()),
				ROEVOService.info).getObject();
		UriTemplate uriTemplate = UriTemplate.fromTemplate(val.toString());
		uriTemplate.set("ro", "ro-value");

		Assert.assertFalse(uriTemplate.getValues().containsKey("any-other"));
	}
}
