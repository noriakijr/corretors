package com.noriakijr.corretors.controller.facade;

import java.util.List;

import com.noriakijr.corretors.model.IEntity;

public interface IFacade {

	public abstract String insert(IEntity entity);
	public abstract String update(IEntity entity);
	public abstract String remove(IEntity entity);
	public abstract List<IEntity> find(IEntity entity);
	
}
