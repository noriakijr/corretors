package com.noriakijr.corretors.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Parcela implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Double vlrParcela;

	private LocalDateTime dtParcela;

	@ManyToOne
	@JoinColumn(name = "pagamento_id")
	private Pagamento pagamento;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getVlrParcela() {
		return vlrParcela;
	}

	public void setVlrParcela(Double vlrParcela) {
		this.vlrParcela = vlrParcela;
	}

	public LocalDateTime getDtParcela() {
		return dtParcela;
	}

	public void setDtParcela(LocalDateTime dtParcela) {
		this.dtParcela = dtParcela;
	}

	public Pagamento getPagamento() {
		return pagamento;
	}

	public void setPagamento(Pagamento pagamento) {
		this.pagamento = pagamento;
	}

}
