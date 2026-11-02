package com.noriakijr.corretors.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noriakijr.corretors.model.ComissaoVendedor;
import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.TipoComissao;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ComissaoVendedorRepository;
import com.noriakijr.corretors.web.dto.ComissaoVendedorResponse;

@Service
public class ComissaoService {

	private final ComissaoVendedorRepository comissoes;

	public ComissaoService(ComissaoVendedorRepository comissoes) {
		this.comissoes = comissoes;
	}

	public void applyPadrao(Plano plano, TipoComissao tipo, Double percentual, Double valorFixo) {
		if (tipo == null) {
			plano.setTipoComissao(null);
			plano.setPercentualComissao(null);
			plano.setValorFixoComissao(null);
			return;
		}
		Valores valores = validar(tipo, percentual, valorFixo);
		plano.setTipoComissao(tipo);
		plano.setPercentualComissao(valores.percentual());
		plano.setValorFixoComissao(valores.valorFixo());
	}

	public ComissaoVendedor upsert(Plano plano, Vendedor vendedor, TipoComissao tipo, Double percentual, Double valorFixo) {
		if (tipo == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo de comissao obrigatorio");
		}
		Valores valores = validar(tipo, percentual, valorFixo);
		ComissaoVendedor regra = comissoes.findByPlanoIdAndVendedorId(plano.getId(), vendedor.getId())
				.orElseGet(ComissaoVendedor::new);
		regra.setPlano(plano);
		regra.setVendedor(vendedor);
		regra.setTipoComissao(tipo);
		regra.setPercentual(valores.percentual());
		regra.setValorFixo(valores.valorFixo());
		return comissoes.save(regra);
	}

	public void remove(Long planoId, Long vendedorId) {
		ComissaoVendedor regra = comissoes.findByPlanoIdAndVendedorId(planoId, vendedorId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"comissao do vendedor nao encontrada"));
		comissoes.delete(regra);
	}

	public List<ComissaoVendedorResponse> list(Long planoId) {
		return grouped().getOrDefault(planoId, List.of());
	}

	public Map<Long, List<ComissaoVendedorResponse>> grouped() {
		return comissoes.findAllWithVendedor().stream()
				.collect(Collectors.groupingBy(
						regra -> regra.getPlano().getId(),
						Collectors.collectingAndThen(Collectors.toList(), ComissaoService::sorted)));
	}

	public void aplicar(Contrato contrato, Plano plano, Vendedor vendedor, double total) {
		Resultado resultado = calcular(plano, vendedor, total);
		contrato.setValorComissao(resultado.valorComissao());
		contrato.setReceitaCorretora(resultado.receitaCorretora());
	}

	private Resultado calcular(Plano plano, Vendedor vendedor, double total) {
		TipoComissao tipo = plano.getTipoComissao();
		Double percentual = plano.getPercentualComissao();
		Double valorFixo = plano.getValorFixoComissao();
		if (vendedor != null && vendedor.getId() != null && plano.getId() != null) {
			ComissaoVendedor excecao = comissoes.findByPlanoIdAndVendedorId(plano.getId(), vendedor.getId())
					.orElse(null);
			if (excecao != null) {
				tipo = excecao.getTipoComissao();
				percentual = excecao.getPercentual();
				valorFixo = excecao.getValorFixo();
			}
		}
		long totalCents = Math.round(total * 100.0);
		if (totalCents < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "total nao pode ser negativo");
		}
		long comissaoCents = 0;
		if (tipo == TipoComissao.PERCENTUAL && percentual != null) {
			if (percentual > 100.0) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"percentual de comissao nao pode passar de 100");
			}
			comissaoCents = Math.round(totalCents * percentual / 100.0);
		} else if (tipo == TipoComissao.VALOR_FIXO && valorFixo != null) {
			comissaoCents = Math.round(valorFixo * 100.0);
			if (comissaoCents > totalCents) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"valor fixo da comissao nao pode ser maior que o total");
			}
		}
		return new Resultado(comissaoCents / 100.0, (totalCents - comissaoCents) / 100.0);
	}

	private static Valores validar(TipoComissao tipo, Double percentual, Double valorFixo) {
		if (tipo == TipoComissao.PERCENTUAL) {
			if (percentual == null || percentual < 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "percentual de comissao invalido");
			}
			if (percentual > 100.0) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"percentual de comissao nao pode passar de 100");
			}
			return new Valores(percentual, null);
		}
		if (valorFixo == null || valorFixo < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valor fixo da comissao invalido");
		}
		return new Valores(null, valorFixo);
	}

	private static List<ComissaoVendedorResponse> sorted(List<ComissaoVendedor> regras) {
		return regras.stream()
				.sorted(Comparator.comparing(
						regra -> regra.getVendedor().getNome() == null ? "" : regra.getVendedor().getNome(),
						String.CASE_INSENSITIVE_ORDER))
				.map(ComissaoVendedorResponse::from)
				.toList();
	}

	private record Valores(Double percentual, Double valorFixo) {
	}

	private record Resultado(double valorComissao, double receitaCorretora) {
	}

}
