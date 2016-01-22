package com.noriakijr.corretors.business.strategy;

import com.noriakijr.corretors.model.IEntity;

public interface IStrategy {

	public abstract String execute(IEntity entity);
	
}
