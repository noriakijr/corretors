package com.noriakijr.corretors.model;

import java.io.Serializable;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Pagamento implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String descricao; 			// Descricao do pagamento
	@Convert(converter = PagamentoConverter.class)
	private PagamentoType tipoPag;		// Forma de pagamento
	private Double vlrBruto; 			// Valor total bruto do pagamento
	private Double vlrLiquido; 			// Valor total liquido do pagamento
	private Double vlrDesconto; 		// Valor de desconto do pagamento

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public PagamentoType getTipoPag() {
		return tipoPag;
	}

	public void setTipoPag(PagamentoType tipoPag) {
		this.tipoPag = tipoPag;
	}

	public Double getVlrBruto() {
		return vlrBruto;
	}

	public void setVlrBruto(Double vlrBruto) {
		this.vlrBruto = vlrBruto;
	}

	public Double getVlrLiquido() {
		return vlrLiquido;
	}

	public void setVlrLiquido(Double vlrLiquido) {
		this.vlrLiquido = vlrLiquido;
	}

	public Double getVlrDesconto() {
		return vlrDesconto;
	}

	public void setVlrDesconto(Double vlrDesconto) {
		this.vlrDesconto = vlrDesconto;
	}

}
