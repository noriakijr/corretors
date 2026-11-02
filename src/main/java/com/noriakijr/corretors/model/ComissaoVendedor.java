package com.noriakijr.corretors.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_comissao_plano_vendedor", columnNames = { "plano_id",
		"vendedor_id" }))
public class ComissaoVendedor implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "plano_id", nullable = false)
	private Plano plano;

	@ManyToOne(optional = false)
	@JoinColumn(name = "vendedor_id", nullable = false)
	private Vendedor vendedor;

	@Enumerated(EnumType.STRING)
	private TipoComissao tipoComissao;

	private Double percentual;

	private Double valorFixo;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Plano getPlano() {
		return plano;
	}

	public void setPlano(Plano plano) {
		this.plano = plano;
	}

	public Vendedor getVendedor() {
		return vendedor;
	}

	public void setVendedor(Vendedor vendedor) {
		this.vendedor = vendedor;
	}

	public TipoComissao getTipoComissao() {
		return tipoComissao;
	}

	public void setTipoComissao(TipoComissao tipoComissao) {
		this.tipoComissao = tipoComissao;
	}

	public Double getPercentual() {
		return percentual;
	}

	public void setPercentual(Double percentual) {
		this.percentual = percentual;
	}

	public Double getValorFixo() {
		return valorFixo;
	}

	public void setValorFixo(Double valorFixo) {
		this.valorFixo = valorFixo;
	}

}
