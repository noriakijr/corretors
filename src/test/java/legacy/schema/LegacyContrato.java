package legacy.schema;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Vendedor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Contrato as mapped before titular and vendedor became ManyToOne.
 * Kept outside the application package so Spring's entity scan does not pick it up.
 */
@Entity
@Table(name = "contrato")
public class LegacyContrato {

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

	public Long getId() {
		return id;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public void setDtAquisicao(LocalDateTime dtAquisicao) {
		this.dtAquisicao = dtAquisicao;
	}

	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	public void setVendedor(Vendedor vendedor) {
		this.vendedor = vendedor;
	}

	public void setPlanoHistorico(PlanoHistorico planoHistorico) {
		this.planoHistorico = planoHistorico;
	}

	public void setPagamento(Pagamento pagamento) {
		this.pagamento = pagamento;
	}

}
