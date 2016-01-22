package com.noriakijr.corretors.controller.facade.impl;

import java.util.List;

import com.noriakijr.corretors.controller.facade.IFacade;
import com.noriakijr.corretors.model.IEntity;
import com.noriakijr.corretors.persistence.IDAO;
import com.noriakijr.corretors.persistence.impl.DaoFactory;

public class Facade implements IFacade {

	private IDAO dao;
	
	public String insert(IEntity entity) {
		dao = DaoFactory.getDao(entity);
		dao.insert(entity);
		return null;
	}

	public String update(IEntity entity) {
		dao = DaoFactory.getDao(entity);
		dao.update(entity);
		return null;
	}

	public String remove(IEntity entity) {
		dao = DaoFactory.getDao(entity);
		dao.delete(entity);
		return null;
	}
	
	public List<IEntity> find(IEntity entity) {
		dao = DaoFactory.getDao(entity);
		return dao.find(entity);
	}
}
