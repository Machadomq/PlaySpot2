package br.com.playspot.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.playspot.quadra.EsporteResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/metricas")
    public MetricasResponse metricas() {
        return adminService.metricas();
    }

    @GetMapping("/empresas")
    public List<EmpresaAdminItem> empresas() {
        return adminService.listarEmpresas();
    }

    @PostMapping("/empresas/{id}/aprovacao")
    public EmpresaAdminItem aprovar(@PathVariable Long id) {
        return adminService.definirAprovacao(id, true);
    }

    @PostMapping("/empresas/{id}/revogacao")
    public EmpresaAdminItem revogar(@PathVariable Long id) {
        return adminService.definirAprovacao(id, false);
    }

    @PostMapping("/esportes")
    @ResponseStatus(HttpStatus.CREATED)
    public EsporteResponse criarEsporte(@Valid @RequestBody EsporteRequest pedido) {
        return adminService.criarEsporte(pedido);
    }

    @PutMapping("/esportes/{id}")
    public EsporteResponse renomearEsporte(@PathVariable Short id, @Valid @RequestBody EsporteRequest pedido) {
        return adminService.renomearEsporte(id, pedido);
    }

    @DeleteMapping("/esportes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerEsporte(@PathVariable Short id) {
        adminService.removerEsporte(id);
    }

    @PostMapping("/usuarios/senha-temporaria")
    public SenhaTemporariaResponse senhaTemporaria(@Valid @RequestBody SenhaTemporariaRequest pedido) {
        return adminService.gerarSenhaTemporaria(pedido);
    }
}
