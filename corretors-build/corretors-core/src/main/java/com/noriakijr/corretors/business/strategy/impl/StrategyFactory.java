package com.noriakijr.corretors.business.strategy.impl;

import java.util.List;
import java.util.Map;

import com.noriakijr.corretors.business.strategy.IStrategy;
import com.noriakijr.corretors.model.IEntity;

public class StrategyFactory {

	private static Map<String, List<IStrategy>> mapStrategy;
	
	static {
		
	}
	
	public static List<IStrategy> getListStrategy(IEntity entity) {
		return mapStrategy.get(entity.getClass().getName());
	}
	
}
