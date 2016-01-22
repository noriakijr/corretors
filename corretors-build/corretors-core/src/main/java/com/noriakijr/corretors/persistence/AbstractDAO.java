package com.noriakijr.corretors.persistence;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.noriakijr.corretors.model.IEntity;

public abstract class AbstractDAO implements Serializable, IDAO {

	private static final long serialVersionUID = 1L;
	
	@PersistenceContext
	private EntityManager em;
	
	public void setEm(EntityManager em) {
		this.em = em;
	}

	public void insert(IEntity entity) {
		em.persist(entity);
	}

	public void delete(IEntity entity) {
		em.remove(entity);
	}

	public void update(IEntity entity) {
		em.merge(entity);
	}

	@SuppressWarnings({ "unchecked" })
	protected List<IEntity> findByQuery(String strSql, Map<String, Object> params) {
		Query query = em.createQuery(strSql);

		if (params != null) {
			populateQueryParameters(query, params);
		}

		List<IEntity> lista = query.getResultList();
		return lista;
	}

	@SuppressWarnings({ "unchecked" })
	protected List<IEntity> listTopQuery(String strSql, Map<String, Object> params,
			Integer top) {
		Query query = em.createQuery(strSql);

		if (params != null) {
			populateQueryParameters(query, params);
		}

		query.setMaxResults(top);
		List<IEntity> lista = query.getResultList();
		return lista;
	}

	@SuppressWarnings({ "unchecked" })
	protected List<IEntity> listNamedQuery(String namedQuery, Map<String, Object> params) {
		Query query = em.createNamedQuery(namedQuery);

		if (params != null) {
			populateQueryParameters(query, params);
		}

		List<IEntity> lista = query.getResultList();
		return lista;
	}

	private void populateQueryParameters(Query query, Map<String, Object> parameters) {
        for (Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }
}
