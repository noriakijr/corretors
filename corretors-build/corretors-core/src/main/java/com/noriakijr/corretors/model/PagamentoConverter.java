package com.noriakijr.corretors.model;

import static com.noriakijr.corretors.model.PagamentoType.CARTAO_CREDITO;
import static com.noriakijr.corretors.model.PagamentoType.CARTAO_DEBITO;
import static com.noriakijr.corretors.model.PagamentoType.CHEQUE;
import static com.noriakijr.corretors.model.PagamentoType.DINHEIRO;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class PagamentoConverter implements AttributeConverter<PagamentoType, String>{

	public String convertToDatabaseColumn(PagamentoType pagamento) {
		switch (pagamento) {
		case DINHEIRO:
			return "DINH";
		case CHEQUE:
			return "CHEQ";
		case CARTAO_DEBITO:
			return "DEBI";
		case CARTAO_CREDITO:
			return "CRED";
		default:
			throw new IllegalArgumentException(new StringBuilder("Unknown").append(pagamento).toString());
		}
	}

	public PagamentoType convertToEntityAttribute(String dbData) {
		switch (dbData) {
		case "DINH":
			return DINHEIRO;
		case "CHEQ":
			return CHEQUE;
		case "DEBI":
			return CARTAO_DEBITO;
		case "CRED":
			return CARTAO_CREDITO;
		default:
			throw new IllegalArgumentException(new StringBuilder("Unknown").append(dbData).toString());
		}
	}

}
