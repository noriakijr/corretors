package com.noriakijr.corretors.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
public class Parcela implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Double vlrParcela;						// valor da parcela
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime dtParcela;						// data da parcela
	@ManyToOne @JoinColumn(name = "pagamento_id")
	private Pagamento pagamento;					// pertence a um determinado pagamento

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

	public DateTime getDtParcela() {
		return dtParcela;
	}

	public void setDtParcela(DateTime dtParcela) {
		this.dtParcela = dtParcela;
	}

	public Pagamento getPagamento() {
		return pagamento;
	}

	public void setPagamento(Pagamento pagamento) {
		this.pagamento = pagamento;
	}


}
