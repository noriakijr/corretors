package com.noriakijr.corretors.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Plano implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nome;

	private String descricao;

	@ManyToOne
	private Seguradora seguradora;

	private Integer limiteDependentes;

	@Enumerated(EnumType.STRING)
	private TipoComissao tipoComissao;

	private Double percentualComissao;

	private Double valorFixoComissao;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Seguradora getSeguradora() {
		return seguradora;
	}

	public void setSeguradora(Seguradora seguradora) {
		this.seguradora = seguradora;
	}

	public Integer getLimiteDependentes() {
		return limiteDependentes;
	}

	public void setLimiteDependentes(Integer limiteDependentes) {
		this.limiteDependentes = limiteDependentes;
	}

	public TipoComissao getTipoComissao() {
		return tipoComissao;
	}

	public void setTipoComissao(TipoComissao tipoComissao) {
		this.tipoComissao = tipoComissao;
	}

	public Double getPercentualComissao() {
		return percentualComissao;
	}

	public void setPercentualComissao(Double percentualComissao) {
		this.percentualComissao = percentualComissao;
	}

	public Double getValorFixoComissao() {
		return valorFixoComissao;
	}

	public void setValorFixoComissao(Double valorFixoComissao) {
		this.valorFixoComissao = valorFixoComissao;
	}

}
