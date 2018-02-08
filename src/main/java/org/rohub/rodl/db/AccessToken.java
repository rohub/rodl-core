
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

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * OAuth access token DAO.
 * 
 * @author Piotr Hołubowicz
 * 
 */
@Entity
@Table(name = "tokens")
@XmlRootElement(name = "access-token")
public class AccessToken implements Serializable {

    /** id. */
    private static final long serialVersionUID = 8724845005623981779L;

    /** token. */
    private String token;

    /** client application. */
    private OAuthClient client;

    /** token owner. */
    private UserProfile user;

    /** token creation date. */
    private Date created = new Date();

    /** token last usage date. */
    private Date lastUsed;

    private Date expireDate;
    
    /**
     * Constructor.
     */
    public AccessToken() {

    }


    /**
     * Constructor.
     * 
     * @param token
     *            token
     * @param client
     *            client application
     * @param user
     *            token owner
     */
    public AccessToken(String token, OAuthClient client, UserProfile user) {
        super();
        this.token = token;
        this.client = client;
        this.user = user;
    }


    /**
     * Constructor. The token value will be a random UUID.
     * 
     * @param client
     *            client application
     * @param user
     *            token owner
     */
    public AccessToken(OAuthClient client, UserProfile user) {
        super();
        this.token = UUID.randomUUID().toString();
        this.client = client;
        this.user = user;
    }


    @Id
    @Column(length = 64)
    @XmlElement
    public String getToken() {
        return token;
    }


    public void setToken(String token) {
        this.token = token;
    }


    @ManyToOne
    @JoinColumn(nullable = false)
    @XmlElement
    public OAuthClient getClient() {
        return client;
    }


    public void setClient(OAuthClient client) {
        this.client = client;
    }


    @ManyToOne
    @JoinColumn(name = "userId", referencedColumnName = "login", nullable = false)
    @XmlElement
    public UserProfile getUser() {
        return user;
    }


    public void setUser(UserProfile user) {
        this.user = user;
    }


    @Basic
    @XmlElement
    public Date getCreated() {
        return created;
    }


    public void setCreated(Date created) {
        this.created = created;
    }


    @Basic
    @XmlElement
    public Date getLastUsed() {
        return lastUsed;
    }


    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    @Basic
    @XmlElement
    public Date getExpireDate(){
    	return expireDate;
    }
    
    public void setExpireDate(Date expireDate){
    	this.expireDate = expireDate;
    }
    
    public boolean expired(){
    	Date expireDate = getExpireDate();
    	if(expireDate == null){
    		return false;
    	}
    	
    	Date now = new Date();
    	return expireDate.before(now);
    }

}
