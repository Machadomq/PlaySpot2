package br.com.playspot.empresa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.NaoEncontradoException;

@Service
public class EmpresaService {

    private final EmpresaRepository empresas;

    public EmpresaService(EmpresaRepository empresas) {
        this.empresas = empresas;
    }

    @Transactional(readOnly = true)
    public Empresa doDono(Long donoId) {
        return empresas.findByDonoId(donoId)
                .orElseThrow(() -> new NaoEncontradoException("Empresa ainda não cadastrada."));
    }

    /** Cria a empresa do dono na primeira vez e atualiza nas seguintes: cada conta tem uma única empresa. */
    @Transactional
    public Empresa salvar(Long donoId, EmpresaRequest dados) {
        Empresa empresa = empresas.findByDonoId(donoId).orElseGet(() -> new Empresa(donoId));
        empresa.atualizar(dados);
        return empresas.save(empresa);
    }
}
