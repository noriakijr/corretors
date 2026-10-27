package com.noriakijr.corretors.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	private LocalDateTime dtInicio;

	private LocalDateTime dtFim;

	@Enumerated(EnumType.STRING)
	private ContratoStatus status;

	@ManyToOne(optional = false)
	@JoinColumn(name = "titular_id", nullable = false)
	private Titular titular;

	@ManyToOne
	@JoinColumn(name = "vendedor_id")
	private Vendedor vendedor;

	@ManyToOne
	@JoinColumn(name = "planoHistorico_id")
	private PlanoHistorico planoHistorico;

	@OneToOne
	@JoinColumn(name = "pagamento_id")
	private Pagamento pagamento;

	@ManyToOne
	@JoinColumn(name = "contrato_anterior_id")
	private Contrato contratoAnterior;

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

	public LocalDateTime getDtInicio() {
		return dtInicio;
	}

	public void setDtInicio(LocalDateTime dtInicio) {
		this.dtInicio = dtInicio;
	}

	public LocalDateTime getDtFim() {
		return dtFim;
	}

	public void setDtFim(LocalDateTime dtFim) {
		this.dtFim = dtFim;
	}

	public ContratoStatus getStatus() {
		return status;
	}

	public void setStatus(ContratoStatus status) {
		this.status = status;
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

	public Contrato getContratoAnterior() {
		return contratoAnterior;
	}

	public void setContratoAnterior(Contrato contratoAnterior) {
		this.contratoAnterior = contratoAnterior;
	}

}
