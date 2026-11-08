package br.com.playspot.empresa;

public record EmpresaResponse(
        Long id,
        String nome,
        String documento,
        String telefone,
        String endereco,
        String cidade,
        String uf,
        int prazoCancelamentoHoras) {

    public static EmpresaResponse de(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getDocumento(),
                empresa.getTelefone(),
                empresa.getEndereco(),
                empresa.getCidade(),
                empresa.getUf(),
                empresa.getPrazoCancelamentoHoras());
    }
}
