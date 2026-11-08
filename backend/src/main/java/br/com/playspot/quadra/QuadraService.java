package br.com.playspot.quadra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.playspot.comum.ArmazenamentoDeArquivos;
import br.com.playspot.comum.DadosInvalidosException;
import br.com.playspot.comum.NaoEncontradoException;
import br.com.playspot.comum.RegraDeNegocioException;
import br.com.playspot.empresa.Empresa;
import br.com.playspot.empresa.EmpresaRepository;

@Service
public class QuadraService {

    static final int MAXIMO_DE_FOTOS = 5;

    private final QuadraRepository quadras;
    private final QuadraFotoRepository fotos;
    private final EsporteRepository esportes;
    private final EmpresaRepository empresas;
    private final ArmazenamentoDeArquivos armazenamento;

    public QuadraService(
            QuadraRepository quadras,
            QuadraFotoRepository fotos,
            EsporteRepository esportes,
            EmpresaRepository empresas,
            ArmazenamentoDeArquivos armazenamento) {
        this.quadras = quadras;
        this.fotos = fotos;
        this.esportes = esportes;
        this.empresas = empresas;
        this.armazenamento = armazenamento;
    }

    public record FotoConteudo(byte[] bytes, String tipoConteudo) {
    }

    @Transactional(readOnly = true)
    public List<QuadraResponse> listar(Long donoId) {
        return empresas.findByDonoId(donoId)
                .map(empresa -> respostas(quadras.findByEmpresaIdOrderByNomeAsc(empresa.getId())))
                .orElse(List.of());
    }

    @Transactional
    public QuadraResponse criar(Long donoId, QuadraRequest dados) {
        Empresa empresa = empresas.findByDonoId(donoId)
                .orElseThrow(() -> new RegraDeNegocioException("Cadastre sua empresa antes de criar quadras."));
        Quadra quadra = new Quadra(empresa.getId());
        quadra.atualizar(dados, esportesValidos(dados));
        return resposta(quadras.save(quadra));
    }

    @Transactional
    public QuadraResponse atualizar(Long donoId, Long quadraId, QuadraRequest dados) {
        Quadra quadra = quadraDoDono(donoId, quadraId);
        quadra.atualizar(dados, esportesValidos(dados));
        return resposta(quadra);
    }

    @Transactional
    public QuadraResponse adicionarFoto(Long donoId, Long quadraId, MultipartFile arquivo) {
        Quadra quadra = quadraDoDono(donoId, quadraId);
        if (fotos.countByQuadraId(quadraId) >= MAXIMO_DE_FOTOS) {
            throw new RegraDeNegocioException("Cada quadra pode ter no máximo " + MAXIMO_DE_FOTOS + " fotos.");
        }

        byte[] conteudo = lerBytes(arquivo);
        FormatoDeImagem formato = FormatoDeImagem.detectar(conteudo)
                .orElseThrow(() -> new DadosInvalidosException("Envie uma imagem JPG, PNG ou WebP."));

        String chave = UUID.randomUUID() + "." + formato.extensao;
        armazenamento.salvar(chave, conteudo);
        fotos.save(new QuadraFoto(quadraId, chave, formato.tipoConteudo));
        return resposta(quadra);
    }

    @Transactional
    public QuadraResponse removerFoto(Long donoId, Long quadraId, Long fotoId) {
        Quadra quadra = quadraDoDono(donoId, quadraId);
        QuadraFoto foto = fotos.findByIdAndQuadraId(fotoId, quadraId)
                .orElseThrow(() -> new NaoEncontradoException("Foto não encontrada."));
        fotos.delete(foto);
        fotos.flush();
        armazenamento.apagar(foto.getChave());
        return resposta(quadra);
    }

    @Transactional(readOnly = true)
    public FotoConteudo lerFoto(Long fotoId) {
        QuadraFoto foto = fotos.findById(fotoId)
                .orElseThrow(() -> new NaoEncontradoException("Foto não encontrada."));
        return new FotoConteudo(armazenamento.ler(foto.getChave()), foto.getTipoConteudo());
    }

    /** Busca sempre dentro da empresa do dono: a quadra de outra empresa se comporta como inexistente. */
    public Quadra quadraDoDono(Long donoId, Long quadraId) {
        return empresas.findByDonoId(donoId)
                .flatMap(empresa -> quadras.findByIdAndEmpresaId(quadraId, empresa.getId()))
                .orElseThrow(() -> new NaoEncontradoException("Quadra não encontrada."));
    }

    private List<Esporte> esportesValidos(QuadraRequest dados) {
        List<Esporte> encontrados = esportes.findAllById(dados.esporteIds());
        if (encontrados.size() != dados.esporteIds().size()) {
            throw new DadosInvalidosException("Esporte inválido.");
        }
        return encontrados;
    }

    private byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private QuadraResponse resposta(Quadra quadra) {
        return respostas(List.of(quadra)).getFirst();
    }

    private List<QuadraResponse> respostas(List<Quadra> lista) {
        List<Long> ids = lista.stream().map(Quadra::getId).toList();
        Map<Long, List<Long>> fotosPorQuadra = fotos.findByQuadraIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        QuadraFoto::getQuadraId,
                        Collectors.mapping(QuadraFoto::getId, Collectors.toList())));
        return lista.stream()
                .map(q -> QuadraResponse.de(q, fotosPorQuadra.getOrDefault(q.getId(), List.of())))
                .toList();
    }
}
