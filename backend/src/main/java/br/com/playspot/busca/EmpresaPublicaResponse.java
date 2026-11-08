package br.com.playspot.busca;

import java.math.BigDecimal;
import java.util.List;

import br.com.playspot.empresa.Empresa;
import br.com.playspot.quadra.QuadraResponse;

/** O que qualquer visitante vê da empresa: não inclui documento nem dados do dono. */
public record EmpresaPublicaResponse(
        Long id,
        String nome,
        String telefone,
        String endereco,
        String cidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude,
        int prazoCancelamentoHoras,
        List<QuadraResponse> quadras) {

    static EmpresaPublicaResponse de(Empresa empresa, List<QuadraResponse> quadras) {
        return new EmpresaPublicaResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getTelefone(),
                empresa.getEndereco(),
                empresa.getCidade(),
                empresa.getUf(),
                empresa.getLatitude(),
                empresa.getLongitude(),
                empresa.getPrazoCancelamentoHoras(),
                quadras);
    }
}
