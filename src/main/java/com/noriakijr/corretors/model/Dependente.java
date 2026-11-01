package com.noriakijr.corretors.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Dependente extends PessoaPF implements Serializable {

	private static final long serialVersionUID = 1L;

	private String parentesco;

	@ManyToOne
	@JoinColumn(name = "titular_id")
	private Titular titular;

	@ManyToOne
	@JoinColumn(name = "contrato_id")
	private Contrato contrato;

	public String getParentesco() {
		return parentesco;
	}

	public void setParentesco(String parentesco) {
		this.parentesco = parentesco;
	}

	public Titular getTitular() {
		return titular;
	}

	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

}
