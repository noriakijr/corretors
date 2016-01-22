package com.noriakijr.corretors.persistence;

import java.util.List;

import com.noriakijr.corretors.model.IEntity;

public interface IDAO {

	public abstract void insert(IEntity entity);

	public abstract void update(IEntity entity);

	public abstract void delete(IEntity entity);

	public abstract List<IEntity> find(IEntity entity);
}
