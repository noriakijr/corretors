package com.noriakijr.corretors.model;

import java.io.Serializable;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pagamento implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String descricao;

	@Convert(converter = PagamentoConverter.class)
	private PagamentoType tipoPag;

	private Double vlrBruto;

	private Double vlrLiquido;

	private Double vlrDesconto;

	@Override
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
