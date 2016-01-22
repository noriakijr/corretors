package com.noriakijr.corretors.model;

import java.io.Serializable;

import javax.persistence.Entity;

@Entity
public class Titular extends PessoaPF implements Serializable {

	private static final long serialVersionUID = 1L;

	private Boolean cartaoEnv; 				// cartoes enviados
	private Boolean seguroVidaEnv; 			// seguro enviado

	public Boolean getCartaoEnv() {
		return cartaoEnv;
	}

	public void setCartaoEnv(Boolean cartaoEnv) {
		this.cartaoEnv = cartaoEnv;
	}

	public Boolean getSeguroVidaEnv() {
		return seguroVidaEnv;
	}

	public void setSeguroVidaEnv(Boolean seguroVidaEnv) {
		this.seguroVidaEnv = seguroVidaEnv;
	}

}
