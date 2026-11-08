package br.com.playspot.empresa;

import java.math.BigDecimal;

public record EmpresaResponse(
        Long id,
        String nome,
        String documento,
        String telefone,
        String endereco,
        String cidade,
        String uf,
        int prazoCancelamentoHoras,
        BigDecimal latitude,
        BigDecimal longitude) {

    public static EmpresaResponse de(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getDocumento(),
                empresa.getTelefone(),
                empresa.getEndereco(),
                empresa.getCidade(),
                empresa.getUf(),
                empresa.getPrazoCancelamentoHoras(),
                empresa.getLatitude(),
                empresa.getLongitude());
    }
}
