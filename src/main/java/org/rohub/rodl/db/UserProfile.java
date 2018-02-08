
package org.rohub.rodl.db;

/*-
 * #%L
 * ROHUB
 * %%
 * Copyright (C) 2010 - 2018 PSNC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.openrdf.rio.RDFFormat;
import org.rohub.rodl.dl.UserMetadata;
import org.rohub.rodl.vocabulary.FOAF;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * RODL user model.
 * 
 * @author piotrhol
 * 
 */
@Entity
@Table(name = "user_profiles")
public final class UserProfile extends UserMetadata implements Serializable {

    /** id. */
    private static final long serialVersionUID = -4468344863067565271L;

    /** access tokens owned by the user. */
    private List<AccessToken> tokens = new ArrayList<AccessToken>();


    /**
     * Constructor.
     */
    public UserProfile() {
        super();
    }


    /**
     * Constructor.
     * 
     * @param login
     *            login
     * @param name
     *            name
     * @param role
     *            role
     * @param uri
     *            uri
     */
    public UserProfile(String login, String name, Role role, URI uri) {
        super(login, name, role, uri);
    }


    /**
     * Constructor.
     * 
     * @param login
     *            login
     * @param name
     *            name
     * @param role
     *            role
     */
    public UserProfile(String login, String name, Role role) {
        super(login, name, role, null);
    }


    @Basic
    public URI getHomePage() {
        return super.getHomePage();
    }


    @Id
    @Column(length = 128)
    public String getLogin() {
        return super.getLogin();
    }


    @Basic
    public String getName() {
        return super.getName();
    }


    @Basic
    public Role getRole() {
        return super.getRole();
    }


    @Transient
    public URI getUri() {
        return super.getUri();
    }


    /**
     * Set URI.
     * 
     * @param uri
     *            uri as string
     */
    public void setUriString(String uri) {
        super.setUri(URI.create(uri));
    }


    @Basic
    public String getUriString() {
        return super.getUri() != null ? super.getUri().toString() : null;
    }


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", orphanRemoval = true)
    @XmlTransient
    public List<AccessToken> getTokens() {
        return tokens;
    }


    public void setTokens(List<AccessToken> tokens) {
        this.tokens = tokens;
    }


    /**
     * Get user profile as a RDF data.
     * 
     * @param format
     *            rdf format.
     * 
     * @return Input stream
     */
    public InputStream getAsInputStream(RDFFormat format) {
        OntModel userModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        Individual agent = userModel.createIndividual(getUri().toString(), FOAF.Agent);
        userModel.add(agent, FOAF.name, getName());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        userModel.write(out, format.getName().toUpperCase());
        return new ByteArrayInputStream(out.toByteArray());
    }

}
