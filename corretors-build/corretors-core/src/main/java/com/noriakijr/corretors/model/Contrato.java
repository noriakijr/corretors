package com.noriakijr.corretors.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
public class Contrato implements Serializable, IEntity {

	private static final long serialVersionUID = 1L;

	@Id	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String codigo;								// codigo do contrato
	private String descricao;							// descricao do contrato
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime dtAquisicao;						// data de aquisicao do plano
	@OneToOne @JoinColumn(name = "titular_id")
	private Titular titular;							// titular que assinou contrato
	@OneToOne @JoinColumn(name = "vendedor_id")
	private Vendedor vendedor;							// quem vendeu o contrato
	@OneToOne @JoinColumn(name = "planoHistorico_id")
	private PlanoHistorico planoHistorico;				// plano adquirido pelo cliente
	@OneToOne @JoinColumn(name = "pagamento_id")
	private Pagamento pagamento;						// pagamento

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

	public DateTime getDtAquisicao() {
		return dtAquisicao;
	}

	public void setDtAquisicao(DateTime dtAquisicao) {
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
