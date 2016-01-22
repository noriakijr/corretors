package com.noriakijr.corretors.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Dependente extends PessoaPF implements Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne @JoinColumn(name="titular_id")
	private Titular titular;
	
	public Titular getTitular() {
		return titular;
	}

	public void setTitular(Titular titular) {
		this.titular = titular;
	}

}
