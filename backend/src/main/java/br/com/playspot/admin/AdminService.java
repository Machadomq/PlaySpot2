package br.com.playspot.admin;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.playspot.comum.NaoEncontradoException;
import br.com.playspot.comum.RegraDeNegocioException;
import br.com.playspot.empresa.Empresa;
import br.com.playspot.empresa.EmpresaRepository;
import br.com.playspot.quadra.Esporte;
import br.com.playspot.quadra.EsporteRepository;
import br.com.playspot.quadra.EsporteResponse;
import br.com.playspot.quadra.QuadraRepository;
import br.com.playspot.reserva.ReservaRepository;
import br.com.playspot.reserva.StatusReserva;
import br.com.playspot.usuario.Papel;
import br.com.playspot.usuario.Usuario;
import br.com.playspot.usuario.UsuarioRepository;

@Service
public class AdminService {

    private static final Duration PERIODO_DAS_METRICAS = Duration.ofDays(30);
    private static final int BYTES_DA_SENHA_TEMPORARIA = 12;

    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final EsporteRepository esportes;
    private final QuadraRepository quadras;
    private final ReservaRepository reservas;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom aleatorio = new SecureRandom();

    public AdminService(
            EmpresaRepository empresas,
            UsuarioRepository usuarios,
            EsporteRepository esportes,
            QuadraRepository quadras,
            ReservaRepository reservas,
            PasswordEncoder passwordEncoder) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.esportes = esportes;
        this.quadras = quadras;
        this.reservas = reservas;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmpresaAdminItem> listarEmpresas() {
        List<Empresa> lista = empresas.findAllByOrderByAprovadaAscNomeAsc();
        Map<Long, Usuario> donos = usuarios.findAllById(lista.stream().map(Empresa::getDonoId).toList()).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return lista.stream().map(empresa -> item(empresa, donos.get(empresa.getDonoId()))).toList();
    }

    @Transactional
    public EmpresaAdminItem definirAprovacao(Long empresaId, boolean aprovada) {
        Empresa empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));
        if (aprovada) {
            empresa.aprovar();
        } else {
            empresa.revogarAprovacao();
        }
        return item(empresa, usuarios.findById(empresa.getDonoId()).orElseThrow());
    }

    @Transactional
    public EsporteResponse criarEsporte(EsporteRequest pedido) {
        String nome = pedido.nome().trim();
        if (esportes.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Este esporte já existe.");
        }
        return EsporteResponse.de(esportes.save(new Esporte(nome)));
    }

    @Transactional
    public EsporteResponse renomearEsporte(Short id, EsporteRequest pedido) {
        Esporte esporte = esporte(id);
        String nome = pedido.nome().trim();
        if (esportes.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException("Este esporte já existe.");
        }
        esporte.renomear(nome);
        return EsporteResponse.de(esporte);
    }

    @Transactional
    public void removerEsporte(Short id) {
        Esporte esporte = esporte(id);
        if (quadras.existsByEsportes_Id(id)) {
            throw new RegraDeNegocioException("Este esporte está em uso por alguma quadra e não pode ser removido.");
        }
        esportes.delete(esporte);
    }

    @Transactional(readOnly = true)
    public MetricasResponse metricas() {
        Instant agora = Instant.now();
        Instant desde = agora.minus(PERIODO_DAS_METRICAS);
        return new MetricasResponse(
                usuarios.countByPapel(Papel.JOGADOR),
                usuarios.countByPapel(Papel.DONO),
                empresas.countByAprovada(true),
                empresas.countByAprovada(false),
                reservasCom(StatusReserva.CONFIRMADA, desde, agora),
                reservasCom(StatusReserva.FALTOU, desde, agora),
                reservasCom(StatusReserva.CANCELADA, desde, agora));
    }

    /** Sem serviço de e-mail, é assim que alguém recupera o acesso: o administrador entrega a senha à pessoa. */
    @Transactional
    public SenhaTemporariaResponse gerarSenhaTemporaria(SenhaTemporariaRequest pedido) {
        Usuario usuario = usuarios.findByEmail(pedido.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        if (usuario.getPapel() == Papel.ADMIN) {
            throw new RegraDeNegocioException("A senha de um administrador não pode ser redefinida por aqui.");
        }
        byte[] bytes = new byte[BYTES_DA_SENHA_TEMPORARIA];
        aleatorio.nextBytes(bytes);
        String senha = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        usuario.trocarSenha(passwordEncoder.encode(senha));
        return new SenhaTemporariaResponse(senha);
    }

    private long reservasCom(StatusReserva status, Instant desde, Instant ate) {
        return reservas.countByStatusAndInicioGreaterThanEqualAndInicioLessThan(status, desde, ate);
    }

    private Esporte esporte(Short id) {
        return esportes.findById(id).orElseThrow(() -> new NaoEncontradoException("Esporte não encontrado."));
    }

    private EmpresaAdminItem item(Empresa empresa, Usuario dono) {
        return new EmpresaAdminItem(
                empresa.getId(),
                empresa.getNome(),
                empresa.getCidade(),
                empresa.getUf(),
                empresa.getDocumento(),
                dono.getNome(),
                dono.getEmail(),
                empresa.isAprovada());
    }
}
