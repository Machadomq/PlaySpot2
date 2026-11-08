package br.com.playspot.admin;

public record EmpresaAdminItem(
        Long id,
        String nome,
        String cidade,
        String uf,
        String documento,
        String donoNome,
        String donoEmail,
        boolean aprovada) {
}
