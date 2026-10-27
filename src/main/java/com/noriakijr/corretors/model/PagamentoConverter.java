package com.noriakijr.corretors.model;

import static com.noriakijr.corretors.model.PagamentoType.CARTAO_CREDITO;
import static com.noriakijr.corretors.model.PagamentoType.CARTAO_DEBITO;
import static com.noriakijr.corretors.model.PagamentoType.CHEQUE;
import static com.noriakijr.corretors.model.PagamentoType.DINHEIRO;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PagamentoConverter implements AttributeConverter<PagamentoType, String> {

	@Override
	public String convertToDatabaseColumn(PagamentoType pagamento) {
		if (pagamento == null) {
			return null;
		}
		return switch (pagamento) {
			case DINHEIRO -> "DINH";
			case CHEQUE -> "CHEQ";
			case CARTAO_DEBITO -> "DEBI";
			case CARTAO_CREDITO -> "CRED";
		};
	}

	@Override
	public PagamentoType convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		return switch (dbData) {
			case "DINH" -> DINHEIRO;
			case "CHEQ" -> CHEQUE;
			case "DEBI" -> CARTAO_DEBITO;
			case "CRED" -> CARTAO_CREDITO;
			default -> throw new IllegalArgumentException("Unknown" + dbData);
		};
	}

}
