package com.noriakijr.corretors.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Contrato implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String codigo;

	private String descricao;

	private LocalDateTime dtAquisicao;

	@OneToOne
	@JoinColumn(name = "titular_id")
	private Titular titular;

	@OneToOne
	@JoinColumn(name = "vendedor_id")
	private Vendedor vendedor;

	@OneToOne
	@JoinColumn(name = "planoHistorico_id")
	private PlanoHistorico planoHistorico;

	@OneToOne
	@JoinColumn(name = "pagamento_id")
	private Pagamento pagamento;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public LocalDateTime getDtAquisicao() {
		return dtAquisicao;
	}

	public void setDtAquisicao(LocalDateTime dtAquisicao) {
		this.dtAquisicao = dtAquisicao;
	}

	public Titular getTitular() {
		return titular;
	}

	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	public Vendedor getVendedor() {
		return vendedor;
	}

	public void setVendedor(Vendedor vendedor) {
		this.vendedor = vendedor;
	}

	public PlanoHistorico getPlanoHistorico() {
		return planoHistorico;
	}

	public void setPlanoHistorico(PlanoHistorico planoHistorico) {
		this.planoHistorico = planoHistorico;
	}

	public Pagamento getPagamento() {
		return pagamento;
	}

	public void setPagamento(Pagamento pagamento) {
		this.pagamento = pagamento;
	}

}
