package com.noriakijr.corretors.util;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAUtil implements Serializable {

	private static final long serialVersionUID = 1L;

	private static EntityManagerFactory factory = Persistence.createEntityManagerFactory("com.noriakijr.dbcorretors");
	
	@Produces
    @RequestScoped
    public EntityManager getEntityManager() {
        try {
            return factory.createEntityManager();
        } catch (Exception e) {
            e.getCause().getLocalizedMessage();
            return null;
        }
    }

    public void close(@Disposes EntityManager em) {
        em.close();
    }
}
