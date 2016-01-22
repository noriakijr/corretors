package com.noriakijr.corretors.persistence.impl;

import java.util.HashMap;
import java.util.Map;

import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.Dependente;
import com.noriakijr.corretors.model.IEntity;
import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.persistence.IDAO;

public class DaoFactory {

	private static Map<String, IDAO> mapDAO = new HashMap<String, IDAO>();
	
	static {
		mapDAO.put(Titular.class.getName(), new ClienteDAO());
		mapDAO.put(Contrato.class.getName(), new ContratoDAO());
		mapDAO.put(Dependente.class.getName(), new DependenteDAO());
		mapDAO.put(Pagamento.class.getName(), new PagamentoDAO());
		mapDAO.put(Parcela.class.getName(), new ParcelaDAO());
		mapDAO.put(Plano.class.getName(), new PlanoDAO());
		mapDAO.put(PlanoHistorico.class.getName(), new PlanoHistoricoDAO());
		mapDAO.put(Vendedor.class.getName(), new VendedorDAO());
		
	}
	
	public static IDAO getDao(IEntity entity) {
		return mapDAO.get(entity.getClass().getName());
	}
	
}
