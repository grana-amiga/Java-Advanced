package br.com.granaamiga.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "dispositivo")
public class Dispositivo {

    @Id
    @Column(name = "id_dispositivo")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_dispositivo")
    @SequenceGenerator(name = "seq_dispositivo", sequenceName = "seq_dispositivo", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(name = "tarifa_kwh", nullable = false, precision = 12, scale = 6)
    private BigDecimal tarifaKwh;

    @Column(name = "meta_mensal_reais", nullable = false, precision = 19, scale = 2)
    private BigDecimal metaMensalReais;

    public Dispositivo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getTarifaKwh() {
        return tarifaKwh;
    }

    public void setTarifaKwh(BigDecimal tarifaKwh) {
        this.tarifaKwh = tarifaKwh;
    }

    public BigDecimal getMetaMensalReais() {
        return metaMensalReais;
    }

    public void setMetaMensalReais(BigDecimal metaMensalReais) {
        this.metaMensalReais = metaMensalReais;
    }
}
